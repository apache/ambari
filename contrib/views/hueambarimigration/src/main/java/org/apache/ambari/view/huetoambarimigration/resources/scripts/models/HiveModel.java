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

package org.apache.ambari.view.huetoambarimigration.resources.scripts.models;

import java.util.ArrayList;

public class HiveModel {

  private String database;
  private String query;
  private String queryTitle;
  private ArrayList<String> filePaths;
  private ArrayList<String> udfClasses;
  private ArrayList<String> udfNames;
  private String ownerName;


  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getQuery() {
    return query;
  }

  public String getQueryTitle() { return queryTitle; }

  public void setQueryTitle(String queryTitle) { this.queryTitle = queryTitle; }

  public void setQuery(String query) {
    this.query = query;
  }

  public ArrayList<String> getFilePaths() {
    return filePaths;
  }

  public void setFilePaths(ArrayList<String> filePaths) {
    this.filePaths = filePaths;
  }

  public ArrayList<String> getUdfClasses() {
    return udfClasses;
  }

  public void setUdfClasses(ArrayList<String> udfClasses) {
    this.udfClasses = udfClasses;
  }

  public ArrayList<String> getUdfNames() {
    return udfNames;
  }

  public void setUdfNames(ArrayList<String> udfNames) {
    this.udfNames = udfNames;
  }

  public String getOwnerName() { return ownerName; }

  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
