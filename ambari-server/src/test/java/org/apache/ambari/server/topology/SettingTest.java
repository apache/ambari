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

package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Test the Setting class
 */
public class SettingTest {

  private static Setting setting;
  private static Map<String, Set<Map<String, String>>> properties;
  private static Set<Map<String, String>> serviceSettings;

  @BeforeClass
  public static void setup() {
    properties = new HashMap<>();
    Set<Map<String, String>> recoverySettings = new HashSet<>();
    Set<Map<String, String>> deploymentSettings = new HashSet<>();
    Set<Map<String, String>> repositorySettings = new HashSet<>();

    // Setting 1: Property1
    Map<String, String> setting1Properties1 = new HashMap<>();
    setting1Properties1.put(Setting.SETTING_NAME_RECOVERY_ENABLED, "true");
    recoverySettings.add(setting1Properties1);

    // Setting 2: Property1 and Property2
    Map<String, String> hdfs = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "HDFS",
      Setting.SETTING_NAME_RECOVERY_ENABLED, "false"
    );

    Map<String, String> yarn = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "YARN"
      // no RECOVERY_ENABLED value for YARN
    );

    Map<String, String> tez = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "TEZ",
      Setting.SETTING_NAME_RECOVERY_ENABLED, "true"
    );

    serviceSettings = ImmutableSet.of(hdfs, yarn, tez);

    Map<String, String> hdfsClient = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "HDFS_CLIENT",
      Setting.SETTING_NAME_RECOVERY_ENABLED, "false"
    );

    Map<String, String> namenode = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "NAMENODE",
      Setting.SETTING_NAME_RECOVERY_ENABLED, "true"
    );

    Map<String, String> datanode = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "DATANODE"
      // no RECOVERY_ENABLED value
    );

    Map<String, String> yarnClient = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "YARN_CLIENT",
      Setting.SETTING_NAME_RECOVERY_ENABLED, "false"
    );

    Map<String, String> resourceManager = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "RESOURCE_MANAGER",
      Setting.SETTING_NAME_RECOVERY_ENABLED, "true"
    );

    Map<String, String> nodeManager = ImmutableMap.of(
      Setting.SETTING_NAME_NAME, "NODE_MANAGER"
      // no RECOVERY_ENABLED value
    );

    Set<Map<String, String>> componentSettings = ImmutableSet.<Map<String, String>>builder()
      .addAll(ImmutableSet.of(hdfsClient, namenode, datanode))
      .addAll(ImmutableSet.of(yarnClient, resourceManager, nodeManager))
      .build();

    //Setting 3: Property 1
    Map<String, String> setting3Properties1 = new HashMap<>();
    setting3Properties1.put(Setting.SETTING_NAME_SKIP_FAILURE, "true");
    deploymentSettings.add(setting3Properties1);

    //Setting 4: Property 1 and 2
    Map<String, String> setting4Properties1 = new HashMap<>();
    setting4Properties1.put(RepositorySetting.OVERRIDE_STRATEGY, RepositorySetting.OVERRIDE_STRATEGY_ALWAYS_APPLY);
    setting4Properties1.put(RepositorySetting.OPERATING_SYSTEM, "redhat7");
    setting4Properties1.put(RepositorySetting.REPO_ID, "HDP");
    setting4Properties1.put(RepositorySetting.BASE_URL, "http://localhost/repo");
    repositorySettings.add(setting4Properties1);

    Map<String, String> setting4Properties2 = new HashMap<>();
    setting4Properties2.put(RepositorySetting.OVERRIDE_STRATEGY, RepositorySetting.OVERRIDE_STRATEGY_ALWAYS_APPLY);
    setting4Properties2.put(RepositorySetting.OPERATING_SYSTEM, "redhat7");
    setting4Properties2.put(RepositorySetting.REPO_ID, "HDP-UTIL");
    setting4Properties2.put(RepositorySetting.BASE_URL, "http://localhost/repo");
    repositorySettings.add(setting4Properties2);

    properties.put(Setting.SETTING_NAME_RECOVERY_SETTINGS, recoverySettings);
    properties.put(Setting.SETTING_NAME_SERVICE_SETTINGS, serviceSettings);
    properties.put(Setting.SETTING_NAME_COMPONENT_SETTINGS, componentSettings);
    properties.put(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS, deploymentSettings);
    properties.put(Setting.SETTING_NAME_REPOSITORY_SETTINGS, repositorySettings);

    setting = new Setting(properties);
  }

  /**
   * Test get and set of entire setting.
   */
  @Test
  public void testGetProperties() {
    assertEquals(properties, setting.getProperties());
  }

  /**
   * Validate the properties for a given setting.
   */
  @Test
  public void testGetSettingProperties() {
    assertEquals(serviceSettings, setting.getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS));
  }

  @Test
  public void recoveryEnabledAtComponentLevel() {
    assertEquals("false", setting.getRecoveryEnabled("HDFS", "HDFS_CLIENT"));
    assertEquals("true", setting.getRecoveryEnabled("HDFS", "NAMENODE"));
    assertEquals("false", setting.getRecoveryEnabled("YARN", "YARN_CLIENT"));
    assertEquals("true", setting.getRecoveryEnabled("YARN", "RESOURCE_MANAGER"));
  }

  @Test
  public void recoveryEnabledAtServiceLevel() {
    assertEquals("true", setting.getRecoveryEnabled("TEZ", "TEZ_CLIENT"));
    assertEquals("false", setting.getRecoveryEnabled("HDFS", "DATANODE"));
  }

  @Test
  public void recoveryEnabledAtClusterLevel() {
    assertEquals("true", setting.getRecoveryEnabled("OOZIE", "OOZIE_SERVER"));
    assertEquals("true", setting.getRecoveryEnabled("YARN", "NODE_MANAGER"));
  }

  @Test
  public void testAutoSkipFailureEnabled() {
    Map<String, String> skipFailureSetting = ImmutableMap.of(Setting.SETTING_NAME_SKIP_FAILURE, "true");
    Setting setting = new Setting(ImmutableMap.of(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS, Collections.singleton(skipFailureSetting)));
    assertTrue(setting.shouldSkipFailure());
  }

  @Test
  public void testAutoSkipFailureDisabled() {
    Map<String, String> skipFailureSetting = ImmutableMap.of(Setting.SETTING_NAME_SKIP_FAILURE, "false");
    Setting setting = new Setting(ImmutableMap.of(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS, Collections.singleton(skipFailureSetting)));
    assertFalse(setting.shouldSkipFailure());
  }

  @Test
  public void testAutoSkipFailureUnspecified() {
    Setting setting = new Setting(ImmutableMap.of());
    assertFalse(setting.shouldSkipFailure());
  }

}
