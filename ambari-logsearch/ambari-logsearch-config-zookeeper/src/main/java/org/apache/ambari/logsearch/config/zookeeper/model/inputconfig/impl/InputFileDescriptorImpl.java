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

package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileDescriptor;

public class InputFileDescriptorImpl extends InputFileBaseDescriptorImpl implements InputFileDescriptor {

  @ShipperConfigElementDescription(
    path = "/input/[]/detach_interval_min",
    type = "integer",
    description = "The period in minutes for checking which files are too old (default: 300)",
    examples = {"60"},
    defaultValue = "1800"
  )
  @Expose
  @SerializedName("detach_interval_min")
  private Integer detachIntervalMin;

  @ShipperConfigElementDescription(
    path = "/input/[]/detach_time_min",
    type = "integer",
    description = "The period in minutes when the application flags a file is too old (default: 2000)",
    examples = {"60"},
    defaultValue = "2000"
  )
  @Expose
  @SerializedName("detach_time_min")
  private Integer detachTimeMin;

  @ShipperConfigElementDescription(
    path = "/input/[]/path_update_interval_min",
    type = "integer",
    description = "The period in minutes for checking new files (default: 5, based on detach values, its possible that a new input wont be monitored)",
    examples = {"5"},
    defaultValue = "5"
  )
  @Expose
  @SerializedName("path_update_interval_min")
  private Integer pathUpdateIntervalMin;

  @ShipperConfigElementDescription(
    path = "/input/[]/max_age_min",
    type = "integer",
    description = "If the file has not modified for long (this time value in minutes), then the checkpoint file can be deleted.",
    examples = {"2000"},
    defaultValue = "0"
  )
  @Expose
  @SerializedName("max_age_min")
  private Integer maxAgeMin;

  @Override
  public Integer getDetachIntervalMin() {
    return this.detachIntervalMin;
  }

  @Override
  public Integer getDetachTimeMin() {
    return this.detachTimeMin;
  }

  @Override
  public Integer getPathUpdateIntervalMin() {
    return this.pathUpdateIntervalMin;
  }

  @Override
  public Integer getMaxAgeMin() {
    return this.maxAgeMin;
  }

  public void setDetachIntervalMin(Integer detachIntervalMin) {
    this.detachIntervalMin = detachIntervalMin;
  }

  public void setDetachTimeMin(Integer detachTimeMin) {
    this.detachTimeMin = detachTimeMin;
  }

  public void setPathUpdateIntervalMin(Integer pathUpdateIntervalMin) {
    this.pathUpdateIntervalMin = pathUpdateIntervalMin;
  }

  public void setMaxAgeMin(Integer maxAgeMin) {
    this.maxAgeMin = maxAgeMin;
  }
}
