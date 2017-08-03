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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.commons.lang.StringUtils;

/**
 * Is used to saving and updating last version of event in host scope
 * @param <T> event with hash to control version
 */
public abstract class AgentHostDataHolder<T extends Hashable> extends AgentDataHolder {
  private Map<String, T> data = new HashMap<>();

  public T getUpdateIfChanged(String agentHash, String hostName) throws AmbariException {
    if (StringUtils.isEmpty(agentHash) || (StringUtils.isNotEmpty(agentHash) && (!data.containsKey(hostName)
        || !agentHash.equals(data.get(hostName).getHash())))) {
      if (!data.containsKey(hostName)) {
        data.put(hostName, getCurrentData(hostName));
        data.get(hostName).setHash(getHash(data.get(hostName)));
      }
      return data.get(hostName);
    }
    return getEmptyData();
  }

  protected abstract T getCurrentData(String hostName) throws AmbariException;

  protected abstract T getEmptyData();

  protected void regenerateHash(String hostName) {
    getData(hostName).setHash(null);
    getData(hostName).setHash(getHash(getData(hostName)));
  }

  public abstract void updateData(T update) throws AmbariException;

  public Map<String, T> getData() {
    return data;
  }

  public void setData(Map<String, T> data) {
    this.data = data;
  }

  public T getData(String hostName) {
    return data.get(hostName);
  }

  public void setData(T data, String hostName) {
    this.data.put(hostName, data);
  }
}
