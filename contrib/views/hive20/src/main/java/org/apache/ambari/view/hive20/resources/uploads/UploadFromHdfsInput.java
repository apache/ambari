/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.uploads;

import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;

import java.io.Serializable;
import java.util.List;

public class UploadFromHdfsInput implements Serializable{
  private Boolean isFirstRowHeader = Boolean.FALSE;
  private String inputFileType;
  private String hdfsPath;
  private String tableName;
  private String databaseName;
  private List<ColumnInfo> header;
  private boolean containsEndlines;

  private String csvDelimiter;
  private String csvEscape;
  private String csvQuote;

  public UploadFromHdfsInput() {
  }

  public String getCsvDelimiter() {
    return csvDelimiter;
  }

  public List<ColumnInfo> getHeader() {
    return header;
  }

  public void setHeader(List<ColumnInfo> header) {
    this.header = header;
  }

  public boolean isContainsEndlines() {
    return containsEndlines;
  }

  public void setContainsEndlines(boolean containsEndlines) {
    this.containsEndlines = containsEndlines;
  }

  public void setCsvDelimiter(String csvDelimiter) {
    this.csvDelimiter = csvDelimiter;
  }

  public String getCsvEscape() {
    return csvEscape;
  }

  public void setCsvEscape(String csvEscape) {
    this.csvEscape = csvEscape;
  }

  public String getCsvQuote() {
    return csvQuote;
  }

  public void setCsvQuote(String csvQuote) {
    this.csvQuote = csvQuote;
  }

  public Boolean getIsFirstRowHeader() {
    return isFirstRowHeader;
  }

  public void setIsFirstRowHeader(Boolean firstRowHeader) {
    isFirstRowHeader = firstRowHeader;
  }

  public String getInputFileType() {
    return inputFileType;
  }

  public void setInputFileType(String inputFileType) {
    this.inputFileType = inputFileType;
  }

  public String getHdfsPath() {
    return hdfsPath;
  }

  public void setHdfsPath(String hdfsPath) {
    this.hdfsPath = hdfsPath;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  @Override
  public String toString() {
    return new StringBuilder("UploadFromHdfsInput{" )
            .append("isFirstRowHeader=").append( isFirstRowHeader )
            .append(", inputFileType='" ).append(inputFileType)
            .append(", hdfsPath='").append(hdfsPath)
            .append(", tableName='").append( tableName )
            .append(", databaseName='").append(databaseName )
            .append('}').toString();
  }
}
