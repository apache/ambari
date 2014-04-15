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

import junit.framework.TestCase;
import org.junit.Test;
import static junit.framework.TestCase.*;

public class RequestOperationLevelTest {

  private final String host_component = "HOST_COMPONENT";
  private final String hostComponent = "HostComponent";

  @Test
  public void testGetInternalLevelName() throws Exception {
    String internal = RequestOperationLevel.getInternalLevelName(host_component);
    assertEquals(internal, hostComponent);
    // Check case-insensitivity
    internal = RequestOperationLevel.getInternalLevelName(host_component.toLowerCase());
    assertEquals(internal, hostComponent);
    // Check wrong param
    try {
      RequestOperationLevel.getInternalLevelName("Wrong_param");
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testGetExternalLevelName() throws Exception {
    String external = RequestOperationLevel.getExternalLevelName(hostComponent);
    assertEquals(external, host_component);
    // Check wrong param
    try {
      RequestOperationLevel.getExternalLevelName("Wrong_param");
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
