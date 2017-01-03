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
import org.apache.ambari.view.hive20.internal.dto.DetailedTableInfo;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class DetailedTableInfoParser extends AbstractTableMetaParser<DetailedTableInfo> {
  /*
    | # Detailed Table Information  | NULL                                                                 | NULL                                                                                                                                                                                                                                                              |
    | Database:                     | default                                                              | NULL                                                                                                                                                                                                                                                              |
    | Owner:                        | admin                                                                | NULL                                                                                                                                                                                                                                                              |
    | CreateTime:                   | Mon Aug 01 13:28:42 UTC 2016                                         | NULL                                                                                                                                                                                                                                                              |
    | LastAccessTime:               | UNKNOWN                                                              | NULL                                                                                                                                                                                                                                                              |
    | Protect Mode:                 | None                                                                 | NULL                                                                                                                                                                                                                                                              |
    | Retention:                    | 0                                                                    | NULL                                                                                                                                                                                                                                                              |
    | Location:                     | hdfs://c6401.ambari.apache.org:8020/apps/hive/warehouse/geolocation  | NULL                                                                                                                                                                                                                                                              |
    | Table Type:                   | MANAGED_TABLE                                                        | NULL                                                                                                                                                                                                                                                              |
    | Table Parameters:             | NULL                                                                 | NULL                                                                                                                                                                                                                                                              |
    |                               | COLUMN_STATS_ACCURATE                                                | {\"BASIC_STATS\":\"true\",\"COLUMN_STATS\":{\"column1\":\"true\",\"column2\":\"true\",\"column3\":\"true\",\"column4\":\"true\",\"column5\":\"true\",\"column6\":\"true\",\"column7\":\"true\",\"column8\":\"true\",\"column9\":\"true\",\"column10\":\"true\"}}  |
    |                               | numFiles                                                             | 1                                                                                                                                                                                                                                                                 |
    |                               | numRows                                                              | 8001                                                                                                                                                                                                                                                              |
    |                               | rawDataSize                                                          | 7104888                                                                                                                                                                                                                                                           |
    |                               | totalSize                                                            | 43236                                                                                                                                                                                                                                                             |
    |                               | transient_lastDdlTime                                                | 1479819460                                                                                                                                                                                                                                                        |
    |                               | NULL                                                                 | NULL                                                                                                                                                                                                                                                              |
   */
  public DetailedTableInfoParser() {
    super("# Detailed Table Information", null, "");
  }

  @Override
  public DetailedTableInfo parse(List<Row> rows) {
    DetailedTableInfo info = new DetailedTableInfo();
    Map<String, Object> parsedSection = parseSection(rows);
    info.setDbName(getString(parsedSection, "Database:"));
    info.setOwner(getString(parsedSection, "Owner:"));
    info.setCreateTime(getString(parsedSection, "CreateTime:"));
    info.setLastAccessTime(getString(parsedSection, "LastAccessTime:"));
    info.setRetention(getString(parsedSection, "Retention:"));
    info.setLocation(getString(parsedSection, "Location:"));
    info.setTableType(getString(parsedSection, "Table Type:"));

    info.setParameters(getMap(parsedSection, "Table Parameters:"));

    return info;
  }

}
