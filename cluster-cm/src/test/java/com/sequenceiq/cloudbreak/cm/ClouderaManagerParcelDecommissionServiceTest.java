package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerParcelDecommissionServiceTest {

    private static final String STACK_NAME = "stackname";

    @InjectMocks
    private ClouderaManagerParcelDecommissionService underTest;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ParcelResourceApi parcelResourceApi;

    @Mock
    private ParcelsResourceApi parcelsResourceApi;

    @Spy
    private ClouderaManagerParcelManagementService parcelManagementService;

    @Mock
    private ApiClient apiClient;

    @Test
    void testDeactivateUnusedComponents() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> activatedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(activatedParcels, ParcelStatus.ACTIVATED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        // WHEN
        ParcelOperationStatus actual = underTest.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, STACK_NAME, usedComponents, productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).deactivateCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).deactivateCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).deactivateCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(1, actual.getSuccessful().size());
        assertEquals(0, actual.getFailed().size());
        assertTrue(actual.getSuccessful().containsEntry("product3", "version3"));
    }

    @Test
    public void testDeactivateUnusedComponentsWhenDeactivationFailsOnParcel() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> activatedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(activatedParcels, ParcelStatus.ACTIVATED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        when(parcelResourceApi.deactivateCommand(STACK_NAME, "product3", "version3")).thenThrow(new ApiException());
        // WHEN and THEN
        ParcelOperationStatus operationStatus = underTest.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, STACK_NAME, usedComponents,
                productsFromImage);

        verify(parcelResourceApi, times(1)).deactivateCommand(STACK_NAME, "product3", "version3");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals(1, operationStatus.getFailed().size());
        assertTrue(operationStatus.getFailed().containsEntry("product3", "version3"));
    }

    @Test
    public void testUndistributeUnusedComponents() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DISTRIBUTED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(clouderaManagerPollingServiceProvider
                .startPollingCmParcelStatus(eq(stack), eq(apiClient), any(), any()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        // WHEN
        ParcelOperationStatus operationStatus = underTest.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).startRemovalOfDistributionCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals(0, operationStatus.getFailed().size());
        assertTrue(operationStatus.getSuccessful().containsEntry("product3", "version3"));
    }

    @Test
    public void testUndistributeUnusedComponentsAndUndistributionFails() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DISTRIBUTED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        when(parcelResourceApi.startRemovalOfDistributionCommand(STACK_NAME, "product3", "version3")).thenThrow(new ApiException());
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(clouderaManagerPollingServiceProvider
                .startPollingCmParcelStatus(eq(stack), eq(apiClient), any(), any()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        // WHEN
        ParcelOperationStatus operationStatus = underTest.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).startRemovalOfDistributionCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals(1, operationStatus.getFailed().size());
        assertTrue(operationStatus.getFailed().containsEntry("product3", "version3"));
    }

    @Test
    public void testRemoveUnusedComponents() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DOWNLOADED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(clouderaManagerPollingServiceProvider
                .startPollingCmParcelDelete(eq(stack), eq(apiClient), any()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).removeDownloadCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(0, operationStatus.getFailed().size());
        assertEquals(1, operationStatus.getSuccessful().size());
        assertTrue(operationStatus.getSuccessful().containsEntry("product3", "version3"));
    }

    @Test
    public void testRemoveUnusedComponentsWhenRemovalFails() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DOWNLOADED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(parcelResourceApi.removeDownloadCommand(STACK_NAME, "product3", "version3")).thenThrow(new ApiException());
        when(clouderaManagerPollingServiceProvider
                .startPollingCmParcelDelete(eq(stack), eq(apiClient), any()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).removeDownloadCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals(0, operationStatus.getSuccessful().size());
        assertTrue(operationStatus.getFailed().containsEntry("product3", "version3"));
    }

    @Test
    public void testRemoveUnusedParcelVersions() throws ApiException {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        ClouderaManagerProduct currentProductWithVersionToKeep = new ClouderaManagerProduct().withName("CDH").withVersion("current");
        doReturn(List.of(
                new ApiParcel().product("ignored").version("current"),
                new ApiParcel().product(currentProductWithVersionToKeep.getName()).version("old"),
                new ApiParcel().product(currentProductWithVersionToKeep.getName()).version("old2"),
                new ApiParcel().product(currentProductWithVersionToKeep.getName()).version(currentProductWithVersionToKeep.getVersion())))
                .when(parcelManagementService).getClouderaManagerParcelsByStatus(parcelsResourceApi, STACK_NAME, ParcelStatus.DISTRIBUTED);
        ArgumentCaptor<Multimap<String, String>> parcelVersionsCaptorForDownloaded = ArgumentCaptor.forClass(Multimap.class);
        when(clouderaManagerPollingServiceProvider
                .startPollingCmParcelStatus(eq(stack), eq(apiClient), parcelVersionsCaptorForDownloaded.capture(), eq(ParcelStatus.DOWNLOADED)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        doReturn(List.of(
                new ApiParcel().product("ignored").version("current"),
                new ApiParcel().product(currentProductWithVersionToKeep.getName()).version("old"),
                new ApiParcel().product(currentProductWithVersionToKeep.getName()).version("old2"),
                new ApiParcel().product(currentProductWithVersionToKeep.getName()).version(currentProductWithVersionToKeep.getVersion())))
                .when(parcelManagementService).getClouderaManagerParcelsByStatus(parcelsResourceApi, STACK_NAME, ParcelStatus.DOWNLOADED);
        ArgumentCaptor<Multimap<String, String>> parcelVersionsCaptorForDelete = ArgumentCaptor.forClass(Multimap.class);
        when(clouderaManagerPollingServiceProvider
                .startPollingCmParcelDelete(eq(stack), eq(apiClient), parcelVersionsCaptorForDelete.capture()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.removeUnusedParcelVersions(apiClient, parcelsResourceApi, parcelResourceApi, stack, currentProductWithVersionToKeep);

        verify(parcelResourceApi).startRemovalOfDistributionCommand(STACK_NAME, currentProductWithVersionToKeep.getName(), "old");
        verify(parcelResourceApi).startRemovalOfDistributionCommand(STACK_NAME, currentProductWithVersionToKeep.getName(), "old2");
        assertEquals(2, parcelVersionsCaptorForDownloaded.getValue().size());
        assertTrue(parcelVersionsCaptorForDownloaded.getValue().containsEntry("CDH", "old"));
        assertTrue(parcelVersionsCaptorForDownloaded.getValue().containsEntry("CDH", "old2"));
        verify(parcelResourceApi).removeDownloadCommand(STACK_NAME, currentProductWithVersionToKeep.getName(), "old");
        verify(parcelResourceApi).removeDownloadCommand(STACK_NAME, currentProductWithVersionToKeep.getName(), "old2");
        assertEquals(2, parcelVersionsCaptorForDelete.getValue().size());
        assertTrue(parcelVersionsCaptorForDelete.getValue().containsEntry("CDH", "old"));
        assertTrue(parcelVersionsCaptorForDelete.getValue().containsEntry("CDH", "old2"));
        verifyNoMoreInteractions(parcelResourceApi);
    }

    @Test
    public void testGetParcelsInStatusThrowsException() throws ApiException {

        doThrow(new ApiException("Operation failed")).
                when(parcelManagementService).getClouderaManagerParcelsByStatus(parcelsResourceApi, STACK_NAME, ParcelStatus.ACTIVATED);

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class, () ->
        underTest.getParcelsInStatus(parcelsResourceApi, STACK_NAME, ParcelStatus.ACTIVATED));

        Assertions.assertEquals(actual.getMessage(), "Unable to fetch the list of activated parcels due to: Operation failed");
    }

    private ApiParcelList createApiParcelList(Map<String, String> products, ParcelStatus parcelStatus) {
        ApiParcelList parcelList = new ApiParcelList();
        List<ApiParcel> apiParcels = products.entrySet().stream().map(entry -> createApiParcel(entry.getKey(), entry.getValue(), parcelStatus))
                .collect(Collectors.toList());
        parcelList.setItems(apiParcels);
        return parcelList;
    }

    private ApiParcel createApiParcel(String product, String version, ParcelStatus parcelStatus) {
        ApiParcel parcel = new ApiParcel();
        parcel.setProduct(product);
        parcel.setVersion(version);
        parcel.setStage(parcelStatus.name());
        return parcel;
    }
}
