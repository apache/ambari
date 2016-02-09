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

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.ResultStatus;

/**
 * Base class for start operation audit events.
 */
public class RequestAuditEvent extends AbstractUserAuditEvent {

  public static class RequestAuditEventBuilder extends AbstractUserAuditEventBuilder<RequestAuditEvent, RequestAuditEventBuilder> {

    private Request.Type requestType;

    protected ResultStatus resultStatus;

    private String url;

    @Override
    protected RequestAuditEvent newAuditEvent() {
      return new RequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);
      builder
        .append(", RequestType(")
        .append(requestType)
        .append("), ")
        .append("url(")
        .append(url)
        .append("), ResultStatus(")
        .append(resultStatus.getStatusCode())
        .append(" ")
        .append(resultStatus.getStatus())
        .append(")");

      if(resultStatus.isErrorState()) {
        builder.append(", Reason(")
          .append(resultStatus.getMessage())
          .append(")");
      }
    }

    /**
     * Sets the request type to be added to the audit event.
     * @param requestType request type to be added to the audit event.
     * @return this builder
     */
    public RequestAuditEventBuilder withRequestType(Request.Type requestType) {
      this.requestType = requestType;

      return this;
    }

    /**
     * Sets the url to be added to the audit event.
     * @param url url to be added to the audit event.
     * @return this builder
     */
    public RequestAuditEventBuilder withUrl(String url) {
      this.url = url;

      return this;
    }

    /**
     * Sets the result status to be added to the audit event.
     * @param resultStatus result status to be added to the audit event.
     * @return this builder
     */
    public RequestAuditEventBuilder withResultStatus(ResultStatus resultStatus) {
      this.resultStatus = resultStatus;

      return this;
    }
  }

  protected RequestAuditEvent() {}

  /**
   * {@inheritDoc}
   */
  protected RequestAuditEvent(RequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link RequestAuditEvent}
   * @return a builder instance
   */
  public static RequestAuditEventBuilder builder() {
    return new RequestAuditEventBuilder();
  }

}
