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


public class ProgressCheckModel {

  private int progressPercentage;
  private int noOfQueryCompleted;
  private int noOfQueryLeft;
  private int totalNoOfQuery;

  public int getProgressPercentage() {
    return progressPercentage;
  }

  public void setProgressPercentage(int progressPercentage) {
    this.progressPercentage = progressPercentage;
  }

  public int getNoOfQueryCompleted() {
    return noOfQueryCompleted;
  }

  public void setNoOfQueryCompleted(int noOfQueryCompleted) {
    this.noOfQueryCompleted = noOfQueryCompleted;
  }

  public int getNoOfQueryLeft() {
    return noOfQueryLeft;
  }

  public void setNoOfQueryLeft(int noOfQueryLeft) {
    this.noOfQueryLeft = noOfQueryLeft;
  }

  public int getTotalNoOfQuery() {
    return totalNoOfQuery;
  }

  public void setTotalNoOfQuery(int totalNoOfQuery) {
    this.totalNoOfQuery = totalNoOfQuery;
  }
}
