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
package org.apache.ambari.server.checks;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.state.stack.PrereqCheckType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * The {@link CheckDescription} class wraps information about an upgrade check
 * in a way that is extendable by 3rd party {@link PreUpgradeCheck}s.
 */
public class CheckDescription {

  public static CheckDescription HOSTS_HEARTBEAT = new CheckDescription("HOSTS_HEARTBEAT",
    PrereqCheckType.HOST,
    "All hosts must be communicating with Ambari. Hosts which are not reachable should be placed in Maintenance Mode.",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "There are hosts which are not communicating with Ambari.").build());

  public static CheckDescription HEALTH = new CheckDescription("HEALTH",
    PrereqCheckType.CLUSTER,
    "Cluster Health",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following issues have been detected on this cluster and should be addressed before upgrading: %s").build());

  public static CheckDescription SERVICE_CHECK = new CheckDescription("SERVICE_CHECK",
    PrereqCheckType.SERVICE,
    "Last Service Check should be more recent than the last configuration change for the given service",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following service configurations have been updated and their Service Checks should be run again: %s").build());

  public static CheckDescription HOSTS_MAINTENANCE_MODE = new CheckDescription("HOSTS_MAINTENANCE_MODE",
    PrereqCheckType.HOST,
    "Hosts in Maintenance Mode will be excluded from the upgrade.",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "There are hosts in Maintenance Mode which excludes them from being upgraded.")
      .put(HostMaintenanceModeCheck.KEY_CANNOT_START_HOST_ORDERED,
          "The following hosts cannot be in Maintenance Mode: {{fails}}.").build());

  public static CheckDescription HOSTS_MASTER_MAINTENANCE = new CheckDescription("HOSTS_MASTER_MAINTENANCE",
    PrereqCheckType.HOST,
    "Hosts in Maintenance Mode must not have any master components",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following hosts must not be in in Maintenance Mode since they host Master components: {{fails}}.").build());

  public static CheckDescription SERVICES_MAINTENANCE_MODE = new CheckDescription("SERVICES_MAINTENANCE_MODE",
    PrereqCheckType.SERVICE,
    "No services can be in Maintenance Mode",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following Services must not be in Maintenance Mode: {{fails}}.").build());

  public static CheckDescription SERVICES_UP = new CheckDescription("SERVICES_UP",
    PrereqCheckType.SERVICE,
    "All services must be started",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following Services must be started: {{fails}}. Try to do a Stop & Start in case they were started outside of Ambari.").build());

  public static CheckDescription COMPONENTS_INSTALLATION = new CheckDescription("COMPONENTS_INSTALLATION",
    PrereqCheckType.SERVICE,
    "All service components must be installed",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following Services must be reinstalled: {{fails}}. Try to reinstall the service components in INSTALL_FAILED state.").build());

  public static CheckDescription PREVIOUS_UPGRADE_COMPLETED = new CheckDescription("PREVIOUS_UPGRADE_COMPLETED",
    PrereqCheckType.CLUSTER,
    "A previous upgrade did not complete.",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The last upgrade attempt did not complete. {{fails}}").build());

  public static CheckDescription CONFIG_MERGE = new CheckDescription("CONFIG_MERGE",
    PrereqCheckType.CLUSTER,
    "Configuration Merge Check",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The following config types will have values overwritten: %s").build());

  public static CheckDescription HARDCODED_STACK_VERSION_PROPERTIES_CHECK = new CheckDescription("HARDCODED_STACK_VERSION_PROPERTIES_CHECK",
    PrereqCheckType.CLUSTER,
    "Found hardcoded stack version in property value.",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "Some properties seem to contain hardcoded stack version string \"%s\"." +
          " That is a potential problem when doing stack update.").build());

  public static CheckDescription VERSION_MISMATCH = new CheckDescription("VERSION_MISMATCH",
    PrereqCheckType.HOST,
    "All components must be reporting the expected version",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "There are components which are not reporting the expected stack version: \n%s").build());

  public static CheckDescription SERVICE_PRESENCE_CHECK = new CheckDescription("SERVICE_PRESENCE_CHECK",
    PrereqCheckType.SERVICE,
    "Service Is Not Supported For Upgrades",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
          "The %s service is currently installed on the cluster. " +
          "This service does not support upgrades and must be removed before the upgrade can continue. " +
          "After upgrading, %s can be reinstalled")
      .put(ServicePresenceCheck.KEY_SERVICE_REMOVED,
          "The %s service is currently installed on the cluster. " +
          "This service is removed from the new release and must be removed before the upgrade can continue. " +
          "After upgrading, %s can be installed").build());

  public static CheckDescription AUTO_START_DISABLED = new CheckDescription("AUTO_START_DISABLED",
    PrereqCheckType.CLUSTER,
    "Auto-Start Disabled Check",
    new ImmutableMap.Builder<String, String>()
      .put(ClusterCheck.DEFAULT,
        "Auto Start must be disabled before performing an Upgrade. To disable Auto Start, navigate to " +
          "Admin > Service Auto Start. Turn the toggle switch off to Disabled and hit Save.").build());


  public static CheckDescription LZO_CONFIG_CHECK = new CheckDescription("LZO_CONFIG_CHECK",
      PrereqCheckType.CLUSTER,
      "LZO Codec Check",
      new ImmutableMap.Builder<String, String>()
          .put(ClusterCheck.DEFAULT,
              "You have LZO codec enabled in the core-site config of your cluster. LZO is no longer installed automatically. " +
                  "If any hosts require LZO, it should be installed before starting the upgrade. " +
                  "Consult Ambari documentation for instructions on how to do this.").build());

  public static CheckDescription JAVA_VERSION = new CheckDescription("JAVA_VERSION",
      PrereqCheckType.CLUSTER,
      "Verify Java version requirement",
      new ImmutableMap.Builder<String, String>()
        .put(ClusterCheck.DEFAULT, "Ambari requires JDK with minimum version %s. Reconfigure Ambari with a JDK that meets the version requirement.")
          .build());

  public static CheckDescription COMPONENTS_EXIST_IN_TARGET_REPO = new CheckDescription("COMPONENTS_EXIST_IN_TARGET_REPO",
      PrereqCheckType.CLUSTER,
      "Verify Cluster Components Exist In Target Repository",
      new ImmutableMap.Builder<String, String>()
        .put(ClusterCheck.DEFAULT, "The following components do not exist in the target repository's stack. They must be removed from the cluster before upgrading.")
          .build());

  public static CheckDescription VALID_SERVICES_INCLUDED_IN_REPOSITORY = new CheckDescription("VALID_SERVICES_INCLUDED_IN_REPOSITORY",
      PrereqCheckType.CLUSTER,
      "The repository is missing services which are required",
      new ImmutableMap.Builder<String, String>()
        .put(ClusterCheck.DEFAULT,
            "The following services are included in the upgrade but the repository is missing their dependencies:\n%s").build());

  /**
   * All of the instantiated {@link CheckDescription}s.
   */
  private static final Set<CheckDescription> s_values = new LinkedHashSet<>();

  /**
   * A unique identifier.
   */
  private final String m_name;
  private final PrereqCheckType m_type;
  private final String m_description;
  private Map<String, String> m_fails;

  /**
   * Constructor.
   *
   * @param name
   * @param type
   * @param description
   * @param fails
   */
  public CheckDescription(String name, PrereqCheckType type, String description, Map<String, String> fails) {
    m_name = name;
    m_type = type;
    m_description = description;
    m_fails = fails;

    if (s_values.contains(this)) {
      throw new RuntimeException("Unable to add the upgrade check description named " + m_name
          + " because it already is registered");
    }

    s_values.add(this);
  }

  /**
   * @return the name of check
   */
  public String name() {
    return m_name;
  }

  /**
   * Gets all of the registered check descriptions.
   *
   * @return
   */
  public Set<CheckDescription> values() {
    return s_values;
  }

  /**
   * @return the type of check
   */
  public PrereqCheckType getType() {
    return m_type;
  }

  /**
   * @return the text associated with the description
   */
  public String getText() {
    return m_description;
  }

  /**
   * @param key the failure text key
   * @return the fail text template.  Never {@code null}
   */
  public String getFail(String key) {
    return m_fails.containsKey(key) ? m_fails.get(key) : "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(m_name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (this == object) {
      return true;
    }

    if (object.getClass() != getClass()) {
      return false;
    }

    CheckDescription that = (CheckDescription) object;

    return Objects.equals(m_name, that.m_name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", m_name).toString();
  }
}
