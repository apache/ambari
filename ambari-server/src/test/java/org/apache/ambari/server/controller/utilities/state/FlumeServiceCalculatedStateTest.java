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

package org.apache.ambari.server.controller.utilities.state;

import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

public class FlumeServiceCalculatedStateTest extends GeneralServiceCalculatedStateTest{
  @Override
  protected String getServiceName() {
    return "FLUME";
  }

  @Override
  protected ServiceCalculatedState getServiceCalculatedStateObject() {
    return new FlumeServiceCalculatedState();
  }

  @Override
  protected void createComponentsAndHosts() throws Exception {
    ServiceComponent masterComponent = service.addServiceComponent("FLUME_HANDLER");


    for (String hostName: hosts){
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6.3");
      host.setHostAttributes(hostAttributes);
      host.setState(HostState.HEALTHY);
      host.persist();
      clusters.mapHostToCluster(hostName, clusterName);

      ServiceComponentHost sch = masterComponent.addServiceComponentHost(hostName);
      sch.setVersion("2.1.1.0");
      sch.setState(State.STARTED);
    }
  }

  @Override
  public void testServiceState_STARTED() throws Exception {
    updateServiceState(State.STARTED);

    State state = serviceCalculatedState.getState(clusterName, getServiceName());
    Assert.assertEquals(State.STARTED, state);
  }

  @Override
  public void testServiceState_STOPPED() throws Exception {
    updateServiceState(State.INSTALLED);

    State state = serviceCalculatedState.getState(clusterName, getServiceName());
    Assert.assertEquals(State.INSTALLED, state);
  }
}
