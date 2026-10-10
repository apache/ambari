/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.orm.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Internal versioned records; never exposed through the generic persistence API. */
@Entity
@Table(name = "mpack_record")
public class MpackRecordEntity {
  public enum Kind {
    CONTROL, UPLOAD, PLAN, RELEASE, BINDING, SNAPSHOT, OPERATION, IDEMPOTENCY
  }

  @Id
  @Column(name = "record_id", length = 255, nullable = false, updatable = false)
  private String id;

  @Enumerated(EnumType.STRING)
  @Column(name = "record_kind", length = 32, nullable = false, updatable = false)
  private Kind kind;

  @Column(name = "revision", nullable = false)
  private Long revision;

  @Column(name = "schema_version", nullable = false)
  private Integer schemaVersion;

  @Lob
  @Basic
  @Column(name = "payload", nullable = false)
  private String payload;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Kind getKind() {
    return kind;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }

  public Long getRevision() {
    return revision;
  }

  public void setRevision(Long revision) {
    this.revision = revision;
  }

  public Integer getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(Integer schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}
