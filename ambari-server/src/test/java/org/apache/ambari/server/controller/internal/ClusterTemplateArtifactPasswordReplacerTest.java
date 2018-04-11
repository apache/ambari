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

package org.apache.ambari.server.controller.internal;


import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ClusterTemplateArtifactPasswordReplacerTest {

  private static final List<Map<String, Object>> CONFIGURATION_1 =
    createConfiguration("hdfs-site", map("property1", "value1"));

  private static final List<Map<String, Object>> CONFIGURATION_2 =
    createConfiguration("yarn-site", map("property2", "value2"));

  private static final List<Map<String, Object>> CONFIGURATION_3 =
    createConfiguration("hbase-site", map("property3", "value3"));

  private static final List<Map<String, Object>> CONFIGURATION_1_REPLACED =
    createConfiguration("hdfs-site", map("property1", "value1.replaced"));

  private static final List<Map<String, Object>> CONFIGURATION_2_REPLACED =
    createConfiguration("yarn-site", map("property2", "value2.replaced"));

  private static final List<Map<String, Object>> CONFIGURATION_3_REPLACED =
    createConfiguration("hbase-site", map("property3", "value3.replaced"));


  private static final Map<String, Object> ORIGINAL_CLUSTER_TEMPLATE =
    createClusterTemplateArtifact(CONFIGURATION_1, CONFIGURATION_2, CONFIGURATION_3);

  private static final Map<String, Object> EXPECTED_PROCESSED_CLUSTER_TEMPLATE =
    createClusterTemplateArtifact(CONFIGURATION_1_REPLACED, CONFIGURATION_2_REPLACED, CONFIGURATION_3_REPLACED);

  /**
   * Test to prove that all configurations are replaced within a cluster template structure no matter where
   * they are defined.
   * @throws Exception
   */
  @Test
  public void testApplyToAllConfigurations() throws Exception {
    // given
    ClusterTemplateArtifactPasswordReplacer replacer = new ClusterTemplateArtifactPasswordReplacer();

    // when
    Object clusterTemplateWithReplacedConfigs =
      replacer.applyToAllConfigurations(
        ORIGINAL_CLUSTER_TEMPLATE,
        config -> replaceValues(config));

    // then
    assertEquals(EXPECTED_PROCESSED_CLUSTER_TEMPLATE, clusterTemplateWithReplacedConfigs);
  }

  /**
   * Creates a cluster template artifact with configuration objects at different levels
   * @param configuration1 mpack instance level configuration
   * @param configuration2 service instance level configuration
   * @param configuration3 cluster template level configuration
   * @return
   */
  private static Map<String, Object> createClusterTemplateArtifact(Object configuration1, Object configuration2, Object configuration3) {
    return
      map(
        "mpack_instances",
        list(
          map(
            "name", "HDPCORE",
            "version", "1.0.0.0",
            "configurations", configuration1,
            "service_instances", list(
              map(
                "name", "ZK1",
                "type", "ZOOKEEPER",
                "configurations", configuration2
              )
            )
          )
        ),
        "host_groups",
        list(
          map(
            "name", "hostgroup1",
            "host_count", "1"
          )
        ),
        "configurations", configuration3
      );
  }

  /**
   *
   * @param configType the config type, e.g. hdfs-site
   * @param properties the properties
   * @return a simple configuration in <code>[{"configType": {"properties": {"key1": "value1", ...}}}]</code> format.
   */
  private static List<Map<String, Object>> createConfiguration(String configType, Map<String, String> properties) {
    return list(map(configType, map("properties", properties)));
  }

  /**
   * Replaces property values by appending a ".replaced" suffix to each value. E.g:
   * <code>[ {"hdfs-site": {"properties": {"property": "value", ...}}}</code>
   * will become
   * <code>[ {"hdfs-site": {"properties": {"property": "value.replaced", ...}}}</code>
   * @param configuration
   * @return
   */
  private static List<Map<String, Object>> replaceValues(List<Map<String, Object>> configuration) {
    return configuration.stream().map(
      config -> {
        String configType = config.keySet().iterator().next();
        Map<String, String> properties =
          ((Map<String, Map<String, String>>)config.values().iterator().next()).values().iterator().next();
        ImmutableMap.Builder<String, Object> replacedProperties = ImmutableMap.builder();
        properties.entrySet().stream().forEach(
          e -> replacedProperties.put(e.getKey(), e.getValue() + ".replaced"));
        return ImmutableMap.<String, Object>of(configType, map("properties", replacedProperties.build()));
      }
    ).collect(toList());
  }

  // ---- Convenience methods to create lists and maps

  private static <T> List<T> list(T... elements) {
    return ImmutableList.copyOf(elements);
  }

  private static <K, V> Map<K, V> map(K k1, V v1) {
    return ImmutableMap.of(k1, v1);
  }

  private static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
    return ImmutableMap.of(k1, v1, k2, v2);
  }

  private static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3) {
    return ImmutableMap.of(k1, v1, k2, v2, k3, v3);
  }

  private static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
  }

}