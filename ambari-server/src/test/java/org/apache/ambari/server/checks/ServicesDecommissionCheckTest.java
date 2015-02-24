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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Unit tests for ServicesDecommissionCheck
 *
 */
public class ServicesDecommissionCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  @Test
  public void testIsApplicable() throws Exception {
    Assert.assertTrue(new ServicesDecommissionCheck().isApplicable(null));
  }

  @Test
  public void testPerform() throws Exception {
    final ServicesDecommissionCheck servicesDecommissionCheck = new ServicesDecommissionCheck();
    servicesDecommissionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    servicesDecommissionCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return Mockito.mock(AmbariMetaInfo.class);
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Service service = Mockito.mock(Service.class);
    Mockito.when(cluster.getServices()).thenReturn(Collections.singletonMap("service", service));
    final ServiceComponent serviceComponent1 = Mockito.mock(ServiceComponent.class);
    final ServiceComponent serviceComponent2 = Mockito.mock(ServiceComponent.class);
    final Map<String, ServiceComponent> serviceComponents = new HashMap<String, ServiceComponent>();
    serviceComponents.put("component1", serviceComponent1);
    serviceComponents.put("component2", serviceComponent2);
    Mockito.when(service.getServiceComponents()).thenReturn(serviceComponents);
    final ServiceComponentHost serviceComponentHost1 = Mockito.mock(ServiceComponentHost.class);
    final ServiceComponentHost serviceComponentHost2 = Mockito.mock(ServiceComponentHost.class);
    Mockito.when(serviceComponent1.getServiceComponentHosts()).thenReturn(Collections.singletonMap("serviceComponentHost1", serviceComponentHost1));
    Mockito.when(serviceComponent2.getServiceComponentHosts()).thenReturn(Collections.singletonMap("serviceComponentHost2", serviceComponentHost2));
    Mockito.when(serviceComponent1.getServiceComponentHost(Mockito.anyString())).thenReturn(serviceComponentHost1);
    Mockito.when(serviceComponent2.getServiceComponentHost(Mockito.anyString())).thenReturn(serviceComponentHost2);
    Mockito.when(serviceComponentHost1.getComponentAdminState()).thenReturn(HostComponentAdminState.DECOMMISSIONED);
    Mockito.when(serviceComponentHost2.getComponentAdminState()).thenReturn(HostComponentAdminState.INSERVICE);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    servicesDecommissionCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    Mockito.when(serviceComponentHost1.getComponentAdminState()).thenReturn(HostComponentAdminState.INSERVICE);
    check = new PrerequisiteCheck(null, null);
    servicesDecommissionCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}
