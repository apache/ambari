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

package org.apache.ambari.server.audit.event.request.event;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.audit.event.request.RequestAuditEvent;

public class ChangeRepositoryVersionRequestAuditEvent extends RequestAuditEvent {

  public static class ChangeRepositoryVersionAuditEventBuilder extends RequestAuditEventBuilder<ChangeRepositoryVersionRequestAuditEvent, ChangeRepositoryVersionAuditEventBuilder> {

    private String stackName;

    private String displayName;

    private String stackVersion;

    private String repoVersion;

    private Map<String, List<Map<String, String>>> repos;

    public ChangeRepositoryVersionAuditEventBuilder() {
      super.withOperation("Repository version change");
    }

    @Override
    protected ChangeRepositoryVersionRequestAuditEvent newAuditEvent() {
      return new ChangeRepositoryVersionRequestAuditEvent(this);
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
        .append("), Display name(")
        .append(displayName)
        .append("), Repo version(")
        .append(repoVersion)
        .append("), Repositories(");

      if (!repos.isEmpty()) {
        builder.append(System.lineSeparator());
      }

      for (Map.Entry<String, List<Map<String, String>>> repo : repos.entrySet()) {
        builder.append("Operating system: ").append(repo.getKey());
        builder.append(System.lineSeparator());
        for (Map<String, String> properties : repo.getValue()) {
          builder.append("    Repository ID(").append(properties.get("repo_id"));
          builder.append("), Repository name(").append(properties.get("repo_name"));
          builder.append("), Base url(").append(properties.get("base_url")).append(")");
          builder.append(System.lineSeparator());
        }
      }

      builder.append(")");
    }

    public ChangeRepositoryVersionAuditEventBuilder withStackName(String stackName) {
      this.stackName = stackName;
      return this;
    }

    public ChangeRepositoryVersionAuditEventBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public ChangeRepositoryVersionAuditEventBuilder withStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
      return this;
    }

    public ChangeRepositoryVersionAuditEventBuilder withRepoVersion(String repoVersion) {
      this.repoVersion = repoVersion;
      return this;
    }

    public ChangeRepositoryVersionAuditEventBuilder withRepos(Map<String, List<Map<String, String>>> repos) {
      this.repos = repos;
      return this;
    }
  }

  protected ChangeRepositoryVersionRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ChangeRepositoryVersionRequestAuditEvent(ChangeRepositoryVersionAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ChangeRepositoryVersionRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ChangeRepositoryVersionAuditEventBuilder builder() {
    return new ChangeRepositoryVersionAuditEventBuilder();
  }

}
