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

package org.apache.ambari.view.hive20.internal.parsers;

import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses the columns from the output of 'describe formatted ${tableName}' output
 */
public class ColumnInfoParser extends AbstractTableMetaParser<List<ColumnInfo>> {
  private static final Logger LOG = LoggerFactory.getLogger(ColumnInfoParser.class);

  public ColumnInfoParser() {
    super("# col_name", "", "");
  }

  @Override
  public List<ColumnInfo> parse(List<Row> rows) {
    List<ColumnInfo> columns = new ArrayList<>();
    /* General Format: Starts from the first index itself
     | # col_name                    | data_type                                                                     | comment                      |
     |                               | NULL                                                                          | NULL                         |
     | viewtime                      | int                                                                           |                              |
     | userid                        | bigint                                                                        |                              |
     | page_url                      | string                                                                        |                              |
     | referrer_url                  | string                                                                        |                              |
     | ip                            | string                                                                        | IP Address of the User       |
     |                               | NULL                                                                          | NULL                         |
     */

    /*Iterator<Row> iterator = rows.iterator();
    int index = 0;
    // Skip first two rows
    while (index < 2) {
      iterator.next();
      index++;
    }

    while (true) {
      Row row = iterator.next();
      // Columns section ends with a empty column name value
      if (index >= rows.size() || "".equalsIgnoreCase((String) row.getRow()[0]))
        break;

      String colName = (String)row.getRow()[0];
      String colType = (String)row.getRow()[1];
      String colComment = (String)row.getRow()[2];

      columns.add(new ColumnInfo(colName, colType, colComment));
      index++;
    }*/


    Map<String, Object> parsedSection = parseSection(rows);
    for(Object obj: parsedSection.values()) {
      if(obj instanceof Entry) {
        Entry entry = (Entry)obj;
        String typeInfo = entry.getValue();
        // parse precision and scale
        List<String> typePrecisionScale = ParserUtils.parseColumnDataType(typeInfo);
        String datatype = typePrecisionScale.get(0);
        String precisionString = typePrecisionScale.get(1);
        String scaleString = typePrecisionScale.get(2);
        Integer precision = !Strings.isNullOrEmpty(precisionString) ? Integer.valueOf(precisionString.trim()): null;
        Integer scale = !Strings.isNullOrEmpty(scaleString) ? Integer.valueOf(scaleString.trim()): null;
        ColumnInfo columnInfo = new ColumnInfo(entry.getName(), datatype, precision, scale, entry.getComment());
        columns.add(columnInfo);
        LOG.debug("found column definition : {}", columnInfo);
      }
    }
    return columns;
  }
}
