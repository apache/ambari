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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

import junit.framework.Assert;


/**
 * Tests the {@link CheckHelper} class
 * Makes sure that people don't forget to add new checks to registry.
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckHelperTest {

  private final Clusters clusters = Mockito.mock(Clusters.class);

  private MockCheck m_mockCheck;

  private CheckDescription m_mockCheckDescription = Mockito.mock(CheckDescription.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  @Mock
  private Object m_mockPerform;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_mockCheck = new MockCheck();

    Mockito.when(m_mockPerform.toString()).thenReturn("Perform!");

    m_services.clear();
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  /**
   * Sunny case when applicable.
   */
  @Test
  public void testPreUpgradeCheck() throws Exception {
    final CheckHelper helper = new CheckHelper();
    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.PASS, request.getResult(m_mockCheckDescription));
  }

  /**
   * Checks can be ignored, even if they are expected to fail.
   */
  @Test
  public void testPreUpgradeCheckNotApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Service service = Mockito.mock(Service.class);

    m_services.put("KAFKA", service);

    Mockito.when(cluster.getServices()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final CheckHelper helper = new CheckHelper();
    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");

    helper.performChecks(request, updateChecksRegistry, configuration);


    Assert.assertEquals(null, request.getResult(m_mockCheckDescription));
  }

  /**
   * Check that throwing an exception still fails.
   */
  @Test
  public void testPreUpgradeCheckThrowsException() throws Exception {
    final CheckHelper helper = new CheckHelper();
    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.FAIL, request.getResult(m_mockCheckDescription));
  }

  /**
   * Test that applicable tests that fail when configured to bypass failures results in a status of {@see PrereqCheckStatus.BYPASS}
   */
  @Test
  public void testPreUpgradeCheckBypassesFailure() throws Exception {
    final CheckHelper helper = new CheckHelper();
    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(true);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception, triggering the bypass
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.BYPASS, request.getResult(m_mockCheckDescription));
  }

  @Test
  public void testPreUpgradeCheckClusterMissing() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Service service = Mockito.mock(Service.class);

    m_services.put("KAFKA", service);

    Mockito.when(cluster.getServices()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getClusterId()).thenReturn(1L);

    Mockito.when(clusters.getCluster(Mockito.anyString())).thenReturn(cluster);

    final CheckHelper helper = new CheckHelper();
    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception, triggering the fail
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.FAIL, request.getResult(m_mockCheckDescription));
  }

  class MockCheck extends AbstractCheckDescriptor {

    protected MockCheck() {
      super(m_mockCheckDescription);

      clustersProvider = new Provider<Clusters>() {

        @Override
        public Clusters get() {
          return clusters;
        }
      };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getApplicableServices() {
      return m_services.keySet();
    }

    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
        throws AmbariException {
      m_mockPerform.toString();
    }
  }
}