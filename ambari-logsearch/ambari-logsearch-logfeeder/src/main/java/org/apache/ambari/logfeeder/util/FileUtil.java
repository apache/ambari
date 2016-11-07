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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class FileUtil {
  private static final Logger LOG = Logger.getLogger(FileUtil.class);
  
  private FileUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static List<File> getAllFileFromDir(File directory, String extension, boolean checkInSubDir) {
    if (!directory.exists()) {
      LOG.error(directory.getAbsolutePath() + " is not exists ");
    } else if (!directory.isDirectory()) {
      LOG.error(directory.getAbsolutePath() + " is not Directory ");
    } else {
      return (List<File>) FileUtils.listFiles(directory, new String[]{extension}, checkInSubDir);
    }
    return new ArrayList<File>();
  }


  public static Object getFileKey(File file) {
    try {
      Path fileFullPath = Paths.get(file.getAbsolutePath());
      if (fileFullPath != null) {
        BasicFileAttributes basicAttr = Files.readAttributes(fileFullPath, BasicFileAttributes.class);
        return basicAttr.fileKey();
      }
    } catch (Throwable ex) {
      LOG.error("Error getting file attributes for file=" + file, ex);
    }
    return file.toString();
  }

  public static File getFileFromClasspath(String filename) {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader().getResource(filename);
    LOG.debug("File Complete URI :" + fileCompleteUrl);
    File file = null;
    try {
      file = new File(fileCompleteUrl.toURI());
    } catch (Exception exception) {
      LOG.debug(exception.getMessage(), exception.getCause());
    }
    return file;
  }

  public static HashMap<String, Object> readJsonFromFile(File jsonFile) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      HashMap<String, Object> jsonmap = mapper.readValue(jsonFile, new TypeReference<HashMap<String, Object>>() {});
      return jsonmap;
    } catch (IOException e) {
      LOG.error(e, e.getCause());
    }
    return new HashMap<String, Object>();
  }
}
