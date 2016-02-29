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

package org.apache.ambari.server.audit.kerberos;


import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.AbstractAuditEvent;

@Immutable
public class AbstractKerberosAuditEvent extends AbstractAuditEvent {
  static abstract class AbstractKerberosAuditEventBuilder<T extends AbstractKerberosAuditEvent, TBuilder extends AbstractKerberosAuditEventBuilder<T, TBuilder>>
    extends AbstractAuditEvent.AbstractAuditEventBuilder<T, TBuilder> {

    protected String operation;
    protected String reasonOfFailure;

    /**
     * Builds and audit log message based on the member variables
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      builder
        .append("Operation(")
        .append(operation);

      builder.append("), Status(")
        .append(reasonOfFailure == null ? "Success" : "Failed");

      if(reasonOfFailure != null) {
        builder.append("), Reason of failure(")
          .append(reasonOfFailure);
      }

      builder.append(")");
    }

    public TBuilder withOperation(String operation) {
      this.operation = operation;
      return (TBuilder) this;
    }

    public TBuilder withReasonOfFailure(String reasonOfFailure) {
      this.reasonOfFailure = reasonOfFailure;
      return (TBuilder) this;
    }
  }

  protected AbstractKerberosAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AbstractKerberosAuditEvent(AbstractKerberosAuditEventBuilder<?, ?> builder) {
    super(builder);
  }

}
