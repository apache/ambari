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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.serveraction.upgrades.ConfigureAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Condition;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Replace;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Transfer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link ConfigureTask} represents a configuration change. This task
 * contains id of change. Change definitions are located in a separate file (config
 * upgrade pack). IDs of change definitions share the same namespace within all
 * stacks
 * <p/>
 *
 * <pre>
 * {@code
 * <task xsi:type="configure" id="hdp_2_3_0_0-UpdateHiveConfig"/>
 * }
 * </pre>
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="configure")
public class ConfigureTask extends ServerSideActionTask {

  private static Logger LOG = LoggerFactory.getLogger(ConfigureTask.class);

  /**
   * The key that represents the configuration type to change (ie hdfs-site).
   */
  public static final String PARAMETER_CONFIG_TYPE = "configure-task-config-type";

  /**
   * Setting key/value pairs can be several per task, so they're passed in as a
   * json-ified list of objects.
   */
  public static final String PARAMETER_KEY_VALUE_PAIRS = "configure-task-key-value-pairs";

  /**
   * Transfers can be several per task, so they're passed in as a json-ified
   * list of objects.
   */
  public static final String PARAMETER_TRANSFERS = "configure-task-transfers";

  /**
   * Replacements can be several per task, so they're passed in as a json-ified list of
   * objects.
   */
  public static final String PARAMETER_REPLACEMENTS = "configure-task-replacements";

  public static final String actionVerb = "Configuring";
  
  /**
   * Gson
   */
  private Gson m_gson = new Gson();

  /**
   * Constructor.
   */
  public ConfigureTask() {
    implClass = ConfigureAction.class.getName();
  }

  private Task.Type type = Task.Type.CONFIGURE;

