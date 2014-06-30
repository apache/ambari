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

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.easymock.EasyMock.createNiceMock;

/**
 * PermissionResourceProvider tests.
 */
public class PermissionResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();

    Request request = createNiceMock(Request.class);

    try {
      provider.createResources(request);
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testGetResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    // built in permissions
    Assert.assertEquals(4, resources.size());
  }

  @Test
  public void testUpdateResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();

    Request request = createNiceMock(Request.class);

    try {
      provider.updateResources(request, null);
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testDeleteResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();

    try {
      provider.deleteResources(null);
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }
}
