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

import org.apache.ambari.view.huetoambarimigration.persistence.utils.PersonalResource;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class MigrationModel implements Serializable,PersonalResource{



  private String id;

  private int numberOfQueryTransfered;
  private String intanceName="";
  private String userNameofhue="";
  private int totalNoQuery;
  private String progressPercentage="";
  private String owner = "";
  private Boolean ifSuccess;
  private String timeTakentotransfer="";

  public String getTimeTakentotransfer() {
    return timeTakentotransfer;
  }

  public void setTimeTakentotransfer(String timeTakentotransfer) {
    timeTakentotransfer = timeTakentotransfer;
  }

  public Boolean getIfSuccess() {
    return ifSuccess;
  }

  public void setIfSuccess(Boolean ifSuccess) {
    ifSuccess = ifSuccess;
  }

  public MigrationModel(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    BeanUtils.populate(this, stringObjectMap);
  }

  public MigrationModel() {

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

  public int getNumberOfQueryTransfered() {
    return numberOfQueryTransfered;
  }

  public void setNumberOfQueryTransfered(int numberOfQueryTransfered) {
    this.numberOfQueryTransfered = numberOfQueryTransfered;
  }

  public int getTotalNoQuery() {
    return totalNoQuery;
  }

  public void setTotalNoQuery(int totalNoQuery) {
    this.totalNoQuery = totalNoQuery;
  }

  public String getProgressPercentage() {
    return progressPercentage;
  }

  public void setProgressPercentage(String progressPercentage) {
    progressPercentage = progressPercentage;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }
}
