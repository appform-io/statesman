package io.appform.statesman.publisher.http;

import com.codahale.metrics.MetricRegistry;
import com.raskasa.metrics.okhttp.InstrumentedOkHttpClients;
import io.appform.statesman.publisher.impl.EventPublisherConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * @author shashank.g
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpUtil {

    public static OkHttpClient defaultClient(final String clientName,
                                             final MetricRegistry registry,
                                             final EventPublisherConfig configuration) {

        int connections = configuration.getConnections();
        connections = connections == 0 ? 10 : connections;

        int idleTimeOutSeconds = configuration.getIdleTimeOutSeconds();
        idleTimeOutSeconds = idleTimeOutSeconds == 0 ? 30 : idleTimeOutSeconds;

        int connTimeout = configuration.getConnectTimeoutMs();
        connTimeout = connTimeout == 0 ? 10000 : connTimeout;

        int opTimeout = configuration.getOpTimeoutMs();
        opTimeout = opTimeout == 0 ? 10000 : opTimeout;

        final Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(connections);
        dispatcher.setMaxRequestsPerHost(connections);

        final OkHttpClient.Builder clientBuilder = (new OkHttpClient.Builder())
                .connectionPool(new ConnectionPool(connections, (long) idleTimeOutSeconds, TimeUnit.SECONDS))
                .connectTimeout((long) connTimeout, TimeUnit.MILLISECONDS)
                .readTimeout((long) opTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout((long) opTimeout, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher);

        return registry != null ? InstrumentedOkHttpClients.create(registry, clientBuilder.build(), clientName) : clientBuilder.build();
    }
}
