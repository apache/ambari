package org.apache.ambari.server.controller.internal;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.StackInfo;
import org.junit.Test;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

@SuppressWarnings("unchecked")
public class BaseBlueprintProcessorTest {

  //todo: Move these tests to the correct location.
  //todo: BaseBluprintProcess no longer exists.
  @Test
  public void testStackRegisterConditionalDependencies() throws Exception {
    StackInfo stackInfo = new StackInfo();

        // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    // create stack for testing
    Stack testStack = new Stack(stackInfo) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 5,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoHCAT() throws Exception {
    // test dependencies
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    // create stack for testing
    Stack testStack = new Stack(new StackInfo()) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoYarnClient()
      throws Exception {
    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    // create stack for testing
    Stack testStack = new Stack(new StackInfo()) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoTezClient()
      throws Exception {
    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    // create stack for testing
    Stack testStack = new Stack(new StackInfo()) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }
    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoMapReduceClient()
      throws Exception {
    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo oozieClientDependency = new TestDependencyInfo(
        "OOZIE/OOZIE_CLIENT");

    // create stack for testing
    Stack testStack = new Stack(new StackInfo()) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(oozieClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }

    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for OOZIE_CLIENT",
        "OOZIE",
        testStack.getDependencyConditionalServiceMap().get(
            oozieClientDependency));
  }

  @Test
  public void testStackRegisterConditionalDependenciesNoOozieClient()
      throws Exception {
    // test dependencies
    final DependencyInfo hCatDependency = new TestDependencyInfo("HIVE/HCAT");
    final DependencyInfo yarnClientDependency = new TestDependencyInfo(
        "YARN/YARN_CLIENT");
    final DependencyInfo tezClientDependency = new TestDependencyInfo(
        "TEZ/TEZ_CLIENT");
    final DependencyInfo mapReduceTwoClientDependency = new TestDependencyInfo(
        "YARN/MAPREDUCE2_CLIENT");

    // create stack for testing
    Stack testStack = new Stack(new StackInfo()) {
      @Override
      public Collection<DependencyInfo> getDependenciesForComponent(
          String component) {
        // simulate the dependencies in a given stack by overriding this method
        if (component.equals("FAKE_MONITORING_SERVER")) {
          Set<DependencyInfo> setOfDependencies = new HashSet<>();

          setOfDependencies.add(hCatDependency);
          setOfDependencies.add(yarnClientDependency);
          setOfDependencies.add(tezClientDependency);
          setOfDependencies.add(mapReduceTwoClientDependency);

          return setOfDependencies;
        }

        return Collections.emptySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void registerConditionalDependencies() {
        // TODO Auto-generated method stub
        super.registerConditionalDependencies();

        Map<DependencyInfo, String> dependencyConditionalServiceMap = getDependencyConditionalServiceMap();
        Collection<DependencyInfo> monitoringDependencies = getDependenciesForComponent("FAKE_MONITORING_SERVER");
        for (DependencyInfo dependency : monitoringDependencies) {
          if (dependency.getComponentName().equals("HCAT")) {
            dependencyConditionalServiceMap.put(dependency, "HIVE");
          } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "OOZIE");
          } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "YARN");
          } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "TEZ");
          } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
            dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
          }
        }
      }

    };

    assertEquals("Initial conditional dependency map should be empty", 0,
        testStack.getDependencyConditionalServiceMap().size());

    testStack.registerConditionalDependencies();

    assertEquals("Set of conditional service mappings is an incorrect size", 4,
        testStack.getDependencyConditionalServiceMap().size());

    assertEquals("Incorrect service dependency for HCAT", "HIVE",
        testStack.getDependencyConditionalServiceMap().get(hCatDependency));
    assertEquals(
        "Incorrect service dependency for YARN_CLIENT",
        "YARN",
        testStack.getDependencyConditionalServiceMap().get(yarnClientDependency));
    assertEquals("Incorrect service dependency for TEZ_CLIENT", "TEZ",
        testStack.getDependencyConditionalServiceMap().get(tezClientDependency));
    assertEquals(
        "Incorrect service dependency for MAPREDUCE2_CLIENT",
        "MAPREDUCE2",
        testStack.getDependencyConditionalServiceMap().get(
            mapReduceTwoClientDependency));
  }

  /**
   * Convenience class for easier setup/initialization of dependencies for unit
   * testing.
   */
  private static class TestDependencyInfo extends DependencyInfo {
    TestDependencyInfo(String dependencyName) {
      setName(dependencyName);
    }
  }
}