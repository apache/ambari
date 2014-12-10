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
package org.apache.ambari.server.events;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;

/**
 * The {@link ActionFinalReportReceivedEvent} is fired when a
 * command report action is received. Event is fired only if command state
 * is COMPLETED/FAILED/ABORTED.
 */
public final class ActionFinalReportReceivedEvent extends AmbariEvent {

  private Long clusterId;
  private String hostname;
  private CommandReport commandReport;
  private String role;

  /**
   * Constructor.
   *
   * @param clusterId (beware, may be null if action is not bound to cluster)
   * @param hostname host that is an origin for a command report
   * @param report full command report (may be null if action has been cancelled)
   * @param role host command role. It is usually present at report entity, but
   * if report is null, we still need some way to determine action type.
   */
  public ActionFinalReportReceivedEvent(Long clusterId, String hostname,
                                        CommandReport report, String role) {
    super(AmbariEventType.ACTION_EXECUTION_FINISHED);
    this.clusterId = clusterId;
    this.hostname = hostname;
    this.commandReport = report;
    this.role = role;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public String getHostname() {
    return hostname;
  }

  public CommandReport getCommandReport() {
    return commandReport;
  }

  public String getRole() {
    return role;
  }

  @Override
  public String toString() {
    return "ActionFinalReportReceivedEvent{" +
            "clusterId=" + clusterId +
            ", hostname='" + hostname + '\'' +
            ", commandReportStatus=" + commandReport.getStatus() +
            ", commandReportRole=" + role +
            '}';
  }
}
