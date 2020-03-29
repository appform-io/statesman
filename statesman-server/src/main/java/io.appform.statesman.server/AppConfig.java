package io.appform.statesman.server;

import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Slf4j
public class AppConfig extends Configuration {

    @NotNull
    @Valid
    private ShardedHibernateFactory shards;
}
