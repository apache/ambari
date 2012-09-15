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

public class TestServiceComponentNodeImpl {

  private ServiceComponentNodeImpl createNewServiceComponentNode(String svcComponent,
      String hostName, boolean isClient) {
    ServiceComponentNodeImpl impl = new ServiceComponentNodeImpl(svcComponent,
        hostName, isClient);
    Assert.assertEquals(ServiceComponentNodeLiveState.INIT,
        impl.getState().getLiveState());
    return impl;
  }

  @Test
  public void testNewServiceComponentNodeImpl() {
    createNewServiceComponentNode("svcComp", "h1", false);
    createNewServiceComponentNode("svcComp", "h1", true);
  }

  private ServiceComponentNodeEvent createEvent(ServiceComponentNodeImpl impl,
      long timestamp, ServiceComponentNodeEventType eventType) {
    ServiceComponentNodeEvent event = new ServiceComponentNodeEvent(eventType,
          impl.getServiceComponentName(), impl.getNodeName(), timestamp);
    return event;
  }



  private void runStateChanges(ServiceComponentNodeImpl impl,
      ServiceComponentNodeEventType startEvent,
      ServiceComponentNodeLiveState startState,
      ServiceComponentNodeLiveState inProgressState,
      ServiceComponentNodeLiveState failedState,
      ServiceComponentNodeLiveState completedState)
    throws Exception {
    long timestamp = 0;

    Assert.assertEquals(startState,
        impl.getState().getLiveState());
    ServiceComponentNodeEvent installEvent = createEvent(impl, ++timestamp,
        startEvent);

    long startTime = timestamp;
    impl.handleEvent(installEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentNodeEvent installEvent2 = createEvent(impl, ++timestamp,
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

    ServiceComponentNodeOpInProgressEvent inProgressEvent1 = new
        ServiceComponentNodeOpInProgressEvent(impl.getServiceComponentName(),
            impl.getNodeName(), ++timestamp);
    impl.handleEvent(inProgressEvent1);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentNodeOpInProgressEvent inProgressEvent2 = new
        ServiceComponentNodeOpInProgressEvent(impl.getServiceComponentName(),
            impl.getNodeName(), ++timestamp);
    impl.handleEvent(inProgressEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());


    ServiceComponentNodeOpFailedEvent failEvent = new
        ServiceComponentNodeOpFailedEvent(impl.getServiceComponentName(),
            impl.getNodeName(), ++timestamp);
    long endTime = timestamp;
    impl.handleEvent(failEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(failedState,
        impl.getState().getLiveState());

    ServiceComponentNodeOpRestartedEvent restartEvent = new
        ServiceComponentNodeOpRestartedEvent(impl.getServiceComponentName(),
            impl.getNodeName(), ++timestamp);
    startTime = timestamp;
    impl.handleEvent(restartEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentNodeOpInProgressEvent inProgressEvent3 = new
        ServiceComponentNodeOpInProgressEvent(impl.getServiceComponentName(),
            impl.getNodeName(), ++timestamp);
    impl.handleEvent(inProgressEvent3);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState().getLiveState());

    ServiceComponentNodeOpSucceededEvent succeededEvent = new
        ServiceComponentNodeOpSucceededEvent(impl.getServiceComponentName(),
            impl.getNodeName(), ++timestamp);
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
    ServiceComponentNodeImpl impl = createNewServiceComponentNode("svcComp",
        "h1", true);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
        ServiceComponentNodeLiveState.INIT,
        ServiceComponentNodeLiveState.INSTALLING,
        ServiceComponentNodeLiveState.INSTALL_FAILED,
        ServiceComponentNodeLiveState.INSTALLED);

    boolean exceptionThrown = false;
    try {
      runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_START,
        ServiceComponentNodeLiveState.INSTALLED,
        ServiceComponentNodeLiveState.STARTING,
        ServiceComponentNodeLiveState.START_FAILED,
        ServiceComponentNodeLiveState.STARTED);
    }
    catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_UNINSTALL,
        ServiceComponentNodeLiveState.INSTALLED,
        ServiceComponentNodeLiveState.UNINSTALLING,
        ServiceComponentNodeLiveState.UNINSTALL_FAILED,
        ServiceComponentNodeLiveState.UNINSTALLED);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_WIPEOUT,
        ServiceComponentNodeLiveState.UNINSTALLED,
        ServiceComponentNodeLiveState.WIPING_OUT,
        ServiceComponentNodeLiveState.WIPEOUT_FAILED,
        ServiceComponentNodeLiveState.INIT);

  }

  @Test
  public void testDaemonStateFlow() throws Exception {
    ServiceComponentNodeImpl impl = createNewServiceComponentNode("svcComp",
        "h1", false);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
        ServiceComponentNodeLiveState.INIT,
        ServiceComponentNodeLiveState.INSTALLING,
        ServiceComponentNodeLiveState.INSTALL_FAILED,
        ServiceComponentNodeLiveState.INSTALLED);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_START,
      ServiceComponentNodeLiveState.INSTALLED,
      ServiceComponentNodeLiveState.STARTING,
      ServiceComponentNodeLiveState.START_FAILED,
      ServiceComponentNodeLiveState.STARTED);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_STOP,
      ServiceComponentNodeLiveState.STARTED,
      ServiceComponentNodeLiveState.STOPPING,
      ServiceComponentNodeLiveState.STOP_FAILED,
      ServiceComponentNodeLiveState.INSTALLED);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_UNINSTALL,
        ServiceComponentNodeLiveState.INSTALLED,
        ServiceComponentNodeLiveState.UNINSTALLING,
        ServiceComponentNodeLiveState.UNINSTALL_FAILED,
        ServiceComponentNodeLiveState.UNINSTALLED);

    runStateChanges(impl, ServiceComponentNodeEventType.NODE_SVCCOMP_WIPEOUT,
        ServiceComponentNodeLiveState.UNINSTALLED,
        ServiceComponentNodeLiveState.WIPING_OUT,
        ServiceComponentNodeLiveState.WIPEOUT_FAILED,
        ServiceComponentNodeLiveState.INIT);

  }


}
