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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.topology.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AmbariServer.class)
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

  private AmbariManagementController controller;
  private AmbariMetaInfo metaInfo;


  @Before
  public void setup() throws Exception {
    controller = createNiceMock(AmbariManagementController.class);
    metaInfo = createNiceMock(AmbariMetaInfo.class);

    StackInfo hdpCore = new StackInfo();
    ServiceInfo hdfs = new ServiceInfo();
    hdfs.setProperties(createProperties());
    hdpCore.setServices(ImmutableList.of(hdfs));
    expect(metaInfo.getStack(anyObject(StackId.class))).andReturn(hdpCore);
    expect(metaInfo.getStacks()).andReturn(ImmutableList.of(hdpCore));
    expect(controller.getAmbariMetaInfo()).andReturn(metaInfo);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(controller);

    replay(controller, metaInfo);
    PowerMock.replay(AmbariServer.class);
  }

  @After
  public void tearDown() {
    reset(controller, metaInfo);
    PowerMock.reset(AmbariServer.class);
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
  public void testGetAllPasswordProperties() {
    assertEquals(EXPECTED_PASSWORD_PROPERTY_MAP, SecretReference.getAllPasswordProperties());
  }

  @Test
  public void testGetAllPasswordPropertiesWithStackId() {
    assertEquals(EXPECTED_PASSWORD_PROPERTY_MAP,
      SecretReference.getAllPasswordProperties(ImmutableList.of(new StackId("HDPCORE", "1.0.0"))));
  }

  @Test
  public void replacePasswordsInConfiguration() {
    Map<String, String> propertyMap =
      PROPERTY_NAMES.stream().collect(toMap(Function.identity(), propertyName -> "someValue"));

    Map<String, String> replacedPropertyMap =
      PROPERTY_NAMES.stream().collect(toMap(
        Function.identity(),
        propertyName -> PASSWORD_PROPERTIES.contains(propertyName) ? secret(propertyName) : "someValue")
      );

    Map<String, Map<String, String>> properties =
      ImmutableMap.of(RANGER_HDFS_POLICYMGR_SSL, propertyMap);

    Map<String, Map<String, Map<String, String>>> attributes =
      ImmutableMap.of(RANGER_HDFS_POLICYMGR_SSL, ImmutableMap.of("final", propertyMap));

    Configuration config = new Configuration(properties, attributes);

    Configuration replacedConfig =
      SecretReference.replacePasswordsInConfigurations(config, EXPECTED_PASSWORD_PROPERTY_MAP);

    assertEquals(replacedPropertyMap, replacedConfig.getProperties().entrySet().iterator().next().getValue());
    assertEquals(replacedPropertyMap,
      replacedConfig.getAttributes().entrySet().iterator().next().getValue().entrySet().iterator().next().getValue());
  }

  private static String secret(String propertyName) {
    return "SECRET:" + RANGER_HDFS_POLICYMGR_SSL + ":" + propertyName;
  }

}