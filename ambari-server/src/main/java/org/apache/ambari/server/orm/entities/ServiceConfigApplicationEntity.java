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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "serviceconfigapplication")
@TableGenerator(name = "service_config_application_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "value"
  , pkColumnValue = "service_config_application_id_seq"
  , initialValue = 1
  , allocationSize = 1
)
public class ServiceConfigApplicationEntity {
  @Id
  @Column(name = "apply_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_config_application_id_generator")
  private Long applyId;

  @Basic
  @Column(name = "service_config_id", updatable = false, insertable = false, nullable = false)
  private Long serviceConfigId;

  @Basic
  @Column(name = "apply_timestamp")
  private Long applyTimestamp;

  @Basic
  @Column(name = "user_name")
  private String user = "_db";

  @Basic
  @Column(name = "note")
  private String note;

  @ManyToOne
  @JoinColumn(name = "service_config_id", referencedColumnName = "service_config_id")
  private ServiceConfigEntity serviceConfigEntity;


  public Long getApplyId() {
    return applyId;
  }

  public void setApplyId(Long applyId) {
    this.applyId = applyId;
  }

  public Long getServiceConfigId() {
    return serviceConfigId;
  }

  public void setServiceConfigId(Long serviceConfigId) {
    this.serviceConfigId = serviceConfigId;
  }

  public Long getApplyTimestamp() {
    return applyTimestamp;
  }

  public void setApplyTimestamp(Long applyTimestamp) {
    this.applyTimestamp = applyTimestamp;
  }

  public ServiceConfigEntity getServiceConfigEntity() {
    return serviceConfigEntity;
  }

  public void setServiceConfigEntity(ServiceConfigEntity serviceConfigEntity) {
    this.serviceConfigEntity = serviceConfigEntity;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
