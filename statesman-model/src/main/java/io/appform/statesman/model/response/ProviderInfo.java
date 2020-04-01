package io.appform.statesman.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProviderInfo {

    private String providerId;
    private String useCase;
    private String providerName;
    private long partitions;
    private boolean active;
}
