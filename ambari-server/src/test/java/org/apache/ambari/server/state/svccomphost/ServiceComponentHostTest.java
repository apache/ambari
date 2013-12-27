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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostConfig;
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
    
    return createNewServiceComponentHost(c, svc, svcComponent, hostName, isClient);
  }
  private ServiceComponentHost createNewServiceComponentHost(
      Cluster c,
      String svc,
      String svcComponent,
      String hostName, boolean isClient) throws AmbariException{

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
    Assert.assertEquals(c.getClusterName(), impl.getClusterName());
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
    if (c.getConfig("time", "" + timestamp) == null) {
      Config config = configFactory.createNew (c, "time",
          new HashMap<String, String>());
      config.setVersionTag("" + timestamp);
      c.addConfig(config);
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
        createNewServiceComponentHost("HDFS", "DATANODE", "h1", false);

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
  public void testActualConfigs() throws Exception {
    ServiceComponentHost sch =
        createNewServiceComponentHost("HDFS", "NAMENODE", "h1", false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    sch.setStackVersion(new StackId("HDP-1.0.0"));
    sch.setDesiredStackVersion(new StackId("HDP-1.1.0"));

    Cluster cluster = clusters.getCluster("C1");

    final ConfigGroup configGroup = configGroupFactory.createNew(cluster,
      "cg1", "t1", "", new HashMap<String, Config>(), new HashMap<String, Host>());

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
    
    Assert.assertFalse(r.isStaleConfig());

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
  public void testCanBeRemoved() throws Exception{
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost("HDFS", "HDFS_CLIENT", "h1", true);

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
    String stackVersion="HDP-2.0.6";
    String clusterName = "c2";
    String hostName = "h3";
    
    clusters.addCluster(clusterName);
    clusters.addHost(hostName);
    clusters.getHost(hostName).setOsType("centos5");
    clusters.getHost(hostName).persist();
    clusters.getCluster(clusterName).setDesiredStackVersion(
        new StackId(stackVersion));
    metaInfo.init();
    clusters.mapHostToCluster(hostName, clusterName);    
    
    Cluster cluster = clusters.getCluster(clusterName);
    
    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName, false);
    ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName, false);
    ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName, false);
    
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
        }});

    Map<String, Map<String, String>> actual = new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }};
    
    sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    sch3.updateActualConfigs(actual);

    makeConfig(cluster, "foo", "version1",
        new HashMap<String,String>() {{ put("a", "c"); }});

    // HDP-x/HDFS does not define type 'foo', so changes do not count to stale
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());
    
    makeConfig(cluster, "hdfs-site", "version1",
        new HashMap<String,String>() {{ put("a", "b"); }});
    
    // HDP-x/HDFS/hdfs-site is not on the actual, but it is defined, so it is stale
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());

    actual.put("hdfs-site", new HashMap<String, String>() {{ put ("tag", "version1"); }});
    
    sch1.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site up to date, only for sch1
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    
    sch2.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site up to date for both
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());
    
    makeConfig(cluster, "hdfs-site", "version2",
        new HashMap<String, String>() {{ put("dfs.journalnode.http-address", "http://foo"); }});

    // HDP-x/HDFS/hdfs-site updated to changed property
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    
    actual.get("hdfs-site").put("tag", "version2");
    sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site updated to changed property
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());
    
    // make a host override
    final Host host = clusters.getHostsForCluster(cluster.getClusterName()).get(hostName);
    Assert.assertNotNull(host);
    
    final Config c = configFactory.createNew(cluster, "hdfs-site",
        new HashMap<String, String>() {{ put("dfs.journalnode.http-address", "http://goo"); }});
    c.setVersionTag("version3");
    c.persist();
    cluster.addConfig(c);
    //host.addDesiredConfig(cluster.getClusterId(), true, "user", c);
    ConfigGroup configGroup = configGroupFactory.createNew(cluster, "g1",
      "t1", "", new HashMap<String, Config>() {{ put("hdfs-site", c); }},
      new HashMap<String, Host>() {{ put("h3", host); }});
    configGroup.persist();
    cluster.addConfigGroup(configGroup);
    
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    
    actual.get("hdfs-site").put(configGroup.getId().toString(), "version3");
    sch2.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());
    
    sch1.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertFalse(sch1.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse().isStaleConfig());
    
    // change 'global' property only affecting global/HDFS
    makeConfig(cluster, "global", "version2",
      new HashMap<String,String>() {{
        put("a", "b");
        put("dfs_namenode_name_dir", "/foo3"); // HDFS only
        put("mapred_log_dir_prefix", "/foo2"); // MR2 only
      }});
    
    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse().isStaleConfig());

    // Change core-site property, only HDFS property
    makeConfig(cluster, "core-site", "version1",
      new HashMap<String,String>() {{
        put("a", "b");
        put("fs.trash.interval", "360"); // HDFS only
      }});

    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());

    actual.put("core-site", new HashMap<String, String>() {{
      put("tag", "version1");
    }});

    sch1.updateActualConfigs(actual);

    final Config c1 = configFactory.createNew(cluster, "core-site",
      new HashMap<String, String>() {{ put("fs.trash.interval", "400"); }});
    c1.setVersionTag("version2");
    c1.persist();
    cluster.addConfig(c1);
    configGroup = configGroupFactory.createNew(cluster, "g2",
      "t2", "", new HashMap<String, Config>() {{ put("core-site", c1); }},
      new HashMap<String, Host>() {{ put("h3", host); }});
    configGroup.persist();
    cluster.addConfigGroup(configGroup);

    Assert.assertTrue(sch1.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse().isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse().isStaleConfig());
  }

  /**
   * Helper method to create a configuration
   * @param cluster the cluster
   * @param type the config type
   * @param tag the config tag
   * @param values the values for the config
   */
  private void makeConfig(Cluster cluster, String type, String tag, Map<String, String> values) {
    Config config = configFactory.createNew(cluster, type, values);
    config.setVersionTag(tag);
    config.persist();
    cluster.addConfig(config);
    cluster.addDesiredConfig("user", config);
  }

  
  
}
