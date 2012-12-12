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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpRestartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUninstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostWipeoutEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;

public class ServiceComponentHostTest {

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

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addCluster("C1");
    clusters.addHost("h1");
    clusters.mapHostToCluster("h1","C1");
    clusters.getHost("h1").persist();
    clusters.getCluster("C1").setDesiredStackVersion(
        new StackId("HDP-0.1"));
    metaInfo.init();
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
      Log.debug("Calling service create"
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
      long timestamp, ServiceComponentHostEventType eventType) {
    switch (eventType) {
      case HOST_SVCCOMP_INSTALL:
        return new ServiceComponentHostInstallEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
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

    ServiceComponentHostEvent installEvent2 = createEvent(impl, ++timestamp,
        startEventType);
    boolean exceptionThrown = false;
    try {
      impl.handleEvent(installEvent2);
    } catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);
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

}
