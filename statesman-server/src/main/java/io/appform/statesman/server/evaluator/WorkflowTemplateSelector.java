package io.appform.statesman.server.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.model.WorkflowTemplate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class WorkflowTemplateSelector {

    private WorkflowProvider workflowProvider;
    private HopeLangEngine hopeLangEngine;
    private final ConcurrentHashMap<String, List<WorkflowTemplateContext>> parsedWorkflowTemplateCache;

    @Inject
    @Builder
    public WorkflowTemplateSelector(
            @Named("workflowTemplateScheduledExecutorService") final ScheduledExecutorService executorService,
            WorkflowProvider workflowProvider) {
        this.workflowProvider = workflowProvider;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        this.parsedWorkflowTemplateCache = new ConcurrentHashMap<>();
        executorService.scheduleWithFixedDelay(this::loadParsedWorkflowTemplates, 0, 600, TimeUnit.SECONDS);
    }

    public Optional<WorkflowTemplate> determineTemplate(JsonNode translatedPayload) {

        List<WorkflowTemplateContext> parsedWorkflowTemplates = parsedWorkflowTemplateCache.values().
                stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return parsedWorkflowTemplates
                .stream()
                .filter(parsedWorkflowContext -> hopeLangEngine
                        .evaluate(parsedWorkflowContext.getParsedRule(), translatedPayload))
                .map(WorkflowTemplateContext::getTemplate)
                .findFirst();
    }


    private void loadParsedWorkflowTemplates(){
        Map<String,List<WorkflowTemplateContext>> parsedTemplates = workflowProvider.getAll()
                .stream()
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
                .collect(Collectors.groupingBy(templateContext -> {
                    return templateContext.getTemplate().getId();
                }));
        parsedWorkflowTemplateCache.putAll(parsedTemplates);
    }

}
