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

package org.apache.ambari.server.state.svccomphost;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Provider;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.entities.HostComponentConfigMappingEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import javax.persistence.EntityManager;

public class ServiceComponentHostTest {
  private static Logger LOG = LoggerFactory.getLogger(ServiceComponentHostTest.class);
  @Inject
  private Injector injector;
  @Inject
  private Clusters clusters;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;
  @Inject
  private AmbariMetaInfo metaInfo;
  @Inject
  private HostComponentStateDAO hostComponentStateDAO;
  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  private ConfigFactory configFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addCluster("C1");
    clusters.addHost("h1");
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.getCluster("C1").setDesiredStackVersion(
        new StackId("HDP-0.1"));
    metaInfo.init();
    clusters.mapHostToCluster("h1","C1");
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  private ServiceComponentHost createNewServiceComponentHost(
      String svc,
      String svcComponent,
      String hostName, boolean isClient) throws AmbariException{
    Cluster c = clusters.getCluster("C1");
    Service s = null;

    try {
      s = c.getService(svc);
    } catch (ServiceNotFoundException e) {
      LOG.debug("Calling service create"
          + ", serviceName=" + svc);
      s = serviceFactory.createNew(c, svc);
      c.addService(s);
      s.persist();
    }

    ServiceComponent sc = null;
    try {
      sc = s.getServiceComponent(svcComponent);
    } catch (ServiceComponentNotFoundException e) {
      sc = serviceComponentFactory.createNew(s, svcComponent);
      s.addServiceComponent(sc);
      sc.persist();
    }

    ServiceComponentHost impl = serviceComponentHostFactory.createNew(
        sc, hostName, isClient);
    impl.persist();
    Assert.assertEquals(State.INIT,
        impl.getState());
    Assert.assertEquals(State.INIT,
        impl.getDesiredState());
    Assert.assertEquals("C1", impl.getClusterName());
    Assert.assertEquals(c.getClusterId(), impl.getClusterId());
    Assert.assertEquals(s.getName(), impl.getServiceName());
    Assert.assertEquals(sc.getName(), impl.getServiceComponentName());
    Assert.assertEquals(hostName, impl.getHostName());
    Assert.assertFalse(
        impl.getDesiredStackVersion().getStackId().isEmpty());
    Assert.assertTrue(impl.getStackVersion().getStackId().isEmpty());

    return impl;
  }

  @Test
  public void testNewServiceComponentHost() throws AmbariException{
    createNewServiceComponentHost("HDFS", "NAMENODE", "h1", false);
    createNewServiceComponentHost("HDFS", "HDFS_CLIENT", "h1", true);
  }

