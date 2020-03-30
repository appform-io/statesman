package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.engine.TransitionStore;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.publisher.impl.KafkaEventClient;
import io.appform.statesman.server.AppConfig;
import io.appform.statesman.server.callbacktransformation.CallbackTransformationTemplates;
import io.appform.statesman.server.dao.action.ActionTemplateStoreCommand;
import io.appform.statesman.server.dao.transition.TransitionStoreCommand;
import io.appform.statesman.server.dao.workflow.WorkflowProviderCommand;
import io.dropwizard.setup.Environment;

public class StatesmanModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActionTemplateStore.class).to(ActionTemplateStoreCommand.class);
        bind(TransitionStore.class).to(TransitionStoreCommand.class);
        bind(WorkflowProvider.class).to(WorkflowProviderCommand.class);
    }

    @Singleton
    @Provides
    public io.appform.statesman.publisher.EventPublisher provideEventPublisher(AppConfig appConfig, Environment environment) {
        return new KafkaEventClient(appConfig.getEventPublisherConfig(),
                environment.metrics(),
                environment.getObjectMapper());
    }

    @Singleton
    @Provides
    public CallbackTransformationTemplates callbackTransformationTemplates(AppConfig config) {
        return config.getCallbackTransformationTemplates();
    }
}
