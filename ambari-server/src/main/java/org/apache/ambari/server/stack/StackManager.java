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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * Manages all stack related behavior including parsing of stacks and providing access to
 * stack information.
 */
public class StackManager {
  /**
   * Provides access to non-stack server functionality
   */
  private StackContext stackContext;

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackManager.class);

  /**
   * Map of stack name to stack info
   */
  private Map<String, StackInfo> stackMap = new HashMap<String, StackInfo>();


  /**
   * Constructor.
   *
   * @param stackRoot     stack root directory
   * @param stackContext  context which provides external functionality
   *
   * @throws AmbariException if an exception occurs while processing the stacks
   */
  public StackManager(File stackRoot, StackContext stackContext) throws AmbariException {
    validateStackDirectory(stackRoot);

    this.stackContext = stackContext;
    Map<String, StackModule> stackModules = new HashMap<String, StackModule>();
    File[] stackFiles = stackRoot.listFiles(AmbariMetaInfo.FILENAME_FILTER);
    for (File stack : stackFiles) {
      if (stack.isFile()) {
        continue;
      }
      for (File stackFolder : stack.listFiles(AmbariMetaInfo.FILENAME_FILTER)) {
        if (stackFolder.isFile()) {
          continue;
        }
        String stackName = stackFolder.getParentFile().getName();
        String stackVersion = stackFolder.getName();

        StackModule stackModule = new StackModule(new StackDirectory(stackFolder.getPath()),stackContext);
        stackModules.put(stackName + stackVersion, stackModule);
        stackMap.put(stackName + stackVersion, stackModule.getModuleInfo());
      }
    }

    if (stackMap.isEmpty()) {
      throw new AmbariException("Unable to find stack definitions under " +
          "stackRoot = " + stackRoot.getAbsolutePath());
    }

    fullyResolveStacks(stackModules);
  }

  /**
   * Obtain the stack info specified by name and version.
   *
   * @param name     name of the stack
   * @param version  version of the stack
   * @return The stack corresponding to the specified name and version.
   *         If no matching stack exists, null is returned.
   */
  public StackInfo getStack(String name, String version) {
    return stackMap.get(name + version);
  }

  /**
   * Obtain all stacks for the given name.
   *
   * @param name  stack name
   * @return A collection of all stacks with the given name.
   *         If no stacks match the specified name, an empty collection is returned.
   */
  public Collection<StackInfo> getStacks(String name) {
    Collection<StackInfo> stacks = new HashSet<StackInfo>();
    for (StackInfo stack: stackMap.values()) {
      if (stack.getName().equals(name)) {
        stacks.add(stack);
      }
    }
    return stacks;
  }

  /**
   * Obtain all stacks.
   *
   * @return collection of all stacks
   */
  public Collection<StackInfo> getStacks() {
    return stackMap.values();
  }

  /**
   * Determine if all tasks which update stack repo urls have completed.
   *
   * @return true if all of the repo update tasks have completed; false otherwise
   */
  public boolean haveAllRepoUrlsBeenResolved() {
    return stackContext.haveAllRepoTasksCompleted();
  }

  /**
   * Fully resolve all stacks.
   *
   * @param stackModules  map of stack id which contains name and version to stack module.
   * @throws AmbariException if unable to resolve all stacks
   */
  private void fullyResolveStacks(Map<String, StackModule> stackModules) throws AmbariException {
    for (StackModule stack : stackModules.values()) {
      if (stack.getResolutionState() == StackModule.State.INIT) {
        stack.resolve(null, stackModules);
      }
    }
    // execute all of the repo tasks in a single thread executor
    stackContext.executeRepoTasks();
  }

  /**
   * Validate that the specified stack root is a valid directory.
   * @param stackRoot  the stack root directory to validate
   * @throws AmbariException if the specified stack root directory is invalid
   */
  private void validateStackDirectory(File stackRoot) throws AmbariException {
    String stackRootAbsPath = stackRoot.getAbsolutePath();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stack information"
          + ", stackRoot = " + stackRootAbsPath);
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists())
      throw new AmbariException("" + Configuration.METADETA_DIR_PATH
          + " should be a directory with stack"
          + ", stackRoot = " + stackRootAbsPath);
  }
}
