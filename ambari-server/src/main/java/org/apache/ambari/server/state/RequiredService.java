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

package org.apache.ambari.server.state;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Required service
 * Is defined for some services in the service level metainfo.xml
 * Specifies required service name and scope
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class RequiredService {

  public RequiredService() {
  }

  public RequiredService(String name) {
    this.name = name;
  }

  public RequiredService(String name, Scope scope) {
    this.name = name;
    this.scope = scope;
  }

  /**
   * Required service name
   */
  private String name;
  /**
   * Required service scope
   * By default is set to INSTALL
   */
  private Scope scope = Scope.INSTALL;

  public String getName() {
    return name;
  }

  public Scope getScope() {
    return scope;
  }

  /**
   * Scope of the required service
   * We could have an INSTALL time dependency (i.e. we should also install the dependent service)
   * or a RUNTIME dependency (i.e. there should be a running instance of the service in the cluster)
   */
  public enum Scope {
    /**
     * We should also install the dependent service. Is used as a default scope
     */
    INSTALL,

    /**
     * There should be a running instance of the service in the cluster
     */
    RUNTIME
  }
}

