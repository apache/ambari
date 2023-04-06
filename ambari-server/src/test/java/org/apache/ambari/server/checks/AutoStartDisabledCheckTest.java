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
import static org.easymock.EasyMock.replay;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * Tests for {@link AutoStartDisabledCheck}
 */
public class AutoStartDisabledCheckTest {

  private final AutoStartDisabledCheck m_check = new AutoStartDisabledCheck();
  private final Clusters m_clusters = EasyMock.createMock(Clusters.class);
  private Map<String, String> m_configMap = new HashMap<>();

  RepositoryVersionEntity repositoryVersionEntity;

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

    repositoryVersionEntity = EasyMock.createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersionEntity.getType()).andReturn(RepositoryType.STANDARD).anyTimes();

    replay(m_clusters, cluster, config, repositoryVersionEntity);

    m_configMap.clear();
  }

  @Test
  public void testNoAutoStart() throws Exception {
    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.AUTO_START_DISABLED, "foo");
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(repositoryVersionEntity);

    Assert.assertTrue(m_check.isApplicable(request));

    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(StringUtils.isBlank(check.getFailReason()));
  }

  @Test
  public void testAutoStartFalse() throws Exception {
    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.AUTO_START_DISABLED, "foo");
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(repositoryVersionEntity);

    Assert.assertTrue(m_check.isApplicable(request));

    m_configMap.put(AutoStartDisabledCheck.RECOVERY_ENABLED_KEY, "false");

    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(StringUtils.isBlank(check.getFailReason()));
  }

  @Test
  public void testAutoStartTrue() throws Exception {
    PrerequisiteCheck check = new PrerequisiteCheck(CheckDescription.AUTO_START_DISABLED, "foo");
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(repositoryVersionEntity);

    Assert.assertTrue(m_check.isApplicable(request));

    m_configMap.put(AutoStartDisabledCheck.RECOVERY_ENABLED_KEY, "true");
    m_configMap.put(AutoStartDisabledCheck.RECOVERY_TYPE_KEY, AutoStartDisabledCheck.RECOVERY_AUTO_START);

    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    Assert.assertTrue(StringUtils.isNotBlank(check.getFailReason()));
    Assert.assertEquals("Auto Start must be disabled before performing an Upgrade. To disable Auto Start, navigate to " +
          "Admin > Service Auto Start. Turn the toggle switch off to Disabled and hit Save.", check.getFailReason());

  }

}
