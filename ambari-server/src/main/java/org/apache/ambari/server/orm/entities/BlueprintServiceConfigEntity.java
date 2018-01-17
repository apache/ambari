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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "blueprint_service_config")
@IdClass(BlueprintServiceConfigEntityPk.class)
public class BlueprintServiceConfigEntity implements BlueprintConfiguration {

  @Id
  @Column(name = "service_id", nullable = false, insertable = false, updatable = false)
  private Long serviceId;

  @Id
  @Column(name = "type_name", nullable = false, insertable = true, updatable = false, length = 100)
  private String type;

  @Column(name = "config_data")
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String configData;

  @Column(name = "config_attributes")
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String configAttributes;

  @ManyToOne
  @JoinColumn(name = "service_id", referencedColumnName = "id", nullable = false)
  private BlueprintServiceEntity service;

  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public String getType() {
    return type;
  }

  @Override
  public String getBlueprintName() {
    return getService().getMpackReference().getBlueprint().getBlueprintName();
  }

  public void setType(String typeName) {
    this.type = typeName;
  }

  public String getConfigData() {
    return configData;
  }

  public void setConfigData(String configData) {
    this.configData = configData;
  }

  public String getConfigAttributes() {
    return configAttributes;
  }

  public void setConfigAttributes(String configAttributes) {
    this.configAttributes = configAttributes;
  }

  public BlueprintServiceEntity getService() {
    return service;
  }

  public void setService(BlueprintServiceEntity service) {
    this.service = service;
  }
}
