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
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapAnonymizeDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
    name = "Map Anonymize",
    description = "The name of the mapping element should be map_anonymize. The value json element should contain the following parameter:"
)
public class MapAnonymizeDescriptorImpl extends MapFieldDescriptorImpl implements MapAnonymizeDescriptor {
  @Override
  public String getJsonName() {
    return "map_anonymize";
  }

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_anonymize/pattern",
    type = "string",
    description = "The pattern to use to identify parts to anonymize. The parts to hide should be marked with the \"<hide>\" string.",
    examples = {"Some secret is here: <hide>, and another one is here: <hide>"}
  )
  @Expose
  private String pattern;

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_anonymize/hide_char",
    type = "string",
    description = "The character to hide with",
    defaultValue = "*",
    examples = {"X", "-"}
  )
  @Expose
  @SerializedName("hide_char")
  private Character hideChar;

  @Override
  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public Character getHideChar() {
    return hideChar;
  }

  public void setHideChar(Character hideChar) {
    this.hideChar = hideChar;
  }
}
