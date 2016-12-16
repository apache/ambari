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
package org.apache.ambari.server.state.kerberos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

@Category({ category.KerberosTest.class})
public class KerberosIdentityDescriptorTest {
  public static final String JSON_VALUE =
      "{" +
          "  \"name\": \"identity_1\"" +
          "," +
          "  \"principal\":" + KerberosPrincipalDescriptorTest.JSON_VALUE +
          "," +
          "  \"keytab\":" + KerberosKeytabDescriptorTest.JSON_VALUE +
          "," +
          "  \"when\": {\"contains\" : [\"services\", \"HIVE\"]}" +
          "}";

  static final Map<String, Object> MAP_VALUE;
  static final Map<String, Object> MAP_VALUE_ALT;
  static final Map<String, Object> MAP_VALUE_REFERENCE;

  static {
    MAP_VALUE = new TreeMap<String, Object>();
    MAP_VALUE.put("name", "identity_1");
    MAP_VALUE.put("principal", KerberosPrincipalDescriptorTest.MAP_VALUE);
    MAP_VALUE.put("keytab", KerberosKeytabDescriptorTest.MAP_VALUE);
    MAP_VALUE.put("password", "secret");

    MAP_VALUE_ALT = new TreeMap<String, Object>();
    MAP_VALUE_ALT.put("name", "identity_2");
    MAP_VALUE_ALT.put("principal", KerberosPrincipalDescriptorTest.MAP_VALUE);
    MAP_VALUE_ALT.put("keytab", KerberosKeytabDescriptorTest.MAP_VALUE);
    MAP_VALUE_ALT.put("password", "secret2");

    TreeMap<String, Object> ownerMap = new TreeMap<String, Object>();
    ownerMap.put("name", "me");
    ownerMap.put("access", "rw");

    TreeMap<String, Object> groupMap = new TreeMap<String, Object>();
    groupMap.put("name", "nobody");
    groupMap.put("access", "");


    TreeMap<String, Object> keytabMap = new TreeMap<String, Object>();
    keytabMap.put("file", "/home/user/me/subject.service.keytab");
    keytabMap.put("owner", ownerMap);
    keytabMap.put("group", groupMap);
    keytabMap.put("configuration", "service-site/me.component.keytab.file");

    MAP_VALUE_REFERENCE = new TreeMap<String, Object>();
    MAP_VALUE_REFERENCE.put("name", "shared_identity");
    MAP_VALUE_REFERENCE.put("reference", "/shared");
    MAP_VALUE_REFERENCE.put("keytab", keytabMap);
  }


  public static void validateFromJSON(KerberosIdentityDescriptor identityDescriptor) {
    Assert.assertNotNull(identityDescriptor);
    Assert.assertFalse(identityDescriptor.isContainer());

    KerberosPrincipalDescriptorTest.validateFromJSON(identityDescriptor.getPrincipalDescriptor());
    KerberosKeytabDescriptorTest.validateFromJSON(identityDescriptor.getKeytabDescriptor());
    Assert.assertNull(identityDescriptor.getPassword());
  }

  public static void validateFromMap(KerberosIdentityDescriptor identityDescriptor) {
    Assert.assertNotNull(identityDescriptor);
    Assert.assertFalse(identityDescriptor.isContainer());

    KerberosPrincipalDescriptorTest.validateFromMap(identityDescriptor.getPrincipalDescriptor());
    KerberosKeytabDescriptorTest.validateFromMap(identityDescriptor.getKeytabDescriptor());
    Assert.assertEquals("secret", identityDescriptor.getPassword());
  }

  public static void validateUpdatedData(KerberosIdentityDescriptor identityDescriptor) {
    Assert.assertNotNull(identityDescriptor);

    KerberosPrincipalDescriptorTest.validateUpdatedData(identityDescriptor.getPrincipalDescriptor());
    KerberosKeytabDescriptorTest.validateUpdatedData(identityDescriptor.getKeytabDescriptor());
    Assert.assertEquals("secret", identityDescriptor.getPassword());
  }

  private static KerberosIdentityDescriptor createFromJSON() {
    Map<?, ?> map = new Gson().fromJson(JSON_VALUE, new TypeToken<Map<?, ?>>() {
    }.getType());
    return new KerberosIdentityDescriptor(map);
  }

  private static KerberosIdentityDescriptor createFromMap() {
    return new KerberosIdentityDescriptor(MAP_VALUE);
  }

  @Test
  public void testJSONDeserialize() {
    validateFromJSON(createFromJSON());
  }

  @Test
  public void testMapDeserialize() {
    validateFromMap(createFromMap());
  }

  @Test
  public void testEquals() throws AmbariException {
    Assert.assertTrue(createFromJSON().equals(createFromJSON()));
    Assert.assertFalse(createFromJSON().equals(createFromMap()));
  }

  @Test
  public void testToMap() throws AmbariException {
    KerberosIdentityDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(MAP_VALUE, descriptor.toMap());
  }

  @Test
  public void testUpdate() {
    KerberosIdentityDescriptor identityDescriptor = createFromJSON();
    KerberosIdentityDescriptor updatedIdentityDescriptor = createFromMap();

    Assert.assertNotNull(identityDescriptor);
    Assert.assertNotNull(updatedIdentityDescriptor);

    identityDescriptor.update(updatedIdentityDescriptor);

    validateUpdatedData(identityDescriptor);
  }

  @Test
  public void testShouldInclude() {
    KerberosIdentityDescriptor identityDescriptor = createFromJSON();

    Map<String, Object> context = new TreeMap<String, Object>();

    context.put("services", new HashSet<String>(Arrays.asList("HIVE", "HDFS", "ZOOKEEPER")));
    Assert.assertTrue(identityDescriptor.shouldInclude(context));

    context.put("services", new HashSet<String>(Arrays.asList("NOT_HIVE", "HDFS", "ZOOKEEPER")));
    Assert.assertFalse(identityDescriptor.shouldInclude(context));
  }
}