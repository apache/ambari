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

import java.io.File;
import java.util.*;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonFilter;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonFilter("propertiesfilter")
public class ServiceInfo {
  /**
   * Format version. Added at schema ver 2
   */
  @XmlTransient
  private String schemaVersion;

  private String name;
  private String version;
  private String user;
  private String comment;
  private List<PropertyInfo> properties;

  @XmlElementWrapper(name="components")
  @XmlElements(@XmlElement(name="component"))
  private List<ComponentInfo> components;

  private boolean isDeleted = false;

  @JsonIgnore
  @XmlTransient
  private volatile Map<String, Set<String>> configLayout = null;

  @XmlElementWrapper(name="configuration-dependencies")
  @XmlElement(name="config-type")
  private List<String> configDependencies;
  
  @XmlTransient
  private File metricsFile = null;
  @XmlTransient
  private Map<String, Map<String, List<MetricDefinition>>> metrics = null;


  /**
   * Internal list of os-specific details (loaded from xml). Added at schema ver 2
   */
  @JsonIgnore
  @XmlElementWrapper(name="osSpecifics")
  @XmlElements(@XmlElement(name="osSpecific"))
  private List<ServiceOsSpecific> serviceOsSpecifics;


  /**
   * Map of of os-specific details that is exposed (and initialised from list)
   * at getter.
   * Added at schema ver 2
   */
  private volatile Map<String, ServiceOsSpecific> serviceOsSpecificsMap;


  /**
   * Added at schema ver 2
   */
  private CommandScriptDefinition commandScript;

  /**
   * Added at schema ver 2
   */
  @XmlElementWrapper(name="customCommands")
  @XmlElements(@XmlElement(name="customCommand"))
  private List<CustomCommandDefinition> customCommands;


  /**
   * Directory, that contains service metadata. Since schema ver 2,
   * we may have multiple service metadata inside folder.
   * Added at schema ver 2
   */
  @XmlTransient
  private String serviceMetadataFolder;

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<PropertyInfo> getProperties() {
    if (properties == null) properties = new ArrayList<PropertyInfo>();
    return properties;
  }

  public List<ComponentInfo> getComponents() {
    if (components == null) components = new ArrayList<ComponentInfo>();
    return components;
  }

  public boolean isClientOnlyService() {
    if (components == null || components.isEmpty()) {
      return false;
    }
    for (ComponentInfo compInfo : components) {
      if (!compInfo.isClient()) {
        return false;
      }
    }
    return true;
  }

  public ComponentInfo getClientComponent() {
    if (components == null || components.isEmpty()) {
      return null;
    }
    for (ComponentInfo compInfo : components) {
      if (compInfo.isClient()) {
        return compInfo;
      }
    }
    return components.get(0);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Service name:" + name + "\nversion:" + version +
        "\nuser:" + user + "\ncomment:" + comment);
    //for (PropertyInfo property : getProperties()) {
    //  sb.append("\tProperty name=" + property.getName() +
    //"\nproperty value=" + property.getValue() + "\ndescription=" + property.getDescription());
    //}
    for (ComponentInfo component : getComponents()) {
      sb.append("\n\n\nComponent:\n");
      sb.append("name=" + component.getName());
      sb.append("\tcategory=" + component.getCategory());
    }

    return sb.toString();
  }
  
  public StackServiceResponse convertToResponse()
  {
    return new StackServiceResponse(getName(), getUser(), getComment(), getVersion(),
        getConfigTypes());
  }
  
  public List<String> getConfigTypes() {
    return configDependencies != null ? configDependencies :
      Collections.unmodifiableList(new ArrayList<String>());
  }

  
  /**
   * @param type the config type
   * @return <code>true</code> if the service defines the supplied type
   */
  public boolean hasConfigType(String type) {
    return configDependencies != null && configDependencies.contains(type);
  }

  /**
   * The purpose of this method is to determine if a service has a property
   * defined in a supplied set:
   * <ul>
   *   <li>If the type is not defined for the service, then no property can exist.</li>
   *   <li>If the type is defined, then check each supplied property for existence.</li>
   * </ul>
   * @param type the config type
   * @param keyNames the names of all the config keys for the given type 
   * @return <code>true</code> if the config is stale
   */
  public boolean hasPropertyFor(String type, Collection<String> keyNames) {
    if (!hasConfigType(type))
      return false;

    buildConfigLayout();
    Set<String> keys = configLayout.get(type);

    for (String staleCheck : keyNames) {
      if (keys != null && keys.contains(staleCheck))
        return true;
    }
    
    return false;
  }

  /**
   * Builds the config map specific to this service.
   */
  private void buildConfigLayout() {
    if (null == configLayout) {
      synchronized(this) {
        if (null == configLayout) {
          configLayout = new HashMap<String, Set<String>>();

          for (PropertyInfo pi : getProperties()) {
            String type = pi.getFilename();
            int idx = type.indexOf(".xml");
            type = type.substring(0, idx);

            if (!configLayout.containsKey(type))
              configLayout.put(type, new HashSet<String>());

            configLayout.get(type).add(pi.getName());
          }
        }
      }
    }
  }

  public List<String> getConfigDependencies() {
    return configDependencies;
  }

  public void setConfigDependencies(List<String> configDependencies) {
    this.configDependencies = configDependencies;
  }

  public String getSchemaVersion() {
    if (schemaVersion == null) {
      return AmbariMetaInfo.SCHEMA_VERSION_LEGACY;
    } else {
      return schemaVersion;
    }
  }


  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }


  public String getServiceMetadataFolder() {
    return serviceMetadataFolder;
  }

  public void setServiceMetadataFolder(String serviceMetadataFolder) {
    this.serviceMetadataFolder = serviceMetadataFolder;
  }

  /**
   * Exposes (and initializes on first use) map of os-specific details.
   * @return
   */
  public Map<String, ServiceOsSpecific> getOsSpecifics() {
    if (serviceOsSpecificsMap == null) {
      synchronized (this) { // Double-checked locking pattern
        if (serviceOsSpecificsMap == null) {
          Map<String, ServiceOsSpecific> tmpMap =
                  new TreeMap<String, ServiceOsSpecific>();
          if (serviceOsSpecifics != null) {
            for (ServiceOsSpecific osSpecific : serviceOsSpecifics) {
              tmpMap.put(osSpecific.getOsType(), osSpecific);
            }
          }
          serviceOsSpecificsMap = tmpMap;
        }
      }
    }
    return serviceOsSpecificsMap;
  }

  public List<CustomCommandDefinition> getCustomCommands() {
    if (customCommands == null) {
      customCommands = new ArrayList<CustomCommandDefinition>();
    }
    return customCommands;
  }

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }

  /**
   * @param file the file containing the metrics definitions
   */
  public void setMetricsFile(File file) {
    metricsFile = file;
  }
  
  /**
   * @return the metrics file, or <code>null</code> if none exists
   */
  public File getMetricsFile() {
    return metricsFile;
  }

  /**
   * @return the metrics defined for this service
   */
  public Map<String, Map<String, List<MetricDefinition>>> getMetrics() {
    return metrics;
  }
  
  /**
   * @param map the metrics for this service
   */
  public void setMetrics(Map<String, Map<String, List<MetricDefinition>>> map) {
    metrics = map;
  }

}
