/*
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
package org.apache.ambari.server.topology.addservice;

import java.util.Map;
import java.util.Set;

/**
 * Processed info for adding new services/components to an existing cluster.
 */
public final class AddServiceInfo {

  private final String clusterName;
  private final Map<String, Map<String, Set<String>>> newServices;

  public AddServiceInfo(String clusterName, Map<String, Map<String, Set<String>>> newServices) {
    this.clusterName = clusterName;
    this.newServices = newServices;
  }

  public String clusterName() {
    return clusterName;
  }

  /**
   * New services to be added to the cluster: service -> component -> host
   * This should include both explicitly requested services, and services of the requested components.
   */
  public Map<String, Map<String, Set<String>>> newServices() {
    return newServices;
  }
}
