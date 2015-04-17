/**
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

package org.apache.ambari.view.hive.resources.jobs;

import org.apache.ambari.view.hive.persistence.utils.Indexed;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.hive.service.cli.thrift.THandleIdentifier;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.apache.hive.service.cli.thrift.TOperationType;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Bean to represent TOperationHandle stored in DB
 */
public class StoredOperationHandle implements Indexed {
  private boolean hasResultSet;
  private double modifiedRowCount;
  private int operationType;
  private String guid;
  private String secret;

  private String jobId;

  private String id;

  public StoredOperationHandle() {}
  public StoredOperationHandle(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    for (Map.Entry<String, Object> entry : stringObjectMap.entrySet())  {
      try {
        PropertyUtils.setProperty(this, entry.getKey(), entry.getValue());
      } catch (NoSuchMethodException e) {
        //do nothing, skip
      }
    }
  }


  public static StoredOperationHandle buildFromTOperationHandle(TOperationHandle handle) {
    StoredOperationHandle storedHandle = new StoredOperationHandle();
    //bool hasResultSet
    storedHandle.setHasResultSet(handle.isHasResultSet());
    //optional double modifiedRowCount
    storedHandle.setModifiedRowCount(handle.getModifiedRowCount());
    //TOperationType operationType
    storedHandle.setOperationType(handle.getOperationType().getValue());
    //THandleIdentifier operationId
    storedHandle.setGuid(Hex.encodeHexString(handle.getOperationId().getGuid()));
    storedHandle.setSecret(Hex.encodeHexString(handle.getOperationId().getSecret()));
    return storedHandle;
  }

  public TOperationHandle toTOperationHandle() {
    TOperationHandle handle = new TOperationHandle();
    handle.setHasResultSet(isHasResultSet());
    handle.setModifiedRowCount(getModifiedRowCount());
    handle.setOperationType(TOperationType.findByValue(getOperationType()));
    THandleIdentifier identifier = new THandleIdentifier();
    try {
      identifier.setGuid(Hex.decodeHex(getGuid().toCharArray()));
      identifier.setSecret(Hex.decodeHex(getSecret().toCharArray()));
    } catch (DecoderException e) {
      throw new ServiceFormattedException("E060 Wrong identifier of OperationHandle is stored in DB");
    }
    handle.setOperationId(identifier);
    return handle;
  }

  public boolean isHasResultSet() {
    return hasResultSet;
  }

  public void setHasResultSet(boolean hasResultSet) {
    this.hasResultSet = hasResultSet;
  }

  public double getModifiedRowCount() {
    return modifiedRowCount;
  }

  public void setModifiedRowCount(double modifiedRowCount) {
    this.modifiedRowCount = modifiedRowCount;
  }

  public int getOperationType() {
    return operationType;
  }

  public void setOperationType(int operationType) {
    this.operationType = operationType;
  }

  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }
}
