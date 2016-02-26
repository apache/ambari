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

package org.apache.ambari.view.hive.resources.uploads.query;

import org.apache.ambari.view.hive.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive.resources.uploads.HiveFileType;

import java.util.List;

/**
 * used as input in Query generation
 */
public class TableInfo {
  private String tableName;
  private String databaseName;
  private List<ColumnDescriptionImpl> columns;
  private HiveFileType hiveFileType;

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

  public List<ColumnDescriptionImpl> getColumns() {
    return columns;
  }

  public void setColumns(List<ColumnDescriptionImpl> columns) {
    this.columns = columns;
  }

  public HiveFileType getHiveFileType() {
    return hiveFileType;
  }

  public void setHiveFileType(HiveFileType hiveFileType) {
    this.hiveFileType = hiveFileType;
  }

  public TableInfo(String databaseName, String tableName, List<ColumnDescriptionImpl> columns, HiveFileType hiveFileType) {
    this.tableName = tableName;
    this.databaseName = databaseName;
    this.columns = columns;
    this.hiveFileType = hiveFileType;
  }

  public TableInfo(TableInfo tableInfo) {
    this.tableName = tableInfo.tableName;
    this.databaseName = tableInfo.databaseName;
    this.columns = tableInfo.columns;
    this.hiveFileType = tableInfo.hiveFileType;
  }

  public TableInfo() {
  }
}
