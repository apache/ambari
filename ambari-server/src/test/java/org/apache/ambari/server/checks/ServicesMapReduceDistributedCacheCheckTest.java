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
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Unit tests for ServicesMapReduceDistributedCacheCheck
 *
 */
public class ServicesMapReduceDistributedCacheCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  private final ServicesMapReduceDistributedCacheCheck servicesMapReduceDistributedCacheCheck = new ServicesMapReduceDistributedCacheCheck();

  @Before
  public void setup() {
    servicesMapReduceDistributedCacheCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    Configuration config = Mockito.mock(Configuration.class);
    Mockito.when(config.getRollingUpgradeMinStack()).thenReturn("HDP-2.2");
    Mockito.when(config.getRollingUpgradeMaxStack()).thenReturn("");
    servicesMapReduceDistributedCacheCheck.config = config;
  }

  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Map<String, Service> services = new HashMap<>();
    final Service service = Mockito.mock(Service.class);

    services.put("YARN", service);

    Mockito.when(cluster.getServices()).thenReturn(services);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);

    Assert.assertTrue(servicesMapReduceDistributedCacheCheck.isApplicable(new PrereqCheckRequest("cluster")));

    PrereqCheckRequest req = new PrereqCheckRequest("cluster");
    req.addResult(CheckDescription.SERVICES_NAMENODE_HA, PrereqCheckStatus.FAIL);
    Assert.assertFalse(servicesMapReduceDistributedCacheCheck.isApplicable(req));

    req.addResult(CheckDescription.SERVICES_NAMENODE_HA, PrereqCheckStatus.PASS);
    Assert.assertTrue(servicesMapReduceDistributedCacheCheck.isApplicable(req));


    services.remove("YARN");
    Assert.assertFalse(servicesMapReduceDistributedCacheCheck.isApplicable(new PrereqCheckRequest("cluster")));
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final DesiredConfig desiredConfig = Mockito.mock(DesiredConfig.class);
    Mockito.when(desiredConfig.getTag()).thenReturn("tag");
    Map<String, DesiredConfig> configMap = new HashMap<String, DesiredConfig>();
    configMap.put("mapred-site", desiredConfig);
    configMap.put("core-site", desiredConfig);
    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<String, String>();
    Mockito.when(config.getProperties()).thenReturn(properties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "hdfs://some/path");
    properties.put("mapreduce.application.classpath", "anything");
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "dfs://some/path");
    properties.put("mapreduce.application.classpath", "anything");
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "hdfs://ha");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "dfs://ha");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Fail due to no dfs
    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }

  @Test
  public void testPerformWithCheckConfig() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final DesiredConfig desiredConfig = Mockito.mock(DesiredConfig.class);
    Mockito.when(desiredConfig.getTag()).thenReturn("tag");
    Map<String, DesiredConfig> configMap = new HashMap<String, DesiredConfig>();
    configMap.put("mapred-site", desiredConfig);
    configMap.put("core-site", desiredConfig);
    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<String, String>();
    Mockito.when(config.getProperties()).thenReturn(properties);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put("dfs-protocols-regex","^([^:]*dfs|wasb|ecs):.*");
    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        servicesMapReduceDistributedCacheCheck.getClass().getName())).thenReturn(checkProperties);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "hdfs://some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "dfs://some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "wasb://some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "ecs://some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "hdfs://ha");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "dfs://ha");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "wasb://ha");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    properties.put("fs.defaultFS", "ecs://ha");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Fail due to no dfs
    properties.put("fs.defaultFS", "anything");
    properties.put("mapreduce.application.framework.path", "/some/path");
    properties.put("mapreduce.application.classpath", "anything");
    request = new PrereqCheckRequest("cluster");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    check = new PrerequisiteCheck(null, null);
    servicesMapReduceDistributedCacheCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}
