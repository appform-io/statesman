package io.appform.statesman.server.ingress;

/**
 *
 */
public class IngressHandlerTest {


/*    @Test
    @SneakyThrows
    public void testDropDetectionSingleShot() {
        IVRDropDetectionConfig dropDetectionConfig = new IVRDropDetectionConfig();
        dropDetectionConfig.setEnabled(true);
        dropDetectionConfig.setDetectionPatterns(
                ImmutableMap.<String, List<String>>builder()
                        .put("kalyrea_karnatata",
                             Collections.singletonList("Question4.*"))
                        .build());
        val node = Jackson.newObjectMapper().readTree("{\"duration\":[\"14\"],\"called\":[\"8068171046\"],\"callFrom\":[\"9986032019\"],\"Question4\":[\"\"],\"Question3\":[\"\"],\"state\":[\"KARNATAKA\"],\"id\":[\"11576507065e94597bb9d6\"],\"Question2\":[\"2\"],\"Question1\":[\"3\"],\"calltime\":[\"13/04/2020 05:52 PM\"]}");
        Assert.assertTrue(IngressHandler.isDroppedCallSingleShot("kalyrea_karnatata", node, dropDetectionConfig));
    }*/

}