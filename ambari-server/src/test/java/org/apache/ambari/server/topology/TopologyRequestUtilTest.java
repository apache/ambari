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

package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.ambari.server.state.StackId;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class TopologyRequestUtilTest {

  private static final String REQUEST_WITH_MPACK_INSTANCES =
    "{ 'mpack_instances' : [ {'name': 'HDPCORE', 'version': '1.0.0-b98'}, {'name': 'EDW', 'version': '1.0.0'} ] }".replace('\'', '"');

  private static final String REQUEST_WITH_INVALID_MPACK_INSTANCE =
    "{ 'mpack_instances' : [ {'name': 'HDPCORE', 'version': '1.0.0-b98'}, {'name': 'EDW'} ] }".replace('\'', '"');

  private static final String REQUEST_WITHOUT_MPACK_INSTANCE = "{}";


  @Test
  public void testGetStackIdsFromRawRequest_normalCase() {
    assertEquals(
      ImmutableSet.of(new StackId("HDPCORE", "1.0.0-b98"), new StackId("EDW", "1.0.0")),
      TopologyRequestUtil.getStackIdsFromRequest(REQUEST_WITH_MPACK_INSTANCES));
  }

  @Test
  public void testGetStackIdsFromRawRequest_noMpackInstances() {
    assertEquals(
      Collections.emptySet(),
      TopologyRequestUtil.getStackIdsFromRequest(REQUEST_WITHOUT_MPACK_INSTANCE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetStackIdsFromRawRequest_wrongMpackInstance() {
    TopologyRequestUtil.getStackIdsFromRequest(REQUEST_WITH_INVALID_MPACK_INSTANCE);
  }

}