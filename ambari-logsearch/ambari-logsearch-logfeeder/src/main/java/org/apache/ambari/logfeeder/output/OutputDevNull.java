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
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputProperties;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Output that just ignore the logs
 */
public class OutputDevNull extends Output<LogFeederProps, InputMarker> {

  private static final Logger LOG = Logger.getLogger(OutputDevNull.class);

  @Override
  public void write(String block, InputMarker inputMarker){
    LOG.trace("Ignore log block: " + block);
  }

  @Override
  public Long getPendingCount() {
    return 0L;
  }

  @Override
  public String getWriteBytesMetricName() {
    return "write:devnull";
  }

  @Override
  public String getOutputType() {
    return "devnull";
  }

  @Override
  public void outputConfigChanged(OutputProperties outputProperties) {
  }

  @Override
  public void close() {
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) {
    throw new UnsupportedOperationException("copyFile method is not yet supported for output=dev_null");
  }

  @Override
  public void init(LogFeederProps LogFeederProps) throws Exception {
  }

  @Override
  public String getShortDescription() {
    return "write:devnull";
  }

  @Override
  public String getStatMetricName() {
    return "write:devnull";
  }
}