  private ServiceComponentHostEvent createEvent(ServiceComponentHostImpl impl,
      long timestamp, ServiceComponentHostEventType eventType)
      throws AmbariException {
    Map<String, String> configs = new HashMap<String, String>();

    Cluster c = clusters.getCluster("C1");
    if (c.getDesiredConfig("time", "" + timestamp) == null) {
      Config config = configFactory.createNew (c, "time",
          new HashMap<String, String>());
      config.setVersionTag("" + timestamp);
      c.addDesiredConfig(config);
      config.persist();
    }

    configs.put("time", "" + timestamp);
    switch (eventType) {
      case HOST_SVCCOMP_INSTALL:
        return new ServiceComponentHostInstallEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp,
            impl.getDesiredStackVersion().getStackId());
      case HOST_SVCCOMP_START:
        return new ServiceComponentHostStartEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp,
            configs);
      case HOST_SVCCOMP_STOP:
        return new ServiceComponentHostStopEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_UNINSTALL:
        return new ServiceComponentHostUninstallEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_FAILED:
        return new ServiceComponentHostOpFailedEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_SUCCEEDED:
        return new ServiceComponentHostOpSucceededEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_IN_PROGRESS:
        return new ServiceComponentHostOpInProgressEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_RESTART:
        return new ServiceComponentHostOpRestartedEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_WIPEOUT:
        return new ServiceComponentHostWipeoutEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
    }
    return null;
  }

  private void runStateChanges(ServiceComponentHostImpl impl,
      ServiceComponentHostEventType startEventType,
      State startState,
      State inProgressState,
      State failedState,
      State completedState)
    throws Exception {
    long timestamp = 0;

    boolean checkConfigs = false;
    if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_START) {
      checkConfigs = true;
    }
    boolean checkStack = false;
    if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL) {
      checkStack = true;
      impl.setStackVersion(null);
    }

    Assert.assertEquals(startState,
        impl.getState());
    ServiceComponentHostEvent startEvent = createEvent(impl, ++timestamp,
        startEventType);

    long startTime = timestamp;
    impl.handleEvent(startEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());
    if (checkConfigs) {
      Assert.assertTrue(impl.getConfigVersions().size() > 0);
      Assert.assertEquals("" + startTime, impl.getConfigVersions().get("time"));
    }
    if (checkStack) {
      Assert.assertNotNull(impl.getStackVersion());
      Assert.assertEquals(impl.getDesiredStackVersion().getStackId(),
          impl.getStackVersion().getStackId());
    }

    ServiceComponentHostEvent installEvent2 = createEvent(impl, ++timestamp,
        startEventType);
   
    boolean exceptionThrown = false;
    LOG.info("Transitioning from " + impl.getState() + " " + installEvent2.getType());
    try {
      impl.handleEvent(installEvent2);
    } catch (Exception e) {
      exceptionThrown = true;
    }
    if (impl.getState() == State.INSTALLING || impl.getState() == State.STARTING) {
      startTime = timestamp;
    // We need to allow install on a install.
      Assert.assertTrue("Exception not thrown on invalid event", !exceptionThrown);
    } 
    else {
      Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);
    }
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent1 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent1);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent2 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());


    ServiceComponentHostOpFailedEvent failEvent = new
        ServiceComponentHostOpFailedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    long endTime = timestamp;
    impl.handleEvent(failEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(failedState,
        impl.getState());

    ServiceComponentHostOpRestartedEvent restartEvent = new
        ServiceComponentHostOpRestartedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    startTime = timestamp;
    impl.handleEvent(restartEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent3 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent3);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpFailedEvent failEvent2 = new
        ServiceComponentHostOpFailedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    endTime = timestamp;
    impl.handleEvent(failEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(failedState,
        impl.getState());

    ServiceComponentHostEvent startEvent2 = createEvent(impl, ++timestamp,
        startEventType);
    startTime = timestamp;
    impl.handleEvent(startEvent2);
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent4 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent4);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpSucceededEvent succeededEvent = new
        ServiceComponentHostOpSucceededEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    endTime = timestamp;
    impl.handleEvent(succeededEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(completedState,
        impl.getState());

  }

  @Test
  public void testClientStateFlow() throws Exception {
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost("HDFS", "HDFS_CLIENT", "h1", true);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        State.INIT,
        State.INSTALLING,
        State.INSTALL_FAILED,
        State.INSTALLED);

    boolean exceptionThrown = false;
    try {
      runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
        State.INSTALLED,
        State.STARTING,
        State.START_FAILED,
        State.STARTED);
    }
    catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        State.INSTALLED,
        State.UNINSTALLING,
        State.UNINSTALL_FAILED,
        State.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        State.UNINSTALLED,
        State.WIPING_OUT,
        State.WIPEOUT_FAILED,
        State.INIT);

  }

  @Test
  public void testDaemonStateFlow() throws Exception {
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost("HDFS", "DATANODE", "h1", false);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        State.INIT,
        State.INSTALLING,
        State.INSTALL_FAILED,
        State.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
      State.INSTALLED,
      State.STARTING,
      State.START_FAILED,
      State.STARTED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      State.STARTED,
      State.STOPPING,
      State.STOP_FAILED,
      State.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        State.INSTALLED,
        State.UNINSTALLING,
        State.UNINSTALL_FAILED,
        State.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        State.UNINSTALLED,
        State.WIPING_OUT,
        State.WIPEOUT_FAILED,
        State.INIT);
  }

  @Test
  public void testJobHandling() {
    // TODO fix once jobs are handled
  }


  @Test
  public void testGetAndSetConfigs() {
    // FIXME config handling
    /*
    public Map<String, Config> getDesiredConfigs();
    public void updateDesiredConfigs(Map<String, Config> configs);
    public Map<String, Config> getConfigs();
    public void updateConfigs(Map<String, Config> configs);
    */
  }

  @Test
  public void testGetAndSetBasicInfo() throws AmbariException {
    ServiceComponentHost sch =
        createNewServiceComponentHost("HDFS", "NAMENODE", "h1", false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    sch.setStackVersion(new StackId("HDP-1.0.0"));
    sch.setDesiredStackVersion(new StackId("HDP-1.1.0"));

    Assert.assertEquals(State.INSTALLING, sch.getState());
    Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
    Assert.assertEquals("HDP-1.0.0",
        sch.getStackVersion().getStackId());
    Assert.assertEquals("HDP-1.1.0",
        sch.getDesiredStackVersion().getStackId());
  }

  @Test
  public void testConvertToResponse() throws AmbariException {
    ServiceComponentHost sch =
        createNewServiceComponentHost("HDFS", "DATANODE", "h1", false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    sch.setStackVersion(new StackId("HDP-1.0.0"));
    ServiceComponentHostResponse r =
        sch.convertToResponse();
    Assert.assertEquals("HDFS", r.getServiceName());
    Assert.assertEquals("DATANODE", r.getComponentName());
    Assert.assertEquals("h1", r.getHostname());
    Assert.assertEquals("C1", r.getClusterName());
    Assert.assertEquals(State.INSTALLED.toString(), r.getDesiredState());
    Assert.assertEquals(State.INSTALLING.toString(), r.getLiveState());
    Assert.assertEquals("HDP-1.0.0", r.getStackVersion());

    // TODO check configs

    StringBuilder sb = new StringBuilder();
    sch.debugDump(sb);
    Assert.assertFalse(sb.toString().isEmpty());
  }

  @Test
  public void testStopInVariousStates() throws AmbariException,
      InvalidStateTransitionException {
    ServiceComponentHost sch =
        createNewServiceComponentHost("HDFS", "DATANODE", "h1", false);
    ServiceComponentHostImpl impl =  (ServiceComponentHostImpl) sch;

    sch.setDesiredState(State.STARTED);
    sch.setState(State.START_FAILED);

    long timestamp = 0;

    ServiceComponentHostEvent stopEvent = createEvent(impl, ++timestamp,
        ServiceComponentHostEventType.HOST_SVCCOMP_STOP);

    long startTime = timestamp;
    impl.handleEvent(stopEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(State.STOPPING,
        impl.getState());

    sch.setState(State.INSTALL_FAILED);

    boolean exceptionThrown = false;
    try {
      impl.handleEvent(stopEvent);
    } catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());

    sch.setState(State.INSTALLED);
    ServiceComponentHostEvent stopEvent2 = createEvent(impl, ++timestamp,
        ServiceComponentHostEventType.HOST_SVCCOMP_STOP);

    startTime = timestamp;
    impl.handleEvent(stopEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(State.STOPPING,
        impl.getState());
  }

  @Test
  public void testLiveStateUpdatesForReconfigure() throws Exception {
    ServiceComponentHost sch =
        createNewServiceComponentHost("HDFS", "DATANODE", "h1", false);
    ServiceComponentHostImpl impl =  (ServiceComponentHostImpl) sch;

    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);

    Map<String, Config> desired = new HashMap<String, Config>();
    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);

    Cluster cluster = clusters.getCluster("C1");
    Config c1 = configFactory.createNew(cluster, "type1", new HashMap<String, String>());
//        new ConfigImpl(cluster, "type1", new HashMap<String, String>(), injector);
    Config c2 = configFactory.createNew(cluster, "type2", new HashMap<String, String>());
//        new ConfigImpl(cluster, "type2", new HashMap<String, String>(),        injector);
    Config c3 = configFactory.createNew(cluster, "type3", new HashMap<String, String>());
//        new ConfigImpl(cluster, "type3", new HashMap<String, String>(),   injector);
    Config c4v5 = configFactory.createNew(cluster, "type4", new HashMap<String, String>());
    Config c2v3 = configFactory.createNew(cluster, "type2", new HashMap<String, String>());


    c1.setVersionTag("v1");
    c2.setVersionTag("v1");
    c3.setVersionTag("v1");
    c4v5.setVersionTag("v5");
    c2v3.setVersionTag("v3");

    c1.persist();
    c2.persist();
    c3.persist();
    c4v5.persist();
    c2v3.persist();

    desired.put("type1", c1);
    desired.put("type2", c2);
    desired.put("type3", c3);
    impl.updateDesiredConfigs(desired);
    impl.persist();

    HostComponentDesiredStateEntityPK desiredPK =
        new HostComponentDesiredStateEntityPK();
    desiredPK.setClusterId(clusters.getCluster("C1").getClusterId());
    desiredPK.setServiceName("HDFS");
    desiredPK.setComponentName("DATANODE");
    desiredPK.setHostName("h1");

    HostComponentDesiredStateEntity desiredEntity =
        hostComponentDesiredStateDAO.findByPK(desiredPK);
    Assert.assertEquals(3,
        desiredEntity.getHostComponentDesiredConfigMappingEntities().size());

    Map<String, String> oldConfigs = new HashMap<String, String>();
    oldConfigs.put("type1", "v1");
    oldConfigs.put("type2", "v1");
    oldConfigs.put("type3", "v1");

    HostComponentStateEntityPK primaryKey =
        new HostComponentStateEntityPK();
    primaryKey.setClusterId(clusters.getCluster("C1").getClusterId());
    primaryKey.setServiceName("HDFS");
    primaryKey.setComponentName("DATANODE");
    primaryKey.setHostName("h1");
    HostComponentStateEntity entity =
        hostComponentStateDAO.findByPK(primaryKey);
    Collection<HostComponentConfigMappingEntity> entities =
        entity.getHostComponentConfigMappingEntities();
    Assert.assertEquals(0, entities.size());

    impl.setConfigs(oldConfigs);
    impl.persist();

    Assert.assertEquals(3, impl.getConfigVersions().size());
    entity = hostComponentStateDAO.findByPK(primaryKey);
    entities = entity.getHostComponentConfigMappingEntities();
    Assert.assertEquals(3, entities.size());

    Map<String, String> newConfigs = new HashMap<String, String>();
    newConfigs.put("type1", "v1");
    newConfigs.put("type2", "v3");
    newConfigs.put("type4", "v5");

    ServiceComponentHostStartEvent startEvent =
        new ServiceComponentHostStartEvent("DATANODE", "h1", 1, newConfigs);

    impl.handleEvent(startEvent);

    Assert.assertEquals(newConfigs.size(),
        impl.getConfigVersions().size());

    entity = hostComponentStateDAO.findByPK(primaryKey);
    entities = entity.getHostComponentConfigMappingEntities();
    Assert.assertEquals(3, entities.size());

    for (HostComponentConfigMappingEntity e : entities) {
      LOG.debug("Found live config "
          + e.getConfigType() + ":" + e.getVersionTag());
      Assert.assertTrue(e.getComponentName().equals("DATANODE")
          && e.getClusterId() == primaryKey.getClusterId()
          && e.getHostName().equals("h1")
          && e.getServiceName().equals("HDFS"));
      if (e.getConfigType().equals("type1")) {
        Assert.assertEquals("v1", e.getVersionTag());
      } else if (e.getConfigType().equals("type2")) {
        Assert.assertEquals("v3", e.getVersionTag());
      } else if (e.getConfigType().equals("type4")) {
        Assert.assertEquals("v5", e.getVersionTag());
      } else {
        fail("Found invalid type");
      }
    }
  }

}
