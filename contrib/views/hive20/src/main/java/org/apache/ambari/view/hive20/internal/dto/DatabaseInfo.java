/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO object to store the Database info
 */
public class DatabaseInfo {
  private String name;
  private Set<TableInfo> tables = new HashSet<>();

  public DatabaseInfo(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<TableInfo> getTables() {
    return tables;
  }

  public void setTables(Set<TableInfo> tables) {
    this.tables = tables;
  }

  public void addTable(TableInfo tableInfo) {
    this.tables.add(tableInfo);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    DatabaseInfo info = (DatabaseInfo) o;

    return new EqualsBuilder()
        .append(getName(), info.getName())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(getName())
        .toHashCode();
  }

  @Override
  public String toString() {
    return "DatabaseInfo{" +
        "name='" + name + '\'' +
        ", tables=" + tables +
        '}';
  }
}
