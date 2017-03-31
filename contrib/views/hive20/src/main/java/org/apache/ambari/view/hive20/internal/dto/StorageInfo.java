/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal.dto;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class StorageInfo {
  private String serdeLibrary;
  private String inputFormat;
  private String outputFormat;
  private String compressed;
  private String numBuckets;
  private List<String> bucketCols;
  private List<ColumnOrder> sortCols;
  private String fileFormat;
  private Map<String, String> parameters;

  public String getFileFormat() {
    return fileFormat;
  }

  public void setFileFormat(String fileFormat) {
    this.fileFormat = fileFormat;
  }

  public String getSerdeLibrary() {
    return serdeLibrary;
  }

  public void setSerdeLibrary(String serdeLibrary) {
    this.serdeLibrary = serdeLibrary;
  }

  public String getInputFormat() {
    return inputFormat;
  }

  public void setInputFormat(String inputFormat) {
    this.inputFormat = inputFormat;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getCompressed() {
    return compressed;
  }

  public void setCompressed(String compressed) {
    this.compressed = compressed;
  }

  public String getNumBuckets() {
    return numBuckets;
  }

  public void setNumBuckets(String numBuckets) {
    this.numBuckets = numBuckets;
  }

  public List<String> getBucketCols() {
    return bucketCols;
  }

  public void setBucketCols(List<String> bucketCols) {
    this.bucketCols = bucketCols;
  }

  public List<ColumnOrder> getSortCols() {
    return sortCols;
  }

  public void setSortCols(List<ColumnOrder> sortCols) {
    this.sortCols = sortCols;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  @Override
  public String toString() {
    return "StorageInfo{" +
        "serdeLibrary='" + serdeLibrary + '\'' +
        ", inputFormat='" + inputFormat + '\'' +
        ", outputFormat='" + outputFormat + '\'' +
        ", compressed='" + compressed + '\'' +
        ", numBuckets='" + numBuckets + '\'' +
        ", bucketCols='" + bucketCols + '\'' +
        ", sortCols='" + sortCols + '\'' +
        ", fileFormat='" + fileFormat + '\'' +
        ", parameters=" + parameters +
        '}';
  }
}
