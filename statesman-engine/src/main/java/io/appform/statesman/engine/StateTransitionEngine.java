package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.DefaultErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.model.DataUpdate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
@Slf4j
public class StateTransitionEngine {
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<ActionExecutor> actionExecutor;
    private final Provider<TransitionStore> transitionStore;
    private final ObjectMapper mapper;
    private final HopeLangEngine hopeLangEngine;
    private final Cache<String, Evaluatable> evalCache = CacheBuilder.newBuilder()
            .maximumSize(100_000)
            .build();
    @Inject
    public StateTransitionEngine(
            Provider<WorkflowProvider> workflowProvider,
            Provider<ActionExecutor> actionExecutor,
            Provider<TransitionStore> transitionStore,
            ObjectMapper mapper) {
        this.workflowProvider = workflowProvider;
        this.actionExecutor = actionExecutor;
        this.transitionStore = transitionStore;
        this.mapper = mapper;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new DefaultErrorHandlingStrategy())
                .build();

    }

    void handle(DataUpdate dataUpdate) {
        val workflowId = dataUpdate.getWorkflowId();
        val workflow = workflowProvider.get()
                .getWorkflow(workflowId)
                .orElse(null);
        Preconditions.checkNotNull(workflow);
        val currentState = workflow.getDataObject().getCurrentState();
        if(currentState.isTerminal()) {
            log.info("Workflow {} is already complete.", workflow.getId());
            return;
        }
        val template = workflowProvider.get()
                .getTemplate(workflow.getTemplateId())
                .orElse(null);
        Preconditions.checkNotNull(template);
        val transitions = transitionStore.get()
                .getTransitionFor(template.getId(), currentState.getName())
                .orElse(null);
        Preconditions.checkNotNull(transitions);
        val evalNode = mapper.createObjectNode();
        evalNode.putObject("data").setAll((ObjectNode)workflow.getDataObject().getData());
        evalNode.putObject("update").setAll((ObjectNode)dataUpdate.getData());
        val selectedTransition = transitions.stream()
                .filter(stateTransition -> {
                    val transitionRule = stateTransition.getRule();
                    var rule = evalCache.getIfPresent(transitionRule.getId());
                    if(null == rule) {
                        rule = hopeLangEngine.parse(transitionRule.getRule());
                        evalCache.put(transitionRule.getId(), rule);
                    }
                    return hopeLangEngine.evaluate(rule, evalNode);
                })
                .findFirst()
                .orElse(null);
        Preconditions.checkNotNull(selectedTransition);
        workflow.getDataObject().setCurrentState(selectedTransition.getToState());
        workflowProvider.get()
                .saveWorkflow(workflow);
        if(!Strings.isNullOrEmpty(selectedTransition.getAction())) {
            actionExecutor.get()
                    .execute(selectedTransition.getAction(), workflow);
        }
        /*
        TODO::
        0. Implement execution listener and then use that to:
        1. Move actions out
        2. Send events
         */
    }
}