  @XmlAttribute(name = "id")
  public String id;

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return type;
  }

  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.SERVER_SIDE_ACTION;
  }

  @Override
  public String getActionVerb() {
    return actionVerb;
  }

  /**
   * This getter is intended to be used only from tests. In production,
   * getConfigurationChanges() logic should be used instead
   * @return id of config upgrade change definition as defined in upgrade pack
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the summary of the task or {@code null}.
   *
   * @return the task summary or {@code null}.
   */
  public String getSummary(ConfigUpgradePack configUpgradePack) {
    if(StringUtils.isNotBlank(id) && null != configUpgradePack){
      ConfigUpgradeChangeDefinition definition = configUpgradePack.enumerateConfigChangesByID().get(id);
      if (null != definition && StringUtils.isNotBlank(definition.summary)) {
          return definition.summary;
      }
    }

    return super.getSummary();
  }

  /**
   * Gets a map containing the following properties pertaining to the
   * configuration value to change:
   * <ul>
   * <li>{@link #PARAMETER_CONFIG_TYPE} - the configuration type (ie hdfs-site)</li>
   * <li>{@link #PARAMETER_KEY_VALUE_PAIRS} - key/value pairs for the
   * configurations</li>
   * <li>{@link #PARAMETER_KEY_VALUE_PAIRS} - key/value pairs for the
   * configurations</li>
   * <li>{@link #PARAMETER_TRANSFERS} - COPY/MOVE/DELETE changes</li>
   * <li>{@link #PARAMETER_REPLACEMENTS} - value replacements</li>
   * </ul>
   *
   * @param cluster
   *          the cluster to use when retrieving conditional properties to test
   *          against (not {@code null}).
   * @return the a map containing the changes to make. This could potentially be
   *         an empty map if no conditions are met. Callers should decide how to
   *         handle a configuration task that is unable to set any configuration
   *         values.
   */
  public Map<String, String> getConfigurationChanges(Cluster cluster,
                                                     ConfigUpgradePack configUpgradePack) {
    Map<String, String> configParameters = new HashMap<>();

    if (id == null || id.isEmpty()) {
      LOG.warn("Config task id is not defined, skipping config change");
      return configParameters;
    }

    if (configUpgradePack == null) {
      LOG.warn("Config upgrade pack is not defined, skipping config change");
      return configParameters;
    }

    // extract config change definition, referenced by current ConfigureTask
    ConfigUpgradeChangeDefinition definition = configUpgradePack.enumerateConfigChangesByID().get(id);
    if (definition == null) {
      LOG.warn(String.format("Can not resolve config change definition by id %s, " +
              "skipping config change", id));
      return configParameters;
    }

    // the first matched condition will win; conditions make configuration tasks singular in
    // the properties that can be set - when there is a condition the task will only contain
    // conditions
    List<Condition> conditions = definition.getConditions();
    if( null != conditions && !conditions.isEmpty() ){
      for (Condition condition : conditions) {
        String conditionConfigType = condition.getConditionConfigType();
        String conditionKey = condition.getConditionKey();
        String conditionValue = condition.getConditionValue();

        // always add the condition's target type just so that we have one to
        // return even if none of the conditions match
        configParameters.put(PARAMETER_CONFIG_TYPE, condition.getConfigType());

        // check the condition; if it passes, set the configuration properties
        // and break
        String checkValue = getDesiredConfigurationValue(cluster,
            conditionConfigType, conditionKey);

        if (conditionValue.equals(checkValue)) {
          List<ConfigurationKeyValue> configurations = new ArrayList<>(1);
          ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
          keyValue.key = condition.getKey();
          keyValue.value = condition.getValue();
          configurations.add(keyValue);

          configParameters.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS,
              m_gson.toJson(configurations));

          return configParameters;
        }
      }
    }

    // this task is not a condition task, so process the other elements normally
    if (null != definition.getConfigType()) {
      configParameters.put(PARAMETER_CONFIG_TYPE, definition.getConfigType());
    }

    // for every <set key=foo value=bar/> add it to this list
    if (null != definition.getKeyValuePairs() && !definition.getKeyValuePairs().isEmpty()) {
      configParameters.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS,
          m_gson.toJson(definition.getKeyValuePairs()));
    }

    // transfers
    List<Transfer> transfers = definition.getTransfers();
    if (null != transfers && !transfers.isEmpty()) {

      List<Transfer> allowedTransfers = new ArrayList<>();
      for (Transfer transfer : transfers) {
        if (transfer.operation == TransferOperation.DELETE) {
          boolean ifKeyIsNotBlank = StringUtils.isNotBlank(transfer.ifKey);
          boolean ifTypeIsNotBlank = StringUtils.isNotBlank(transfer.ifType);

          //  value doesn't required for Key Check
          if (ifKeyIsNotBlank && ifTypeIsNotBlank && transfer.ifKeyState == PropertyKeyState.ABSENT) {
            boolean keyPresent = getDesiredConfigurationKeyPresence(cluster, transfer.ifType, transfer.ifKey);
            if (keyPresent) {
              LOG.info("Skipping property delete for {}/{} as the key {} for {} is present",
                definition.getConfigType(), transfer.deleteKey, transfer.ifKey, transfer.ifType);
              continue;
            }
          }

          if (ifKeyIsNotBlank && ifTypeIsNotBlank && transfer.ifValue == null &&
            transfer.ifKeyState == PropertyKeyState.PRESENT) {
            boolean keyPresent = getDesiredConfigurationKeyPresence(cluster, transfer.ifType, transfer.ifKey);
            if (!keyPresent) {
              LOG.info("Skipping property delete for {}/{} as the key {} for {} is not present",
                definition.getConfigType(), transfer.deleteKey, transfer.ifKey, transfer.ifType);
              continue;
            }
          }

          if (ifKeyIsNotBlank && ifTypeIsNotBlank && transfer.ifValue != null) {

            String ifConfigType = transfer.ifType;
            String ifKey = transfer.ifKey;
            String ifValue = transfer.ifValue;

            String checkValue = getDesiredConfigurationValue(cluster, ifConfigType, ifKey);
            if (!ifValue.toLowerCase().equals(StringUtils.lowerCase(checkValue))) {
              // skip adding
              LOG.info("Skipping property delete for {}/{} as the value {} for {}/{} is not equal to {}",
                       definition.getConfigType(), transfer.deleteKey, checkValue, ifConfigType, ifKey, ifValue);
              continue;
            }
          }
        }
        allowedTransfers.add(transfer);
      }
      configParameters.put(ConfigureTask.PARAMETER_TRANSFERS, m_gson.toJson(allowedTransfers));
    }

    // replacements
    List<Replace> replacements = definition.getReplacements();
    if( null != replacements && !replacements.isEmpty() ){
      configParameters.put(ConfigureTask.PARAMETER_REPLACEMENTS, m_gson.toJson(replacements));
    }

    return configParameters;
  }

  /**
   * Gets the value of the specified cluster property.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @param configType
   *          the configuration type (ie hdfs-site) (not {@code null}).
   * @param propertyKey
   *          the key to retrieve (not {@code null}).
   * @return the value or {@code null} if it does not exist.
   */
  private String getDesiredConfigurationValue(Cluster cluster,
      String configType, String propertyKey) {

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    if (null == desiredConfig) {
      return null;
    }

    Config config = cluster.getConfig(configType, desiredConfig.getTag());
    if (null == config) {
      return null;
    }

    return config.getProperties().get(propertyKey);
  }

  /**
   * Gets the property presence state
   * @param cluster
   *          the cluster (not {@code null}).
   * @param configType
   *          the configuration type (ie hdfs-site) (not {@code null}).
   * @param propertyKey
   *          the key to retrieve (not {@code null}).
   * @return {@code true} if property key exists or {@code false} if not.
   */
  private boolean getDesiredConfigurationKeyPresence(Cluster cluster,
      String configType, String propertyKey) {

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    if (null == desiredConfig) {
      return false;
    }

    Config config = cluster.getConfig(configType, desiredConfig.getTag());
    if (null == config) {
      return false;
    }
    return config.getProperties().containsKey(propertyKey);
  }

}