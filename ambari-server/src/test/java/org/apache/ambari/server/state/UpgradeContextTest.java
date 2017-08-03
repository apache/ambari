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
package org.apache.ambari.server.state;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.hadoop.metrics2.sink.relocated.google.common.collect.Lists;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link UpgradeContext}.
 */
public class UpgradeContextTest extends EasyMockSupport {

  @Mock
  private UpgradeEntity m_completedRevertableUpgrade;

  @Mock
  private RepositoryVersionEntity m_completedUpgradeTargetRepositoryVersion;

  @Mock
  private RepositoryVersionEntity m_completedUpgradeSourceRepositoryVersion;


  @Mock
  private UpgradeDAO m_upgradeDAO;

  @Before
  public void setup() {
    injectMocks(this);

    expect(m_completedUpgradeSourceRepositoryVersion.getId()).andReturn(1L).anyTimes();
    expect(m_completedUpgradeSourceRepositoryVersion.getStackId()).andReturn(new StackId("HDP", "2.6")).anyTimes();
    expect(m_completedUpgradeTargetRepositoryVersion.getId()).andReturn(1L).anyTimes();
    expect(m_completedUpgradeTargetRepositoryVersion.getStackId()).andReturn(new StackId("HDP", "2.6")).anyTimes();

    UpgradeHistoryEntity upgradeHistoryEntity = createNiceMock(UpgradeHistoryEntity.class);
    expect(upgradeHistoryEntity.getServiceName()).andReturn("HDFS").atLeastOnce();
    expect(upgradeHistoryEntity.getFromReposistoryVersion()).andReturn(m_completedUpgradeSourceRepositoryVersion).anyTimes();
    expect(upgradeHistoryEntity.getTargetRepositoryVersion()).andReturn(m_completedUpgradeTargetRepositoryVersion).anyTimes();
    List<UpgradeHistoryEntity> upgradeHistory = Lists.newArrayList(upgradeHistoryEntity);

    expect(m_upgradeDAO.findUpgrade(1L)).andReturn(m_completedRevertableUpgrade).anyTimes();

    expect(
        m_upgradeDAO.findLastUpgradeForCluster(EasyMock.anyLong(),
            eq(Direction.UPGRADE))).andReturn(m_completedRevertableUpgrade).anyTimes();

    expect(m_completedRevertableUpgrade.getDirection()).andReturn(Direction.UPGRADE).anyTimes();
    expect(m_completedRevertableUpgrade.getRepositoryVersion()).andReturn(m_completedUpgradeTargetRepositoryVersion).anyTimes();
    expect(m_completedRevertableUpgrade.getOrchestration()).andReturn(RepositoryType.PATCH).anyTimes();
    expect(m_completedRevertableUpgrade.getHistory()).andReturn(upgradeHistory).anyTimes();
    expect(m_completedRevertableUpgrade.getUpgradePackage()).andReturn(null).anyTimes();
  }

  /**
   * Tests that the {@link UpgradeContext} for a reversion has the correct
   * parameters set.
   *
   * @throws Exception
   */
  @Test
  public void testRevert() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    UpgradeHelper upgradeHelper = createNiceMock(UpgradeHelper.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    RepositoryVersionEntity hdfsRepositoryVersion = createNiceMock(RepositoryVersionEntity.class);

    Service service = createNiceMock(Service.class);
    UpgradePack upgradePack = createNiceMock(UpgradePack.class);

    expect(upgradeHelper.suggestUpgradePack(EasyMock.anyString(), EasyMock.anyObject(StackId.class),
        EasyMock.anyObject(StackId.class), EasyMock.anyObject(Direction.class),
        EasyMock.anyObject(UpgradeType.class), EasyMock.anyString())).andReturn(upgradePack).once();

    expect(service.getDesiredRepositoryVersion()).andReturn(hdfsRepositoryVersion).once();
    expect(cluster.getService("HDFS")).andReturn(service).atLeastOnce();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.ROLLING.name());
    requestMap.put(UpgradeResourceProvider.UPGRADE_REVERT_UPGRADE_ID, "1");

    replayAll();

    UpgradeContext context = new UpgradeContext(cluster, requestMap, null, upgradeHelper,
        m_upgradeDAO, repositoryVersionDAO, configHelper);

    assertEquals(Direction.DOWNGRADE, context.getDirection());
    assertEquals(RepositoryType.PATCH, context.getOrchestrationType());
    assertEquals(1, context.getSupportedServices().size());
    assertTrue(context.isPatchRevert());

    verifyAll();
  }

  /**
   * Tests that the {@link UpgradeContext} for a patch downgrade has the
   * correcting scope/orchestration set.
   *
   * @throws Exception
   */
  @Test
  public void testDowngradeScope() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    UpgradeHelper upgradeHelper = createNiceMock(UpgradeHelper.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    RepositoryVersionEntity hdfsRepositoryVersion = createNiceMock(RepositoryVersionEntity.class);
    Service service = createNiceMock(Service.class);
    UpgradePack upgradePack = createNiceMock(UpgradePack.class);

    expect(upgradeHelper.suggestUpgradePack(EasyMock.anyString(), EasyMock.anyObject(StackId.class),
        EasyMock.anyObject(StackId.class), EasyMock.anyObject(Direction.class),
        EasyMock.anyObject(UpgradeType.class), EasyMock.anyString())).andReturn(upgradePack).once();

    expect(service.getDesiredRepositoryVersion()).andReturn(hdfsRepositoryVersion).once();
    expect(cluster.getService("HDFS")).andReturn(service).atLeastOnce();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.NON_ROLLING.name());
    requestMap.put(UpgradeResourceProvider.UPGRADE_DIRECTION, Direction.DOWNGRADE.name());

    replayAll();

    UpgradeContext context = new UpgradeContext(cluster, requestMap, null, upgradeHelper,
        m_upgradeDAO, repositoryVersionDAO, configHelper);

    assertEquals(Direction.DOWNGRADE, context.getDirection());
    assertEquals(RepositoryType.PATCH, context.getOrchestrationType());
    assertEquals(1, context.getSupportedServices().size());
    assertFalse(context.isPatchRevert());

    verifyAll();
  }

}
