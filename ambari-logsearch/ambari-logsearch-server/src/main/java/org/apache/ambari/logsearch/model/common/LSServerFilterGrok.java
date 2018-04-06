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

import javax.validation.constraints.NotNull;

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LSServerFilterGrok extends LSServerFilter {
  @JsonProperty("log4j_format")
  private String log4jFormat;

  @NotNull
  @JsonProperty("multiline_pattern")
  private String multilinePattern;

  @NotNull
  @JsonProperty("message_pattern")
  private String messagePattern;

  @JsonProperty
  private boolean skipOnError;

  @JsonProperty
  private boolean deepExtract;

  public LSServerFilterGrok() {}

  public LSServerFilterGrok(FilterDescriptor filterDescriptor) {
    super(filterDescriptor);
    if (filterDescriptor instanceof FilterGrokDescriptor) {
      FilterGrokDescriptor filterGrokDescriptor = (FilterGrokDescriptor)filterDescriptor;
      this.log4jFormat = filterGrokDescriptor.getLog4jFormat();
      this.multilinePattern = filterGrokDescriptor.getMultilinePattern();
      this.messagePattern = filterGrokDescriptor.getMessagePattern();
      this.skipOnError = filterGrokDescriptor.isSkipOnError();
      this.deepExtract = filterGrokDescriptor.isDeepExtract();
    }
  }

  public String getLog4jFormat() {
    return log4jFormat;
  }

  public void setLog4jFormat(String log4jFormat) {
    this.log4jFormat = log4jFormat;
  }

  public String getMultilinePattern() {
    return multilinePattern;
  }

  public void setMultilinePattern(String multilinePattern) {
    this.multilinePattern = multilinePattern;
  }

  public String getMessagePattern() {
    return messagePattern;
  }

  public void setMessagePattern(String messagePattern) {
    this.messagePattern = messagePattern;
  }

  public boolean isSkipOnError() {
    return skipOnError;
  }

  public void setSkipOnError(boolean skipOnError) {
    this.skipOnError = skipOnError;
  }

  public boolean isDeepExtract() {
    return deepExtract;
  }

  public void setDeepExtract(boolean deepExtract) {
    this.deepExtract = deepExtract;
  }
}
