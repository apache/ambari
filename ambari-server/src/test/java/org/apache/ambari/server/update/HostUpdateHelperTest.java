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
package org.apache.ambari.server.update;


import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.CollectionPresentationUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

public class HostUpdateHelperTest {

  @Test
  public void testValidateHostChanges_Success() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    Cluster mockCluster = easyMockSupport.createNiceMock(Cluster.class);
    Host host1 = easyMockSupport.createNiceMock(Host.class);
    Host host2 = easyMockSupport.createNiceMock(Host.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    Map<String, String> hosts = new HashMap<>();
    List<Host> clusterHosts = new ArrayList<>();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
       }
    });

    clusterHosts.add(host1);
    clusterHosts.add(host2);

    hosts.put("host1","host10");
    hosts.put("host2","host11");

    clusterHostsToChange.put("cl1", hosts);

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getCluster("cl1")).andReturn(mockCluster).once();
    expect(mockCluster.getHosts()).andReturn(clusterHosts).once();
    expect(host1.getHostName()).andReturn("host1");
    expect(host2.getHostName()).andReturn("host2");

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    hostUpdateHelper.validateHostChanges();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testValidateHostChanges_InvalidHost() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    Cluster mockCluster = easyMockSupport.createNiceMock(Cluster.class);
    Host host1 = easyMockSupport.createNiceMock(Host.class);
    Host host2 = easyMockSupport.createNiceMock(Host.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    Map<String, String> hosts = new HashMap<>();
    List<Host> clusterHosts = new ArrayList<>();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
      }
    });

    clusterHosts.add(host1);
    clusterHosts.add(host2);

    hosts.put("host1","host10");
    hosts.put("host3","host11");

    clusterHostsToChange.put("cl1", hosts);

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getCluster("cl1")).andReturn(mockCluster).once();
    expect(mockCluster.getHosts()).andReturn(clusterHosts).once();
    expect(host1.getHostName()).andReturn("host1");
    expect(host2.getHostName()).andReturn("host2");

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    try{
      hostUpdateHelper.validateHostChanges();
    } catch(AmbariException e) {
      assert(e.getMessage().equals("Hostname(s): host3 was(were) not found."));
    }

    easyMockSupport.verifyAll();
  }

  @Test
  public void testValidateHostChanges_InvalidCluster() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
      }
    });

    clusterHostsToChange.put("cl1", null);

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getCluster("cl1")).andReturn(null).once();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    try{
      hostUpdateHelper.validateHostChanges();
    } catch(AmbariException e) {
      assert(e.getMessage().equals("Cluster cl1 was not found."));
    }

    easyMockSupport.verifyAll();
  }

  @Test
  public void testValidateHostChanges_HostChangesNull() throws Exception {
    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, null);

    hostUpdateHelper.setHostChangesFileMap(null);

    try{
      hostUpdateHelper.validateHostChanges();
    } catch(AmbariException e) {
      assert(e.getMessage().equals("File with host names changes is null or empty"));
    }
  }

  @Test
  public void testUpdateHostsInConfigurations() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ClusterDAO mockClusterDAO = easyMockSupport.createNiceMock(ClusterDAO.class);
    final EntityManager entityManager = createNiceMock(EntityManager.class);
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    Cluster mockCluster = easyMockSupport.createNiceMock(Cluster.class);
    ClusterEntity mockClusterEntity1 = easyMockSupport.createNiceMock(ClusterEntity.class);
    ClusterEntity mockClusterEntity2 = easyMockSupport.createNiceMock(ClusterEntity.class);
    ClusterConfigEntity mockClusterConfigEntity1 = easyMockSupport.createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity mockClusterConfigEntity2 = easyMockSupport.createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity mockClusterConfigEntity3 = easyMockSupport.createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity mockClusterConfigEntity4 = easyMockSupport.createNiceMock(ClusterConfigEntity.class);
    StackEntity mockStackEntity = easyMockSupport.createNiceMock(StackEntity.class);
    ReadWriteLock mockReadWriteLock = easyMockSupport.createNiceMock(ReadWriteLock.class);
    Lock mockLock = easyMockSupport.createNiceMock(Lock.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    Map<String, String> hosts = new HashMap<>();
    List<ClusterConfigEntity> clusterConfigEntities1 = new ArrayList<>();
    List<ClusterConfigEntity> clusterConfigEntities2 = new ArrayList<>();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(EntityManager.class).toInstance(entityManager);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(ClusterDAO.class).toInstance(mockClusterDAO);
      }
    });

    hosts.put("host11","host55");
    hosts.put("host5","host1");
    hosts.put("host1","host5");
    hosts.put("host55","host11");

    clusterConfigEntities1.add(mockClusterConfigEntity1);
    clusterConfigEntities1.add(mockClusterConfigEntity2);

    clusterConfigEntities2.add(mockClusterConfigEntity3);
    clusterConfigEntities2.add(mockClusterConfigEntity4);

    clusterHostsToChange.put("cl1", hosts);

    expect(mockClusterDAO.findByName("cl1")).andReturn(mockClusterEntity1).once();
    expect(mockClusterDAO.findById(1L)).andReturn(mockClusterEntity2).atLeastOnce();

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();

    expect(mockClusters.getCluster("cl1")).andReturn(mockCluster).once();
    expect(mockCluster.getClusterGlobalLock()).andReturn(mockReadWriteLock).atLeastOnce();
    expect(mockCluster.getClusterId()).andReturn(1L).atLeastOnce();

    expect(mockReadWriteLock.writeLock()).andReturn(mockLock).atLeastOnce();

    expect(mockClusterEntity1.getClusterConfigEntities()).andReturn(clusterConfigEntities1).atLeastOnce();
    expect(mockClusterEntity2.getClusterConfigEntities()).andReturn(clusterConfigEntities2).atLeastOnce();

    expect(mockClusterConfigEntity1.getStack()).andReturn(mockStackEntity).once();
    expect(mockClusterConfigEntity1.getData()).andReturn("{\"testProperty1\" : \"testValue_host1\", " +
            "\"testProperty2\" : \"testValue_host5\", \"testProperty3\" : \"testValue_host11\", " +
            "\"testProperty4\" : \"testValue_host55\"}").atLeastOnce();
    expect(mockClusterConfigEntity1.getTag()).andReturn("testTag1").atLeastOnce();
    expect(mockClusterConfigEntity1.getType()).andReturn("testType1").atLeastOnce();
    expect(mockClusterConfigEntity1.getVersion()).andReturn(1L).atLeastOnce();

    expect(mockClusterConfigEntity2.getStack()).andReturn(mockStackEntity).once();
    expect(mockClusterConfigEntity2.getData()).andReturn("{\"testProperty5\" : \"test_host1_test_host5_test_host11_test_host55\"}").atLeastOnce();
    expect(mockClusterConfigEntity2.getTag()).andReturn("testTag2").atLeastOnce();
    expect(mockClusterConfigEntity2.getType()).andReturn("testType2").atLeastOnce();
    expect(mockClusterConfigEntity2.getVersion()).andReturn(2L).atLeastOnce();

    expect(mockClusterConfigEntity3.getTag()).andReturn("testTag1").atLeastOnce();
    expect(mockClusterConfigEntity3.getType()).andReturn("testType1").atLeastOnce();
    expect(mockClusterConfigEntity3.getVersion()).andReturn(1L).atLeastOnce();

    expect(mockClusterConfigEntity4.getTag()).andReturn("testTag2").atLeastOnce();
    expect(mockClusterConfigEntity4.getType()).andReturn("testType2").atLeastOnce();
    expect(mockClusterConfigEntity4.getVersion()).andReturn(2L).atLeastOnce();

    Capture<String> dataCapture = EasyMock.newCapture();
    mockClusterConfigEntity3.setData(EasyMock.capture(dataCapture));
    expectLastCall();

    mockClusterConfigEntity4.setData("{\"testProperty5\":\"test_host5_test_host1_test_host55_test_host11\"}");
    expectLastCall();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    hostUpdateHelper.updateHostsInConfigurations();
    easyMockSupport.verifyAll();

    // Depends on hashing, string representation can be different
    Assert.assertTrue(CollectionPresentationUtils.isJsonsEquals("{\"testProperty4\":\"testValue_host11\",\"testProperty3\":\"testValue_host55\"," +
        "\"testProperty2\":\"testValue_host1\",\"testProperty1\":\"testValue_host5\"}", dataCapture.getValue()));
  }

  @Test
  public void testCheckForSecurity_SecureCluster() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    Cluster mockCluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockConfig = easyMockSupport.createNiceMock(Config.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    Map<String, String> clusterEnvProperties = new HashMap<>();

    clusterHostsToChange.put("cl1", null);
    clusterEnvProperties.put("security_enabled", "true");

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getCluster("cl1")).andReturn(mockCluster).once();
    expect(mockCluster.getDesiredConfigByType("cluster-env")).andReturn(mockConfig).once();
    expect(mockConfig.getProperties()).andReturn(clusterEnvProperties).once();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    try{
      hostUpdateHelper.checkForSecurity();
    } catch(AmbariException e) {
      assert(e.getMessage().equals("Cluster(s) cl1 from file, is(are) in secure mode. Please, turn off security mode."));
    }
    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckForSecurity_NonSecureCluster() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    Cluster mockCluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockConfig = easyMockSupport.createNiceMock(Config.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    Map<String, String> clusterEnvProperties = new HashMap<>();

    clusterHostsToChange.put("cl1", null);
    clusterEnvProperties.put("security_enabled", "false");

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getCluster("cl1")).andReturn(mockCluster).once();
    expect(mockCluster.getDesiredConfigByType("cluster-env")).andReturn(mockConfig).once();
    expect(mockConfig.getProperties()).andReturn(clusterEnvProperties).once();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    hostUpdateHelper.checkForSecurity();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHostsInDB() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final HostDAO mockHostDAO = easyMockSupport.createNiceMock(HostDAO.class);
    final ClusterDAO mockClusterDAO = easyMockSupport.createNiceMock(ClusterDAO.class);
    final EntityManager entityManager = createNiceMock(EntityManager.class);
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    ClusterEntity mockClusterEntity = easyMockSupport.createNiceMock(ClusterEntity.class);
    HostEntity mockHostEntity1 = easyMockSupport.createNiceMock(HostEntity.class);
    HostEntity mockHostEntity2 = easyMockSupport.createNiceMock(HostEntity.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    List<HostEntity> hostEntities = new ArrayList<>();
    Map<String, String> hosts = new HashMap<>();

    hosts.put("host1","host10");
    hosts.put("host2","host11");

    clusterHostsToChange.put("cl1", hosts);

    hostEntities.add(mockHostEntity1);
    hostEntities.add(mockHostEntity2);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(EntityManager.class).toInstance(entityManager);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(ClusterDAO.class).toInstance(mockClusterDAO);
        bind(HostDAO.class).toInstance(mockHostDAO);
      }
    });

    expect(mockClusterDAO.findByName("cl1")).andReturn(mockClusterEntity).once();
    expect(mockClusterEntity.getHostEntities()).andReturn(hostEntities).once();
    expect(mockHostEntity1.getHostName()).andReturn("host1").atLeastOnce();
    expect(mockHostEntity2.getHostName()).andReturn("host2").atLeastOnce();

    mockHostEntity1.setHostName("host10");
    expectLastCall();

    mockHostEntity2.setHostName("host11");
    expectLastCall();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    hostUpdateHelper.updateHostsInDB();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHostsForAlertsInDB() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final AlertsDAO mockAlertsDAO = easyMockSupport.createNiceMock(AlertsDAO.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDispatchDAO mockAlertDispatchDAO = easyMockSupport.createNiceMock(AlertDispatchDAO.class);
    final EntityManager entityManager = createNiceMock(EntityManager.class);
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final ClusterDAO mockClusterDAO = easyMockSupport.createNiceMock(ClusterDAO.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    Cluster mockCluster = easyMockSupport.createNiceMock(Cluster.class);
    AlertCurrentEntity mockAlertCurrentEntity1 = easyMockSupport.createNiceMock(AlertCurrentEntity.class);
    AlertCurrentEntity mockAlertCurrentEntity2 = easyMockSupport.createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity mockAlertHistoryEntity1 = easyMockSupport.createNiceMock(AlertHistoryEntity.class);
    AlertHistoryEntity mockAlertHistoryEntity2 = easyMockSupport.createNiceMock(AlertHistoryEntity.class);
    AlertDefinitionEntity mockAlertDefinitionEntity = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);
    Map<String, Map<String, String>> clusterHostsToChange = new HashMap<>();
    List<AlertCurrentEntity> alertCurrentEntities = new ArrayList<>();
    List<AlertDefinitionEntity> alertDefinitionEntities = new ArrayList<>();
    Map<String, Cluster> clusterMap = new HashMap<>();
    Map<String, String> hosts = new HashMap<>();

    hosts.put("host1","host10");
    hosts.put("host2","host11");

    clusterHostsToChange.put("cl1", hosts);

    clusterMap.put("cl1", mockCluster);

    alertCurrentEntities.add(mockAlertCurrentEntity1);
    alertCurrentEntities.add(mockAlertCurrentEntity2);

    alertDefinitionEntities.add(mockAlertDefinitionEntity);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(EntityManager.class).toInstance(entityManager);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(ClusterDAO.class).toInstance(mockClusterDAO);
        bind(Clusters.class).toInstance(mockClusters);
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(AlertDispatchDAO.class).toInstance(mockAlertDispatchDAO);
        bind(AlertsDAO.class).toInstance(mockAlertsDAO);
        bind(AlertDefinitionDAO.class).toInstance(mockAlertDefinitionDAO);
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(clusterMap).once();
    expect(mockCluster.getClusterId()).andReturn(1L).once();
    expect(mockAlertsDAO.findCurrentByCluster(1L)).andReturn(alertCurrentEntities).once();
    expect(mockAlertCurrentEntity1.getAlertHistory()).andReturn(mockAlertHistoryEntity1).once();
    expect(mockAlertCurrentEntity2.getAlertHistory()).andReturn(mockAlertHistoryEntity2).once();
    expect(mockAlertHistoryEntity1.getHostName()).andReturn("host1").atLeastOnce();
    expect(mockAlertHistoryEntity2.getHostName()).andReturn("host2").atLeastOnce();
    expect(mockAlertDefinitionDAO.findAll(1L)).andReturn(alertDefinitionEntities).once();

    mockAlertHistoryEntity1.setHostName("host10");
    expectLastCall();

    mockAlertHistoryEntity2.setHostName("host11");
    expectLastCall();

    mockAlertDefinitionEntity.setHash(anyString());
    expectLastCall();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, null, mockInjector);

    hostUpdateHelper.setHostChangesFileMap(clusterHostsToChange);

    easyMockSupport.replayAll();
    hostUpdateHelper.updateHostsForAlertsInDB();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testInitHostChangesFileMap_SUCCESS() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final Configuration mockConfiguration = easyMockSupport.createNiceMock(Configuration.class);
    JsonObject cluster = new JsonObject();
    JsonObject hostPairs = new JsonObject();
    hostPairs.add("Host1", new JsonPrimitive("Host11"));
    hostPairs.add("Host2", new JsonPrimitive("Host22"));
    cluster.add("cl1", hostPairs);

    expect(mockConfiguration.getHostChangesJson(null)).andReturn(cluster).once();

    HostUpdateHelper hostUpdateHelper = new HostUpdateHelper(null, mockConfiguration, null);

    easyMockSupport.replayAll();
    hostUpdateHelper.initHostChangesFileMap();
    easyMockSupport.verifyAll();

    Map<String, Map<String,String>> hostChangesFileMap = hostUpdateHelper.getHostChangesFileMap();
    Assert.assertTrue(hostChangesFileMap.get("cl1").containsKey("host1"));
    Assert.assertTrue(hostChangesFileMap.get("cl1").containsKey("host2"));
    Assert.assertTrue(hostChangesFileMap.get("cl1").get("host1").equals("host11"));
    Assert.assertTrue(hostChangesFileMap.get("cl1").get("host2").equals("host22"));
  }


}

