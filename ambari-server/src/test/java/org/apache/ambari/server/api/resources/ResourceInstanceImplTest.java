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


package org.apache.ambari.server.api.resources;


import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * ResourceInstanceImpl unit tests.
 */
public class ResourceInstanceImplTest {
  @Test
  public void testIsCollection__True() {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, "cluster");
    mapIds.put(Resource.Type.Service, null);

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstance instance = new ResourceInstanceImpl(mapIds, resourceDefinition, null);
    assertTrue(instance.isCollectionResource());

    verify(resourceDefinition);
  }

  @Test
  public void testIsCollection__False() {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, "cluster");
    mapIds.put(Resource.Type.Service, "service");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstance instance = new ResourceInstanceImpl(mapIds, resourceDefinition, null);
    assertFalse(instance.isCollectionResource());

    verify(resourceDefinition);
  }
}
