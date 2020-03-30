package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.server.dao.providers.StoredProvider;

public class DBModule extends AbstractModule {

    private final DBShardingBundle<?> dbShardingBundle;

    public DBModule(DBShardingBundle<?> dbShardingBundle) {
        this.dbShardingBundle = dbShardingBundle;
    }

    @Singleton
    @Provides
    public LookupDao<StoredProvider> provideProvider() {
        return dbShardingBundle.createParentObjectDao(StoredProvider.class);
    }
}
