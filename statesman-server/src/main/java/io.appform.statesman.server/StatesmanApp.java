package io.appform.statesman.server;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Stage;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.statesman.engine.util.MapperUtils;
import io.appform.statesman.server.module.DBModule;
import io.dropwizard.Application;
import io.dropwizard.riemann.RiemannBundle;
import io.dropwizard.riemann.RiemannConfig;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class StatesmanApp extends Application<AppConfig> {

    private DBShardingBundle<AppConfig> dbShardingBundle;

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
    }

    public static void main(String args[]) throws Exception {
        StatesmanApp app = new StatesmanApp();
        app.run();
    }

    private void setMapperProperties(ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
    }
}
