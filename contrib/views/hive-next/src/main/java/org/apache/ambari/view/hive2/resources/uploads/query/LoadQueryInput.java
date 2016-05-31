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

package org.apache.ambari.view.hive2.resources.uploads.query;

/**
 * input for QueryGenerator for generating Load From Query
 */
public class LoadQueryInput {
  private String hdfsFilePath;
  private String databaseName;
  private String tableName;

  public LoadQueryInput(String hdfsFilePath, String databaseName, String tableName) {
    this.hdfsFilePath = hdfsFilePath;
    this.databaseName = databaseName;
    this.tableName = tableName;
  }

  public String getHdfsFilePath() {
    return hdfsFilePath;
  }

  public void setHdfsFilePath(String hdfsFilePath) {
    this.hdfsFilePath = hdfsFilePath;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  @Override
  public String toString() {
    return "LoadQueryInput{" +
            "hdfsFilePath='" + hdfsFilePath + '\'' +
            ", databaseName='" + databaseName + '\'' +
            ", tableName='" + tableName + '\'' +
            '}';
  }
}
