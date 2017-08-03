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

package org.apache.ambari.server.agent.stomp;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.commons.lang.StringUtils;

/**
 * Is used to saving and updating last version of event in cluster scope
 * @param <T> event with hash to control version
 */
public abstract class AgentClusterDataHolder<T extends Hashable> extends AgentDataHolder {
  private T data;

  public T getUpdateIfChanged(String agentHash) throws AmbariException {
    if (StringUtils.isEmpty(agentHash) || (StringUtils.isNotEmpty(agentHash) && (data == null || !agentHash.equals(data.getHash())))) {
      if (data == null) {
        data = getCurrentData();
        data.setHash(getHash(data));
      }
      return data;
    }
    return getEmptyData();
  }

  protected abstract T getCurrentData() throws AmbariException;

  protected abstract T getEmptyData();

  protected void regenerateHash() {
    getData().setHash(null);
    getData().setHash(getHash(getData()));
  }

  public abstract void updateData(T update) throws AmbariException;

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
