/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.RandomPortJerseyTest;
import org.apache.ambari.server.api.GsonJsonProvider;
import org.apache.ambari.server.api.MpackJsonProvider;
import org.apache.ambari.server.mpack.MpackCatalog;
import org.apache.ambari.server.mpack.MpackException;
import org.apache.ambari.server.mpack.MpackJson;
import org.apache.ambari.server.mpack.MpackLifecycleService;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;

public class MpackLifecycleHttpTest extends RandomPortJerseyTest {
  private MpackLifecycleService lifecycle;

  @Override
  protected ResourceConfig configure() {
    forceSet(TestProperties.CONTAINER_PORT, getPort(0) + "");
    return new ResourceConfig().register(MpackLifecycleApiService.class)
        .register(GsonJsonProvider.class).register(JacksonFeature.class)
        .register(MpackJsonProvider.class).register(MpackExceptionMapper.class);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
    org.springframework.security.core.Authentication authentication = TestAuthenticationFactory.createAdministrator();
    authentication.getAuthorities().forEach(authority ->
        ((org.apache.ambari.server.security.authorization.AmbariGrantedAuthority) authority)
            .getPrivilegeEntity().getPermission().setPermissionName(
                org.apache.ambari.server.orm.entities.PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    lifecycle = mock(MpackLifecycleService.class);
    when(lifecycle.targets()).thenReturn(List.of(Map.of("stack_name", "BIGTOP", "stack_version", "3.3.0")));
    MpackLifecycleApiService.initialize(lifecycle, mock(MpackCatalog.class));
    super.setUp();
  }

  @After
  @Override
  public void tearDown() throws Exception {
    try {
      super.tearDown();
    } finally {
      SecurityContextHolder.clearContext();
      SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
    }
  }

  @Test
  public void capabilitiesRetainTypedContractWithLegacyProviderRegistered() {
    try (Response response = target("mpack_capabilities").request().get()) {
      assertEquals(200, response.getStatus());
      JsonNode value = MpackJson.read(response.readEntity(String.class));
      assertTrue(value.path("schema_version").isIntegralNumber());
      assertEquals(1, value.path("schema_version").intValue());
      assertEquals("STACK_VERSION", value.path("binding_scope").textValue());
      assertEquals("BIGTOP", value.path("target_stack_versions").get(0).path("stack_name").textValue());
      assertTrue(value.path("operations").isArray());
      assertFalse(value.has("_children"));
    }
  }

  @Test
  public void mappedErrorsRetainCodeAndStructuredDetails() throws Exception {
    when(lifecycle.bindings()).thenThrow(new MpackException(MpackException.Code.STALE_PLAN,
        "The retained plan is stale", Map.of("expected_revision", 7)));
    try (Response response = target("mpack_bindings").request().get()) {
      assertEquals(409, response.getStatus());
      JsonNode value = MpackJson.read(response.readEntity(String.class));
      assertEquals(1, value.path("schema_version").intValue());
      assertEquals("STALE_PLAN", value.path("error").path("code").textValue());
      assertEquals(7, value.path("error").path("details").path("expected_revision").intValue());
    }
  }
}
