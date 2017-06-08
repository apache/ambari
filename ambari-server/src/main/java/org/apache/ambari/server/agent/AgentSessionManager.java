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
package org.apache.ambari.server.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.state.Host;

import com.google.inject.Singleton;

@Singleton
public class AgentSessionManager {
  private static ConcurrentHashMap<String, Host> registeredHosts = new ConcurrentHashMap<>();

  public void register(String sessionId, Host registeredHost) {
    String existKey = registeredHosts.entrySet().stream()
        .filter(e -> e.getValue().getHostName().equals(registeredHost.getHostName())).map(Map.Entry::getKey)
        .findAny().orElse(null);
    if (existKey != null) {
      registeredHosts.remove(existKey);
    }
    registeredHosts.put(sessionId, registeredHost);
  }

  public boolean isRegistered(String sessionId) {
    return registeredHosts.containsKey(sessionId);
  }

  public Host getHost(String sessionId) throws HostNotRegisteredException {
    if (registeredHosts.containsKey(sessionId)) {
      return registeredHosts.get(sessionId);
    }
    throw new HostNotRegisteredException(sessionId);
  }


}
