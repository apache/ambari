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

import java.io.Serializable;

public class UploadFromHdfsInput implements Serializable{
  private Boolean isFirstRowHeader;
  private String inputFileType;
  private String hdfsPath;
  private String tableName;
  private String databaseName;

  public UploadFromHdfsInput() {
  }

  public UploadFromHdfsInput(Boolean isFirstRowHeader, String inputFileType, String hdfsPath, String tableName, String databaseName) {
    this.isFirstRowHeader = isFirstRowHeader;
    this.inputFileType = inputFileType;
    this.hdfsPath = hdfsPath;
    this.tableName = tableName;
    this.databaseName = databaseName;
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
    return "UploadFromHdfsInput{" +
            "isFirstRowHeader=" + isFirstRowHeader +
            ", inputFileType='" + inputFileType + '\'' +
            ", hdfsPath='" + hdfsPath + '\'' +
            ", tableName='" + tableName + '\'' +
            ", databaseName='" + databaseName + '\'' +
            '}';
  }
}
