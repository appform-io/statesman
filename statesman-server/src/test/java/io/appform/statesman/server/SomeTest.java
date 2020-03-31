package io.appform.statesman.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.dropwizard.jackson.Jackson;
import lombok.val;
import org.junit.Test;

/**
 *
 */
public class SomeTest {
    @Test
    public void test() throws Exception {
        String yaml = "" +
                "type: ONE_SHOT\n" +
                "template: >-\n" +
                "  {\n" +
                "   \"type\" : \"ozonetel_punjab\",\n" +
                "   \"phone\" : \"{{number}}\",\n" +
                "   \"language\": {{{ map_lookup op_1='MH' op_2='HI' op_3='EN' pointer='/payload[question1]/0'}}},\n" +
                "   \"covidTest\": {{{ map_lookup op_1=true op_2=false pointer='/payload[question2]/0'}}},\n" +
                "   \"ageGreaterThanSixty\": {{{ map_lookup op_1=true op_2=false pointer='/payload[question3]/0'}}},\n" +
                "   \"travelled\": {{{ map_lookup op_1=true op_2=false pointer='/payload[question4]/0'}}},\n" +
                "   \"contact\": {{{ map_lookup op_1=true op_2=false pointer='/payload[question5]/0'}}},\n" +
                "   \"existingDiseases\": {{{ map_lookup op_1='heart' op_2='cancer' op_3='diabetes' op_4='pregnancy' op_5='none' pointer='/payload[question6]/0'}}},\n" +
                "   \"symptoms\": {{{ map_lookup op_1='vomit' op_2='fever' op_3='wheezing' op_4='headache' op_5='none' pointer='/payload[question7]/0'}}}\n" +
                "  }";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        val config = mapper.readValue(yaml, OneShotTransformationTemplate.class);
        System.out.println(Jackson.newObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config));
    }
}
