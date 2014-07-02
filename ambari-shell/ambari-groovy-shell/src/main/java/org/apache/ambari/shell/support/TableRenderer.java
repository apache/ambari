/**
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
package org.apache.ambari.shell.support;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;

/**
 * Utility class used to render tables.
 */
public final class TableRenderer {

  private TableRenderer() {
    throw new IllegalStateException();
  }

  /**
   * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
   * number of columns.
   *
   * @param rows    rows of the table
   * @param headers headers of the table
   * @return the formatted table
   */
  public static String renderSingleMap(Map<String, String> rows, String... headers) {
    return renderMultiValueMap(convert(rows), headers);
  }

  /**
   * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
   * number of columns.
   *
   * @param rows    rows of the table, each value will be added as a new row with the same key
   * @param headers headers of the table
   * @return formatted table
   */
  public static String renderMultiValueMap(Map<String, List<String>> rows, String... headers) {
    Table table = createTable(headers);
    if (rows != null) {
      for (String key : rows.keySet()) {
        List<String> values = rows.get(key);
        if (values != null) {
          for (String value : values) {
            table.addRow(key, value);
          }
        }
      }
    }
    return format(table);
  }

  /**
   * Renders a 3 columns wide table with the given headers and rows. If headers are provided it should match with the
   * number of columns.
   *
   * @param rows    rows of the table, value map will be added as the last 2 columns to the table
   * @param headers headers of the table
   * @return formatted table
   */
  public static String renderMapValueMap(Map<String, Map<String, String>> rows, String... headers) {
    Table table = createTable(headers);
    if (rows != null) {
      for (String key1 : rows.keySet()) {
        Map<String, String> values = rows.get(key1);
        if (values != null) {
          for (String key2 : values.keySet()) {
            table.addRow(key1, key2, values.get(key2));
          }
        }
      }
    }
    return format(table);
  }

  private static Table createTable(String... headers) {
    Table table = new Table();
    if (headers != null) {
      int column = 1;
      for (String header : headers) {
        table.addHeader(column++, new TableHeader(header));
      }
    }
    return table;
  }

  private static Map<String, List<String>> convert(Map<String, String> map) {
    Map<String, List<String>> result = new HashMap<String, List<String>>(map.size());
    if (map != null) {
      for (String key : map.keySet()) {
        result.put(key, singletonList(map.get(key)));
      }
    }
    return result;
  }

  private static String format(Table table) {
    table.calculateColumnWidths();
    return table.toString();
  }
}
