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
package org.apache.ambari.server.orm.dao;

import org.apache.ambari.server.state.AlertState;

/**
 * Used to return alert summary data out of the database.
 */
public class AlertSummaryDTO {

  private int okCount;
  private int warningCount;
  private int criticalCount;
  private int unknownCount;
  
  /**
   * Constructor, used by JPA.  JPA invokes this constructor, even if there
   * are no records in the resultset.  In that case, all arguments are {@code null}.
   */
  public AlertSummaryDTO(Number ok, Number warning, Number critical, Number unknown) {
    okCount = null == ok ? 0 : ok.intValue();
    warningCount = null == warning ? 0 : warning.intValue();
    criticalCount = null == critical ? 0 : critical.intValue();
    unknownCount = null == unknown ? 0 : unknown.intValue();
  }
  
  /**
   * @return the count of {@link AlertState#OK} states
   */
  public int getOkCount() {
    return okCount;
  }
  
  /**
   * @return the count of {@link AlertState#WARNING} states
   */
  public int getWarningCount() {
    return warningCount;
  }  
  
  /**
   * @return the count of {@link AlertState#CRITICAL} states
   */
  public int getCriticalCount() {
    return criticalCount;
  }
  
  /**
   * @return the count of {@link AlertState#UNKNOWN} states
   */
  public int getUnknownCount() {
    return unknownCount;
  }  
}
