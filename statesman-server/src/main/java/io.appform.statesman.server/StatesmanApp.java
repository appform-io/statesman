package io.appform.statesman.server;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.inject.Stage;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.functionmetrics.FunctionMetricsManager;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.impl.HttpEventPublisher;
import io.appform.statesman.server.exception.GenericExceptionMapper;
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
        environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
}
