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
package org.apache.ambari.server.state.stack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider.ConfigurationPackBuilder;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.stack.ModuleFileUnmarshaller;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping.ExecuteStage;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.Task.Type;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

/**
 * Tests that for every upgrade pack found, that all referenced configuration
 * IDs exist in the {@code config-upgrade.xml} which will be used/created. Also
 * ensures that every XML file is valid against its XSD.
 */
@Category({ category.StackUpgradeTest.class})
public class ConfigUpgradeValidityTest {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigUpgradeValidityTest.class);

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;

  private int validatedConfigCount = 0;

  @Before
  public void before() throws Exception {
    validatedConfigCount = 0;

    // ensure that we use the real stacks
    InMemoryDefaultTestModule testModule = new InMemoryDefaultTestModule();
    testModule.getProperties().put(Configuration.METADATA_DIR_PATH.getKey(),
        "src/main/resources/stacks");

    injector = Guice.createInjector(testModule);
    injector.getInstance(GuiceJpaInitializer.class);

    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * Tests that every ID specified in an upgrade pack actually exists in the
   * created {@link ConfigUpgradePack}.
   *
   * @throws Exception
   */
  @Test
  public void testConfigurationDefinitionsExist() throws Exception {
    Collection<StackInfo> stacks = ambariMetaInfo.getStacks();
    Assert.assertFalse(stacks.isEmpty());

    for( StackInfo stack : stacks ){
      if (!stack.isActive()) {
        LOG.info("Skipping configuration validity test for {}", new StackId(stack));
        continue;
      }

      Map<String, UpgradePack> upgradePacks = ambariMetaInfo.getUpgradePacks(stack.getName(), stack.getVersion());
      for (String key : upgradePacks.keySet()) {
        UpgradePack upgradePack = upgradePacks.get(key);
        StackId sourceStack = new StackId(stack);

        ConfigUpgradePack configUpgradePack = ConfigurationPackBuilder.build(upgradePack,
            sourceStack);

        // do configure tasks in the group section
        List<Grouping> groups = upgradePack.getGroups(Direction.UPGRADE);
        for( Grouping group : groups ){
          if( group instanceof ClusterGrouping ){
            ClusterGrouping clusterGrouping = (ClusterGrouping)group;
            if( null != clusterGrouping.executionStages ){
              for( ExecuteStage executionStage : clusterGrouping.executionStages ){
                if( executionStage.task.getType() == Type.CONFIGURE ){
                  ConfigureTask configureTask = (ConfigureTask) executionStage.task;
                  assertIdDefinitionExists(configureTask.id, configUpgradePack, upgradePack,
                      sourceStack);
                }
              }
            }
          }
        }

        // do tasks in the "processing" section
        Map<String, Map<String, ProcessingComponent>> tasks = upgradePack.getTasks();
        for( Map<String,ProcessingComponent> value : tasks.values() ){
          for( ProcessingComponent processingComponent : value.values() ){
            if (null != processingComponent.preTasks) {
              for (Task preTask : processingComponent.preTasks) {
                if (preTask.getType() == Type.CONFIGURE) {
                  ConfigureTask configureTask = (ConfigureTask) preTask;
                  assertIdDefinitionExists(configureTask.id, configUpgradePack, upgradePack,
                      sourceStack);
                }
              }

              if (null != processingComponent.tasks) {
                for (Task task : processingComponent.tasks) {
                  if (task.getType() == Type.CONFIGURE) {
                    ConfigureTask configureTask = (ConfigureTask) task;
                    assertIdDefinitionExists(configureTask.id, configUpgradePack, upgradePack,
                        sourceStack);
                  }
                }
              }

              if (null != processingComponent.postTasks) {
                for (Task postTask : processingComponent.postTasks) {
                  if (postTask.getType() == Type.CONFIGURE) {
                    ConfigureTask configureTask = (ConfigureTask) postTask;
                    assertIdDefinitionExists(configureTask.id, configUpgradePack, upgradePack,
                        sourceStack);
                  }
                }
              }
            }
          }
        }
      }
    }

    // make sure we actually checked a bunch of configs :)
    Assert.assertTrue(validatedConfigCount > 100);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testValidateConfigUpgradePacks() throws Exception {
    IOFileFilter filter = new IOFileFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return false;
      }

      @Override
      public boolean accept(File file) {
        // file has the folder named 'upgrades', ends with '.xml' and is NOT
        // 'config-upgrade.xml'
        if (file.getAbsolutePath().contains("upgrades")
            && file.getAbsolutePath().endsWith("config-upgrade.xml")) {
          return true;
        }

        return false;
      }
    };

    List<File> files = new ArrayList<>();

    files.addAll(FileUtils.listFiles(new File("src/main/resources/stacks"), filter,
        FileFilterUtils.directoryFileFilter()));

    files.addAll(FileUtils.listFiles(new File("src/test/resources/stacks"), filter,
        FileFilterUtils.directoryFileFilter()));

    files.addAll(FileUtils.listFiles(new File("src/test/resources/stacks_with_upgrade_cycle"),
        filter, FileFilterUtils.directoryFileFilter()));

    ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

    int filesTestedCount = 0;
    for (File file : files) {
      String fileContent = FileUtils.readFileToString(file, "UTF-8");

      // these things must be in upgrade packs for them to work anyway
      if (fileContent.contains("<upgrade-config-changes")
          && fileContent.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
        if (!fileContent.contains("xsi:noNamespaceSchemaLocation=\"upgrade-config.xsd\"")) {
          String msg = String.format(
              "File %s appears to be a config upgrade pack, but does not define 'upgrade-config.xsd' as its schema",
              file.getAbsolutePath());
          Assert.fail(msg);
        } else {
          filesTestedCount++;
          unmarshaller.unmarshal(ConfigUpgradePack.class, file, true);
        }
      }
    }

    Assert.assertTrue(
        "This test didn't appear to do any work which could indicate that it failed to find files to validate",
        filesTestedCount > 5);
  }

  /**
   * Asserts that an ID exists in a {@link ConfigUpgradePack}, throwing an
   * informative message if it does not.
   *
   * @param id
   * @param configUpgradePack
   * @param upgradePack
   * @param sourceStackId
   */
  private void assertIdDefinitionExists(String id, ConfigUpgradePack configUpgradePack,
      UpgradePack upgradePack, StackId sourceStackId) {
    Assert.assertNotNull(id);

    if (configUpgradePack.enumerateConfigChangesByID().containsKey(id)) {
      validatedConfigCount++;

      LOG.info("Validated {} from upgrade pack {} for {}", id, upgradePack.getTargetStack(),
          sourceStackId);

      return;
    }

    Assert.fail(String.format("Missing %s in upgrade from %s to %s (%s)", id, sourceStackId,
        upgradePack.getTargetStack(), upgradePack.getType()));
  }
}
