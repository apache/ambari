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

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.checks.ClusterCheck;
import org.apache.ambari.server.checks.PreUpgradeCheck;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
@RunWith(MockitoJUnitRunner.class)
public class CheckHelperTest extends EasyMockSupport {

  private final Clusters clusters = Mockito.mock(Clusters.class);

  private MockCheck m_mockCheck;

  private CheckDescription m_mockCheckDescription = Mockito.mock(CheckDescription.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private Object m_mockPerform;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_mockCheck = new MockCheck();

    Mockito.when(m_mockPerform.toString()).thenReturn("Perform!");

    m_services.clear();
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  /**
   * Sunny case when applicable.
   */
  @Test
  public void testPreUpgradeCheck() throws Exception {
    final CheckHelper helper = new CheckHelper();
    Configuration configuration = createNiceMock(Configuration.class);
    List<PreUpgradeCheck> updateChecksRegistry = new ArrayList<>();

    expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    updateChecksRegistry.add(m_mockCheck);

    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);

    replayAll();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

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

    Mockito.when(cluster.getServicesByName()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final CheckHelper helper = new CheckHelper();
    Configuration configuration = createNiceMock(Configuration.class);
    List<PreUpgradeCheck> updateChecksRegistry = new ArrayList<>();

    expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    updateChecksRegistry.add(m_mockCheck);

    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);

    replayAll();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    helper.performChecks(request, updateChecksRegistry, configuration);


    Assert.assertEquals(null, request.getResult(m_mockCheckDescription));
  }

  /**
   * Check that throwing an exception still fails.
   */
  @Test
  public void testPreUpgradeCheckThrowsException() throws Exception {
    final CheckHelper helper = new CheckHelper();
    Configuration configuration = createNiceMock(Configuration.class);
    List<PreUpgradeCheck> updateChecksRegistry = new ArrayList<>();

    expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);

    replayAll();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.FAIL, request.getResult(m_mockCheckDescription));
  }

  /**
   * Test that applicable tests that fail when configured to bypass failures results in a status of {@see PrereqCheckStatus.BYPASS}
   */
  @Test
  public void testPreUpgradeCheckBypassesFailure() throws Exception {
    final CheckHelper helper = new CheckHelper();
    Configuration configuration = createNiceMock(Configuration.class);
    List<PreUpgradeCheck> updateChecksRegistry = new ArrayList<>();

    expect(configuration.isUpgradePrecheckBypass()).andReturn(true);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception, triggering the bypass
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);

    replayAll();

    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);
    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.BYPASS, request.getResult(m_mockCheckDescription));
  }

  @Test
  public void testPreUpgradeCheckClusterMissing() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Service service = Mockito.mock(Service.class);

    m_services.put("KAFKA", service);

    Mockito.when(cluster.getServicesByName()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getClusterId()).thenReturn(1L);

    Mockito.when(clusters.getCluster(Mockito.anyString())).thenReturn(cluster);

    final CheckHelper helper = new CheckHelper();
    Configuration configuration = createNiceMock(Configuration.class);
    List<PreUpgradeCheck> updateChecksRegistry = new ArrayList<>();

    expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception, triggering the fail
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);

    replayAll();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(PrereqCheckStatus.FAIL, request.getResult(m_mockCheckDescription));
  }

  class MockCheck extends ClusterCheck {

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
    public UpgradeCheckResult perform(PrereqCheckRequest request)
        throws AmbariException {
      m_mockPerform.toString();
      return new UpgradeCheckResult(this);
    }
  }
}