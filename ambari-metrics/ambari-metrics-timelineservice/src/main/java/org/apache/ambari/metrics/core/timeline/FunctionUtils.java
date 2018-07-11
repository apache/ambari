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

package org.apache.ambari.metrics.core.timeline;

import com.google.common.collect.Multimap;
import org.apache.ambari.metrics.core.timeline.aggregators.Function;

import java.util.Collection;
import java.util.List;

import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricUtils.getJavaRegexFromSqlRegex;

class FunctionUtils {

  static Collection<List<Function>> findMetricFunctions(Multimap<String, List<Function>> metricFunctions,
                                                         String metricName) {
    if (metricFunctions.containsKey(metricName)) {
      return metricFunctions.get(metricName);
    }

    for (String metricNameEntry : metricFunctions.keySet()) {
      String metricRegEx = getJavaRegexFromSqlRegex(metricNameEntry);
      if (metricName.matches(metricRegEx)) {
        return metricFunctions.get(metricNameEntry);
      }
    }

    return null;
  }

}
