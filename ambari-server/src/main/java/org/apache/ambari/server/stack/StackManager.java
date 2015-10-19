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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * Manages all stack related behavior including parsing of stacks and providing access to
 * stack information.
 */
public class StackManager {

  /**
   * Delimiter used for parent path string
   * Example:
   *  HDP/2.0.6/HDFS
   *  common-services/HDFS/2.1.0.2.0
   */
  public static String PATH_DELIMITER = "/";

  /**
   * Prefix used for common services parent path string
   */
  public static final String COMMON_SERVICES = "common-services";

  /**
   * Provides access to non-stack server functionality
   */
  private StackContext stackContext;

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackManager.class);

  /**
   * Map of stack id to stack info
   */
  private Map<String, StackInfo> stackMap = new HashMap<String, StackInfo>();

  /**
   * Constructor. Initialize stack manager.
   *
   * @param stackRoot
   *          stack root directory
   * @param commonServicesRoot
   *          common services root directory
   * @param osFamily
   *          the OS family read from resources
   * @param metaInfoDAO
   *          metainfo DAO automatically injected
   * @param actionMetadata
   *          action meta data automatically injected
   * @param stackDao
   *          stack DAO automatically injected
   *
   * @throws AmbariException
   *           if an exception occurs while processing the stacks
   */
  @Inject
  public StackManager(@Assisted("stackRoot") File stackRoot,
      @Assisted("commonServicesRoot") @Nullable File commonServicesRoot,
      @Assisted OsFamily osFamily, MetainfoDAO metaInfoDAO,
      ActionMetadata actionMetadata, StackDAO stackDao)
      throws AmbariException {

    LOG.info("Initializing the stack manager...");

    validateStackDirectory(stackRoot);
    validateCommonServicesDirectory(commonServicesRoot);

    stackMap = new HashMap<String, StackInfo>();
    stackContext = new StackContext(metaInfoDAO, actionMetadata, osFamily);

    Map<String, ServiceModule> commonServiceModules = parseCommonServicesDirectory(commonServicesRoot);
    Map<String, StackModule> stackModules = parseStackDirectory(stackRoot);

    fullyResolveCommonServices(stackModules, commonServiceModules);
    fullyResolveStacks(stackModules, commonServiceModules);

    // for every stack read in, ensure that we have a database entry for it;
    // don't put try/catch logic around this since a failure here will
    // cause other things to break down the road
    Collection<StackInfo> stacks = getStacks();
    for( StackInfo stack : stacks ){
      String stackName = stack.getName();
      String stackVersion = stack.getVersion();

      if (stackDao.find(stackName, stackVersion) == null) {
        LOG.info("Adding stack {}-{} to the database", stackName, stackVersion);

        StackEntity stackEntity = new StackEntity();
        stackEntity.setStackName(stackName);
        stackEntity.setStackVersion(stackVersion);

        stackDao.create(stackEntity);
      }
    }
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
    return stackMap.get(name + StackManager.PATH_DELIMITER + version);
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
   * @param stackModules          map of stack id which contains name and version to stack module.
   * @param commonServiceModules  map of common service id which contains name and version to stack module.
   * @throws AmbariException if unable to resolve all stacks
   */
  private void fullyResolveStacks(
      Map<String, StackModule> stackModules, Map<String, ServiceModule> commonServiceModules)
      throws AmbariException {
    // Resolve all stacks without finalizing the stacks.
    for (StackModule stack : stackModules.values()) {
      if (stack.getModuleState() == ModuleState.INIT) {
        stack.resolve(null, stackModules, commonServiceModules);
      }
    }
    // Finalize the common services and stacks to remove sub-modules marked for deletion.
    // Finalizing the stacks AFTER all stacks are resolved ensures that the sub-modules marked for deletion are
    // inherited into the child module when explicit parent is defined and thereby ensuring all modules from parent module
    // are inlined into the child module even if the module is marked for deletion.
    for(ServiceModule commonService : commonServiceModules.values()) {
      commonService.finalizeModule();
    }
    for (StackModule stack : stackModules.values()) {
      stack.finalizeModule();
    }
    // Execute all of the repo tasks in a single thread executor
    stackContext.executeRepoTasks();
  }

  /**
   * Fully resolve common services.
   *
   * @param stackModules          map of stack id which contains name and version to stack module.
   * @param commonServiceModules  map of common service id which contains name and version to common service module.
   * @throws AmbariException if unable to resolve all common services
   */
  private void fullyResolveCommonServices(
      Map<String, StackModule> stackModules, Map<String, ServiceModule> commonServiceModules)
      throws AmbariException {
    for(ServiceModule commonService : commonServiceModules.values()) {
      if (commonService.getModuleState() == ModuleState.INIT) {
        commonService.resolveCommonService(stackModules, commonServiceModules);
      }
    }
  }

  /**
   * Validate that the specified common services root is a valid directory.
   *
   * @param commonServicesRoot the common services root directory to validate
   * @throws AmbariException if the specified common services root directory is invalid
   */
  private void validateCommonServicesDirectory(File commonServicesRoot) throws AmbariException {
    if(commonServicesRoot != null) {
      LOG.info("Validating common services directory {} ...",
          commonServicesRoot);

      String commonServicesRootAbsolutePath = commonServicesRoot.getAbsolutePath();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Loading common services information"
            + ", commonServicesRoot = " + commonServicesRootAbsolutePath);
      }

      if (!commonServicesRoot.isDirectory() && !commonServicesRoot.exists()) {
        throw new AmbariException("" + Configuration.COMMON_SERVICES_DIR_PATH
            + " should be a directory with common services"
            + ", commonServicesRoot = " + commonServicesRootAbsolutePath);
      }
    }
  }

  /**
   * Validate that the specified stack root is a valid directory.
   *
   * @param stackRoot  the stack root directory to validate
   * @throws AmbariException if the specified stack root directory is invalid
   */
  private void validateStackDirectory(File stackRoot) throws AmbariException {
    LOG.info("Validating stack directory {} ...", stackRoot);

    String stackRootAbsPath = stackRoot.getAbsolutePath();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stack information"
          + ", stackRoot = " + stackRootAbsPath);
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists()) {
      throw new AmbariException("" + Configuration.METADATA_DIR_PATH
          + " should be a directory with stack"
          + ", stackRoot = " + stackRootAbsPath);
    }
  }

  /**
   * Parse the specified common services root directory
   *
   * @param commonServicesRoot  the common services root directory to parse
   * @return map of common service id which contains name and version to common service module.
   * @throws AmbariException if unable to parse all common services
   */
  private Map<String, ServiceModule> parseCommonServicesDirectory(File commonServicesRoot) throws AmbariException {
    Map<String, ServiceModule> commonServiceModules = new HashMap<String, ServiceModule>();

    if(commonServicesRoot != null) {
      File[] commonServiceFiles = commonServicesRoot.listFiles(AmbariMetaInfo.FILENAME_FILTER);
      for (File commonService : commonServiceFiles) {
        if (commonService.isFile()) {
          continue;
        }
        for (File serviceFolder : commonService.listFiles(AmbariMetaInfo.FILENAME_FILTER)) {
          String serviceName = serviceFolder.getParentFile().getName();
          String serviceVersion = serviceFolder.getName();
          ServiceDirectory serviceDirectory = new CommonServiceDirectory(serviceFolder.getPath());
          ServiceMetainfoXml metaInfoXml = serviceDirectory.getMetaInfoFile();
          if (metaInfoXml != null) {
            if (metaInfoXml.isValid()) {
              for (ServiceInfo serviceInfo : metaInfoXml.getServices()) {
                ServiceModule serviceModule = new ServiceModule(stackContext, serviceInfo, serviceDirectory, true);

                String commonServiceKey = serviceInfo.getName() + StackManager.PATH_DELIMITER + serviceInfo.getVersion();
                commonServiceModules.put(commonServiceKey, serviceModule);
              }
            } else {
              ServiceModule serviceModule = new ServiceModule(stackContext, new ServiceInfo(), serviceDirectory, true);
              serviceModule.setValid(false);
              serviceModule.setErrors(metaInfoXml.getErrors());
              commonServiceModules.put(metaInfoXml.getSchemaVersion(), serviceModule);
              metaInfoXml.setSchemaVersion(null);
            }
          }
        }
      }
    }
    return commonServiceModules;
  }

  /**
   * Parse the specified stack root directory
   *
   * @param stackRoot  the stack root directory to parse
   * @return map of stack id which contains name and version to stack module.
   * @throws AmbariException if unable to parse all stacks
   */
  private Map<String, StackModule> parseStackDirectory(File stackRoot) throws AmbariException {
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

        StackModule stackModule = new StackModule(new StackDirectory(stackFolder.getPath()), stackContext);
        String stackKey = stackName + StackManager.PATH_DELIMITER + stackVersion;
        stackModules.put(stackKey, stackModule);
        stackMap.put(stackKey, stackModule.getModuleInfo());
      }
    }

    if (stackMap.isEmpty()) {
      throw new AmbariException("Unable to find stack definitions under " +
          "stackRoot = " + stackRoot.getAbsolutePath());
    }
    return stackModules;
  }
}
