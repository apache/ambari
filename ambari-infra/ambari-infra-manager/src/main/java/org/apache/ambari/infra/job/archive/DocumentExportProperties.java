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
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.JobParameters;

import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.lang.StringUtils.isBlank;

public class DocumentExportProperties extends JobProperties<DocumentExportProperties> {
  private int readBlockSize;
  private int writeBlockSize;
  private String destinationDirectoryPath;
  private String fileNameSuffixColumn;
  private String fileNameSuffixDateFormat;
  private SolrProperties solr;
  private String s3AccessFile;
  private String s3KeyPrefix;
  private String s3BucketName;
  private String s3Endpoint;
  private transient Supplier<Optional<S3Properties>> s3Properties;

  public DocumentExportProperties() {
    super(DocumentExportProperties.class);
    s3Properties = this::loadS3Properties;
  }

  private Optional<S3Properties> loadS3Properties() {
    if (isBlank(s3BucketName))
      return Optional.empty();

    String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
    String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

    if (isBlank(accessKey) || isBlank(secretKey)) {
      if (isBlank(s3AccessFile))
        return Optional.empty();
      try (CSVParser csvParser = CSVParser.parse(new FileReader(s3AccessFile), DEFAULT.withHeader("Access key ID", "Secret access key"))) {
        Iterator<CSVRecord> iterator = csvParser.iterator();
        if (!iterator.hasNext()) {
          return Optional.empty();
        }

        CSVRecord record = csvParser.iterator().next();
        Map<String, Integer> header = csvParser.getHeaderMap();
        accessKey = record.get(header.get("Access key ID"));
        secretKey = record.get(header.get("Secret access key"));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return Optional.of(new S3Properties(
            accessKey,
            secretKey,
            s3KeyPrefix,
            s3BucketName,
            s3Endpoint));
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
    s3Properties = this::loadS3Properties;
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
    return s3Properties.get();
  }

  @Override
  public void apply(JobParameters jobParameters) {
    readBlockSize = getIntJobParameter(jobParameters, "readBlockSize", readBlockSize);
    writeBlockSize = getIntJobParameter(jobParameters, "writeBlockSize", writeBlockSize);
    destinationDirectoryPath = jobParameters.getString("destinationDirectoryPath", destinationDirectoryPath);
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

    if (isBlank(destinationDirectoryPath))
      throw new IllegalArgumentException("The property destinationDirectoryPath can not be null or empty string!");

    if (isBlank(fileNameSuffixColumn))
      throw new IllegalArgumentException("The property fileNameSuffixColumn can not be null or empty string!");

    solr.validate();
    s3Properties().ifPresent(S3Properties::validate);
  }
}
