package io.appform.statesman.engine.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;

import javax.annotation.Nullable;

public class MapperUtils {
    private static ObjectMapper objectMapper;

    private MapperUtils() {
    }

    public static void initialize(final ObjectMapper inMapper) {
        objectMapper = inMapper;
    }

    @Nullable
    public static byte[] serialize(Object data) {
        return serialize(objectMapper, data);
    }

    @Nullable
    public static <T> T deserialize(byte[] data, Class<T> valueType) {
        return deserialize(objectMapper, data, valueType);
    }

    @Nullable
    public static <T> T deserialize( byte[] data, TypeReference<T> typeReference) {
        return deserialize(objectMapper, data, typeReference);
    }

    @Nullable
    public static <T> T deserialize(ObjectMapper mapper, byte[] data, Class<T> valueType) {
        try {
            if (data == null) {
                return null;
            }
            return mapper.readValue(data, valueType);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.JSON_ERROR);
        }
    }

    @Nullable
    public static <T> T deserialize(ObjectMapper mapper, String data, Class<T> valueType) {
        try {
            if (data == null) {
                return null;
            }
            return mapper.readValue(data, valueType);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.JSON_ERROR);

        }
    }

    @Nullable
    public static <T> T deserialize(ObjectMapper mapper, byte[] data, TypeReference<T> typeReference) {
        try {
            if (data == null) {
                return null;
            }
            return mapper.readValue(data, typeReference);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.JSON_ERROR);

        }
    }

    @Nullable
    public static <T> T deserialize(ObjectMapper mapper, String data, TypeReference<T> typeReference) {
        try {
            if (data == null) {
                return null;
            }
            return mapper.readValue(data, typeReference);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.JSON_ERROR);

        }
    }

    @Nullable
    public static <T> T deserializeObject(ObjectMapper mapper, Object data, Class<T> valueType) {
        try {
            if (data == null) {
                return null;
            }
            return mapper.convertValue(data, valueType);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.JSON_ERROR);

        }
    }

    @Nullable
    public static byte[] serialize(ObjectMapper mapper, Object data) {
        try {
            if (data == null) {
                return null;
            }
            return mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw StatesmanError.propagate(e, ResponseCode.JSON_ERROR);

        }
    }
}
