/*
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

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Set;

import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.HostSummaryRenderer;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * HostResourceDefinition unit tests.
 */
public class HostResourceDefinitionTest {

  @Test
  public void testGetPluralName() {
    assertEquals("hosts", new HostResourceDefinition().getPluralName());
  }

  @Test
  public void testGetSingularName() {
    assertEquals("host", new HostResourceDefinition().getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    final ResourceDefinition resource = new HostResourceDefinition();
    Set<SubResourceDefinition> subResources = resource.getSubResourceDefinitions();

    assertEquals(
      ImmutableSet.of(Resource.Type.Alert, Resource.Type.HostComponent, Resource.Type.HostKerberosIdentity),
      subResources.stream().map(SubResourceDefinition::getType).collect(toSet())
    );
  }

  @Test
  public void getRenderer() {
    ResourceDefinition resource = new HostResourceDefinition();
    assertSame(HostSummaryRenderer.class, resource.getRenderer("summary").getClass());
    assertSame(DefaultRenderer.class, resource.getRenderer(null).getClass());
  }
}
