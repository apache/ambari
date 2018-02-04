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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.util.Providers;

public class RejectUnknownStacksTest extends EasyMockSupport {

  private final StackId validStackId = new StackId("valid", "1.0");
  private final StackId anotherValidStackId = new StackId("another", "2.1");
  private final StackId invalidStackId = new StackId("invalid", "3.2");
  private final StackId anotherInvalidStackId = new StackId("invalid", "1.1");
  private RejectUnknownStacks validator;

  @Before
  public void setUp() {
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    validator = new RejectUnknownStacks(Providers.of(metaInfo));

    expect(metaInfo.isKnownStack(validStackId)).andReturn(true).anyTimes();
    expect(metaInfo.isKnownStack(anotherValidStackId)).andReturn(true).anyTimes();
    expect(metaInfo.isKnownStack(invalidStackId)).andReturn(false).anyTimes();
    expect(metaInfo.isKnownStack(anotherInvalidStackId)).andReturn(false).anyTimes();
  }

  @Test(expected = InvalidTopologyException.class) // THEN
  public void rejectsUnknownStack() throws InvalidTopologyException {
    // GIVEN
    ClusterTopology topology = createNiceMock(ClusterTopology.class);
    expect(topology.getStackIds()).andReturn(ImmutableSet.of(validStackId, invalidStackId)).anyTimes();
    replayAll();

    // WHEN
    validator.validate(topology);
  }

  @Test
  public void acceptsKnownStack() throws InvalidTopologyException {
    // GIVEN
    ClusterTopology topology = createNiceMock(ClusterTopology.class);
    expect(topology.getStackIds()).andReturn(ImmutableSet.of(validStackId, anotherValidStackId)).anyTimes();
    replayAll();

    // WHEN
    validator.validate(topology);

    // THEN
    // no exception
  }

  @Test
  public void reportsUnknownStacks() {
    // GIVEN
    ClusterTopology topology = createNiceMock(ClusterTopology.class);
    expect(topology.getStackIds()).andReturn(ImmutableSet.of(invalidStackId, anotherInvalidStackId)).anyTimes();
    replayAll();

    // WHEN
    try {
      validator.validate(topology);
      fail("Expected " + InvalidTopologyException.class);
    } catch (InvalidTopologyException e) {
      // THEN
      assertTrue(e.getMessage(), e.getMessage().contains(invalidStackId.toString()));
      assertTrue(e.getMessage(), e.getMessage().contains(anotherInvalidStackId.toString()));
    }
  }
}
