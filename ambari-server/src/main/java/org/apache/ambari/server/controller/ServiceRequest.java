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
package org.apache.ambari.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates any request relating to a state change or action in
 * a service or its components.
 */
public class ServiceRequest extends Request {
  private String clusterName;
  private String service;
  private String configVersion;
  private String hostComponentMapVersion;

  public class ComponentRequest {
    private String componentName;
    private Map<String, String> params = new HashMap<String, String>();
  }

  public void addComponentRequest(ComponentRequest c) {
    components.add(c);
  }

  public ServiceRequest(long requestId, Request.Method m, String clusterName,
      String service, String configVersion, String hostComponentMapVersion) {
    super(requestId, m);
    this.clusterName = clusterName;
    this.service = service;
    this.configVersion = configVersion;
    this.hostComponentMapVersion = hostComponentMapVersion;
  }

  private List<ComponentRequest> components = new ArrayList<ComponentRequest>();
}
