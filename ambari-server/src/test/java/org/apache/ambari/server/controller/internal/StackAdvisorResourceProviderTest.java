/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.controller.internal.StackAdvisorResourceProvider.CONFIGURATIONS_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.StackAdvisorResourceProvider.USER_CONTEXT_OPERATION_DETAILS_PROPERTY;
import static org.apache.ambari.server.controller.internal.StackAdvisorResourceProvider.USER_CONTEXT_OPERATION_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import jakarta.ws.rs.WebApplicationException;

import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyStackAdvisorPlanner;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Lists;

public class StackAdvisorResourceProviderTest {

  private RecommendationResourceProvider provider;

  @Test
  public void testCalculateConfigurations() throws Exception {
    Request request = createMockRequest(
        CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string",
        CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", Lists.newArrayList("array1", "array2"));

    Map<String, Map<String, Map<String, String>>> calculatedConfigurations = provider.calculateConfigurations(request);

    assertNotNull(calculatedConfigurations);
    assertEquals(1, calculatedConfigurations.size());
    Map<String, Map<String, String>> site = calculatedConfigurations.get("site");
    assertNotNull(site);
    assertEquals(1, site.size());
    Map<String, String> properties = site.get("properties");
    assertNotNull(properties);
    assertEquals(2, properties.size());
    assertEquals("string", properties.get("string_prop"));
    assertEquals("[array1, array2]", properties.get("array_prop"));
  }

  @Nonnull
  private RecommendationResourceProvider createRecommendationResourceProvider() {
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    return new RecommendationResourceProvider(ambariManagementController);
  }

  @Nonnull
  private Request createMockRequest(Object... propertyKeysAndValues) {
    Request request = mock(Request.class);
    Set<Map<String, Object>> propertiesSet = new HashSet<>();
    Map<String, Object> propertiesMap = new HashMap<>();
    Iterator<Object> it = Arrays.asList(propertyKeysAndValues).iterator();
    while(it.hasNext()) {
      String key = (String)it.next();
      Object value = it.next();
      propertiesMap.put(key, value);
    }
    propertiesSet.add(propertiesMap);
    doReturn(propertiesSet).when(request).getProperties();
    return request;
  }

  @Test
  public void testReadUserContext() throws Exception {
    Request request = createMockRequest(
        CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string",
        USER_CONTEXT_OPERATION_PROPERTY, "op1",
        USER_CONTEXT_OPERATION_DETAILS_PROPERTY, "op_det");

    Map<String, String> userContext = provider.readUserContext(request);

    assertNotNull(userContext);
    assertEquals(2, userContext.size());
    assertEquals("op1", userContext.get("operation"));
    assertEquals("op_det", userContext.get("operation_details"));
  }

  @Test
  public void testCalculateConfigurationsWithNullPropertyValues() throws Exception {
    Request request = createMockRequest(
        CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", null,
        CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", Lists.newArrayList("array1", "array2"));

    Map<String, Map<String, Map<String, String>>> calculatedConfigurations = provider.calculateConfigurations(request);

    assertNotNull(calculatedConfigurations);
    assertEquals(1, calculatedConfigurations.size());
    Map<String, Map<String, String>> site = calculatedConfigurations.get("site");
    assertNotNull(site);
    assertEquals(1, site.size());
    Map<String, String> properties = site.get("properties");
    assertNotNull(properties);

    assertEquals("[array1, array2]", properties.get("array_prop"));

    // config properties with null values should be ignored
    assertFalse(properties.containsKey("string_prop"));
  }

 
  @Test
  public void testStackAdvisorWithEmptyHosts() {
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    RecommendationResourceProvider provider = new RecommendationResourceProvider(ambariManagementController);

    Request request = mock(Request.class);
    Set<Map<String, Object>> propertiesSet = new HashSet<>();
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put("hosts", new LinkedHashSet<>());
    propertiesMap.put("recommend", "configurations");
    propertiesSet.add(propertiesMap);
    doReturn(propertiesSet).when(request).getProperties();

    try {
      provider.createResources(request);
      Assert.fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void rejectsUnauthorizedNumericTargetBeforeClusterOrHostReads() throws Exception {
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(22L)).thenReturn(cluster);
    when(cluster.getResourceId()).thenReturn(5L);
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterUser("cluster-a-user", 4L));

    WebApplicationException error = expectAdvisorError(advisorRequest(22L, List.of("foreign")));

    assertEquals(403, error.getResponse().getStatus());
    verify(cluster, never()).getDesiredStackVersion();
    verify(cluster, never()).getHosts();
    verify(clusters, never()).hostExists(anyString());
  }

  @Test
  public void reportsMissingNumericTargetWithoutLeakingExceptionDetails() throws Exception {
    Clusters clusters = mock(Clusters.class);
    when(clusters.getCluster(99L)).thenThrow(new ClusterNotFoundException(99L));
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator("ambari-admin"));

    WebApplicationException error = expectAdvisorError(advisorRequest(99L, List.of("missing")));

    assertEquals(404, error.getResponse().getStatus());
    assertEquals("ADVISOR_TARGET_NOT_FOUND",
        ((Map<?, ?>) error.getResponse().getEntity()).get("code"));
    verify(clusters, never()).hostExists(anyString());
  }

  @Test
  public void rejectsForeignHostsFromEveryRequestRepresentation() throws Exception {
    Clusters clusters = mock(Clusters.class);
    Cluster target = mock(Cluster.class);
    Cluster foreign = mock(Cluster.class);
    ManagedDependencyStackAdvisorPlanner planner = mock(ManagedDependencyStackAdvisorPlanner.class);
    when(clusters.getCluster(11L)).thenReturn(target);
    when(target.getClusterId()).thenReturn(11L);
    when(target.getResourceId()).thenReturn(4L);
    when(target.getDesiredStackVersion()).thenReturn(new StackId("BIGTOP", "3.2.0"));
    when(foreign.getClusterId()).thenReturn(22L);
    when(clusters.hostExists("foreign.example")).thenReturn(true);
    when(clusters.getClustersForHost("foreign.example")).thenReturn(Set.of(foreign));
    initAdvisorProvider(clusters, planner);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterUser("cluster-a-user", 4L));

    List<Map<String, Object>> representations = List.of(
        Map.of("hosts", List.of("foreign.example")),
        Map.of("recommendations/blueprint_cluster_binding/host_groups",
            Set.of(Map.of("hosts", Set.of(Map.of("fqdn", "foreign.example"))))),
        Map.of("recommendations/config_groups",
            Set.of(Map.of("hosts", List.of("foreign.example")))));

    for (Map<String, Object> representation : representations) {
      WebApplicationException error = expectAdvisorError(advisorRequest(11L, representation));
      assertEquals(403, error.getResponse().getStatus());
    }
    verify(target, never()).getHosts();
    verifyNoInteractions(planner);
  }

