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

import org.apache.ambari.view.hive2.resources.uploads.query.RowFormat;
import org.apache.ambari.view.hive2.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive2.resources.uploads.HiveFileType;

import java.io.Serializable;
import java.util.List;

/**
 * used as input in Query generation
 */
public class TableInfo implements Serializable{
  private String tableName;
  private String databaseName;
  private List<ColumnDescriptionImpl> header;
  private HiveFileType hiveFileType;

  private RowFormat rowFormat;

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

  public List<ColumnDescriptionImpl> getHeader() {
    return header;
  }

  public void setHeader(List<ColumnDescriptionImpl> header) {
    this.header = header;
  }

  public HiveFileType getHiveFileType() {
    return hiveFileType;
  }

  public void setHiveFileType(HiveFileType hiveFileType) {
    this.hiveFileType = hiveFileType;
  }

  public RowFormat getRowFormat() {
    return rowFormat;
  }

  public void setRowFormat(RowFormat rowFormat) {
    this.rowFormat = rowFormat;
  }

  public TableInfo(String databaseName, String tableName, List<ColumnDescriptionImpl> header, HiveFileType hiveFileType, RowFormat rowFormat) {
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.header = header;
    this.hiveFileType = hiveFileType;
    this.rowFormat = rowFormat;
  }

  public TableInfo(TableInfo tableInfo) {
    this.tableName = tableInfo.tableName;
    this.databaseName = tableInfo.databaseName;
    this.header = tableInfo.header;
    this.hiveFileType = tableInfo.hiveFileType;
    this.rowFormat = tableInfo.rowFormat;
  }

  public TableInfo() {
  }
}
