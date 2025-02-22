package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.telemetry.model.Monitoring;

@ExtendWith(MockitoExtension.class)
public class MonitoringConfigServiceTest {

    private static final Integer DEFAULT_CM_SMON_PORT = 61010;

    @InjectMocks
    private MonitoringConfigService underTest;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private ExporterConfiguration cmMonitoringConfiguration;

    @Mock
    private MonitoringGlobalAuthConfig monitoringGlobalAuthConfig;

    @Mock
    private MonitoringAgentConfiguration monitoringAgentConfiguration;

    @Mock
    private Monitoring monitoring;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new MonitoringConfigService(monitoringConfiguration, monitoringGlobalAuthConfig);
    }

    @Test
    public void testCreateMonitoringConfigs() {
        // GIVEN
        MonitoringClusterType clusterType = MonitoringClusterType.CLOUDERA_MANAGER;
        MonitoringAuthConfig authConfig = new MonitoringAuthConfig("user", "pass".toCharArray());
        given(monitoringConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.isDevStack()).willReturn(false);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(DEFAULT_CM_SMON_PORT);
        given(monitoring.getRemoteWriteUrl()).willReturn("https://myendpoint/api/v1/receive");
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(monitoring, clusterType, authConfig, null, true, false);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("https://myendpoint/api/v1/receive", result.getRemoteWriteUrl());
        assertEquals("false", result.toMap().get("useDevStack").toString());
        assertEquals(DEFAULT_CM_SMON_PORT, result.getCmMetricsExporterPort());
    }

    @Test
    public void testCreateMonitoringConfigsWithGlobalAuthConfig() {
        // GIVEN
        MonitoringClusterType clusterType = MonitoringClusterType.CLOUDERA_MANAGER;
        MonitoringAuthConfig authConfig = new MonitoringAuthConfig("user", "pass".toCharArray());
        given(monitoringConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.isDevStack()).willReturn(false);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(monitoring.getRemoteWriteUrl()).willReturn("https://myendpoint/api/v1/receive");
        given(monitoringGlobalAuthConfig.isEnabled()).willReturn(true);
        given(monitoringGlobalAuthConfig.getToken()).willReturn("my-token");
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(monitoring, clusterType, authConfig, null, true, false);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("my-token", result.toMap().get("token").toString());

    }

    @Test
    public void testCreateMonitoringConfigsWithDevStack() {
        // GIVEN
        MonitoringClusterType clusterType = MonitoringClusterType.CLOUDERA_MANAGER;
        MonitoringAuthConfig authConfig = new MonitoringAuthConfig("user", "pass".toCharArray());
        given(monitoringConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.isDevStack()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(monitoringConfiguration.getRemoteWriteUrl()).willReturn("https://myendpoint/$accountid");
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(monitoring, clusterType, authConfig, null, true, false);
        // THEN
        assertTrue(result.isEnabled());
        assertNull(result.getRemoteWriteUrl());
        assertEquals(result.toMap().get("useDevStack").toString(), "true");
    }

    @Test
    public void testCreateMonitoringConfigsWithSaasDisabled() {
        // GIVEN
        MonitoringClusterType clusterType = MonitoringClusterType.CLOUDERA_MANAGER;
        MonitoringAuthConfig authConfig = new MonitoringAuthConfig("user", "pass".toCharArray());
        given(monitoringConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(monitoringConfiguration.getRemoteWriteUrl()).willReturn("https://myendpoint/$accountid");
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(monitoring, clusterType, authConfig, null, false, false);
        // THEN
        assertFalse(result.isEnabled());
    }

    @Test
    public void testCreateMonitoringConfigsWithSaasDisabledAndPaasEnabled() {
        // GIVEN
        MonitoringClusterType clusterType = MonitoringClusterType.CLOUDERA_MANAGER;
        MonitoringAuthConfig authConfig = new MonitoringAuthConfig("user", "pass".toCharArray());
        given(monitoringConfiguration.isEnabled()).willReturn(true);
        given(monitoringConfiguration.isPaasSupport()).willReturn(true);
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(monitoringConfiguration.getAgent()).willReturn(monitoringAgentConfiguration);
        given(monitoringConfiguration.getRemoteWriteUrl()).willReturn("https://myendpoint/$accountid");
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(monitoring, clusterType, authConfig, null, false, false);
        // THEN
        assertTrue(result.isEnabled());
    }

    @Test
    public void testCreateMonitoringConfigsWithNulls() {
        // GIVEN
        // WHEN
        MonitoringConfigView result = underTest.createMonitoringConfig(monitoring, null, null, null, true, false);
        // THEN
        assertFalse(result.isEnabled());
    }
}
