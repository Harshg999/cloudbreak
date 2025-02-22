package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;

@ExtendWith(MockitoExtension.class)
class EnvironmentDtoConverterTest {

    @Mock
    private AuthenticationDtoConverter authenticationDtoConverter;

    @Mock
    private CostTagging costTagging;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DefaultInternalAccountTagService defaultInternalAccountTagService;

    @Mock
    private AccountTagService accountTagService;

    @Mock
    private CrnUserDetailsService crnUserDetailsService;

    @Mock
    private EnvironmentRecipeService environmentRecipeService;

    @InjectMocks
    private EnvironmentDtoConverter underTest;

    @Test
    public void testEnvironmentToEnvironmentDtoFreeIpaCreationWithoutAwsParameters() {
        Environment source = new Environment();
        source.setId(1L);
        source.setFreeIpaInstanceType("large");
        source.setFreeIpaImageId("imageid");
        source.setFreeIpaImageCatalog("imagecatalog");
        source.setFreeIpaInstanceCountByGroup(1);
        source.setFreeIpaEnableMultiAz(true);
        source.setCreateFreeIpa(true);
        source.setCloudPlatform("AWS");
        source.setAuthentication(new EnvironmentAuthentication());

        when(environmentRecipeService.getRecipes(1L)).thenReturn(Set.of("recipe1", "recipe2"));
        EnvironmentDto environmentDto = underTest.environmentToDto(source);
        FreeIpaCreationDto freeIpaCreation = environmentDto.getFreeIpaCreation();
        assertNotNull(freeIpaCreation);
        assertEquals("large", freeIpaCreation.getInstanceType());
        assertEquals("imageid", freeIpaCreation.getImageId());
        assertEquals("imagecatalog", freeIpaCreation.getImageCatalog());
        assertEquals(1, freeIpaCreation.getInstanceCountByGroup());
        assertTrue(freeIpaCreation.isEnableMultiAz());
        assertTrue(freeIpaCreation.getCreate());
        assertNull(freeIpaCreation.getAws());
        assertThat(freeIpaCreation.getRecipes()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

}