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
package org.apache.ambari.server.checks;


import static org.easymock.EasyMock.expect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class DatabaseConsistencyCheckHelperTest {

  @Test
  public void testCheckForNotMappedConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });



    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select type_name from clusterconfig where type_name not in (select type_name from clusterconfigmapping)")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();

    DatabaseConsistencyCheckHelper.checkForNotMappedConfigsToCluster();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckForConfigsSelectedMoreThanOnce() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, ccm.type_name from clusterconfigmapping ccm " +
            "join clusters c on ccm.cluster_id=c.cluster_id " +
            "group by c.cluster_name, ccm.type_name " +
            "having sum(selected) > 1")).andReturn(mockResultSet);



    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);


    easyMockSupport.replayAll();

    DatabaseConsistencyCheckHelper.checkForConfigsSelectedMoreThanOnce();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckForHostsWithoutState() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });



    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select host_name from hosts where host_id not in (select host_id from hoststate)")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();



    DatabaseConsistencyCheckHelper.checkForHostsWithoutState();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckHostComponentStatesCountEqualsHostComponentsDesiredStates() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });



    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select count(*) from hostcomponentstate")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select count(*) from hostcomponentdesiredstate")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select count(*) FROM hostcomponentstate hcs " +
            "JOIN hostcomponentdesiredstate hcds ON hcs.service_name=hcds.service_name AND " +
            "hcs.component_name=hcds.component_name AND hcs.host_id=hcds.host_id")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select component_name, host_id from hostcomponentstate group by component_name, host_id having count(component_name) > 1")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();


    DatabaseConsistencyCheckHelper.checkHostComponentStates();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckServiceConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariMetaInfo mockAmbariMetainfo = easyMockSupport.createNiceMock(AmbariMetaInfo.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet stackResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet serviceConfigResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final ServiceInfo mockHDFSServiceInfo = easyMockSupport.createNiceMock(ServiceInfo.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariMetaInfo.class).toInstance(mockAmbariMetainfo);
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    Map<String, ServiceInfo> services = new HashMap<>();
    services.put("HDFS", mockHDFSServiceInfo);

    Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();
    configAttributes.put("core-site", new HashMap<String, Map<String, String>>());

    expect(mockHDFSServiceInfo.getConfigTypeAttributes()).andReturn(configAttributes);
    expect(mockAmbariMetainfo.getServices("HDP", "2.2")).andReturn(services);
    expect(serviceConfigResultSet.next()).andReturn(true).times(2);
    expect(serviceConfigResultSet.getString("service_name")).andReturn("HDFS").andReturn("HBASE");
    expect(serviceConfigResultSet.getString("type_name")).andReturn("core-site").andReturn("hbase-env");
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDP");
    expect(stackResultSet.getString("stack_version")).andReturn("2.2");
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, service_name from clusterservices cs " +
            "join clusters c on cs.cluster_id=c.cluster_id " +
            "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
            "join clusters c on sc.cluster_id=c.cluster_id " +
            "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
            "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
            "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
            "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name, sc.version")).andReturn(serviceConfigResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
            "join clusterconfigmapping ccm on cc.type_name=ccm.type_name and cc.version_tag=ccm.version_tag and cc.cluster_id=ccm.cluster_id " +
            "join clusters c on ccm.cluster_id=c.cluster_id " +
            "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name " +
            "having sum(ccm.selected) < 1")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();

    mockAmbariMetainfo.init();

    DatabaseConsistencyCheckHelper.checkServiceConfigs();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckServiceConfigs_missingServiceConfigGeneratesWarning() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariMetaInfo mockAmbariMetainfo = easyMockSupport.createNiceMock(AmbariMetaInfo.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet clusterServicesResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet stackResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet serviceConfigResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final ServiceInfo mockHDFSServiceInfo = easyMockSupport.createNiceMock(ServiceInfo.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariMetaInfo.class).toInstance(mockAmbariMetainfo);
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    Map<String, ServiceInfo> services = new HashMap<>();
    services.put("HDFS", mockHDFSServiceInfo);

    Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();
    configAttributes.put("core-site", new HashMap<String, Map<String, String>>());

    expect(mockHDFSServiceInfo.getConfigTypeAttributes()).andReturn(configAttributes);
    expect(mockAmbariMetainfo.getServices("HDP", "2.2")).andReturn(services);
    expect(clusterServicesResultSet.next()).andReturn(true);
    expect(clusterServicesResultSet.getString("service_name")).andReturn("OPENSOFT R");
    expect(clusterServicesResultSet.getString("cluster_name")).andReturn("My Cluster");
    expect(serviceConfigResultSet.next()).andReturn(true);
    expect(serviceConfigResultSet.getString("service_name")).andReturn("HDFS");
    expect(serviceConfigResultSet.getString("type_name")).andReturn("core-site");
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDP");
    expect(stackResultSet.getString("stack_version")).andReturn("2.2");
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, service_name from clusterservices cs " +
        "join clusters c on cs.cluster_id=c.cluster_id " +
        "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)")).andReturn(clusterServicesResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
        "join clusters c on sc.cluster_id=c.cluster_id " +
        "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
        "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
        "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
        "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
        "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
        "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
        "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
        "group by c.cluster_name, cs.service_name, cc.type_name, sc.version")).andReturn(serviceConfigResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
        "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
        "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
        "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
        "join clusterconfigmapping ccm on cc.type_name=ccm.type_name and cc.version_tag=ccm.version_tag and cc.cluster_id=ccm.cluster_id " +
        "join clusters c on ccm.cluster_id=c.cluster_id " +
        "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
        "group by c.cluster_name, cs.service_name, cc.type_name " +
        "having sum(ccm.selected) < 1")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();

    mockAmbariMetainfo.init();

    DatabaseConsistencyCheckHelper.resetErrorWarningFlags();
    DatabaseConsistencyCheckHelper.checkServiceConfigs();

    easyMockSupport.verifyAll();

    Assert.assertTrue("Missing service config for OPENSOFT R should have triggered a warning.",
        DatabaseConsistencyCheckHelper.ifWarningsFound());
    Assert.assertFalse("No errors should have been triggered.", DatabaseConsistencyCheckHelper.ifErrorsFound());
  }


}

