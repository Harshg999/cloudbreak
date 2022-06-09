package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;

public class RedbeamsStartUpgradeRequest extends RedbeamsEvent {

    private final String majorVersion;

    public RedbeamsStartUpgradeRequest(Long resourceId, String majorVersion) {
        super(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), resourceId);
        this.majorVersion = majorVersion;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    @Override
    public String toString() {
        return "RedbeamsStartUpgradeRequest{" +
                "majorVersion='" + majorVersion + '\'' +
                "} " + super.toString();
    }
}
