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
package org.apache.ambari.server.actionmanager;

import java.util.Map;

import org.apache.ambari.server.Role;

/** 
 * This class encapsulates all the information for an action
 * on a host for a particular role. This class will be used to schedule, persist and track
 * an action.
 */
public class HostRole {
  private final String host;
  private final Role role;
  private Map<String, String> params = null;
  private final float successFactor;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private String manifest = null;
  
  public  HostRole(String host, Role role, float successFactor) {
    this.host = host;
    this.role = role;
    this.successFactor = successFactor;
  }
}
