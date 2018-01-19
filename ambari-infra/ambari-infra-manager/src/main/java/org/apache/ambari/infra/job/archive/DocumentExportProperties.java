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

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.batch.core.JobParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.validation.constraints.Min;

import static org.apache.commons.lang.StringUtils.isBlank;

@Configuration
@PropertySource(value = {"classpath:infra-manager.properties"})
@ConfigurationProperties(prefix = "infra-manager.jobs.solr_data_export")
public class DocumentExportProperties {
  @NotBlank
  private String zooKeeperSocket;
  @Min(1)
  private int readBlockSize;
  @Min(1)
  private int writeBlockSize;
  @NotBlank
  private String destinationDirectoryPath;
  @NotBlank
  private String fileNameSuffixColumn;
  private SolrQueryProperties query;

  public String getZooKeeperSocket() {
    return zooKeeperSocket;
  }

  public void setZooKeeperSocket(String zooKeeperSocket) {
    this.zooKeeperSocket = zooKeeperSocket;
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

  public void apply(JobParameters jobParameters) {
    // TODO: solr query params
    zooKeeperSocket = jobParameters.getString("zooKeeperSocket", zooKeeperSocket);
    readBlockSize = getIntJobParameter(jobParameters, "readBlockSize", readBlockSize);
    writeBlockSize = getIntJobParameter(jobParameters, "writeBlockSize", writeBlockSize);
    destinationDirectoryPath = jobParameters.getString("destinationDirectoryPath", destinationDirectoryPath);
    query.setCollection(jobParameters.getString("collection", query.getCollection()));
    query.setQueryText(jobParameters.getString("queryText", query.getQueryText()));
    query.setFilterQueryText(jobParameters.getString("filterQueryText", query.getFilterQueryText()));
  }

  private int getIntJobParameter(JobParameters jobParameters, String parameterName, int defaultValue) {
    String writeBlockSizeText = jobParameters.getString(parameterName);
    if (isBlank(writeBlockSizeText))
      return defaultValue;
    return this.writeBlockSize = Integer.parseInt(writeBlockSizeText);
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
}
