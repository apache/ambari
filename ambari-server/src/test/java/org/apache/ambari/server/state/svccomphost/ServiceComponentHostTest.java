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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
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
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

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
  Provider<EntityManager> entityManagerProvider;
  @Inject
  private ConfigFactory configFactory;
  @Inject
  private ConfigGroupFactory configGroupFactory;
  @Inject
  private ConfigHelper configHelper;
  @Inject
  private OrmTestHelper helper;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private HostDAO hostDAO;

  private String clusterName = "c1";
  private String hostName1 = "h1";
  private Map<String, String> hostAttributes = new HashMap<String, String>();


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    StackId stackId = new StackId("HDP-0.1");
    createCluster(stackId, clusterName);
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");

    Set<String> hostNames = new HashSet<String>();
    hostNames.add(hostName1);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    Cluster c1 = clusters.getCluster(clusterName);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  private ClusterEntity createCluster(StackId stackId, String clusterName) throws AmbariException {
    clusters.addCluster(clusterName, stackId);
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
    Assert.assertNotNull(clusterEntity);
    return clusterEntity;
  }

  private void addHostsToCluster(String clusterName, Map<String, String> hostAttributes, Set<String> hostNames) throws AmbariException {
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    for (String hostName : hostNames) {
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);
      host.setHostAttributes(hostAttributes);
      host.persist();
    }

    clusterEntity.setHostEntities(hostEntities);
    clusterDAO.merge(clusterEntity);

    clusters.mapHostsToCluster(hostNames, clusterName);
  }

  private ServiceComponentHost createNewServiceComponentHost(String clusterName,
      String svc,
      String svcComponent,
      String hostName, boolean isClient) throws AmbariException{
    Cluster c = clusters.getCluster(clusterName);
    Assert.assertNotNull(c.getConfigGroups());
    return createNewServiceComponentHost(c, svc, svcComponent, hostName);
  }

  private ServiceComponentHost createNewServiceComponentHost(
      Cluster c,
      String svc,
      String svcComponent,
      String hostName) throws AmbariException{

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
        sc, hostName);

    impl.persist();

    Assert.assertEquals(State.INIT, impl.getState());
    Assert.assertEquals(State.INIT, impl.getDesiredState());
    Assert.assertEquals(SecurityState.UNSECURED, impl.getSecurityState());
    Assert.assertEquals(SecurityState.UNSECURED, impl.getDesiredSecurityState());
    Assert.assertEquals(c.getClusterName(), impl.getClusterName());
    Assert.assertEquals(c.getClusterId(), impl.getClusterId());
    Assert.assertEquals(s.getName(), impl.getServiceName());
    Assert.assertEquals(sc.getName(), impl.getServiceComponentName());
    Assert.assertEquals(hostName, impl.getHostName());

    Assert.assertNotNull(c.getServiceComponentHosts(hostName));

    Assert.assertFalse(
        impl.getDesiredStackVersion().getStackId().isEmpty());

    Assert.assertFalse(impl.getStackVersion().getStackId().isEmpty());

    return impl;
  }

  @Test
  public void testNewServiceComponentHost() throws AmbariException{
    createNewServiceComponentHost(clusterName, "HDFS", "NAMENODE", hostName1, false);
    createNewServiceComponentHost(clusterName, "HDFS", "HDFS_CLIENT", hostName1, true);
  }

  private ServiceComponentHostEvent createEvent(ServiceComponentHostImpl impl,
      long timestamp, ServiceComponentHostEventType eventType)
      throws AmbariException {

    Cluster c = clusters.getCluster(clusterName);
    if (c.getConfig("time", String.valueOf(timestamp)) == null) {
      Config config = configFactory.createNew (c, "time",
          new HashMap<String, String>(), new HashMap<String, Map<String,String>>());
      config.setTag(String.valueOf(timestamp));
      c.addConfig(config);
      config.persist();
    }

    switch (eventType) {
      case HOST_SVCCOMP_INSTALL:
        return new ServiceComponentHostInstallEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp,
            impl.getDesiredStackVersion().getStackId());
      case HOST_SVCCOMP_START:
        return new ServiceComponentHostStartEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
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
      case HOST_SVCCOMP_DISABLE:
          return new ServiceComponentHostDisableEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_WIPEOUT:
        return new ServiceComponentHostWipeoutEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      default:
        return null;
    }
  }

  private void runStateChanges(ServiceComponentHostImpl impl,
      ServiceComponentHostEventType startEventType,
      State startState,
      State inProgressState,
      State failedState,
      State completedState)
    throws Exception {
    long timestamp = 0;

    boolean checkStack = false;
    if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL) {
      checkStack = true;
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
    if (impl.getState() == State.INSTALLING || impl.getState() == State.STARTING
      || impl.getState() == State.UNINSTALLING
        || impl.getState() == State.WIPING_OUT
        || impl.getState() == State.STARTED
        ) {
      startTime = timestamp;
    // Exception is not expected on valid event
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

    ServiceComponentHostEvent succeededEvent;
    if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_START) {
      succeededEvent = new ServiceComponentHostStartedEvent(impl.getServiceComponentName(),
          impl.getHostName(), ++timestamp);
    } else if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_STOP) {
      succeededEvent = new ServiceComponentHostStoppedEvent(impl.getServiceComponentName(),
          impl.getHostName(), ++timestamp);
    } else {
      succeededEvent = new
          ServiceComponentHostOpSucceededEvent(impl.getServiceComponentName(),
          impl.getHostName(), ++timestamp);
    }

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
        createNewServiceComponentHost(clusterName, "HDFS", "HDFS_CLIENT", hostName1, true);

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
        State.INSTALLED,
        State.STARTED);
    }
    catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        State.INSTALLED,
        State.UNINSTALLING,
        State.UNINSTALLING,
        State.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        State.UNINSTALLED,
        State.WIPING_OUT,
        State.WIPING_OUT,
        State.INIT);

  }

  @Test
  public void testDaemonStateFlow() throws Exception {
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        State.INIT,
        State.INSTALLING,
        State.INSTALL_FAILED,
        State.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
      State.INSTALLED,
      State.STARTING,
      State.INSTALLED,
      State.STARTED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      State.STARTED,
      State.STOPPING,
      State.STARTED,
      State.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        State.INSTALLED,
        State.UNINSTALLING,
        State.UNINSTALLING,
        State.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        State.UNINSTALLED,
        State.WIPING_OUT,
        State.WIPING_OUT,
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
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "NAMENODE", hostName1, false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    sch.setStackVersion(new StackId("HDP-1.2.0"));
    sch.setDesiredStackVersion(new StackId("HDP-1.2.0"));

    Assert.assertEquals(State.INSTALLING, sch.getState());
    Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
    Assert.assertEquals("HDP-1.2.0",
        sch.getStackVersion().getStackId());
    Assert.assertEquals("HDP-1.2.0",
        sch.getDesiredStackVersion().getStackId());
  }

  @Test
  public void testActualConfigs() throws Exception {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "NAMENODE", hostName1, false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    sch.setStackVersion(new StackId("HDP-1.2.0"));
    sch.setDesiredStackVersion(new StackId("HDP-1.2.0"));

    Cluster cluster = clusters.getCluster(clusterName);

    final ConfigGroup configGroup = configGroupFactory.createNew(cluster,
      "cg1", "t1", "", new HashMap<String, Config>(), new HashMap<Long, Host>());

    configGroup.persist();
    cluster.addConfigGroup(configGroup);

    Map<String, Map<String,String>> actual =
        new HashMap<String, Map<String, String>>() {{
          put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
          put("core-site", new HashMap<String,String>() {{ put("tag", "version1");
            put(configGroup.getId().toString(), "version2"); }});
        }};

    sch.updateActualConfigs(actual);

    Map<String, HostConfig> confirm = sch.getActualConfigs();

    Assert.assertEquals(2, confirm.size());
    Assert.assertTrue(confirm.containsKey("global"));
    Assert.assertTrue(confirm.containsKey("core-site"));
    Assert.assertEquals(1, confirm.get("core-site").getConfigGroupOverrides().size());
  }

  @Test
  public void testConvertToResponse() throws AmbariException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    sch.setStackVersion(new StackId("HDP-1.2.0"));
    ServiceComponentHostResponse r = sch.convertToResponse();
    Assert.assertEquals("HDFS", r.getServiceName());
    Assert.assertEquals("DATANODE", r.getComponentName());
    Assert.assertEquals(hostName1, r.getHostname());
    Assert.assertEquals(clusterName, r.getClusterName());
    Assert.assertEquals(State.INSTALLED.toString(), r.getDesiredState());
    Assert.assertEquals(State.INSTALLING.toString(), r.getLiveState());
    Assert.assertEquals("HDP-1.2.0", r.getStackVersion());

    Assert.assertFalse(r.isStaleConfig());

    // TODO check configs

    StringBuilder sb = new StringBuilder();
    sch.debugDump(sb);
    Assert.assertFalse(sb.toString().isEmpty());
  }

  @Test
  public void testStopInVariousStates() throws AmbariException, InvalidStateTransitionException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);
    ServiceComponentHostImpl impl =  (ServiceComponentHostImpl) sch;

    sch.setDesiredState(State.STARTED);
    sch.setState(State.INSTALLED);

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
  public void testDisableInVariousStates() throws AmbariException, InvalidStateTransitionException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS",
        "DATANODE", hostName1, false);
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl) sch;

    // Test valid states in which host component can be disabled
    long timestamp = 0;
    HashSet<State> validStates = new HashSet<State>();
    validStates.add(State.INSTALLED);
    validStates.add(State.INSTALL_FAILED);
    validStates.add(State.UNKNOWN);
    validStates.add(State.DISABLED);
    for (State state : validStates) {
      sch.setState(state);
      ServiceComponentHostEvent disableEvent = createEvent(impl, ++timestamp,
          ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE);
      impl.handleEvent(disableEvent);
      // TODO: At present operation timestamps are not getting updated.
      Assert.assertEquals(-1, impl.getLastOpStartTime());
      Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
      Assert.assertEquals(-1, impl.getLastOpEndTime());
      Assert.assertEquals(State.DISABLED, impl.getState());
    }

    // Test invalid states in which host component cannot be disabled
    HashSet<State> invalidStates = new HashSet<State>();
    invalidStates.add(State.INIT);
    invalidStates.add(State.INSTALLING);
    invalidStates.add(State.STARTING);
    invalidStates.add(State.STARTED);
    invalidStates.add(State.STOPPING);
    invalidStates.add(State.UNINSTALLING);
    invalidStates.add(State.UNINSTALLED);
    invalidStates.add(State.UPGRADING);

    for (State state : invalidStates) {
      sch.setState(state);
      ServiceComponentHostEvent disableEvent = createEvent(impl, ++timestamp,
          ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE);
      boolean exceptionThrown = false;
      try {
        impl.handleEvent(disableEvent);
      } catch (Exception e) {
        exceptionThrown = true;
      }
      Assert.assertTrue("Exception not thrown on invalid event",
          exceptionThrown);
      // TODO: At present operation timestamps are not getting updated.
      Assert.assertEquals(-1, impl.getLastOpStartTime());
      Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
      Assert.assertEquals(-1, impl.getLastOpEndTime());
    }
  }

  @Test
  public void testCanBeRemoved() throws Exception{
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost(clusterName, "HDFS", "HDFS_CLIENT", hostName1, true);

    for (State state : State.values()) {
      impl.setState(state);

      if (state.isRemovableState()) {
        Assert.assertTrue(impl.canBeRemoved());
      }
      else {
        Assert.assertFalse(impl.canBeRemoved());
      }
    }
  }

  @Test
  public void testStaleConfigs() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<String>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    final HostEntity hostEntity = hostDAO.findByName(hostName);
    Assert.assertNotNull(hostEntity.getHostId());

    Cluster cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);
    ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName);

    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);
    sch1.setStackVersion(new StackId(stackVersion));

    sch2.setDesiredState(State.INSTALLED);
    sch2.setState(State.INSTALLING);
    sch2.setStackVersion(new StackId(stackVersion));

    sch3.setDesiredState(State.INSTALLED);
    sch3.setState(State.INSTALLING);
    sch3.setStackVersion(new StackId(stackVersion));

    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    makeConfig(cluster, "global", "version1",
        new HashMap<String,String>() {{
          put("a", "b");
          put("dfs_namenode_name_dir", "/foo1"); // HDFS only
          put("mapred_log_dir_prefix", "/foo2"); // MR2 only
        }}, new HashMap<String, Map<String,String>>());

    Map<String, Map<String, String>> actual = new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }};

    sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    sch3.updateActualConfigs(actual);

    makeConfig(cluster, "foo", "version1",
        new HashMap<String,String>() {{ put("a", "c"); }}, new HashMap<String, Map<String,String>>());

    // HDP-x/HDFS does not define type 'foo', so changes do not count to stale
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    makeConfig(cluster, "hdfs-site", "version1",
        new HashMap<String,String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());

    // HDP-x/HDFS/hdfs-site is not on the actual, but it is defined, so it is stale
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());

    actual.put("hdfs-site", new HashMap<String, String>() {{ put ("tag", "version1"); }});

    sch1.updateActualConfigs(actual);
    // previous value from cache
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch1.setRestartRequired(false);
    // HDP-x/HDFS/hdfs-site up to date, only for sch1
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());

    sch2.updateActualConfigs(actual);
    // previous value from cache
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    //reset restartRequired flag + invalidating isStale cache(
    // after start/restart command execution completed)
    sch2.setRestartRequired(false);
    // HDP-x/HDFS/hdfs-site up to date for both
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    makeConfig(cluster, "hdfs-site", "version2",
        new HashMap<String, String>() {{ put("dfs.journalnode.http-address", "http://foo"); }},
        new HashMap<String, Map<String,String>>());

    // HDP-x/HDFS/hdfs-site updated to changed property
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());

    actual.get("hdfs-site").put("tag", "version2");
    sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    // HDP-x/HDFS/hdfs-site updated to changed property
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    // make a host override
    final Host host = clusters.getHostsForCluster(clusterName).get(hostName);
    Assert.assertNotNull(host);

    final Config c = configFactory.createNew(cluster, "hdfs-site",
        new HashMap<String, String>() {{ put("dfs.journalnode.http-address", "http://goo"); }},
        new HashMap<String, Map<String,String>>());
    c.setTag("version3");
    c.persist();
    cluster.addConfig(c);
    host.addDesiredConfig(cluster.getClusterId(), true, "user", c);
    ConfigGroup configGroup = configGroupFactory.createNew(cluster, "g1",
      "t1", "", new HashMap<String, Config>() {{ put("hdfs-site", c); }},
      new HashMap<Long, Host>() {{ put(hostEntity.getHostId(), host); }});
    configGroup.persist();
    cluster.addConfigGroup(configGroup);

    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());

    actual.get("hdfs-site").put(configGroup.getId().toString(), "version3");
    sch2.updateActualConfigs(actual);
    // previous value from cache
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch2.setRestartRequired(false);
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    sch1.updateActualConfigs(actual);
    // previous value from cache
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch1.setRestartRequired(false);
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    // change 'global' property only affecting global/HDFS
    makeConfig(cluster, "global", "version2",
      new HashMap<String,String>() {{
        put("a", "b");
        put("dfs_namenode_name_dir", "/foo3"); // HDFS only
        put("mapred_log_dir_prefix", "/foo2"); // MR2 only
      }}, new HashMap<String, Map<String,String>>());

    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());

    // Change core-site property, only HDFS property
    makeConfig(cluster, "core-site", "version1",
      new HashMap<String,String>() {{
        put("a", "b");
        put("fs.trash.interval", "360"); // HDFS only
      }}, new HashMap<String, Map<String,String>>());

    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());

    actual.put("core-site", new HashMap<String, String>() {{
      put("tag", "version1");
    }});

    sch1.updateActualConfigs(actual);

    final Config c1 = configFactory.createNew(cluster, "core-site",
      new HashMap<String, String>() {{ put("fs.trash.interval", "400"); }},
      new HashMap<String, Map<String,String>>());
    c1.setTag("version2");
    c1.persist();
    cluster.addConfig(c1);
    configGroup = configGroupFactory.createNew(cluster, "g2",
      "t2", "", new HashMap<String, Config>() {{ put("core-site", c1); }},
      new HashMap<Long, Host>() {{ put(hostEntity.getHostId(), host); }});
    configGroup.persist();
    cluster.addConfigGroup(configGroup);

    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());

    // Test actual configs are updated for deleted config group
    Long id = configGroup.getId();
    HashMap<String, String> tags = new HashMap<String, String>(2);
    tags.put("tag", "version1");
    tags.put(id.toString(), "version2");
    actual.put("core-site", tags);
    sch3.updateActualConfigs(actual);
    // previous value from cache
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch3.setRestartRequired(false);

    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());

    cluster.deleteConfigGroup(id);
    Assert.assertNull(cluster.getConfigGroups().get(id));

    sch3.updateActualConfigs(actual);
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());

    tags.remove(id.toString());
    sch3.updateActualConfigs(actual);
    // previous value from cache
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch3.setRestartRequired(false);
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());
  }

  @Test
  public void testStaleConfigsAttributes() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<String>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    Cluster cluster = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);
    ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName);

    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);
    sch1.setStackVersion(new StackId(stackVersion));

    sch2.setDesiredState(State.INSTALLED);
    sch2.setState(State.INSTALLING);
    sch2.setStackVersion(new StackId(stackVersion));

    sch3.setDesiredState(State.INSTALLED);
    sch3.setState(State.INSTALLING);
    sch3.setStackVersion(new StackId(stackVersion));

    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());

    makeConfig(cluster, "global", "version1",
        new HashMap<String,String>() {{
          put("a", "b");
          put("dfs_namenode_name_dir", "/foo1"); // HDFS only
          put("mapred_log_dir_prefix", "/foo2"); // MR2 only
        }}, new HashMap<String, Map<String,String>>());
    makeConfig(cluster, "hdfs-site", "version1",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, new HashMap<String, Map<String,String>>());
    Map<String, Map<String, String>> actual = new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
      put("hdfs-site", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }};

    sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    sch3.updateActualConfigs(actual);

    makeConfig(cluster, "mapred-site", "version1",
      new HashMap<String,String>() {{ put("a", "c"); }},new HashMap<String, Map<String,String>>(){{
       put("final", new HashMap<String, String>(){{
         put("a", "true");
       }});
      }});
    // HDP-x/HDFS does not define type 'foo', so changes do not count to stale
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());
    actual = new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
      put("mapred-site", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }};
    sch3.setRestartRequired(false);
    sch3.updateActualConfigs(actual);
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());

    // Now add config-attributes
    Map<String, Map<String, String>> c1PropAttributes = new HashMap<String, Map<String,String>>();
    c1PropAttributes.put("final", new HashMap<String, String>());
    c1PropAttributes.get("final").put("hdfs1", "true");
    makeConfig(cluster, "hdfs-site", "version2",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, c1PropAttributes);
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    sch3.setRestartRequired(false);
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());

    // Now change config-attributes
    Map<String, Map<String, String>> c2PropAttributes = new HashMap<String, Map<String,String>>();
    c2PropAttributes.put("final", new HashMap<String, String>());
    c2PropAttributes.get("final").put("hdfs1", "false");
    makeConfig(cluster, "hdfs-site", "version3",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, c2PropAttributes);
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    sch3.setRestartRequired(false);
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());

    // Now change config-attributes
    makeConfig(cluster, "hdfs-site", "version4",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, new HashMap<String, Map<String,String>>());
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    sch3.setRestartRequired(false);
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());
  }

  /**
   * Helper method to create a configuration
   * @param cluster the cluster
   * @param type the config type
   * @param tag the config tag
   * @param values the values for the config
   */
  private void makeConfig(Cluster cluster, String type, String tag, Map<String, String> values, Map<String, Map<String, String>> attributes) {
    Config config = configFactory.createNew(cluster, type, values, attributes);
    config.setTag(tag);
    config.persist();
    cluster.addConfig(config);
    cluster.addDesiredConfig("user", Collections.singleton(config));
  }

  @Test
  public void testMaintenance() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<String>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    Cluster cluster = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    HostEntity hostEntity = hostDAO.findByName(hostName);
    Assert.assertNotNull(hostEntity);

    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);
    ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName);

    HostComponentDesiredStateEntityPK pk = new HostComponentDesiredStateEntityPK();
    pk.setClusterId(Long.valueOf(cluster.getClusterId()));
    pk.setComponentName(sch1.getServiceComponentName());
    pk.setServiceName(sch1.getServiceName());
    pk.setHostId(hostEntity.getHostId());

    HostComponentDesiredStateDAO dao = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentDesiredStateEntity entity = dao.findByPK(pk);
    Assert.assertEquals(MaintenanceState.OFF, entity.getMaintenanceState());
    Assert.assertEquals(MaintenanceState.OFF, sch1.getMaintenanceState());

    sch1.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON, sch1.getMaintenanceState());

    entity = dao.findByPK(pk);
    Assert.assertEquals(MaintenanceState.ON, entity.getMaintenanceState());
  }


  @Test
  public void testSecurityState() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<String>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    Cluster cluster = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    HostEntity hostEntity = hostDAO.findByName(hostName);
    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);

    HostComponentDesiredStateDAO daoHostComponentDesiredState = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentDesiredStateEntity entityHostComponentDesiredState;
    HostComponentDesiredStateEntityPK pkHostComponentDesiredState = new HostComponentDesiredStateEntityPK();
    pkHostComponentDesiredState.setClusterId(cluster.getClusterId());
    pkHostComponentDesiredState.setComponentName(sch1.getServiceComponentName());
    pkHostComponentDesiredState.setServiceName(sch1.getServiceName());
    pkHostComponentDesiredState.setHostId(hostEntity.getHostId());

    HostComponentStateDAO daoHostComponentState = injector.getInstance(HostComponentStateDAO.class);
    HostComponentStateEntity entityHostComponentState;

    for(SecurityState state: SecurityState.values()) {
      sch1.setSecurityState(state);
      entityHostComponentState = daoHostComponentState.findByIndex(cluster.getClusterId(),
          sch1.getServiceName(), sch1.getServiceComponentName(), hostEntity.getHostId());

      Assert.assertNotNull(entityHostComponentState);
      Assert.assertEquals(state, entityHostComponentState.getSecurityState());

      try {
        sch1.setDesiredSecurityState(state);
        Assert.assertTrue(state.isEndpoint());
        entityHostComponentDesiredState = daoHostComponentDesiredState.findByPK(pkHostComponentDesiredState);
        Assert.assertNotNull(entityHostComponentDesiredState);
        Assert.assertEquals(state, entityHostComponentDesiredState.getSecurityState());
      } catch (AmbariException e) {
        Assert.assertFalse(state.isEndpoint());
      }
    }
  }
}
