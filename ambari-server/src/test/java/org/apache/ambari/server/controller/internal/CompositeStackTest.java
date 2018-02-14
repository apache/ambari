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
package org.apache.ambari.server.controller.internal;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerTest;
import org.apache.ambari.server.state.StackId;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompositeStackTest {

  private static Set<Stack> elements;
  private static StackDefinition composite;

  @BeforeClass
  public static void initStack() throws Exception{
    StackManager stackManager = StackManagerTest.createTestStackManager();
    elements = stackManager.getStacksByName().values().stream()
      .flatMap(stacks -> stacks.stream().limit(1))
      .filter(Objects::nonNull)
      .map(Stack::new)
      .collect(toSet());
    composite = StackDefinition.of(elements);
  }

  @Test
  public void getStackIds() {
    assertEquals(elements.size(), composite.getStackIds().size());
  }

  @Test
  public void getServices() {
    Set<String> services = new HashSet<>(composite.getServices());
    for (Stack stack : elements) {
      assertTrue(services.containsAll(stack.getServices()));
    }
    for (Stack stack : elements) {
      services.removeAll(stack.getServices());
    }
    assertEquals(emptySet(), services);
  }

  @Test
  public void getComponents() {
    Set<String> components = new HashSet<>(composite.getComponents());
    for (Stack stack : elements) {
      assertTrue(components.containsAll(stack.getComponents()));
    }
    for (Stack stack : elements) {
      components.removeAll(stack.getComponents());
    }
    assertEquals(emptySet(), components);
  }

  @Test
  public void getStacksForService() {
    Set<String> services = new HashSet<>(composite.getServices());
    for (String service : services) {
      Set<StackId> stackIds = new HashSet<>();
      for (Stack stack : elements) {
        if (stack.getServices().contains(service)) {
          stackIds.add(stack.getStackId());
        }
      }
      assertEquals(service, stackIds, composite.getStacksForService(service));
    }
  }

  @Test
  public void getStacksForComponent() {
    Set<String> components = new HashSet<>(composite.getComponents());
    for (String component : components) {
      Set<StackId> stackIds = new HashSet<>();
      for (Stack stack : elements) {
        if (stack.getComponents().contains(component)) {
          stackIds.add(stack.getStackId());
        }
      }
      assertEquals(component, stackIds, composite.getStacksForComponent(component));
    }
  }

  // TODO add more tests after StackDefinition interface is finalized

}
