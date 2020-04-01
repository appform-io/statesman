package io.appform.statesman.model.request;

import io.appform.statesman.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateProvider {

    @NotNull
    @NotEmpty
    private String providerId;

    @NotNull
    @NotEmpty
    private String providerName;

    @NotNull
    @NotEmpty
    private String useCase;

    @Min(1)
    @Max(64)
    @Builder.Default
    private int partitions = Constants.MAX_PROVIDER_PARTITIONS;


}
