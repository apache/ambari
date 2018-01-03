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

import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.JobParameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DocumentExportProperties {
  private String zooKeeperConnectionString;
  private int readBlockSize;
  private int writeBlockSize;
  private String destinationDirectoryPath;
  private String fileNameSuffixColumn;
  private SolrQueryProperties query;
  private String s3AccessKey;
  private String s3SecretKey;
  private String s3KeyPrefix;
  private String s3BucketName;
  private String s3Endpoint;

  public String getZooKeeperConnectionString() {
    return zooKeeperConnectionString;
  }

  public void setZooKeeperConnectionString(String zooKeeperConnectionString) {
    this.zooKeeperConnectionString = zooKeeperConnectionString;
  }

  public int getReadBlockSize() {
    return readBlockSize;
  }

  public void setReadBlockSize(int readBlockSize) {
    this.readBlockSize = readBlockSize;
  }

  public int getWriteBlockSize() {
    return writeBlockSize;
  }

  public void setWriteBlockSize(int writeBlockSize) {
    this.writeBlockSize = writeBlockSize;
  }

  public String getDestinationDirectoryPath() {
    return destinationDirectoryPath;
  }

  public void setDestinationDirectoryPath(String destinationDirectoryPath) {
    this.destinationDirectoryPath = destinationDirectoryPath;
  }

  public String getFileNameSuffixColumn() {
    return fileNameSuffixColumn;
  }

  public void setFileNameSuffixColumn(String fileNameSuffixColumn) {
    this.fileNameSuffixColumn = fileNameSuffixColumn;
  }

  public SolrQueryProperties getQuery() {
    return query;
  }

  public void setQuery(SolrQueryProperties query) {
    this.query = query;
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

  public String getS3KeyPrefix() {
    return s3KeyPrefix;
  }

  public void setS3KeyPrefix(String s3KeyPrefix) {
    this.s3KeyPrefix = s3KeyPrefix;
  }

  public String getS3BucketName() {
    return s3BucketName;
  }

  public void setS3BucketName(String s3BucketName) {
    this.s3BucketName = s3BucketName;
  }

  public String getS3Endpoint() {
    return s3Endpoint;
  }

  public void setS3Endpoint(String s3Endpoint) {
    this.s3Endpoint = s3Endpoint;
  }

  public void apply(JobParameters jobParameters) {
    zooKeeperConnectionString = jobParameters.getString("zooKeeperConnectionString", zooKeeperConnectionString);
    readBlockSize = getIntJobParameter(jobParameters, "readBlockSize", readBlockSize);
    writeBlockSize = getIntJobParameter(jobParameters, "writeBlockSize", writeBlockSize);
    destinationDirectoryPath = jobParameters.getString("destinationDirectoryPath", destinationDirectoryPath);
    query.apply(jobParameters);
  }

  private int getIntJobParameter(JobParameters jobParameters, String parameterName, int defaultValue) {
    String valueText = jobParameters.getString(parameterName);
    if (isBlank(valueText))
      return defaultValue;
    return Integer.parseInt(valueText);
  }

  public DocumentExportProperties deepCopy() {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String json = objectMapper.writeValueAsString(this);
      return objectMapper.readValue(json, DocumentExportProperties.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Optional<S3Properties> s3Properties() {
    if (!isBlank(s3AccessKey) && !isBlank(s3SecretKey) && !isBlank(s3BucketName))
      return Optional.of(new S3Properties(s3AccessKey, s3SecretKey, s3KeyPrefix, s3BucketName, s3Endpoint));
    return Optional.empty();
  }

  public void validate() {
    if (isBlank(zooKeeperConnectionString))
      throw new IllegalArgumentException("The property zooKeeperConnectionString can not be null or empty string!");

    if (readBlockSize == 0)
      throw new IllegalArgumentException("The property readBlockSize must be greater than 0!");

    if (writeBlockSize == 0)
      throw new IllegalArgumentException("The property writeBlockSize must be greater than 0!");

    if (isBlank(destinationDirectoryPath))
      throw new IllegalArgumentException("The property destinationDirectoryPath can not be null or empty string!");

    if (isBlank(fileNameSuffixColumn))
      throw new IllegalArgumentException("The property fileNameSuffixColumn can not be null or empty string!");

    query.validate();
  }
}
