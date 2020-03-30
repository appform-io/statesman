package io.appform.statesman.server.utils;

import io.appform.statesman.model.request.CreateProvider;
import io.appform.statesman.model.response.ProviderInfo;
import io.appform.statesman.server.dao.providers.StoredProvider;

public class ProviderUtils {

    public static StoredProvider toDto(CreateProvider request){
        return StoredProvider.builder()
                .providerId(request.getProviderId())
                .providerName(request.getProviderName())
                .partitions(request.getPartitions())
                .active(true)
                .build();
    }

    public static ProviderInfo toDto(StoredProvider storedProvider){
        return ProviderInfo.builder()
                .providerId(storedProvider.getProviderId())
                .providerName(storedProvider.getProviderName())
                .partitions(storedProvider.getPartitions())
                .active(storedProvider.isActive())
                .build();
    }

}
