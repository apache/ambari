/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.uploads.query;

public class InsertFromQueryInput {
  private String fromDatabase;
  private String fromTable;
  private String toDatabase;
  private String toTable;

  public InsertFromQueryInput() {
  }

  public InsertFromQueryInput(String fromDatabase, String fromTable, String toDatabase, String toTable) {
    this.fromDatabase = fromDatabase;
    this.fromTable = fromTable;
    this.toDatabase = toDatabase;
    this.toTable = toTable;
  }

  public String getFromDatabase() {
    return fromDatabase;
  }

  public void setFromDatabase(String fromDatabase) {
    this.fromDatabase = fromDatabase;
  }

  public String getFromTable() {
    return fromTable;
  }

  public void setFromTable(String fromTable) {
    this.fromTable = fromTable;
  }

  public String getToDatabase() {
    return toDatabase;
  }

  public void setToDatabase(String toDatabase) {
    this.toDatabase = toDatabase;
  }

  public String getToTable() {
    return toTable;
  }

  public void setToTable(String toTable) {
    this.toTable = toTable;
  }
}
