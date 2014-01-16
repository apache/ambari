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

package org.apache.ambari.server.state;


import javax.xml.bind.annotation.XmlElement;

/**
 * Represents stack component dependency information.
 */
public class DependencyInfo {
  /**
   * The name of the component which is the dependency.
   * Specified in the form serviceName/componentName.
   */
  private String m_name;

  /**
   * The scope of the dependency.  Either "cluster" or "host".
   */
  private String m_scope;

  /**
   * Auto-deployment information for the dependency.
   * If auto-deployment is enabled for the dependency, the dependency is
   * automatically deployed if it is not specified in the provided topology.
   */
  @XmlElement(name="auto-deploy")
  private AutoDeployInfo m_autoDeploy;


  /**
   * Setter for name property.
   *
   * @param name the name of the component which is the dependency
   *             in the form serviceName/componentName
   */
  public void setName(String name) {
    m_name = name;
  }

  /**
   * Getter for name property.
   *
   * @return the name of the component which is the dependency
   *         in the form serviceName/componentName
   */
  public String getName() {
    return m_name;
  }

  /**
   * Setter for scope property.
   *
   * @param scope the scope of the dependency.  Either "cluster" or "host".
   */
  public void setScope(String scope) {
    m_scope = scope;
  }

  /**
   * Getter for scope property.
   *
   * @return either "cluster" or "host".
   */
  public String getScope() {
    return m_scope;
  }

  /**
   * Setter for auto-deploy property.
   *
   * @param autoDeploy auto-deploy information
   */
  public void setAutoDeploy(AutoDeployInfo autoDeploy) {
    m_autoDeploy = autoDeploy;
  }

  /**
   * Getter for the auto-deploy property.
   *
   * @return auto-deploy information
   */
  public AutoDeployInfo getAutoDeploy() {
    return m_autoDeploy;
  }
}
