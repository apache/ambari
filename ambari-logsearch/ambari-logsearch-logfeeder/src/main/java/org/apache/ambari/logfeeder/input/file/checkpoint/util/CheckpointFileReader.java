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
package org.apache.ambari.logfeeder.input.file.checkpoint.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.util.Map;

public class CheckpointFileReader {

  private CheckpointFileReader() {
  }

  public static File[] getFiles(File checkPointFolderFile, String checkPointExtension) {
    String searchPath = "*" + checkPointExtension;
    FileFilter fileFilter = new WildcardFileFilter(searchPath);
    return checkPointFolderFile.listFiles(fileFilter);
  }

  public static Map<String, String> getCheckpointObject(File checkPointFile) throws IOException {
    final Map<String, String> jsonCheckPoint;
    try (RandomAccessFile checkPointReader = new RandomAccessFile(checkPointFile, "r")) {
      int contentSize = checkPointReader.readInt();
      byte b[] = new byte[contentSize];
      int readSize = checkPointReader.read(b, 0, contentSize);
      if (readSize != contentSize) {
        throw new IllegalArgumentException("Couldn't read expected number of bytes from checkpoint file. expected=" + contentSize + ", read="
          + readSize + ", checkPointFile=" + checkPointFile);
      } else {
        String jsonCheckPointStr = new String(b, 0, readSize);
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        jsonCheckPoint = gson.fromJson(jsonCheckPointStr, type);
      }
    }
    return jsonCheckPoint;
  }


}
