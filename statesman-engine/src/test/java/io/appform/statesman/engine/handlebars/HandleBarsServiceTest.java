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
        Assert.assertEquals(true,
                            objectMapper.readTree(
                                    handleBarsService.transform(
                                            JsonNodeValueResolver.INSTANCE,
                                            template,
                                            objectMapper.createObjectNode()
                                                    .set("payload[question2]",
                                                         objectMapper.createArrayNode().add("1"))))
                                    .get("language")
                                    .asBoolean());
    }

    @Test
    @SneakyThrows
    public void transformArray() {
        HandleBarsService handleBarsService = new HandleBarsService();
        final String template = "{\"language\" : {{{ map_lookup op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }";
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
        Assert.assertEquals("EN", jsonNode.get(0).asText());
        Assert.assertEquals("KA", jsonNode.get(1).asText());
    }
}