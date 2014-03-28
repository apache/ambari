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

package org.apache.ambari.server.view.configuration;

import javax.servlet.http.HttpServlet;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View configuration.
 */
@XmlRootElement(name="view")
@XmlAccessorType(XmlAccessType.FIELD)
public class ViewConfig {
  /**
   * The unique view name.
   */
  private String name;

  /**
   * The public view name.
   */
  private String label;

  /**
   * The view version.
   */
  private String version;

  /**
   * The list of view parameters.
   */
  @XmlElement(name="parameter")
  private List<ParameterConfig> parameters;

  /**
   * The list of view resources.
   */
  @XmlElement(name="resource")
  private List<ResourceConfig> resources;

  /**
   * The list of view instances.
   */
  @XmlElement(name="instance")
  private List<InstanceConfig> instances;

  /**
   * Get the unique name.
   *
   * @return the view name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the public view name.
   *
   * @return the view label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Get the view version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Get the list of view parameters.
   *
   * @return the list of parameters
   */
  public List<ParameterConfig> getParameters() {
    return parameters == null ? Collections.<ParameterConfig>emptyList() : parameters;
  }

  /**
   * Get the list of view resources.
   *
   * @return return the list of resources
   */
  public List<ResourceConfig> getResources() {
    return resources == null ? Collections.<ResourceConfig>emptyList() : resources;
  }

  /**
   * Get the list of view instances.
   *
   * @return the list of view instances
   */
  public List<InstanceConfig> getInstances() {
    return instances == null ? Collections.<InstanceConfig>emptyList() : instances;
  }
}
