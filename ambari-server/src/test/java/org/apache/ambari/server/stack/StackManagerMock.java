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

package org.apache.ambari.server.stack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Directory tree rescans and stack modules parsing take much time on every module init.
 * This class enhances {@code}StackManager{@code} to reuse already parsed modules.
 */
public class StackManagerMock extends StackManager {

  // Ensure correct behavior during the parallel test execution.
  private static final Lock lock = new ReentrantLock();

  // Some tests use different stack locations.
  private static final Map<ModulesPathsKey, CachedModules> pathsToCachedModulesMap = new HashMap<>();

  /**
   * Constructor. Initialize stack manager.
   *
   * @param resourcesRoot      resources root directory
   * @param stackRoot          stack root directory
   * @param commonServicesRoot common services root directory
   * @param extensionRoot      extensions root directory
   * @param osFamily           the OS family read from resources
   * @param validate           validate all stack and service definitions
   * @param refreshArchives    refresh archive.zip and .hash
   * @param metaInfoDAO        metainfo DAO automatically injected
   * @param actionMetadata     action meta data automatically injected
   * @param stackDao           stack DAO automatically injected
   * @param extensionDao       extension DAO automatically injected
   * @param linkDao            extension link DAO automatically injected
   * @param helper             Ambari management helper automatically injected
   * @throws AmbariException if an exception occurs while processing the stacks
   */
  @AssistedInject
  public StackManagerMock(
    @Assisted("resourcesRoot") final File resourcesRoot,
    @Assisted("stackRoot") final File stackRoot,
    @Assisted("commonServicesRoot") @Nullable final File commonServicesRoot,
    @Assisted("extensionRoot") @Nullable final File extensionRoot,
    @Assisted final OsFamily osFamily,
    @Assisted("validate") final boolean validate,
    @Assisted("refreshArchives") final boolean refreshArchives,
    final MetainfoDAO metaInfoDAO,
    final ActionMetadata actionMetadata,
    final StackDAO stackDao,
    final ExtensionDAO extensionDao,
    final ExtensionLinkDAO linkDao, final AmbariManagementHelper helper) throws AmbariException {
    super(resourcesRoot, stackRoot, commonServicesRoot, extensionRoot, osFamily, validate, refreshArchives, metaInfoDAO, actionMetadata, stackDao, extensionDao, linkDao, helper);
    currentStackRoot = stackRoot;
    currentCommonServicesRoot = commonServicesRoot;
    currentExtensionRoot = extensionRoot;
  }

  public static void invalidateKey(File stackRoot, File commonServicesRoot, File extensionRoot) {
    ModulesPathsKey pathsKey = new ModulesPathsKey(stackRoot, commonServicesRoot, extensionRoot);
    pathsToCachedModulesMap.remove(pathsKey);
  }

  // Paths for this instance.
  private File currentStackRoot;
  private File currentCommonServicesRoot;
  private File currentExtensionRoot;

  public void invalidateCurrentPaths() {
    invalidateKey(currentStackRoot, currentCommonServicesRoot, currentExtensionRoot);
  }

  private static class ModulesPathsKey {
    private String stackRoot;
    private String commonServicesRoot;
    private String extensionRoot;

    public ModulesPathsKey(File stackRoot, File commonServicesRoot, File extensionRoot) {
      this.stackRoot = stackRoot == null ? "" : stackRoot.getPath();
      this.commonServicesRoot = commonServicesRoot == null ? "" : commonServicesRoot.getPath();
      this.extensionRoot = extensionRoot == null ? "" : extensionRoot.getPath();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ModulesPathsKey that = (ModulesPathsKey) o;

      if (stackRoot != null ? !stackRoot.equals(that.stackRoot) : that.stackRoot != null) {
        return false;
      }
      if (commonServicesRoot != null ? !commonServicesRoot.equals(that.commonServicesRoot) : that.commonServicesRoot != null) {
        return false;
      }
      return !(extensionRoot != null ? !extensionRoot.equals(that.extensionRoot) : that.extensionRoot != null);

    }

    @Override
    public int hashCode() {
      int result = stackRoot != null ? stackRoot.hashCode() : 0;
      result = 31 * result + (commonServicesRoot != null ? commonServicesRoot.hashCode() : 0);
      result = 31 * result + (extensionRoot != null ? extensionRoot.hashCode() : 0);
      return result;
    }
  }

  private static class CachedModules {
    private Map<String, ServiceModule> cachedCommonServiceModules;
    private Map<String, StackModule> cachedStackModules;
    private Map<String, ExtensionModule> cachedExtensionModules;
    private NavigableMap<String, StackInfo> cachedStackMap;

    public CachedModules(Map<String, ServiceModule> cachedCommonServiceModules, Map<String, StackModule> cachedStackModules,
                         Map<String, ExtensionModule> cachedExtensionModules, NavigableMap<String, StackInfo> cachedStackMap) {
      this.cachedCommonServiceModules = cachedCommonServiceModules;
      this.cachedStackModules = cachedStackModules;
      this.cachedExtensionModules = cachedExtensionModules;
      this.cachedStackMap = cachedStackMap;
    }

    public Map<String, ServiceModule> getCachedCommonServiceModules() {
      return cachedCommonServiceModules;
    }

    public Map<String, StackModule> getCachedStackModules() {
      return cachedStackModules;
    }

    public Map<String, ExtensionModule> getCachedExtensionModules() {
      return cachedExtensionModules;
    }

    public NavigableMap<String, StackInfo> getCachedStackMap() {
      return cachedStackMap;
    }
  }

  @Override
  protected void updateArchives(
    File resourcesRoot, File stackRoot, Map<String, StackModule> stackModules, Map<String, ServiceModule> commonServiceModules,
    Map<String, ExtensionModule> extensionModules ) throws AmbariException {
    /*
     * Note: Skip refreshing archives
     */
  }

  @Override
  protected void parseDirectories(File stackRoot, File commonServicesRoot, File extensionRoot) throws AmbariException {
    try {
      // Ensure correct behavior during the parallel test execution.
      lock.lock();

      ModulesPathsKey pathsKey = new ModulesPathsKey(stackRoot, commonServicesRoot, extensionRoot);
      CachedModules cachedModules = pathsToCachedModulesMap.get(pathsKey);

      if (cachedModules == null) {
        super.parseDirectories(stackRoot, commonServicesRoot, extensionRoot);
        CachedModules newEntry = new CachedModules(commonServiceModules, stackModules, extensionModules, stackMap);
        pathsToCachedModulesMap.put(pathsKey, newEntry);
      } else {
        commonServiceModules = cachedModules.getCachedCommonServiceModules();
        stackModules = cachedModules.getCachedStackModules();
        extensionModules = cachedModules.getCachedExtensionModules();
        stackMap = cachedModules.getCachedStackMap();
      }
    } finally {
      lock.unlock();
    }
  }
}
