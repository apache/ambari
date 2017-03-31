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

import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.ambari.view.huetoambarimigration.persistence.utils.PersonalResource;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

public class MigrationResponse implements Serializable,PersonalResource{


  private String id;

  private int numberOfQueryTransfered;
  private String intanceName="";
  private String userNameofhue="";
  private int totalNoQuery;
  private int progressPercentage;
  private String owner = "";
  private String totalTimeTaken="";
  private String jobtype="";
  private String isNoQuerySelected="";
  private int flag;
  private String error;

  public String getTotalTimeTaken() {
    return totalTimeTaken;
  }

  public void setTotalTimeTaken(String totalTimeTaken) {
    this.totalTimeTaken = totalTimeTaken;
  }

  public String getIsNoQuerySelected() {
    return isNoQuerySelected;
  }

  public void setIsNoQuerySelected(String isNoQuerySelected) {
    this.isNoQuerySelected = isNoQuerySelected;
  }

  public String getJobtype() {
    return jobtype;
  }

  public void setJobtype(String jobtype) {
    this.jobtype = jobtype;
  }

  public MigrationResponse(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    BeanUtils.populate(this, stringObjectMap);
  }

  public MigrationResponse() {

  }


  public int getTotalNoQuery() {
    return totalNoQuery;
  }

  public void setTotalNoQuery(int totalNoQuery) {
    this.totalNoQuery = totalNoQuery;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public int getNumberOfQueryTransfered() {
    return numberOfQueryTransfered;
  }

  public void setNumberOfQueryTransfered(int numberOfQueryTransfered) {
    this.numberOfQueryTransfered = numberOfQueryTransfered;
  }

  public String getIntanceName() {
    return intanceName;
  }

  public void setIntanceName(String intanceName) {
    this.intanceName = intanceName;
  }

  public String getUserNameofhue() {
    return userNameofhue;
  }

  public void setUserNameofhue(String userNameofhue) {
    this.userNameofhue = userNameofhue;
  }


  public int getProgressPercentage() {
    return progressPercentage;
  }

  public void setProgressPercentage(int progressPercentage) {
    this.progressPercentage = progressPercentage;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public int getFlag() { return flag; }

  public void setFlag(int flag) { this.flag = flag; }

  public String getError() { return error; }

  public void setError(String error) { this.error = error; }

}
