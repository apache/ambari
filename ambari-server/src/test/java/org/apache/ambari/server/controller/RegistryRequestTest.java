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
package org.apache.ambari.server.controller;

import org.apache.ambari.server.registry.RegistryType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for RegistryRequest
 */
public class RegistryRequestTest {
  @Test
  public void testBasicGetAndSet() {
    RegistryRequest registryRequest =
            new RegistryRequest((Long) 1L, "hwx", RegistryType.JSON, "hwx-software-registry.json");
    Assert.assertEquals((Long) 1L, registryRequest.getRegistryId());
    Assert.assertEquals("hwx-software-registry.json", registryRequest.getRegistryUri());
    Assert.assertEquals("hwx", registryRequest.getRegistryName());
    Assert.assertEquals(RegistryType.JSON, registryRequest.getRegistryType());
  }
}
