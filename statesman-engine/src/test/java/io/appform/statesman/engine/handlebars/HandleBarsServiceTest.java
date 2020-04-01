package io.appform.statesman.engine.handlebars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class HandleBarsServiceTest {

    @Test
    @SneakyThrows
    public void transform() {
        HandleBarsService handleBarsService = new HandleBarsService();
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
        HandleBarsService handleBarsService = new HandleBarsService();
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
        HandleBarsService handleBarsService = new HandleBarsService();
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
        HandleBarsService handleBarsService = new HandleBarsService();
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add("192")
                                             .add("2"))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(4, jsonNode.size());
        Assert.assertEquals("EN", jsonNode.get(0).asText());
        Assert.assertEquals("HI", jsonNode.get(1).asText()); //( gets replaced by last value
        Assert.assertEquals("KA", jsonNode.get(2).asText());
        Assert.assertEquals("KA", jsonNode.get(3).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayIntConversion() {
        HandleBarsService handleBarsService = new HandleBarsService();
        final String template = "{\"language\" : {{{ map_lookup_arr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                handleBarsService.transform(
                        JsonNodeValueResolver.INSTANCE,
                        template,
                        objectMapper.createObjectNode()
                                .set("payload[question1]",
                                     objectMapper.createArrayNode()
                                             .add(192)
                                             .add(2))))
                .get("language");
        Assert.assertTrue(jsonNode.isArray());
        Assert.assertEquals(4, jsonNode.size());
        Assert.assertEquals("EN", jsonNode.get(0).asText());
        Assert.assertEquals("HI", jsonNode.get(1).asText()); //( gets replaced by last value
        Assert.assertEquals("KA", jsonNode.get(2).asText());
        Assert.assertEquals("KA", jsonNode.get(3).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayInvalidTextValueToDefault() {
        HandleBarsService handleBarsService = new HandleBarsService();
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
        Assert.assertEquals(4, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
        Assert.assertEquals("EN", jsonNode.get(1).asText());
        Assert.assertEquals("KA", jsonNode.get(2).asText());
        Assert.assertEquals("KA", jsonNode.get(3).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayInvalidTypeToDefault() {
        HandleBarsService handleBarsService = new HandleBarsService();
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
        Assert.assertEquals(4, jsonNode.size());
        Assert.assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
        Assert.assertEquals("HI", jsonNode.get(1).asText());
        Assert.assertEquals("HI", jsonNode.get(2).asText());
        Assert.assertEquals("HI", jsonNode.get(3).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayMissingNodeToDefault() {
        HandleBarsService handleBarsService = new HandleBarsService();
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
        HandleBarsService handleBarsService = new HandleBarsService();
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
        HandleBarsService handleBarsService = new HandleBarsService();
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
        HandleBarsService handleBarsService = new HandleBarsService();
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
}