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

/**
 * Entity to represent an mpack level configuration in the blueprint.
 */
@Entity
@Table(name = "blueprint_mpack_configuration")
@IdClass(BlueprintMpackConfigEntityPk.class)
public class BlueprintMpackConfigEntity implements BlueprintConfiguration {

  @Id
  @Column(name = "mpack_instance_id", nullable = false, insertable = false, updatable = false)
  private Long mpackInstanceId;

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
  @JoinColumn(name = "mpack_instance_id", referencedColumnName = "id", nullable = false)
  private BlueprintMpackInstanceEntity mpackInstance;

  /**
   * @return the id of the mpack instance entity this configuration belongs to
   */
  public Long getMpackInstanceId() {
    return mpackInstanceId;
  }

  /**
   * @param mpackInstanceId the id of the instance referency entity this configuration belongs to
   */
  public void setMpackInstanceId(Long mpackInstanceId) {
    this.mpackInstanceId = mpackInstanceId;
  }

  /**
   * @return the configuration type
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * @param typeName the type of the configuration
   */
  @Override
  public void setType(String typeName) {
    this.type = typeName;
  }

  /**
   * @return the configuration data encoded in json
   */
  public String getConfigData() {
    return configData;
  }

  /**
   * @param blueprintName the name of the blueprint
   */
  @Override
  public void setBlueprintName(String blueprintName) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the name of the blueprint
   */
  @Override
  public String getBlueprintName() {
    return getMpackInstance().getBlueprint().getBlueprintName();
  }

  /**
   * @param configData the configuration data encoded in json
   */
  public void setConfigData(String configData) {
    this.configData = configData;
  }

  /**
   * @return the configuration attributes encoded in json
   */
  public String getConfigAttributes() {
    return configAttributes;
  }

  /**
   * @param configAttributes  the configuration attributes encoded in json
   */
  public void setConfigAttributes(String configAttributes) {
    this.configAttributes = configAttributes;
  }

  /**
   * @return the mpack instance entity this configuration belongs to
   */
  public BlueprintMpackInstanceEntity getMpackInstance() {
    return mpackInstance;
  }

  /**
   * @param mpackInstance the mpack instance entity this configuration belongs to
   */
  public void setMpackInstance(BlueprintMpackInstanceEntity mpackInstance) {
    this.mpackInstance = mpackInstance;
  }
}
