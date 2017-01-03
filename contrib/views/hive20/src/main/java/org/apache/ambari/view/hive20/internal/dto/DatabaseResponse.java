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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseResponse {
  private String id;
  private String name;
  private Set<TableResponse> tables;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<TableResponse> getTables() {
    return tables;
  }

  public void addTable(TableResponse table) {
    if(tables == null) {
      tables = new HashSet<>();
    }
    tables.add(table);
  }

  public void addAllTables(Collection<TableResponse> tableResponses) {
    if(tables == null) {
      tables = new HashSet<>();
    }
    tables.addAll(tableResponses);
  }


}
