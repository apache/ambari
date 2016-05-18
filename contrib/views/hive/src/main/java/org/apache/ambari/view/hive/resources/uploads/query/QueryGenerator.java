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

import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.resources.uploads.*;
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
    List<ColumnDescriptionImpl> cdList = tableInfo.getColumns();

    StringBuilder query = new StringBuilder();
    query.append("create table " + tableName + " (");
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

      query.append(cd.getName() + " " + cd.getType());
      if (cd.getPrecision() != null) {
        query.append("(").append(cd.getPrecision());
        if (cd.getScale() != null) {
          query.append(",").append(cd.getScale());
        }
        query.append(")");
      }

    }

    query.append(")");

    if (tableInfo.getHiveFileType() == HiveFileType.TEXTFILE)
      query.append(" ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE;");
    else
      query.append(" STORED AS " + tableInfo.getHiveFileType() + ";");

    String queryString = query.toString();
    LOG.info("Query : {}", queryString);
    return queryString;
  }

  public String generateInsertFromQuery(InsertFromQueryInput ifqi) {
    String insertQuery = "insert into table " + ifqi.getToDatabase() + "." + ifqi.getToTable() + " select * from " + ifqi.getFromDatabase() + "." + ifqi.getFromTable();
    LOG.info("Insert Query : {}", insertQuery);
    return insertQuery;
  }

  public String generateDropTableQuery(DeleteQueryInput deleteQueryInput) {
    String dropQuery = "drop table " + deleteQueryInput.getDatabase() + "." + deleteQueryInput.getTable();
    LOG.info("Drop Query : {}", dropQuery);
    return dropQuery;
  }

  public String generateLoadQuery(LoadQueryInput loadQueryInput) {
    String loadFromQuery = "LOAD DATA INPATH '"  + loadQueryInput.getHdfsFilePath() + "' INTO TABLE " + loadQueryInput.getDatabaseName() + "." + loadQueryInput.getTableName() + ";" ;
    LOG.info("Load From Query : {}", loadFromQuery);
    return loadFromQuery;
  }
}
