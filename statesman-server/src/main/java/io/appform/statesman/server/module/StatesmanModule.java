package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.engine.TransitionStore;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.action.ActionRegistry;
import io.appform.statesman.engine.action.MapBasedActionRegistry;
import io.appform.statesman.engine.observer.ObservableEventBus;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableGuavaEventBus;
import io.appform.statesman.engine.observer.observers.ActionInvoker;
import io.appform.statesman.engine.observer.observers.FoxtrotEventSender;
import io.appform.statesman.engine.observer.observers.WorkflowPersister;
import io.appform.statesman.publisher.impl.KafkaEventClient;
import io.appform.statesman.server.AppConfig;
import io.appform.statesman.server.callbacktransformation.CallbackTransformationTemplates;
import io.appform.statesman.server.dao.action.ActionTemplateStoreCommand;
import io.appform.statesman.server.dao.transition.TransitionStoreCommand;
import io.appform.statesman.server.dao.workflow.WorkflowProviderCommand;
import io.dropwizard.setup.Environment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class StatesmanModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActionTemplateStore.class).to(ActionTemplateStoreCommand.class);
        bind(TransitionStore.class).to(TransitionStoreCommand.class);
        bind(WorkflowProvider.class).to(WorkflowProviderCommand.class);
        bind(ActionRegistry.class).to(MapBasedActionRegistry.class);
        bind(ObservableEventBus.class).to(ObservableGuavaEventBus.class);
        bind(ObservableEventBusSubscriber.class)
                .annotatedWith(Names.named("workflowPersister"))
                .to(WorkflowPersister.class);
        bind(ObservableEventBusSubscriber.class)
                .annotatedWith(Names.named("actionHandler"))
                .to(ActionInvoker.class);
        bind(ObservableEventBusSubscriber.class)
                .annotatedWith(Names.named("foxtrotEventSender"))
                .to(FoxtrotEventSender.class);
    }

    @Singleton
    @Provides
    public io.appform.statesman.publisher.EventPublisher provideEventPublisher(
            AppConfig appConfig,
            Environment environment) {
        return new KafkaEventClient(appConfig.getEventPublisherConfig(),
                                    environment.metrics(),
                                    environment.getObjectMapper());
    }

    @Singleton
    @Provides
    public CallbackTransformationTemplates callbackTransformationTemplates(AppConfig config) {
        return config.getCallbackTransformationTemplates();
    }

    @Provides
    @Singleton
    @Named("workflowTemplateScheduledExecutorService")
    public ScheduledExecutorService workflowTemplateScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
