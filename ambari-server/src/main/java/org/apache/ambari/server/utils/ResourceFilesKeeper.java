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
package org.apache.ambari.server.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceFilesKeeper {
  private static final Logger LOG = LoggerFactory.getLogger
          (ResourceFilesKeeper.class);

  private static final String HOOKS_DIR = "hooks";
  private static final String PACKAGE_DIR = "package";
  private static final String COMMON_SERVICES_DIR = "common-services";
  private static final String EXTENSIONS_DIR = "extensions";
  private static final String CUSTOM_ACTIONS_DIR="custom_actions";
  private static final String HOST_SCRIPTS_DIR="host_scripts";
  private static final String DASHBOARDS_DIR="dashboards";

  private static final String METAINFO_XML = "metainfo.xml";

  private String resourcesDir;
  private String stacksRoot;
  private boolean noZip;
  private boolean verbose;

  private List<String> archive_directories = new ArrayList<String>() {{
    add(HOOKS_DIR);
    add(PACKAGE_DIR);
  }};

  public ResourceFilesKeeper() {
  }

  public void setNoZip(boolean noZip) {
    this.noZip = noZip;
  }

  public void setResourcesDir(String resourcesDir) {
    this.resourcesDir = resourcesDir;
  }

  public void setStacksRoot(String stacksRoot) {
    this.stacksRoot = stacksRoot;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void updateAllDirectoryArchives() {
    List<String> validStacks = getValidStacks();
    iterativeUpdateArchiveDirectories(validStacks);

    String commonServicesRoot = resourcesDir + File.separator + COMMON_SERVICES_DIR;
    List<String> validCommonServices = getValidCommonServices(commonServicesRoot);
    iterativeUpdateArchiveDirectories(validCommonServices);

    String extensionsRoot = resourcesDir + File.separator + EXTENSIONS_DIR;
    List<String> validValidExtensions = getValidExtensions(extensionsRoot);
    iterativeUpdateArchiveDirectories(validValidExtensions);

    updateResourcesSubDirArchive(CUSTOM_ACTIONS_DIR);

    updateResourcesSubDirArchive(HOST_SCRIPTS_DIR);

    updateResourcesSubDirArchive(DASHBOARDS_DIR);
  }

  public List<String> getValidStacks() {
    return getMetainfoDirectories(stacksRoot);
  }

  public List<String> getValidCommonServices(String commonServicesRoot) {
    return getMetainfoDirectories(commonServicesRoot);
  }

  public List<String> getValidExtensions(String extensionsRoot) {
    return getMetainfoDirectories(extensionsRoot);
  }

  public void updateResourcesSubDirArchive(String subDir) {
    String fullPath = resourcesDir + File.separator + subDir;
    File fullPathFile = new File(fullPath);
    if (fullPathFile.exists() && fullPathFile.isDirectory()) {
      ResourceFilesKeeperHelper.updateDirectoryArchive(fullPath, noZip);
    }
  }

  public void iterativeUpdateArchiveDirectories(List<String> subDirsList) {
    for (String subDir : subDirsList) {
      File[] serviceFiles = new File(subDir).listFiles();
      for (File serviceFile : serviceFiles) {
        if (serviceFile.isDirectory()) {
          if (archive_directories.contains(serviceFile.getName())) {
            ResourceFilesKeeperHelper.updateDirectoryArchive(serviceFile.getPath(), noZip);
          }
        }
      }
    }
  }

  public List<String> getMetainfoDirectories(String rootDir) {
    List<String> validItems = new ArrayList<>();
    List<File> metainfoDirectories = new ArrayList<>();
    File rootStackDir = new File(rootDir);

    if (!rootStackDir.exists() || !rootStackDir.isDirectory()) {
      return new ArrayList<>();
    }

    for (File file : rootStackDir.listFiles()) {
      if (file.isDirectory()) {
        for (File stackFile : file.listFiles()) {
          if (stackFile.isDirectory()) {
            metainfoDirectories.add(stackFile);
          }
        }
      }
    }

    for (File stackDir : metainfoDirectories) {
      String metainfoPath = stackDir.getPath() + File.separator + METAINFO_XML;
      if (new File(metainfoPath).exists()) {
        validItems.add(stackDir.getPath());
      }
    }
    return validItems;
  }


  /*
  * Main method from which we are calling all checks
  * */
  public static void main(String[] args) throws Exception {
    ResourceFilesKeeper resourceFilesKeeper = null;
    try {

      resourceFilesKeeper = new ResourceFilesKeeper();
      resourceFilesKeeper.setResourcesDir(args[0]);
      resourceFilesKeeper.setStacksRoot(args[1]);
      resourceFilesKeeper.setNoZip(Boolean.parseBoolean(args[2]));
      //resourceFilesKeeperHelper.setVerbose(Boolean.parseBoolean(args[3]));

      System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA!!!!");

      resourceFilesKeeper.updateAllDirectoryArchives();

    } catch (Throwable e) {
     if (e instanceof AmbariException) {
        LOG.error("Exception occurred during updating archives:", e);
        throw (AmbariException)e;
      } else {
        LOG.error("Unexpected error, updating archives failed", e);
        throw new Exception("Unexpected error, updating archives failed", e);
      }
    }
  }


}

