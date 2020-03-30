package io.appform.statesman.engine.handlebars;

import org.junit.Test;

/**
 *
 */
public class HandleBarsServiceTest {

    @Test
    public void transform() {
        HandleBarsService handleBarsService = new HandleBarsService();
        final String template = "{\"language\" : \"{{ map_lookup op_1=\"EN\" op_2=\"KA\" op_3=\"HI\" key=\"payload[question3]\"}}\"}";

    }
}