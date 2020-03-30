package io.appform.statesman.publisher.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor
class BasePublisher {

    static final MediaType APPLICATION_JSON = MediaType.parse("application/json");

    final ObjectMapper mapper;
    final String endpoint;
    final OkHttpClient client;
}
