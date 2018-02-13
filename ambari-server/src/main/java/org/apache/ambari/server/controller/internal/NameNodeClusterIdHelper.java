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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.state.HostConfig;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A helper class to find the clusterId of a Name Node.
 */
public class NameNodeClusterIdHelper {

    private String getClusterIdFromJmx(String nameNodeUrl) throws IOException{
        String value;
        URL url = new URL(nameNodeUrl + "/jmx?get=Hadoop:service=NameNode,name=NameNodeInfo::ClusterId");
        try (InputStreamReader isr = new InputStreamReader(url.openStream())) {
            JsonObject obj = new JsonParser().parse(isr).getAsJsonObject();
            value = obj.get("beans").getAsJsonArray().get(0).getAsJsonObject().get("ClusterId").getAsString();
        }
        return value;
    }

    /**
     * Finds the clusterId of the Name Node using the http jmx interface.
     * The necessary hostname and port is read from the AmbariManagementController.
     * @param managementController
     * @param clusterName
     * @param hostConfigMap
     * @return
     * @throws SystemException
     */
    String getNameNodeClusterId(AmbariManagementController managementController, String clusterName,
                                Map<String, HostConfig> hostConfigMap) throws SystemException{
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.setClusterName(clusterName);
        String type = "hdfs-site";
        configurationRequest.setType(type);
        HostConfig hostConfig = hostConfigMap.get(type);
        String versionTag = hostConfig.getDefaultVersionTag();
        configurationRequest.setVersionTag(versionTag);
        Set<ConfigurationRequest> configurationRequestSet = new HashSet<>();
        configurationRequestSet.add(configurationRequest);

        try {
            Set<ConfigurationResponse> confResps = managementController.getConfigurations(configurationRequestSet);
            ConfigurationResponse confResp = confResps.iterator().next();
            String baseUrl = "http://" + confResp.getConfigs().get("dfs.namenode.http-address");
            return getClusterIdFromJmx(baseUrl);
        } catch (Exception e) {
            throw new SystemException("Error getting NAMENODE clusterId", e);
        }
    }
}

