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

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * AbstractProviderModule tests.
 */
public class AbstractProviderModuleTest {
  @Test
  public void testGetMetricsVersion() throws Exception {

    TestAbstractProviderModule module = new TestAbstractProviderModule("HDP-1.0");
    PropertyHelper.MetricsVersion version = module.getMetricsVersion("c1");
    Assert.assertEquals(PropertyHelper.MetricsVersion.HDP1, version);
    version = module.getMetricsVersion("c2");
    Assert.assertNull(version);

    module = new TestAbstractProviderModule("HDPLocal-1.3.2");
    version = module.getMetricsVersion("c1");
    Assert.assertEquals(PropertyHelper.MetricsVersion.HDP1, version);

    module = new TestAbstractProviderModule("HDP-2.0.1");
    version = module.getMetricsVersion("c1");
    Assert.assertEquals(PropertyHelper.MetricsVersion.HDP2, version);

    module = new TestAbstractProviderModule("HDP-2.0.1.x");
    version = module.getMetricsVersion("c1");
    Assert.assertEquals(PropertyHelper.MetricsVersion.HDP2, version);

    module = new TestAbstractProviderModule("HDP-9.9.9");
    version = module.getMetricsVersion("c1");
    Assert.assertEquals(PropertyHelper.MetricsVersion.HDP2, version);

    module = new TestAbstractProviderModule("HDPLocal-2.0.0");
    version = module.getMetricsVersion("c1");
    Assert.assertEquals(PropertyHelper.MetricsVersion.HDP2, version);
  }

  private static class TestAbstractProviderModule extends AbstractProviderModule {
    private final String clusterVersion;

    private TestAbstractProviderModule(String clusterVersion) {
      this.clusterVersion = clusterVersion;
    }

    @Override
    protected ResourceProvider createResourceProvider(Resource.Type type) {
      return new TestResourceProvider(type, clusterVersion);
    }
  }

  private static class TestResourceProvider implements ResourceProvider {
    private final Resource.Type type;
    private final String clusterVersion;

    private TestResourceProvider(Resource.Type type, String clusterVersion) {
      this.type = type;
      this.clusterVersion = clusterVersion;
    }

    @Override
    public RequestStatus createResources(Request request)
        throws SystemException, UnsupportedPropertyException,
        ResourceAlreadyExistsException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

      if (type == Resource.Type.Cluster) {
        Resource cluster = new ResourceImpl(Resource.Type.Cluster);
        cluster.setProperty(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID, 1);
        cluster.setProperty(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "c1");
        cluster.setProperty(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, clusterVersion);

        return Collections.singleton(cluster);
      }
      return Collections.emptySet();
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public Map<Resource.Type, String> getKeyPropertyIds() {
      return null;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return null;
    }
  }
}
