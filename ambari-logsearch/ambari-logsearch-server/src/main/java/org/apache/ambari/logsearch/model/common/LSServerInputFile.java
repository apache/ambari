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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;

import io.swagger.annotations.ApiModel;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileDescriptor;

@ApiModel
public class LSServerInputFile extends LSServerInputFileBase {

  @JsonProperty("detach_interval_min")
  private Integer detachIntervalMin;

  @JsonProperty("detach_time_min")
  private Integer detachTimeMin;

  @JsonProperty("path_update_interval_min")
  private Integer pathUpdateIntervalMin;

  @JsonProperty("max_age_min")
  private Integer maxAgeMin;

  public LSServerInputFile() {}

  public LSServerInputFile(InputDescriptor inputDescriptor) {
    super(inputDescriptor);
    InputFileDescriptor inputFileDescriptor = (InputFileDescriptor)inputDescriptor;
    this.detachIntervalMin = inputFileDescriptor.getDetachIntervalMin();
    this.detachTimeMin = inputFileDescriptor.getDetachTimeMin();
    this.pathUpdateIntervalMin = inputFileDescriptor.getPathUpdateIntervalMin();
    this.maxAgeMin = inputFileDescriptor.getMaxAgeMin();
  }

  public Integer getDetachIntervalMin() {
    return detachIntervalMin;
  }

  public void setDetachIntervalMin(Integer detachIntervalMin) {
    this.detachIntervalMin = detachIntervalMin;
  }

  public Integer getDetachTimeMin() {
    return detachTimeMin;
  }

  public void setDetachTimeMin(Integer detachTimeMin) {
    this.detachTimeMin = detachTimeMin;
  }

  public Integer getPathUpdateIntervalMin() {
    return pathUpdateIntervalMin;
  }

  public void setPathUpdateIntervalMin(Integer pathUpdateIntervalMin) {
    this.pathUpdateIntervalMin = pathUpdateIntervalMin;
  }

  public Integer getMaxAgeMin() {
    return maxAgeMin;
  }

  public void setMaxAgeMin(Integer maxAgeMin) {
    this.maxAgeMin = maxAgeMin;
  }
}
