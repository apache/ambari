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


import javax.annotation.concurrent.Immutable;

/**
 * Login failure audit event.
 */
@Immutable
public class LoginFailedAuditEvent extends AbstractLoginAuditEvent {

  public static class LoginFailedAuditEventBuilder
    extends AbstractLoginAuditEventBuilder<LoginFailedAuditEvent, LoginFailedAuditEventBuilder> {

    private String reason;

    /**
     * Appends to the audit message details the reason
     * that caused the login failure.
     * @param builder builder for the audit message details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Status(Login failed !)")
        .append(", Reason(")
        .append(this.reason)
        .append(")")
        ;
    }

    private LoginFailedAuditEventBuilder() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LoginFailedAuditEvent newAuditEvent() {
      return new LoginFailedAuditEvent(this);
    }

    /**
     * Sets the root cause of the login failure.
     * @param reason root cause of the login failure.
     * @return this builder
     */
    public LoginFailedAuditEventBuilder withReason(String reason) {
      this.reason = reason;
      return this;
    }

  }


  private LoginFailedAuditEvent() {}

  /**
   * {@inheritDoc}
   */
  private LoginFailedAuditEvent(LoginFailedAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link LoginFailedAuditEvent}
   * @return a builder instance
   */
  public static LoginFailedAuditEventBuilder builder() {
    return new LoginFailedAuditEventBuilder();
  }
}
