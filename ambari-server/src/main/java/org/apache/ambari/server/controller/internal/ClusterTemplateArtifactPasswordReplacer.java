/*
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

package org.apache.ambari.server.controller.internal;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.Configurable;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.utils.SecretReference;

import com.google.common.collect.Multimap;

/**
 * Helper class for replacing password properties in cluster template artifacts. {@see #replacePasswords}
 */
public class ClusterTemplateArtifactPasswordReplacer {

  /**
   * Replaces all passwords (service config passwords and default password) in a received cluster creation template
   * artifact. Mpack (stack) information is used to identify password type properties. If the cluster template does not
   * specify mpacks, all installed stacks are queried for password type properties.
   *
   * @param artifactData the raw cluster template artifact as parsed json
   * @return the cluster template artifact with passwords replaced
   */
  public Map<String, Object> replacePasswords(Map<String, Object> artifactData) {
    Collection<StackId> stackIds = extractStackIdsFromClusterRequest(artifactData);
    // get all password properties from the specified stacks or
    // all stacks if the cluster template doesn't specify mpacks
    Multimap<String, String> passwordProperties = stackIds.isEmpty() ?
      SecretReference.getAllPasswordProperties() :
      SecretReference.getAllPasswordProperties(stackIds);
    Map<String, Object> passwordsReplaced = replacePasswordsInConfigurations(artifactData, passwordProperties);
    passwordsReplaced.replace("default_password", SecretReference.SECRET_PREFIX + ":default_password");
    return passwordsReplaced;
  }

  /**
   * Replaces passwords in the received cluster template artifact based on the received password information extracted
   * from stacks.
   * @param artifactData the cluster template artifact
   * @param passwordProperties a multimap containing password type properties. The map has a structure of
   *                           config type -> password properties.
   * @return the cluster template artifact with passwords replaced
   */
  protected Map<String, Object> replacePasswordsInConfigurations(Map<String, Object> artifactData,
                                                                 Multimap<String, String> passwordProperties) {
    return (Map<String, Object>)
      applyToAllConfigurations(artifactData,
        config -> {
          Configuration configuration = Configurable.parseConfigs(config);
          Configuration replacedConfiguration =
            SecretReference.replacePasswordsInConfigurations(configuration, passwordProperties);
          return Configurable.convertConfigToMap(replacedConfiguration);
        }
      );
  }

  /**
   * <p> Recursively scans the received data structure consisting of maps, lists ans simple values (a parsed Json) and
   * applies the @{code transform} function to each configurations found. </p>
   * <p> A value counts as configuration if it has a type of @{link java.util.List} and is a value in a map with
   * {@code "configurations"} key.
   *
   * </p>
   * @param data the data to process recursively (is a structure of maps, lists and simple values)
   * @param transform the transformation to apply to configuration values
   * @return a replication of the input data with the transformation applied to all configuration values.
   */
  protected Object applyToAllConfigurations(Object data,
                                            Function<List<Map<String, Object>>, Object> transform) {
    if (data instanceof List<?>) {
      return processList((List<Object>)data, transform);
    }
    else if (data instanceof Map<?, ?>) {
      return processMap((Map<String, Object>)data, transform);
    }
    else {
      return data;
    }
  }

  /**
   * Recursively call {@link #applyToAllConfigurations(Object, Function)} on all items in the list
   * @param listItem the list to process
   * @param transform the transformation to be passed to {@code applyToAllConfigurations}
   * @return the transformed list
   */
  protected List<Object> processList(List<Object> listItem, Function<List<Map<String, Object>>, Object> transform) {
    return listItem.stream().
      map(item -> applyToAllConfigurations(item, transform)).
      collect(toList());
  }

  /**
   * Process map items in a cluster template artifact structure. For all configuration type map entries,
   * {@code transform} will be called on the value. For other entries {@link #applyToAllConfigurations(Object, Function)}
   * will be called recursively
   * @param mapItem the map to process
   * @param transform the transformation to apply on configuration type entries
   * @return the transformed map
   */
  protected Map<String, Object> processMap(Map<String, Object> mapItem, Function<List<Map<String, Object>>, Object> transform) {
    return mapItem.entrySet().stream().map(
      entry -> {
        // apply transformation for configuration entries
        if ("configurations".equals(entry.getKey()) && entry.getValue() instanceof List<?>) {
          return new AbstractMap.SimpleEntry<>(
            entry.getKey(),
            transform.apply((List<Map<String, Object>>)entry.getValue()));
        }
        // recursively call applyToAllConfigurations() for non-configuration entries
        else {
          return new AbstractMap.SimpleEntry<>(
            entry.getKey(),
            applyToAllConfigurations(entry.getValue(), transform));
        }
      }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Extracts mpack / stack information from the cluster template artifacts
   * @param artifactData the cluster template artifact
   * @return a collection of stack id's (an empty collection if the artifact does not contain mpack references)
   */
  private Collection<StackId> extractStackIdsFromClusterRequest(Map<String, Object> artifactData) {
    List<Map<String, Object>> mpackInstances =
      (List<Map<String, Object>>)artifactData.getOrDefault("mpack_instances", emptyList());
    return mpackInstances.stream().
      map(mpackMap -> new StackId((String)mpackMap.get("type"), (String)mpackMap.get("version"))).
      collect(toList());
  }


}
