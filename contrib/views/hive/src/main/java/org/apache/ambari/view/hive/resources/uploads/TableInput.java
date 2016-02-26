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

package org.apache.ambari.view.hive.resources.uploads;

import java.util.List;

/**
 * used as input in REST call
 */
class TableInput {
  public Boolean isFirstRowHeader;
  public List<ColumnDescriptionImpl> header;
  public String tableName;
  public String databaseName;
  /**
   * the format of the file created for the table inside hive : ORC TEXTFILE etc.
   */
  public String fileType;
  /**
   * the format of the file uploaded. CSV, JSON, XML etc.
   */
  public String fileFormat;

  public TableInput() {
  }

  public Boolean getIsFirstRowHeader() {
    return isFirstRowHeader;
  }

  public void setIsFirstRowHeader(Boolean isFirstRowHeader) {
    this.isFirstRowHeader = isFirstRowHeader;
  }

  public List<ColumnDescriptionImpl> getHeader() {
    return header;
  }

  public void setHeader(List<ColumnDescriptionImpl> header) {
    this.header = header;
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

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getFileFormat() {
    return fileFormat;
  }

  public void setFileFormat(String fileFormat) {
    this.fileFormat = fileFormat;
  }
}
