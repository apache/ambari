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

import junit.framework.Assert;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 *
 */
public class ResourceImplTest {

  @Test
  public void testGetType() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    Assert.assertEquals(Resource.Type.Cluster, resource.getType());

    resource = new ResourceImpl(Resource.Type.Service);
    Assert.assertEquals(Resource.Type.Service, resource.getType());

    resource = new ResourceImpl(Resource.Type.Host);
    Assert.assertEquals(Resource.Type.Host, resource.getType());

    resource = new ResourceImpl(Resource.Type.Component);
    Assert.assertEquals(Resource.Type.Component, resource.getType());

    resource = new ResourceImpl(Resource.Type.HostComponent);
    Assert.assertEquals(Resource.Type.HostComponent, resource.getType());
  }

  @Test
  public void testSetGetProperty() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    String propertyId = PropertyHelper.getPropertyId("c1", "p1");
    resource.setProperty(propertyId, "foo");
    Assert.assertEquals("foo", resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, 1);
    Assert.assertEquals(1, resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, (float) 1.99);
    Assert.assertEquals((float) 1.99, resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, 1.99);
    Assert.assertEquals(1.99, resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, 65L);
    Assert.assertEquals(65L, resource.getPropertyValue(propertyId));
  }
}
