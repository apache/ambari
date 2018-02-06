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
package org.apache.ambari.server.topology.validators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class RejectUnknownComponentsTest {

  private TopologyValidator validator = new RejectUnknownComponents();
  private final ClusterTopology topology = createNiceMock(ClusterTopology.class);
  private final StackDefinition stack = createNiceMock(StackDefinition.class);

  @Before
  public void setUp() {
    expect(topology.getStack()).andReturn(stack).anyTimes();
  }

  @After
  public void tearDown() {
    reset(topology, stack);
  }

  @Test
  public void acceptsKnownComponents() throws Exception {
    // GIVEN
    componentsInTopologyAre("VALID_COMPONENT", "ANOTHER_COMPONENT");
    validComponentsAre("VALID_COMPONENT", "ANOTHER_COMPONENT", "ONE_MORE_COMPONENT");

    // WHEN
    validator.validate(topology);

    // THEN
    // no exception expected
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsUnknownComponents() throws Exception {
    // GIVEN
    componentsInTopologyAre("VALID_COMPONENT", "UNKNOWN_COMPONENT");
    validComponentsAre("VALID_COMPONENT", "ANOTHER_COMPONENT");

    // WHEN
    validator.validate(topology);
  }

  private void componentsInTopologyAre(String... components) {
    expect(topology.getComponentNames()).andReturn(Stream.of(components)).anyTimes();
    replay(topology);
  }

  private void validComponentsAre(String... components) {
    expect(stack.getComponents()).andReturn(ImmutableSet.<String>builder().add(components).build()).anyTimes();
    replay(stack);
  }

}
