/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.util;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.types.FileSet;

public class FileUtil {
  private static final Logger logger = Logger.getLogger(FileUtil.class);

  private FileUtil() {
    throw new UnsupportedOperationException();
  }

  public static File getFileFromClasspath(String filename) {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader().getResource(filename);
    logger.debug("File Complete URI :" + fileCompleteUrl);
    File file = null;
    try {
      file = new File(fileCompleteUrl.toURI());
    } catch (Exception exception) {
      logger.debug(exception.getMessage(), exception.getCause());
    }
    return file;
  }

  public static void createDirectory(String dirPath) {
    File dir = new File(dirPath);
    if (!dir.exists()) {
      logger.info("Directory " + dirPath + " does not exist. Creating ...");
      boolean mkDirSuccess = dir.mkdirs();
      if (!mkDirSuccess) {
        String errorMessage = String.format("Could not create directory %s", dirPath);
        logger.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    }
  }

  public static void setPermissionOnDirectory(String dirPath, String permission) {
    Chmod chmod = new Chmod();
    chmod.setProject(new Project());
    FileSet fileSet = new FileSet();
    fileSet.setDir(new File(dirPath));
    fileSet.setIncludes("**");
    chmod.addFileset(fileSet);
    chmod.setPerm(permission);
    chmod.execute();
  }
}
