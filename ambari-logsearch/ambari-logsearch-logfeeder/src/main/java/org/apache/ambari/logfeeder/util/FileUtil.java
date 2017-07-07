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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class FileUtil {
  private static final Logger LOG = Logger.getLogger(FileUtil.class);
  
  private FileUtil() {
    throw new UnsupportedOperationException();
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
  
  public static HashMap<String, Object> getJsonFileContentFromClassPath(String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    try (InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(fileName)) {
      return mapper.readValue(inputStream, new TypeReference<HashMap<String, Object>>() {});
    } catch (IOException e) {
      LOG.error(e, e.getCause());
    }
    return new HashMap<String, Object>();
  }
  
  public static void move(File source, File target) throws IOException {
    Path sourcePath = Paths.get(source.getAbsolutePath());
    Path targetPath = Paths.get(target.getAbsolutePath());
    Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
}
