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

package org.apache.ambari.server.view;

import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ResourceConfigTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;

import static org.easymock.EasyMock.createNiceMock;

/**
 * ViewRegistry tests.
 */
public class ViewRegistryTest {
  @Test
  public void testAddGetDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    Assert.assertEquals(viewDefinition, registry.getDefinition("MY_VIEW"));

    Collection<ViewEntity> viewDefinitions = registry.getDefinitions();

    Assert.assertEquals(1, viewDefinitions.size());

    Assert.assertEquals(viewDefinition, viewDefinitions.iterator().next());
  }

  @Test
  public void testAddGetInstanceDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = ViewInstanceEntityTest.getViewInstanceEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    registry.addInstanceDefinition(viewDefinition, viewInstanceDefinition);

    Assert.assertEquals(viewInstanceDefinition, registry.getInstanceDefinition("MY_VIEW", "INSTANCE1"));

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewDefinition);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceDefinition, viewInstanceDefinitions.iterator().next());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewRegistry registry = ViewRegistry.getInstance();

    ResourceConfig config = ResourceConfigTest.getResourceConfigs().get(0);
    Resource.Type type1 = new Resource.Type("myType");

    ResourceProvider provider1 = createNiceMock(ResourceProvider.class);
    viewDefinition.addResourceProvider(type1, provider1);

    viewDefinition.addResourceConfiguration(type1, config);
    registry.addDefinition(viewDefinition);
    Set<SubResourceDefinition> subResourceDefinitions = registry.getSubResourceDefinitions("MY_VIEW");

    Assert.assertEquals(1, subResourceDefinitions.size());
    Assert.assertEquals("myType", subResourceDefinitions.iterator().next().getType().name());
  }

  @Test
  public void testAddInstanceDefinition() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    ViewEntity viewEntity = ViewEntityTest.getViewEntity();
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity(viewEntity, instanceConfig);

    registry.addDefinition(viewEntity);
    registry.addInstanceDefinition(viewEntity, viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());
  }

  @Before
  public void before() throws Exception {
    ViewRegistry.getInstance().clear();
  }

  @AfterClass
  public static void afterClass() {
    ViewRegistry.getInstance().clear();
  }
}
