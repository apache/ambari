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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Mpack instance entity for blueprints
 */
@Entity
@DiscriminatorValue("Blueprint")
public class BlueprintMpackInstanceEntity extends MpackInstanceEntity {

  @ManyToOne
  @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name")
  private BlueprintEntity blueprint;

  /**
   * @return the blueprint
   */
  public BlueprintEntity getBlueprint() {
    return blueprint;
  }

  /**
   * @param blueprint the blueprint
   */
  public void setBlueprint(BlueprintEntity blueprint) {
    this.blueprint = blueprint;
  }

  @Override
  public String getBlueprintName() {
    return blueprint.getBlueprintName();
  }
}
