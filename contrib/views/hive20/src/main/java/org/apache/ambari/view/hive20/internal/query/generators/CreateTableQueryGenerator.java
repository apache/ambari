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

package org.apache.ambari.view.hive20.internal.query.generators;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.ColumnOrder;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class CreateTableQueryGenerator implements QueryGenerator{
  private static final String COMMENT = "COMMENT";
  public static final String ESCAPE_DELIM = "escape.delim";
  public static final String FIELD_DELIM = "field.delim";
  public static final String COLELCTION_DELIM = "colelction.delim";
  public static final String MAPKEY_DELIM = "mapkey.delim";
  public static final String LINE_DELIM = "line.delim";
  public static final String SERIALIZATION_NULL_FORMAT = "serialization.null.format";
  private TableMeta tableMeta;
  public CreateTableQueryGenerator(TableMeta tableMeta) {
    this.tableMeta = tableMeta;
  }

  @Override
  public Optional<String> getQuery(){
    StringBuffer query = new StringBuffer();
    query.append("CREATE TABLE ");
    query.append("`").append(tableMeta.getDatabase()).append("`").append(".");
    query.append("`").append(tableMeta.getTable()).append("`").append(" ");
    query.append("(").append(getColumnQuery(tableMeta.getColumns())).append(") ");
    if(null != tableMeta.getDetailedInfo() && null != tableMeta.getDetailedInfo().getParameters()){
      String tableComment = tableMeta.getDetailedInfo().getParameters().get(COMMENT);
      if(!Strings.isNullOrEmpty(tableComment)){
        tableComment = tableMeta.getDetailedInfo().getParameters().get(COMMENT.toLowerCase());
        if(!Strings.isNullOrEmpty(tableComment)) {
          query.append(" COMMENT ").append(tableComment);
        }
      }
    }
    if(null != tableMeta.getPartitionInfo() ) {
      if (tableMeta.getPartitionInfo().getColumns() != null && !tableMeta.getPartitionInfo().getColumns().isEmpty()) {
        query.append(" PARTITIONED BY ( ").append(getColumnQuery(tableMeta.getPartitionInfo().getColumns())).append(")");
      }
    }
    if(null != tableMeta.getStorageInfo()) {
      if (!QueryGenerationUtils.isNullOrEmpty(tableMeta.getStorageInfo().getBucketCols())) {
        query.append(" CLUSTERED BY (").append(Joiner.on(",").join(tableMeta.getStorageInfo().getBucketCols())).append(")");
      }
      if (!QueryGenerationUtils.isNullOrEmpty(tableMeta.getStorageInfo().getSortCols())) {
        query.append(" SORTED BY (").append(getSortColQuery(tableMeta.getStorageInfo().getSortCols())).append(")");
      }
      if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getNumBuckets())) {
        query.append(" INTO ").append(tableMeta.getStorageInfo().getNumBuckets()).append(" BUCKETS ");
      }
      // TODO : Skewed information not available right now.

      if(!isNullOrEmpty(tableMeta.getStorageInfo().getParameters())) {
        if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(ESCAPE_DELIM)) ||
          !Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(FIELD_DELIM)) ||
          !Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(COLELCTION_DELIM)) ||
          !Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(MAPKEY_DELIM)) ||
          !Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(LINE_DELIM)) ||
          !Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(SERIALIZATION_NULL_FORMAT))
          ) {
          query.append(" ROW FORMAT DELIMITED ");
          if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(FIELD_DELIM))) {
            query.append(" FIELDS TERMINATED BY '").append(tableMeta.getStorageInfo().getParameters().get(FIELD_DELIM)).append("'");
          }
          if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(ESCAPE_DELIM))) {
            query.append(" ESCAPED BY '").append(tableMeta.getStorageInfo().getParameters().get(ESCAPE_DELIM)).append("'");
          }
          if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(COLELCTION_DELIM))) {
            query.append(" COLLECTION ITEMS TERMINATED BY '").append(tableMeta.getStorageInfo().getParameters().get(COLELCTION_DELIM)).append("'");
          }
          if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(MAPKEY_DELIM))) {
            query.append(" MAP KEYS TERMINATED BY '").append(tableMeta.getStorageInfo().getParameters().get(MAPKEY_DELIM)).append("'");
          }
          if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(LINE_DELIM))) {
            query.append(" LINES TERMINATED BY '").append(tableMeta.getStorageInfo().getParameters().get(LINE_DELIM)).append("'");
          }
          if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getParameters().get(SERIALIZATION_NULL_FORMAT))) {
            query.append(" NULL DEFINED AS '").append(tableMeta.getStorageInfo().getParameters().get(SERIALIZATION_NULL_FORMAT)).append("'");
          }
        }
      }

      // STORED AS file_format
      if(!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getFileFormat()) && !tableMeta.getStorageInfo().getFileFormat().trim().isEmpty()){
        query.append(" STORED AS ").append(tableMeta.getStorageInfo().getFileFormat().trim());
      }else if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getInputFormat()) ||
        !Strings.isNullOrEmpty(tableMeta.getStorageInfo().getOutputFormat())
        ) {
        query.append(" STORED AS ");
        if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getInputFormat())) {
          query.append(" INPUTFORMAT '").append(tableMeta.getStorageInfo().getInputFormat()).append("'");
        }
        if (!Strings.isNullOrEmpty(tableMeta.getStorageInfo().getOutputFormat())) {
          query.append(" OUTPUTFORMAT '").append(tableMeta.getStorageInfo().getOutputFormat()).append("'");
        }
      }
    }

    if(null != tableMeta.getDetailedInfo()) {
      if (!Strings.isNullOrEmpty(tableMeta.getDetailedInfo().getLocation())) {
        query.append(" LOCATION '").append(tableMeta.getDetailedInfo().getLocation()).append("'");
      }

      if (QueryGenerationUtils.isNullOrEmpty(tableMeta.getDetailedInfo().getParameters())) {
        String props = QueryGenerationUtils.getPropertiesAsKeyValues(tableMeta.getDetailedInfo().getParameters());

        query.append(" TBLPROPERTIES (").append(props).append(")");
      }
    }

    return Optional.of(query.toString());
  }

  private boolean isNullOrEmpty(Map map) {
    return null == map || map.isEmpty();
  }

  private String getSortColQuery(List<ColumnOrder> sortCols) {
    List<String> sortColsList = FluentIterable.from(sortCols).transform(new Function<ColumnOrder, String>() {
      @Nullable
      @Override
      public String apply(@Nullable ColumnOrder input) {
        return input.getColumnName() + " " + input.getOrder().name();
      }
    }).toList();
    return Joiner.on(",").join(sortColsList);
  }

  private String getColumnQuery(List<ColumnInfo> columns) {
    List<String> columnQuery = FluentIterable.from(columns).transform(new Function<ColumnInfo, String>() {
      @Nullable
      @Override
      public String apply(@Nullable ColumnInfo column) {
        return QueryGenerationUtils.getColumnRepresentation(column);
      }
    }).toList();

    return Joiner.on(",").join(columnQuery);
  }

}
