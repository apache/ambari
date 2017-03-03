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

  //Hive Job Statutes                                                 Hue Corresponding Status
  private static final String JOB_STATE_INITIALIZED = "INITIALIZED";   //submitted
  private static final String JOB_STATE_RUNNING = "RUNNING";           //running
  private static final String JOB_STATE_FINISHED = "SUCCEEDED";        //available
  private static final String JOB_STATE_ERROR = "ERROR";               //failed
  private static final String JOB_STATE_UNKNOWN = "UNKNOWN";           //expired

  private String database;
  private String query;
  private String queryTitle;
  private ArrayList<String> filePaths;
  private ArrayList<String> udfClasses;
  private ArrayList<String> udfNames;
  private String ownerName;
  private String jobStatus;


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

  public String getJobStatus() { return jobStatus; }

  public void setJobStatus(int state) {
    switch (state) {
      case 0:
        jobStatus = JOB_STATE_INITIALIZED;
        break;
      case 1:
        jobStatus = JOB_STATE_RUNNING;
        break;
      case 2:
        jobStatus = JOB_STATE_FINISHED;
        break;
      case 3:
        jobStatus = JOB_STATE_ERROR;
        break;
      case 4:
        jobStatus = JOB_STATE_UNKNOWN;
        break;
      default:
        return;
    }

  }
}
