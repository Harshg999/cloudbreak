package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTriggerRequest;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class StackCcmUpgradeServiceTest {

    private static final Long CLUSTER_ID = 123L;

    private static final Long STACK_ID = 234L;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackService stackService;

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private EnvironmentClientService environmentService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @InjectMocks
    private StackCcmUpgradeService underTest;

    @BeforeEach
    void setUp() {
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCM", "CCMV2" }, mode = Mode.INCLUDE)
    void testUpgradeCcm(Tunnel tunnel) {
        Stack stack = createStack(tunnel, Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn("crn")), any())).thenReturn(stack);
        when(environmentService.getByCrn("envCrn")).thenReturn(environment);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn("crn"));

        ArgumentCaptor<UpgradeCcmTriggerRequest> requestCaptor = ArgumentCaptor.forClass(UpgradeCcmTriggerRequest.class);
        verify(reactorNotifier).notify(eq(STACK_ID), eq("UPGRADE_CCM_TRIGGER_EVENT"), requestCaptor.capture());
        UpgradeCcmTriggerRequest request = requestCaptor.getValue();
        assertThat(request.getResourceId()).isEqualTo(STACK_ID);
        assertThat(request.getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(request.getOldTunnel()).isEqualTo(tunnel);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.TRIGGERED);
        assertThat(response.getResourceCrn()).isEqualTo("crn");
    }

    @Test
    void environmentNotLatest() {
        Stack stack = createStack(Tunnel.CCM, Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.DIRECT);
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn("crn")), any())).thenReturn(stack);
        when(environmentService.getByCrn("envCrn")).thenReturn(environment);

        assertThatThrownBy(() -> underTest.upgradeCcm(NameOrCrn.ofCrn("crn")))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(reactorNotifier);
    }

    @Test
    void testAlreadyUpgraded() {
        Stack stack = createStack(Tunnel.latestUpgradeTarget(), Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn("crn")), any())).thenReturn(stack);
        when(environmentService.getByCrn("envCrn")).thenReturn(environment);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn("crn"));

        verifyNoInteractions(reactorNotifier);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.SKIP);
        assertThat(response.getResourceCrn()).isEqualTo("crn");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = { "AVAILABLE", "MAINTENANCE_MODE_ENABLED", "UPGRADE_CCM_FAILED" }, mode = Mode.EXCLUDE)
    void testStackUnavailable(Status status) {
        Stack stack = createStack(Tunnel.CCM, status);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn("crn")), any())).thenReturn(stack);
        when(environmentService.getByCrn("envCrn")).thenReturn(environment);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn("crn"));

        verifyNoInteractions(reactorNotifier);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.ERROR);
        assertThat(response.getResourceCrn()).isEqualTo("crn");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCM", "CCMV2", "CCMV2_JUMPGATE" }, mode = Mode.EXCLUDE)
    void testWrongOldTunnel(Tunnel tunnel) {
        Stack stack = createStack(tunnel, Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn("crn")), any())).thenReturn(stack);
        when(environmentService.getByCrn("envCrn")).thenReturn(environment);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn("crn"));

        verifyNoInteractions(reactorNotifier);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.ERROR);
        assertThat(response.getResourceCrn()).isEqualTo("crn");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    private DetailedEnvironmentResponse createEnvironment(Tunnel tunnel) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setTunnel(tunnel);
        return environment;
    }

    private Stack createStack(Tunnel tunnel, Status status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(tunnel);
        stack.setEnvironmentCrn("envCrn");
        stack.setResourceCrn("crn");
        stack.setStackStatus(new StackStatus(stack, status, null, null));
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        return stack;
    }
}
