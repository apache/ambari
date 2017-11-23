/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.ambari.server.utils.ResourceUtils;
import org.junit.Test;

public class SecurityConfigurationTest {

  public static final String SECURITY_CONFIG_JSON = ResourceUtils.getResource("blueprintv2/security_config.json");
  public static final Map<String, ?> EXPECTED_KERBEROS_DESCRIPTOR = ImmutableMap.of(
    "identities", ImmutableList.of(identity("smokeuser","${cluster-env/smokeuser}@${realm}")),
    "services", ImmutableList.of(
      ImmutableMap.of(
        "name", "AMBARI",
        "identities", ImmutableList.of(),
        "components", ImmutableList.of(ImmutableMap.of(
          "name", "AMBARI_SERVER",
          "identities", ImmutableList.of(identity("ambari-server", "ambari-server@${realm}"))
        ))
      ),
      ImmutableMap.of(
        "name", "HDFS",
        "identities", ImmutableList.of(),
        "components", ImmutableList.of(ImmutableMap.of(
          "name", "NAMENODE",
          "identities", ImmutableList.of(identity("hdfs", "${hadoop-env/hdfs_user}@${realm}"))
        ))
      )
    ),
    "properties", ImmutableMap.of("principal_suffix", "")
  );


  @Test
  public void getDescriptor() throws Exception {
    SecurityConfiguration securityConfig = new ObjectMapper().readValue(SECURITY_CONFIG_JSON, SecurityConfiguration.class);
    assertEquals(EXPECTED_KERBEROS_DESCRIPTOR, new ObjectMapper().readValue(securityConfig.getDescriptor(), Map.class));
  }

  private static final Map<String, ?> identity(String name, String value) {
    return ImmutableMap.of(
      "name", name,
      "principal", ImmutableMap.of(
        "value", value,
        "type", "user"
      ));
  }
}