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
 * Base class for authorization related audit events.
 */
public abstract class AbstractAuthorizationEvent extends AbstractUserAuditEvent {

  static abstract class AbstractAuthorizationEventBuilder<T extends AbstractAuthorizationEvent, TBuilder extends AbstractAuthorizationEventBuilder<T, TBuilder>>
    extends AbstractUserAuditEventBuilder<T, TBuilder> {

    private String httpMethodName;
    private String resourcePath;

    /**
     * Appends the audit event details the HTTP method and
     * the path of the resource being accessed.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Operation(")
        .append(this.httpMethodName)
        .append("), ResourcePath(")
        .append(this.resourcePath)
        .append(")");
    }

    /**
     * Sets the HTTP method of the request.
     * @param httpMethodName HTTP method of the request (GET/PUT/POST/DELETE)
     * @return this builder
     */
    public TBuilder withHttpMethodName(String httpMethodName) {
      this.httpMethodName = httpMethodName;
      return (TBuilder) this;
    }

    /**
     * Sets the path being accessed.
     * @param resourcePath the path of the resource being accessed.
     * @return this builder
     */
    public TBuilder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return (TBuilder) this;
    }


  }

  /**
   * Constructor.
   */
  protected AbstractAuthorizationEvent() {}

  /**
   * {@inheritDoc}
   */
  protected AbstractAuthorizationEvent(AbstractAuthorizationEventBuilder<?, ?> builder) {
    super(builder);
  }
}
