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
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;

import javax.annotation.Nullable;

import static org.apache.ambari.view.hive20.internal.query.generators.QueryGenerationUtils.isNullOrEmpty;

public class AnalyzeTableQueryGenerator implements QueryGenerator {
  private TableMeta tableMeta;
  private final Boolean shouldAnalyzeColumns;

  public AnalyzeTableQueryGenerator(TableMeta tableMeta, Boolean shouldAnalyzeColumns) {
    this.tableMeta = tableMeta;
    this.shouldAnalyzeColumns = shouldAnalyzeColumns;
  }


  @Override
  public Optional<String> getQuery() throws ServiceException {
    String query = getTableStatsQuery() + ";";
    if(shouldAnalyzeColumns){
      query += "\n" + getTableStatsQuery() + " FOR COLUMNS ;";
    }

    return Optional.of(query);
  }

  private String getTableStatsQuery() {
    StringBuilder query = new StringBuilder("ANALYZE TABLE " );
    query.append("`").append(tableMeta.getDatabase()).append("`").append(".").append("`").append(tableMeta.getTable()).append("`");

    if( null != tableMeta.getPartitionInfo() && !isNullOrEmpty(tableMeta.getPartitionInfo().getColumns())){
      query.append(" PARTITION (")
        .append(Joiner.on(",")
          .join(FluentIterable.from(tableMeta.getPartitionInfo().getColumns())
            .transform(
              new Function<ColumnInfo, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable ColumnInfo columnInfo) {
                  return "`" + columnInfo.getName() + "`";
                }
              })
          )
        )
        .append(")");
    }


    query.append(" COMPUTE STATISTICS ");
    return query.toString();
  }
}
