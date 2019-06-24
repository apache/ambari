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

import static java.util.Objects.requireNonNull;
import static org.apache.ambari.infra.job.archive.ExportDestination.HDFS;
import static org.apache.ambari.infra.job.archive.ExportDestination.LOCAL;
import static org.apache.ambari.infra.json.StringToDurationConverter.toDuration;
import static org.apache.ambari.infra.json.StringToFsPermissionConverter.toFsPermission;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.time.Duration;
import java.util.Optional;

import org.apache.ambari.infra.job.JobProperties;
import org.apache.ambari.infra.job.Validatable;
import org.apache.ambari.infra.json.DurationToStringConverter;
import org.apache.ambari.infra.json.FsPermissionToStringConverter;
import org.apache.ambari.infra.json.StringToDurationConverter;
import org.apache.ambari.infra.json.StringToFsPermissionConverter;
import org.apache.hadoop.fs.permission.FsPermission;
import org.springframework.batch.core.JobParameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ArchivingProperties extends JobProperties<ArchivingProperties> implements Validatable {
  private int readBlockSize;
  private int writeBlockSize;
  private ExportDestination destination;
  private String localDestinationDirectory;
  private String fileNameSuffixColumn;
  private String fileNameSuffixDateFormat;
  private SolrProperties solr;
  private String hdfsEndpoint;
  private String hdfsDestinationDirectory;
  @JsonSerialize(converter = FsPermissionToStringConverter.class)
  @JsonDeserialize(converter = StringToFsPermissionConverter.class)
  private FsPermission hdfsFilePermission;
  private String hdfsKerberosPrincipal;
  private String hdfsKerberosKeytabPath;
  private String start;
  private String end;
  @JsonSerialize(converter = DurationToStringConverter.class)
  @JsonDeserialize(converter = StringToDurationConverter.class)
  private Duration ttl;

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

  public void setSolr(SolrProperties solr) {
    this.solr = solr;
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

  public FsPermission getHdfsFilePermission() {
    return hdfsFilePermission;
  }

  public void setHdfsFilePermission(FsPermission hdfsFilePermission) {
    this.hdfsFilePermission = hdfsFilePermission;
  }

  public String getHdfsKerberosPrincipal() {
    return hdfsKerberosPrincipal;
  }

  public void setHdfsKerberosPrincipal(String hdfsKerberosPrincipal) {
    this.hdfsKerberosPrincipal = hdfsKerberosPrincipal;
  }

  public String getHdfsKerberosKeytabPath() {
    return hdfsKerberosKeytabPath;
  }

  public void setHdfsKerberosKeytabPath(String hdfsKerberosKeytabPath) {
    this.hdfsKerberosKeytabPath = hdfsKerberosKeytabPath;
  }

  public Optional<HdfsProperties> hdfsProperties() {
    if (isBlank(hdfsDestinationDirectory))
      return Optional.empty();

    return Optional.of(new HdfsProperties(
            hdfsEndpoint,
            hdfsDestinationDirectory,
            hdfsFilePermission,
            hdfsKerberosPrincipal,
            hdfsKerberosKeytabPath));
  }

  public String getStart() {
    return start;
  }

  public void setStart(String start) {
    this.start = start;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }

  public Duration getTtl() {
    return ttl;
  }

  public void setTtl(Duration ttl) {
    this.ttl = ttl;
  }

  @Override
  public void validate() {
    if (readBlockSize <= 0)
      throw new IllegalArgumentException("The property readBlockSize must be greater than 0!");

    if (writeBlockSize <= 0)
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

      case HDFS:
        hdfsProperties()
                .orElseThrow(() -> new IllegalArgumentException("HDFS related properties must be set if the destination is " + HDFS.name()))
                .validate();
    }

    requireNonNull(solr, "No solr query was specified for archiving job!");
    solr.validate();
  }

  @Override
  public ArchivingProperties merge(JobParameters jobParameters) {
    ArchivingProperties archivingProperties = new ArchivingProperties();
    archivingProperties.setReadBlockSize(getIntJobParameter(jobParameters, "readBlockSize", readBlockSize));
    archivingProperties.setWriteBlockSize(getIntJobParameter(jobParameters, "writeBlockSize", writeBlockSize));
    archivingProperties.setDestination(ExportDestination.valueOf(jobParameters.getString("destination", destination.name())));
    archivingProperties.setLocalDestinationDirectory(jobParameters.getString("localDestinationDirectory", localDestinationDirectory));
    archivingProperties.setFileNameSuffixColumn(jobParameters.getString("fileNameSuffixColumn", fileNameSuffixColumn));
    archivingProperties.setFileNameSuffixDateFormat(jobParameters.getString("fileNameSuffixDateFormat", fileNameSuffixDateFormat));
    archivingProperties.setHdfsEndpoint(jobParameters.getString("hdfsEndpoint", hdfsEndpoint));
    archivingProperties.setHdfsDestinationDirectory(jobParameters.getString("hdfsDestinationDirectory", hdfsDestinationDirectory));
    archivingProperties.setHdfsFilePermission(toFsPermission(jobParameters.getString("hdfsFilePermission", FsPermissionToStringConverter.toString(hdfsFilePermission))));
    archivingProperties.setHdfsKerberosPrincipal(jobParameters.getString("hdfsKerberosPrincipal", hdfsKerberosPrincipal));
    archivingProperties.setHdfsKerberosKeytabPath(jobParameters.getString("hdfsKerberosKeytabPath", hdfsKerberosKeytabPath));
    archivingProperties.setSolr(solr.merge(jobParameters));
    archivingProperties.setStart(jobParameters.getString("start"));
    archivingProperties.setEnd(jobParameters.getString("end"));
    archivingProperties.setTtl(toDuration(jobParameters.getString("ttl", DurationToStringConverter.toString(ttl))));
    return archivingProperties;
  }

  private int getIntJobParameter(JobParameters jobParameters, String parameterName, int defaultValue) {
    String valueText = jobParameters.getString(parameterName);
    if (isBlank(valueText))
      return defaultValue;
    return Integer.parseInt(valueText);
  }
}
