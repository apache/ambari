/*
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
package org.apache.ambari.server.api.query.render;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * The {@link AlertStateValues} class holds various information about an alert
 * state, such as the number of instances of that state and the most recent
 * timestamp.
 */
public final class AlertStateValues {
  /**
   * The total count of non-maintenance mode instances.
   */
  @JsonProperty(value = "count")
  public int Count = 0;

  /**
   * The time of the last state change.
   */
  @JsonProperty(value = "original_timestamp")
  public long Timestamp = 0;

  /**
   * The total count of instances in maintenance mode.
   */
  @JsonProperty(value = "maintenance_count")
  public int MaintenanceCount = 0;

  /**
   * The most recently received text from any instance of the alert.
   */
  @JsonProperty(value = "latest_text")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public String AlertText = null;
}
