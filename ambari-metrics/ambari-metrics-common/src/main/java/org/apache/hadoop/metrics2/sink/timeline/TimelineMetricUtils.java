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

package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TimelineMetricUtils {

  /**
   * Given a SQL regex, convert it to JAVA regex.
   * @param sqlRegex
   * @return
   */
  public static String getJavaRegexFromSqlRegex(String sqlRegex) {
    String javaRegEx;
    if (sqlRegex.contains("*") || sqlRegex.contains("__%")) {
      //Special case handling for metric name with * and __%.
      //For example, dfs.NNTopUserOpCounts.windowMs=300000.op=*.user=%.count
      // or dfs.NNTopUserOpCounts.windowMs=300000.op=__%.user=%.count
      String metricNameWithEscSeq = sqlRegex.replace("*", "\\*").replace("__%", "..%");
      javaRegEx = metricNameWithEscSeq.replace("%", ".*");
    } else {
      javaRegEx = sqlRegex.replace("%", ".*");
    }
    return javaRegEx;
  }

  /**
   * Wrapper method to split comma separated strings and then invoke getJavaRegexFromSqlRegex.
   * @param commaSeparatedMetricPatternsString
   * @return
   */
  public static List<String> getJavaMetricPatterns(String commaSeparatedMetricPatternsString) {

    List<String> javaPatterns = new ArrayList<>();
    if (StringUtils.isEmpty(commaSeparatedMetricPatternsString)) {
      return javaPatterns;
    }

    for (String patternString : commaSeparatedMetricPatternsString.split(",")) {
      String javaPatternString = getJavaRegexFromSqlRegex(patternString);
      javaPatterns.add(javaPatternString);
    }
    return javaPatterns;
  }
}
