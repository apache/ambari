/**
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

package org.apache.ambari.server.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.alert.Scope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class OrmTestHelper {

  @Inject
  public Provider<EntityManager> entityManagerProvider;

  @Inject
  public Injector injector;

  @Inject
  public UserDAO userDAO;

  @Inject
  public AlertDefinitionDAO alertDefinitionDAO;

  @Inject
  public AlertDispatchDAO alertDispatchDAO;

  @Inject
  public AlertsDAO alertsDAO;

  public EntityManager getEntityManager() {
    return entityManagerProvider.get();
  }

  /**
   * creates some test data
   */
  @Transactional
  public void createDefaultData() {

    ResourceTypeEntity resourceTypeEntity =  new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setClusterInfo("test_cluster_info1");

    HostEntity host1 = new HostEntity();
    HostEntity host2 = new HostEntity();
    HostEntity host3 = new HostEntity();

    host1.setHostName("test_host1");
    host2.setHostName("test_host2");
    host3.setHostName("test_host3");
    host1.setIpv4("192.168.0.1");
    host2.setIpv4("192.168.0.2");
    host3.setIpv4("192.168.0.3");

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    hostEntities.add(host1);
    hostEntities.add(host2);

    clusterEntity.setHostEntities(hostEntities);

    // both sides of relation should be set when modifying in runtime
    host1.setClusterEntities(Arrays.asList(clusterEntity));
    host2.setClusterEntities(Arrays.asList(clusterEntity));

    HostStateEntity hostStateEntity1 = new HostStateEntity();
    hostStateEntity1.setCurrentState(HostState.HEARTBEAT_LOST);
    hostStateEntity1.setHostEntity(host1);
    HostStateEntity hostStateEntity2 = new HostStateEntity();
    hostStateEntity2.setCurrentState(HostState.HEALTHY);
    hostStateEntity2.setHostEntity(host2);
    host1.setHostStateEntity(hostStateEntity1);
    host2.setHostStateEntity(hostStateEntity2);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setServiceName("HDFS");
    clusterServiceEntity.setClusterEntity(clusterEntity);
    List<ClusterServiceEntity> clusterServiceEntities = new ArrayList<ClusterServiceEntity>();
    clusterServiceEntities.add(clusterServiceEntity);
    clusterEntity.setClusterServiceEntities(clusterServiceEntities);

    getEntityManager().persist(host1);
    getEntityManager().persist(host2);
    getEntityManager().persist(resourceTypeEntity);
    getEntityManager().persist(resourceEntity);
    getEntityManager().persist(clusterEntity);
    getEntityManager().persist(hostStateEntity1);
    getEntityManager().persist(hostStateEntity2);
    getEntityManager().persist(clusterServiceEntity);
  }

  @Transactional
  public void createTestUsers() {
    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setName(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME);
    getEntityManager().persist(principalTypeEntity);

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);

    getEntityManager().persist(principalEntity);

    PasswordEncoder encoder = injector.getInstance(PasswordEncoder.class);

    UserEntity admin = new UserEntity();
    admin.setUserName("administrator");
    admin.setUserPassword(encoder.encode("admin"));
    admin.setPrincipal(principalEntity);

    Set<UserEntity> users = new HashSet<UserEntity>();

    users.add(admin);

    userDAO.create(admin);

    principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    getEntityManager().persist(principalEntity);

    UserEntity userWithoutRoles = new UserEntity();
    userWithoutRoles.setUserName("userWithoutRoles");
    userWithoutRoles.setUserPassword(encoder.encode("test"));
    userWithoutRoles.setPrincipal(principalEntity);
    userDAO.create(userWithoutRoles);

  }

  @Transactional
  public void performTransactionMarkedForRollback() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterDAO.removeByName("test_cluster1");
    getEntityManager().getTransaction().setRollbackOnly();
  }

  @Transactional
  public void createStageCommands() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(1L);
    requestEntity.setClusterId(clusterDAO.findByName("test_cluster1").getClusterId());

    StageEntity stageEntity = new StageEntity();
    stageEntity.setRequest(requestEntity);
    stageEntity.setClusterId(clusterDAO.findByName("test_cluster1").getClusterId());
    stageEntity.setRequestId(1L);
    stageEntity.setStageId(1L);

    requestEntity.setStages(Collections.singletonList(stageEntity));

    HostRoleCommandEntity commandEntity = new HostRoleCommandEntity();
    HostRoleCommandEntity commandEntity2 = new HostRoleCommandEntity();
    HostRoleCommandEntity commandEntity3 = new HostRoleCommandEntity();
    HostEntity host1 = hostDAO.findByName("test_host1");
    HostEntity host2 = hostDAO.findByName("test_host2");
    commandEntity.setHost(host1);
    host1.getHostRoleCommandEntities().add(commandEntity);
    commandEntity.setHostName("test_host1");
    commandEntity.setRoleCommand(RoleCommand.INSTALL);
    commandEntity.setStatus(HostRoleStatus.QUEUED);
    commandEntity.setRole(Role.DATANODE);
    commandEntity2.setHost(host2);
    host2.getHostRoleCommandEntities().add(commandEntity2);
    commandEntity2.setRoleCommand(RoleCommand.EXECUTE);
    commandEntity2.setRole(Role.NAMENODE);
    commandEntity2.setStatus(HostRoleStatus.COMPLETED);
    commandEntity3.setHost(host1);
    host1.getHostRoleCommandEntities().add(commandEntity3);
    commandEntity3.setRoleCommand(RoleCommand.START);
    commandEntity3.setRole(Role.SECONDARY_NAMENODE);
    commandEntity3.setStatus(HostRoleStatus.IN_PROGRESS);
    commandEntity.setStage(stageEntity);
    commandEntity2.setStage(stageEntity);
    commandEntity3.setStage(stageEntity);

    stageEntity.setHostRoleCommands(new ArrayList<HostRoleCommandEntity>());
    stageEntity.getHostRoleCommands().add(commandEntity);
    stageEntity.getHostRoleCommands().add(commandEntity2);
    stageEntity.getHostRoleCommands().add(commandEntity3);

    requestDAO.create(requestEntity);
    stageDAO.create(stageEntity);
    hostRoleCommandDAO.create(commandEntity3);
    hostRoleCommandDAO.create(commandEntity);
    hostRoleCommandDAO.create(commandEntity2);
    hostDAO.merge(host1);
    hostDAO.merge(host2);
  }

  /**
   * Creates an empty cluster with an ID.
   *
   * @return the cluster ID.
   */
  @Transactional
  public Long createCluster() {
    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);

    ResourceTypeEntity resourceTypeEntity =  new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);

    clusterDAO.create(clusterEntity);

    clusterEntity = clusterDAO.findByName(clusterEntity.getClusterName());
    Assert.notNull(clusterEntity);
    Assert.isTrue(clusterEntity.getClusterId() > 0);
    return clusterEntity.getClusterId();
  }

  /**
   * Creates an alert target.
   *
   * @return
   */
  @Transactional
  public AlertTargetEntity createAlertTarget() throws Exception {
    AlertTargetEntity target = new AlertTargetEntity();
    target.setDescription("Target Description");
    target.setNotificationType("EMAIL");
    target.setProperties("Target Properties");
    target.setTargetName("Target Name " + System.currentTimeMillis());

    alertDispatchDAO.create(target);
    return alertDispatchDAO.findTargetById(target.getTargetId());
  }

  /**
   * Creates an alert definition.
   *
   * @param clusterId
   * @return
   * @throws Exception
   */
  @Transactional
  public AlertDefinitionEntity createAlertDefinition(long clusterId)
      throws Exception {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("Alert Definition "
        + System.currentTimeMillis());
    definition.setServiceName("Service " + System.currentTimeMillis());
    definition.setComponentName(null);
    definition.setClusterId(clusterId);
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(60);
    definition.setScope(Scope.SERVICE);
    definition.setSource("Source " + System.currentTimeMillis());
    definition.setSourceType("SCRIPT");

    alertDefinitionDAO.create(definition);
    return alertDefinitionDAO.findById(definition.getDefinitionId());
  }

  /**
   * Creates an alert group.
   *
   * @param clusterId
   * @param targets
   * @return
   * @throws Exception
   */
  @Transactional
  public AlertGroupEntity createAlertGroup(long clusterId,
      Set<AlertTargetEntity> targets) throws Exception {
    AlertGroupEntity group = new AlertGroupEntity();
    group.setDefault(false);
    group.setGroupName("Group Name " + System.currentTimeMillis());
    group.setClusterId(clusterId);
    group.setAlertTargets(targets);

    alertDispatchDAO.create(group);
    return alertDispatchDAO.findGroupById(group.getGroupId());
  }
}
