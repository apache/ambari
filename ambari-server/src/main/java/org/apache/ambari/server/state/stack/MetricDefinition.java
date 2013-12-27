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

package org.apache.ambari.server.state.stack;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to represent metrics for a stack component.
 */
public class MetricDefinition {
  private String type = null;
  private Map<String, String> properties = null;
  private Map<String, Metric> metrics = null;
  
  public String getType() {
    return type;
  }
  
  public Map<String, String> getProperties() {
    return properties;
  }
  
  public Map<String, Metric> getMetrics() {
    return metrics;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{type=").append(type);
    sb.append(";properties=").append(properties);
    sb.append(";metric_count=").append(metrics.size());
    sb.append('}');
    
    return sb.toString();
  }
  
}
