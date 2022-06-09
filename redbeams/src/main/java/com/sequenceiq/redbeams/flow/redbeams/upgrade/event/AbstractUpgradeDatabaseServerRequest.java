package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public abstract class AbstractUpgradeDatabaseServerRequest extends RedbeamsEvent {
    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    public AbstractUpgradeDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack databaseStack) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.databaseStack = databaseStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }

    @Override
    public String toString() {
        return "AbstractUpgradeDatabaseServerRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", databaseStack=" + databaseStack +
                "} " + super.toString();
    }

}