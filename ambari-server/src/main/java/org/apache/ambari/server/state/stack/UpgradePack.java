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
package org.apache.ambari.server.state.stack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.ServiceCheckGrouping;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an upgrade pack.
 */
@XmlRootElement(name="upgrade")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradePack {

  private static Logger LOG = LoggerFactory.getLogger(UpgradePack.class);

  /**
   * Name of the file without the extension, such as upgrade-2.2
   */
  private String name;

  @XmlElement(name="target")
  private String target;

  @XmlElement(name="target-stack")
  private String targetStack;

  @XmlElement(name="type", defaultValue="rolling")
  private UpgradeType type;

  @XmlElementWrapper(name="upgrade-path")
  @XmlElement(name="intermediate-stack")
  private List<IntermediateStack> intermediateStacks;

  @XmlElementWrapper(name="order")
  @XmlElement(name="group")
  private List<Grouping> groups;

  @XmlElement(name="prerequisite-checks")
  private PrerequisiteChecks prerequisiteChecks;

  /**
   * In the case of a rolling upgrade, will specify processing logic for a particular component.
   * NonRolling upgrades are simpler so the "processing" is embedded into the  group's "type", which is a function like
   * "stop" or "start".
   */
  @XmlElementWrapper(name="processing")
  @XmlElement(name="service")
  private List<ProcessingService> processing;

  /**
   * {@code true} to automatically skip slave/client component failures. The
   * default is {@code false}.
   */
  @XmlElement(name = "skip-failures")
  private boolean skipFailures = false;

  /**
   * {@code true} to allow downgrade, {@code false} to disable downgrade.
   * Tag is optional and can be {@code null}, use {@code isDowngradeAllowed} getter instead.
   */
  @XmlElement(name = "downgrade-allowed", required = false)
  private boolean downgradeAllowed = true;

  /**
   * {@code true} to automatically skip service check failures. The default is
   * {@code false}.
   */
  @XmlElement(name = "skip-service-check-failures")
  private boolean skipServiceCheckFailures = false;

  @XmlTransient
  private Map<String, List<String>> m_orders = null;

  @XmlTransient
  private Map<String, Map<String, ProcessingComponent>> m_process = null;

  @XmlTransient
  private boolean m_resolvedGroups = false;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  /**
   * @return the target version for the upgrade pack
   */
  public String getTarget() {
    return target;
  }

  /**
   * @return the type of upgrade, e.g., "ROLLING" or "NON_ROLLING"
   */
  public UpgradeType getType() {
    return type;
  }

  /**
   * @return the preCheck name, e.g. "CheckDescription"
   */
  public List<String> getPrerequisiteChecks() {
    return new ArrayList<String>(prerequisiteChecks.checks);
  }

  /**
   *
   * @return the prerequisite check configuration
   */
  public PrerequisiteCheckConfig getPrerequisiteCheckConfig() {
    return prerequisiteChecks.configuration;
  }

  /**
   * Gets a list of stacks which are between the current stack version and the
   * target stack version inclusive. For example, if upgrading from HDP-2.2 to
   * HDP-2.4, this should include HDP-2.3 and HDP-2.4.
   * <p/>
   * This method is used to combine the correct configuration packs for a
   * specific upgrade from
   * {@link AmbariMetaInfo#getConfigUpgradePack(String, String)}.
   *
   * @return a list of intermediate stacks (target stack inclusive) or
   *         {@code null} if none.
   */
  public List<IntermediateStack> getIntermediateStacks() {
    return intermediateStacks;
  }

  /**
   * @return the target stack, or {@code null} if the upgrade is within the same stack
   */
  public String getTargetStack() {
    return targetStack;
  }

  /**
   * Gets whether skippable components that failed are automatically skipped.
   *
   * @return the skipComponentFailures
   */
  public boolean isComponentFailureAutoSkipped() {
    return skipFailures;
  }

  /**
   * Gets whether skippable service checks that failed are automatically
   * skipped.
   *
   * @return the skipServiceCheckFailures
   */
  public boolean isServiceCheckFailureAutoSkipped() {
    return skipServiceCheckFailures;
  }

  /**
   * Gets the groups defined for the upgrade pack. If a direction is defined for
   * a group, it must match the supplied direction to be returned
   *
   * @param direction
   *          the direction to return the ordered groups
   * @return the list of groups
   */
  public List<Grouping> getGroups(Direction direction) {
    List<Grouping> list = new ArrayList<Grouping>();
    if (direction.isUpgrade()) {
      list = groups;
    } else {
      if (type == UpgradeType.ROLLING) {
        list = getDowngradeGroupsForRolling();
      } else if (type == UpgradeType.NON_ROLLING) {
        list = getDowngradeGroupsForNonrolling();
      }
    }

    List<Grouping> checked = new ArrayList<Grouping>();
    for (Grouping group : list) {
      if (null == group.intendedDirection || direction == group.intendedDirection) {
        checked.add(group);
      }
    }

    return checked;
  }

  /**
   * @return {@code true} if upgrade pack supports downgrade or {@code false} if not.
   */
  public boolean isDowngradeAllowed(){
    return downgradeAllowed;
  }

  public boolean canBeApplied(String targetVersion){
    // check that upgrade pack can be applied to selected stack
    // converting 2.2.*.* -> 2\.2(\.\d+)?(\.\d+)?(-\d+)?

    String regexPattern = getTarget().replaceAll("\\.", "\\\\."); // . -> \.
    regexPattern = regexPattern.replaceAll("\\\\\\.\\*", "(\\\\\\.\\\\d+)?"); // \.* -> (\.\d+)?
    regexPattern = regexPattern.concat("(-\\d+)?");
    return Pattern.matches(regexPattern, targetVersion);
  }

  /**
   * Calculates the group orders when performing a rolling downgrade
   * <ul>
   *   <li>ClusterGroupings must remain at the same positions (first/last).</li>
   *   <li>When there is a ServiceCheck group, it must ALWAYS follow the same</li>
   *       preceding group, whether for an upgrade or a downgrade.</li>
   *   <li>All other groups must follow the reverse order.</li>
   * </ul>
   * For example, give the following order of groups:
   * <ol>
   *   <li>PRE_CLUSTER</li>
   *   <li>ZK</li>
   *   <li>CORE_MASTER</li>
   *   <li>SERVICE_CHECK_1</li>
   *   <li>CLIENTS</li>
   *   <li>FLUME</li>
   *   <li>SERVICE_CHECK_2</li>
   *   <li>POST_CLUSTER</li>
   * </ol>
   * The reverse would be:
   * <ol>
   *   <li>PRE_CLUSTER</li>
   *   <li>FLUME</li>
   *   <li>SERVICE_CHECK_2</li>
   *   <li>CLIENTS</li>
   *   <li>CORE_MASTER</li>
   *   <li>SERVICE_CHECK_1</li>
   *   <li>ZK</li>
   *   <li>POST_CLUSTER</li>
   * </ol>
   * @return the list of groups, reversed appropriately for a downgrade.
   */
  private List<Grouping> getDowngradeGroupsForRolling() {
    List<Grouping> reverse = new ArrayList<Grouping>();

    int idx = 0;
    int iter = 0;
    Iterator<Grouping> it = groups.iterator();

    while (it.hasNext()) {
      Grouping g = it.next();
      if (ClusterGrouping.class.isInstance(g)) {
        reverse.add(g);
        idx++;
      } else {
        if (iter+1 < groups.size()) {
          Grouping peek = groups.get(iter+1);
          if (ServiceCheckGrouping.class.isInstance(peek)) {
            reverse.add(idx, it.next());
            reverse.add(idx, g);
            iter++;
          } else {
            reverse.add(idx, g);
          }
        }
      }
      iter++;
    }

    return reverse;
  }

  private List<Grouping> getDowngradeGroupsForNonrolling() {
    List<Grouping> list = new ArrayList<Grouping>();
    for (Grouping g : groups) {
      list.add(g);
    }
    return list;
  }

  /**
   * Gets the tasks by which services and components should be upgraded.
   * @return a map of service_name -> map(component_name -> process).
   */
  public Map<String, Map<String, ProcessingComponent>> getTasks() {

    if (null == m_process) {
      m_process = new LinkedHashMap<String, Map<String, ProcessingComponent>>();

      if (processing != null) {
        for (ProcessingService svc : processing) {
          if (!m_process.containsKey(svc.name)) {
            m_process.put(svc.name, new LinkedHashMap<String, ProcessingComponent>());
          }

          Map<String, ProcessingComponent> componentMap = m_process.get(svc.name);

          for (ProcessingComponent pc : svc.components) {
            if (pc != null) {
              componentMap.put(pc.name, pc);
            } else {
              LOG.warn("ProcessingService {} has null amongst it's values " +
                "(total {} components)", svc.name, svc.components.size());
            }
          }
        }
      }
    }

    return m_process;
  }

  /**
   * A service definition that holds a list of components in the 'order' element.
   */
  public static class OrderService {

    @XmlAttribute(name="name")
    public String serviceName;

    @XmlElement(name="component")
    public List<String> components;
  }

  /**
   * A service definition in the 'processing' element.
   */
  public static class ProcessingService {

    @XmlAttribute
    public String name;

    @XmlElement(name="component")
    public List<ProcessingComponent> components;
  }

  /**
   * A component definition in the 'processing/service' path.
   */
  public static class ProcessingComponent {

    @XmlAttribute
    public String name;

    @XmlElementWrapper(name="pre-upgrade")
    @XmlElement(name="task")
    public List<Task> preTasks;

    @XmlElementWrapper(name="pre-downgrade")
    @XmlElement(name="task")
    public List<Task> preDowngradeTasks;


    @XmlElementWrapper(name="upgrade")
    @XmlElement(name="task")
    public List<Task> tasks;

    @XmlElementWrapper(name="post-upgrade")
    @XmlElement(name="task")
    public List<Task> postTasks;

    @XmlElementWrapper(name="post-downgrade")
    @XmlElement(name="task")
    public List<Task> postDowngradeTasks;
  }

  /**
   * An intermediate stack definition in
   * upgrade/upgrade-path/intermediate-stack path
   */
  public static class IntermediateStack {

    @XmlAttribute
    public String version;
  }

  /**
   * Container class to specify list of additional prerequisite checks to run in addition to the
   * required prerequisite checks and configuration properties for all prerequisite checks
   */
  public static class PrerequisiteChecks {
    /**
     * List of additional prerequisite checks to run in addition to required prerequisite checks
     */
    @XmlElement(name="check", type=String.class)
    public List<String> checks = new ArrayList<String>();

    /**
     * Prerequisite checks configuration
     */
    @XmlElement(name="configuration")
    public PrerequisiteCheckConfig configuration;
  }

  /**
   * Prerequisite checks configuration
   */
  public static class PrerequisiteCheckConfig {
    /**
     * Global config properties common to all prereq checks
     */
    @XmlElement(name="property")
    public List<PrerequisiteProperty> globalProperties;

    /**
     * Config properties for individual prerequisite checks
     */
    @XmlElement(name="check-properties")
    public List<PrerequisiteCheckProperties> prerequisiteCheckProperties;

    /**
     * Get global config properties as a map
     * @return Map of global config properties
     */
    public Map<String, String> getGlobalProperties() {
      if(globalProperties == null) {
        return null;
      }
      Map<String, String> result = new HashMap<String, String>();
      for (PrerequisiteProperty property : globalProperties) {
        result.put(property.name, property.value);
      }
      return result;
    }

    /**
     * Get config properties for a given prerequisite check as a map
     * @param checkName The prerequisite check name
     * @return Map of config properties for the prerequisite check
     */
    public Map<String, String> getCheckProperties(String checkName) {
      if(prerequisiteCheckProperties == null) {
        return null;
      }
      for(PrerequisiteCheckProperties checkProperties : prerequisiteCheckProperties) {
        if(checkProperties.name.equalsIgnoreCase(checkName)) {
          return checkProperties.getProperties();
        }
      }
      return null;
    }
  }

  /**
   * Config properties for a specific prerequisite check.
   */
  public static class PrerequisiteCheckProperties {
    /**
     * Prereq check name
     */
    @XmlAttribute
    public String name;

    /**
     * Config properties for the prerequisite check
     */
    @XmlElement(name="property")
    public List<PrerequisiteProperty> properties;

    /**
     * Get config properties as a map
     * @return Map of config properties
     */
    public Map<String, String> getProperties() {
      if(properties == null) {
        return null;
      }

      Map<String, String> result = new HashMap<String, String>();
      for (PrerequisiteProperty property : properties) {
        result.put(property.name, property.value);
      }
      return result;
    }
  }

  /**
   * Prerequisite check config property
   */
  public static class PrerequisiteProperty {
    @XmlAttribute
    public String name;

    @XmlValue
    public String value;
  }
}
