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

package org.apache.ambari.logfeeder.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;

public class OutputFile extends Output {
  static Logger logger = Logger.getLogger(OutputFile.class);

  PrintWriter outWriter = null;
  String filePath = null;
  String codec;

  @Override
  public void init() throws Exception {
    super.init();

    filePath = getStringValue("path");
    if (filePath == null || filePath.isEmpty()) {
      logger.error("Filepath config property <path> is not set in config file.");
      return;
    }
    codec = getStringValue("codec");
    if (codec == null || codec.trim().isEmpty()) {
      codec = "json";
    } else {
      if (codec.trim().equalsIgnoreCase("csv")) {
        codec = "csv";
      } else if (codec.trim().equalsIgnoreCase("json")) {
        codec = "csv";
      } else {
        logger.error("Unsupported codec type. codec=" + codec
          + ", will use json");
        codec = "json";
      }
    }
    logger.info("Out filePath=" + filePath + ", codec=" + codec);
    File outFile = new File(filePath);
    if (outFile.getParentFile() != null) {
      File parentDir = outFile.getParentFile();
      if (!parentDir.isDirectory()) {
        parentDir.mkdirs();
      }
    }

    outWriter = new PrintWriter(new BufferedWriter(new FileWriter(outFile,
      true)));

    logger.info("init() is successfull. filePath="
      + outFile.getAbsolutePath());
  }

  @Override
  public void close() {
    logger.info("Closing file." + getShortDescription());
    if (outWriter != null) {
      try {
        outWriter.close();
      } catch (Throwable t) {
        // Ignore this exception
      }
    }
    isClosed = true;
  }

  @Override
  public void write(Map<String, Object> jsonObj, InputMarker inputMarker)
    throws Exception {
    String outStr = null;
    CSVPrinter csvPrinter = null;
    try {
      if (codec.equals("csv")) {
        csvPrinter = new CSVPrinter(outWriter, CSVFormat.RFC4180);
        //TODO:
      } else {
        outStr = LogFeederUtil.getGson().toJson(jsonObj);
      }
      if (outWriter != null && outStr != null) {
        statMetric.count++;

        outWriter.println(outStr);
        outWriter.flush();
      }
    } finally {
      if (csvPrinter != null) {
        try {
          csvPrinter.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @Override
  synchronized public void write(String block, InputMarker inputMarker) throws Exception {
    if (outWriter != null && block != null) {
      statMetric.count++;

      outWriter.println(block);
      outWriter.flush();
    }
  }

  @Override
  public String getShortDescription() {
    return "output:destination=file,path=" + filePath;
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "copyFile method is not yet supported for output=file");
  }

}
