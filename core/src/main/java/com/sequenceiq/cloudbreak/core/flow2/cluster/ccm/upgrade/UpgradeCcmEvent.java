package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpgradeCcmEvent implements FlowEvent {
    UPGRADE_CCM_EVENT("UPGRADE_CCM_TRIGGER_EVENT"),
    UPGRADE_CCM_TUNNEL_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmTunnelUpdateResult.class)),
    UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmPushSaltStatesResult.class)),
    UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmReconfigureNginxResult.class)),
    UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmRegisterClusterProxyResult.class)),
    UPGRADE_CCM_HEALTH_CHECK_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmHealthCheckResult.class)),
    UPGRADE_CCM_FAILED_EVENT(EventSelectorUtil.selector(UpgradeCcmFailedEvent.class)),
    UPGRADE_CCM_REMOVE_AGENTS_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmRemoveAgentResult.class)),
    UPGRADE_CCM_DEREGISTER_AGENTS_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmDeregisterAgentResult.class)),

    FINALIZED_EVENT("UPGRADE_CCM_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("UPGRADE_CCM_FAIL_HANDLED_EVENT");

    private final String event;

    UpgradeCcmEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
