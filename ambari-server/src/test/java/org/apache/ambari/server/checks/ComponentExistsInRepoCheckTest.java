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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * Tests for {@link ComponentsExistInRepoCheck}
 */
@Ignore
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
public class ComponentExistsInRepoCheckTest extends EasyMockSupport {

  private final ComponentsExistInRepoCheck m_check = new ComponentsExistInRepoCheck();

  @Mock
  private Clusters m_clusters;

  @Mock
  private Cluster m_cluster;

  // pick two stacks which have different services
  private final StackId SOURCE_STACK = new StackId("HDP", "0.1");
  private final StackId TARGET_STACK = new StackId("HDP", "2.2.0");

  private final Map<String, Service> CLUSTER_SERVICES = new HashMap<>();
  private final Map<String, ServiceComponent> FOO_SERVICE_COMPONENTS = new HashMap<>();
  private final Map<String, ServiceComponent> ZK_SERVICE_COMPONENTS = new HashMap<>();

  @Mock
  private AmbariMetaInfo m_ambariMetaInfo;

  @Mock
  private Service m_fooService;

  @Mock
  private Service m_zookeeperService;

  @Mock
  private ServiceInfo m_fooInfo;

  @Mock
  private ServiceInfo m_zookeeperInfo;

  @Mock
  private ComponentInfo m_fooComponentInfo;

  @Mock
  private ComponentInfo m_zookeeperServerInfo;

  @Mock
  private ServiceComponent m_fooComponent;

