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

package org.apache.ambari.server.upgrade;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.KeyValueEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.ambari.server.upgrade.UpgradeCatalog170.CLUSTER_STATE_STACK_HDP_2_1;
import static org.apache.ambari.server.upgrade.UpgradeCatalog170.JOBS_VIEW_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog170.SHOW_JOBS_FOR_NON_ADMIN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog170.YARN_RESOURCEMANAGER_WEBAPP_ADDRESS_PROPERTY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog170.YARN_SITE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog170.YARN_TIMELINE_SERVICE_WEBAPP_ADDRESS_PROPERTY;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * UpgradeCatalog170 unit tests.
 */
public class UpgradeCatalog170Test {

  private Injector injector;
  private final String CLUSTER_NAME = "c1";
  private final String SERVICE_NAME = "HDFS";
  private final String HOST_NAME = "h1";
  private final String DESIRED_STACK_VERSION = "{\"stackName\":\"HDP\",\"stackVersion\":\"2.0.6\"}";

  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createNiceMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  private ClusterEntity createCluster() {
    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
      resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1L);
    clusterEntity.setClusterName(CLUSTER_NAME);
    clusterEntity.setDesiredStackVersion(DESIRED_STACK_VERSION);
    clusterEntity.setProvisioningState(State.INIT);
    clusterEntity.setResource(resourceEntity);
    clusterDAO.create(clusterEntity);
    return clusterEntity;
  }

  private ClusterServiceEntity createService(ClusterEntity clusterEntity, String serviceName) {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterId(1L);
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceName(serviceName);
    clusterServiceDAO.create(clusterServiceEntity);
    return clusterServiceEntity;
  }

  private ClusterServiceEntity addService(ClusterEntity clusterEntity, String serviceName) {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);

    ClusterServiceEntity clusterServiceEntity = createService(clusterEntity, serviceName);

    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredStackVersion(DESIRED_STACK_VERSION);
    serviceDesiredStateEntity.setClusterId(1L);
    serviceDesiredStateEntity.setServiceName(serviceName);
    serviceDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);

    clusterServiceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);
    clusterEntity.getClusterServiceEntities().add(clusterServiceEntity);

    clusterDAO.merge(clusterEntity);

    return clusterServiceEntity;
  }


  private HostEntity createHost(ClusterEntity clusterEntity) {
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(HOST_NAME);
    hostEntity.setClusterEntities(Collections.singletonList(clusterEntity));
    hostDAO.create(hostEntity);
    clusterEntity.getHostEntities().add(hostEntity);
    clusterDAO.merge(clusterEntity);
    return hostEntity;
  }

  @Transactional
  private void addComponent(ClusterEntity clusterEntity, ClusterServiceEntity clusterServiceEntity, HostEntity hostEntity, String componentName) {
    ServiceComponentDesiredStateEntity componentDesiredStateEntity = new ServiceComponentDesiredStateEntity();
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setComponentName(componentName);
    componentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    componentDesiredStateEntity.setDesiredStackVersion(DESIRED_STACK_VERSION);
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setClusterId(clusterServiceEntity.getClusterId());

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentDesiredStateEntity hostComponentDesiredStateEntity = new HostComponentDesiredStateEntity();
    hostComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    hostComponentDesiredStateEntity.setHostName(hostEntity.getHostName());
    hostComponentDesiredStateEntity.setComponentName(componentName);
    hostComponentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    hostComponentDesiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    hostComponentDesiredStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);
    hostComponentDesiredStateEntity.setHostEntity(hostEntity);
    hostComponentDesiredStateDAO.create(hostComponentDesiredStateEntity);


    HostComponentStateEntity hostComponentStateEntity = new HostComponentStateEntity();
    hostComponentStateEntity.setHostEntity(hostEntity);
    hostComponentStateEntity.setHostName(hostEntity.getHostName());
    hostComponentStateEntity.setComponentName(componentName);
    hostComponentStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    hostComponentStateEntity.setClusterId(clusterEntity.getClusterId());
    hostComponentStateEntity.setCurrentStackVersion(clusterEntity.getDesiredStackVersion());
    hostComponentStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);

    componentDesiredStateEntity.setHostComponentStateEntities(Collections.singletonList(hostComponentStateEntity));
    componentDesiredStateEntity.setHostComponentDesiredStateEntities(Collections.singletonList(hostComponentDesiredStateEntity));

    hostEntity.getHostComponentStateEntities().add(hostComponentStateEntity);
    hostEntity.getHostComponentDesiredStateEntities().add(hostComponentDesiredStateEntity);

    clusterServiceEntity.getServiceComponentDesiredStateEntities().add(componentDesiredStateEntity);

    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    serviceComponentDesiredStateDAO.merge(componentDesiredStateEntity);
    hostDAO.merge(hostEntity);
    clusterServiceDAO.merge(clusterServiceEntity);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement stmt = createNiceMock(PreparedStatement.class);
    Configuration configuration = createNiceMock(Configuration.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
    expect(dbAccessor.getNewConnection()).andReturn(connection);
    expect(connection.prepareStatement("SELECT config_id FROM clusterconfig " +
      "WHERE type_name = ? ORDER BY create_timestamp")).andReturn(stmt);
    expect(connection.prepareStatement("UPDATE clusterconfig SET version = ? " +
      "WHERE config_id = ?")).andReturn(stmt);
    stmt.close();
    expectLastCall().times(2);
    connection.close();
    expectLastCall();

    Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> maskColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> systemColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> valueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> maskedColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> stageCommandParamsColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> stageHostParamsColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> groupsCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertDefinitionColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertHistoryColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertCurrentColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertGroupColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertTargetCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertGroupTargetCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertGroupingCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertNoticeCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> serviceConfigCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> serviceConfigMappingCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<DBAccessor.DBColumnInfo> configDataClusterConfigCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> configDataBlueprintConfigurationCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> configDataHostGroupConfigurationCapture = new Capture<DBAccessor.DBColumnInfo>();

    dbAccessor.createTable(eq("groups"),
        capture(groupsCapture), eq("group_id"));

    setViewExpectations(dbAccessor, maskColumnCapture, systemColumnCapture);
    setViewParameterExpectations(dbAccessor, maskedColumnCapture);
    setViewInstancePropertyExpectations(dbAccessor, valueColumnCapture);
    setViewInstanceDataExpectations(dbAccessor, dataValueColumnCapture);
    setClusterConfigExpectations(dbAccessor, clusterConfigAttributesColumnCapture);
    setStageExpectations(dbAccessor, stageCommandParamsColumnCapture, stageHostParamsColumnCapture);

    dbAccessor.createTable(eq("alert_definition"),
        capture(alertDefinitionColumnCapture), eq("definition_id"));

    dbAccessor.createTable(eq("alert_history"),
      capture(alertHistoryColumnCapture), eq("alert_id"));

    dbAccessor.createTable(eq("alert_current"),
      capture(alertCurrentColumnCapture), eq("alert_id"));

    dbAccessor.createTable(eq("alert_group"), capture(alertGroupColumnCapture),
      eq("group_id"));

    dbAccessor.createTable(eq("alert_target"), capture(alertTargetCapture),
        eq("target_id"));

    dbAccessor.createTable(eq("alert_group_target"),
        capture(alertGroupTargetCapture), eq("group_id"), eq("target_id"));

    dbAccessor.createTable(eq("alert_grouping"), capture(alertGroupingCapture),
        eq("group_id"), eq("definition_id"));

    dbAccessor.createTable(eq("alert_notice"), capture(alertNoticeCapture),
        eq("notification_id"));

    dbAccessor.alterColumn(eq("clusterconfig"), capture(configDataClusterConfigCapture));
    dbAccessor.alterColumn(eq("blueprint_configuration"), capture(configDataBlueprintConfigurationCapture));
    dbAccessor.alterColumn(eq("hostgroup_configuration"), capture(configDataHostGroupConfigurationCapture));

    dbAccessor.createTable(eq("serviceconfig"), capture(serviceConfigCapture),
        eq("service_config_id"));

    dbAccessor.createTable(eq("serviceconfigmapping"),
        capture(serviceConfigMappingCapture), eq("service_config_id"),
        eq("config_id"));

    dbAccessor.executeSelect(anyObject(String.class));
    expectLastCall().andReturn(resultSet).anyTimes();
    resultSet.next();
    expectLastCall().andReturn(false).anyTimes();
    resultSet.close();
    expectLastCall().anyTimes();

    replay(dbAccessor, configuration, resultSet, connection, stmt);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet, connection, stmt);

    assertClusterConfigColumns(clusterConfigAttributesColumnCapture);
    assertViewColumns(maskColumnCapture, systemColumnCapture);
    assertViewParameterColumns(maskedColumnCapture);
    assertViewInstancePropertyColumns(valueColumnCapture);
    assertViewInstanceDataColumns(dataValueColumnCapture);
    assertStageColumns(stageCommandParamsColumnCapture, stageHostParamsColumnCapture);

    assertEquals(4, groupsCapture.getValue().size());
    List<DBAccessor.DBColumnInfo> columnInfoList = groupsCapture.getValue();
    for (DBAccessor.DBColumnInfo info : columnInfoList) {
      if (info.getName().equals("group_name")) {
        assertEquals(Integer.valueOf(255), info.getLength());
        break;
      }
    }

    assertEquals(12, alertDefinitionColumnCapture.getValue().size());
    assertEquals(11, alertHistoryColumnCapture.getValue().size());
    assertEquals(7, alertCurrentColumnCapture.getValue().size());
    assertEquals(5, alertGroupColumnCapture.getValue().size());
    assertEquals(5, alertTargetCapture.getValue().size());
    assertEquals(2, alertGroupTargetCapture.getValue().size());
    assertEquals(2, alertGroupingCapture.getValue().size());
    assertEquals(5, alertNoticeCapture.getValue().size());
    assertEquals(2, serviceConfigCapture.getValue().size());
    assertEquals(2, serviceConfigMappingCapture.getValue().size());
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Injector injector = createNiceMock(Injector.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Clusters clusters = createStrictMock(Clusters.class);
    Config config = createStrictMock(Config.class);
    Config pigConfig = createStrictMock(Config.class);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    PermissionEntity adminPermission = createNiceMock(PermissionEntity.class);
    PermissionEntity viewUsePermission = createNiceMock(PermissionEntity.class);

    ClusterConfigEntity clusterConfigEntity = createNiceMock(ClusterConfigEntity.class);
    ConfigGroupConfigMappingDAO configGroupConfigMappingDAO = createNiceMock(ConfigGroupConfigMappingDAO.class);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    PrincipalDAO principalDAO = createNiceMock(PrincipalDAO.class);
    PrincipalTypeDAO principalTypeDAO = createNiceMock(PrincipalTypeDAO.class);
    ClusterDAO clusterDAO = createNiceMock(ClusterDAO.class);
    ResourceTypeDAO resourceTypeDAO = createNiceMock(ResourceTypeDAO.class);
    ResourceDAO resourceDAO = createNiceMock(ResourceDAO.class);
    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    PermissionDAO permissionDAO = createNiceMock(PermissionDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
    KeyValueDAO keyValueDAO = createNiceMock(KeyValueDAO.class);

    EntityTransaction trans = createNiceMock(EntityTransaction.class);
    CriteriaBuilder cb = createNiceMock(CriteriaBuilder.class);
    CriteriaQuery<HostRoleCommandEntity> cq = createNiceMock(CriteriaQuery.class);
    Root<HostRoleCommandEntity> hrc = createNiceMock(Root.class);
    Path<Long> taskId = null;
    Path<String> outputLog = null;
    Path<String> errorLog = null;
    Order o = createNiceMock(Order.class);
    TypedQuery<HostRoleCommandEntity> q = createNiceMock(TypedQuery.class);
    List<HostRoleCommandEntity> r = new ArrayList<HostRoleCommandEntity>();
    ResultSet userRolesResultSet = createNiceMock(ResultSet.class);
    ClusterEntity clusterEntity = createNiceMock(ClusterEntity.class);
    ClusterConfigEntity configEntity = createNiceMock(ClusterConfigEntity.class);

    Method m = AbstractUpgradeCatalog.class.getDeclaredMethod
        ("updateConfigurationProperties", String.class, Map.class, boolean.class, boolean.class);
    Method n = AbstractUpgradeCatalog.class.getDeclaredMethod("getEntityManagerProvider");
    Method l = AbstractUpgradeCatalog.class.getDeclaredMethod
        ("addNewConfigurationsFromXml");
    
    UpgradeCatalog170 upgradeCatalog = createMockBuilder(UpgradeCatalog170.class)
      .addMockedMethod(m).addMockedMethod(n).addMockedMethod(l).createMock();

    List<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities =
            new ArrayList<ConfigGroupConfigMappingEntity>();
    ConfigGroupConfigMappingEntity configGroupConfigMappingEntity = new ConfigGroupConfigMappingEntity();
    configGroupConfigMappingEntity.setConfigType(Configuration.GLOBAL_CONFIG_TAG);
    configGroupConfigMappingEntity.setClusterConfigEntity(clusterConfigEntity);
    configGroupConfigMappingEntity.setClusterId(1L);
    configGroupConfigMappingEntities.add(configGroupConfigMappingEntity);

    Map<String, Cluster> clustersMap = new HashMap<String, Cluster>();
    clustersMap.put("c1", cluster);

    Map<String, String> globalConfigs = new HashMap<String, String>();
    globalConfigs.put("prop1", "val1");
    globalConfigs.put("smokeuser_keytab", "val2");

    Map<String, String> pigSettings = new HashMap<String, String>();
    pigSettings.put("pig-content", "foo");

    Set<String> envDicts = new HashSet<String>();
    envDicts.add("hadoop-env");
    envDicts.add("global");

    Set<String> configTypes = new HashSet<String>();
    configTypes.add("hadoop-env");

    Map<String, String> contentOfHadoopEnv = new HashMap<String, String>();
    contentOfHadoopEnv.put("content", "env file contents");

    upgradeCatalog.updateConfigurationProperties("hadoop-env",
        globalConfigs, false, true);
    expectLastCall();

    upgradeCatalog.addNewConfigurationsFromXml();
    expectLastCall();

    expect(dbAccessor.executeSelect("SELECT role_name, user_id FROM user_roles")).andReturn(userRolesResultSet).once();
    expect(entityManager.getTransaction()).andReturn(trans).anyTimes();
    expect(entityManager.getCriteriaBuilder()).andReturn(cb).anyTimes();
    expect(entityManager.createQuery(cq)).andReturn(q).anyTimes();
    expect(trans.isActive()).andReturn(true).anyTimes();
    expect(upgradeCatalog.getEntityManagerProvider()).andReturn(entityManagerProvider).anyTimes();
    expect(cb.createQuery(HostRoleCommandEntity.class)).andReturn(cq).anyTimes();
    expect(cb.desc(taskId)).andReturn(o).anyTimes();
    expect(cq.from(HostRoleCommandEntity.class)).andReturn(hrc).anyTimes();
    expect(cq.select(hrc)).andReturn(cq).anyTimes();
    expect(cq.where(anyObject(Predicate.class))).andReturn(cq).anyTimes();
    expect(hrc.get(isA(SingularAttribute.class))).andReturn(taskId).times(2);
    expect(hrc.get(isA(SingularAttribute.class))).andReturn(outputLog).once();
    expect(hrc.get(isA(SingularAttribute.class))).andReturn(errorLog).once();
    expect(q.setMaxResults(1000)).andReturn(q).anyTimes();
    expect(q.getResultList()).andReturn(r).anyTimes();
    expect(clusterConfigEntity.getData()).andReturn("{\"dtnode_heapsize\":\"1028m\"}");

    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
    expect(injector.getInstance(ConfigHelper.class)).andReturn(configHelper).anyTimes();
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(amc).anyTimes();
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(clustersMap).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(clustersMap.values().iterator().next()).anyTimes();
    expect(clusters.getClusters()).andReturn(clustersMap).times(1);
    expect(cluster.getDesiredConfigByType("global")).andReturn(config).anyTimes();
    expect(cluster.getDesiredConfigByType("oozie-log4j")).andReturn(config).anyTimes();
    expect(cluster.getDesiredConfigByType("oozie-env")).andReturn(config).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L);
    expect(cluster.getNextConfigVersion("hadoop-env")).andReturn(3L);
    expect(config.getProperties()).andReturn(globalConfigs).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.1")).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "prop1", "c1")).andReturn(envDicts).once();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "smokeuser_keytab", "c1")).andReturn(new HashSet<String>()).once();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "content", "c1")).andReturn(envDicts).once();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "dtnode_heapsize", "c1")).andReturn(configTypes).once();

    expect(injector.getInstance(ConfigGroupConfigMappingDAO.class)).andReturn(configGroupConfigMappingDAO).anyTimes();
    expect(injector.getInstance(UserDAO.class)).andReturn(userDAO).anyTimes();
    expect(injector.getInstance(PrincipalDAO.class)).andReturn(principalDAO).anyTimes();
    expect(injector.getInstance(PrincipalTypeDAO.class)).andReturn(principalTypeDAO).anyTimes();
    expect(injector.getInstance(ClusterDAO.class)).andReturn(clusterDAO).anyTimes();
    expect(injector.getInstance(ResourceTypeDAO.class)).andReturn(resourceTypeDAO).anyTimes();
    expect(injector.getInstance(ResourceDAO.class)).andReturn(resourceDAO).anyTimes();
    expect(injector.getInstance(ViewDAO.class)).andReturn(viewDAO).anyTimes();
    expect(injector.getInstance(ViewInstanceDAO.class)).andReturn(viewInstanceDAO).anyTimes();
    expect(injector.getInstance(PermissionDAO.class)).andReturn(permissionDAO).anyTimes();
    expect(injector.getInstance(PrivilegeDAO.class)).andReturn(privilegeDAO).anyTimes();
    expect(injector.getInstance(KeyValueDAO.class)).andReturn(keyValueDAO).anyTimes();
    expect(injector.getInstance(ViewRegistry.class)).andReturn(viewRegistry).anyTimes();

    expect(userRolesResultSet.next()).andReturn(true).times(3);
    expect(userRolesResultSet.next()).andReturn(false).times(1);
    expect(userRolesResultSet.getString(1)).andReturn("admin").times(1);
    expect(userRolesResultSet.getString(1)).andReturn("user").times(2);
    expect(userRolesResultSet.getInt(2)).andReturn(1).times(2);
    expect(userRolesResultSet.getInt(2)).andReturn(2).times(1);

    UserEntity userEntity1 = createNiceMock(UserEntity.class);
    UserEntity userEntity2 = createNiceMock(UserEntity.class);
    PrincipalEntity userPrincipal1 = createNiceMock(PrincipalEntity.class);
    PrincipalEntity userPrincipal2 = createNiceMock(PrincipalEntity.class);
    Set<PrivilegeEntity> userPrivileges1 = createNiceMock(Set.class);
    Set<PrivilegeEntity> userPrivileges2 = createNiceMock(Set.class);
    expect(userEntity1.getPrincipal()).andReturn(userPrincipal1).anyTimes();
    expect(userEntity2.getPrincipal()).andReturn(userPrincipal2).anyTimes();
    expect(userPrincipal1.getPrivileges()).andReturn(userPrivileges1).anyTimes();
    expect(userPrincipal2.getPrivileges()).andReturn(userPrivileges2).anyTimes();
    expect(userPrivileges1.add(anyObject(PrivilegeEntity.class))).andReturn(true).once();
    expect(userDAO.findByPK(1)).andReturn(userEntity1).times(2);
    expect(userDAO.findByPK(2)).andReturn(userEntity2).once();
    expect(userDAO.merge(userEntity1)).andReturn(userEntity1).once();
    expect(userDAO.merge(userEntity2)).andReturn(userEntity2).once();

    expect(configGroupConfigMappingDAO.findAll()).andReturn(configGroupConfigMappingEntities).once();
    expect(userDAO.findAll()).andReturn(Collections.<UserEntity> emptyList()).times(1);
    expect(userDAO.findAll()).andReturn(Arrays.asList(userEntity1, userEntity2)).times(1);
    expect(clusterDAO.findAll()).andReturn(Collections.<ClusterEntity> emptyList()).times(2);

    String yarnConfig = String.format("{'%s':'%s', '%s':'%s'}",
        YARN_TIMELINE_SERVICE_WEBAPP_ADDRESS_PROPERTY, "timeline:8081",
        YARN_RESOURCEMANAGER_WEBAPP_ADDRESS_PROPERTY, "resource_man:8081");
    expect(configGroupConfigMappingDAO.findAll()).andReturn(configGroupConfigMappingEntities).once();
    expect(clusterDAO.findAll()).andReturn(Collections.singletonList(clusterEntity)).anyTimes();
    expect(configEntity.getData()).andReturn(yarnConfig);
    expect(clusterDAO.findConfig(1L, YARN_SITE, "version1")).andReturn(configEntity).anyTimes();
    expect(viewDAO.findAll()).andReturn(Collections.<ViewEntity> emptyList()).anyTimes();
    expect(viewInstanceDAO.findAll()).andReturn(Collections.<ViewInstanceEntity> emptyList()).anyTimes();
    expect(permissionDAO.findAmbariAdminPermission()).andReturn(adminPermission).anyTimes();
    expect(permissionDAO.findViewUsePermission()).andReturn(viewUsePermission).anyTimes();
    expect(permissionDAO.findClusterOperatePermission()).andReturn(null);
    expect(permissionDAO.findClusterReadPermission()).andReturn(null);

    expect(cluster.getDesiredConfigByType("pig-properties")).andReturn(pigConfig).anyTimes();
    expect(pigConfig.getProperties()).andReturn(pigSettings).anyTimes();

    ViewEntity jobsView = createNiceMock(ViewEntity.class);
    KeyValueEntity showJobsKeyValue = createNiceMock(KeyValueEntity.class);
    UserEntity user = createNiceMock(UserEntity.class);
    ClusterConfigMappingEntity configMappingEntity = createNiceMock(ClusterConfigMappingEntity.class);
    ClusterStateEntity clusterStateEntity = createNiceMock(ClusterStateEntity.class);

    expect(clusterEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(clusterEntity.getConfigMappingEntities()).andReturn(Collections.singleton(configMappingEntity)).times(2);
    expect(clusterEntity.getClusterStateEntity()).andReturn(clusterStateEntity).anyTimes();
    expect(clusterStateEntity.getCurrentStackVersion()).andReturn(CLUSTER_STATE_STACK_HDP_2_1);
    expect(configMappingEntity.getType()).andReturn(YARN_SITE).anyTimes();
    expect(configMappingEntity.isSelected()).andReturn(1).anyTimes();
    expect(configMappingEntity.getTag()).andReturn("version1");
    expect(userDAO.findAll()).andReturn(Collections.singletonList(user));
    expect(jobsView.getCommonName()).andReturn(JOBS_VIEW_NAME);
    expect(jobsView.getVersion()).andReturn("1.0.0");
    expect(viewDAO.findByCommonName(JOBS_VIEW_NAME)).andReturn(jobsView).once();
    expect(showJobsKeyValue.getValue()).andReturn("true");
    expect(keyValueDAO.findByKey(SHOW_JOBS_FOR_NON_ADMIN_KEY)).andReturn(showJobsKeyValue);
    expect(privilegeDAO.findAllByPrincipal(anyObject(List.class))).andReturn(Collections.<PrivilegeEntity>emptyList());
    expect(viewDAO.merge(jobsView)).andReturn(jobsView);

    resourceDAO.create(anyObject(ResourceEntity.class));
    expect(adminPermission.getId()).andReturn(3);
    expect(viewUsePermission.getId()).andReturn(4);
    viewInstanceDAO.create(anyObject(ViewInstanceEntity.class));
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        ((ViewInstanceEntity) getCurrentArguments()[0]).getResource().setId(1L);
        return null;
      }
    });
    keyValueDAO.remove(showJobsKeyValue);
    privilegeDAO.create(anyObject(PrivilegeEntity.class));

    replay(entityManager, trans, upgradeCatalog, cb, cq, hrc, q, userRolesResultSet);

    replay(dbAccessor, configuration, injector, cluster, clusters, amc, config, configHelper, pigConfig);
    replay(userDAO, clusterDAO, viewDAO, viewInstanceDAO, permissionDAO, configGroupConfigMappingDAO);
    replay(resourceTypeDAO, resourceDAO, keyValueDAO, privilegeDAO, clusterConfigEntity);
    replay(jobsView, showJobsKeyValue, user);
    replay(userEntity1, userEntity2, userPrincipal1, userPrincipal2, userPrivileges1, userPrivileges2);
    replay(viewRegistry, viewUsePermission, adminPermission);
    replay(clusterEntity, configEntity, configMappingEntity, clusterStateEntity);

    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);
    f = c.getDeclaredField("dbAccessor");
    f.setAccessible(true);
    f.set(upgradeCatalog, dbAccessor);
    f = c.getDeclaredField("injector");
    f.setAccessible(true);
    f.set(upgradeCatalog, injector);

    upgradeCatalog.executeDMLUpdates();

    verify(upgradeCatalog, dbAccessor, configuration, injector, cluster, clusters, amc, config, configHelper,
        jobsView, showJobsKeyValue, privilegeDAO, viewDAO, viewInstanceDAO, resourceDAO, keyValueDAO, userRolesResultSet,
        userEntity1, userEntity2, userPrincipal1, userPrincipal2, userPrivileges1, userPrivileges2,
        viewRegistry, clusterEntity, configEntity, configMappingEntity, clusterStateEntity);
  }

  @Test
  public void testMoveHcatalogIntoHiveService()  throws AmbariException {
    final ClusterEntity clusterEntity = createCluster();
    final ClusterServiceEntity clusterServiceEntityHDFS = addService(clusterEntity, "HDFS");
    final ClusterServiceEntity clusterServiceEntityHIVE = addService(clusterEntity, "HIVE");
    final ClusterServiceEntity clusterServiceEntityHCATALOG = addService(clusterEntity, "HCATALOG");
    final ClusterServiceEntity clusterServiceEntityWEBHCAT = addService(clusterEntity, "WEBHCAT");
    final HostEntity hostEntity = createHost(clusterEntity);
    addComponent(clusterEntity, clusterServiceEntityHDFS, hostEntity, "NAMENODE");
    addComponent(clusterEntity, clusterServiceEntityHIVE, hostEntity, "HIVE_SERVER");
    addComponent(clusterEntity, clusterServiceEntityHCATALOG, hostEntity, "HCAT");
    addComponent(clusterEntity, clusterServiceEntityWEBHCAT, hostEntity, "WEBHCAT_SERVER");
    UpgradeCatalog170 upgradeCatalog170 = injector.getInstance(UpgradeCatalog170.class);
    upgradeCatalog170.moveHcatalogIntoHiveService();

    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    ServiceComponentDesiredStateEntityPK pkHCATInHive = new ServiceComponentDesiredStateEntityPK();
    pkHCATInHive.setComponentName("HCAT");
    pkHCATInHive.setClusterId(clusterEntity.getClusterId());
    pkHCATInHive.setServiceName("HIVE");
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pkHCATInHive);
    assertNotNull(serviceComponentDesiredStateEntity);

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentDesiredStateEntityPK hcDesiredStateEntityPk  = new HostComponentDesiredStateEntityPK();
    hcDesiredStateEntityPk.setServiceName("HIVE");
    hcDesiredStateEntityPk.setClusterId(clusterEntity.getClusterId());
    hcDesiredStateEntityPk.setComponentName("HCAT");
    hcDesiredStateEntityPk.setHostName(HOST_NAME);
    HostComponentDesiredStateEntity hcDesiredStateEntity = hostComponentDesiredStateDAO.findByPK(hcDesiredStateEntityPk);
    assertNotNull(hcDesiredStateEntity);

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    HostComponentStateEntityPK hcStateEntityPk  = new HostComponentStateEntityPK();
    hcStateEntityPk.setServiceName("HIVE");
    hcStateEntityPk.setClusterId(clusterEntity.getClusterId());
    hcStateEntityPk.setComponentName("HCAT");
    hcStateEntityPk.setHostName(HOST_NAME);
    HostComponentStateEntity hcStateEntity = hostComponentStateDAO.findByPK(hcStateEntityPk);
    assertNotNull(hcStateEntity);
  }

  @Test
  public void updateClusterProvisionState()  throws AmbariException {
    ClusterEntity clusterEntity = createCluster();
    UpgradeCatalog170 upgradeCatalog170 = injector.getInstance(UpgradeCatalog170.class);
    upgradeCatalog170.updateClusterProvisionState();    //action

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    String assertMsg = "updated provisioning state should be installed";
    String expectedState = "INSTALLED";
    String actualState = clusterDAO.findById(1L).getProvisioningState().name();
    assertEquals(assertMsg,expectedState, actualState);  //validation
  }


  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("1.7.0", upgradeCatalog.getTargetVersion());
  }

  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
      }
    };
    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog170.class);
  }

  private void assertClusterConfigColumns(Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture) {
    DBAccessor.DBColumnInfo column = clusterConfigAttributesColumnCapture.getValue();
    assertEquals("config_attributes", column.getName());
    assertEquals(32000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertEquals(null, column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void setClusterConfigExpectations(DBAccessor dbAccessor,
                                   Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture)
      throws SQLException {
    dbAccessor.addColumn(eq("clusterconfig"),
        capture(clusterConfigAttributesColumnCapture));
  }

  private void setStageExpectations(DBAccessor dbAccessor,
                                    Capture<DBAccessor.DBColumnInfo> stageCommandParamsColumnCapture,
                                    Capture<DBAccessor.DBColumnInfo> stageHostParamsColumnCapture)
    throws SQLException {
    dbAccessor.addColumn(eq("stage"),
      capture(stageCommandParamsColumnCapture));

    dbAccessor.addColumn(eq("stage"),
      capture(stageHostParamsColumnCapture));
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("1.6.1", upgradeCatalog.getSourceVersion());
  }

  private void setViewExpectations(DBAccessor dbAccessor,
                                   Capture<DBAccessor.DBColumnInfo> maskColumnCapture,
                                   Capture<DBAccessor.DBColumnInfo> systemColumnCapture)
    throws SQLException {

    dbAccessor.addColumn(eq("viewmain"), capture(maskColumnCapture));
    dbAccessor.addColumn(eq("viewmain"), capture(systemColumnCapture));
  }

  private void setViewParameterExpectations(DBAccessor dbAccessor,
                                            Capture<DBAccessor.DBColumnInfo> maskedColumnCapture)
    throws SQLException {

    dbAccessor.addColumn(eq("viewparameter"), capture(maskedColumnCapture));
  }

  private void setViewInstancePropertyExpectations(DBAccessor dbAccessor,
                                                   Capture<DBAccessor.DBColumnInfo> valueColumnCapture)
      throws SQLException {

    dbAccessor.alterColumn(eq("viewinstanceproperty"), capture(valueColumnCapture));
  }

  private void setViewInstanceDataExpectations(DBAccessor dbAccessor,
                                                   Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture)
      throws SQLException {

    dbAccessor.alterColumn(eq("viewinstancedata"), capture(dataValueColumnCapture));
  }

  private void assertViewColumns(
    Capture<DBAccessor.DBColumnInfo> maskColumnCapture,
    Capture<DBAccessor.DBColumnInfo> systemColumnCapture) {

    DBAccessor.DBColumnInfo column = maskColumnCapture.getValue();
    assertEquals("mask", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = systemColumnCapture.getValue();
    assertEquals("system_view", column.getName());
    assertEquals(1, (int) column.getLength());
    assertEquals(Character.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertViewParameterColumns(
    Capture<DBAccessor.DBColumnInfo> maskedColumnCapture) {
    DBAccessor.DBColumnInfo column = maskedColumnCapture.getValue();
    assertEquals("masked", column.getName());
    assertEquals(1, (int) column.getLength());
    assertEquals(Character.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertViewInstancePropertyColumns(
      Capture<DBAccessor.DBColumnInfo> valueColumnCapture) {
    DBAccessor.DBColumnInfo column = valueColumnCapture.getValue();
    assertEquals("value", column.getName());
    assertEquals(2000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertViewInstanceDataColumns(
      Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture) {
    DBAccessor.DBColumnInfo column = dataValueColumnCapture.getValue();
    assertEquals("value", column.getName());
    assertEquals(2000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertStageColumns(Capture<DBAccessor.DBColumnInfo> stageCommandParamsColumnCapture,
                                  Capture<DBAccessor.DBColumnInfo> stageHostParamsColumnCapture) {
    DBAccessor.DBColumnInfo column = stageCommandParamsColumnCapture.getValue();
    assertEquals("command_params", column.getName());
    assertEquals(byte[].class, column.getType());
    assertEquals(null, column.getDefaultValue());
    assertTrue(column.isNullable());

    column = stageHostParamsColumnCapture.getValue();
    assertEquals("host_params", column.getName());
    assertEquals(byte[].class, column.getType());
    assertEquals(null, column.getDefaultValue());
    assertTrue(column.isNullable());
  }
}
