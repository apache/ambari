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

package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;



@Table(name = "clustersettings")
@NamedQueries({
        @NamedQuery(name = "clusterSettingByClusterIdAndSettingName", query =
                "SELECT clusterSetting " +
                        "FROM ClusterSettingEntity clusterSetting " +
                        "JOIN clusterSetting.clusterEntity cluster " +
                        "WHERE clusterSetting.clusterSettingName=:clusterSettingName AND cluster.clusterId=:clusterId"),
        @NamedQuery(name = "clusterSettingById", query =
                "SELECT clusterSetting " +
                        "FROM ClusterSettingEntity clusterSetting " +
                        "WHERE clusterSetting.clusterSettingId=:clusterSettingId"),
})
@Entity
@TableGenerator(name = "cluster_setting_id_generator",
        table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
        , pkColumnValue = "cluster_setting_id_seq"
        , initialValue = 1
)
public class ClusterSettingEntity {

    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "cluster_setting_id_generator")
    private Long clusterSettingId;

    @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
    private Long clusterId;

    @Column(name = "setting_name", nullable = false, insertable = true, updatable = false)
    private String clusterSettingName;

    @Column(name = "setting_value", nullable = false, insertable = true, updatable = true)
    private String clusterSettingValue;

    @ManyToOne
    @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
    private ClusterEntity clusterEntity;

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getClusterSettingId() {
        return clusterSettingId;
    }

    public void setClusterSettingId(Long clusterSettingId) {
        this.clusterSettingId = clusterSettingId;
    }


    public String getClusterSettingName() {
        return clusterSettingName;
    }

    public void setClusterSettingName(String clusterSettingName) {
        this.clusterSettingName = clusterSettingName;
    }

    public String getClusterSettingValue() {
        return clusterSettingValue;
    }

    public void setClusterSettingValue(String clusterSettingValue) {
        this.clusterSettingValue = clusterSettingValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterSettingEntity that = (ClusterSettingEntity) o;
        return Objects.equals(clusterId, that.clusterId) &&
               Objects.equals(clusterSettingName, that.clusterSettingName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, clusterSettingName);
    }

    public ClusterEntity getClusterEntity() {
        return clusterEntity;
    }

    public void setClusterEntity(ClusterEntity clusterEntity) {
        this.clusterEntity = clusterEntity;
    }

}
