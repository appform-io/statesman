package io.appform.statesman.server.utils;

import io.appform.statesman.model.request.CreateProvider;
import io.appform.statesman.server.dao.providers.StoredProvider;

public class ProviderUtils {

    public static StoredProvider toDao(CreateProvider request){
        return StoredProvider.builder()
                .providerId(request.getProviderId())
                .providerName(request.getProviderName())
                .partitions(request.getPartitions())
                .build();
    }
}
