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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;

public class CompressionUtil {

  private static final Logger LOG = Logger.getLogger(CompressionUtil.class);

  public static File compressFile(File inputFile, File outputFile, String algoName) {
    CompressorOutputStream cos = null;
    FileInputStream ios = null;
    try {
      if (!inputFile.exists()) {
        throw new IllegalArgumentException("Input File:" + inputFile.getAbsolutePath() + " is not exist.");
      }
      if (inputFile.isDirectory()) {
        throw new IllegalArgumentException("Input File:" + inputFile.getAbsolutePath() + " is a directory.");
      }
      File parent = outputFile.getParentFile();
      if (parent != null && !parent.exists()) {
        boolean isParentCreated = parent.mkdirs();
        if (!isParentCreated) {
          throw new IllegalAccessException( "User does not have permission to create parent directory :" + parent.getAbsolutePath());
        }
      }
      OutputStream out = new FileOutputStream(outputFile);
      cos = new CompressorStreamFactory().createCompressorOutputStream(algoName, out);
      ios = new FileInputStream(inputFile);
      IOUtils.copy(ios, cos);
    } catch (Exception e) {
      LOG.error(e);
    } finally {
      if (cos != null) {
        try {
          cos.close();
        } catch (IOException e) {
          LOG.error(e);
        }
      }
      if (ios != null) {
        try {
          ios.close();
        } catch (IOException e) {
          LOG.error(e);
        }
      }
    }
    return outputFile;
  }
}
