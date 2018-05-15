/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.ambari.view.hive20.internal.dto;

public class ColumnStats {
  public static final String COLUMN_NAME = "# col_name";
  public static final String DATA_TYPE = "data_type";
  public static final String MIN = "min";
  public static final String MAX = "max";
  public static final String NUM_NULLS = "num_nulls";
  public static final String DISTINCT_COUNT = "distinct_count";
  public static final String AVG_COL_LEN = "avg_col_len";
  public static final String MAX_COL_LEN = "max_col_len";
  public static final String NUM_TRUES = "num_trues";
  public static final String NUM_FALSES = "num_falses";
  public static final String COMMENT = "comment";

  private String databaseName;
  private String tableName;
  private String columnName;
  private String dataType;
  private String min;
  private String max;
  private String numNulls;
  private String distinctCount;
  private String avgColLen;
  private String maxColLen;
  private String numTrues;
  private String numFalse;
  private String comment;
  private String columnStatsAccurate;

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

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getMin() {
    return min;
  }

  public void setMin(String min) {
    this.min = min;
  }

  public String getMax() {
    return max;
  }

  public void setMax(String max) {
    this.max = max;
  }

  public String getNumNulls() {
    return numNulls;
  }

  public void setNumNulls(String numNulls) {
    this.numNulls = numNulls;
  }

  public String getDistinctCount() {
    return distinctCount;
  }

  public void setDistinctCount(String distinctCount) {
    this.distinctCount = distinctCount;
  }

  public String getAvgColLen() {
    return avgColLen;
  }

  public void setAvgColLen(String avgColLen) {
    this.avgColLen = avgColLen;
  }

  public String getMaxColLen() {
    return maxColLen;
  }

  public void setMaxColLen(String maxColLen) {
    this.maxColLen = maxColLen;
  }

  public String getNumTrues() {
    return numTrues;
  }

  public void setNumTrues(String numTrues) {
    this.numTrues = numTrues;
  }

  public String getNumFalse() {
    return numFalse;
  }

  public void setNumFalse(String numFalse) {
    this.numFalse = numFalse;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ColumnStats{");
    sb.append("tableName='").append(tableName).append('\'');
    sb.append(", columnName='").append(columnName).append('\'');
    sb.append(", dataType='").append(dataType).append('\'');
    sb.append(", min='").append(min).append('\'');
    sb.append(", max='").append(max).append('\'');
    sb.append(", numNulls='").append(numNulls).append('\'');
    sb.append(", distinctCount='").append(distinctCount).append('\'');
    sb.append(", avgColLen='").append(avgColLen).append('\'');
    sb.append(", maxColLen='").append(maxColLen).append('\'');
    sb.append(", numTrues='").append(numTrues).append('\'');
    sb.append(", numFalse='").append(numFalse).append('\'');
    sb.append(", comment='").append(comment).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public void setColumnStatsAccurate(String columnStatsAccurate) {
    this.columnStatsAccurate = columnStatsAccurate;
  }
}