  @Test
  public void resolvesLegacyRequestToItsSingleAuthorizedCluster() throws Exception {
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getResourceId()).thenReturn(4L);
    when(cluster.getDesiredStackVersion()).thenReturn(new StackId("BIGTOP", "3.2.0"));
    when(clusters.hostExists("a.example")).thenReturn(true);
    when(clusters.getClustersForHost("a.example")).thenReturn(Set.of(cluster));
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterUser("cluster-a-user", 4L));

    StackAdvisorRequest result = provider.prepareStackAdvisorRequest(
        advisorRequest(null, List.of("a.example")));

    assertEquals(Long.valueOf(11L), result.getClusterId());
  }

  @Test
  public void preservesUnmanagedTargetStackRecommendationCompatibility() throws Exception {
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(11L)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getResourceId()).thenReturn(4L);
    when(clusters.hostExists("a.example")).thenReturn(true);
    when(clusters.getClustersForHost("a.example")).thenReturn(Set.of(cluster));
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterUser("cluster-a-user", 4L));

    StackAdvisorRequest result = provider.prepareStackAdvisorRequest(
        advisorRequest(11L, List.of("a.example")));

    assertEquals("BIGTOP", result.getStackName());
    assertEquals("3.2.0", result.getStackVersion());
    verify(cluster, never()).getDesiredStackVersion();
  }

  @Test
  public void permitsAuthorizedCreationWithRegisteredUnassignedHosts() throws Exception {
    Clusters clusters = mock(Clusters.class);
    when(clusters.hostExists("new.example")).thenReturn(true);
    when(clusters.getClustersForHost("new.example")).thenReturn(Set.of());
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator("ambari-admin"));

    StackAdvisorRequest result = provider.prepareStackAdvisorRequest(
        advisorRequest(null, List.of("new.example")));

    assertNull(result.getClusterId());
  }

  @Test
  public void autoCompleteRejectsStatusOnlyPrincipalBeforeConfigReads() throws Exception {
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(11L)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getResourceId()).thenReturn(4L);
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class));
    SecurityContextHolder.getContext().setAuthentication(limitedClusterUser(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO));

    WebApplicationException error = expectAdvisorError(autoCompleteRequest(11L));

    assertEquals(403, error.getResponse().getStatus());
    verify(cluster, never()).getHosts();
    verify(cluster, never()).getServices();
    verify(cluster, never()).getService(anyString());
    verify(cluster, never()).getConfigGroupsById(6L);
  }

  @Test
  public void autoCompleteAllowsReadOnlyConfigPrincipal() throws Exception {
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    Service service = mock(Service.class);
    ServiceInfo serviceInfo = mock(ServiceInfo.class);
    ConfigGroup configGroup = mock(ConfigGroup.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    when(clusters.getCluster(11L)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getResourceId()).thenReturn(4L);
    when(cluster.getHosts()).thenReturn(List.of());
    when(cluster.getServices()).thenReturn(Map.of("HBASE", service));
    when(cluster.getServiceComponentHosts()).thenReturn(List.of());
    when(cluster.getService("HBASE")).thenReturn(service);
    when(service.getDesiredStackId()).thenReturn(new StackId("BIGTOP", "3.2.0"));
    when(metaInfo.getService("BIGTOP", "3.2.0", "HBASE")).thenReturn(serviceInfo);
    when(serviceInfo.getConfigDependenciesWithComponents()).thenReturn(List.of());
    when(cluster.getDesiredConfigs()).thenReturn(Map.of());
    when(cluster.getConfigGroupsById(6L)).thenReturn(configGroup);
    when(configGroup.getConfigurations()).thenReturn(Map.of());
    when(configGroup.getHosts()).thenReturn(Map.of());
    initAdvisorProvider(clusters, mock(ManagedDependencyStackAdvisorPlanner.class), metaInfo);
    SecurityContextHolder.getContext().setAuthentication(limitedClusterUser(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO, RoleAuthorization.CLUSTER_VIEW_CONFIGS));

    StackAdvisorRequest result = provider.prepareStackAdvisorRequest(autoCompleteRequest(11L));

    assertEquals(Long.valueOf(11L), result.getClusterId());
    verify(cluster).getDesiredConfigs();
    verify(cluster).getConfigGroupsById(6L);
  }

  private Request advisorRequest(Long clusterId, List<String> hosts) {
    return advisorRequest(clusterId, Map.of("hosts", hosts));
  }

  private Request advisorRequest(Long clusterId, Map<String, Object> hostProperties) {
    Map<String, Object> properties = new HashMap<>(hostProperties);
    if (clusterId != null) {
      properties.put("clusterId", Long.toString(clusterId));
    }
    properties.put("Versions/stack_name", "BIGTOP");
    properties.put("Versions/stack_version", "3.2.0");
    properties.put("recommend", "configurations");
    properties.put("services", List.of("HBASE"));
    Request request = mock(Request.class);
    doReturn(Set.of(properties)).when(request).getProperties();
    return request;
  }

  private Request autoCompleteRequest(long clusterId) {
    HashMap<String, Object> configGroup = new HashMap<>();
    configGroup.put("group_id", "6");
    HashSet<HashMap<String, Object>> configGroups = new HashSet<>();
    configGroups.add(configGroup);
    return createMockRequest(
        "clusterId", Long.toString(clusterId),
        "serviceName", "HBASE",
        "autoComplete", "true",
        "Versions/stack_name", "BIGTOP",
        "Versions/stack_version", "3.2.0",
        "recommend", "configurations",
        "recommendations/config_groups", configGroups);
  }

  private WebApplicationException expectAdvisorError(Request request) throws Exception {
    try {
      provider.createResources(request);
      fail("Expected Stack Advisor request to be rejected");
      return null;
    } catch (WebApplicationException e) {
      return e;
    }
  }

  private void initAdvisorProvider(Clusters clusters,
      ManagedDependencyStackAdvisorPlanner planner) {
    initAdvisorProvider(clusters, planner, mock(AmbariMetaInfo.class));
  }

  private void initAdvisorProvider(Clusters clusters,
      ManagedDependencyStackAdvisorPlanner planner, AmbariMetaInfo metaInfo) {
    StackAdvisorResourceProvider.init(mock(StackAdvisorHelper.class), mock(Configuration.class),
        clusters, metaInfo, planner);
  }

  private Authentication limitedClusterUser(RoleAuthorization... authorizations) {
    Authentication authentication = TestAuthenticationFactory.createClusterUser(
        "limited-cluster-user", 4L);
    AmbariGrantedAuthority authority = (AmbariGrantedAuthority) authentication
        .getAuthorities().iterator().next();
    PermissionEntity permission = authority.getPrivilegeEntity().getPermission();
    permission.getAuthorizations().clear();
    permission.addAuthorizations(Arrays.asList(authorizations));
    return authentication;
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Before
  public void init() {
    provider = createRecommendationResourceProvider();
  }
}
