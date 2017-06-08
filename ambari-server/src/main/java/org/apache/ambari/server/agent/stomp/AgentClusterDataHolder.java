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

package org.apache.ambari.server.agent.stomp;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

/**
 * Is used to saving and updating last version of event
 * @param <T> event with hash to control version
 */
public abstract class AgentClusterDataHolder<T extends Hashable> {
  private String parentHash;
  private String currentHash;
  private T data;

  public final String salt = "";

  public T getUpdateIfChanged(String agentHash) throws AmbariException {
    if (StringUtils.isEmpty(agentHash) || (StringUtils.isNotEmpty(agentHash) && !agentHash.equals(currentHash))) {
      if (data == null) {
        data = getCurrentData();
        parentHash = currentHash;
        currentHash = getHash(data);
        data.setHash(currentHash);
      }
      return data;
    }
    return getEmptyData();
  }

  protected abstract T getCurrentData() throws AmbariException;

  protected abstract T getEmptyData();

  protected void regenerateHash() {
    setCurrentHash(null);
    setParentHash(getCurrentHash());
    setCurrentHash(getHash(getData()));
    getData().setHash(getCurrentHash());
  }

  protected String getHash(T data) {
    String json = new Gson().toJson(data);
    String generatedPassword = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.update(salt.getBytes("UTF-8"));
      byte[] bytes = md.digest(json.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for(int i=0; i< bytes.length ;i++){
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      generatedPassword = sb.toString();
    }
    catch (NoSuchAlgorithmException e){
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return generatedPassword;
  }

  public abstract void updateData(T update) throws AmbariException;

  public String getParentHash() {
    return parentHash;
  }

  public void setParentHash(String parentHash) {
    this.parentHash = parentHash;
  }

  public String getCurrentHash() {
    return currentHash;
  }

  public void setCurrentHash(String currentHash) {
    this.currentHash = currentHash;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
