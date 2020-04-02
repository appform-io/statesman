package io.appform.statesman.server.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.model.WorkflowTemplate;
import io.dropwizard.lifecycle.Managed;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class WorkflowTemplateSelector implements Managed {

    private WorkflowProvider workflowProvider;
    private HopeLangEngine hopeLangEngine;
    private final AtomicReference<List<WorkflowTemplateContext>> parsedWorkflowTemplates;
    private final ScheduledExecutorService executorService;

    @Inject
    public WorkflowTemplateSelector(WorkflowProvider workflowProvider) {
        this.workflowProvider = workflowProvider;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        this.parsedWorkflowTemplates = new AtomicReference<>(new ArrayList<>());
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::loadParsedWorkflowTemplates, 0, 600, TimeUnit.SECONDS);

    }

    public Optional<WorkflowTemplate> determineTemplate(JsonNode translatedPayload) {

        return parsedWorkflowTemplates.get()
                .stream()
                .filter(parsedWorkflowContext -> hopeLangEngine
                        .evaluate(parsedWorkflowContext.getParsedRule(), translatedPayload))
                .map(WorkflowTemplateContext::getTemplate)
                .findFirst();
    }


    private void loadParsedWorkflowTemplates() {
        List<WorkflowTemplateContext> parsedTemplates = workflowProvider.getAll()
                .stream()
                .filter(WorkflowTemplate::isActive)
                .flatMap(template -> template.getRules().stream().map(rule -> {
                    Evaluatable parsedTemplateRule;
                    try {
                        parsedTemplateRule = hopeLangEngine.parse(rule);
                    } catch (Exception e) {
                        log.error("Error parsing template for workflow: " + template.getId(), e);
                        return null;
                    }
                    return new WorkflowTemplateContext(template, parsedTemplateRule);
                }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        parsedWorkflowTemplates.set(parsedTemplates);
    }

    @Override
    public void start(){

    }

    @Override
    public void stop() {
        executorService.shutdown();
    }


}
