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

package org.apache.hms.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hms.common.conf.CommonConfigurationKeys;

public class ZookeeperUtil {
  public static String COMMAND_STATUS = "/status";
  private static final Pattern BASENAME = Pattern.compile(".*?([^/]*)$");
  
  public static String getClusterPath(String clusterName) {
    StringBuilder clusterNode = new StringBuilder();
    clusterNode.append(CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT);
    clusterNode.append("/");
    clusterNode.append(clusterName);
    return clusterNode.toString();
  }
  
  public static String getCommandStatusPath(String cmdPath) {
    StringBuilder cmdStatusPath = new StringBuilder();
    cmdStatusPath.append(cmdPath);
    cmdStatusPath.append(COMMAND_STATUS);
    return cmdStatusPath.toString();
  }
  
  public static String getBaseURL(String url) {
      Matcher matcher = BASENAME.matcher(url);
      if (matcher.matches()) {
        return matcher.group(1);
      } else {
        throw new IllegalArgumentException("Can't parse " + url);
      }
  }
  
  public static String getNodesManifestPath(String id) {
    StringBuilder nodesPath = new StringBuilder();
    nodesPath.append(CommonConfigurationKeys.ZOOKEEPER_NODES_MANIFEST_PATH_DEFAULT);
    nodesPath.append("/");
    nodesPath.append(id);
    return nodesPath.toString();
  }

  public static String getSoftwareManifestPath(String id) {
    StringBuilder nodesPath = new StringBuilder();
    nodesPath.append(CommonConfigurationKeys.ZOOKEEPER_SOFTWARE_MANIFEST_PATH_DEFAULT);
    nodesPath.append("/");
    nodesPath.append(id);
    return nodesPath.toString();
  }
  
  public static String getConfigManifestPath(String id) {
    StringBuilder configPath = new StringBuilder();
    configPath.append(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT);
    configPath.append("/");
    configPath.append(id);
    return configPath.toString();
  }
}
