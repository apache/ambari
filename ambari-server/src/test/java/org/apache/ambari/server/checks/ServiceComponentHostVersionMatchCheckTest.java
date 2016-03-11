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
package org.apache.ambari.server.checks;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceComponentHostVersionMatchCheckTest {
  private static final String CLUSTER_NAME = "cluster1";
  private static final String FIRST_SERVICE_NAME = "service1";
  private static final String FIRST_SERVICE_COMPONENT_NAME = "component1";
  private static final String FIRST_SERVICE_COMPONENT_HOST_NAME = "host1";
  private static final String SECOND_SERVICE_COMPONENT_HOST_NAME = "host2";
  private static final String CURRENT_VERSION = "1.1.1.1";
  private static final String OTHER_VERSION = "1.2.3.4";
  private ServiceComponentHostVersionMatchCheck versionMismatchCheck;
  private Cluster cluster;
  private ServiceComponentHost firstServiceComponentHost;
  private ServiceComponentHost secondServiceComponentHost;

  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);
    versionMismatchCheck = new ServiceComponentHostVersionMatchCheck();
    versionMismatchCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    cluster = mock(Cluster.class, RETURNS_DEEP_STUBS);
    when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);

    Service firstService = mock(Service.class);
    Map<String, Service> services = ImmutableMap.of(FIRST_SERVICE_NAME, firstService);
    when(cluster.getServices()).thenReturn(services);

    ServiceComponent firstServiceComponent = mock(ServiceComponent.class);
    Map<String, ServiceComponent> components = ImmutableMap.of(FIRST_SERVICE_COMPONENT_NAME, firstServiceComponent);
    when(firstServiceComponent.isVersionAdvertised()).thenReturn(true);
    when(firstService.getServiceComponents()).thenReturn(components);

    firstServiceComponentHost = mock(ServiceComponentHost.class);
    secondServiceComponentHost = mock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> firstServiceComponentHosts = ImmutableMap.of(
        FIRST_SERVICE_COMPONENT_HOST_NAME, firstServiceComponentHost,
        SECOND_SERVICE_COMPONENT_HOST_NAME, secondServiceComponentHost
    );
    when(firstServiceComponent.getServiceComponentHosts()).thenReturn(firstServiceComponentHosts);
  }

  @Test
  public void testWarningWhenHostWithVersionOtherThanCurrentClusterVersionExists() throws Exception {
    when(cluster.getCurrentClusterVersion().getRepositoryVersion().getVersion()).thenReturn(CURRENT_VERSION);
    when(firstServiceComponentHost.getVersion()).thenReturn(CURRENT_VERSION);
    when(secondServiceComponentHost.getVersion()).thenReturn(OTHER_VERSION);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    versionMismatchCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
  }

  @Test
  public void testWarningWhenHostWithVersionOtherThanCurrentClusterVersionDoesNotExist() throws Exception {
    when(cluster.getCurrentClusterVersion().getRepositoryVersion().getVersion()).thenReturn(CURRENT_VERSION);
    when(firstServiceComponentHost.getVersion()).thenReturn(CURRENT_VERSION);
    when(secondServiceComponentHost.getVersion()).thenReturn(CURRENT_VERSION);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    versionMismatchCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testWarningWhenHostsWithDifferentVersionsExistAndCurrentClusterVersionIsUnknown() throws Exception {
    when(cluster.getCurrentClusterVersion().getRepositoryVersion()).thenReturn(null);
    when(firstServiceComponentHost.getVersion()).thenReturn(CURRENT_VERSION);
    when(secondServiceComponentHost.getVersion()).thenReturn(OTHER_VERSION);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    versionMismatchCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
  }

  @Test
  public void testWarningWhenAllHostsHaveSameVersionAndCurrentClusterVersionIsUnknown() throws Exception {
    when(cluster.getCurrentClusterVersion().getRepositoryVersion()).thenReturn(null);
    when(firstServiceComponentHost.getVersion()).thenReturn(CURRENT_VERSION);
    when(secondServiceComponentHost.getVersion()).thenReturn(CURRENT_VERSION);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    versionMismatchCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}