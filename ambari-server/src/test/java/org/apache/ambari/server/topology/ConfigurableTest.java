/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class ConfigurableTest {
  public static final String CONFIG_JSON;
  // TODO: This crazy initialization is needed to work around a suspected Eclipse java compiler bug.
  //       It will be removed once unit test compilation will have been fixed and IDE will be able to use javac.
  static {
    String str1 =
      "[" +
        "  {" +
        "    'hdfs-site': {" +
        "      'properties': {" +
        "        'dfs.block.access.token.enable': 'true'," +
        "        'dfs.blocksize': '134217728'" +
        "      }," +
        "      'properties_attributes': {" +
        "        'final': {" +
        "          'fs.webhdfs.enabled': 'true'," +
        "          'dfs.namenode.http-address': 'true'" +
        "        }" +
        "      }" +
        "    }" +
        "  }," +
        "  {" +
        "    'core-site': {" +
        "      'properties': {" +
        "        'fs.defaultFS': 'hdfs://mycluster'," +
        "        'fs.trash.interval': '360'" +
        "      }," +
        "      'properties_attributes': {" +
        "        'final': {" +
        "          'fs.defaultFS': 'true'" +
        "        }" +
        "      }" +
        "    }" +
        "  }" +
        "]";
    String str2 = str1.replace('\'', '"');
    CONFIG_JSON = str2;
  }

  private List<Map<String, Map<String, Map<String, ? extends Object>>>> rawConfig;
  private Map<String, Map<String, String>> expectedProperties;
  private Map<String, Map<String, Map<String, String>>>expectedAttributes;
  private SimpleConfigurable configurable;

  @Before
  public void setUp() throws Exception {
    // TODO: Remove this check of static field initialization correctness once IDE can use javac
    assertEquals(CONFIG_JSON.replace('\'', '"'), CONFIG_JSON);

    configurable = new SimpleConfigurable();
    rawConfig = new ObjectMapper().readValue(CONFIG_JSON,
      new TypeReference<List<Map<String, Map<String, Map<String, ? extends Object>>>>>() {});

    expectedProperties = ImmutableMap.of(
      "hdfs-site", ImmutableMap.of(
        "dfs.block.access.token.enable", "true",
        "dfs.blocksize", "134217728"
      ),
      "core-site", ImmutableMap.of(
        "fs.defaultFS", "hdfs://mycluster",
        "fs.trash.interval", "360"
      )
    );

    expectedAttributes = ImmutableMap.of(
      "hdfs-site", ImmutableMap.of(
        "final", ImmutableMap.of(
          "fs.webhdfs.enabled", "true",
          "dfs.namenode.http-address", "true"
        )
      ),
      "core-site", ImmutableMap.of(
        "final", ImmutableMap.of("fs.defaultFS", "true")
      )
    );
  }

  @Test
  public void setConfigs() throws Exception {
    configurable.setConfigs(rawConfig);
    assertEquals(expectedProperties, configurable.getConfiguration().getProperties());
    assertEquals(expectedAttributes, configurable.getConfiguration().getAttributes());
  }

  @Test
  public void getConfigs() throws Exception {
    Configuration conf = new Configuration(expectedProperties, expectedAttributes);
    configurable.setConfiguration(conf);
    assertEquals(rawConfig, configurable.getConfigs());
  }

  static class SimpleConfigurable implements Configurable {

    private Configuration configuration;

    @Override
    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }

    @Override
    public Configuration getConfiguration() {
      return this.configuration;
    }
  }
}