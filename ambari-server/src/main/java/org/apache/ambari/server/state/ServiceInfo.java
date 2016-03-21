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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.Validable;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonFilter;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonFilter("propertiesfilter")
public class ServiceInfo implements Validable{

  /**
   * Format version. Added at schema ver 2
   */
  @XmlTransient
  private String schemaVersion;

  private String name;
  private String displayName;
  private String version;
  private String comment;
  private String serviceType;
  private List<PropertyInfo> properties;

  @XmlElementWrapper(name="components")
  @XmlElements(@XmlElement(name="component"))
  private List<ComponentInfo> components;

  @XmlElement(name="deleted")
  private boolean isDeleted = false;

  @JsonIgnore
  @XmlTransient
  private volatile Map<String, Set<String>> configLayout = null;

  @XmlElementWrapper(name="configuration-dependencies")
  @XmlElement(name="config-type")
  private List<String> configDependencies;

  @XmlElementWrapper(name="excluded-config-types")
  @XmlElement(name="config-type")
  private Set<String> excludedConfigTypes = new HashSet<String>();

  @XmlTransient
  private Map<String, Map<String, Map<String, String>>> configTypes;

  @JsonIgnore
  private Boolean monitoringService;
  
  @JsonIgnore
  @XmlElement(name = "restartRequiredAfterChange")
  private Boolean restartRequiredAfterChange;

  @JsonIgnore
  @XmlElement(name = "restartRequiredAfterRackChange")
  private Boolean restartRequiredAfterRackChange;

  @XmlElement(name = "extends")
  private String parent;

  @XmlElement(name = "widgetsFileName")
  private String widgetsFileName = AmbariMetaInfo.WIDGETS_DESCRIPTOR_FILE_NAME;

  @XmlElement(name = "metricsFileName")
  private String metricsFileName = AmbariMetaInfo.SERVICE_METRIC_FILE_NAME;

  @XmlTransient
  private volatile Map<String, PropertyInfo> requiredProperties;

  public Boolean isRestartRequiredAfterChange() {
    return restartRequiredAfterChange;
  }

  public void setRestartRequiredAfterChange(Boolean restartRequiredAfterChange) {
    this.restartRequiredAfterChange = restartRequiredAfterChange;
  }

  @XmlTransient
  private File metricsFile = null;

  @XmlTransient
  private Map<String, Map<String, List<MetricDefinition>>> metrics = null;
  
  @XmlTransient
  private File alertsFile = null;

  @XmlTransient
  private File kerberosDescriptorFile = null;

  @XmlTransient
  private File widgetsDescriptorFile = null;
  
  @XmlTransient
  private boolean valid = true;

  /**
   * 
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   * 
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @XmlTransient
  private Set<String> errorSet = new HashSet<String>();
  
  @Override
  public void setErrors(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection getErrors() {
    return errorSet;
  }   
  
  @Override
  public void setErrors(Collection error) {
    this.errorSet.addAll(error);
  }
  /**
   * Internal list of os-specific details (loaded from xml). Added at schema ver 2
   */
  @JsonIgnore
  @XmlElementWrapper(name="osSpecifics")
  @XmlElements(@XmlElement(name="osSpecific"))
  private List<ServiceOsSpecific> serviceOsSpecifics;
  
  @JsonIgnore
  @XmlElement(name="configuration-dir")
  private String configDir = AmbariMetaInfo.SERVICE_CONFIG_FOLDER_NAME;

  @JsonIgnore
  @XmlElement(name = "themes-dir")
  private String themesDir = AmbariMetaInfo.SERVICE_THEMES_FOLDER_NAME;

  @JsonIgnore
  @XmlElementWrapper(name = "themes")
  @XmlElements(@XmlElement(name = "theme"))
  private List<ThemeInfo> themes;

  @XmlTransient
  private volatile Map<String, ThemeInfo> themesMap;


