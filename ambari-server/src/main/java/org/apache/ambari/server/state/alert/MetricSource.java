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
package org.apache.ambari.server.state.alert;

import com.google.gson.annotations.SerializedName;

/**
 * Alert when the source type is defined as {@link SourceType#METRIC}
 */
public class MetricSource extends Source {
  
  private String host = null;
  
  @SerializedName("jmx")
  private String jmxInfo = null;
  
  @SerializedName("ganglia")
  private String gangliaInfo = null;
  
  /**
   * @return the jmx info, if this metric is jmx-based
   */
  public String getJmxInfo() {
    return jmxInfo;
  }
  
  /**
   * @return the ganglia info, if this metric is ganglia-based
   */
  public String getGangliaInfo() {
    return gangliaInfo;
  }
  
  /**
   * @return the host info, which may include port information
   */
  public String getHost() {
    return host;
  }
  
  
}
