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
package org.apache.ambari.server.service.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Provider;

public class ManagedMetricsIdentityServiceTest {
  private static final long CLUSTER_ID = 42L;
  private static final String USERNAME = "ambari-metrics-sd-42";
  private static final String PASSWORD = "GeneratedMetricsPassword123";

  @Test
  public void testManagedModeCreatesIdentityPrivilegeAndDesiredConfig() throws Exception {
    Harness harness = new Harness(Map.of(
        ManagedMetricsIdentityService.MANAGED_PROPERTY, "true",
        ManagedMetricsIdentityService.USERNAME_PROPERTY, "prometheus-scraper",
        ManagedMetricsIdentityService.PASSWORD_PROPERTY, ""));
    UserEntity user = harness.user();
    when(harness.users.getUserEntity(USERNAME)).thenReturn(null, user);
    when(harness.users.createUser(USERNAME, USERNAME,
        "Ambari Metrics service discovery", true)).thenReturn(user);
    when(harness.passwordHelper.createSecurePassword(32, 4, 4, 4, 0, 0))
        .thenReturn(PASSWORD);
    when(harness.privilegeDAO.exists(any(PrivilegeEntity.class))).thenReturn(false);
    ArgumentCaptor<Map<String, String>> properties = ArgumentCaptor.forClass(Map.class);
    when(harness.configFactory.createNew(eq(harness.stackId), eq(harness.cluster),
        eq(ManagedMetricsIdentityService.CONFIG_TYPE), anyString(), properties.capture(),
        eq(Map.of()))).thenReturn(harness.managedConfig);

    harness.service.provision(CLUSTER_ID);

    verify(harness.users).addLocalAuthentication(user, PASSWORD);
    verify(harness.privilegeDAO).create(any(PrivilegeEntity.class));
    verify(harness.cluster).addConfig(harness.managedConfig);
    verify(harness.cluster).addDesiredConfig(eq("ambari-server"),
        eq(Set.of(harness.managedConfig)), anyString());
    Assert.assertEquals(USERNAME,
        properties.getValue().get(ManagedMetricsIdentityService.USERNAME_PROPERTY));
    Assert.assertEquals(PASSWORD,
        properties.getValue().get(ManagedMetricsIdentityService.PASSWORD_PROPERTY));
  }

  @Test
  public void testManagedModeIsIdempotentWhenIdentityAlreadyExists() throws Exception {
    Harness harness = new Harness(Map.of(
        ManagedMetricsIdentityService.MANAGED_PROPERTY, "true",
        ManagedMetricsIdentityService.USERNAME_PROPERTY, USERNAME,
        ManagedMetricsIdentityService.PASSWORD_PROPERTY, PASSWORD));
    UserEntity user = harness.user();
    UserAuthenticationEntity authentication = mock(UserAuthenticationEntity.class);
    when(harness.users.getUserEntity(USERNAME)).thenReturn(user);
    when(harness.users.getUserAuthenticationEntities(user, UserAuthenticationType.LOCAL))
        .thenReturn(Set.of(authentication));
    when(authentication.getAuthenticationKey()).thenReturn("matching-password-hash");
    when(harness.passwordEncoder.matches(PASSWORD, "matching-password-hash")).thenReturn(true);
    when(harness.privilegeDAO.exists(any(PrivilegeEntity.class))).thenReturn(true);

    harness.service.provision(CLUSTER_ID);

    verify(harness.users).setUserActive(user, true);
    verify(harness.users, never()).addLocalAuthentication(any(UserEntity.class), anyString());
    verifyNoInteractions(harness.configFactory);
    verify(harness.privilegeDAO, never()).create(any(PrivilegeEntity.class));
    verify(harness.cluster, never()).addConfig(any(Config.class));
  }

  @Test
  public void testManagedModeRepairsDriftedLocalPassword() throws Exception {
    Harness harness = new Harness(Map.of(
        ManagedMetricsIdentityService.MANAGED_PROPERTY, "true",
        ManagedMetricsIdentityService.USERNAME_PROPERTY, USERNAME,
        ManagedMetricsIdentityService.PASSWORD_PROPERTY, PASSWORD));
    UserEntity user = harness.user();
    UserAuthenticationEntity authentication = mock(UserAuthenticationEntity.class);
    when(harness.users.getUserEntity(USERNAME)).thenReturn(user);
    when(harness.users.getUserAuthenticationEntities(user, UserAuthenticationType.LOCAL))
        .thenReturn(Set.of(authentication));
    when(authentication.getUserAuthenticationId()).thenReturn(17L);
    when(authentication.getAuthenticationKey()).thenReturn("stale-password-hash");
    when(harness.passwordEncoder.matches(PASSWORD, "stale-password-hash")).thenReturn(false);
    when(harness.privilegeDAO.exists(any(PrivilegeEntity.class))).thenReturn(true);

    harness.service.provision(CLUSTER_ID);

    verify(harness.users).removeAuthentication(USERNAME, 17L);
    verify(harness.users).addLocalAuthentication(user, PASSWORD);
    verifyNoInteractions(harness.configFactory);
  }

