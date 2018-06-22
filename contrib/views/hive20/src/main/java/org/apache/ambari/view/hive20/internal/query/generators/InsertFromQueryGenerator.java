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
import org.apache.ambari.view.hive20.client.ColumnDescription;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.resources.uploads.query.InsertFromQueryInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public class InsertFromQueryGenerator implements QueryGenerator{
  protected final static Logger LOG =
      LoggerFactory.getLogger(InsertFromQueryGenerator.class);

  private InsertFromQueryInput insertFromQueryInput;

  public InsertFromQueryGenerator(InsertFromQueryInput insertFromQueryInput) {
    this.insertFromQueryInput = insertFromQueryInput;
  }

  @Override
  public Optional<String> getQuery() throws ServiceException {
    StringBuilder insertQuery = new StringBuilder();
    //Dynamic partition strict mode requires at least one static partition column. To turn this off set hive.exec.dynamic.partition.mode=nonstrict
    insertQuery.append("set hive.exec.dynamic.partition.mode=nonstrict;").append("\n");

    insertQuery.append(" FROM ").append("`").append(insertFromQueryInput.getFromDatabase()).append("`.`")
        .append(insertFromQueryInput.getFromTable()).append("` tempTable");

    insertQuery.append(" INSERT INTO TABLE `").append(insertFromQueryInput.getToDatabase()).append('`').append(".")
        .append("`").append(insertFromQueryInput.getToTable()).append("`");
        // PARTITION (partcol1[=val1], partcol2[=val2] ...)
        if(insertFromQueryInput.getPartitionedColumns() != null && insertFromQueryInput.getPartitionedColumns().size() > 0){
          insertQuery.append(" PARTITION ").append("(");
          insertQuery.append(Joiner.on(",").join(FluentIterable.from(insertFromQueryInput.getPartitionedColumns()).transform(new Function<ColumnInfo, String>() {
            @Override
            public String apply(ColumnInfo columnInfo) {
              return "`" + columnInfo.getName() + "`";
            }
          })));
          insertQuery.append(" ) ");
        }

    insertQuery.append(" SELECT ");

    List<ColumnInfo> allColumns = new LinkedList<>(insertFromQueryInput.getNormalColumns());
    // this order matters or first normal columns and in the last partitioned columns matters.
    allColumns.addAll(insertFromQueryInput.getPartitionedColumns());
    boolean first = true;
    for(ColumnInfo column : allColumns){
      String type = column.getType();
      boolean unhex = insertFromQueryInput.getUnhexInsert() && (
          ColumnDescription.DataTypes.STRING.toString().equals(type)
              || ColumnDescription.DataTypes.VARCHAR.toString().equals(type)
              || ColumnDescription.DataTypes.CHAR.toString().equals(type)
      );

      if(!first){
        insertQuery.append(", ");
      }

      if(unhex) {
        insertQuery.append("UNHEX(");
      }

      insertQuery.append("tempTable.");
      insertQuery.append('`').append(column.getName()).append('`');

      if(unhex) {
        insertQuery.append(")");
      }

      first = false;
    }

    insertQuery.append(";");
    String query = insertQuery.toString();
    LOG.info("Insert From Query : {}", query);
    return Optional.of(query);
  }
}
