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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VersioningPropertyProvider Tests
 */
public class VersioningPropertyProviderTest {
  @Test
  public void testPopulateResources() throws Exception {

    Map<String, PropertyHelper.MetricsVersion> clusterVersionsMap =
        new HashMap<String, PropertyHelper.MetricsVersion>();

    clusterVersionsMap.put("c1", PropertyHelper.MetricsVersion.HDP1);
    clusterVersionsMap.put("c2", PropertyHelper.MetricsVersion.HDP2);

    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers =
        new HashMap<PropertyHelper.MetricsVersion, AbstractPropertyProvider>();

    TestJMXPropertyProvider propertyProvider1 = new TestJMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));
    providers.put(PropertyHelper.MetricsVersion.HDP1, propertyProvider1);


    TestJMXPropertyProvider propertyProvider2 = new TestJMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    providers.put(PropertyHelper.MetricsVersion.HDP2, propertyProvider2);

    // set propertyProvider2 as the default provider
    VersioningPropertyProvider provider = new VersioningPropertyProvider(clusterVersionsMap, providers,
        propertyProvider2, PropertyHelper.getPropertyId("HostRoles", "cluster_name"));


    Request request = PropertyHelper.getReadRequest();

    Resource resource1 = new ResourceImpl(Resource.Type.HostComponent);
    resource1.setProperty(PropertyHelper.getPropertyId("HostRoles", "cluster_name"), "c1");


    provider.populateResources(Collections.singleton(resource1), request, null);

    Assert.assertEquals(resource1, propertyProvider1.getResource());
    Assert.assertNull(propertyProvider2.getResource());

    propertyProvider1.setResource(null);
    propertyProvider2.setResource(null);

    Resource resource2 = new ResourceImpl(Resource.Type.HostComponent);
    resource2.setProperty(PropertyHelper.getPropertyId("HostRoles", "cluster_name"), "c2");

    provider.populateResources(Collections.singleton(resource2), request, null);

    Assert.assertNull(propertyProvider1.getResource());
    Assert.assertEquals(resource2, propertyProvider2.getResource());

    propertyProvider1.setResource(null);
    propertyProvider2.setResource(null);

    Set<Resource> resources = new HashSet<Resource>();
    resources.add(resource1);
    resources.add(resource2);

    provider.populateResources(resources, request, null);

    Assert.assertEquals(resource1, propertyProvider1.getResource());
    Assert.assertEquals(resource2, propertyProvider2.getResource());


    // test resource with no associated cluster ... should go to default provider2
    propertyProvider1.setResource(null);
    propertyProvider2.setResource(null);

    Resource resource3 = new ResourceImpl(Resource.Type.HostComponent);

    provider.populateResources(Collections.singleton(resource3), request, null);

    Assert.assertNull(propertyProvider1.getResource());
    Assert.assertEquals(resource3, propertyProvider2.getResource());
  }

  private class TestJMXPropertyProvider extends JMXPropertyProvider {

    private Resource resource = null;


    public TestJMXPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                                   String clusterNamePropertyId,
                                   String hostNamePropertyId,
                                   String componentNamePropertyId,
                                   String statePropertyId,
                                   Set<String> healthyStates) {

      super(componentMetrics, null, null, clusterNamePropertyId, hostNamePropertyId,
          componentNamePropertyId, statePropertyId, healthyStates);
    }

    public Resource getResource() {
      return resource;
    }

    public void setResource(Resource resource) {
      this.resource = resource;
    }

    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {

      if (resources.size() == 1) {
        resource = resources.iterator().next();
      }
      return resources;
    }
  }
}
