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
package org.apache.ambari.logfeeder.input.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

public class GZIPReader extends InputStreamReader {

  private static Logger logger = Logger.getLogger(GZIPReader.class);

  public GZIPReader(String fileName) throws FileNotFoundException {
    super(getStream(fileName));
    logger.info("Created GZIPReader for file : " + fileName);
  }

  public GZIPReader(File file) throws FileNotFoundException {
    super(getStream(file.getName()));
  }

  private static InputStream getStream(String fileName) {
    InputStream gzipStream = null;
    InputStream fileStream = null;
    try {
      fileStream = new FileInputStream(fileName);
      gzipStream = new GZIPInputStream(fileStream);
    } catch (Exception e) {
      logger.error(e, e.getCause());
    }
    return gzipStream;
  }

  /**
   * validating file based on magic number
   *
   * @param fileName
   * @return
   */
  public static boolean isValidFile(String fileName) {
    // TODO make it generic and put in factory itself
    InputStream is = null;
    try {
      is = new FileInputStream(fileName);
      byte[] signature = new byte[2];
      int nread = is.read(signature); // read the gzip signature
      return nread == 2 && signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b;
    } catch (IOException e) {
      return false;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
