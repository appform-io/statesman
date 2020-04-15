package io.appform.statesman.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.ingress.IngressHandler;
import io.appform.statesman.server.requests.IngressCallback;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

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

    @Test
    @SneakyThrows
    public void testKalKaTemplate() {
        val hb = new HandleBarsService();
        val mapper = Jackson.newObjectMapper();
        val callback = new IngressCallback("kaleyra", "?state=KARNATAKA&id=4228889695e9445572f70e&callFrom=9986032019&called=8068171046&Question1=3&Question2=2&Question3=2&Question4=12&calltime=13/04/2020%2004:27%20PM&duration=37", "/ivrhandler/kaleyra", mapper.createObjectNode());
        final MultivaluedMap<String, String> parsedParams = IngressHandler.parseQueryParams(callback);
        final JsonNode parsedNode = mapper.valueToTree(parsedParams);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedNode));
        val template = "{ \"type\" : \"kaleyra\",\"state\" : \"karnataka\",\"phone\" : \"{{callFrom/0}}\", \"language\": {{{ map_lookup op_1='kannada' op_2='hindi' op_3='english' pointer='/Question1/0'}}}, \"covidTest\": {{{ map_lookup op_1=false op_2=true op_3=false pointer='/Question2/0'}}}, \"contact\": {{{ map_lookup op_1=true op_2=false pointer='/Question3/0'}}}, \"symptoms\": {{{ map_lookup_arr op_1='fever' op_2='cough' op_3='throatpain' op_4='wheezing' op_5='others' op_6='none' pointer='/Question4/0'}}}, \"callStartTime\": {{toEpochTime calltime/0 'dd/MM/YYYY hh:mm a' 'IST'}}, \"callDuration\": {{duration/0}}000, \"callDropped\": false, \"allDataCollected\": true }";
        System.out.println(mapper.valueToTree(new OneShotTransformationTemplate("kaleyra_karnatak", "", TranslationTemplateType.INGRESS, template)));
    }
}
