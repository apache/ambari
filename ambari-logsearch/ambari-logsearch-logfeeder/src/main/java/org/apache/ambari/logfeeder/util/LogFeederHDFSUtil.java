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

public class LogFeederHDFSUtil {
  private static final Logger LOG = Logger.getLogger(LogFeederHDFSUtil.class);

  private LogFeederHDFSUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static boolean copyFromLocal(String sourceFilepath, String destFilePath, FileSystem fileSystem, boolean overwrite,
      boolean delSrc) {
    Path src = new Path(sourceFilepath);
    Path dst = new Path(destFilePath);
    boolean isCopied = false;
    try {
      LOG.info("copying localfile := " + sourceFilepath + " to hdfsPath := " + destFilePath);
      fileSystem.copyFromLocalFile(delSrc, overwrite, src, dst);
      isCopied = true;
    } catch (Exception e) {
      LOG.error("Error copying local file :" + sourceFilepath + " to hdfs location : " + destFilePath, e);
    }
    return isCopied;
  }

  public static FileSystem buildFileSystem(String hdfsHost, String hdfsPort) {
    try {
      Configuration configuration = buildHdfsConfiguration(hdfsHost, hdfsPort);
      FileSystem fs = FileSystem.get(configuration);
      return fs;
    } catch (Exception e) {
      LOG.error("Exception is buildFileSystem :", e);
    }
    return null;
  }

  private static Configuration buildHdfsConfiguration(String hdfsHost, String hdfsPort) {
    String url = "hdfs://" + hdfsHost + ":" + hdfsPort + "/";
    Configuration configuration = new Configuration();
    configuration.set("fs.default.name", url);
    return configuration;
  }

  public static void closeFileSystem(FileSystem fileSystem) {
    if (fileSystem != null) {
      try {
        fileSystem.close();
      } catch (IOException e) {
        LOG.error(e.getLocalizedMessage(), e.getCause());
      }
    }
  }
}