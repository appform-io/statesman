package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import io.appform.dropwizard.sharding.DBShardingBundle;

public class DBModule extends AbstractModule {

    private final DBShardingBundle<?> dbShardingBundle;

    public DBModule(DBShardingBundle<?> dbShardingBundle) {
        this.dbShardingBundle = dbShardingBundle;
    }


}
