package io.appform.statesman.server.callbacktransformation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.model.StateTransition;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class CallbackTransformationTemplatesTest {

    @Test
    @SneakyThrows
    public void testMultiStep() {
        List<StepByStepTransformationTemplate.StepSelection> steps = new ArrayList<>();
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"1\"", "{\n" +
                "              \"type\": \"exotel\",\n" +
                "              \"phone\" : \"{{From/0}}\",\n" +
                "              \"state\": \"Punjab\" \n" +
                "              \"language\": {{{ map_lookup op_1='PUN' op_2='HI' op_3='EN' pointer='/digits'}}}\n" +
                "            }"));
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"2\"", "{\n" +
                "              \"covidTest\": {{{ map_lookup op_1=true op_2=false pointer='/digits'}}}\n" +
                "            }"));
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"3\"", "{\n" +
                "              \"ageGreaterThanSixty\": \"{{ map_lookup op_1=true op_2=false pointer='/digits'}}}\n" +
                "            }"));
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"4\"", "{\n" +
                "              \"travelled\": {{{ map_lookup op_1=true op_2=false pointer='/digits'}}}\n" +
                "            }"));
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"5\"", "{\n" +
                "              \"contact\": {{{ map_lookup op_1=true op_2=false pointer='/digits'}}}\n" +
                "            }"));
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"6\"", "{\n" +
                "              \"existingDiseases\": {{{ map_lookup op_1='heart' op_2='diabetes' op_3='none' pointer='/digits'}}}\n" +
                "            }"));
        steps.add(new StepByStepTransformationTemplate.StepSelection("\"$.Question[0]\" == \"7\"", "{\n" +
                "              \"symptoms\": {{{ map_lookup_arr op_1='fever' op_2='cough' op_3='throatpain' op_4='wheezing' op_5='others' op_6='none' pointer='/digits'}}}\n" +
                "            }"));

                StepByStepTransformationTemplate template = new StepByStepTransformationTemplate("exotel", "/CallSid/0", "", TranslationTemplateType.INGRESS, steps, "");
        final ObjectMapper mapper = Jackson.newObjectMapper();
        val engine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        final JsonNode providedData = mapper.readTree("{\n" +
                                                          "   \"CallSid\" : [\n" +
                                                          "      \"0550b49a9fe2b8455459e2c500c91443\"\n" +
                                                          "   ],\n" +
                                                          "   \"CallType\" : [\n" +
                                                          "      \"call-attempt\"\n" +
                                                          "   ],\n" +
                                                          "   \"?auth\" : [\n" +
                                                          "      \"04bc99956d714d91a47edc7eced06843\"\n" +
                                                          "   ],\n" +
                                                          "   \"From\" : [\n" +
                                                          "      \"09986032019\"\n" +
                                                          "   ],\n" +
                                                          "   \"tenant_id\" : [\n" +
                                                          "      \"120235\"\n" +
                                                          "   ],\n" +
                                                          "   \"ProcessStatus\" : [\n" +
                                                          "      \"null\"\n" +
                                                          "   ],\n" +
                                                          "   \"Question\" : [\n" +
                                                          "      \"1\"\n" +
                                                          "   ],\n" +
                                                          "   \"Created\" : [\n" +
                                                          "      \"Fri, 03 Apr 2020 21:55:41\"\n" +
                                                          "   ],\n" +
                                                          "   \"RecordingUrl\" : [\n" +
                                                          "      \"null\"\n" +
                                                          "   ],\n" +
                                                          "   \"CallFrom\" : [\n" +
                                                          "      \"09986032019\"\n" +
                                                          "   ],\n" +
                                                          "   \"digits\" : [\n" +
                                                          "      \"\\\"3\\\"\"\n" +
                                                          "   ],\n" +
                                                          "   \"EndTime\" : [\n" +
                                                          "      \"1970-01-01 05:30:00\"\n" +
                                                          "   ],\n" +
                                                          "   \"DialWhomNumber\" : [\n" +
                                                          "      \"\"\n" +
                                                          "   ],\n" +
                                                          "   \"DialCallStatus\" : [\n" +
                                                          "      \"null\"\n" +
                                                          "   ],\n" +
                                                          "   \"DialCallDuration\" : [\n" +
                                                          "      \"0\"\n" +
                                                          "   ],\n" +
                                                          "   \"StartTime\" : [\n" +
                                                          "      \"2020-04-03 21:55:41\"\n" +
                                                          "   ],\n" +
                                                          "   \"Direction\" : [\n" +
                                                          "      \"incoming\"\n" +
                                                          "   ],\n" +
                                                          "   \"ForwardedFrom\" : [\n" +
                                                          "      \"null\"\n" +
                                                          "   ],\n" +
                                                          "   \"To\" : [\n" +
                                                          "      \"08047192290\"\n" +
                                                          "   ],\n" +
                                                          "   \"CurrentTime\" : [\n" +
                                                          "      \"2020-04-03 21:56:14\"\n" +
                                                          "   ],\n" +
                                                          "   \"CallTo\" : [\n" +
                                                          "      \"08047192290\"\n" +
                                                          "   ],\n" +
                                                          "   \"state\" : [\n" +
                                                          "      \"MAHARASHTRA\"\n" +
                                                          "   ],\n" +
                                                          "   \"flow_id\" : [\n" +
                                                          "      \"282123\"\n" +
                                                          "   ]\n" +
                                                          "}\n");
        val selectedStep = template.getTemplates()
                .stream()
                .filter(step -> engine.evaluate(step.getSelectionRule(), providedData))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(selectedStep);
    }

    @Test
    @SneakyThrows
    public void matchTest() {
        val mapper = Jackson.newObjectMapper();
        List<StateTransition> transitions = mapper.readValue("[{\"id\":\"PN_DATA_COLLECTED\",\"type\":\"EVALUATED\",\"fromState\":\"START\",\"active\":true,\"rule\":\"path.exists(\\\"$.update.covidTest\\\") == true && path.exists(\\\"$.update.symptoms\\\") == true && path.exists(\\\"$.update.existingDiseases\\\") == true && path.exists(\\\"$.update.contact\\\") == true && path.exists(\\\"$.update.travelled\\\") == true && path.exists(\\\"$.update.ageGreaterThanSixty\\\") == true && path.exists(\\\"$.update.language\\\") == true && \\\"$.update.callDropped\\\" == false\",\"toState\":{\"name\":\"DATA_COLLECTED\",\"terminal\":false}},{\"id\":\"PN_CALL_DROPPED\",\"type\":\"EVALUATED\",\"fromState\":\"START\",\"active\":true,\"rule\":\"\\\"$.update.callDropped\\\" == true\",\"toState\":{\"name\":\"CALL_DROPPED\",\"terminal\":true},\"action\":\"SMS_SOCIAL_DISTANCE\"},{\"id\":\"PN_DATA_STEP_COLLECTED\",\"type\":\"EVALUATED\",\"fromState\":\"START\",\"active\":true,\"rule\":\"path.exists(\\\"$.data.covidTest\\\") == true && path.exists(\\\"$.data.symptoms\\\") == true && path.exists(\\\"$.data.existingDiseases\\\") == true && path.exists(\\\"$.data.contact\\\") == true && path.exists(\\\"$.data.travelled\\\") == true && path.exists(\\\"$.data.ageGreaterThanSixty\\\") == true && path.exists(\\\"$.data.language\\\") == true && \\\"$.data.callDropped\\\" == false\",\"toState\":{\"name\":\"DATA_COLLECTED\",\"terminal\":false}}]", new TypeReference<List<StateTransition>>() {});
        val node = mapper.readTree("{\n" +
                                           "    \"data\": {\n" +
                                           "      \"type\": \"exotel\",\n" +
                                           "      \"phone\": \"09986032019\",\n" +
                                           "      \"state\": \"Punjab\",\n" +
                                           "      \"language\": \"EN\",\n" +
                                           "      \"callDropped\": false,\n" +
                                           "      \"covidTest\": false,\n" +
                                           "      \"ageGreaterThanSixty\": false,\n" +
                                           "      \"travelled\": false,\n" +
                                           "      \"contact\": false,\n" +
                                           "      \"existingDiseases\": \"none\",\n" +
                                           "      \"symptoms\": [\n" +
                                           "        \"fever\",\n" +
                                           "        \"cough\"\n" +
                                           "      ]\n" +
                                           "    }\n" +
                                           "}");
        final HopeLangEngine engine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        LoadingCache<String, Evaluatable> evalCache = Caffeine.newBuilder().build(engine::parse);
        final Optional<StateTransition> match = transitions.stream()
                .filter(stateTransition -> stateTransition.getType().equals(StateTransition.Type.EVALUATED))
                .filter(StateTransition::isActive)
                .filter(stateTransition -> engine.evaluate(evalCache.get(stateTransition.getRule()), node))
                .findFirst();
        Assert.assertTrue(match.isPresent());
    }
}