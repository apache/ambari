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
package org.apache.ambari.server.serveraction.upgrades;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ConfigMergeHelper;
import org.apache.ambari.server.state.ConfigMergeHelper.ThreeWayValue;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Masked;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Replace;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Transfer;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link ConfigureAction} is used to alter a configuration property during
 * an upgrade. It will only produce a new configuration if an actual change is
 * occuring. For some configure tasks, the value is already at the desired
 * property or the conditions of the task are not met. In these cases, a new
 * configuration will not be created. This task can perform any of the following
 * actions in a single declaration:
 * <ul>
 * <li>Copy a configuration to a new property key, optionally setting a default
 * if the original property did not exist</li>
 * <li>Copy a configuration to a new property key from one configuration type to
 * another, optionally setting a default if the original property did not exist</li>
 * <li>Rename a configuration, optionally setting a default if the original
 * property did not exist</li>
 * <li>Delete a configuration property</li>
 * <li>Set a configuration property</li>
 * <li>Conditionally set a configuration property based on another configuration
 * property value</li>
 * </ul>
 */
public class ConfigureAction extends AbstractServerAction {

  /**
   * Used to lookup the cluster.
   */
  @Inject
  private Clusters m_clusters;

  /**
   * Used to update the configuration properties.
   */
  @Inject
  private AmbariManagementController m_controller;

  /**
   * Used to assist in the creation of a {@link ConfigurationRequest} to update
   * configuration values.
   */
  @Inject
  private ConfigHelper m_configHelper;

  /**
   * The Ambari configuration.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Used to lookup stack properties which are the configuration properties that
   * are defined on the stack.
   */
  @Inject
  private Provider<AmbariMetaInfo> m_ambariMetaInfo;

  @Inject
  private ConfigMergeHelper m_mergeHelper;

  /**
   * Gson
   */
  @Inject
  private Gson m_gson;

