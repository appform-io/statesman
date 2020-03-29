package io.appform.statesman.server;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.inject.Stage;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.statesman.server.module.DBModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class StatemanApp extends Application<AppConfig> {

    private DBShardingBundle<AppConfig> dbShardingBundle;

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

    }
}
