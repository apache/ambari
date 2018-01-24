package org.apache.ambari.infra.job.archive;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.apache.commons.lang.StringUtils.isBlank;

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
public class S3Uploader extends AbstractFileAction {

  private static final Logger LOG = LoggerFactory.getLogger(S3Uploader.class);

  private final AmazonS3Client client;
  private final String keyPrefix;
  private final String bucketName;

  public S3Uploader(S3Properties s3Properties) {
    LOG.info("Initializing S3 client with " + s3Properties);

    this.keyPrefix = s3Properties.getS3KeyPrefix();
    this.bucketName = s3Properties.getS3BucketName();
    BasicAWSCredentials credentials = new BasicAWSCredentials(s3Properties.getS3AccessKey(), s3Properties.getS3SecretKey());
    client = new AmazonS3Client(credentials);
    if (!isBlank(s3Properties.getS3EndPoint()))
      client.setEndpoint(s3Properties.getS3EndPoint());
//     Note: without pathStyleAccess=true endpoint going to be <bucketName>.<host>:<port>
//    client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
  }

  @Override
  public File onPerform(File inputFile) {
    String key = keyPrefix + inputFile.getName();

    if (client.doesObjectExist(bucketName, key)) {
      throw new UnsupportedOperationException(String.format("Object '%s' already exists in bucket '%s'", key, bucketName));
    }

    client.putObject(bucketName, key, inputFile);
    return inputFile;
  }
}
