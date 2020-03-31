package io.appform.statesman.publisher.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.publisher.model.Event;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author shashank.g
 */
@Slf4j
public class QueueEventPublisher extends HttpClient implements EventPublisher {

    private static final int RETRIES = 5;
    private static final int MAX_PAYLOAD_SIZE = 2000000; //2MB
    private final String path;
    private final MessageSenderThread messageSenderThread;
    private final ScheduledExecutorService scheduler;
    private IBigQueue messageQueue;


    /**
     * Instantiates a new Queued queueEventPublisher.
     */
    public QueueEventPublisher(final ObjectMapper mapper,
                               final EventPublisherConfig config,
                               final MetricRegistry registry) throws IOException {
        super(mapper, HttpUtil.defaultClient(SyncEventPublisher.class.getSimpleName(), registry, config.getHttpClientConfiguration()));
        this.path = config.getQueuePath();

        final SyncEventPublisher syncEventPublisher = new SyncEventPublisher(
                mapper,
                config,
                registry);

        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        try {
            Files.createDirectories(Paths.get(path), attr);
        } catch (final FileAlreadyExistsException e) {
            log.warn("queue path already exists");
        }

        this.messageQueue = new BigQueueImpl(path, "statesman-messages");
        this.messageSenderThread = new MessageSenderThread(this, syncEventPublisher, messageQueue, path, mapper, config.getBatchSize());
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.scheduler.scheduleWithFixedDelay(messageSenderThread, 0, 1, TimeUnit.SECONDS);
        this.scheduler.scheduleWithFixedDelay(new QueueCleaner(messageQueue, path), 0, 15, TimeUnit.SECONDS);
    }

    @Override
    public void publish(Event event) throws Exception {
        this.messageQueue.enqueue(mapper.writeValueAsBytes(event));
    }

    @Override
    public void publish(String topic, List<Event> events) throws Exception {
        for (Event event : events) {
            event.setTopic(topic);
            this.messageQueue.enqueue(mapper.writeValueAsBytes(event));
        }
    }

    private void enqueue(List<Event> events) throws IOException {
        for (Event event : events) {
            this.messageQueue.enqueue(mapper.writeValueAsBytes(event));
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("queue={} closing_queued_sender", new Object[]{path});
        while (!messageQueue.isEmpty()) {
            Thread.sleep(500);
            log.info("queue={} message_queue_not_empty waiting_for_queue_to_get_empty", new Object[]{path});
        }

        while (messageSenderThread.isRunning()) {
            Thread.sleep(500);
            log.info("queue={} message_sender_thread_still_running waiting_for_completion", new Object[]{path});
        }
        this.scheduler.shutdownNow();
        log.info("queue={} shutting_down_message_sender_thread", new Object[]{path});
        log.info("queue={} shutdown_completed_for_message_sender_thread", new Object[]{path});
    }

    @Override
    public void start() throws Exception {
        log.info("starting queue sender");
    }

    private static final class MessageSenderThread implements Runnable {
        private final QueueEventPublisher queueEventPublisher;
        private final ObjectMapper mapper;
        private IBigQueue messageQueue;
        private int batchSize;
        private String path;
        private final SyncEventPublisher publisher;
        private AtomicBoolean running = new AtomicBoolean(false);

        public MessageSenderThread(QueueEventPublisher queueEventPublisher,
                                   SyncEventPublisher publisher,
                                   IBigQueue messageQueue,
                                   String path,
                                   ObjectMapper mapper,
                                   int batchSize) {
            this.queueEventPublisher = queueEventPublisher;
            this.messageQueue = messageQueue;
            this.path = path;
            this.mapper = mapper;
            this.batchSize = batchSize;
            this.publisher = publisher;
        }

        @Override
        public void run() {
            running.set(true);
            try {
                while (!messageQueue.isEmpty()) {
                    log.info("queue={} messages_found_in_message_queue sender_invoked", new Object[]{path});
                    List<Event> entries = Lists.newArrayListWithExpectedSize(batchSize);
                    int sizeOfPayload = 0;

                    for (int i = 0; i < batchSize; i++) {
                        byte data[] = messageQueue.dequeue();
                        if (null == data) {
                            break;
                        }
                        // Check added to keep avoid payload size greater than 2MB from being pushed in one batch calls
                        sizeOfPayload += data.length + 24 + 8;
                        if (sizeOfPayload > MAX_PAYLOAD_SIZE) {
                            if (data.length + 24 + 8 > MAX_PAYLOAD_SIZE) { //A single message > 2MB..
                                log.error("queue={} message_size_limit_exceeded(2MB) message={}", new Object[]{path, new String(data)});
                                continue; //Move to next message
                            } else {
                                log.info("queue={} batch_data_size_exceeds_threshold(2MB) size={} truncating_batch_size enqueuing_last_message_for_next_batch",
                                        new Object[]{path, sizeOfPayload});
                                messageQueue.enqueue(data);
                                break;
                            }
                        }
                        entries.add(mapper.readValue(data, Event.class));
                    }
                    if (!entries.isEmpty()) {
                        int retryCount = 0;
                        do {
                            retryCount++;
                            try {
                                final Map<String, List<Event>> topicEventsMap = getEventTypeListMap(entries);
                                topicEventsMap.forEach((topic, events) -> publisher.publish(topic, entries));

                                log.info("queue={} statesman_messages_sent count={}", new Object[]{path, entries.size()});
                                break;
                            } catch (final Throwable t) {
                                log.error("queue={} message_send_failed count={}", new Object[]{path, entries.size()}, t);
                            }
                        } while (retryCount <= RETRIES);
                        if (retryCount > RETRIES) {
                            log.error("queue={} message_send_failed probably_api_down  re-queuing_messages", new Object[]{path});
                            queueEventPublisher.enqueue(entries);
                            break;
                        }
                    } else {
                        log.info("queue={} nothing_to_send_to_statesman", new Object[]{path});
                    }
                }
            } catch (Exception e) {
                log.error("queue={} message_send_failed", new Object[]{path}, e);
            }
            running.set(false);
        }

        private boolean isRunning() {
            return running.get();
        }
    }

    //move to lambda
    private static Map<String, List<Event>> getEventTypeListMap(List<Event> entries) {
        final Map<String, List<Event>> topicEventsMap = Maps.newHashMap();
        entries.forEach(entry -> {
            if (topicEventsMap.get(entry.getTopic()) != null) {
                topicEventsMap.get(entry.getTopic()).add(entry);
            } else {
                List<Event> events = Lists.newArrayList();
                events.add(entry);
                topicEventsMap.put(entry.getTopic(), events);
            }
        });
        return topicEventsMap;
    }

    private static final class QueueCleaner implements Runnable {
        private IBigQueue messageQueue;
        private String path;

        private QueueCleaner(IBigQueue messageQueue, String path) {
            this.messageQueue = messageQueue;
            this.path = path;
        }

        @Override
        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                this.messageQueue.gc();
                log.info("queue={} ran_gc_on_statesman_message_queue took={}", new Object[]{path, System.currentTimeMillis() - startTime});
            } catch (Exception e) {
                log.error("queue={} gc_failed_on_statesman_message_queue", new Object[]{path}, e);
            }
        }
    }

}
