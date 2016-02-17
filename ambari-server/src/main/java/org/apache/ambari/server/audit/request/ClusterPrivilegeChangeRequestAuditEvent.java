/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class ClusterPrivilegeChangeRequestAuditEvent extends RequestAuditEvent {

  public static class ClusterPrivilegeChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<ClusterPrivilegeChangeRequestAuditEvent, ClusterPrivilegeChangeRequestAuditEventBuilder> {

    private Map<String, List<String>> users;
    private Map<String, List<String>> groups;

    public ClusterPrivilegeChangeRequestAuditEventBuilder() {
      super.withOperation("Role change");
    }

    @Override
    protected ClusterPrivilegeChangeRequestAuditEvent newAuditEvent() {
      return new ClusterPrivilegeChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      Set<String> roleSet = new HashSet<String>();
      roleSet.addAll(users.keySet());
      roleSet.addAll(groups.keySet());

      builder.append(", Roles(");
      builder.append(System.lineSeparator());

      List<String> lines = new LinkedList<String>();

      for(String role : roleSet) {
        lines.add(role + ": ");
        if(users.get(role) != null && !users.get(role).isEmpty()) {
          lines.add("  Users: " + StringUtils.join(users.get(role), ", "));
        }
        if(groups.get(role) != null && !groups.get(role).isEmpty()) {
          lines.add("  Groups: " + StringUtils.join(groups.get(role), ", "));
        }
      }

      builder.append(StringUtils.join(lines,System.lineSeparator()));

      builder.append(")");
    }

    public ClusterPrivilegeChangeRequestAuditEventBuilder withUsers(Map<String, List<String>> users) {
      this.users = users;
      return this;
    }

    public ClusterPrivilegeChangeRequestAuditEventBuilder withGroups(Map<String, List<String>> groups) {
      this.groups = groups;
      return this;
    }
  }

  protected ClusterPrivilegeChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ClusterPrivilegeChangeRequestAuditEvent(ClusterPrivilegeChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ClusterPrivilegeChangeRequestAuditEvent}
   * @return a builder instance
   */
  public static ClusterPrivilegeChangeRequestAuditEventBuilder builder() {
    return new ClusterPrivilegeChangeRequestAuditEventBuilder();
  }

}
