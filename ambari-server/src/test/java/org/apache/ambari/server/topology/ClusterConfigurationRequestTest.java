/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * ClusterConfigurationRequest unit tests
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({AmbariContext.class})
public class ClusterConfigurationRequestTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private Blueprint blueprint;

  @Mock(type = MockType.NICE)
  private AmbariContext ambariContext;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private ClusterTopology topology;

  @Mock(type = MockType.NICE)
  private StackAdvisorBlueprintProcessor stackAdvisorBlueprintProcessor;

  @Mock(type = MockType.NICE)
  private Stack stack;

  @Mock(type = MockType.NICE)
  private AmbariManagementController controller;

  @Mock(type = MockType.NICE)
  private KerberosHelper kerberosHelper;

  @Test
  public void testProcessClusterConfigRequestIncludeKererosConfigs() throws Exception {

    Map<String, Map<String, String>> existingConfig = new HashMap<String, Map<String, String>>();
    Configuration stackConfig = new Configuration(existingConfig,
      new HashMap<String, Map<String, Map<String, String>>>());

    PowerMock.mockStatic(AmbariContext.class);
    AmbariContext.getController();
    expectLastCall().andReturn(controller).anyTimes();

    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.getKerberosHelper()).andReturn(kerberosHelper).times(2);

    expect(clusters.getCluster("testCluster")).andReturn(cluster).anyTimes();

    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getAllConfigurationTypes(anyString())).andReturn(Collections.<String>singletonList("testConfigType")
    ).anyTimes();
    expect(stack.getExcludedConfigurationTypes(anyString())).andReturn(Collections.<String>emptySet()).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(anyString(), anyString())).andReturn(Collections.<String,
      Stack.ConfigProperty>emptyMap()).anyTimes();

    Set<String> services = new HashSet<>();
    services.add("HDFS");
    services.add("KERBEROS");
    services.add("ZOOKEPER");
    expect(blueprint.getServices()).andReturn(services).anyTimes();
    expect(topology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getConfiguration()).andReturn(stackConfig).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(Collections.<String, HostGroupInfo>emptyMap());
    expect(topology.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    expect(ambariContext.getClusterName(Long.valueOf(1))).andReturn("testCluster").anyTimes();
    expect(ambariContext.createConfigurationRequests(anyObject(Map.class))).andReturn(Collections
      .<ConfigurationRequest>emptyList()).anyTimes();

    Map<String, Map<String, String>> kerberosConfig = new HashMap<String, Map<String, String>>();
    Map<String, String> properties = new HashMap<>();
    properties.put("testPorperty", "testValue");
    kerberosConfig.put("testConfigType", properties);
    expect(kerberosHelper.ensureHeadlessIdentities(anyObject(Cluster.class), anyObject(Map.class), anyObject
      (Set.class))).andReturn(true).once();
    expect(kerberosHelper.getServiceConfigurationUpdates(anyObject(Cluster.class), anyObject(Map.class), anyObject
      (Set.class))).andReturn(kerberosConfig).once();


    PowerMock.replay(stack, blueprint, topology, controller, clusters, kerberosHelper, ambariContext,
      AmbariContext
      .class);

    ClusterConfigurationRequest clusterConfigurationRequest = new ClusterConfigurationRequest(
      ambariContext, topology, false, stackAdvisorBlueprintProcessor, true);
    clusterConfigurationRequest.process();

    verify(blueprint, topology, ambariContext, controller, kerberosHelper);

  }

}
