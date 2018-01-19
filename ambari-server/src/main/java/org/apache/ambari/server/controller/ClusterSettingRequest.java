/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;


public class ClusterSettingRequest {

    private String clusterName; // REF
    private String clusterSettingName; // GET/CREATE/DELETE
    private String clusterSettingValue;

    public ClusterSettingRequest(String clusterName, String clusterSettingName, String clusterSettingValue) {
        this.clusterName = clusterName;
        this.clusterSettingName = clusterSettingName;
        this.clusterSettingValue = clusterSettingValue;
    }

    /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName the clusterName to set
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * @return the clustesettingName
     */
    public String getClusterSettingName() {
        return clusterSettingName;
    }

    /**
     * @param clusterSettingName the cluster setting name to set
     */
    public void setClusterSettingName(String clusterSettingName) {
        this.clusterSettingName = clusterSettingName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("clusterName=" + clusterName
                + ", clusterSettingName=" + clusterSettingName
                + ", clusterSettingValue=" + clusterSettingValue);
        return sb.toString();
    }

    /**
     * @return the clustesettingName
     */
    public String getClusterSettingValue() {
        return clusterSettingValue;
    }

    /**
     * @param clusterSettingValue the cluster setting value to set
     */
    public void setClusterSettingValue(String clusterSettingValue) {
        this.clusterSettingValue = clusterSettingValue;
    }

}