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

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapDateDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
    name = "Map Date",
    description = "The name of the mapping element should be map_date. The value json element may contain the following parameters:"
)
public class MapDateDescriptorImpl extends MapFieldDescriptorImpl implements MapDateDescriptor {
  @Override
  public String getJsonName() {
    return "map_date";
  }

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_date/src_date_pattern",
    type = "string",
    description = "If it is specified than the mapper converts from this format to the target, and also adds missing year",
    examples = {"MMM dd HH:mm:ss"}
  )
  @Expose
  @SerializedName("src_date_pattern")
  private String sourceDatePattern;

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_date/target_date_pattern",
    type = "string",
    description = "If 'epoch' then the field is parsed as seconds from 1970, otherwise the content used as pattern",
    examples = {"yyyy-MM-dd HH:mm:ss,SSS", "epoch"}
  )
  @Expose
  @SerializedName("target_date_pattern")
  private String targetDatePattern;

  @Override
  public String getSourceDatePattern() {
    return sourceDatePattern;
  }

  public void setSourceDatePattern(String sourceDatePattern) {
    this.sourceDatePattern = sourceDatePattern;
  }

  @Override
  public String getTargetDatePattern() {
    return targetDatePattern;
  }

  public void setTargetDatePattern(String targetDatePattern) {
    this.targetDatePattern = targetDatePattern;
  }
}
