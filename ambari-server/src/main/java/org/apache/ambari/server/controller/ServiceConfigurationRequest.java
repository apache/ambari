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

import java.util.Map;

/**
 * This class encapsulates a configuration update request.
 * The configuration properties are grouped at service level. It is assumed that
 * different components of a service don't overload same property name.
 */
public class ServiceConfigurationRequest extends Request {
  final private String clusterName;
  final private String serviceName;

  //The complete set of desired configurations can be derived as
  //properties corresponding to baseConfigVersion overridden with
  //desiredProperties.
  final private String baseConfigVersion;
  private Map<String, String> desiredProperties = null;
  private Map<String, String> hostComponentMap = null;

  public ServiceConfigurationRequest(long requestId, Request.Method m,
      String clusterName, String serviceName, String baseConfigVersion,
      Map<String, String> desiredProperties,
      Map<String, String> hostComponentMap) {
    super(requestId, m);
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.baseConfigVersion = baseConfigVersion;
    this.desiredProperties = desiredProperties;
    this.hostComponentMap = hostComponentMap;
  }
}
