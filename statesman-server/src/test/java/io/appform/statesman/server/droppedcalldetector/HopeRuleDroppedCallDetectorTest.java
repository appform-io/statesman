package io.appform.statesman.server.droppedcalldetector;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class HopeRuleDroppedCallDetectorTest {
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    @Test
    @SneakyThrows
    public void testDroppedCall() {
        val node = MAPPER.readTree("{\n" +
                                           "  \"type\": \"kaleyra\",\n" +
                                           "  \"wfSource\": \"ivr\",\n" +
                                           "  \"state\": \"madhyapradesh\",\n" +
                                           "  \"status\": \"1\",\n" +
                                           "  \"Q1\": -1,\n" +
                                           "  \"Q2\": 2,\n" +
                                           "  \"Q3\": -1,\n" +
                                           "  \"Q4\": -1,\n" +
                                           "  \"callStartTime\": 1577604660000,\n" +
                                           "  \"callDropped\": false,\n" +
                                           "  \"providerCallbackId\": \"13630329385ea92d2cb1e2\",\n" +
                                           "  \"callDuration\": 28000,\n" +
                                           "  \"allDataCollected\": true\n" +
                                           "}");
        val detector = new HopeRuleDroppedCallDetector();
        Assert.assertTrue(detector.detectDroppedCall(
                OneShotTransformationTemplate.builder()
                        .dropDetectionRule("str.match(\"^$\", \"$.status\") == false")
                        .build(),
                node));
        Assert.assertFalse(detector.detectDroppedCall(
                OneShotTransformationTemplate.builder()
                        .dropDetectionRule("\"$.Q2\" < 0")
                        .build(),
                node));
    }

    @Test
    @SneakyThrows
    public void testDroppedCallComplex() {
        val detector = new HopeRuleDroppedCallDetector();

        val template = OneShotTransformationTemplate.builder()
                .dropDetectionRule("\"$.Q2\" != 2 && \"$.Q4\" < 0 && \"$.status\" != \"answer\"")
                .build();

        //Q2 is set
        Assert.assertFalse(detector.detectDroppedCall(
                template,
                MAPPER.readTree("{\n" +
                                        "  \"Q1\": 2,\n" +
                                        "  \"Q2\": 2,\n" +
                                        "  \"Q3\": -1,\n" +
                                        "  \"Q4\": -1,\n" +
                                        "  \"status\": \"\"\n" +
                                        "}")));

        //Nothing is set
        Assert.assertTrue(detector.detectDroppedCall(
                template,
                MAPPER.readTree("{\n" +
                                        "  \"Q1\": -1,\n" +
                                        "  \"Q2\": -1,\n" +
                                        "  \"Q3\": -1,\n" +
                                        "  \"Q4\": -1,\n" +
                                        "  \"status\": \"\"\n" +
                                        "}")));
        //1 is selected but not answered
        Assert.assertTrue(detector.detectDroppedCall(
                template,
                MAPPER.readTree("{\n" +
                                        "  \"Q1\": 3,\n" +
                                        "  \"Q2\": 1,\n" +
                                        "  \"Q3\": -1,\n" +
                                        "  \"Q4\": -1,\n" +
                                        "  \"status\": \"\"\n" +
                                        "}")));

        //1 is selected but answered
        Assert.assertFalse(detector.detectDroppedCall(
                template,
                MAPPER.readTree("{\n" +
                                        "  \"Q1\": 3,\n" +
                                        "  \"Q2\": 1,\n" +
                                        "  \"Q3\": -1,\n" +
                                        "  \"Q4\": -1,\n" +
                                        "  \"status\": \"answer\"\n" +
                                        "}")));
    }
}