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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnOrder;
import org.apache.ambari.view.hive20.internal.dto.Order;
import org.apache.ambari.view.hive20.internal.dto.StorageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Storage Information from the describe formatted output.
 */
public class StorageInfoParser extends AbstractTableMetaParser<StorageInfo> {
  private static final Logger LOG = LoggerFactory.getLogger(StorageInfoParser.class);


  public StorageInfoParser() {
    super("# Storage Information", null, "");
  }

  @Override
  public StorageInfo parse(List<Row> rows) {
    StorageInfo info = new StorageInfo();
    Map<String, Object> parsedSection = parseSection(rows);

    info.setSerdeLibrary(getString(parsedSection, "SerDe Library:"));
    info.setInputFormat(getString(parsedSection, "InputFormat:"));
    info.setOutputFormat(getString(parsedSection, "OutputFormat:"));
    info.setCompressed(getString(parsedSection, "Compressed:"));
    info.setNumBuckets(getString(parsedSection, "Num Buckets:"));
    info.setBucketCols(parseBucketColumns(getString(parsedSection, "Bucket Columns:")));
    info.setSortCols(parseSortCols(getString(parsedSection, "Sort Columns:")));
    info.setParameters(getMap(parsedSection, "Storage Desc Params:"));

    return info;
  }

  private List<String> parseBucketColumns(String string) {
    String[] strings = string.split("[\\[\\],]");
    return FluentIterable.from(Arrays.asList(strings)).filter(new Predicate<String>() {
      @Override
      public boolean apply(@Nullable String input) {
        return !(null == input || input.trim().length() == 0) ;
      }
    }).transform(new Function<String, String>() {
      @Override
      public String apply(String input) {
        return input.trim();
      }
    }).toList();
  }

  private List<ColumnOrder> parseSortCols(String str) {
    String patternStr = "Order\\s*\\(\\s*col\\s*:\\s*([^,]+)\\s*,\\s*order\\s*:\\s*(\\d)\\s*\\)";
    Pattern pattern = Pattern.compile(patternStr);

    Matcher matcher = pattern.matcher(str);

    LinkedList<ColumnOrder> list = new LinkedList<>();
    while(matcher.find()){
      String colName = matcher.group(1);
      String orderString = matcher.group(2);
      Order order = Order.fromOrdinal(Integer.valueOf(orderString));
      ColumnOrder co = new ColumnOrder(colName, order);
      list.add(co);
      LOG.debug("columnOrder : {}", co);
    }

    return list;
  }
}
