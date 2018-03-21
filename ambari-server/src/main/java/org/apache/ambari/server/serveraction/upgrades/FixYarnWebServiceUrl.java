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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

/**
 * yarn.log.server.web-service.url is added in HDP 2.6
 * It takes value from yarn.timeline-service.webapp.address if the yarn.http.policy is HTTP_ONLY
 * and takes value from yarn.timeline-service.webapp.https.address if the yarn.http.policy is HTTPS_ONLY.
 * This class is used when moving from HDP-2.3/HDP-2.4/HDP-2.5 to HDP2.6
 */
public class FixYarnWebServiceUrl extends AbstractUpgradeServerAction {
    private static final String SOURCE_CONFIG_TYPE = "yarn-site";
    private static final String YARN_TIMELINE_WEBAPP_HTTPADDRESS = "yarn.timeline-service.webapp.address";
    private static final String YARN_TIMELINE_WEBAPP_HTTPSADDRESS = "yarn.timeline-service.webapp.https.address";
    private static final String YARN_LOGSERVER_WEBSERVICE_URL = "yarn.log.server.web-service.url";
    private static final String YARN_HTTP_POLICY = "yarn.http.policy";
    private static final String HTTP = "HTTP_ONLY";
    private static final String HTTPS = "HTTPS_ONLY";

    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
            throws AmbariException, InterruptedException{

        String clusterName = getExecutionCommand().getClusterName();
        Cluster cluster = getClusters().getCluster(clusterName);
        Config config = cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

        if (config == null) {
            return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
                    String.format("Source type %s not found", SOURCE_CONFIG_TYPE), "");
        }

        Map<String, String> properties = config.getProperties();
        String policy = properties.get(YARN_HTTP_POLICY);

        if (policy == null) {
            return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                    String.format("%s/%s property is null", SOURCE_CONFIG_TYPE,YARN_HTTP_POLICY), "");
        }

        if (policy.equalsIgnoreCase(HTTP)) {
           String webappHttpAddress = properties.get(YARN_TIMELINE_WEBAPP_HTTPADDRESS);

           if (webappHttpAddress == null) {
               return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                       String.format("%s/%s property is null", SOURCE_CONFIG_TYPE, YARN_TIMELINE_WEBAPP_HTTPADDRESS), "");
           }

           properties.put(YARN_LOGSERVER_WEBSERVICE_URL, "http://" + webappHttpAddress + "/ws/v1/applicationhistory");

        } else if (policy.equalsIgnoreCase(HTTPS)) {
            String webappHttpsAddress = properties.get(YARN_TIMELINE_WEBAPP_HTTPSADDRESS);

            if (webappHttpsAddress == null) {
                return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                        String.format("%s/%s property is null", SOURCE_CONFIG_TYPE, YARN_TIMELINE_WEBAPP_HTTPSADDRESS), "");
            }

            properties.put(YARN_LOGSERVER_WEBSERVICE_URL, "https://"+webappHttpsAddress+"/ws/v1/applicationhistory");

        } else {
          return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                    String.format("%s/%s property contains an invalid value. It should be from [%s,%s]", SOURCE_CONFIG_TYPE, YARN_HTTP_POLICY,HTTP,HTTPS), "");
        }

        config.setProperties(properties);
        config.save();
        agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

        return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                String.format("The %s/%s property was changed based on %s. ", SOURCE_CONFIG_TYPE, YARN_LOGSERVER_WEBSERVICE_URL, YARN_HTTP_POLICY),"");
    }
}
