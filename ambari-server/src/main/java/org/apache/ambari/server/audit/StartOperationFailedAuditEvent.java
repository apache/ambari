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
 * Start operation request rejected.
 */
@Immutable
public class StartOperationFailedAuditEvent extends AbstractStartOperationAuditEvent {

  public static class StartOperationFailedAuditEventBuilder
    extends AbstractStartOperationAuditEventBuilder<StartOperationFailedAuditEvent, StartOperationFailedAuditEventBuilder> {

    private String reason;

    private StartOperationFailedAuditEventBuilder() {}

    /**
     * Appends to the audit event the reason due to which the
     * operation was rejected.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Status(Request failed to be processed !), Reason(")
        .append(this.reason)
        .append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StartOperationFailedAuditEvent newAuditEvent() {
      return new StartOperationFailedAuditEvent(this);
    }

    /**
     * Sets the reason due to which the operation was rejected.
     * @param reason the reason due to which the operation was rejected.
     * @return this builder.
     */
    public StartOperationFailedAuditEventBuilder withReason(String reason) {
      this.reason = reason;
      return this;
    }
  }

  private StartOperationFailedAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private StartOperationFailedAuditEvent(StartOperationFailedAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link StartOperationFailedAuditEvent}
   * @return a builder instance
   */
  public static StartOperationFailedAuditEventBuilder builder() {
    return new StartOperationFailedAuditEventBuilder();
  }
}
