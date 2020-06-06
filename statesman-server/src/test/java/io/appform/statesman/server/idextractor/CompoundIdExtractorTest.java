package io.appform.statesman.server.idextractor;

import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.FoxtrotClientConfig;
import io.appform.statesman.model.HttpClientConfiguration;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 *
 */
@Slf4j
public class CompoundIdExtractorTest {
    private final HandleBarsService handleBarsService = new HandleBarsService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public WireMockRule foxtrotServer = new WireMockRule(WireMockConfiguration.wireMockConfig().port(8888));

    @Test
    public void extractId() {
        foxtrotServer.stubFor(post(urlEqualTo("/foxtrot/v1/fql"))
                             .withRequestBody(equalTo("select groupingKey from statesman " +
                                                              "where eventData.data.phone='1111111111' " +
                                                              "and eventData.newState='CALL_NEEDED' limit 1"))
                             .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-type", "application/json")
                                        .withBody("{\"opcode\":\"query\",\"headers\":[{\"name\":\"groupingKey\",\"maxLength\":38}],\"rows\":[{\"groupingKey\":\"f50b888c-1ebb-4b3f-9a67-20ceedb57c38\"}]}"))
                             );
        foxtrotServer.stubFor(post(urlEqualTo("/foxtrot/v1/fql"))
                                      .withRequestBody(equalTo("select groupingKey from statesman " +
                                                                       "where eventData.data.phone='0000000000' " +
                                                                       "and eventData.newState='CALL_NEEDED' limit 1"))
                                      .willReturn(aResponse()
                                                          .withStatus(204)));
        val idExtractor = new CompoundIdExtractor(
                new JsonPathIdExtractor(),
                new FqlIdExtractor(
                        () -> handleBarsService,
                        new FoxtrotClientConfig(
                                "http://localhost:8888",
                                ""),
                        () -> new HttpClient(mapper,
                                             HttpUtil.defaultClient("test",
                                                                    SharedMetricRegistries.getOrCreate("test"),
                                                                    new HttpClientConfiguration())),
                        mapper
                ));
//        Assert.assertTrue(idExtractor.extractId(null, null).isPresent());
        val oneShotTemplate = new OneShotTransformationTemplate(
                "test",
                "/id/0",
                "select groupingKey from statesman where eventData.data.phone='{{phone/0}}' and eventData.newState='CALL_NEEDED' limit 1",
                TranslationTemplateType.INGRESS,
                "",
                "");
        Assert.assertTrue(idExtractor.extractId(oneShotTemplate, null).isPresent());
        final ObjectNode root1 = mapper.createObjectNode();
        root1.set("id", mapper.createArrayNode().add("testId"));
        root1.set("phone", mapper.createArrayNode().add("1111111111"));
        Assert.assertEquals(
                "f50b888c-1ebb-4b3f-9a67-20ceedb57c38",
                idExtractor.extractId(oneShotTemplate, root1).orElse(null));
        final ObjectNode root2 = mapper.createObjectNode();
        root2.set("id", mapper.createArrayNode().add("testId"));
        root2.set("phone", mapper.createArrayNode().add("0000000000"));
        try {
            idExtractor.extractId(oneShotTemplate, root2);
            Assert.fail("Should have thrown exception");
        } catch (Exception e) {
            Assert.assertEquals(IllegalStateException.class, e.getClass());
        }
    }

}