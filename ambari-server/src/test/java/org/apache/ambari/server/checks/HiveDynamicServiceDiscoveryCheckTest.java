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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Tests for {@link HiveDynamicServiceDiscoveryCheck}
 */
public class HiveDynamicServiceDiscoveryCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);

  private final HiveDynamicServiceDiscoveryCheck m_check = new HiveDynamicServiceDiscoveryCheck();

  /**
   *
   */
  @Before
  public void setup() {
    m_check.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };
    Configuration config = Mockito.mock(Configuration.class);
    Mockito.when(config.getRollingUpgradeMinStack()).thenReturn("HDP-2.2");
    Mockito.when(config.getRollingUpgradeMaxStack()).thenReturn("");
    m_check.config = config;
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    final DesiredConfig desiredConfig = Mockito.mock(DesiredConfig.class);
    Mockito.when(desiredConfig.getTag()).thenReturn("tag");
    Map<String, DesiredConfig> configMap = new HashMap<String, DesiredConfig>();
    configMap.put("hive-site", desiredConfig);

    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<String, String>();
    Mockito.when(config.getProperties()).thenReturn(properties);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put("min-failure-stack-version","HDP-2.3.0.0");
    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    // Check HDP-2.2.x => HDP-2.2.y
    request.setSourceStackId(new StackId("HDP-2.2.4.2"));
    request.setTargetStackId(new StackId("HDP-2.2.8.4"));
    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());

    // Check HDP-2.2.x => HDP-2.3.y
    request.setSourceStackId(new StackId("HDP-2.2.4.2"));
    request.setTargetStackId(new StackId("HDP-2.3.8.4"));
    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Check HDP-2.3.x => HDP-2.3.y
    request.setSourceStackId(new StackId("HDP-2.3.4.2"));
    request.setTargetStackId(new StackId("HDP-2.3.8.4"));
    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Check HDP-2.3.x => HDP-2.4.y
    request.setSourceStackId(new StackId("HDP-2.3.4.2"));
    request.setTargetStackId(new StackId("HDP-2.4.8.4"));
    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Check when properties are specified
    properties.put("hive.server2.support.dynamic.service.discovery", "true");
    properties.put("hive.zookeeper.quorum", "host");
    properties.put("hive.server2.zookeeper.namespace", "namespace");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

}
