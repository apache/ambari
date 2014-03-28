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

package org.apache.ambari.server.orm.entities;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.view.ResourceProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;

/**
 * ViewInstanceEntity tests.
 */
public class ViewInstanceEntityTest {

  @Test
  public void testGetViewEntity() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertEquals(viewDefinition, viewInstanceDefinition.getViewEntity());
  }

  @Test
  public void testGetConfiguration() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertEquals(instanceConfig, viewInstanceDefinition.getConfiguration());
  }

  @Test
  public void testGetName() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Assert.assertEquals("INSTANCE1", viewInstanceDefinition.getName());
  }

  @Test
  public void testAddGetProperty() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    viewInstanceDefinition.putProperty("p1", "v1");
    viewInstanceDefinition.putProperty("p2", "v2");
    viewInstanceDefinition.putProperty("p3", "v3");

    Map<String, String> properties = viewInstanceDefinition.getPropertyMap();

    Assert.assertEquals(3, properties.size());

    Assert.assertEquals("v1", properties.get("p1"));
    Assert.assertEquals("v2", properties.get("p2"));
    Assert.assertEquals("v3", properties.get("p3"));
  }

  @Test
  public void testAddGetService() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Object service = new Object();

    viewInstanceDefinition.addService("resources", service);

    Object service2 = new Object();

    viewInstanceDefinition.addService("subresources", service2);

    Assert.assertEquals(service, viewInstanceDefinition.getService("resources"));
    Assert.assertEquals(service2, viewInstanceDefinition.getService("subresources"));
  }

  @Test
  public void testAddGetResourceProvider() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    Assert.assertEquals(provider, viewInstanceDefinition.getResourceProvider(type));
    Assert.assertEquals(provider, viewInstanceDefinition.getResourceProvider("myType"));
  }

  @Test
  public void testContextPath() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Assert.assertEquals(ViewInstanceEntity.VIEWS_CONTEXT_PATH_PREFIX + "MY_VIEW/INSTANCE1",
        viewInstanceDefinition.getContextPath());
  }

  public static ViewInstanceEntity getViewInstanceEntity() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    return new ViewInstanceEntity(viewDefinition, instanceConfig);
  }
}
