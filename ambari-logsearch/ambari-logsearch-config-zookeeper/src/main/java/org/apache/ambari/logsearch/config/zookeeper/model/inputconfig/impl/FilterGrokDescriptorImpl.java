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

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FilterGrokDescriptorImpl extends FilterDescriptorImpl implements FilterGrokDescriptor {
  @Expose
  @SerializedName("log4j_format")
  private String log4jFormat;

  @Expose
  @SerializedName("multiline_pattern")
  private String multilinePattern;

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
