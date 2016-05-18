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

package org.apache.ambari.server.audit.event.request;

import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.request.RequestAuditEvent;

/**
 * Audit event for deleting a repository version
 */
@Immutable
public class DeleteRepositoryVersionRequestAuditEvent extends RequestAuditEvent {

  public static class DeleteRepositoryVersionAuditEventBuilder extends RequestAuditEventBuilder<DeleteRepositoryVersionRequestAuditEvent, DeleteRepositoryVersionAuditEventBuilder> {

    /**
     * Stack name
     */
    private String stackName;

    /**
     * Stack version
     */
    private String stackVersion;

    /**
     * Repository version
     */
    private String repoVersion;

    public DeleteRepositoryVersionAuditEventBuilder() {
      super.withOperation("Repository version removal");
    }

    @Override
    protected DeleteRepositoryVersionRequestAuditEvent newAuditEvent() {
      return new DeleteRepositoryVersionRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Stack(")
        .append(stackName)
        .append("), Stack version(")
        .append(stackVersion)
        .append("), Repo version ID(")
        .append(repoVersion)
        .append(")");
    }

    public DeleteRepositoryVersionAuditEventBuilder withStackName(String stackName) {
      this.stackName = stackName;
      return this;
    }

    public DeleteRepositoryVersionAuditEventBuilder withStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
      return this;
    }

    public DeleteRepositoryVersionAuditEventBuilder withRepoVersion(String repoVersion) {
      this.repoVersion = repoVersion;
      return this;
    }
  }

  protected DeleteRepositoryVersionRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected DeleteRepositoryVersionRequestAuditEvent(DeleteRepositoryVersionAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link DeleteRepositoryVersionRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static DeleteRepositoryVersionAuditEventBuilder builder() {
    return new DeleteRepositoryVersionAuditEventBuilder();
  }

}
