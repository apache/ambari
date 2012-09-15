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

package org.apache.ambari.server.state.live;

import org.junit.Assert;
import org.junit.Test;

public class TestServiceComponentHostImpl {

  private ServiceComponentHostImpl createNewServiceComponentHost(String svcComponent,
      String hostName, boolean isClient) {
    ServiceComponentHostImpl impl = new ServiceComponentHostImpl(svcComponent,
        hostName, isClient);
    Assert.assertEquals(ServiceComponentHostLiveState.INIT,
        impl.getState().getLiveState());
    return impl;
  }

  @Test
  public void testNewServiceComponentHostImpl() {
    createNewServiceComponentHost("svcComp", "h1", false);
    createNewServiceComponentHost("svcComp", "h1", true);
  }

  private ServiceComponentHostEvent createEvent(ServiceComponentHostImpl impl,
      long timestamp, ServiceComponentHostEventType eventType) {
    ServiceComponentHostEvent event = new ServiceComponentHostEvent(eventType,
          impl.getServiceComponentName(), impl.getHostName(), timestamp);
    return event;
  }



  private void runStateChanges(ServiceComponentHostImpl impl,
      ServiceComponentHostEventType startEvent,
      ServiceComponentHostLiveState startState,
      ServiceComponentHostLiveState inProgressState,
      ServiceComponentHostLiveState failedState,
      ServiceComponentHostLiveState completedState)
    throws Exception {
    long timestamp = 0;

    Assert.assertEquals(startState,
        impl.getState().getLiveState());
    ServiceComponentHostEvent installEvent = createEvent(impl, ++timestamp,
        startEvent);

    long startTime = timestamp;
    impl.handleEvent(installEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentHostEvent installEvent2 = createEvent(impl, ++timestamp,
        startEvent);
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
        impl.getState().getLiveState());

    ServiceComponentHostOpInProgressEvent inProgressEvent1 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent1);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentHostOpInProgressEvent inProgressEvent2 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());


    ServiceComponentHostOpFailedEvent failEvent = new
        ServiceComponentHostOpFailedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    long endTime = timestamp;
    impl.handleEvent(failEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(failedState,
        impl.getState().getLiveState());

    ServiceComponentHostOpRestartedEvent restartEvent = new
        ServiceComponentHostOpRestartedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    startTime = timestamp;
    impl.handleEvent(restartEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentHostOpInProgressEvent inProgressEvent3 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent3);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentHostOpSucceededEvent succeededEvent = new
        ServiceComponentHostOpSucceededEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    endTime = timestamp;
    impl.handleEvent(succeededEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(completedState,
        impl.getState().getLiveState());

  }

  @Test
  public void testClientStateFlow() throws Exception {
    ServiceComponentHostImpl impl = createNewServiceComponentHost("svcComp",
        "h1", true);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        ServiceComponentHostLiveState.INIT,
        ServiceComponentHostLiveState.INSTALLING,
        ServiceComponentHostLiveState.INSTALL_FAILED,
        ServiceComponentHostLiveState.INSTALLED);

    boolean exceptionThrown = false;
    try {
      runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
        ServiceComponentHostLiveState.INSTALLED,
        ServiceComponentHostLiveState.STARTING,
        ServiceComponentHostLiveState.START_FAILED,
        ServiceComponentHostLiveState.STARTED);
    }
    catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        ServiceComponentHostLiveState.INSTALLED,
        ServiceComponentHostLiveState.UNINSTALLING,
        ServiceComponentHostLiveState.UNINSTALL_FAILED,
        ServiceComponentHostLiveState.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        ServiceComponentHostLiveState.UNINSTALLED,
        ServiceComponentHostLiveState.WIPING_OUT,
        ServiceComponentHostLiveState.WIPEOUT_FAILED,
        ServiceComponentHostLiveState.INIT);

  }

  @Test
  public void testDaemonStateFlow() throws Exception {
    ServiceComponentHostImpl impl = createNewServiceComponentHost("svcComp",
        "h1", false);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        ServiceComponentHostLiveState.INIT,
        ServiceComponentHostLiveState.INSTALLING,
        ServiceComponentHostLiveState.INSTALL_FAILED,
        ServiceComponentHostLiveState.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
      ServiceComponentHostLiveState.INSTALLED,
      ServiceComponentHostLiveState.STARTING,
      ServiceComponentHostLiveState.START_FAILED,
      ServiceComponentHostLiveState.STARTED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      ServiceComponentHostLiveState.STARTED,
      ServiceComponentHostLiveState.STOPPING,
      ServiceComponentHostLiveState.STOP_FAILED,
      ServiceComponentHostLiveState.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        ServiceComponentHostLiveState.INSTALLED,
        ServiceComponentHostLiveState.UNINSTALLING,
        ServiceComponentHostLiveState.UNINSTALL_FAILED,
        ServiceComponentHostLiveState.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        ServiceComponentHostLiveState.UNINSTALLED,
        ServiceComponentHostLiveState.WIPING_OUT,
        ServiceComponentHostLiveState.WIPEOUT_FAILED,
        ServiceComponentHostLiveState.INIT);

  }


}
