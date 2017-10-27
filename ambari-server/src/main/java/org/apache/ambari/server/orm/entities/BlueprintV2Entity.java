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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.ambari.server.state.SecurityType;

/**
 * Entity representing a Blueprint.
 */
@Table(name = "blueprintv2")
@NamedQuery(name = "allBlueprintsv2",
  query = "SELECT blueprint FROM BlueprintV2Entity blueprint")
@Entity
public class BlueprintV2Entity {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = true,
    updatable = false, unique = true, length = 100)
  private String blueprintName;

  @Basic
  @Enumerated(value = EnumType.STRING)
  @Column(name = "security_type", nullable = false, insertable = true, updatable = true)
  private SecurityType securityType = SecurityType.NONE;

  @Basic
  @Column(name = "security_descriptor_reference", nullable = true, insertable = true, updatable = true)
  private String securityDescriptorReference;

  @Basic
  @Column(name = "content", nullable = false, insertable = true, updatable = true)
  private String content;

  public String getBlueprintName() {
    return blueprintName;
  }

  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  public SecurityType getSecurityType() {
    return securityType;
  }

  public void setSecurityType(SecurityType securityType) {
    this.securityType = securityType;
  }

  public String getSecurityDescriptorReference() {
    return securityDescriptorReference;
  }

  public void setSecurityDescriptorReference(String securityDescriptorReference) {
    this.securityDescriptorReference = securityDescriptorReference;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
