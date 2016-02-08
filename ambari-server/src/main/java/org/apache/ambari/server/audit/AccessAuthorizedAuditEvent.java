/**
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
package org.apache.ambari.server.audit;

import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;

/**
 * Access to given resource is authorized.
 */
@Immutable
public class AccessAuthorizedAuditEvent extends AbstractAuthorizationEvent {

  public static class AccessAuthorizedAuditEventBuilder
    extends AbstractAuthorizationEventBuilder<AccessAuthorizedAuditEvent, AccessAuthorizedAuditEventBuilder> {

    private List<String> privileges;

    /**
     * Appends to the aduit event detail the list of the privileges
     * possessed by the principal requesting access to a resource.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Privileges(")
        .append(StringUtils.join(privileges, ","))
        .append(")")
        .append(", Status(Access authorized !)");
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    protected AccessAuthorizedAuditEvent newAuditEvent() {
      return new AccessAuthorizedAuditEvent(this);
    }


    /**
     * Sets the list of privileges the privileges
     * possessed by the principal requesting access to a resource.
     * @param privileges
     * @return this builder
     */
    public AccessAuthorizedAuditEventBuilder withPrivileges(List<String> privileges) {
      this.privileges = privileges;

      return this;
    }
  }

  private AccessAuthorizedAuditEvent() {}

  /**
   * {@inheritDoc}
   */
  private AccessAuthorizedAuditEvent(AccessAuthorizedAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AccessAuthorizedAuditEvent}
   * @return a builder instance
   */
  public static AccessAuthorizedAuditEventBuilder builder() {
    return new AccessAuthorizedAuditEventBuilder();
  }

}