  @Mock
  private ServiceComponent m_zookeeperServer;

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Before
  public void before() throws Exception {

    EasyMockSupport.injectMocks(this);

    m_check.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    m_check.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return m_ambariMetaInfo;
      }
    };

    expect(m_cluster.getServicesByName()).andReturn(CLUSTER_SERVICES).atLeastOnce();
    expect(m_cluster.getService("ZOOKEEPER")).andReturn(m_zookeeperService).anyTimes();
    expect(m_cluster.getService("FOO_SERVICE")).andReturn(m_fooService).anyTimes();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(m_cluster).anyTimes();

    ZK_SERVICE_COMPONENTS.put("ZOOKEEPER_SERVER", m_zookeeperServer);
    FOO_SERVICE_COMPONENTS.put("FOO_COMPONENT", m_fooComponent);

    expect(m_zookeeperService.getServiceType()).andReturn("ZOOKEEPER").anyTimes();
    expect(m_fooService.getServiceType()).andReturn("FOO_SERVICE").anyTimes();
    expect(m_zookeeperService.getServiceComponents()).andReturn(ZK_SERVICE_COMPONENTS).anyTimes();
    expect(m_fooService.getServiceComponents()).andReturn(FOO_SERVICE_COMPONENTS).anyTimes();

    expect(m_zookeeperInfo.getComponentByName("ZOOKEEPER_SERVER")).andReturn(
        m_zookeeperServerInfo).anyTimes();

    expect(m_fooInfo.getComponentByName("FOO_COMPONENT")).andReturn(m_fooComponentInfo).anyTimes();

    expect(m_ambariMetaInfo.getService(TARGET_STACK.getStackName(), TARGET_STACK.getStackVersion(),
        "ZOOKEEPER")).andReturn(m_zookeeperInfo).anyTimes();

    expect(m_ambariMetaInfo.getComponent(TARGET_STACK.getStackName(),
        TARGET_STACK.getStackVersion(), "ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(
            m_zookeeperServerInfo).anyTimes();

    expect(m_clusterVersionSummary.getAvailableServiceNames()).andReturn(CLUSTER_SERVICES.keySet()).anyTimes();

  }

  /**
   * Tests that the check passes when services and components exist.
   *
   * @throws Exception
   */
  @Test
  public void testCheckPassesWhenServicAndComponentsExist() throws Exception {
    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    CLUSTER_SERVICES.put("ZOOKEEPER", m_zookeeperService);
    expect(m_zookeeperInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_zookeeperInfo.isDeleted()).andReturn(false).atLeastOnce();
    expect(m_zookeeperServerInfo.isVersionAdvertised()).andReturn(true).atLeastOnce();
    expect(m_zookeeperServerInfo.isDeleted()).andReturn(false).atLeastOnce();

    replayAll();

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
    Assert.assertTrue(result.getFailedDetail().isEmpty());
    Assert.assertTrue(StringUtils.isBlank(result.getFailReason()));
  }

  /**
   * Tests that the check passes when a service doesn't exist but isn't
   * advertising its version.
   *
   * @throws Exception
   */
  @Test
  public void testCheckPassesWhenComponentNotAdvertisingVersion() throws Exception {
    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    CLUSTER_SERVICES.put("FOO_SERVICE", m_fooService);

    expect(m_ambariMetaInfo.getService(TARGET_STACK.getStackName(), TARGET_STACK.getStackVersion(),
        "FOO_SERVICE")).andReturn(m_fooInfo).anyTimes();

    expect(m_ambariMetaInfo.getComponent(TARGET_STACK.getStackName(),
        TARGET_STACK.getStackVersion(), "FOO_SERVICE", "FOO_COMPONENT")).andReturn(
            m_fooComponentInfo).atLeastOnce();

    expect(m_fooInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_fooInfo.isDeleted()).andReturn(false).atLeastOnce();
    expect(m_fooComponentInfo.isVersionAdvertised()).andReturn(false).atLeastOnce();
    expect(m_fooComponentInfo.isDeleted()).andReturn(true).atLeastOnce();

    replayAll();

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
    Assert.assertTrue(result.getFailedDetail().isEmpty());
    Assert.assertTrue(StringUtils.isBlank(result.getFailReason()));
  }

  /**
   * Tests that the check fails when the service exists but was deleted.
   *
   * @throws Exception
   */
  @Test
  public void testCheckFailsWhenServiceExistsButIsDeleted() throws Exception {
    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    CLUSTER_SERVICES.put("ZOOKEEPER", m_zookeeperService);
    expect(m_zookeeperInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_zookeeperInfo.isDeleted()).andReturn(true).atLeastOnce();

    replayAll();

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
    Assert.assertEquals(1, result.getFailedDetail().size());
    Assert.assertTrue(result.getFailedOn().contains("ZOOKEEPER"));
  }

  /**
   * Tests that the check fails when the component exists but what deleted.
   *
   * @throws Exception
   */
  @Test
  public void testCheckFailsWhenComponentExistsButIsDeleted() throws Exception {
    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();

    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    CLUSTER_SERVICES.put("ZOOKEEPER", m_zookeeperService);
    expect(m_zookeeperInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_zookeeperInfo.isDeleted()).andReturn(false).atLeastOnce();
    expect(m_zookeeperServerInfo.isVersionAdvertised()).andReturn(true).atLeastOnce();
    expect(m_zookeeperServerInfo.isDeleted()).andReturn(true).atLeastOnce();

    replayAll();

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
    Assert.assertEquals(1, result.getFailedDetail().size());
    Assert.assertTrue(result.getFailedOn().contains("ZOOKEEPER_SERVER"));
  }

  /**
   * Tests that the check fails when the component exists but what deleted.
   *
   * @throws Exception
   */
  @Test
  public void testCheckFailsWhenServiceIsMissing() throws Exception {
    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();

    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    CLUSTER_SERVICES.put("ZOOKEEPER", m_zookeeperService);
    CLUSTER_SERVICES.put("FOO_SERVICE", m_fooService);

    expect(m_ambariMetaInfo.getService(TARGET_STACK.getStackName(), TARGET_STACK.getStackVersion(),
        "FOO_SERVICE")).andThrow(new StackAccessException(""));

    expect(m_zookeeperInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_zookeeperInfo.isDeleted()).andReturn(false).atLeastOnce();
    expect(m_zookeeperServerInfo.isVersionAdvertised()).andReturn(true).atLeastOnce();
    expect(m_zookeeperServerInfo.isDeleted()).andReturn(false).atLeastOnce();

    replayAll();

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
    Assert.assertEquals(1, result.getFailedDetail().size());
    Assert.assertTrue(result.getFailedOn().contains("FOO_SERVICE"));
  }

  /**
   * Tests that the check fails when the component exists but what deleted.
   *
   * @throws Exception
   */
  @Test
  public void testCheckFailsWhenComponentIsMissing() throws Exception {
    UpgradePlanEntity upgradePlan = createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();

    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    CLUSTER_SERVICES.put("FOO_SERVICE", m_fooService);

    expect(m_ambariMetaInfo.getService(TARGET_STACK.getStackName(), TARGET_STACK.getStackVersion(),
        "FOO_SERVICE")).andReturn(m_fooInfo).anyTimes();

    expect(m_ambariMetaInfo.getComponent(TARGET_STACK.getStackName(),
        TARGET_STACK.getStackVersion(), "FOO_SERVICE", "FOO_COMPONENT")).andThrow(
            new StackAccessException(""));

    expect(m_zookeeperInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_zookeeperInfo.isDeleted()).andReturn(false).atLeastOnce();
    expect(m_zookeeperServerInfo.isVersionAdvertised()).andReturn(true).atLeastOnce();
    expect(m_zookeeperServerInfo.isDeleted()).andReturn(false).atLeastOnce();

    expect(m_fooInfo.isValid()).andReturn(true).atLeastOnce();
    expect(m_fooInfo.isDeleted()).andReturn(false).atLeastOnce();

    replayAll();

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
    Assert.assertEquals(1, result.getFailedDetail().size());
    Assert.assertTrue(result.getFailedOn().contains("FOO_COMPONENT"));
  }

}
