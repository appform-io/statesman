package io.appform.statesman.server;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Stage;
import com.hystrix.configurator.core.HystrixConfigurationFactory;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.functionmetrics.FunctionMetricsManager;
import io.appform.statesman.server.exception.GenericExceptionMapper;
import io.appform.statesman.server.module.DBModule;
import io.appform.statesman.server.module.StatesmanModule;
import io.appform.statesman.server.utils.MapperUtils;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.riemann.RiemannBundle;
import io.dropwizard.riemann.RiemannConfig;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.zapodot.hystrix.bundle.HystrixBundle;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class StatesmanApp extends Application<AppConfig> {

    private DBShardingBundle<AppConfig> dbShardingBundle;

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                                               new EnvironmentVariableSubstitutor(false)));
        final ObjectMapper mapper = bootstrap.getObjectMapper();
        setMapperProperties(mapper);
        MapperUtils.initialize(mapper);
        this.dbShardingBundle = dbShardingBundle();
        bootstrap.addBundle(dbShardingBundle);
        bootstrap.addBundle(guiceBundle(dbShardingBundle));
        bootstrap.addBundle(hystrixBundle());
        bootstrap.addBundle(riemannBundle());
        bootstrap.addBundle(swaggerBundle());
    }


    @Override
    public void run(AppConfig appConfig, Environment environment) {
        FunctionMetricsManager.initialize("commands", environment.metrics());
        HystrixConfigurationFactory.init(appConfig.getHystrix());
        environment.jersey().register(GenericExceptionMapper.class);
    }

    public static void main(String[] args) throws Exception {
        StatesmanApp app = new StatesmanApp();
        app.run(args);
    }


    private DBShardingBundle<AppConfig> dbShardingBundle() {
        return new DBShardingBundle<AppConfig>("io.appform.statesman.server") {
            @Override
            protected ShardedHibernateFactory getConfig(AppConfig appConfig) {
                return appConfig.getShards();
            }
        };
    }

    private GuiceBundle<AppConfig> guiceBundle(DBShardingBundle<AppConfig> dbShardingBundle) {
        return GuiceBundle.<AppConfig>builder()
                .enableAutoConfig(getClass().getPackage().getName())
                .modules(new DBModule(dbShardingBundle))
                .modules(new StatesmanModule())
                .build(Stage.PRODUCTION);
    }

    private HystrixBundle hystrixBundle() {
        return HystrixBundle
                .builder()
                .disableStreamServletInAdminContext()
                .withApplicationStreamPath("/hystrix.stream")
                .build();
    }

    private SwaggerBundle<AppConfig> swaggerBundle() {
        return new SwaggerBundle<AppConfig>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AppConfig config) {
                return config.getSwagger();
            }
        };
    }

    private RiemannBundle<AppConfig> riemannBundle() {
        return new RiemannBundle<AppConfig>() {
            @Override
            public RiemannConfig getRiemannConfiguration(AppConfig configuration) {
                return configuration.getRiemann();
            }
        };
    }

    private void setMapperProperties(ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
    }
}
