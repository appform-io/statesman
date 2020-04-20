package io.appform.statesman.engine.handlebars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
public class HandleBarsServiceTest {

    private final HandleBarsService handleBarsService = new HandleBarsService();

    @Test
    @SneakyThrows
    public void transform() {
        final String template = "{\"language\" : {{{ map_lookup op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]/0'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals("KA",
                            objectMapper.readTree(
                                    handleBarsService.transform(
                                            JsonNodeValueResolver.INSTANCE,
                                            template,
                                            objectMapper.createObjectNode()
                                                    .set("payload[question1]",
                                                         objectMapper.createArrayNode().add("2"))))
                                    .get("language")
                                    .asText());
    }

    @Test
    @SneakyThrows
    public void transformBool() {
        final String template = "{\"language\" : {{{ map_lookup op_1=true op_2=false pointer='/payload[question2]/0'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode language = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question2]",
                                     objectMapper.createArrayNode().add("1"))))
                .get("language");
        Assert.assertTrue(language.asBoolean());
    }

    @Test
    @SneakyThrows
    public void transformArray() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add("1")
                                             .add("2"))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(2, jsonNode.size());
        Assert.assertEquals("EN", jsonNode.get(0).asText());
        Assert.assertEquals("KA", jsonNode.get(1).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayClubbed() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add("19")
                                             .add("2"))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(3, jsonNode.size());
        Assert.assertEquals("EN", jsonNode.get(0).asText());
        Assert.assertEquals("HI", jsonNode.get(1).asText()); //( gets replaced by last value
        Assert.assertEquals("KA", jsonNode.get(2).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayIntConversion() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add(19)
                                             .add(2))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(3, jsonNode.size());
        Assert.assertEquals("EN", jsonNode.get(0).asText());
        Assert.assertEquals("HI", jsonNode.get(1).asText()); //( gets replaced by last value
        Assert.assertEquals("KA", jsonNode.get(2).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayInvalidTextValueToDefault() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add("a")
                                             .add("122"))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(3, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
        Assert.assertEquals("EN", jsonNode.get(1).asText());
        Assert.assertEquals("KA", jsonNode.get(2).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayInvalidTypeToDefault() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add(true)
                                             .add(false)
                                             .add(true)
                                             .add(true))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(1, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
    }

    @Test
    @SneakyThrows
    public void transformArrayMissingNodeToDefault() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(1, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
    }

    @Test
    @SneakyThrows
    public void transformArrayEmptyKeyToDefault() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer=''}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(1, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
    }

    @Test
    @SneakyThrows
    public void transformArraySingleElement() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .put("payload[question1]", "2")))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(1, jsonNode.size());
        Assert.assertEquals("KA", jsonNode.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void transformArraySingleElementDefaultSelect() {
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode().put("payload[question1]", "")))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(1, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void translate() {
        final String template = "{\"language\" : {{{ translate op_telegu='TG' op_tamil='TM' op_hindi='HI' op_english='EN' pointer='/ticket.cf_language'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals("HI",
                            objectMapper.readTree(
                                    handleBarsService.transform(
                                            JsonNodeValueResolver.INSTANCE,
                                            template,
                                            objectMapper.createObjectNode()
                                                    .put("ticket.cf_language", "Hindi")))
                                    .get("language")
                                    .asText());
    }

    @Test
    @SneakyThrows
    public void translateBool() {
        final String template = "{\"language\" : {{{ translate op_true='Resolved' op_false='Closed' pointer='/ticket.cf_resolved'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals("Resolved",
                            objectMapper.readTree(
                                    handleBarsService.transform(
                                            JsonNodeValueResolver.INSTANCE,
                                            template,
                                            objectMapper.createObjectNode()
                                                    .put("ticket.cf_resolved", true)))
                                    .get("language")
                                    .asText());
    }

    @Test
    @SneakyThrows
    public void translateInt() {
        final String template = "{\"language\" : {{{ translate op_1='Resolved' op_2='Closed' pointer='/ticket.cf_resolved'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals("Resolved",
                            objectMapper.readTree(
                                    handleBarsService.transform(
                                            JsonNodeValueResolver.INSTANCE,
                                            template,
                                            objectMapper.createObjectNode()
                                                    .put("ticket.cf_resolved", 1)))
                                    .get("language")
                                    .asText());
    }

    @Test
    @SneakyThrows
    public void translateMissing() {
        final String template = "{\"language\" : {{{ translate op_1='Resolved' op_2='Closed' pointer='/ticket.cf_resolved'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals("null", objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .put("ticket.cf_resolved", 9)))
                .get("language")
                .asText());
    }

    @Test
    @SneakyThrows
    public void translateArray() {
        final String template = "{\"language\" : {{{ translate_arr op_telegu='TG' op_tamil='TM' op_hindi='HI' op_english='EN' pointer='/ticket.cf_language'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode arr = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("ticket.cf_language",
                                     objectMapper.createArrayNode()
                                             .add("Telegu")
                                             .add("Hindi"))))
                .get("language");
        Assert.assertTrue(arr.isArray());
        Assert.assertEquals(2, arr.size());
        Assert.assertEquals("TG", arr.get(0).asText());
        Assert.assertEquals("HI", arr.get(1).asText());
    }

    @Test
    @SneakyThrows
    public void translateArraySingleElement() {
        final String template = "{\"language\" : {{{ translate_arr op_telegu='TG' op_tamil='TM' op_hindi='HI' op_english='EN' pointer='/ticket.cf_language'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode arr = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .put("ticket.cf_language", "Telegu")))
                .get("language");
        Assert.assertTrue(arr.isArray());
        Assert.assertEquals(1, arr.size());
        Assert.assertEquals("TG", arr.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void translateToHTML() {
        final String template = "<b>Age:</b> {{{translate_txt op_true='Above Sixty' op_false='Below Sixty' pointer='/ageAboveSixty'}}}<br> <b>Travel Status:</b> {{{translate_txt op_true='Travelled abroad' op_false='Has not travelled abroad' pointer='/travelled'}}}<br><b>Exposure:</b> {{{translate_txt op_true='Has had contact with COVID-19 patient' op_false='No contact' pointer='/contact'}}}<br> <b>Pre existing conditions:</b> {{{translate_txt op_heart='Heart problems, Asthma or any other Lung problems' op_cancer='Cancer or on chemotherapy or other low immunity problems' op_diabetes='Diabetes or Kidney problems' op_pregnant='Pregnant at present or recently delivered a baby' op_none='No Prexistting Conditions' pointer='/existingDiseases'}}}<br> <b>Symptoms:</b> {{{translate_arr_txt op_fever='Fever' op_cough='Dry Cough' op_throatpain='Throat Pain' op_wheezing='Wheezing' op_others='Others' op_none='none' pointer='/symptoms'}}}";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals(
                "<b>Age:</b> Above Sixty<br> <b>Travel Status:</b> Travelled abroad<br><b>Exposure:</b> Has had contact with COVID-19 patient<br> <b>Pre existing conditions:</b> Diabetes or Kidney problems<br> <b>Symptoms:</b> Dry Cough, Throat Pain, Wheezing",
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.readTree("{\n" +
                                                      "   \"ageAboveSixty\":true,\n" +
                                                      "   \"travelled\":true,\n" +
                                                      "   \"contact\":true,\n" +
                                                      "   \"existingDiseases\":\"diabetes\",\n" +
                                                      "   \"symptoms\":[\n" +
                                                      "      \"cough\",\n" +
                                                      "      \"throatpain\",\n" +
                                                      "      \"wheezing\"\n" +
                                                      "   ]\n" +
                                                      "}")));
    }
    @Test
    @SneakyThrows
    public void translateToHTMLNull() {
        final String template = "<b>Age:</b> Above Sixty<br> <b>Travel Status:</b> <br><b>Exposure:</b> Has had contact with COVID-19 patient<br> <b>Pre existing conditions:</b> Diabetes or Kidney problems<br> <b>Symptoms:</b> Dry Cough, Throat Pain, Wheezing";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        Assert.assertEquals(
                "<b>Age:</b> Above Sixty<br> <b>Travel Status:</b> <br><b>Exposure:</b> Has had contact with COVID-19 patient<br> <b>Pre existing conditions:</b> Diabetes or Kidney problems<br> <b>Symptoms:</b> Dry Cough, Throat Pain, Wheezing",
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.readTree("{\n" +
                                                      "   \"ageAboveSixty\":true,\n" +
                                                      "   \"travelled\":93,\n" +
                                                      "   \"contact\":true,\n" +
                                                      "   \"existingDiseases\":\"diabetes\",\n" +
                                                      "   \"symptoms\":[\n" +
                                                      "      \"cough\",\n" +
                                                      "      \"throatpain\",\n" +
                                                      "      \"blah\",\n" +
                                                      "      \"wheezing\"\n" +
                                                      "   ]\n" +
                                                      "}")));
    }

    @Test
    @SneakyThrows
    public void translateSpaceWords() {
        final String template = "{\"language\" : {{{ translate_arr op_hello_world='HW' op_hello_mars='HM' pointer='/ticket.cf_language'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode arr = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .put("ticket.cf_language", "Hello World")))
                .get("language");
        Assert.assertTrue(arr.isArray());
        Assert.assertEquals(1, arr.size());
        Assert.assertEquals("HW", arr.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void phoneTest() {
        final String template = "{{phone value}}";
        Assert.assertEquals("1234567890", handleBarsService.transform(
                                    template, Collections.singletonMap("value", "1234567890sdasa%^%$$$^$ %$ _")));
        Assert.assertEquals("1234567890",
                            handleBarsService.transform(template, Collections.singletonMap("value", "+91 1234567890")));
        Assert.assertEquals("1234567890",
                            handleBarsService.transform(template, Collections.singletonMap("value", "01234567890")));
        Assert.assertEquals("1234567890",
                            handleBarsService.transform(template, Collections.singletonMap("value", "+91-1234567890")));
        Assert.assertEquals("1234567890",
                            handleBarsService.transform(template, Collections.singletonMap("value", "1234567890")));
        Assert.assertEquals("",
                            handleBarsService.transform(template, Collections.emptyMap()));
        Assert.assertEquals("",
                            handleBarsService.transform(template, Collections.singletonMap("value", "1234")));
        Assert.assertEquals("", handleBarsService.transform(
                                    template, Collections.singletonMap("value", "sasdasd asdqqweq weq we")));

    }

    @Test
    public void testNormalize() {
        val hb = new HandleBarsService();
        val node = Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "Dr. Jeykll & Mr. Hyde")
                .put("state", "punjab");
        Assert.assertEquals("dr_jeykll_mr_hyde", hb.transform("{{normalize name}}", node));
        Assert.assertEquals("DR_JEYKLL_MR_HYDE", hb.transform("{{normalize_upper name}}", node));
        Assert.assertEquals("Punjab", hb.transform("{{normalize_init_cap state}}", node));
    }

    @Test
    public void testElapsedTime() {
        val hb = new HandleBarsService();
        val currDate = new Date();
        Assert.assertTrue(currDate.getTime() <= Long.parseLong(
                Objects.requireNonNull(hb.transform("{{currTime}}",
                                                    Jackson.newObjectMapper().createObjectNode()))));
    }


    @Test
    @SneakyThrows
    public void testElapsedTime1() {
        val hb = new HandleBarsService();
        val currDate = new Date();
        val node = Jackson.newObjectMapper()
                .readTree("{\n" +
                                  "   \"StartTime\" : [\n" +
                                  "      \"2020-04-12 23:25:15\"\n" +
                                  "   ],\n" +
                                  "   \"CurrentTime\" : [\n" +
                                  "      \"2020-04-12 23:25:59\"\n" +
                                  "   ]\n" +
                                  "}\n");
        final String out = hb.transform("{{elapsedTime \"YYYY-MM-dd HH:mm:ss\" StartTime/0 CurrentTime/0}}", node);
        Assert.assertEquals("44000", out);
    }

    @Test
    @SneakyThrows
    public void testEmpty() {
        val hb = new HandleBarsService();
        val currDate = new Date();
        final ObjectMapper mapper = Jackson.newObjectMapper();
        val node = mapper
                .createObjectNode();
        node.set("arr", mapper.createArrayNode());
        node.set("nodata", mapper.createArrayNode().add(""));
        node.set("data", mapper.createArrayNode().add("1").add("2"));

        Assert.assertEquals("true", hb.transform("{{empty arr}}", node));
        Assert.assertEquals("false", hb.transform("{{notEmpty arr}}", node));

        Assert.assertEquals("true", hb.transform("{{empty nodata}}", node));
        Assert.assertEquals("false", hb.transform("{{notEmpty nodata}}", node));

        Assert.assertEquals("false", hb.transform("{{empty data}}", node));
        Assert.assertEquals("true", hb.transform("{{notEmpty data}}", node));
    }


    @Test
    @SneakyThrows
    public void testParseToInt() {
        val hb = new HandleBarsService();
        val currDate = new Date();
        final ObjectMapper mapper = Jackson.newObjectMapper();


        Assert.assertEquals("1", hb.transform("{{toInt value}}", mapper.createObjectNode().put("value", "1")));
        Assert.assertEquals("-1", hb.transform("{{toInt value}}", mapper.createObjectNode().put("value", "")));
        Assert.assertEquals("-1", hb.transform("{{toInt value}}", mapper.createObjectNode().put("value", "abc")));
        Assert.assertEquals("-1", hb.transform("{{toInt value}}", mapper.createObjectNode()));

    }

}