  /**
   * Map of of os-specific details that is exposed (and initialised from list)
   * at getter.
   * Added at schema ver 2
   */
  private volatile Map<String, ServiceOsSpecific> serviceOsSpecificsMap;

  /**
   * This is used to add service check actions for services.
   * Added at schema ver 2
   */
  private CommandScriptDefinition commandScript;

  /**
   * Added at schema ver 2
   */
  @XmlElementWrapper(name="customCommands")
  @XmlElements(@XmlElement(name="customCommand"))
  private List<CustomCommandDefinition> customCommands;
  
  @XmlElementWrapper(name="requiredServices")
  @XmlElement(name="service")
  private List<String> requiredServices = new ArrayList<String>();

  /**
   * Meaning: stores subpath from stack root to exact directory, that contains
   * service scripts and templates. Since schema ver 2,
   * we may have multiple service metadata inside folder.
   * Added at schema ver 2
   */
  @XmlTransient
  private String servicePackageFolder;

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

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  
  public String getServiceType() {
	return serviceType;
  }

  public void setServiceType(String serviceType) {
	this.serviceType = serviceType;
  }

public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
  public List<String> getRequiredServices() {
    return requiredServices;
  }

  public String getWidgetsFileName() {
    return widgetsFileName;
  }

  public void setWidgetsFileName(String widgetsFileName) {
    this.widgetsFileName = widgetsFileName;
  }

  public String getMetricsFileName() {
    return metricsFileName;
  }

  public void setMetricsFileName(String metricsFileName) {
    this.metricsFileName = metricsFileName;
  }

  public void setRequiredServices(List<String> requiredServices) {
    this.requiredServices = requiredServices;
  }
  public List<PropertyInfo> getProperties() {
    if (properties == null) properties = new ArrayList<PropertyInfo>();
    return properties;
  }

  public List<ComponentInfo> getComponents() {
    if (components == null) components = new ArrayList<ComponentInfo>();
    return components;
  }
  /**
   * Finds ComponentInfo by component name
   * @param componentName  name of the component
   * @return ComponentInfo componentName or null
   */
  public ComponentInfo getComponentByName(String componentName){
    for(ComponentInfo componentInfo : getComponents()) {
      if(componentInfo.getName().equals(componentName)){
        return componentInfo;
      }
    }
    return null;
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
    ComponentInfo client = null;

    if (components != null) {
      for (ComponentInfo compInfo : components) {
        if (compInfo.isClient()) {
          client = compInfo;
          break;
        }
      }
    }
    return client;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Service name:");
    sb.append(name);
    sb.append("\nService type:");
    sb.append(serviceType); 
    sb.append("\nversion:");
    sb.append(version);
    sb.append("\ncomment:");
    sb.append(comment);
    //for (PropertyInfo property : getProperties()) {
    //  sb.append("\tProperty name=" + property.getName() +
    //"\nproperty value=" + property.getValue() + "\ndescription=" + property.getDescription());
    //}
    for (ComponentInfo component : getComponents()) {
      sb.append("\n\n\nComponent:\n");
      sb.append("name=");
      sb.append(component.getName());
      sb.append("\tcategory=");
      sb.append(component.getCategory());
    }

    return sb.toString();
  }

  /**
   * Obtain the config types associated with this service.
   * The returned map is an unmodifiable view.
   * @return unmodifiable map of config types associated with this service
   */
  public synchronized Map<String, Map<String, Map<String, String>>> getConfigTypeAttributes() {
    Map<String, Map<String, Map<String, String>>> tmpConfigTypes = configTypes == null ?
        new HashMap<String, Map<String, Map<String, String>>>() : configTypes;

    for(String excludedtype : excludedConfigTypes){
      tmpConfigTypes.remove(excludedtype);
    }

    return Collections.unmodifiableMap(tmpConfigTypes);
  }

