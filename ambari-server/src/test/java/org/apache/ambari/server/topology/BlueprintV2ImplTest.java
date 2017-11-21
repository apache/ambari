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
package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class BlueprintV2ImplTest {

  private ServiceGroup serviceGroup;
  private HostGroupV2Impl hostGroup;

  @Before
  public void setUp() throws Exception {
    serviceGroup = new ServiceGroup();
    serviceGroup.setName("CORE");

    hostGroup = new HostGroupV2Impl();
    hostGroup.setName("node");
    hostGroup.setBlueprintName("blue");
    hostGroup.setCardinality("1");
  }

  @Test
  public void getComponentsForServiceId() {
    Set<ComponentV2> components = Sets.newHashSet(
      createZookeeperServer(serviceGroup, "ZK1"),
      createZookeeperServer(serviceGroup, "ZK2")
    );
    BlueprintV2Impl bp = createBlueprint(components);

    Set<ServiceId> serviceIds = serviceGroup.getServices().stream()
      .map(Service::getId)
      .collect(toSet());
    for (ServiceId serviceId : serviceIds) {
      Set<ComponentV2> expectedComponents = components.stream()
        .filter(c -> c.getServiceId().equals(serviceId))
        .collect(toSet());
      assertEquals(expectedComponents, Sets.newHashSet(bp.getComponents(serviceId)));
    }
  }

  @Test
  public void getComponentsForService() {
    Set<ComponentV2> components = Sets.newHashSet(
      createZookeeperServer(serviceGroup, "ZK1"),
      createZookeeperServer(serviceGroup, "ZK2")
    );
    BlueprintV2Impl bp = createBlueprint(components);

    for (Service service : serviceGroup.getServices()) {
      Set<ComponentV2> expectedComponents = components.stream()
        .filter(c -> c.getService().equals(service))
        .collect(toSet());
      assertEquals(expectedComponents, Sets.newHashSet(bp.getComponents(service)));
    }
  }

  private BlueprintV2Impl createBlueprint(Set<ComponentV2> components) {
    Set<Service> services = components.stream()
      .map(ComponentV2::getService)
      .collect(toSet());
    serviceGroup.setServices(services);
    hostGroup.setComponents(components);

    BlueprintV2Impl bp = new BlueprintV2Impl();
    bp.setHostGroups(Collections.singleton(hostGroup));
    bp.setServiceGroups(Collections.singleton(serviceGroup));
    bp.postDeserialization();
    return bp;
  }

  private ComponentV2 createZookeeperServer(ServiceGroup serviceGroup, String serviceName) {
    Service service = new Service();
    service.setName(serviceName);
    service.setServiceGroup(serviceGroup);
    service.setType("ZOOKEEPER");
    ComponentV2 component = new ComponentV2();
    component.setType("ZOOKEEPER_SERVER");
    component.setService(service);
    component.setServiceGroup(serviceGroup.getName());
    component.setServiceName(serviceName);
    return component;
  }
}
