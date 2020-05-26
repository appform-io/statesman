package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.observer.ObservableEventBus;
import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.DataAction;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.*;

/**
 *
 */
@Singleton
@Slf4j
public class StateTransitionEngine {
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<TransitionStore> transitionStore;
    private final ObjectMapper mapper;
    private final DataActionExecutor dataActionExecutor;
    private final ObservableEventBus eventBus;
    private final Provider<ActionExecutor> actionExecutor;

    private final HopeLangEngine hopeLangEngine = HopeLangEngine.builder()
            .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
            .build();

    private final LoadingCache<String, Evaluatable> evalCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build(hopeLangEngine::parse);

    @Inject
    public StateTransitionEngine(
            Provider<WorkflowProvider> workflowProvider,
            Provider<TransitionStore> transitionStore,
            ObjectMapper mapper,
            DataActionExecutor dataActionExecutor,
            ObservableEventBus eventBus,
            Provider<ActionExecutor> actionExecutor) {
        this.workflowProvider = workflowProvider;
        this.transitionStore = transitionStore;
        this.mapper = mapper;
        this.dataActionExecutor = dataActionExecutor;
        this.eventBus = eventBus;
        this.actionExecutor = actionExecutor;
    }

    public AppliedTransitions handle(DataUpdate dataUpdate) {
        return handle(dataUpdate, null);
    }

    public AppliedTransitions handle(DataUpdate dataUpdate, DataAction defaultAction) {
        val transitions = new ArrayList<AppliedTransition>();
        AppliedTransition transition = null;
        Set<String> evaluatedRuleSet = new HashSet<>();
        do {
            transition = handleSingleTransition(dataUpdate, evaluatedRuleSet, defaultAction).orElse(null);
            if (null != transition) {
                transitions.add(transition);
                evaluatedRuleSet.add(transition.getTransitionId());
            }
        } while (null != transition);
        log.debug("workflowId:{},transitions:{}", dataUpdate.getWorkflowId(), transitions);
        return new AppliedTransitions(dataUpdate.getWorkflowId(), transitions);
    }

    private Optional<AppliedTransition> handleSingleTransition(
            DataUpdate dataUpdate,
            Set<String> alreadyVisited,
            DataAction defaultAction) {
        val workflowId = dataUpdate.getWorkflowId();
        log.debug("Existing transitions for {}: {}", workflowId, alreadyVisited);
        val workflow = workflowProvider.get()
                .getWorkflow(workflowId)
                .orElse(null);
        Preconditions.checkNotNull(workflow);
        final DataObject dataObject = workflow.getDataObject();
        val currentState = dataObject.getCurrentState();
        if (currentState.isTerminal()) {
            log.info("Workflow {} is already complete.", workflow.getId());
            return Optional.empty();
        }
        val template = workflowProvider.get()
                .getTemplate(workflow.getTemplateId())
                .orElse(null);
        Preconditions.checkNotNull(template);
        val transitions = transitionStore.get()
                .getTransitionFor(template.getId(), currentState.getName());
        Preconditions.checkNotNull(transitions);
        val evalNode = mapper.createObjectNode();
        evalNode.putObject("data").setAll((ObjectNode) dataObject.getData());
        evalNode.putObject("update").setAll((ObjectNode) dataUpdate.getData());
        val selectedTransition = transitions.stream()
                .filter(stateTransition -> stateTransition.getType().equals(StateTransition.Type.EVALUATED))
                .filter(StateTransition::isActive)
                .filter(stateTransition -> !alreadyVisited.contains(stateTransition.getId()))
                .filter(stateTransition -> hopeLangEngine.evaluate(evalCache.get(stateTransition.getRule()), evalNode))
                .findFirst()
                .orElse(defaultTransition(transitions, alreadyVisited));
        if (null == selectedTransition) {
            log.debug("No matching transition for: {} for update: {}", workflowId, dataUpdate);
            if (null != defaultAction) {
                log.debug("Applying default action of type: {}", defaultAction.getType().name());
                if(alreadyVisited == null || alreadyVisited.isEmpty()) {
                    dataObject.setData(dataActionExecutor.apply(dataObject, dataUpdate));
                    workflowProvider.get().updateWorkflow(workflow);
                }
            }
            return Optional.empty();
        }
        if(alreadyVisited == null || alreadyVisited.isEmpty()) {
            dataObject.setData(dataActionExecutor.apply(dataObject, dataUpdate));
        }
        dataObject.setCurrentState(selectedTransition.getToState());
        val action = applyAction(workflow, dataObject, selectedTransition);
        workflowProvider.get().updateWorkflow(workflow);
        eventBus.publish(new StateTransitionEvent(
                template, workflow, dataUpdate, currentState, action));
        return Optional.of(new AppliedTransition(currentState,
                                                 selectedTransition.getToState(),
                                                 selectedTransition.getId()));
    }

    private String applyAction(Workflow workflow, DataObject dataObject, StateTransition selectedTransition) {
        String workflowId = workflow.getId();
        val action = selectedTransition.getAction();
        if (!Strings.isNullOrEmpty(action)) {
            JsonNode actionResponse = null;
            try {
                actionResponse = actionExecutor.get()
                        .execute(action, workflow)
                        .orElse(null);
            }
            catch (Exception e) {
                log.error("Error executing action " + action + " for wfid: " + workflowId, e);
            }
            if (null == actionResponse || actionResponse.isNull() || actionResponse.isMissingNode() || !actionResponse.isObject()) {
                log.warn("Empty/Non object action response for action {} for workflow {}",
                         action, workflowId);
            }
            else {
                dataObject.setData(dataActionExecutor.apply(
                        dataObject, new DataUpdate(workflowId, actionResponse, new MergeDataAction())));
            }


        }
        return action;
    }

    private StateTransition defaultTransition(
            List<StateTransition> transitions,
            Set<String> alreadyVisited) {
        return transitions.stream()
                .filter(stateTransition -> stateTransition.getType().equals(StateTransition.Type.DEFAULT))
                .filter(StateTransition::isActive)
                .filter(stateTransition -> !alreadyVisited.contains(stateTransition.getId()))
                .findFirst()
                .orElse(null);
    }
}
