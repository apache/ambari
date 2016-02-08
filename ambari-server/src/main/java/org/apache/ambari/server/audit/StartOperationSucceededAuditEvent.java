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
 * Start operation request was accepted.
 */
@Immutable
public class StartOperationSucceededAuditEvent extends AbstractStartOperationAuditEvent {

  public static class StartOperationSucceededAuditEventBuilder
    extends AbstractStartOperationAuditEventBuilder<StartOperationSucceededAuditEvent, StartOperationSucceededAuditEventBuilder> {

    private String requestId;

    private StartOperationSucceededAuditEventBuilder() {}

    /**
     * Appends to the audit event the identifier of the
     * operation through whcih the operation progress can be tracked.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Status(Request queued for processing with request id ")
        .append(this.requestId)
        .append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StartOperationSucceededAuditEvent newAuditEvent() {
      return new StartOperationSucceededAuditEvent(this);
    }

    /**
     * Sets the identifier of the operation through whcih the operation progress can be tracked.
     * @param requestId he identifier of the operation through whcih the operation progress can be tracked.
     * @return this builder
     */
    public StartOperationSucceededAuditEventBuilder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
    }
  }

  private  StartOperationSucceededAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private StartOperationSucceededAuditEvent(StartOperationSucceededAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link StartOperationSucceededAuditEvent}
   * @return a builder instance
   */
  public static StartOperationSucceededAuditEventBuilder builder() {
    return new StartOperationSucceededAuditEventBuilder();
  }
}
