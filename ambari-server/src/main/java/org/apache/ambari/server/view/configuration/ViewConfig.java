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
   * The list of servlets.
   */
  @XmlElement(name="servlet")
  private List<ServletConfig> servlets;

  /**
   * The mapping of servlet names to servlet classes.
   */
  public Map<String, Class<? extends HttpServlet>> servletPathMap = null;

  /**
   * The list of servlet mappings.
   */
  @XmlElement(name="servlet-mapping")
  private List<ServletMappingConfig> mappings;

  /**
   * The mapping of servlet names to URL patterns.
   */
  public Map<String, String> servletURLPatternMap = null;

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
    return parameters;
  }

  /**
   * Get the list of view resources.
   *
   * @return return the list of resources
   */
  public List<ResourceConfig> getResources() {
    return resources;
  }

  /**
   * Get the list of view instances.
   *
   * @return the list of view instances
   */
  public List<InstanceConfig> getInstances() {
    return instances;
  }

  /**
   * Get the list of servlets.
   *
   * @return the list of view servlets
   */
  public List<ServletConfig> getServlets() {
    return servlets;
  }

  /**
   * Get the list of servlet mappings.
   *
   * @return the list of view servlet mappings.
   */
  public List<ServletMappingConfig> getMappings() {
    return mappings;
  }

  /**
   * Get the mapping of servlet names to servlet classes.
   *
   * @param cl  the class loader
   *
   * @return the mapping of servlet names to servlet classes
   *
   * @throws ClassNotFoundException if a servlet class can not be loaded
   */
  public synchronized Map<String, Class<? extends HttpServlet>> getServletPathMap(ClassLoader cl)
      throws ClassNotFoundException{
    if (servletPathMap == null) {
      servletPathMap = new HashMap<String, Class<? extends HttpServlet>>();
      if (servlets != null) {
        for (ServletConfig servletConfig : servlets) {
          servletPathMap.put(servletConfig.getName(), servletConfig.getServletClass(cl));
        }
      }
    }
    return servletPathMap;
  }

  /**
   * Get the mapping of servlet names to URL patterns.
   *
   * @return the mapping of servlet names to URL patterns
   */
  public synchronized Map<String, String> getServletURLPatternMap() {
    if (servletURLPatternMap == null) {
      servletURLPatternMap = new HashMap<String, String>();
      if (mappings != null) {
        for (ServletMappingConfig servletMappingConfig : mappings) {
          servletURLPatternMap.put(servletMappingConfig.getName(), servletMappingConfig.getUrlPattern());
        }
      }
    }
    return servletURLPatternMap;
  }
}
