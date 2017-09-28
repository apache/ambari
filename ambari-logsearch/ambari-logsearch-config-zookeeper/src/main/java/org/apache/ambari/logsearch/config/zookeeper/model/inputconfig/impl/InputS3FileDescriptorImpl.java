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
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputS3FileDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
  name = "S3 File Input",
  description = "S3 file inputs have the following parameters in addition to the general file parameters:"
)
public class InputS3FileDescriptorImpl extends InputFileBaseDescriptorImpl implements InputS3FileDescriptor {
  @ShipperConfigElementDescription(
    path = "/input/[]/s3_access_key",
    type = "string",
    description = "The access key used for AWS credentials."
  )
  @Expose
  @SerializedName("s3_access_key")
  private String s3AccessKey;

  @ShipperConfigElementDescription(
    path = "/input/[]/s3_secret_key",
    type = "string",
    description = "The secret key used for AWS credentials."
  )
  @Expose
  @SerializedName("s3_secret_key")
  private String s3SecretKey;

  @Override
  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public void setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }

  @Override
  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public void setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }
}
