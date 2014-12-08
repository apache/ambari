/**
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KerberosIdentityDescriptorTest {
  public static final String JSON_VALUE =
      "{" +
          "  \"name\": \"identity_1\"" +
          "," +
          "  \"principal\":" + KerberosPrincipalDescriptorTest.JSON_VALUE +
          "," +
          "  \"keytab\":" + KerberosKeytabDescriptorTest.JSON_VALUE +
          "}";

  public static final Map<String, Object> MAP_VALUE =
      new HashMap<String, Object>() {
        {
          put("name", "identity_1");
          put("principal", KerberosPrincipalDescriptorTest.MAP_VALUE);
          put("keytab", KerberosKeytabDescriptorTest.MAP_VALUE);
          put("password", "secret");
        }
      };

  public static final Map<String, Object> MAP_VALUE_ALT =
      new HashMap<String, Object>() {
        {
          put("name", "identity_2");
          put("principal", KerberosPrincipalDescriptorTest.MAP_VALUE);
          put("keytab", KerberosKeytabDescriptorTest.MAP_VALUE);
          put("password", "secret2");
        }
      };

  public static final Map<String, Object> MAP_VALUE_REFERENCE =
      new HashMap<String, Object>() {
        {
          put("name", "/shared");
          put("keytab", new HashMap<String, Object>() {
            {
              put("file", "/home/user/me/subject.service.keytab");

              put("owner", new HashMap<String, Object>() {{
                put("name", "me");
                put("access", "rw");
              }});

              put("group", new HashMap<String, Object>() {{
                put("name", "nobody");
                put("access", "");
              }});

              put("configuration", "service-site/me.component.keytab.file");
            }
          });
        }
      };

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
}