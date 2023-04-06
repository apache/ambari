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

package org.apache.ambari.logsearch.model.common;

import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileBaseDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
public abstract class LSServerInputFileBase extends LSServerInput {
  @JsonProperty("checkpoint_interval_ms")
  private Integer checkpointIntervalMs;

  @JsonProperty("process_file")
  private Boolean processFile;

  @JsonProperty("copy_file")
  private Boolean copyFile;
  
  public LSServerInputFileBase() {}
  
  public LSServerInputFileBase(InputDescriptor inputDescriptor) {
    super(inputDescriptor);
    
    InputFileBaseDescriptor inputFileBaseDescriptor = (InputFileBaseDescriptor)inputDescriptor;
    this.checkpointIntervalMs = inputFileBaseDescriptor.getCheckpointIntervalMs();
    this.processFile = inputFileBaseDescriptor.getProcessFile();
    this.copyFile = inputFileBaseDescriptor.getCopyFile();
  }

  public Integer getCheckpointIntervalMs() {
    return checkpointIntervalMs;
  }

  public void setCheckpointIntervalMs(Integer checkpointIntervalMs) {
    this.checkpointIntervalMs = checkpointIntervalMs;
  }

  public Boolean getProcessFile() {
    return processFile;
  }

  public void setProcessFile(Boolean processFile) {
    this.processFile = processFile;
  }

  public Boolean getCopyFile() {
    return copyFile;
  }

  public void setCopyFile(Boolean copyFile) {
    this.copyFile = copyFile;
  }
}