  /**
   * Add the given type and set it's attributes.
   * If the type is marked for exclusion, it will not be added.
   *
   * @param type            configuration type
   * @param typeAttributes  attributes associated with the type
   */
  public synchronized void setTypeAttributes(String type, Map<String, Map<String, String>> typeAttributes) {
    if (this.configTypes == null) {
      configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    }
    configTypes.put(type, typeAttributes);
  }

  /**
   * Set all types and associated attributes.  Any previously existing types and
   * attributes are removed prior to setting the new values.
   *
   * @param types map of type attributes
   */
  public synchronized void setAllConfigAttributes(Map<String, Map<String, Map<String, String>>> types) {
    configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    for (Map.Entry<String, Map<String, Map<String, String>>> entry : types.entrySet()) {
      setTypeAttributes(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Determine of the service has a dependency on the provided configuration type.
   * @param type the config type
   * @return <code>true</code> if the service defines a dependency on the provided type
   */
  public boolean hasConfigDependency(String type) {
    return configDependencies != null && configDependencies.contains(type);
  }

  /**
   * Determine if the service contains the specified config type
   * @param type  config type to check
   * @return true if the service has the specified config type; false otherwise
   */
  public boolean hasConfigType(String type) {
    return configTypes != null && configTypes.containsKey(type)
        && !excludedConfigTypes.contains(type);
  }

  /**
   * Determine if the service has a dependency on the provided type and contains any of the provided properties.
   * This can be used in determining if a property is stale.

   * @param type the config type
   * @param keyNames the names of all the config keys for the given type 
   * @return <code>true</code> if the config is stale
   */
  public boolean hasDependencyAndPropertyFor(String type, Collection<String> keyNames) {
    if (!hasConfigDependency(type))
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
  public List<String> getConfigDependenciesWithComponents(){
    List<String> retVal = new ArrayList<String>();
    if(configDependencies != null){
      retVal.addAll(configDependencies);
    }
    if(components != null){
      for (ComponentInfo c : components) {
        if(c.getConfigDependencies() != null){
          retVal.addAll(c.getConfigDependencies());
        }
      }
    }
    return retVal.size() == 0 ? (configDependencies == null ? null : configDependencies) : retVal;
  }

  public void setConfigDependencies(List<String> configDependencies) {
    this.configDependencies = configDependencies;
  }

  public String getSchemaVersion() {
    if (schemaVersion == null) {
      return AmbariMetaInfo.SCHEMA_VERSION_2;
    } else {
      return schemaVersion;
    }
  }


  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }


  public String getServicePackageFolder() {
    return servicePackageFolder;
  }

  public void setServicePackageFolder(String servicePackageFolder) {
    this.servicePackageFolder = servicePackageFolder;
  }

  /**
   * Exposes (and initializes on first use) map of os-specific details.
   * @return  map of OS specific details keyed by family
   */
  public Map<String, ServiceOsSpecific> getOsSpecifics() {
    if (serviceOsSpecificsMap == null) {
      synchronized (this) { // Double-checked locking pattern
        if (serviceOsSpecificsMap == null) {
          Map<String, ServiceOsSpecific> tmpMap =
                  new TreeMap<String, ServiceOsSpecific>();
          if (serviceOsSpecifics != null) {
            for (ServiceOsSpecific osSpecific : serviceOsSpecifics) {
              tmpMap.put(osSpecific.getOsFamily(), osSpecific);
            }
          }
          serviceOsSpecificsMap = tmpMap;
        }
      }
    }
    return serviceOsSpecificsMap;
  }

  public void setOsSpecifics(Map<String, ServiceOsSpecific> serviceOsSpecificsMap) {
    this.serviceOsSpecificsMap = serviceOsSpecificsMap;
  }

  public List<CustomCommandDefinition> getCustomCommands() {
    if (customCommands == null) {
      customCommands = new ArrayList<CustomCommandDefinition>();
    }
    return customCommands;
  }

  public void setCustomCommands(List<CustomCommandDefinition> customCommands) {
    this.customCommands = customCommands;
  }

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }

  public void setCommandScript(CommandScriptDefinition commandScript) {
    this.commandScript = commandScript;
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
  
  /**
   * @return the configuration directory name
   */
  public String getConfigDir() {
    return configDir;
  }

  /**
   * @return whether the service is a monitoring service
   */
  public Boolean isMonitoringService() {
    return monitoringService;
  }

  /**
   * @param monitoringService whether the service is a monitoring service
   */
  public void setMonitoringService(Boolean monitoringService) {
    this.monitoringService = monitoringService;
  }

  /**
   * @param file the file containing the alert definitions
   */
  public void setAlertsFile(File file) {
    alertsFile = file;
  }

  /**
   * @return the alerts file, or <code>null</code> if none exists
   */
  public File getAlertsFile() {
    return alertsFile;
  }

  /**
   * @param file the file containing the alert definitions
   */
  public void setKerberosDescriptorFile(File file) {
    kerberosDescriptorFile = file;
  }

  /**
   * @return the kerberos descriptor file, or <code>null</code> if none exists
   */
  public File getKerberosDescriptorFile() {
    return kerberosDescriptorFile;
  }

  /**
   * @return the widgets descriptor file, or <code>null</code> if none exists
   */
  public File getWidgetsDescriptorFile() {
    return widgetsDescriptorFile;
  }

  public void setWidgetsDescriptorFile(File widgetsDescriptorFile) {
    this.widgetsDescriptorFile = widgetsDescriptorFile;
  }

  /**
   * @return config types this service contains configuration for, but which are primarily related to another service
   */
  public Set<String> getExcludedConfigTypes() {
    return excludedConfigTypes;
  }

  public void setExcludedConfigTypes(Set<String> excludedConfigTypes) {
    this.excludedConfigTypes = excludedConfigTypes;
  }

  //todo: ensure that required properties are never modified...
  public Map<String, PropertyInfo> getRequiredProperties() {
    Map<String, PropertyInfo> result = requiredProperties;
    if (result == null) {
      synchronized(this) {
        result = requiredProperties;
        if (result == null) {
          requiredProperties = result = new HashMap<String, PropertyInfo>();
          List<PropertyInfo> properties = getProperties();
          for (PropertyInfo propertyInfo : properties) {
            if (propertyInfo.isRequireInput()) {
              result.put(propertyInfo.getName(), propertyInfo);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Determine whether or not a restart is required for this service after a host rack info change.
   *
   * @return true if a restart is required
   */
  public Boolean isRestartRequiredAfterRackChange() {
    return restartRequiredAfterRackChange;
  }

  /**
   * Set indicator for required restart after a host rack info change.
   *
   * @param restartRequiredAfterRackChange  true if a restart is required
   */
  public void setRestartRequiredAfterRackChange(Boolean restartRequiredAfterRackChange) {
    this.restartRequiredAfterRackChange = restartRequiredAfterRackChange;
  }

  public String getThemesDir() {
    return themesDir;
  }

  public void setThemesDir(String themesDir) {
    this.themesDir = themesDir;
  }

  public List<ThemeInfo> getThemes() {
    return themes;
  }

  public void setThemes(List<ThemeInfo> themes) {
    this.themes = themes;
  }

  public Map<String, ThemeInfo> getThemesMap() {
    if (themesMap == null) {
      synchronized (this) {
      }
      if (themesMap == null) {
        Map<String, ThemeInfo> tmp = new TreeMap<String, ThemeInfo>();
        if (themes != null) {
          for (ThemeInfo theme : themes) {
            tmp.put(theme.getFileName(), theme);
          }
        }
        themesMap = tmp;
      }
    }
    return themesMap;
  }

  public void setThemesMap(Map<String, ThemeInfo> themesMap) {
    this.themesMap = themesMap;
  }
}
