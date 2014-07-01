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

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.view.ResourceProvider;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * ViewContextImpl tests.
 */
public class ViewContextImplTest {
  @Test
  public void testGetViewName() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals("MY_VIEW", viewContext.getViewName());
  }

  @Test
  public void testGetInstanceName() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals("INSTANCE1", viewContext.getInstanceName());
  }

  @Test
  public void testGetProperties() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    viewInstanceDefinition.putProperty("p1", "v1");
    viewInstanceDefinition.putProperty("p2", new DefaultMasker().mask("v2"));
    viewInstanceDefinition.putProperty("p3", "v3");

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Map<String, String> properties = viewContext.getProperties();
    Assert.assertEquals(3, properties.size());

    Assert.assertEquals("v1", properties.get("p1"));
    Assert.assertEquals("v2", properties.get("p2"));
    Assert.assertEquals("v3", properties.get("p3"));
  }

  @Test
  public void testGetPropertiesWithParameters() throws Exception {
    InstanceConfig instanceConfig = createNiceMock(InstanceConfig.class);
    replay(instanceConfig);
    ViewEntity viewDefinition = createNiceMock(ViewEntity.class);
    expect(viewDefinition.getCommonName()).andReturn("View").times(2);
    expect(viewDefinition.getClassLoader()).andReturn(ViewContextImplTest.class.getClassLoader()).anyTimes();
    expect(viewDefinition.getConfiguration()).andReturn(ViewConfigTest.getConfig()).anyTimes();
    replay(viewDefinition);
    ViewInstanceEntity viewInstanceDefinition = createMockBuilder(ViewInstanceEntity.class)
        .addMockedMethod("getUsername")
        .addMockedMethod("getName")
        .addMockedMethod("getViewEntity")
        .withConstructor(viewDefinition, instanceConfig).createMock();
    expect(viewInstanceDefinition.getUsername()).andReturn("User").times(1);
    expect(viewInstanceDefinition.getUsername()).andReturn("User2").times(1);
    expect(viewInstanceDefinition.getName()).andReturn("Instance").times(3);
    expect(viewInstanceDefinition.getViewEntity()).andReturn(viewDefinition).times(1);
    replay(viewInstanceDefinition);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    viewInstanceDefinition.putProperty("p1", "/tmp/some/path/${username}");
    viewInstanceDefinition.putProperty("p2", new DefaultMasker().mask("/tmp/path/$viewName"));
    viewInstanceDefinition.putProperty("p3", "/path/$instanceName");
    viewInstanceDefinition.putProperty("p4", "/path/to/${unspecified_parameter}");
    viewInstanceDefinition.putProperty("p5", "/path/to/${incorrect_parameter");
    viewInstanceDefinition.putProperty("p6", "/path/to/\\${username}");
    viewInstanceDefinition.putProperty("p7", "/path/to/\\$viewName");

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Map<String, String> properties = viewContext.getProperties();
    Assert.assertEquals(7, properties.size());
    Assert.assertEquals("/tmp/some/path/User", properties.get("p1"));
    Assert.assertEquals("/tmp/path/View", properties.get("p2"));
    Assert.assertEquals("/path/Instance", properties.get("p3"));
    Assert.assertEquals("/path/to/${unspecified_parameter}", properties.get("p4"));
    Assert.assertEquals("/path/to/${incorrect_parameter", properties.get("p5"));
    Assert.assertEquals("/path/to/${username}", properties.get("p6"));
    Assert.assertEquals("/path/to/$viewName", properties.get("p7"));

    properties = viewContext.getProperties();
    Assert.assertEquals(7, properties.size());
    Assert.assertEquals("/tmp/some/path/User2", properties.get("p1"));
    Assert.assertEquals("/tmp/path/View", properties.get("p2"));
    Assert.assertEquals("/path/Instance", properties.get("p3"));
    Assert.assertEquals("/path/to/${unspecified_parameter}", properties.get("p4"));
    Assert.assertEquals("/path/to/${incorrect_parameter", properties.get("p5"));
    Assert.assertEquals("/path/to/${username}", properties.get("p6"));
    Assert.assertEquals("/path/to/$viewName", properties.get("p7"));
  }

  @Test
  public void testGetResourceProvider() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW{1.0.0}/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals(provider, viewContext.getResourceProvider("myType"));
  }

  @Test
  public void testGetURLStreamProvider() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertNotNull(viewContext.getURLStreamProvider());
  }

  @Test
  public void testViewURLStreamProvider() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL("spec", "requestMethod", "params", headerMap)).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewContextImpl.ViewURLStreamProvider viewURLStreamProvider =
        new ViewContextImpl.ViewURLStreamProvider(streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, inputStream);
  }
}
