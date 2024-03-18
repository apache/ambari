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

package org.apache.ambari.logsearch.config.json.model.inputconfig.impl;

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileBaseDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
  name = "File Input",
  description = "File inputs have some additional parameters:"
)
public class InputFileBaseDescriptorImpl extends InputDescriptorImpl implements InputFileBaseDescriptor {
  @ShipperConfigElementDescription(
    path = "/input/[]/checkpoint_interval_ms",
    type = "integer",
    description = "The time interval in ms when the checkpoint file should be updated.",
    examples = {"10000"},
    defaultValue = "5000"
  )
  @Expose
  @SerializedName("checkpoint_interval_ms")
  private Integer checkpointIntervalMs;

  @ShipperConfigElementDescription(
    path = "/input/[]/process_file",
    type = "boolean",
    description = "Should the file be processed.",
    examples = {"true", "false"},
    defaultValue = "true"
  )
  @Expose
  @SerializedName("process_file")
  private Boolean processFile;

  @ShipperConfigElementDescription(
    path = "/input/[]/copy_file",
    type = "boolean",
    description = "Should the file be copied (only if not processed).",
    examples = {"true", "false"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("copy_file")
  private Boolean copyFile;

  @Override
  public Boolean getProcessFile() {
    return processFile;
  }

  public void setProcessFile(Boolean processFile) {
    this.processFile = processFile;
  }

  @Override
  public Boolean getCopyFile() {
    return copyFile;
  }

  public void setCopyFile(Boolean copyFile) {
    this.copyFile = copyFile;
  }

  @Override
  public Integer getCheckpointIntervalMs() {
    return checkpointIntervalMs;
  }

  public void setCheckpointIntervalMs(Integer checkpointIntervalMs) {
    this.checkpointIntervalMs = checkpointIntervalMs;
  }
}
