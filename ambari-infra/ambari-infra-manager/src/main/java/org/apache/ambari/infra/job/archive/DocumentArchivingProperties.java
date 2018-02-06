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

import org.apache.ambari.infra.job.JobProperties;
import org.springframework.batch.core.JobParameters;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.apache.ambari.infra.job.archive.ExportDestination.HDFS;
import static org.apache.ambari.infra.job.archive.ExportDestination.LOCAL;
import static org.apache.ambari.infra.job.archive.ExportDestination.S3;
import static org.apache.commons.lang.StringUtils.isBlank;

public class DocumentArchivingProperties extends JobProperties<DocumentArchivingProperties> {
  private int readBlockSize;
  private int writeBlockSize;
  private ExportDestination destination;
  private String localDestinationDirectory;
  private String fileNameSuffixColumn;
  private String fileNameSuffixDateFormat;
  private SolrProperties solr;
  private String s3AccessFile;
  private String s3KeyPrefix;
  private String s3BucketName;
  private String s3Endpoint;

  private String hdfsEndpoint;
  private String hdfsDestinationDirectory;

  public DocumentArchivingProperties() {
    super(DocumentArchivingProperties.class);
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

  public ExportDestination getDestination() {
    return destination;
  }

  public void setDestination(ExportDestination destination) {
    this.destination = destination;
  }

  public String getLocalDestinationDirectory() {
    return localDestinationDirectory;
  }

  public void setLocalDestinationDirectory(String localDestinationDirectory) {
    this.localDestinationDirectory = localDestinationDirectory;
  }

  public String getFileNameSuffixColumn() {
    return fileNameSuffixColumn;
  }

  public void setFileNameSuffixColumn(String fileNameSuffixColumn) {
    this.fileNameSuffixColumn = fileNameSuffixColumn;
  }

  public String getFileNameSuffixDateFormat() {
    return fileNameSuffixDateFormat;
  }

  public void setFileNameSuffixDateFormat(String fileNameSuffixDateFormat) {
    this.fileNameSuffixDateFormat = fileNameSuffixDateFormat;
  }

  public SolrProperties getSolr() {
    return solr;
  }

  public void setSolr(SolrProperties query) {
    this.solr = query;
  }

  public String getS3AccessFile() {
    return s3AccessFile;
  }

  public void setS3AccessFile(String s3AccessFile) {
    this.s3AccessFile = s3AccessFile;
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

  public Optional<S3Properties> s3Properties() {
    if (isBlank(s3BucketName))
      return Optional.empty();

    return Optional.of(new S3Properties(
            s3AccessFile,
            s3KeyPrefix,
            s3BucketName,
            s3Endpoint));
  }

  public String getHdfsEndpoint() {
    return hdfsEndpoint;
  }

  public void setHdfsEndpoint(String hdfsEndpoint) {
    this.hdfsEndpoint = hdfsEndpoint;
  }

  public String getHdfsDestinationDirectory() {
    return hdfsDestinationDirectory;
  }

  public void setHdfsDestinationDirectory(String hdfsDestinationDirectory) {
    this.hdfsDestinationDirectory = hdfsDestinationDirectory;
  }

  @Override
  public void apply(JobParameters jobParameters) {
    readBlockSize = getIntJobParameter(jobParameters, "readBlockSize", readBlockSize);
    writeBlockSize = getIntJobParameter(jobParameters, "writeBlockSize", writeBlockSize);
    destination = ExportDestination.valueOf(jobParameters.getString("destination", destination.name()));
    localDestinationDirectory = jobParameters.getString("localDestinationDirectory", localDestinationDirectory);
    s3AccessFile = jobParameters.getString("s3AccessFile", s3AccessFile);
    s3BucketName = jobParameters.getString("s3BucketName", s3BucketName);
    s3KeyPrefix = jobParameters.getString("s3KeyPrefix", s3KeyPrefix);
    s3Endpoint = jobParameters.getString("s3Endpoint", s3Endpoint);
    hdfsEndpoint = jobParameters.getString("hdfsEndpoint", hdfsEndpoint);
    hdfsDestinationDirectory = jobParameters.getString("hdfsDestinationDirectory", hdfsDestinationDirectory);
    solr.apply(jobParameters);
  }

  private int getIntJobParameter(JobParameters jobParameters, String parameterName, int defaultValue) {
    String valueText = jobParameters.getString(parameterName);
    if (isBlank(valueText))
      return defaultValue;
    return Integer.parseInt(valueText);
  }

  @Override
  public void validate() {
    if (readBlockSize == 0)
      throw new IllegalArgumentException("The property readBlockSize must be greater than 0!");

    if (writeBlockSize == 0)
      throw new IllegalArgumentException("The property writeBlockSize must be greater than 0!");

    if (isBlank(fileNameSuffixColumn)) {
      throw new IllegalArgumentException("The property fileNameSuffixColumn can not be null or empty string!");
    }

    requireNonNull(destination, "The property destination can not be null!");
    switch (destination) {
      case LOCAL:
        if (isBlank(localDestinationDirectory))
          throw new IllegalArgumentException(String.format(
                  "The property localDestinationDirectory can not be null or empty string when destination is set to %s!", LOCAL.name()));
        break;

      case S3:
        s3Properties()
                .orElseThrow(() -> new IllegalArgumentException("S3 related properties must be set if the destination is " + S3.name()))
                .validate();
        break;

      case HDFS:
        if (isBlank(hdfsEndpoint))
          throw new IllegalArgumentException(String.format(
                  "The property hdfsEndpoint can not be null or empty string when destination is set to %s!", HDFS.name()));
        if (isBlank(hdfsDestinationDirectory))
          throw new IllegalArgumentException(String.format(
                  "The property hdfsDestinationDirectory can not be null or empty string when destination is set to %s!", HDFS.name()));
    }

    requireNonNull(solr, "No solr query was specified for archiving job!");
    solr.validate();
  }
}
