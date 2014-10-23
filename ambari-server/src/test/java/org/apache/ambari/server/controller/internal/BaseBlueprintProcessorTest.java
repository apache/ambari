package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.state.DependencyInfo;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

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

public class BaseBlueprintProcessorTest {

  @Before
  public void setUp() throws Exception {
    BaseBlueprintProcessor.stackInfo = null;
  }


  @Test
  public void testStackRegisterConditionalDependencies() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo("YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo("TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo("YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo("OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack =
      new Stack("HDP", "2.1", mockMgmtController) {
        @Override
        public Collection<DependencyInfo> getDependenciesForComponent(String component) {
          // simulate the dependencies in a given stack by overriding this method
          if (component.equals("NAGIOS_SERVER")) {
            Set<DependencyInfo> setOfDependencies = new HashSet<DependencyInfo>();

            setOfDependencies.add(hCatDependency);
            setOfDependencies.add(yarnClientDependency);
            setOfDependencies.add(tezClientDependency);
            setOfDependencies.add(mapReduceTwoClientDependency);
            setOfDependencies.add(oozieClientDependency);

            return setOfDependencies;
          }

            return Collections.emptySet();
        }
      };

    assertEquals("Initial conditional dependency map should be empty",
                 0, testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size",
                 5, testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT",
                 "HIVE", testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for YARN_CLIENT",
                 "YARN", testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT",
                 "TEZ", testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals("Incorrect service dependency for MAPREDUCE2_CLIENT",
                 "MAPREDUCE2", testStack.getDependencyConditionalServiceMap().get(mapReduceTwoClientDependency));
    assertEquals("Incorrect service dependency for OOZIE_CLIENT",
                 "OOZIE", testStack.getDependencyConditionalServiceMap().get(oozieClientDependency));

    mockSupport.verifyAll();
  }


  @Test
  public void testStackRegisterConditionalDependenciesNoHCAT() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    // test dependencies
    final DependencyInfo yarnClientDependency = new TestDependencyInfo("YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo("TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo("YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo("OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack =
      new Stack("HDP", "2.1", mockMgmtController) {
        @Override
        public Collection<DependencyInfo> getDependenciesForComponent(String component) {
          // simulate the dependencies in a given stack by overriding this method
          if (component.equals("NAGIOS_SERVER")) {
            Set<DependencyInfo> setOfDependencies = new HashSet<DependencyInfo>();

            setOfDependencies.add(yarnClientDependency);
            setOfDependencies.add(tezClientDependency);
            setOfDependencies.add(mapReduceTwoClientDependency);
            setOfDependencies.add(oozieClientDependency);

            return setOfDependencies;
          }

          return Collections.emptySet();
        }
      };

    assertEquals("Initial conditional dependency map should be empty",
      0, testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size",
      4, testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for YARN_CLIENT",
      "YARN", testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT",
      "TEZ", testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals("Incorrect service dependency for MAPREDUCE2_CLIENT",
      "MAPREDUCE2", testStack.getDependencyConditionalServiceMap().get(mapReduceTwoClientDependency));
    assertEquals("Incorrect service dependency for OOZIE_CLIENT",
      "OOZIE", testStack.getDependencyConditionalServiceMap().get(oozieClientDependency));

    mockSupport.verifyAll();
  }


  @Test
  public void testStackRegisterConditionalDependenciesNoYarnClient() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo("TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo("YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo("OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack =
      new Stack("HDP", "2.1", mockMgmtController) {
        @Override
        public Collection<DependencyInfo> getDependenciesForComponent(String component) {
          // simulate the dependencies in a given stack by overriding this method
          if (component.equals("NAGIOS_SERVER")) {
            Set<DependencyInfo> setOfDependencies = new HashSet<DependencyInfo>();

            setOfDependencies.add(hCatDependency);
            setOfDependencies.add(tezClientDependency);
            setOfDependencies.add(mapReduceTwoClientDependency);
            setOfDependencies.add(oozieClientDependency);

            return setOfDependencies;
          }

          return Collections.emptySet();
        }
      };

    assertEquals("Initial conditional dependency map should be empty",
      0, testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size",
      4, testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT",
      "HIVE", testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT",
      "TEZ", testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals("Incorrect service dependency for MAPREDUCE2_CLIENT",
      "MAPREDUCE2", testStack.getDependencyConditionalServiceMap().get(mapReduceTwoClientDependency));
    assertEquals("Incorrect service dependency for OOZIE_CLIENT",
      "OOZIE", testStack.getDependencyConditionalServiceMap().get(oozieClientDependency));

    mockSupport.verifyAll();
  }


  @Test
  public void testStackRegisterConditionalDependenciesNoTezClient() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo("YARN/YARN_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo("YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo("OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack =
      new Stack("HDP", "2.1", mockMgmtController) {
        @Override
        public Collection<DependencyInfo> getDependenciesForComponent(String component) {
          // simulate the dependencies in a given stack by overriding this method
          if (component.equals("NAGIOS_SERVER")) {
            Set<DependencyInfo> setOfDependencies = new HashSet<DependencyInfo>();

            setOfDependencies.add(hCatDependency);
            setOfDependencies.add(yarnClientDependency);
            setOfDependencies.add(mapReduceTwoClientDependency);
            setOfDependencies.add(oozieClientDependency);

            return setOfDependencies;
          }

          return Collections.emptySet();
        }
      };

    assertEquals("Initial conditional dependency map should be empty",
      0, testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size",
      4, testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT",
      "HIVE", testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for YARN_CLIENT",
      "YARN", testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for MAPREDUCE2_CLIENT",
      "MAPREDUCE2", testStack.getDependencyConditionalServiceMap().get(mapReduceTwoClientDependency));
    assertEquals("Incorrect service dependency for OOZIE_CLIENT",
      "OOZIE", testStack.getDependencyConditionalServiceMap().get(oozieClientDependency));

    mockSupport.verifyAll();
  }


  @Test
  public void testStackRegisterConditionalDependenciesNoMapReduceClient() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo("YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo("TEZ/TEZ_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo("OOZIE/OOZIE_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack =
      new Stack("HDP", "2.1", mockMgmtController) {
        @Override
        public Collection<DependencyInfo> getDependenciesForComponent(String component) {
          // simulate the dependencies in a given stack by overriding this method
          if (component.equals("NAGIOS_SERVER")) {
            Set<DependencyInfo> setOfDependencies = new HashSet<DependencyInfo>();

            setOfDependencies.add(hCatDependency);
            setOfDependencies.add(yarnClientDependency);
            setOfDependencies.add(tezClientDependency);
            setOfDependencies.add(oozieClientDependency);

            return setOfDependencies;
          }

          return Collections.emptySet();
        }
      };

    assertEquals("Initial conditional dependency map should be empty",
      0, testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size",
      4, testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT",
      "HIVE", testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for YARN_CLIENT",
      "YARN", testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT",
      "TEZ", testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals("Incorrect service dependency for OOZIE_CLIENT",
      "OOZIE", testStack.getDependencyConditionalServiceMap().get(oozieClientDependency));

    mockSupport.verifyAll();
  }


  @Test
  public void testStackRegisterConditionalDependenciesNoOozieClient() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();
    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    // setup mock expectations
    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo("YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo("TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo("YARN/MAPREDUCE2_CLIENT");

    mockSupport.replayAll();

    // create stack for testing
    Stack testStack =
      new Stack("HDP", "2.1", mockMgmtController) {
        @Override
        public Collection<DependencyInfo> getDependenciesForComponent(String component) {
          // simulate the dependencies in a given stack by overriding this method
          if (component.equals("NAGIOS_SERVER")) {
            Set<DependencyInfo> setOfDependencies = new HashSet<DependencyInfo>();

            setOfDependencies.add(hCatDependency);
            setOfDependencies.add(yarnClientDependency);
            setOfDependencies.add(tezClientDependency);
            setOfDependencies.add(mapReduceTwoClientDependency);

            return setOfDependencies;
          }

          return Collections.emptySet();
        }
      };

    assertEquals("Initial conditional dependency map should be empty",
      0, testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size",
      4, testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT",
      "HIVE", testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for YARN_CLIENT",
      "YARN", testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT",
      "TEZ", testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals("Incorrect service dependency for MAPREDUCE2_CLIENT",
      "MAPREDUCE2", testStack.getDependencyConditionalServiceMap().get(mapReduceTwoClientDependency));

    mockSupport.verifyAll();
  }


  /**
   * Convenience class for easier setup/initialization of dependencies
   * for unit testing.
   */
  private static class TestDependencyInfo extends DependencyInfo {
    TestDependencyInfo(String dependencyName) {
      setName(dependencyName);
    }
  }
}