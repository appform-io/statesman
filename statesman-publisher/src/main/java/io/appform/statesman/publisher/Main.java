package io.appform.statesman.publisher;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.EventType;

import java.util.List;
import java.util.Map;

/**
 * @author shashank.g
 */
public class Main {
    public static void main(String[] args) {
        List<Event> entries = Lists.newArrayList();

        entries.add(Event.builder()
                .eventType(EventType.reporting)
                .groupingKey("R1")
                .eventData("R1")
                .build());
        entries.add(Event.builder()
                .eventType(EventType.app)
                .groupingKey("A2")
                .eventData("A2")
                .build());

        entries.add(Event.builder()
                .eventType(EventType.reporting)
                .groupingKey("R2")
                .eventData("R2")
                .build());

        final Map<EventType, List<Event>> topicEventsMap = Maps.newHashMap();


        entries.forEach(entry -> {
            if (topicEventsMap.get(entry.getEventType()) != null) {
                topicEventsMap.get(entry.getEventType()).add(entry);
            }else {
                List<Event> events = Lists.newArrayList();
                events.add(entry);
                topicEventsMap.put(entry.getEventType(), events);

            }
        });
        System.out.println(topicEventsMap);
    }
}
