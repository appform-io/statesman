package io.appform.statesman.server;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Stage;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.functionmetrics.FunctionMetricsManager;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.impl.HttpEventPublisher;
import io.appform.statesman.server.exception.GenericExceptionMapper;
import io.appform.statesman.engine.util.MapperUtils;
import io.appform.statesman.server.module.DBModule;
import io.appform.statesman.server.resources.EventResource;
import io.appform.statesman.server.resources.ProviderResource;
import io.dropwizard.Application;
import io.dropwizard.riemann.RiemannBundle;
import io.dropwizard.riemann.RiemannConfig;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.zapodot.hystrix.bundle.HystrixBundle;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class StatesmanApp extends Application<AppConfig> {

    private DBShardingBundle<AppConfig> dbShardingBundle;
    private EventPublisher eventPublisher;
    private final HystrixBundle hystrixBundle = initHystrixBundle();

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        final ObjectMapper mapper = bootstrap.getObjectMapper();
        setMapperProperties(mapper);
        MapperUtils.initialize(mapper);
        this.dbShardingBundle = new DBShardingBundle<AppConfig>("io.appform.statesman.server") {
            @Override
            protected ShardedHibernateFactory getConfig(AppConfig appConfig) {
                return appConfig.getShards();
            }
        };
        bootstrap.addBundle(dbShardingBundle);
        bootstrap.addBundle(guiceBundle(dbShardingBundle));
        bootstrap.addBundle(this.hystrixBundle);
        bootstrap.addBundle(new RiemannBundle<AppConfig>() {
            @Override
            public RiemannConfig getRiemannConfiguration(AppConfig configuration) {
                return configuration.getRiemann();
            }
        });
    }

    GuiceBundle<AppConfig> guiceBundle(DBShardingBundle<AppConfig> dbShardingBundle) {
        return GuiceBundle.<AppConfig>builder()
                .enableAutoConfig(getClass().getPackage().getName())
                .modules(new DBModule(dbShardingBundle))
                .build(Stage.PRODUCTION);
    }


    @Override
    public void run(AppConfig appConfig, Environment environment) throws Exception {
        FunctionMetricsManager.initialize("commands", environment.metrics());

        //TODO: move to guice -  testing this for now
        this.eventPublisher = new HttpEventPublisher(appConfig.getEventPublisherConfig(),
                environment.metrics(),
                environment.getObjectMapper());

        environment.jersey().register(ProviderResource.class);
        environment.jersey().register(new EventResource(eventPublisher));
        environment.jersey().register(GenericExceptionMapper.class);
    }

    public static void main(String args[]) throws Exception {
        StatesmanApp app = new StatesmanApp();
        app.run();
    }

    private HystrixBundle initHystrixBundle() {
        return HystrixBundle
                .builder()
                .disableStreamServletInAdminContext()
                .withApplicationStreamPath("/hystrix.stream")
                .build();
    }
    private void setMapperProperties(ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
    }
}
