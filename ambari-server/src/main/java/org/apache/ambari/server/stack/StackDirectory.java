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
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates IO operations on a stack definition stack directory.
 */
//todo: Normalize all path return values.
//todo: Currently some are relative and some are absolute.
//todo: Current values were dictated by the StackInfo expectations.
public class StackDirectory extends StackDefinitionDirectory {
  /**
   * hooks directory path
   */
  private String hooksDir;

  /**
   * upgrades directory path
   */
  private String upgradesDir;

  /**
   * rco file path
   */
  private String rcoFilePath;

  /**
   * kerberos descriptor file path
   */
  private String kerberosDescriptorFilePath;

  /**
   * kerberos descriptor file path
   */
  private String widgetsDescriptorFilePath;

  /**
   * repository file
   */
  private RepositoryXml repoFile;

  /**
   * role command order
   */
  private StackRoleCommandOrder roleCommandOrder;

  /**
   * repository directory
   */
  private String repoDir;

  /**
   * collection of service directories
   */
  private Collection<ServiceDirectory> serviceDirectories;

  /**
   * map of upgrade pack name to upgrade pack
   */
  private Map<String, UpgradePack> upgradePacks;

  /**
   * Config delta from prev stack
   */
  private ConfigUpgradePack configUpgradePack;

  /**
   * metainfo file representation
   */
  private StackMetainfoXml metaInfoXml;

  /**
   * file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * name of the hooks directory
   */
  public static final String HOOKS_FOLDER_NAME = "hooks";

  /**
   * repository directory name
   */
  private final static String REPOSITORY_FOLDER_NAME = "repos";

  /**
   * repository file name
   */
  private final static String REPOSITORY_FILE_NAME = "repoinfo.xml";

  /**
   * metainfo file name
   */
  private static final String STACK_METAINFO_FILE_NAME = "metainfo.xml";

  /**
   * upgrades directory name
   */
  private static final String UPGRADE_PACK_FOLDER_NAME = "upgrades";

  /**
   * role command order file name
   */
  private static final String ROLE_COMMAND_ORDER_FILE = "role_command_order.json";

  /**
   * logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackDirectory.class);


  /**
   * Constructor.
   *
   * @param directory  stack directory
   * @throws AmbariException if unable to parse the stack directory
   */
  public StackDirectory(String directory) throws AmbariException {
    super(directory);
    parsePath();
  }

  /**
   * Obtain the stack directory name.
   *
   * @return stack directory name
   */
  public String getStackDirName() {
    return getDirectory().getParentFile().getName();
  }

  /**
   * Obtain the hooks directory path.
   *
   * @return hooks directory path
   */
  public String getHooksDir() {
    return hooksDir;
  }

  /**
   * Obtain the upgrades directory path.
   *
   * @return upgrades directory path
   */
  public String getUpgradesDir() {
    return upgradesDir;
  }

  /**
   * Obtain the rco file path.
   *
   * @return rco file path
   */
  public String getRcoFilePath() {
    return rcoFilePath;
  }

  /**
   * Obtain the path to the (stack-level) Kerberos descriptor file
   *
   * @return the path to the (stack-level) Kerberos descriptor file
   */
  public String getKerberosDescriptorFilePath() {
    return kerberosDescriptorFilePath;
  }

  /**
   * Obtain the path to the (stack-level) widgets descriptor file
   *
   * @return the path to the (stack-level) widgets descriptor file
   */
  public String getWidgetsDescriptorFilePath() {
    return widgetsDescriptorFilePath;
  }

  /**
   * Obtain the repository directory path.
   *
   * @return repository directory path
   */
  public String getRepoDir() {
    return repoDir;
  }

  /**
   * Obtain the repository file object representation.
   *
   * @return repository file object representation
   */
  public RepositoryXml getRepoFile() {
    return repoFile;
  }

  /**
   * Obtain the object representation of the stack metainfo.xml file.
   *
   * @return object representation of the stack metainfo.xml file
   */
  public StackMetainfoXml getMetaInfoFile() {
    return metaInfoXml;
  }

  /**
   * Obtain a collection of all service directories.
   *
   * @return collection of all service directories
   */
  public Collection<ServiceDirectory> getServiceDirectories() {
    return serviceDirectories;
  }

  /**
   * Obtain a map of all upgrade packs.
   *
   * @return map of upgrade pack name to upgrade pack or null if no packs available
   */
  public Map<String, UpgradePack> getUpgradePacks() {
    return upgradePacks;
  }

  /**
   * @return Config delta from prev stack or null if no config upgrade patches available
   */
  public ConfigUpgradePack getConfigUpgradePack() {
    return configUpgradePack;
  }

  /**
   * Obtain the object representation of the stack role_command_order.json file
   *
   * @return object representation of the stack role_command_order.json file
   */

