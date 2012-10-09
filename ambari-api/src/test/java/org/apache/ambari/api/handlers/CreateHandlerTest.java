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

package org.apache.ambari.api.handlers;

import org.apache.ambari.api.resources.ResourceDefinition;
import org.apache.ambari.api.services.PersistenceManager;
import org.apache.ambari.api.services.Request;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for CreateHandler.
 */
public class CreateHandlerTest {

  @Test
  public void testHandleRequest() {
    Request request = createMock(Request.class);
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    PersistenceManager pm = createStrictMock(PersistenceManager.class);

    Map<PropertyId, Object> resourceProperties = new HashMap<PropertyId, Object>();

    // expectations
    expect(request.getResourceDefinition()).andReturn(resource);
    expect(request.getHttpBodyProperties()).andReturn(resourceProperties);
    resource.setProperties(resourceProperties);
    expect(request.getPersistenceManager()).andReturn(pm);
    pm.persist(resource);

    replay(request, resource, pm);

    Object returnVal = new CreateHandler().handleRequest(request);
    //todo: additional assertions on return value?
    assertNotNull(returnVal);

    verify(request, resource, pm);
  }
}