  /**
   * Aside from the normal execution, this method performs the following logic, with
   * the stack values set in the table below:
   * <p>
   * <table>
   *  <tr>
   *    <th>Upgrade Path</th>
   *    <th>direction</th>
   *    <th>Stack Actual</th>
   *    <th>Stack Desired</th>
   *    <th>Config Stack</th>
   *    <th>Action</th>
   *  </tr>
   *  <tr>
   *    <td>2.2.x -> 2.2.y</td>
   *    <td>upgrade or downgrade</td>
   *    <td>2.2</td>
   *    <td>2.2</td>
   *    <td>2.2</td>
   *    <td>if value has changed, create a new config object with new value</td>
   *  </tr>
   *  <tr>
   *    <td>2.2 -> 2.3</td>
   *    <td>upgrade</td>
   *    <td>2.2</td>
   *    <td>2.3: set before action is executed</td>
   *    <td>2.3: set before action is executed</td>
   *    <td>new configs are already created; just update with new properties</td>
   *  </tr>
   *  <tr>
   *    <td>2.3 -> 2.2</td>
   *    <td>downgrade</td>
   *    <td>2.2</td>
   *    <td>2.2: set before action is executed</td>
   *    <td>2.2</td>
   *    <td>configs are already managed, results are the same as 2.2.x -> 2.2.y</td>
   *  </tr>
   * </table>
   * </p>
   *
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(
      ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Map<String,String> commandParameters = getCommandParameters();
    if( null == commandParameters || commandParameters.isEmpty() ){
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to change configuration values without command parameters");
    }

    String clusterName = commandParameters.get("clusterName");

    // such as hdfs-site or hbase-env
    String configType = commandParameters.get(ConfigureTask.PARAMETER_CONFIG_TYPE);

    // extract transfers
    List<ConfigurationKeyValue> keyValuePairs = Collections.emptyList();
    String keyValuePairJson = commandParameters.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    if (null != keyValuePairJson) {
      keyValuePairs = m_gson.fromJson(
          keyValuePairJson, new TypeToken<List<ConfigurationKeyValue>>(){}.getType());
    }

    // extract transfers
    List<Transfer> transfers = Collections.emptyList();
    String transferJson = commandParameters.get(ConfigureTask.PARAMETER_TRANSFERS);
    if (null != transferJson) {
      transfers = m_gson.fromJson(
        transferJson, new TypeToken<List<Transfer>>(){}.getType());
    }

    // extract replacements
    List<Replace> replacements = Collections.emptyList();
    String replaceJson = commandParameters.get(ConfigureTask.PARAMETER_REPLACEMENTS);
    if (null != replaceJson) {
      replacements = m_gson.fromJson(
          replaceJson, new TypeToken<List<Replace>>(){}.getType());
    }

    // if there is nothing to do, then skip the task
    if (keyValuePairs.isEmpty() && transfers.isEmpty() && replacements.isEmpty()) {
      String message = "cluster={0}, type={1}, transfers={2}, replacements={3}, configurations={4}";
      message = MessageFormat.format(message, clusterName, configType, transfers, replacements,
          keyValuePairs);

      StringBuilder buffer = new StringBuilder(
          "Skipping this configuration task since none of the conditions were met and there are no transfers or replacements").append("\n");

      buffer.append(message);

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", buffer.toString(), "");
    }

    // if only 1 of the required properties was null and no transfer properties,
    // then something went wrong
    if (null == clusterName || null == configType
        || (keyValuePairs.isEmpty() && transfers.isEmpty() && replacements.isEmpty())) {
      String message = "cluster={0}, type={1}, transfers={2}, replacements={3}, configurations={4}";
      message = MessageFormat.format(message, clusterName, configType, transfers, replacements, keyValuePairs);
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", message);
    }

    Cluster cluster = m_clusters.getCluster(clusterName);

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    Config config = cluster.getConfig(configType, desiredConfig.getTag());

    StackId currentStack = cluster.getCurrentStackVersion();
    StackId targetStack = cluster.getDesiredStackVersion();
    StackId configStack = config.getStackId();

    // !!! initial reference values
    Map<String, String> base = config.getProperties();
    Map<String, String> newValues = new HashMap<String, String>(base);

    boolean changedValues = false;

    // !!! do transfers first before setting defined values
    StringBuilder outputBuffer = new StringBuilder(250);
    for (Transfer transfer : transfers) {
      switch (transfer.operation) {
        case COPY:
          String valueToCopy = null;
          if( null == transfer.fromType ) {
            // copying from current configuration
            valueToCopy = base.get(transfer.fromKey);
          } else {
            // copying from another configuration
            Config other = cluster.getDesiredConfigByType(transfer.fromType);
            if (null != other){
              Map<String, String> otherValues = other.getProperties();
              if (otherValues.containsKey(transfer.fromKey)){
                valueToCopy = otherValues.get(transfer.fromKey);
              }
            }
          }

          // if the value is null use the default if it exists
          if (StringUtils.isBlank(valueToCopy) && !StringUtils.isBlank(transfer.defaultValue)) {
            valueToCopy = transfer.defaultValue;
          }

          if (StringUtils.isNotBlank(valueToCopy)) {
            // possibly coerce the value on copy
            if (transfer.coerceTo != null) {
              switch (transfer.coerceTo) {
                case YAML_ARRAY: {
                  // turn c6401,c6402 into ['c6401',c6402']
                  String[] splitValues = StringUtils.split(valueToCopy, ',');
                  List<String> quotedValues = new ArrayList<String>(splitValues.length);
                  for (String splitValue : splitValues) {
                    quotedValues.add("'" + StringUtils.trim(splitValue) + "'");
                  }

                  valueToCopy = "[" + StringUtils.join(quotedValues, ',') + "]";

                  break;
                }
                default:
                  break;
              }
            }

            // at this point we know that we have a changed value
            changedValues = true;
            newValues.put(transfer.toKey, valueToCopy);

            // append standard output
            outputBuffer.append(MessageFormat.format("Created {0}/{1} = \"{2}\"\n", configType,
                transfer.toKey, mask(transfer, valueToCopy)));
          }
          break;
        case MOVE:
          // if the value existed previously, then update the maps with the new
          // key; otherwise if there is a default value specified, set the new
          // key with the default
          if (newValues.containsKey(transfer.fromKey)) {
            newValues.put(transfer.toKey, newValues.remove(transfer.fromKey));
            changedValues = true;

            // append standard output
            outputBuffer.append(MessageFormat.format("Renamed {0}/{1} to {2}/{3}\n", configType,
                transfer.fromKey, configType, transfer.toKey));
          } else if (StringUtils.isNotBlank(transfer.defaultValue)) {
            newValues.put(transfer.toKey, transfer.defaultValue);
            changedValues = true;

            // append standard output
            outputBuffer.append(MessageFormat.format(
                "Created {0}/{1} with default value \"{2}\"\n",
                configType, transfer.toKey, mask(transfer, transfer.defaultValue)));
          }

          break;
        case DELETE:
          if ("*".equals(transfer.deleteKey)) {
            newValues.clear();

            // append standard output
            outputBuffer.append(MessageFormat.format("Deleted all keys from {0}\n", configType));

            for (String keeper : transfer.keepKeys) {
              if (base.containsKey(keeper) && base.get(keeper) != null) {
                newValues.put(keeper, base.get(keeper));

                // append standard output
                outputBuffer.append(MessageFormat.format("Preserved {0}/{1} after delete\n",
                  configType, keeper));
              }
            }

            // !!! with preserved edits, find the values that are different from
            // the stack-defined and keep them - also keep values that exist in
            // the config but not on the stack
            if (transfer.preserveEdits) {
              List<String> edited = findValuesToPreserve(clusterName, config);
              for (String changed : edited) {
                newValues.put(changed, base.get(changed));

                // append standard output
                outputBuffer.append(MessageFormat.format("Preserved {0}/{1} after delete\n",
                    configType, changed));
              }
            }

            changedValues = true;
          } else {
            newValues.remove(transfer.deleteKey);
            changedValues = true;

            // append standard output
            outputBuffer.append(MessageFormat.format("Deleted {0}/{1}\n", configType,
                transfer.deleteKey));
          }

          break;
      }
    }

    // set all key/value pairs
    if (null != keyValuePairs && !keyValuePairs.isEmpty()) {
      for (ConfigurationKeyValue keyValuePair : keyValuePairs) {
        String key = keyValuePair.key;
        String value = keyValuePair.value;

        if (null != key) {
          String oldValue = base.get(key);

          // !!! values are not changing, so make this a no-op
          if (null != oldValue && value.equals(oldValue)) {
            if (currentStack.equals(targetStack) && !changedValues) {
              outputBuffer.append(MessageFormat.format(
                  "{0}/{1} for cluster {2} would not change, skipping setting", configType, key,
                  clusterName));

              // continue because this property is not changing
              continue;
            }
          }

          // !!! only put a key/value into this map of new configurations if
          // there was a key, otherwise this will put something like null=null
          // into the configs which will cause NPEs after upgrade - this is a
          // byproduct of the configure being able to take a list of transfers
          // without a key/value to set
          newValues.put(key, value);

          final String message;
          if (StringUtils.isEmpty(value)) {
            message = MessageFormat.format("{0}/{1} changed to an empty value", configType, key);
          } else {
            message = MessageFormat.format("{0}/{1} changed to \"{2}\"\n", configType, key,
                mask(keyValuePair, value));
          }

          outputBuffer.append(message);
        }
      }
    }

    // !!! string replacements happen only on the new values.
    for (Replace replacement : replacements) {
      // the key might exist but might be null, so we need to check this
      // condition when replacing a part of the value
      String toReplace = newValues.get(replacement.key);
      if (StringUtils.isNotBlank(toReplace)) {
        if (!toReplace.contains(replacement.find)) {
          outputBuffer.append(MessageFormat.format("String \"{0}\" was not found in {1}/{2}\n",
              replacement.find, configType, replacement.key));
        } else {
          String replaced = StringUtils.replace(toReplace, replacement.find, replacement.replaceWith);

          newValues.put(replacement.key, replaced);

          outputBuffer.append(
              MessageFormat.format("Replaced {0}/{1} containing \"{2}\" with \"{3}\"", configType,
                  replacement.key, replacement.find, replacement.replaceWith));

          outputBuffer.append(System.lineSeparator());
        }
      } else {
        outputBuffer.append(MessageFormat.format(
            "Skipping replacement for {0}/{1} because it does not exist or is empty.",
            configType, replacement.key));
        outputBuffer.append(System.lineSeparator());
      }
    }

    // !!! check to see if we're going to a new stack and double check the
    // configs are for the target.  Then simply update the new properties instead
    // of creating a whole new history record since it was already done
    if (!targetStack.equals(currentStack) && targetStack.equals(configStack)) {
      config.setProperties(newValues);
      config.persist(false);

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outputBuffer.toString(), "");
    }

    // !!! values are different and within the same stack.  create a new
    // config and service config version
    String serviceVersionNote = "Stack Upgrade";

    String auditName = getExecutionCommand().getRoleParams().get(ServerAction.ACTION_USER_NAME);

    if (auditName == null) {
      auditName = m_configuration.getAnonymousAuditName();
    }

    m_configHelper.createConfigType(cluster, m_controller, configType,
        newValues, auditName, serviceVersionNote);

    String message = "Finished updating configuration ''{0}''";
    message = MessageFormat.format(message, configType);
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", message, "");
  }


  /**
   * Finds the values that should be preserved during a delete. This includes:
   * <ul>
   * <li>Properties that existed on the stack but were changed to a different
   * value</li>
   * <li>Properties that do not exist on the stack</li>
   * </ul>
   *
   * @param clusterName
   *          the cluster name
   * @param config
   *          the config with the tag to find conflicts
   * @return the list of changed property keys
   * @throws AmbariException
   */
  private List<String> findValuesToPreserve(String clusterName, Config config)
      throws AmbariException {
    List<String> result = new ArrayList<String>();

    Map<String, Map<String, ThreeWayValue>> conflicts =
        m_mergeHelper.getConflicts(clusterName, config.getStackId());

    Map<String, ThreeWayValue> conflictMap = conflicts.get(config.getType());

    // process the conflicts, if any, and add them to the list
    if (null != conflictMap && !conflictMap.isEmpty()) {
      for (Map.Entry<String, ThreeWayValue> entry : conflictMap.entrySet()) {
        ThreeWayValue twv = entry.getValue();
        if (null == twv.oldStackValue) {
          result.add(entry.getKey());
        } else if (null != twv.savedValue && !twv.oldStackValue.equals(twv.savedValue)) {
          result.add(entry.getKey());
        }
      }
    }


    String configType = config.getType();
    Cluster cluster = m_clusters.getCluster(clusterName);
    StackId oldStack = cluster.getCurrentStackVersion();

    // iterate over all properties for every cluster service; if the property
    // has the correct config type (ie oozie-site or hdfs-site) then add it to
    // the list of original stack propertiess
    Set<String> stackPropertiesForType = new HashSet<String>(50);
    for (String serviceName : cluster.getServices().keySet()) {
      Set<PropertyInfo> serviceProperties = m_ambariMetaInfo.get().getServiceProperties(
          oldStack.getStackName(), oldStack.getStackVersion(), serviceName);

      for (PropertyInfo property : serviceProperties) {
        String type = ConfigHelper.fileNameToConfigType(property.getFilename());
        if (type.equals(configType)) {
          stackPropertiesForType.add(property.getName());
        }
      }
    }

    // now iterate over all stack properties, adding them to the list if they
    // match
    Set<PropertyInfo> stackProperties = m_ambariMetaInfo.get().getStackProperties(
        oldStack.getStackName(),
        oldStack.getStackVersion());

    for (PropertyInfo property : stackProperties) {
      String type = ConfigHelper.fileNameToConfigType(property.getFilename());
      if (type.equals(configType)) {
        stackPropertiesForType.add(property.getName());
      }
    }

    // see if any keys exist in the old config but not the the original stack
    // for this config type; that means they were added and should be preserved
    Map<String, String> base = config.getProperties();
    Set<String> baseKeys = base.keySet();
    for( String baseKey : baseKeys ){
      if (!stackPropertiesForType.contains(baseKey)) {
        result.add(baseKey);
      }
    }

    return result;
  }

  private static String mask(Masked mask, String value) {
    if (mask.mask) {
      return StringUtils.repeat("*", value.length());
    }
    return value;
  }

}
