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
package org.apache.ambari.logfeeder.util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

public enum LogfeederHDFSUtil {
  INSTANCE;
  private static Logger logger = Logger.getLogger(LogfeederHDFSUtil.class);

  public void createHDFSDir(String dirPath, FileSystem dfs) {
    Path src = new Path(dirPath);
    try {
      if (dfs.isDirectory(src)) {
        logger.info("hdfs dir dirPath=" + dirPath + "  is already exist.");
        return;
      }
      boolean isDirCreated = dfs.mkdirs(src);
      if (isDirCreated) {
        logger.debug("HDFS dirPath=" + dirPath + " created successfully.");
      } else {
        logger.warn("HDFS dir creation failed dirPath=" + dirPath);
      }
    } catch (IOException e) {
      logger.error("HDFS dir creation failed dirPath=" + dirPath, e.getCause());
    }
  }

  public boolean copyFromLocal(String sourceFilepath, String destFilePath,
      FileSystem fileSystem, boolean overwrite, boolean delSrc) {
    Path src = new Path(sourceFilepath);
    Path dst = new Path(destFilePath);
    boolean isCopied = false;
    try {
      logger.info("copying localfile := " + sourceFilepath + " to hdfsPath := "
          + destFilePath);
      fileSystem.copyFromLocalFile(delSrc, overwrite, src, dst);
      isCopied = true;
    } catch (Exception e) {
      logger.error("Error copying local file :" + sourceFilepath
          + " to hdfs location : " + destFilePath, e);
    }
    return isCopied;
  }

  public FileSystem buildFileSystem(String hdfsHost, String hdfsPort) {
    try {
      Configuration configuration = buildHdfsConfiguration(hdfsHost, hdfsPort);
      FileSystem fs = FileSystem.get(configuration);
      return fs;
    } catch (Exception e) {
      logger.error("Exception is buildFileSystem :", e);
    }
    return null;
  }

  public void closeFileSystem(FileSystem fileSystem) {
    if (fileSystem != null) {
      try {
        fileSystem.close();
      } catch (IOException e) {
        logger.error(e.getLocalizedMessage(), e.getCause());
      }
    }
  }

  public Configuration buildHdfsConfiguration(String hdfsHost, String hdfsPort) {
    String url = "hdfs://" + hdfsHost + ":" + hdfsPort + "/";
    Configuration configuration = new Configuration();
    configuration.set("fs.default.name", url);
    return configuration;
  }

}