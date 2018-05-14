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

import java.io.Serializable;
import java.util.List;

/**
 *
 */
public class TableMeta implements Serializable{
  private String id;
  private String database;
  private String table;
  private List<ColumnInfo> columns;
  private String ddl;
  private PartitionInfo partitionInfo;
  private DetailedTableInfo detailedInfo;
  private TableStats tableStats;
  private StorageInfo storageInfo;
  private ViewInfo viewInfo;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public List<ColumnInfo> getColumns() {
    return columns;
  }

  public void setColumns(List<ColumnInfo> columns) {
    this.columns = columns;
  }

  public String getDdl() {
    return ddl;
  }

  public void setDdl(String ddl) {
    this.ddl = ddl;
  }

  public PartitionInfo getPartitionInfo() {
    return partitionInfo;
  }

  public void setPartitionInfo(PartitionInfo partitionInfo) {
    this.partitionInfo = partitionInfo;
  }

  public DetailedTableInfo getDetailedInfo() {
    return detailedInfo;
  }

  public void setDetailedInfo(DetailedTableInfo detailedInfo) {
    this.detailedInfo = detailedInfo;
  }

  public StorageInfo getStorageInfo() {
    return storageInfo;
  }

  public void setStorageInfo(StorageInfo storageInfo) {
    this.storageInfo = storageInfo;
  }

  public ViewInfo getViewInfo() {
    return viewInfo;
  }

  public void setViewInfo(ViewInfo viewInfo) {
    this.viewInfo = viewInfo;
  }

  public TableStats getTableStats() {
    return tableStats;
  }

  public void setTableStats(TableStats tableStats) {
    this.tableStats = tableStats;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TableMeta{");
    sb.append("id='").append(id).append('\'');
    sb.append(", database='").append(database).append('\'');
    sb.append(", table='").append(table).append('\'');
    sb.append(", columns=").append(columns);
    sb.append(", ddl='").append(ddl).append('\'');
    sb.append(", partitionInfo=").append(partitionInfo);
    sb.append(", detailedInfo=").append(detailedInfo);
    sb.append(", storageInfo=").append(storageInfo);
    sb.append(", viewInfo=").append(viewInfo);
    sb.append('}');
    return sb.toString();
  }
}
