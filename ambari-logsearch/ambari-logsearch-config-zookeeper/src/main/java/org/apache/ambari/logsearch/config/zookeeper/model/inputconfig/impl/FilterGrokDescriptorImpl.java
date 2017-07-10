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
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
  name = "Grok Filter",
  description = "Grok filters have the following additional parameters:"
)
public class FilterGrokDescriptorImpl extends FilterDescriptorImpl implements FilterGrokDescriptor {
  @ShipperConfigElementDescription(
    path = "/filter/[]/log4j_format",
    type = "string",
    description = "The log4j pattern of the log, not used, it is only there for documentation.",
    examples = {"%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n"}
  )
  @Expose
  @SerializedName("log4j_format")
  private String log4jFormat;

  @ShipperConfigElementDescription(
    path = "/filter/[]/multiline_pattern",
    type = "string",
    description = "The grok pattern that shows that the line is not a log line on it's own but the part of a multi line entry.",
    examples = {"^(%{TIMESTAMP_ISO8601:logtime})"}
  )
  @Expose
  @SerializedName("multiline_pattern")
  private String multilinePattern;

  @ShipperConfigElementDescription(
    path = "/filter/[]/message_pattern",
    type = "string",
    description = "The grok pattern to use to parse the log entry.",
    examples = {"(?m)^%{TIMESTAMP_ISO8601:logtime}%{SPACE}-%{SPACE}%{LOGLEVEL:level}%{SPACE}\\[%{DATA:thread_name}\\@%{INT:line_number}\\]%{SPACE}-%{SPACE}%{GREEDYDATA:log_message}"}
  )
  @Expose
  @SerializedName("message_pattern")
  private String messagePattern;

  @Override
  public String getLog4jFormat() {
    return log4jFormat;
  }

  public void setLog4jFormat(String log4jFormat) {
    this.log4jFormat = log4jFormat;
  }

  @Override
  public String getMultilinePattern() {
    return multilinePattern;
  }

  @Override
  public void setMultilinePattern(String multilinePattern) {
    this.multilinePattern = multilinePattern;
  }

  @Override
  public String getMessagePattern() {
    return messagePattern;
  }

  public void setMessagePattern(String messagePattern) {
    this.messagePattern = messagePattern;
  }
}
