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

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputProperties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class OutputFile extends Output<LogFeederProps, InputFileMarker> {
  private static final Logger LOG = Logger.getLogger(OutputFile.class);

  private PrintWriter outWriter;
  private String filePath = null;
  private String codec;
  private LogFeederProps logFeederProps;

  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    this.logFeederProps = logFeederProps;
    filePath = getStringValue("path");
    if (StringUtils.isEmpty(filePath)) {
      LOG.error("Filepath config property <path> is not set in config file.");
      return;
    }
    codec = getStringValue("codec");
    if (StringUtils.isBlank(codec)) {
      codec = "json";
    } else {
      if (codec.trim().equalsIgnoreCase("csv")) {
        codec = "csv";
      } else if (codec.trim().equalsIgnoreCase("json")) {
        codec = "csv";
      } else {
        LOG.error("Unsupported codec type. codec=" + codec + ", will use json");
        codec = "json";
      }
    }
    LOG.info("Out filePath=" + filePath + ", codec=" + codec);
    File outFile = new File(filePath);
    if (outFile.getParentFile() != null) {
      File parentDir = outFile.getParentFile();
      if (!parentDir.isDirectory()) {
        parentDir.mkdirs();
      }
    }

    outWriter = new PrintWriter(new BufferedWriter(new FileWriter(outFile, true)));

    LOG.info("init() is successfull. filePath=" + outFile.getAbsolutePath());
  }

  @Override
  public void close() {
    LOG.info("Closing file." + getShortDescription());
    if (outWriter != null) {
      try {
        outWriter.close();
      } catch (Throwable t) {
        // Ignore this exception
      }
    }
    setClosed(true);
  }

  @Override
  public void write(Map<String, Object> jsonObj, InputFileMarker inputMarker) throws Exception {
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
        statMetric.value++;

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
  synchronized public void write(String block, InputFileMarker inputMarker) throws Exception {
    if (outWriter != null && block != null) {
      statMetric.value++;

      outWriter.println(block);
      outWriter.flush();
    }
  }

  @Override
  public Long getPendingCount() {
    return null;
  }

  @Override
  public String getWriteBytesMetricName() {
    return "output.kafka.write_bytes";
  }

  @Override
  public String getShortDescription() {
    return "output:destination=file,path=" + filePath;
  }

  @Override
  public String getStatMetricName() {
    return "output.file.write_logs";
  }

  @Override
  public String getOutputType() {
    throw new IllegalStateException("This method should be overriden if the Output wants to monitor the configuration");
  }

  @Override
  public void outputConfigChanged(OutputProperties outputProperties) {
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("copyFile method is not yet supported for output=file");
  }
}
