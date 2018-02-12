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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * Composite primary key for {@link BlueprintServiceConfigEntity}
 */
public class BlueprintServiceConfigEntityPk {

  @Id
  @Column(name = "service_id", nullable = false, insertable = true, updatable = false)
  private Long serviceId;

  @Id
  @Column(name = "type_name", nullable = false, insertable = true, updatable = false, length = 100)
  private String type;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BlueprintServiceConfigEntityPk that = (BlueprintServiceConfigEntityPk) o;
    return Objects.equals(serviceId, that.serviceId) &&
      Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, type);
  }
}
