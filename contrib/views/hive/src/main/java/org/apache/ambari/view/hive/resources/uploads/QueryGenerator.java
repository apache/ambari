/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.uploads;

import org.apache.ambari.view.hive.client.ColumnDescription;
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
    List<ColumnDescription> cdList = tableInfo.getColumns();

    StringBuilder query = new StringBuilder();
    query.append("create table " + tableName + " (");
    Collections.sort(cdList, new Comparator<ColumnDescription>() {
      @Override
      public int compare(ColumnDescription o1, ColumnDescription o2) {
        return o1.getPosition() - o2.getPosition();
      }
    });

    boolean first = true;
    for (ColumnDescription cd : cdList) {
      if (first) {
        first = false;
      } else {
        query.append(", ");
      }

      query.append(cd.getName() + " " + cd.getType());
    }

    query.append(") ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE;");

    String queryString = query.toString();
    LOG.info("Query : %S", queryString);
    return queryString;
  }
}
