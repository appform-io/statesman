package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.impl.HttpEventPublisher;
import io.appform.statesman.server.AppConfig;
import io.dropwizard.setup.Environment;

public class StatesmanModule extends AbstractModule {

    @Singleton
    @Provides
    public EventPublisher provideEventPublisher(AppConfig appConfig, Environment environment) {
        return new HttpEventPublisher(appConfig.getEventPublisherConfig(),
                environment.metrics(),
                environment.getObjectMapper());
    }

}
