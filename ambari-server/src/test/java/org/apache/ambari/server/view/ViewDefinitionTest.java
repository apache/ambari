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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ResourceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * ViewDefinition tests.
 */
public class ViewDefinitionTest {

  public static ViewDefinition getViewDefinition() throws Exception {
    return getViewDefinition(ViewConfigTest.getConfig());
  }

  public static ViewDefinition getViewDefinition(ViewConfig viewConfig) throws Exception {
    Properties properties = new Properties();
    properties.put("p1", "v1");
    properties.put("p2", "v2");
    properties.put("p3", "v3");

    Configuration ambariConfig = new Configuration(properties);
    return new ViewDefinition(viewConfig, ambariConfig);
  }

  @Test
  public void testGetName() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();
    Assert.assertEquals("MY_VIEW", viewDefinition.getName());
  }

  @Test
  public void testGetLabel() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();
    Assert.assertEquals("My View!", viewDefinition.getLabel());
  }

  @Test
  public void testGetVersion() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();
    Assert.assertEquals("1.0.0", viewDefinition.getVersion());
  }

  @Test
  public void testGetConfiguration() throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    ViewDefinition viewDefinition = getViewDefinition(viewConfig);
    Assert.assertEquals(viewConfig, viewDefinition.getConfiguration());
  }

  @Test
  public void testGetAmbariProperty() throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    ViewDefinition viewDefinition = getViewDefinition(viewConfig);
    Assert.assertEquals("v1", viewDefinition.getAmbariProperty("p1"));
    Assert.assertEquals("v2", viewDefinition.getAmbariProperty("p2"));
    Assert.assertEquals("v3", viewDefinition.getAmbariProperty("p3"));
  }

  @Test
  public void testAddGetResourceProvider() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();

    ResourceProvider provider1 = createNiceMock(ResourceProvider.class);

    Resource.Type type1 = new Resource.Type("myType1");
    viewDefinition.addResourceProvider(type1, provider1);

    Assert.assertEquals(provider1, viewDefinition.getResourceProvider(type1));

    ResourceProvider provider2 = createNiceMock(ResourceProvider.class);

    Resource.Type type2 = new Resource.Type("myType2");
    viewDefinition.addResourceProvider(type2, provider2);

    Assert.assertEquals(provider2, viewDefinition.getResourceProvider(type2));

    Set<Resource.Type> types = viewDefinition.getViewResourceTypes();

    Assert.assertEquals(2, types.size());

    Assert.assertTrue(types.contains(type1));
    Assert.assertTrue(types.contains(type2));
  }

  @Test
  public void testAddGetResourceDefinition() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();

    ViewSubResourceDefinition definition = createNiceMock(ViewSubResourceDefinition.class);
    Resource.Type type = new Resource.Type("myType");

    expect(definition.getType()).andReturn(type);

    replay(definition);

    viewDefinition.addResourceDefinition(definition);

    Assert.assertEquals(definition, viewDefinition.getResourceDefinition(type));

    verify(definition);
  }

  @Test
  public void testAddGetResourceConfiguration() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();

    ResourceConfig config = ResourceConfigTest.getResourceConfigs().get(0);

    Resource.Type type1 = new Resource.Type("myType");

    viewDefinition.addResourceConfiguration(type1, config);

    Assert.assertEquals(config, viewDefinition.getResourceConfigurations().get(type1));

    Resource.Type type2 = new Resource.Type("myType2");

    viewDefinition.addResourceConfiguration(type2, config);

    Assert.assertEquals(config, viewDefinition.getResourceConfigurations().get(type2));
  }

  @Test
  public void testAddGetInstanceDefinition() throws Exception {
    ViewDefinition viewDefinition = getViewDefinition();

    ViewInstanceDefinition definition = createNiceMock(ViewInstanceDefinition.class);

    expect(definition.getName()).andReturn("instance1");

    replay(definition);

    viewDefinition.addInstanceDefinition(definition);

    Assert.assertEquals(definition, viewDefinition.getInstanceDefinition("instance1"));

    Collection<ViewInstanceDefinition> definitions = viewDefinition.getInstanceDefinitions();

    Assert.assertEquals(1, definitions.size());

    Assert.assertTrue(definitions.contains(definition));

    verify(definition);
  }
}
