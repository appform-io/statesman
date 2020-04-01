package io.appform.statesman.server.utils;

import com.google.common.base.Strings;
import io.appform.statesman.model.request.CreateProvider;
import io.appform.statesman.model.response.ProviderInfo;
import io.appform.statesman.server.dao.providers.StoredProvider;

import java.util.UUID;

public class ProviderUtils {

    public static StoredProvider toDto(CreateProvider request) {
        String providerId = Strings.isNullOrEmpty(request.getProviderId())
                ? UUID.randomUUID().toString()
                : request.getProviderId();
        return StoredProvider.builder()
                .providerId(providerId)
                .providerName(request.getProviderName())
                .useCase(request.getUseCase())
                .partitions(request.getPartitions())
                .active(true)
                .build();
    }

    public static ProviderInfo toDto(StoredProvider storedProvider) {
        return ProviderInfo.builder()
                .providerId(storedProvider.getProviderId())
                .useCase(storedProvider.getUseCase())
                .providerName(storedProvider.getProviderName())
                .partitions(storedProvider.getPartitions())
                .active(storedProvider.isActive())
                .build();
    }

}
