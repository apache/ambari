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

import org.apache.ambari.view.hive2.client.ColumnDescription;
import org.apache.ambari.view.hive2.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive2.resources.uploads.HiveFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * generates the sql query from given data
 */
public class QueryGenerator {
  protected final static Logger LOG =
          LoggerFactory.getLogger(QueryGenerator.class);

  public String generateCreateQuery(TableInfo tableInfo) {
    String tableName = tableInfo.getTableName();
    List<ColumnDescriptionImpl> cdList = tableInfo.getHeader();

    StringBuilder query = new StringBuilder();
    query.append("CREATE TABLE ").append(tableName).append(" (");
    Collections.sort(cdList, new Comparator<ColumnDescription>() {
      @Override
      public int compare(ColumnDescription o1, ColumnDescription o2) {
        return o1.getPosition() - o2.getPosition();
      }
    });

    boolean first = true;
    for (ColumnDescriptionImpl cd : cdList) {
      if (first) {
        first = false;
      } else {
        query.append(", ");
      }

      query.append(cd.getName()).append(" ").append(cd.getType());
      if (cd.getPrecision() != null) {
        query.append("(").append(cd.getPrecision());
        if (cd.getScale() != null) {
          query.append(",").append(cd.getScale());
        }
        query.append(")");
      }

    }

    query.append(")");

    if(tableInfo.getHiveFileType().equals(HiveFileType.TEXTFILE)) {
      query.append(getRowFormatQuery(tableInfo.getRowFormat()));
    }
    query.append(" STORED AS ").append(tableInfo.getHiveFileType().toString());
    String queryString = query.append(";").toString();
    LOG.info("Query : {}", queryString);
    return queryString;
  }

  private String getRowFormatQuery(RowFormat rowFormat) {
    StringBuilder sb = new StringBuilder();
    if(rowFormat != null) {
      sb.append(" ROW FORMAT DELIMITED");
      if(rowFormat.getFieldsTerminatedBy() != null ){
        sb.append(" FIELDS TERMINATED BY '").append(rowFormat.getFieldsTerminatedBy()).append('\'');
      }
      if(rowFormat.getEscapedBy() != null){
        String escape = String.valueOf(rowFormat.getEscapedBy());
        if(rowFormat.getEscapedBy() == '\\'){
          escape = escape + '\\'; // special handling of slash as its escape char for strings in hive as well.
        }
        sb.append(" ESCAPED BY '").append(escape).append('\'');
      }
    }

    return sb.toString();
  }

  public String generateInsertFromQuery(InsertFromQueryInput ifqi) {
    StringBuilder insertQuery = new StringBuilder("INSERT INTO TABLE ").append(ifqi.getToDatabase()).append(".")
                                .append(ifqi.getToTable()).append(" SELECT ");

    boolean first = true;
    for(ColumnDescriptionImpl column : ifqi.getHeader()){
      String type = column.getType();
      boolean unhex = ifqi.getUnhexInsert() && (
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

      insertQuery.append(column.getName());

      if(unhex) {
        insertQuery.append(")");
      }

      first = false;
    }

    insertQuery.append(" FROM ").append(ifqi.getFromDatabase()).append(".").append(ifqi.getFromTable()).append(";");
    String query = insertQuery.toString();
    LOG.info("Insert Query : {}", query);
    return query;
  }

  public String generateDropTableQuery(DeleteQueryInput deleteQueryInput) {
    String dropQuery = new StringBuilder("DROP TABLE ").append(deleteQueryInput.getDatabase())
                      .append(".").append(deleteQueryInput.getTable()).append(";").toString();
    LOG.info("Drop Query : {}", dropQuery);
    return dropQuery;
  }
}
