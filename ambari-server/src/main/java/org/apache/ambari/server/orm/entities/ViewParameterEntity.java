/**
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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a parameter of a View.
 */
@javax.persistence.IdClass(ViewParameterEntityPK.class)
@Table(name = "viewparameter")
@Entity
public class ViewParameterEntity {
  @Id
  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  /**
   * The parameter name.
   */
  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The parameter description.
   */
  @Column
  @Basic
  private String description;

  /**
   * Indicates whether or not the parameter is required.
   */
  @Column
  @Basic
  private char required;

  @ManyToOne
  @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false)
  private ViewEntity view;


  // ----- ViewParameterEntity -----------------------------------------------

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName() {
    return viewName;
  }

  /**
   * Set the view name.
   *
   * @param viewName  the view name
   */
  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  /**
   * Get the parameter name.
   *
   * @return the parameter name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the parameter name.
   *
   * @param name  the parameter name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the parameter description.
   *
   * @return the parameter description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the parameter description.
   *
   * @param description  the parameter description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Determine whether or not the parameter is required.
   *
   * @return true if the parameter is required
   */
  public boolean isRequired() {
    return required == 'y' || required == 'Y';
  }

  /**
   * Set the flag which indicate whether or not the parameter is required.
   *
   * @param required  the required flag; true if the parameter is required
   */
  public void setRequired(boolean required) {
    this.required = (required ? 'Y' : 'N');
  }

  /**
   * Get the associated view entity.
   *
   * @return the view entity
   */
  public ViewEntity getViewEntity() {
    return view;
  }

  /**
   * Set the associated view entity.
   *
   * @param view the view entity
   */
  public void setViewEntity(ViewEntity view) {
    this.view = view;
  }
}
