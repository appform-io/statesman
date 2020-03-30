package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.DBShardingBundle;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.statesman.server.dao.action.StoredActionTemplate;
import io.appform.statesman.server.dao.providers.StoredProvider;
import io.appform.statesman.server.dao.state.StoredStateTransition;
import io.appform.statesman.server.dao.workflow.StoredWorkflowInstance;
import io.appform.statesman.server.dao.workflow.StoredWorkflowTemplate;

public class DBModule extends AbstractModule {

    private final DBShardingBundle<?> dbShardingBundle;

    public DBModule(DBShardingBundle<?> dbShardingBundle) {
        this.dbShardingBundle = dbShardingBundle;
    }

    @Singleton
    @Provides
    public LookupDao<StoredProvider> provideProviderLookupDao() {
        return dbShardingBundle.createParentObjectDao(StoredProvider.class);
    }


    @Singleton
    @Provides
    public LookupDao<StoredActionTemplate> provideActionTemplateLookupDao() {
        return dbShardingBundle.createParentObjectDao(StoredActionTemplate.class);
    }

    @Singleton
    @Provides
    public LookupDao<StoredWorkflowTemplate> provideWorkflowTemplateLookupDao() {
        return dbShardingBundle.createParentObjectDao(StoredWorkflowTemplate.class);
    }

    @Singleton
    @Provides
    public LookupDao<StoredWorkflowInstance> provideWorkflowInstanceLookupDao() {
        return dbShardingBundle.createParentObjectDao(StoredWorkflowInstance.class);
    }

    @Singleton
    @Provides
    public RelationalDao<StoredStateTransition> provideStateTransitionRelationalDao() {
        return dbShardingBundle.createRelatedObjectDao(StoredStateTransition.class);
    }
}
