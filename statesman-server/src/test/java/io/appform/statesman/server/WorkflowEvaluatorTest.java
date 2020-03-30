package io.appform.statesman.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.appform.statesman.model.State;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.server.dao.workflow.WorkflowProviderCommand;
import io.appform.statesman.server.evaluator.WorkflowTemplateSelector;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Provider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkflowEvaluatorTest {

    private static final String WFT_ID1 = "wft1";
    private static final String WFT_ID2 = "wft2";
    private WorkflowTemplateSelector workflowEvaluator;
    private ObjectMapper mapper;

    @Before
    public void setup() {
        val wft1 = new WorkflowTemplate(WFT_ID1,
                                        WFT_ID1,
                                        true,
                                        ImmutableList.of("\"$.lang\" == \"KA\"",
                                                         "\"$.lang\" == \"KA\" && \"$.covidTest\" == \"true\" "),
                                        new State("A", false));
        val wft2 = new WorkflowTemplate(WFT_ID2,
                                        WFT_ID2,
                                        true,
                                        ImmutableList.of("\"$.lang\" == \"MH\"",
                                                         "\"$.lang\" == \"MH\" && \"$.ageGreaterThanSixty\" == \"true\" "),
                                        new State("B", false));

        final Provider<WorkflowProviderCommand> workflowProviderCommandProvider = () -> {
            val provider = mock(WorkflowProviderCommand.class);
            when(provider.getAll())
                    .thenReturn(ImmutableList.of(wft1, wft2));
            return provider;
        };

        workflowEvaluator = new WorkflowTemplateSelector(workflowProviderCommandProvider.get());
        mapper = new ObjectMapper();
    }

    @Test
    public void testWorkflowEvaluation() {
        final WorkflowTemplate t1 = workflowEvaluator.determineTemplate(
                mapper.createObjectNode()
                        .put("lang", "KA")).orElse(null);
        Assert.assertNotNull(t1);
        Assert.assertEquals(WFT_ID1, t1.getId());
        final WorkflowTemplate t2 = workflowEvaluator.determineTemplate(
                mapper.createObjectNode()
                        .put("lang", "MH")).orElse(null);
        Assert.assertNotNull(t2);
        Assert.assertEquals(WFT_ID2, t2.getId());
    }

}
