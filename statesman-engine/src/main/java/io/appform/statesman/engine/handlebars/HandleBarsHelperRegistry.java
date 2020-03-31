package io.appform.statesman.engine.handlebars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Strings;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.dropwizard.jackson.Jackson;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;


@Slf4j
public class HandleBarsHelperRegistry {

    private static final String OPERATION_NOT_SUPPORTED = "Operation not supported: ";
    private static final String DEFAULT_TIME_ZONE = "IST";
    private static final DecimalFormat decimalFormat = new DecimalFormat("######.##");
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    private final Handlebars handlebars;

    private HandleBarsHelperRegistry(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    public static HandleBarsHelperRegistry newInstance(Handlebars handlebars) {
        return new HandleBarsHelperRegistry(handlebars);
    }

    public void register() {
        registerRupees();
        registerSuccess();
        registerStr();
        registerDecimalFormat();
        registerDate();
        registerRSub();
        registerToUpperCase();
        registerToLowerCase();
        registerDateFormat();
        registerStreq();
        registerEq();
        registerLt();
        registerLte();
        registerGt();
        registerGte();
        registerMapLookup();
    }

    private Object compareGte(int lhs) {
        return lhs >= 0
               ? "true"
               : null;
    }

    private Object compareGt(int lhs) {
        return lhs > 0
               ? "true"
               : null;
    }

    private Object compareLte(int lhs) {
        return lhs <= 0
               ? "true"
               : null;
    }

    private Object compareLt(int lhs) {
        return lhs < 0
               ? "true"
               : null;
    }

    private Object compareEq(int lhs) {
        return lhs == 0
               ? "true"
               : null;
    }

    private void registerGte() {
        handlebars.registerHelper("gte", (Helper<Number>) (aNumber, options) -> {
            val option = options.param(0);
            if (option instanceof Long) {
                return compareGte(Long.compare(aNumber.longValue(), (Long) option));
            }
            if (option instanceof Integer) {
                return compareGte(Integer.compare(aNumber.intValue(), (Integer) option));
            }
            if (option instanceof Double) {
                return compareGte(Double.compare(aNumber.doubleValue(), (Double) option));
            }
            if (option instanceof String) {
                return compareGte(Double.compare(aNumber.doubleValue(), Double.valueOf((String) option)));
            }
            throw new StatesmanError(ResponseCode.OPERATION_NOT_SUPPORTED);
        });
    }

    private void registerGt() {
        handlebars.registerHelper("gt", (Helper<Number>) (aNumber, options) -> {
            val option = options.param(0);
            if (option instanceof Long) {
                return compareGt(Long.compare(aNumber.longValue(), (Long) option));
            }
            if (option instanceof Integer) {
                return compareGt(Integer.compare(aNumber.intValue(), (Integer) option));
            }
            if (option instanceof Double) {
                return compareGt(Double.compare(aNumber.doubleValue(), (Double) option));
            }
            if (option instanceof String) {
                return compareGt(Double.compare(aNumber.doubleValue(), Double.valueOf((String) option)));
            }
            throw new StatesmanError(ResponseCode.OPERATION_NOT_SUPPORTED);
        });
    }

    private void registerLte() {
        handlebars.registerHelper("lte", (Helper<Number>) (aNumber, options) -> {
            val option = options.param(0);
            if (option instanceof Long) {
                return compareLte(Long.compare(aNumber.longValue(), (Long) option));
            }
            if (option instanceof Integer) {
                return compareLte(Integer.compare(aNumber.intValue(), (Integer) option));
            }
            if (option instanceof Double) {
                return compareLte(Double.compare(aNumber.doubleValue(), (Double) option));
            }
            if (option instanceof String) {
                return compareLte(Double.compare(aNumber.doubleValue(), Double.valueOf((String) option)));
            }
            throw new StatesmanError(ResponseCode.OPERATION_NOT_SUPPORTED);
        });
    }


    private void registerLt() {
        handlebars.registerHelper("lt", (Helper<Number>) (aNumber, options) -> {
            val option = options.param(0);
            if (option instanceof Long) {
                int lhs = Long.compare(aNumber.longValue(), (Long) option);
                return compareLt(lhs);
            }
            if (option instanceof Integer) {
                return compareLt(Integer.compare(aNumber.intValue(), (Integer) option));
            }
            if (option instanceof Double) {
                return compareLt(Double.compare(aNumber.doubleValue(), (Double) option));
            }
            if (option instanceof String) {
                return compareLt(Double.compare(aNumber.doubleValue(), Double.valueOf((String) option)));
            }
            throw new StatesmanError(ResponseCode.OPERATION_NOT_SUPPORTED);
        });
    }

    private void registerEq() {
        handlebars.registerHelper("eq", (Helper<Number>) (aNumber, options) -> {
            val option = options.param(0);
            if (option instanceof Long) {
                int lhs = Long.compare(aNumber.longValue(), (Long) option);
                return compareEq(lhs);
            }
            if (option instanceof Integer) {
                return compareEq(Integer.compare(aNumber.intValue(), (Integer) option));
            }
            if (option instanceof Double) {
                return compareEq(Double.compare(aNumber.doubleValue(), (Double) option));
            }
            if (option instanceof String) {
                return compareEq(Double.compare(aNumber.doubleValue(), Double.valueOf((String) option)));
            }
            throw new StatesmanError(ResponseCode.OPERATION_NOT_SUPPORTED);
        });
    }


    private void registerStreq() {
        handlebars.registerHelper("streq", (Helper<String>) (value, options) -> {
            if (!Strings.isNullOrEmpty(value) && value.equals(options.param(0))) {
                return "true";
            }
            return null;
        });
    }


    private void registerDateFormat() {
        handlebars.registerHelper("dateFormat", (Helper<Long>) (context, options) -> {
            try {
                if (null != context) {
                    SimpleDateFormat sdf = new SimpleDateFormat(options.param(0));
                    String timeZone =
                            options.params.length < 2
                            ? DEFAULT_TIME_ZONE
                            : options.param(1);
                    sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
                    return sdf.format(context);
                }
            }
            catch (Exception e) {
                log.error("Error formatting date", e);
            }
            return null;
        });
    }

    private void registerToLowerCase() {
        handlebars.registerHelper("toLowerCase", (Helper<String>) (context, options) -> {
            if (!Strings.isNullOrEmpty(context)) {
                return context.toLowerCase().trim();
            }
            return null;
        });
    }

    private void registerToUpperCase() {
        handlebars.registerHelper("toUpperCase", (Helper<String>) (context, options) -> {
            if (!Strings.isNullOrEmpty(context)) {
                return context.toUpperCase().trim();
            }
            return null;
        });
    }

    private void registerRSub() {
        handlebars.registerHelper("rsub", (Helper<String>) (context, options) -> {
            if (!Strings.isNullOrEmpty(context)) {
                int length = context.length();
                Integer index = options.param(0);
                if (length >= index) {
                    return context.substring(length - index);
                }
                return context;
            }
            return null;
        });
    }

    private void registerDate() {
        handlebars.registerHelper("date", (Helper<Long>) (context, options) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            try {
                if (null != context) {
                    return sdf.format(new Date(context));
                }
            }
            catch (Exception e) {
                log.error("Error converting date", e);
            }
            return sdf.format(new Date());
        });
    }

