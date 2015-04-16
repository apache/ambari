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

package org.apache.ambari.server.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class ServiceTest {

  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private AmbariMetaInfo metaInfo;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(
        ServiceComponentFactory.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    clusterName = "foo";
    clusters.addCluster(clusterName, new StackId("HDP-0.1"));
    cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testCreateService() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    s.persist();
    Service service = cluster.getService(serviceName);

    Assert.assertNotNull(service);
    Assert.assertEquals(serviceName, service.getName());
    Assert.assertEquals(cluster.getClusterId(),
        service.getCluster().getClusterId());
    Assert.assertEquals(cluster.getClusterName(),
        service.getCluster().getClusterName());
    Assert.assertEquals(State.INIT, service.getDesiredState());
    Assert.assertEquals(SecurityState.UNSECURED, service.getSecurityState());
    Assert.assertFalse(
        service.getDesiredStackVersion().getStackId().isEmpty());
  }

  @Test
  public void testGetAndSetServiceInfo() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    s.persist();

    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    service.setDesiredStackVersion(new StackId("HDP-1.2.0"));
    Assert.assertEquals("HDP-1.2.0",
        service.getDesiredStackVersion().getStackId());

    service.setDesiredState(State.INSTALLING);
    Assert.assertEquals(State.INSTALLING, service.getDesiredState());

    // FIXME todo use DAO to verify persisted object maps to inmemory state

  }


  @Test
  public void testAddAndGetServiceComponents() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    s.persist();

    Service service = cluster.getService(serviceName);

    Assert.assertNotNull(service);

    Assert.assertTrue(s.getServiceComponents().isEmpty());

    ServiceComponent sc1 =
        serviceComponentFactory.createNew(s, "NAMENODE");
    ServiceComponent sc2 =
        serviceComponentFactory.createNew(s, "DATANODE1");
    ServiceComponent sc3 =
        serviceComponentFactory.createNew(s, "DATANODE2");

    Map<String, ServiceComponent> comps = new
        HashMap<String, ServiceComponent>();
    comps.put(sc1.getName(), sc1);
    comps.put(sc2.getName(), sc2);

    s.addServiceComponents(comps);

    Assert.assertEquals(2, s.getServiceComponents().size());
    Assert.assertNotNull(s.getServiceComponent(sc1.getName()));
    Assert.assertNotNull(s.getServiceComponent(sc2.getName()));

    try {
      s.getServiceComponent(sc3.getName());
      fail("Expected error when looking for invalid component");
    } catch (Exception e) {
      // Expected
    }

    s.addServiceComponent(sc3);

    sc1.persist();
    sc2.persist();
    sc3.persist();

    ServiceComponent sc4 = s.addServiceComponent("HDFS_CLIENT");
    Assert.assertNotNull(s.getServiceComponent(sc4.getName()));
    Assert.assertEquals(State.INIT,
        s.getServiceComponent("HDFS_CLIENT").getDesiredState());
    Assert.assertTrue(sc4.isClientComponent());
    sc4.persist();

    Assert.assertEquals(4, s.getServiceComponents().size());

    Assert.assertNotNull(s.getServiceComponent(sc3.getName()));
    Assert.assertEquals(sc3.getName(),
        s.getServiceComponent(sc3.getName()).getName());
    Assert.assertEquals(s.getName(),
        s.getServiceComponent(sc3.getName()).getServiceName());
    Assert.assertEquals(cluster.getClusterName(),
        s.getServiceComponent(sc3.getName()).getClusterName());

    sc4.setDesiredState(State.INSTALLING);
    Assert.assertEquals(State.INSTALLING,
        s.getServiceComponent("HDFS_CLIENT").getDesiredState());

  }

  @Test
  public void testGetAndSetConfigs() {
    // FIXME add unit tests for configs once impl done
    /*
      public Map<String, Config> getDesiredConfigs();
      public void updateDesiredConfigs(Map<String, Config> configs);
     */
  }


  @Test
  public void testConvertToResponse() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ServiceResponse r = s.convertToResponse();
    Assert.assertEquals(s.getName(), r.getServiceName());
    Assert.assertEquals(s.getCluster().getClusterName(),
        r.getClusterName());
    Assert.assertEquals(s.getDesiredStackVersion().getStackId(),
        r.getDesiredStackVersion());
    Assert.assertEquals(s.getDesiredState().toString(),
        r.getDesiredState());

    service.setDesiredStackVersion(new StackId("HDP-1.2.0"));
    service.setDesiredState(State.INSTALLING);
    r = s.convertToResponse();
    Assert.assertEquals(s.getName(), r.getServiceName());
    Assert.assertEquals(s.getCluster().getClusterName(),
        r.getClusterName());
    Assert.assertEquals(s.getDesiredStackVersion().getStackId(),
        r.getDesiredStackVersion());
    Assert.assertEquals(s.getDesiredState().toString(),
        r.getDesiredState());
    // FIXME add checks for configs

    StringBuilder sb = new StringBuilder();
    s.debugDump(sb);
    // TODO better checks?
    Assert.assertFalse(sb.toString().isEmpty());

  }

  @Test
  public void testDeleteServiceComponent() throws Exception {
    Service hdfs = cluster.addService("HDFS");
    Service mapReduce = cluster.addService("MAPREDUCE");

    hdfs.persist();

    ServiceComponent nameNode = hdfs.addServiceComponent("NAMENODE");
    nameNode.persist();
    ServiceComponent jobTracker = mapReduce.addServiceComponent("JOBTRACKER");

    assertEquals(2, cluster.getServices().size());
    assertEquals(1, hdfs.getServiceComponents().size());
    assertEquals(1, mapReduce.getServiceComponents().size());
    assertTrue(hdfs.isPersisted());
    assertFalse(mapReduce.isPersisted());

    hdfs.deleteServiceComponent("NAMENODE");

    assertEquals(0, hdfs.getServiceComponents().size());
    assertEquals(1, mapReduce.getServiceComponents().size());

    mapReduce.deleteServiceComponent("JOBTRACKER");

    assertEquals(0, hdfs.getServiceComponents().size());
    assertEquals(0, mapReduce.getServiceComponents().size());

  }

  @Test
  public void testCanBeRemoved() throws Exception{
    Service service = cluster.addService("HDFS");

    for (State state : State.values()) {
      service.setDesiredState(state);

      if (state.isRemovableState()) {
        org.junit.Assert.assertTrue(service.canBeRemoved());
      }
      else {
        org.junit.Assert.assertFalse(service.canBeRemoved());
      }
    }

    ServiceComponent component = service.addServiceComponent("NAMENODE");
    // can't remove a STARTED component
    component.setDesiredState(State.STARTED);

    for (State state : State.values()) {
      service.setDesiredState(state);
      // should always be false if the sub component can not be removed
      org.junit.Assert.assertFalse(service.canBeRemoved());
    }
  }

  @Test
  public void testServiceMaintenance() throws Exception {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    s.persist();

    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ClusterServiceDAO dao = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity entity = dao.findByClusterAndServiceNames(clusterName, serviceName);
    Assert.assertNotNull(entity);
    Assert.assertEquals(MaintenanceState.OFF, entity.getServiceDesiredStateEntity().getMaintenanceState());
    Assert.assertEquals(MaintenanceState.OFF, service.getMaintenanceState());

    service.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON, service.getMaintenanceState());

    entity = dao.findByClusterAndServiceNames(clusterName, serviceName);
    Assert.assertNotNull(entity);
    Assert.assertEquals(MaintenanceState.ON, entity.getServiceDesiredStateEntity().getMaintenanceState());
  }

  @Test
  public void testSecurityState() throws Exception {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    s.persist();

    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ClusterServiceDAO dao = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity entity = dao.findByClusterAndServiceNames(clusterName, serviceName);
    Assert.assertNotNull(entity);
    Assert.assertEquals(SecurityState.UNSECURED, entity.getServiceDesiredStateEntity().getSecurityState());
    Assert.assertEquals(SecurityState.UNSECURED, service.getSecurityState());

    service.setSecurityState(SecurityState.SECURED_KERBEROS);
    Assert.assertEquals(SecurityState.SECURED_KERBEROS, service.getSecurityState());

    entity = dao.findByClusterAndServiceNames(clusterName, serviceName);
    Assert.assertNotNull(entity);
    Assert.assertEquals(SecurityState.SECURED_KERBEROS, entity.getServiceDesiredStateEntity().getSecurityState());

    // Make sure there are no issues setting all endpoint values...
    for(SecurityState state: SecurityState.ENDPOINT_STATES) {
      service.setSecurityState(state);
      Assert.assertEquals(state, service.getSecurityState());
    }

    // Make sure there transitional states are not allowed
    for(SecurityState state: SecurityState.TRANSITIONAL_STATES) {
      try {
        service.setSecurityState(state);
        Assert.fail(String.format("SecurityState %s is not a valid desired service state", state.toString()));
      }
      catch (AmbariException e) {
        // this is acceptable
      }
    }
  }
}
