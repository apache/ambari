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

package org.apache.ambari.server.utils;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.topology.Configuration;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

public class SecretReferenceTest {

  public static final String RANGER_HDFS_POLICYMGR_SSL = "ranger-hdfs-policymgr-ssl.xml";
  public static final String RANGER_HDFS_POLICYMGR_SSL_XML = RANGER_HDFS_POLICYMGR_SSL + ".xml";

  public static final Set<String> PASSWORD_PROPERTIES = ImmutableSet.of(
    "xasecure.policymgr.clientssl.keystore.password",
    "xasecure.policymgr.clientssl.truststore.password"
  );

  public static final List<String> PROPERTY_NAMES = ImmutableList.of(
    "xasecure.policymgr.clientssl.keystore.password",
    "xasecure.policymgr.clientssl.truststore.password",
    "xasecure.policymgr.clientssl.keystore.credential.file",
    "xasecure.policymgr.clientssl.truststore.credential.file"
  );

  public static final Multimap<String, String> EXPECTED_PASSWORD_PROPERTY_MAP =
    ImmutableSetMultimap.<String, String>builder().
    putAll(RANGER_HDFS_POLICYMGR_SSL, PASSWORD_PROPERTIES).
    build();

  private StackInfo hdpCore;

  @Before
  public void setup() throws Exception {
    hdpCore = new StackInfo();
    ServiceInfo hdfs = new ServiceInfo();
    hdfs.setProperties(createProperties());
    hdpCore.setServices(ImmutableList.of(hdfs));
  }


  private List<PropertyInfo> createProperties() {
    return PROPERTY_NAMES.stream().map(propertyName -> {
      PropertyInfo propertyInfo = new PropertyInfo();
      propertyInfo.setFilename(RANGER_HDFS_POLICYMGR_SSL_XML);
      propertyInfo.setName(propertyName);
      if (PASSWORD_PROPERTIES.contains(propertyName)) {
        propertyInfo.setPropertyTypes(ImmutableSet.of(PropertyInfo.PropertyType.PASSWORD));
      }
      return propertyInfo;
    }).collect(toList());
  }

  @Test
  public void testGetAllPasswordPropertiesInternal() {
    assertEquals(EXPECTED_PASSWORD_PROPERTY_MAP,
      SecretReference.getAllPasswordPropertiesInternal(ImmutableList.of(hdpCore)));
  }


  @Test
  public void testReplacePasswordsInConfiguration() {
    // given
    Map<String, String> propertyMap =
      PROPERTY_NAMES.stream().collect(toMap(Function.identity(), propertyName -> "someValue"));

    Map<String, Map<String, String>> properties =
      ImmutableMap.of(RANGER_HDFS_POLICYMGR_SSL, propertyMap);

    Map<String, Map<String, Map<String, String>>> attributes =
      ImmutableMap.of(RANGER_HDFS_POLICYMGR_SSL, ImmutableMap.of("final", propertyMap));

    Configuration config = new Configuration(properties, attributes);

    // when
    Configuration replacedConfig =
      SecretReference.replacePasswordsInConfigurations(config, EXPECTED_PASSWORD_PROPERTY_MAP);

    // then
    Map<String, String> replacedPropertyMap =
      PROPERTY_NAMES.stream().collect(toMap(
        Function.identity(),
        propertyName -> PASSWORD_PROPERTIES.contains(propertyName) ?
          secret(propertyName) :
          "someValue")
      );

    assertEquals(replacedPropertyMap, replacedConfig.getProperties().entrySet().iterator().next().getValue());
    assertEquals(replacedPropertyMap,
      replacedConfig.getAttributes().entrySet().iterator().next().getValue().entrySet().iterator().next().getValue());
  }

  private static String secret(String propertyName) {
    return "SECRET:" + RANGER_HDFS_POLICYMGR_SSL + ":" + propertyName;
  }

}