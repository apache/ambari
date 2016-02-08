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

/**
 * Base class for start operation audit events.
 */
public class AbstractStartOperationAuditEvent extends AbstractUserAuditEvent {
  static abstract class AbstractStartOperationAuditEventBuilder<T extends AbstractStartOperationAuditEvent, TBuilder extends AbstractStartOperationAuditEventBuilder<T, TBuilder>>
    extends AbstractUserAuditEventBuilder<T, TBuilder> {

    private String requestDetails;

    /**
     * Appends to the event the details of the incoming request.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", RequestDetails(")
        .append(this.requestDetails)
        .append(")");
    }

    /**
     * Sets the request details to be added to the audit event.
     * @param requestDetails request details to be added to the audit event.
     * @return this builder
     */
    public TBuilder withRequestDetails(String requestDetails) {
      this.requestDetails = requestDetails;

      return (TBuilder)this;
    }
  }

  protected AbstractStartOperationAuditEvent() {}

  /**
   * {@inheritDoc}
   */
  protected AbstractStartOperationAuditEvent(AbstractStartOperationAuditEventBuilder<?, ?> builder) {
    super(builder);
  }
}