  public StackRoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrder;
  }

  /**
   * Parse the stack directory.
   *
   * @throws AmbariException if unable to parse the directory
   */
  private void parsePath() throws AmbariException {
    Collection<String> subDirs = Arrays.asList(directory.list());
    if (subDirs.contains(HOOKS_FOLDER_NAME)) {
      // hooksDir is expected to be relative to stack root
      hooksDir = getStackDirName() + File.separator + getName() +
          File.separator + HOOKS_FOLDER_NAME;
    } else {
      LOG.debug("Hooks folder " + getAbsolutePath() + File.separator +
          HOOKS_FOLDER_NAME + " does not exist");
    }

    if (subDirs.contains(AmbariMetaInfo.RCO_FILE_NAME)) {
      // rcoFile is expected to be absolute
      rcoFilePath = getAbsolutePath() + File.separator + AmbariMetaInfo.RCO_FILE_NAME;
    }


    if (subDirs.contains(AmbariMetaInfo.KERBEROS_DESCRIPTOR_FILE_NAME)) {
      // kerberosDescriptorFilePath is expected to be absolute
      kerberosDescriptorFilePath = getAbsolutePath() + File.separator + AmbariMetaInfo.KERBEROS_DESCRIPTOR_FILE_NAME;
    }

    if (subDirs.contains(AmbariMetaInfo.WIDGETS_DESCRIPTOR_FILE_NAME)) {
      widgetsDescriptorFilePath = getAbsolutePath() + File.separator + AmbariMetaInfo.WIDGETS_DESCRIPTOR_FILE_NAME;
    }

    parseUpgradePacks(subDirs);
    parseServiceDirectories(subDirs);
    parseRepoFile(subDirs);
    parseMetaInfoFile();
    parseRoleCommandOrder();
  }

  /**
   * Parse the repository file.
   *
   * @param subDirs stack directory sub directories
   * @throws AmbariException if unable to parse the repository file
   */
  private void parseRepoFile(Collection<String> subDirs) throws AmbariException {
    File repositoryFile;

    if (subDirs.contains(REPOSITORY_FOLDER_NAME)) {
      repoDir = getAbsolutePath() + File.separator + REPOSITORY_FOLDER_NAME;
      repositoryFile = new File(getPath()+ File.separator +
          REPOSITORY_FOLDER_NAME + File.separator + REPOSITORY_FILE_NAME);

      if (repositoryFile.exists()) {
        try {
          repoFile = unmarshaller.unmarshal(RepositoryXml.class, repositoryFile);
        } catch (JAXBException e) {
          repoFile = new RepositoryXml();
          repoFile.setValid(false);
          String msg = "Unable to parse repo file at location: " +
                       repositoryFile.getAbsolutePath();
          repoFile.setErrors(msg);
          LOG.warn(msg);
        }
      }
    }

    if (repoFile == null || !repoFile.isValid()) {
      LOG.warn("No repository information defined for "
          + ", stackName=" + getStackDirName()
          + ", stackVersion=" + getPath()
          + ", repoFolder=" + getPath() + File.separator + REPOSITORY_FOLDER_NAME);
    }
  }

  /**
   * Parse the stack metainfo file.
   *
   * @throws AmbariException if unable to parse the stack metainfo file
   */
  private void parseMetaInfoFile() throws AmbariException {
    File stackMetaInfoFile = new File(getAbsolutePath()
        + File.separator + STACK_METAINFO_FILE_NAME);

    //todo: is it ok for this file not to exist?
    if (stackMetaInfoFile.exists()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reading stack version metainfo from file " + stackMetaInfoFile.getAbsolutePath());
      }

      try {
        metaInfoXml = unmarshaller.unmarshal(StackMetainfoXml.class, stackMetaInfoFile);
      } catch (JAXBException e) {
        metaInfoXml = new StackMetainfoXml();
        metaInfoXml.setValid(false);
        String msg = "Unable to parse stack metainfo.xml file at location: " +
                     stackMetaInfoFile.getAbsolutePath();
        metaInfoXml.setErrors(msg);
        LOG.warn(msg);
      }
    }
  }

  /**
   * Parse the stacks service directories.
   *
   * @param subDirs  stack sub directories
   * @throws AmbariException  if unable to parse the service directories
   */
  private void parseServiceDirectories(Collection<String> subDirs) throws AmbariException {
    Collection<ServiceDirectory> dirs = new HashSet<ServiceDirectory>();

    if (subDirs.contains(ServiceDirectory.SERVICES_FOLDER_NAME)) {
      String servicesDir = getAbsolutePath() + File.separator + ServiceDirectory.SERVICES_FOLDER_NAME;
      File baseServiceDir = new File(servicesDir);
      File[] serviceFolders = baseServiceDir.listFiles(AmbariMetaInfo.FILENAME_FILTER);
      if (serviceFolders != null) {
        for (File d : serviceFolders) {
          if (d.isDirectory()) {
            try {
              dirs.add(new StackServiceDirectory(d.getAbsolutePath()));
            } catch (AmbariException e) {
              //todo: this seems as though we should propagate this exception
              //todo: eating it now to keep backwards compatibility
              LOG.warn(String.format("Unable to parse stack definition service at '%s'.  Ignoring service. : %s",
                  d.getAbsolutePath(), e.toString()));
            }
          }
        }
      }
    }

    if (dirs.isEmpty()) {
      //todo: what does it mean for a stack to have no services?
      LOG.info("The stack defined at '" + getAbsolutePath() + "' contains no services");
    }
    serviceDirectories = dirs;
  }

  /**
   * Parse all stack upgrade files for the stack.
   *
   * @param subDirs  stack sub directories
   * @throws AmbariException if unable to parse stack upgrade file
   */
  private void parseUpgradePacks(Collection<String> subDirs) throws AmbariException {
    Map<String, UpgradePack> upgradeMap = new HashMap<>();
    ConfigUpgradePack configUpgradePack = null;
    if (subDirs.contains(UPGRADE_PACK_FOLDER_NAME)) {
      File f = new File(getAbsolutePath() + File.separator + UPGRADE_PACK_FOLDER_NAME);
      if (f.isDirectory()) {
        upgradesDir = f.getAbsolutePath();
        for (File upgradeFile : f.listFiles(XML_FILENAME_FILTER)) {
          if (upgradeFile.getName().toLowerCase().startsWith(CONFIG_UPGRADE_XML_FILENAME_PREFIX)) {
            try { // Parse config upgrade pack
              if (configUpgradePack == null) {
                configUpgradePack = unmarshaller.unmarshal(ConfigUpgradePack.class, upgradeFile);
              } else { // If user messed things up with lower/upper case filenames
                throw new AmbariException(String.format("There are multiple files with name like %s" +
                        upgradeFile.getAbsolutePath()));
              }
            } catch (JAXBException e) {
              throw new AmbariException("Unable to parse stack upgrade file at location: " +
                      upgradeFile.getAbsolutePath(), e);
            }
          } else {
            try {
              String upgradePackName = FilenameUtils.removeExtension(upgradeFile.getName());
              UpgradePack pack = unmarshaller.unmarshal(UpgradePack.class, upgradeFile);
              pack.setName(upgradePackName);
              upgradeMap.put(upgradePackName, pack);
            } catch (JAXBException e) {
              throw new AmbariException("Unable to parse stack upgrade file at location: " +
                      upgradeFile.getAbsolutePath(), e);
            }
          }
        }
      }
    }

    if (upgradesDir == null) {
      LOG.info("Stack '{}' doesn't contain an upgrade directory ", getPath());
    }

    if (! upgradeMap.isEmpty()) {
      upgradePacks = upgradeMap;
    }

    if (configUpgradePack != null) {
      this.configUpgradePack = configUpgradePack;
    } else {
      LOG.info("Stack '{}' doesn't contain config upgrade pack file", getPath());
    }

  }

  /**
   * Parse role command order file
   */
  private void parseRoleCommandOrder() {
    HashMap<String, Object> result = null;
    ObjectMapper mapper = new ObjectMapper();
    try {
      TypeReference<Map<String, Object>> rcoElementTypeReference = new TypeReference<Map<String, Object>>() {};
      if (rcoFilePath != null) {
        File file = new File(rcoFilePath);
        result = mapper.readValue(file, rcoElementTypeReference);
        LOG.info("Role command order info was loaded from file: {}", file.getAbsolutePath());
      } else {
        LOG.info("Stack '{}' doesn't contain role command order file", getPath());
        result = new HashMap<String, Object>();
      }
      roleCommandOrder = new StackRoleCommandOrder(result);
      parseRoleCommandOrdersForServices();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Role Command Order for " + rcoFilePath);
        roleCommandOrder.printRoleCommandOrder(LOG);
      }
    } catch (IOException e) {
      LOG.error(String.format("Can not read role command order info %s", rcoFilePath), e);
    }
  }

  private void parseRoleCommandOrdersForServices() {
    if (rcoFilePath != null) {
      File stack = new File(rcoFilePath).getParentFile();
      File servicesDir = new File(stack, "services");
      File[] services = servicesDir.listFiles();
      for (File service : services) {
        if (service.isDirectory()) {
          File rcoFile = new File(service, ROLE_COMMAND_ORDER_FILE);
          if (rcoFile.exists())
            parseRoleCommandOrdersForService(rcoFile);
        }
      }
    }
  }

  private void parseRoleCommandOrdersForService(File rcoFile) {
    HashMap<String, Object> result = null;
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<Map<String, Object>> rcoElementTypeReference = new TypeReference<Map<String, Object>>() {};
    try {
      result = mapper.readValue(rcoFile, rcoElementTypeReference);
      LOG.info("Role command order info was loaded from file: {}", rcoFile.getAbsolutePath());
      StackRoleCommandOrder serviceRoleCommandOrder = new StackRoleCommandOrder(result);
      roleCommandOrder.merge(serviceRoleCommandOrder, true);
    } catch (IOException e) {
      LOG.error(String.format("Can not read role command order info %s", rcoFile), e);
    }
  }

}
