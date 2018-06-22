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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * Tests for {@link AutoStartDisabledCheck}
 */
@Ignore
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
public class AutoStartDisabledCheckTest {

  private final AutoStartDisabledCheck m_check = new AutoStartDisabledCheck();
  private final Clusters m_clusters = EasyMock.createMock(Clusters.class);
  private Map<String, String> m_configMap = new HashMap<>();

  @Before
  public void before() throws Exception {

    m_check.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    Cluster cluster = EasyMock.createMock(Cluster.class);

    Map<String, DesiredConfig> map = new HashMap<>();
    map.put(AutoStartDisabledCheck.CLUSTER_ENV_TYPE, new DesiredConfig());

    expect(cluster.getDesiredConfigs()).andReturn(map).anyTimes();

    Config config = EasyMock.createMock(Config.class);
    expect(config.getProperties()).andReturn(m_configMap).anyTimes();

    expect(cluster.getConfig(EasyMock.eq(AutoStartDisabledCheck.CLUSTER_ENV_TYPE), EasyMock.anyString()))
      .andReturn(config).anyTimes();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    m_configMap.clear();
  }

  @Test
  public void testNoAutoStart() throws Exception {
    UpgradePlanEntity upgradePlan = EasyMock.createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    Assert.assertTrue(m_check.isApplicable(request));

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
    Assert.assertTrue(StringUtils.isBlank(result.getFailReason()));
  }

  @Test
  public void testAutoStartFalse() throws Exception {
    UpgradePlanEntity upgradePlan = EasyMock.createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    Assert.assertTrue(m_check.isApplicable(request));

    m_configMap.put(AutoStartDisabledCheck.RECOVERY_ENABLED_KEY, "false");

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
    Assert.assertTrue(StringUtils.isBlank(result.getFailReason()));
  }

  @Test
  public void testAutoStartTrue() throws Exception {
    UpgradePlanEntity upgradePlan = EasyMock.createNiceMock(UpgradePlanEntity.class);
    expect(upgradePlan.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    PrereqCheckRequest request = new PrereqCheckRequest(upgradePlan);

    Assert.assertTrue(m_check.isApplicable(request));

    m_configMap.put(AutoStartDisabledCheck.RECOVERY_ENABLED_KEY, "true");
    m_configMap.put(AutoStartDisabledCheck.RECOVERY_TYPE_KEY, AutoStartDisabledCheck.RECOVERY_AUTO_START);

    UpgradeCheckResult result = m_check.perform(request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());
    Assert.assertTrue(StringUtils.isNotBlank(result.getFailReason()));
    Assert.assertEquals("Auto Start must be disabled before performing an Upgrade. To disable Auto Start, navigate to " +
          "Admin > Service Auto Start. Turn the toggle switch off to Disabled and hit Save.", result.getFailReason());

  }

}
