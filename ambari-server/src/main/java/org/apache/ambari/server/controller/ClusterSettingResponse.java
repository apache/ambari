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

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

public class ClusterSettingResponse {

    private Long clusterId;
    private String clusterName;
    private Long clusterSettingId;
    private String clusterSettingName;
    private String clusterSettingValue;

    public ClusterSettingResponse(Long clusterId, String clusterName, Long clusterSettingId,
                                  String clusterSettingName, String clusterSettingValue) {
        this.clusterId = clusterId;
        this.clusterSettingId = clusterSettingId;
        this.clusterName = clusterName;
        this.clusterSettingName = clusterSettingName;
        this.clusterSettingValue = clusterSettingValue;
    }

    /**
     * @return the clusterId
     */
    public Long getClusterId() {
        return clusterId;
    }

    /**
     * @param clusterId the clusterId to set
     */
    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
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
     * @return the cluster Setting Id
     */
    public Long getClusterSettingId() {
        return clusterSettingId;
    }

    /**
     * @param  clusterSettingId the cluster Setting Id
     */
    public void setClusterSettingId(Long clusterSettingId) {
        this.clusterSettingId = clusterSettingId;
    }



    /**
     * @return the cluster setting name
     */
    public String getClusterSettingName() {
        return clusterSettingName;
    }

    /**
     * @param  clusterSettingName the cluster setting name
     */
    public void setClusterSettingName(String clusterSettingName) {
        this.clusterSettingName = clusterSettingName;
    }



    /**
     * @return the cluster setting name
     */
    public String getClusterSettingValue() {
        return clusterSettingValue;
    }

    /**
     * @param  clusterSettingValue the cluster setting value
     */
    public void setClusterSettingValue(String clusterSettingValue) {
        this.clusterSettingValue = clusterSettingValue;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterSettingResponse that = (ClusterSettingResponse) o;

        return Objects.equals(clusterId, that.clusterId) &&
               Objects.equals(clusterSettingId, that.clusterSettingId) &&
               Objects.equals(clusterName, that.clusterName) &&
               Objects.equals(clusterSettingName, that.clusterSettingName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, clusterSettingId, clusterName, clusterSettingName);
    }

    /**
     * Interface to help correct Swagger documentation generation
     */
    public interface ClusterSettingResponseSwagger extends ApiModel {
        @ApiModelProperty(name = "ClusterSettingInfo")
        ClusterSettingResponse getClusterSettingResponse();
    }

}
