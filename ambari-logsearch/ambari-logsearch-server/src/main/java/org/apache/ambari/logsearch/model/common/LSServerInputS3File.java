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

import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputS3FileDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LSServerInputS3File extends LSServerInputFileBase {
  @NotNull
  @JsonProperty("s3_access_key")
  private String s3AccessKey;
  
  @NotNull
  @JsonProperty("s3_secret_key")
  private String s3SecretKey;
  
  public LSServerInputS3File() {}
  
  public LSServerInputS3File(InputDescriptor inputDescriptor) {
    super(inputDescriptor);
    InputS3FileDescriptor inputS3FileDescriptor = (InputS3FileDescriptor)inputDescriptor;
    this.s3AccessKey = inputS3FileDescriptor.getS3AccessKey();
    this.s3SecretKey = inputS3FileDescriptor.getS3SecretKey();
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public void setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public void setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }
}