  @Test
  public void testPendingConfigIsUpdatedBeforeItBecomesDesired() throws Exception {
    Harness harness = new Harness(Map.of(
        ManagedMetricsIdentityService.MANAGED_PROPERTY, "true",
        ManagedMetricsIdentityService.USERNAME_PROPERTY, "prometheus-scraper",
        ManagedMetricsIdentityService.PASSWORD_PROPERTY, ""));
    UserEntity user = harness.user();
    when(harness.clusters.getCluster("test-cluster")).thenReturn(harness.cluster);
    when(harness.cluster.getConfig(ManagedMetricsIdentityService.CONFIG_TYPE, "INITIAL"))
        .thenReturn(harness.currentConfig);
    when(harness.users.getUserEntity(USERNAME)).thenReturn(null, user);
    when(harness.users.createUser(USERNAME, USERNAME,
        "Ambari Metrics service discovery", true)).thenReturn(user);
    when(harness.passwordHelper.createSecurePassword(32, 4, 4, 4, 0, 0))
        .thenReturn(PASSWORD);

    harness.service.provisionPendingConfig("test-cluster", "INITIAL");

    verify(harness.currentConfig).setProperties(Map.of(
        ManagedMetricsIdentityService.MANAGED_PROPERTY, "true",
        ManagedMetricsIdentityService.USERNAME_PROPERTY, USERNAME,
        ManagedMetricsIdentityService.PASSWORD_PROPERTY, PASSWORD));
    verify(harness.currentConfig).save();
    verifyNoInteractions(harness.configFactory);
  }

  @Test
  public void testManualModeDeactivatesManagedIdentity() throws Exception {
    Harness harness = new Harness(Map.of(
        ManagedMetricsIdentityService.MANAGED_PROPERTY, "false",
        ManagedMetricsIdentityService.USERNAME_PROPERTY, "operator-managed"));
    UserEntity user = mock(UserEntity.class);
    when(harness.users.getUserEntity(USERNAME)).thenReturn(user);

    harness.service.provision(CLUSTER_ID);

    verify(harness.users).setUserActive(user, false);
    verifyNoInteractions(harness.configFactory, harness.permissionDAO,
        harness.privilegeDAO, harness.resourceDAO, harness.principalDAO);
  }

  private static final class Harness {
    private final Provider<Clusters> clustersProvider = mock(Provider.class);
    private final Clusters clusters = mock(Clusters.class);
    private final Cluster cluster = mock(Cluster.class);
    private final Config currentConfig = mock(Config.class);
    private final Config managedConfig = mock(Config.class);
    private final ConfigFactory configFactory = mock(ConfigFactory.class);
    private final Users users = mock(Users.class);
    private final SecurePasswordHelper passwordHelper = mock(SecurePasswordHelper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final PermissionDAO permissionDAO = mock(PermissionDAO.class);
    private final PrivilegeDAO privilegeDAO = mock(PrivilegeDAO.class);
    private final ResourceDAO resourceDAO = mock(ResourceDAO.class);
    private final PrincipalDAO principalDAO = mock(PrincipalDAO.class);
    private final StackId stackId = new StackId("BIGTOP", "3.2.0");
    private final ManagedMetricsIdentityService service;

    private Harness(Map<String, String> properties) throws Exception {
      when(clustersProvider.get()).thenReturn(clusters);
      when(clusters.getCluster(CLUSTER_ID)).thenReturn(cluster);
      when(cluster.getClusterId()).thenReturn(CLUSTER_ID);
      when(cluster.getResourceId()).thenReturn(7L);
      when(cluster.getServices()).thenReturn(Map.of("VICTORIAMETRICS", mock(Service.class)));
      when(cluster.getDesiredConfigByType(ManagedMetricsIdentityService.CONFIG_TYPE))
          .thenReturn(currentConfig);
      when(currentConfig.getProperties()).thenReturn(properties);
      when(currentConfig.getPropertiesAttributes()).thenReturn(Map.of());
      when(currentConfig.getStackId()).thenReturn(stackId);
      service = new ManagedMetricsIdentityService(clustersProvider, configFactory, users,
          passwordHelper, passwordEncoder, permissionDAO, privilegeDAO, resourceDAO, principalDAO);
    }

    private UserEntity user() {
      UserEntity user = mock(UserEntity.class);
      PrincipalEntity principal = new PrincipalEntity();
      PermissionEntity permission = mock(PermissionEntity.class);
      ResourceEntity resource = mock(ResourceEntity.class);
      when(user.getPrincipal()).thenReturn(principal);
      when(permissionDAO.findClusterReadPermission()).thenReturn(permission);
      when(resourceDAO.findById(7L)).thenReturn(resource);
      return user;
    }
  }
}
