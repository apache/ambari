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
package org.apache.ambari.spi.upgrade;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link UpgradeActionOperations} is used to instruct Ambari Server to perform
 * actions during an upgrade. It is returns by
 * {@link UpgradeAction#getOperations()}.
 */
public class UpgradeActionOperations {

  /**
   * Any configuration changes which should be made.
   */
  private List<ConfigurationChanges> m_configurationChanges;

  /**
   * A buffer that the {@link UpgradeAction} can use to pass messages back to
   * Ambari to display to the user.
   */
  private StringBuilder m_standardOutput;

  /**
   * Sets configuration changes which are a part of the actions to be perfomred
   * during the upgrade.
   *
   * @param configurationChanges
   *          the configuration changes to make.
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   */
  public UpgradeActionOperations setConfigurationChanges(
      List<ConfigurationChanges> configurationChanges) {
    m_configurationChanges = configurationChanges;
    return this;
  }

  /**
   * Sets a {@link StringBuilder} which is used by the server to display
   * messages about what the action did.
   *
   * @param standardOutput
   * @return an instance of the {@link UpgradeActionOperations} with the value
   *         set.
   */
  public UpgradeActionOperations setStandardOutput(StringBuilder standardOutput) {
    m_standardOutput = standardOutput;
    return this;
  }

  /**
   * Gets the configurations changes which should be performed by the server.
   *
   * @return the configuration changes.
   */
  public List<ConfigurationChanges> getConfigurationChanges() {
    return m_configurationChanges;
  }

  /**
   * Gets the standard output, if any, for the server to display as part of the
   * action being run.
   *
   * @return any messages that should be display along with the command's
   *         status.
   */
  public StringBuilder getStandardOutput() {
    return m_standardOutput;
  }

  /**
   * The type of configuration change being made.
   */
  public enum ChangeType {
    /**
     * Adds or updates the property key along with the supplied value.
     */
    SET,

    /**
     * Removes the specified property key from the configuration type.
     */
    REMOVE
  }

  /**
   * The additions, updates, and removals for a specific configuration type,
   * such as {@code foo-site}.
   */
  public static class ConfigurationChanges {
    /**
     * The configuration type, such as {@code foo-site}.
     */
    private final String m_configType;

    /**
     * The property changes for this configuration type.
     */
    private final List<PropertyChange> m_changes = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param configType
     */
    public ConfigurationChanges(String configType) {
      m_configType = configType;
    }

    /**
     * @param propertyName
     * @param propertyValue
     * @return
     */
    public ConfigurationChanges set(String propertyName, String propertyValue) {
      PropertyChange propertyChange = new PropertyChange(ChangeType.SET, propertyName,
          propertyValue);

      m_changes.add(propertyChange);
      return this;
    }

    /**
     * @param propertyName
     * @return
     */
    public ConfigurationChanges remove(String propertyName) {
      PropertyChange propertyChange = new PropertyChange(ChangeType.REMOVE, propertyName, null);
      m_changes.add(propertyChange);
      return this;
    }

    /**
     * Gets the name of the configuration tyoe, such as {@code foo-site}.
     *
     * @return the config type name.
     */
    public String getConfigType() {
      return m_configType;
    }

    /**
     * Gets all of the additions, updates, and removals for this configuration
     * type.
     *
     * @return
     */
    public List<PropertyChange> getPropertyChanges() {
      return m_changes;
    }
  }

  /**
   * The {@link PropertyChange} class represents either the addition, setting,
   * or removal of a configuration property.
   */
  public static class PropertyChange {
    /**
     * The change type.
     */
    private final ChangeType m_changeType;

    /**
     * The name of the property.
     */
    private final String m_propertyName;

    /**
     * The value to use if the type is {@link ConfigurationChangeType#SET}.
     */
    private final String m_propertyValue;

    /**
     * Constructor.
     *
     * @param changeType
     *          the type of configuration change.
     * @param propertyName
     *          the name of the property being added, updated, or removed.
     * @param propertyValue
     *          the value to add/update if the type is
     *          {@link ConfigurationChangeType#SET}.
     */
    public PropertyChange(ChangeType changeType,
        String propertyName, String propertyValue) {
      m_changeType = changeType;
      m_propertyName = propertyName;
      m_propertyValue = propertyValue;
    }

    /**
     * Gets the type of configuration change.
     *
     * @return the change type.
     */
    public ChangeType getChangeType() {
      return m_changeType;
    }

    /**
     * Gets the name of the property to add, update, or remove.
     *
     * @return the property name.
     */
    public String getPropertyName() {
      return m_propertyName;
    }

    /**
     * Gets the name of the property value to set if the configuration type is
     * {@link ConfigurationChangeType#SET}.
     *
     * @return the property value.
     */
    public String getPropertyValue() {
      return m_propertyValue;
    }
  }
}
