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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KerberosComponentDescriptorTest {
  public static final String JSON_VALUE =
      " {" +
          "  \"name\": \"COMPONENT_NAME\"," +
          "  \"identities\": [" +
          KerberosIdentityDescriptorTest.JSON_VALUE +
          "]," +
          "  \"configurations\": [" +
          "    {" +
          "      \"service-site\": {" +
          "        \"service.component.property1\": \"value1\"," +
          "        \"service.component.property2\": \"value2\"" +
          "      }" +
          "    }" +
          "  ]" +
          "}";

  public static final Map<String, Object> MAP_VALUE =
      new HashMap<String, Object>() {
        {
          put("name", "A_DIFFERENT_COMPONENT_NAME");
          put(KerberosDescriptorType.IDENTITY.getDescriptorPluralName(), new ArrayList<Object>() {{
            add(KerberosIdentityDescriptorTest.MAP_VALUE);
            add(KerberosIdentityDescriptorTest.MAP_VALUE_ALT);
            add(KerberosIdentityDescriptorTest.MAP_VALUE_REFERENCE);
          }});
          put(KerberosDescriptorType.CONFIGURATION.getDescriptorPluralName(), new ArrayList<Map<String, Object>>() {{
            add(new HashMap<String, Object>() {
              {
                put("service-site", new HashMap<String, String>() {
                  {
                    put("service.component.property1", "red");
                    put("service.component.property", "green");
                  }
                });
              }
            });
          }});
        }
      };

  public static void validateFromJSON(KerberosComponentDescriptor componentDescriptor) {
    Assert.assertNotNull(componentDescriptor);
    Assert.assertTrue(componentDescriptor.isContainer());

    Assert.assertEquals("COMPONENT_NAME", componentDescriptor.getName());

    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities();

    Assert.assertNotNull(identities);
    Assert.assertEquals(1, identities.size());

    Map<String, KerberosConfigurationDescriptor> configurations = componentDescriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("service-site");

    Assert.assertNotNull(configuration);

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("service-site", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("value1", properties.get("service.component.property1"));
    Assert.assertEquals("value2", properties.get("service.component.property2"));
  }

  public static void validateFromMap(KerberosComponentDescriptor componentDescriptor) {
    Assert.assertNotNull(componentDescriptor);
    Assert.assertTrue(componentDescriptor.isContainer());

    Assert.assertEquals("A_DIFFERENT_COMPONENT_NAME", componentDescriptor.getName());

    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities();

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Map<String, KerberosConfigurationDescriptor> configurations = componentDescriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("service-site");

    Assert.assertNotNull(configuration);

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("service-site", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("red", properties.get("service.component.property1"));
    Assert.assertEquals("green", properties.get("service.component.property"));
  }

  public static void validateUpdatedData(KerberosComponentDescriptor componentDescriptor) {
    Assert.assertNotNull(componentDescriptor);

    Assert.assertEquals("A_DIFFERENT_COMPONENT_NAME", componentDescriptor.getName());

    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities();

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Map<String, KerberosConfigurationDescriptor> configurations = componentDescriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("service-site");

    Assert.assertNotNull(configuration);

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("service-site", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(3, properties.size());
    Assert.assertEquals("red", properties.get("service.component.property1"));
    Assert.assertEquals("value2", properties.get("service.component.property2"));
    Assert.assertEquals("green", properties.get("service.component.property"));
  }

  private static KerberosComponentDescriptor createFromJSON() {
    Map<Object, Object> map = new Gson()
        .fromJson(JSON_VALUE, new TypeToken<Map<Object, Object>>() {
        }.getType());
    return new KerberosComponentDescriptor(map);
  }

  private static KerberosComponentDescriptor createFromMap() throws AmbariException {
    return new KerberosComponentDescriptor(MAP_VALUE);
  }

  @Test
  public void testJSONDeserialize() {
    validateFromJSON(createFromJSON());
  }

  @Test
  public void testMapDeserialize() throws AmbariException {
    validateFromMap(createFromMap());
  }

  @Test
  public void testEquals() throws AmbariException {
    Assert.assertTrue(createFromJSON().equals(createFromJSON()));
    Assert.assertFalse(createFromJSON().equals(createFromMap()));
  }

  @Test
  public void testToMap() throws AmbariException {
    KerberosComponentDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(MAP_VALUE, descriptor.toMap());
  }


  @Test
  public void testUpdate() throws AmbariException {
    KerberosComponentDescriptor componentDescriptor = createFromJSON();
    KerberosComponentDescriptor updatedComponentDescriptor = createFromMap();

    Assert.assertNotNull(componentDescriptor);
    Assert.assertNotNull(updatedComponentDescriptor);

    componentDescriptor.update(updatedComponentDescriptor);

    validateUpdatedData(componentDescriptor);
  }
}