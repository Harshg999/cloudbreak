package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

public class UpgradeDatabaseServerRequest extends AbstractUpgradeDatabaseServerRequest {

    private String targetMajorVersion;

    public UpgradeDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack databaseStack, String targetMajorVersion) {
        super(cloudContext, cloudCredential, databaseStack);
    }

    public String getTargetMajorVersion() {
        return targetMajorVersion;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerRequest{" +
                "targetMajorVersion='" + targetMajorVersion + '\'' +
                "} " + super.toString();
    }
}