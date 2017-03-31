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
import org.apache.ambari.view.hive20.internal.dto.PartitionInfo;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PartitionInfoParser extends AbstractTableMetaParser<PartitionInfo> {
  private static final Logger LOG = LoggerFactory.getLogger(PartitionInfoParser.class);

  /*
      General format
      | # Partition Information       | NULL                                                                          | NULL                         |
      | # col_name                    | data_type                                                                     | comment                      |
      |                               | NULL                                                                          | NULL                         |
      | dt                            | string                                                                        |                              |
      | country                       | string                                                                        |                              |
      |                               | NULL                                                                          | NULL                         |
     */

  public PartitionInfoParser() {
    super("# Partition Information", "# col_name", "", "");
  }

  @Override
  public PartitionInfo parse(List<Row> rows) {
    List<ColumnInfo> columns = new ArrayList<>();


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
        LOG.debug("found partition column definition : {}", columnInfo);
      }
    }
    return columns.size() > 0? new PartitionInfo(columns) : null;
  }
}
