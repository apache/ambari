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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * SimpleResourceDefinition tests.
 */
public class SimpleResourceDefinitionTest {

  @Test
  public void testGetPluralName() throws Exception {
    ResourceDefinition resourceDefinition =
        new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages", Resource.Type.Task);

    assertEquals("stages", resourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    ResourceDefinition resourceDefinition =
        new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages", Resource.Type.Task);

    assertEquals("stage", resourceDefinition.getSingularName());
  }

  @Test
  public void testGetCreateDirectives() {
    ResourceDefinition resourceDefinition =
        new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages", Resource.Type.Task);

    assertEquals(Collections.EMPTY_SET, resourceDefinition.getCreateDirectives());

    resourceDefinition = new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages",
            Collections.singleton(Resource.Type.Task), Arrays.asList("do_something", "do_something_else"));

    assertEquals(new HashSet<String>() {{add("do_something"); add("do_something_else");}},
        resourceDefinition.getCreateDirectives());
  }
}