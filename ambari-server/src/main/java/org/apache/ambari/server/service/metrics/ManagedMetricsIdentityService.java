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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/** Provisions the cluster-scoped account used by managed Metrics HTTP service discovery. */
@Singleton
public class ManagedMetricsIdentityService {
  static final String CONFIG_TYPE = "victoriametrics-scrape";
  static final String MANAGED_PROPERTY = "managed_discovery_identity";
  static final String USERNAME_PROPERTY = "ambari_sd_username";
  static final String PASSWORD_PROPERTY = "ambari_sd_password";

  private static final String SERVICE_NAME = "VICTORIAMETRICS";
  private static final String USERNAME_PREFIX = "ambari-metrics-sd-";
  private static final String DISPLAY_NAME = "Ambari Metrics service discovery";
  private static final String SYSTEM_USER = "ambari-server";

  private final Provider<Clusters> clusters;
  private final ConfigFactory configFactory;
  private final Users users;
  private final SecurePasswordHelper securePasswordHelper;
  private final PasswordEncoder passwordEncoder;
  private final PermissionDAO permissionDAO;
  private final PrivilegeDAO privilegeDAO;
  private final ResourceDAO resourceDAO;
  private final PrincipalDAO principalDAO;

  @Inject
  public ManagedMetricsIdentityService(Provider<Clusters> clusters, ConfigFactory configFactory,
      Users users, SecurePasswordHelper securePasswordHelper, PasswordEncoder passwordEncoder,
      PermissionDAO permissionDAO, PrivilegeDAO privilegeDAO, ResourceDAO resourceDAO,
      PrincipalDAO principalDAO) {
    this.clusters = clusters;
    this.configFactory = configFactory;
    this.users = users;
    this.securePasswordHelper = securePasswordHelper;
    this.passwordEncoder = passwordEncoder;
    this.permissionDAO = permissionDAO;
    this.privilegeDAO = privilegeDAO;
    this.resourceDAO = resourceDAO;
    this.principalDAO = principalDAO;
  }

  @Transactional
  public synchronized void provision(long clusterId) throws AmbariException {
    provision(clusters.get().getCluster(clusterId));
  }

  @Transactional
  public synchronized void provision(String clusterName) throws AmbariException {
    provision(clusters.get().getCluster(clusterName));
  }

  @Transactional
  public synchronized void provisionPendingConfig(String clusterName, String configTag)
      throws AmbariException {
    Cluster cluster = clusters.get().getCluster(clusterName);
    Config config = cluster.getConfig(CONFIG_TYPE, configTag);
    if (config != null) {
      provision(cluster, config, true);
    }
  }

  private void provision(Cluster cluster) throws AmbariException {
    provision(cluster, cluster.getDesiredConfigByType(CONFIG_TYPE), false);
  }

  private void provision(Cluster cluster, Config current, boolean updateInPlace)
      throws AmbariException {
    String username = USERNAME_PREFIX + cluster.getClusterId();
    Service service = cluster.getServices().get(SERVICE_NAME);
    if (service == null || current == null) {
      deactivateUser(username);
      return;
    }

    Map<String, String> currentProperties = current.getProperties();
    if (!Boolean.parseBoolean(currentProperties.getOrDefault(MANAGED_PROPERTY, "true"))) {
      deactivateUser(username);
      return;
    }

    String configuredUsername = currentProperties.get(USERNAME_PROPERTY);
    String configuredPassword = currentProperties.get(PASSWORD_PROPERTY);
    boolean credentialsChanged = !username.equals(configuredUsername)
        || configuredPassword == null || configuredPassword.isBlank();
    String password = credentialsChanged ? generatePassword(username) : configuredPassword;

    ensureUser(username, password, credentialsChanged);
    ensureClusterReadPrivilege(cluster, users.getUserEntity(username));

    if (credentialsChanged) {
      Map<String, String> properties = new HashMap<>(currentProperties);
      properties.put(USERNAME_PROPERTY, username);
      properties.put(PASSWORD_PROPERTY, password);
      if (updateInPlace) {
        current.setProperties(properties);
        current.save();
        return;
      }
      String tag = "managed-metrics-identity-" + UUID.randomUUID();
      Config managedConfig = configFactory.createNew(current.getStackId(), cluster, CONFIG_TYPE,
          tag, properties, current.getPropertiesAttributes());
      cluster.addConfig(managedConfig);
      cluster.addDesiredConfig(SYSTEM_USER, Set.of(managedConfig),
          "Provision managed Metrics service discovery identity");
    }
  }

  private void deactivateUser(String username) throws AmbariException {
    UserEntity user = users.getUserEntity(username);
    if (user != null) {
      users.setUserActive(user, false);
    }
  }

  private String generatePassword(String username) throws AmbariException {
    for (int attempt = 0; attempt < 100; attempt++) {
      int minimumPunctuation = attempt % 2;
      String password = securePasswordHelper.createSecurePassword(
          32, 4, 4, 4, minimumPunctuation, 0);
      try {
        users.validatePassword(password, username);
        return password;
      } catch (IllegalArgumentException ignored) {
        // Try another secure value that satisfies the configured Ambari password policy.
      }
    }
    throw new AmbariException(
        "Unable to generate a managed Metrics password that satisfies the Ambari password policy");
  }

  private void ensureUser(String username, String password, boolean rotatePassword)
      throws AmbariException {
    UserEntity user = users.getUserEntity(username);
    if (user == null) {
      user = users.createUser(username, username, DISPLAY_NAME, true);
      users.addLocalAuthentication(user, password);
      return;
    }

    users.setUserActive(user, true);
    ArrayList<UserAuthenticationEntity> localAuthentications = new ArrayList<>(
        users.getUserAuthenticationEntities(user, UserAuthenticationType.LOCAL));
    boolean passwordMatches = localAuthentications.stream().anyMatch(
        authentication -> passwordEncoder.matches(password, authentication.getAuthenticationKey()));
    if (rotatePassword || !passwordMatches) {
      for (UserAuthenticationEntity authentication : localAuthentications) {
        users.removeAuthentication(username, authentication.getUserAuthenticationId());
      }
      user = users.getUserEntity(username);
      users.addLocalAuthentication(user, password);
    }
  }

  private void ensureClusterReadPrivilege(Cluster cluster, UserEntity user) throws AmbariException {
    PermissionEntity permission = permissionDAO.findClusterReadPermission();
    ResourceEntity resource = resourceDAO.findById(cluster.getResourceId());
    if (permission == null || resource == null || user == null) {
      throw new AmbariException(
          "Unable to resolve the managed Metrics user, cluster resource, or read-only permission");
    }

    PrivilegeEntity privilege = new PrivilegeEntity();
    privilege.setPermission(permission);
    privilege.setPrincipal(user.getPrincipal());
    privilege.setResource(resource);
    if (!privilegeDAO.exists(privilege)) {
      privilegeDAO.create(privilege);
      user.getPrincipal().getPrivileges().add(privilege);
      principalDAO.merge(user.getPrincipal());
    }
  }
}
