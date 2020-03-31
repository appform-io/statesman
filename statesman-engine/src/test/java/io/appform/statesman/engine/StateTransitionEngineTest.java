package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBus;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.dataaction.impl.MergeSelectedDataAction;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 */
public class StateTransitionEngineTest {

    private static final String WFT_ID = "wft1";
    private static final String WF_ID = "wf1";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private interface States {
        State A = new State("A", false);
        State B = new State("B", false);
        State C = new State("C", false);
        State D = new State("D", true);
        State E = new State("E", true);
    }

    private interface StateTransitions {
        StateTransition a2b = new StateTransition("T1",
                                                  StateTransition.Type.EVALUATED,
                                                  States.A.getName(),
                                                  true,
                                                  "\"$.update.Q1\" == 3",
                                                  States.B,
                                                  null);
        StateTransition b2c = new StateTransition("T2",
                                                  StateTransition.Type.EVALUATED,
                                                  States.B.getName(),
                                                  true,
                                                  "\"$.update.Q2\" == 4",
                                                  States.C,
                                                  null);
        StateTransition c2d = new StateTransition("T3",
                                                  StateTransition.Type.EVALUATED,
                                                  States.C.getName(),
                                                  true,
                                                  "\"$.update.Q3\" == 1",
                                                  States.D,
                                                  null);
        StateTransition c2e = new StateTransition("T4",
                                                  StateTransition.Type.EVALUATED,
                                                  States.D.getName(),
                                                  true,
                                                  "\"$.update.Q3\" == 2",
                                                  States.E,
                                                  null);
    }

    private StateTransitionEngine engine;

    @Before
    public void setup() {
        val wft = new WorkflowTemplate(WF_ID, WF_ID, true, Collections.emptyList(), States.A);
        val wf = new Workflow(WF_ID,
                              WFT_ID,
                              new DataObject(MAPPER.createObjectNode(), wft.getStartState(), new Date(), new Date()));
        final Provider<WorkflowProvider> workflowProvider = () -> {
            val provider = mock(WorkflowProvider.class);
            when(provider.getTemplate(anyString()))
                    .thenReturn(Optional.of(wft));
            when(provider.getWorkflow(anyString()))
                    .thenReturn(Optional.of(wf));
            return provider;
        };
        final Provider<TransitionStore> transitionStore = () -> {
            val transitions = ImmutableMap.<String, List<StateTransition>>builder()
                    .put("A", ImmutableList.of(StateTransitions.a2b))
                    .put("B", ImmutableList.of(StateTransitions.b2c))
                    .put("C", ImmutableList.of(StateTransitions.c2d, StateTransitions.c2e))
                    .build();
            return new TransitionStore() {

                @Override
                public List<StateTransition> getAllTransitions(String workflowTemplateId) {
                    return null;
                }

                @Override
                public List<StateTransition> update(String workflowTemplateId, StateTransition stateTransition) {
                    return null;
                }

                @Override
                public Optional<StateTransition> create(String workflowTemplateId, StateTransition stateTransition) {
                    return null;
                }

                @Override
                public List<StateTransition> getTransitionFor(
                        String workflowTmplId, String fromState) {
                    return transitions.get(fromState);
                }
            };
        };
        final ObservableEventBus eventBus = mock(ObservableEventBus.class);
        doNothing().when(eventBus).publish(any(ObservableEvent.class));
        engine = new StateTransitionEngine(workflowProvider,
                                           transitionStore,
                                           MAPPER,
                                           new DataActionExecutor(MAPPER),
                                           eventBus);
    }

    @Test
    @SneakyThrows
    public void handle() {
        var transition = engine.handle(new DataUpdate(
                WF_ID,
                MAPPER.createObjectNode()
                        .put("Q1", 2),
                new MergeSelectedDataAction(Collections.singletonList("Q1"))));
        Assert.assertTrue(transition.getTransitions().isEmpty());
        transition = engine.handle(new DataUpdate(
                WF_ID,
                MAPPER.createObjectNode()
                        .put("Q1", 3),
                new MergeSelectedDataAction(Collections.singletonList("Q1"))));
        Assert.assertFalse(transition.getTransitions().isEmpty());
        Assert.assertEquals(1, transition.getTransitions().size());
        Assert.assertEquals(States.A, transition.getTransitions().get(0).getOldState());
        Assert.assertEquals(States.B, transition.getTransitions().get(0).getNewState());
        transition = engine.handle(new DataUpdate(
                WF_ID,
                MAPPER.createObjectNode()
                        .put("Q2", 4),
                new MergeSelectedDataAction(Collections.singletonList("Q2"))));
        Assert.assertFalse(transition.getTransitions().isEmpty());
        Assert.assertEquals(1, transition.getTransitions().size());
        Assert.assertEquals(States.B, transition.getTransitions().get(0).getOldState());
        Assert.assertEquals(States.C, transition.getTransitions().get(0).getNewState());
        transition = engine.handle(new DataUpdate(
                WF_ID,
                MAPPER.createObjectNode()
                        .put("Q3", 1),
                new MergeSelectedDataAction(Collections.singletonList("Q3"))));
        Assert.assertFalse(transition.getTransitions().isEmpty());
        Assert.assertEquals(1, transition.getTransitions().size());
        Assert.assertEquals(States.C, transition.getTransitions().get(0).getOldState());
        Assert.assertEquals(States.D, transition.getTransitions().get(0).getNewState());
    }

    @Test
    @SneakyThrows
    public void testOneShot() {
        var transition = engine.handle(new DataUpdate(
                WF_ID,
                MAPPER.createObjectNode()
                        .put("Q1", 3)
                        .put("Q2", 4)
                        .put("Q3", 1),
                new MergeDataAction()));
        Assert.assertFalse(transition.getTransitions().isEmpty());
        Assert.assertEquals(3, transition.getTransitions().size());
        Assert.assertEquals(States.A, transition.getTransitions().get(0).getOldState());
        Assert.assertEquals(States.B, transition.getTransitions().get(0).getNewState());
        Assert.assertEquals(States.B, transition.getTransitions().get(1).getOldState());
        Assert.assertEquals(States.C, transition.getTransitions().get(1).getNewState());
        Assert.assertEquals(States.C, transition.getTransitions().get(2).getOldState());
        Assert.assertEquals(States.D, transition.getTransitions().get(2).getNewState());
    }
}