    private void registerDecimalFormat() {
        handlebars.registerHelper("decimalFormat", (Helper<Number>) (aNumber, options) -> {
            DecimalFormat decimalFormatReqd =
                    (options.params.length > 0 && !Strings.isNullOrEmpty(options.param(0)))
                    ? new DecimalFormat(options.param(0))
                    : decimalFormat;
            return decimalFormatReqd.format(aNumber.doubleValue());
        });
    }

    private void registerStr() {
        handlebars.registerHelper("str",
                                  (Helper<Number>) (aNumber, options) -> String.valueOf(aNumber.doubleValue()));
    }

    private void registerSuccess() {
        handlebars.registerHelper("success", (Helper<Boolean>) (context, options) -> context
                                                                                     ? "succeeded"
                                                                                     : "failed");
    }

    private void registerRupees() {
        handlebars.registerHelper("rupees", (Helper<Number>) (aNumber, options) -> {
            double value = 0;
            if (aNumber instanceof Long) {
                value = aNumber.longValue();
            }
            if (aNumber instanceof Integer) {
                value = aNumber.intValue();
            }
            if (aNumber instanceof Double) {
                value = aNumber.doubleValue();
            }
            return decimalFormat.format(value / 100.0);
        });
    }

    private void registerMapLookup() {
        handlebars.registerHelper("map_lookup", new Helper<JsonNode>() {
            @Override
            public CharSequence apply(JsonNode node, Options options) throws IOException {
                final String key = options.hash("pointer");
                if (Strings.isNullOrEmpty(key)) {
                    return null;
                }
                val keyNode = node.at(key);
                if (null == keyNode || keyNode.isNull() || keyNode.isMissingNode() || !keyNode.isTextual()) {
                    return null;
                }
                int value = Integer.parseInt(keyNode.asText());
                if (value < 10) {
                    return MAPPER.writeValueAsString(options.hash("op_" + value));
                }
                List<Integer> selectedOptions = new ArrayList<>();
                while (value > 0) {
                    selectedOptions.add(value % 10);
                    value = value / 10;
                }
                if (keyNode.isArray()) {
                    return MAPPER.writeValueAsString(
                            selectedOptions
                                    .stream()
                                    .map(option -> options.hash("op_" + option))
                                    .collect(Collectors.toList()));
                }
                return null;
            }

            private JsonNode toNode(Object object) {
                return MAPPER.valueToTree(object);
            }
        });
    }
}
