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

package org.apache.ambari.view.hive.utils;


import org.apache.hadoop.fs.FSDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HdfsUtil {
  private final static Logger LOG =
      LoggerFactory.getLogger(HdfsUtil.class);

  /**
   * Write string to file with overwriting
   * @param filePath path to file
   * @param content new content of file
   */
  public static void putStringToFile(HdfsApi hdfs, String filePath, String content) {
    FSDataOutputStream stream;
    try {
      synchronized (hdfs) {
        stream = hdfs.create(filePath, true);
        stream.writeBytes(content);
        stream.close();
      }
    } catch (IOException e) {
      throw new ServiceFormattedException("Could not write file " + filePath, e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Could not write file " + filePath, e);
    }
  }


  /**
   * Increment index appended to filename until find first unallocated file
   * @param fullPathAndFilename path to file and prefix for filename
   * @param extension file extension
   * @return if fullPathAndFilename="/tmp/file",extension=".txt" then filename will be like "/tmp/file_42.txt"
   */
  public static String findUnallocatedFileName(HdfsApi hdfs, String fullPathAndFilename, String extension) {
    int triesCount = 0;
    String newFilePath;
    boolean isUnallocatedFilenameFound;

    try {
      do {
        newFilePath = String.format(fullPathAndFilename + "%s" + extension, (triesCount == 0) ? "" : "_" + triesCount);
        LOG.debug("Trying to find free filename " + newFilePath);

        isUnallocatedFilenameFound = !hdfs.exists(newFilePath);
        if (isUnallocatedFilenameFound) {
          LOG.debug("File created successfully!");
        }

        triesCount += 1;
      } while (!isUnallocatedFilenameFound);
    } catch (IOException e) {
      throw new ServiceFormattedException("Error in creation: " + e.toString(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Error in creation: " + e.toString(), e);
    }

    return newFilePath;
  }
}
