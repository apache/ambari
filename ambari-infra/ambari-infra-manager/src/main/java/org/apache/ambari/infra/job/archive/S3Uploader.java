package org.apache.ambari.infra.job.archive;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

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
public class S3Uploader implements FileAction {

  private final AmazonS3Client client;
  private final String keyPrefix;
  private final String bucketName;

  public S3Uploader(S3Properties s3Properties) {
    this.keyPrefix = s3Properties.getKeyPrefix();
    this.bucketName = s3Properties.getBucketName();
    BasicAWSCredentials credentials = new BasicAWSCredentials(s3Properties.getAccessKey(), s3Properties.getSecretKey());
    client = new AmazonS3Client(credentials);
  }

  @Override
  public File perform(File inputFile) {
    String key = keyPrefix + inputFile.getName();

    if (client.doesObjectExist(bucketName, key)) {
      System.out.println("Object '" + key + "' already exists");
      System.exit(0);
    }

    client.putObject(bucketName, key, inputFile);
    return inputFile;
  }
}
