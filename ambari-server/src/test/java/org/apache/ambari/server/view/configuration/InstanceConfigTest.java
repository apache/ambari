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

package org.apache.ambari.server.view.configuration;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.List;

/**
 * InstanceConfig tests.
 */
public class InstanceConfigTest {
  @Test
  public void testGetName() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("INSTANCE1", instances.get(0).getName());
    Assert.assertEquals("INSTANCE2", instances.get(1).getName());
  }

  @Test
  public void testGetProperties() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    List<PropertyConfig> properties = instances.get(0).getProperties();
    Assert.assertEquals(2, properties.size());

    properties = instances.get(1).getProperties();
    Assert.assertEquals(1, properties.size());
  }

  public static List<InstanceConfig> getInstanceConfigs() throws JAXBException {
    ViewConfig config = ViewConfigTest.getConfig();
    return config.getInstances();
  }
}
