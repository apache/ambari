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

import org.apache.ambari.server.state.AlertState;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The {@link AlertStateSummary} class holds information about each possible
 * alert state.
 */
public final class AlertStateSummary {
  /**
   * The {@link AlertState#OK} state information.
   */
  @JsonProperty(value = "OK")
  public final AlertStateValues Ok = new AlertStateValues();

  /**
   * The {@link AlertState#WARNING} state information.
   */
  @JsonProperty(value = "WARNING")
  public final AlertStateValues Warning = new AlertStateValues();

  /**
   * The {@link AlertState#CRITICAL} state information.
   */
  @JsonProperty(value = "CRITICAL")
  public final AlertStateValues Critical = new AlertStateValues();

  /**
   * The {@link AlertState#UNKNOWN} state information.
   */
  @JsonProperty(value = "UNKNOWN")
  public final AlertStateValues Unknown = new AlertStateValues();
}
