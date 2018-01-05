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
package org.apache.ambari.infra.job.archive;

import static org.apache.commons.lang.StringUtils.isBlank;

public class S3Properties {
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final String s3KeyPrefix;
  private final String s3BucketName;
  private final String s3EndPoint;

  public S3Properties(String s3AccessKey, String s3SecretKey, String s3KeyPrefix, String s3BucketName, String s3EndPoint) {
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.s3KeyPrefix = s3KeyPrefix;
    this.s3BucketName = s3BucketName;
    this.s3EndPoint = s3EndPoint;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public String getS3KeyPrefix() {
    return s3KeyPrefix;
  }

  public String getS3BucketName() {
    return s3BucketName;
  }

  public String getS3EndPoint() {
    return s3EndPoint;
  }

  @Override
  public String toString() {
    return "S3Properties{" +
            "s3AccessKey='" + s3AccessKey + '\'' +
            ", s3KeyPrefix='" + s3KeyPrefix + '\'' +
            ", s3BucketName='" + s3BucketName + '\'' +
            ", s3EndPoint='" + s3EndPoint + '\'' +
            '}';
  }

  public void validate() {
    if (isBlank(s3AccessKey))
      throw new IllegalArgumentException("The property s3AccessKey can not be null or empty string!");

    if (isBlank(s3SecretKey))
      throw new IllegalArgumentException("The property s3SecretKey can not be null or empty string!");

    if (isBlank(s3BucketName))
      throw new IllegalArgumentException("The property s3BucketName can not be null or empty string!");
  }
}
