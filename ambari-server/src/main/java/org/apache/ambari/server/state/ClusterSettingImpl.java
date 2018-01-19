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

package org.apache.ambari.server.state;


import org.apache.ambari.server.AmbariException;

import org.apache.ambari.server.controller.ClusterSettingResponse;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterSettingDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterSettingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ClusterSettingImpl implements ClusterSetting {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterSettingImpl.class);

    private final Cluster cluster;

    private final ClusterDAO clusterDAO;
    private final ClusterSettingDAO clusterSettingDAO;
    private final AmbariEventPublisher eventPublisher;

    private Long clusterSettingId;
    private String clusterSettingName;
    private String clusterSettingValue;

    @AssistedInject
    public ClusterSettingImpl(@Assisted Cluster cluster,
                            @Assisted("clusterSettingName") String clusterSettingName,
                            @Assisted("clusterSettingValue") String clusterSettingValue,
                            ClusterDAO clusterDAO,
                            ClusterSettingDAO clusterSettingDAO,
                            AmbariEventPublisher eventPublisher) throws AmbariException {

        this.cluster = cluster;
        this.clusterDAO = clusterDAO;
        this.clusterSettingDAO = clusterSettingDAO;
        this.eventPublisher = eventPublisher;

        this.clusterSettingName = clusterSettingName;
        this.clusterSettingValue = clusterSettingValue;

        ClusterSettingEntity clusterSettingEntity = new ClusterSettingEntity();
        clusterSettingEntity.setClusterId(cluster.getClusterId());
        clusterSettingEntity.setClusterSettingId(clusterSettingId);
        clusterSettingEntity.setClusterSettingName(clusterSettingName);
        clusterSettingEntity.setClusterSettingValue(clusterSettingValue);

        persist(clusterSettingEntity);
    }

    @AssistedInject
    public ClusterSettingImpl(@Assisted Cluster cluster,
                            @Assisted ClusterSettingEntity clusterSettingEntity,
                            ClusterDAO clusterDAO,
                            ClusterSettingDAO clusterSettingDAO,
                            AmbariEventPublisher eventPublisher) throws AmbariException {
        this.cluster = cluster;
        this.clusterDAO = clusterDAO;
        this.clusterSettingDAO = clusterSettingDAO;
        this.eventPublisher = eventPublisher;

        this.clusterSettingId = clusterSettingEntity.getClusterSettingId();
        this.clusterSettingName = clusterSettingEntity.getClusterSettingName();
        this.clusterSettingValue = clusterSettingEntity.getClusterSettingValue();

        clusterSettingDAO.merge(clusterSettingEntity);
    }

    @Override
    public Long getClusterSettingId() {
        return clusterSettingId;
    }

    @Override
    public String getClusterSettingName() {
        return clusterSettingName;
    }

    @Override
    public void setClusterSettingName(String clusterSettingName) {
        ClusterSettingEntity entity = getClusterSettingEntity();
        entity.setClusterSettingName(clusterSettingName);
        clusterSettingDAO.merge(entity);
        this.clusterSettingName = clusterSettingName;
    }

    @Override
    public String getClusterSettingValue() {
        return clusterSettingValue;
    }

    @Override
    public void setClusterSettingValue(String clusterSettingValue) {
        ClusterSettingEntity entity = getClusterSettingEntity();
        entity.setClusterSettingValue(clusterSettingValue);
        clusterSettingDAO.merge(entity);
        this.clusterSettingValue = clusterSettingValue;
    }

    @Override
    public long getClusterId() {
        return cluster.getClusterId();
    }

    @Override
    public ClusterSettingResponse convertToResponse() {
        ClusterSettingResponse r = new ClusterSettingResponse(cluster.getClusterId(),
                cluster.getClusterName(), getClusterSettingId(), getClusterSettingName(), getClusterSettingValue());
        return r;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public void debugDump(StringBuilder sb) {
        sb.append("ClusterSetting={ clusterSettingName=").append(getClusterSettingName())
                .append(", clusterSettingValue=").append(getClusterSettingValue())
                .append(", clusterName=").append(cluster.getClusterName())
                .append(", clusterId=").append(cluster.getClusterId())
                .append(" }");
    }

    private void persist(ClusterSettingEntity clusterSettingEntity) {
        persistEntities(clusterSettingEntity);
        refresh();

        cluster.addClusterSetting(this);
    }

    @Transactional
    protected void persistEntities(ClusterSettingEntity clusterSettingEntity) {
        long clusterId = cluster.getClusterId();

        ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
        clusterSettingEntity.setClusterEntity(clusterEntity);
        clusterSettingDAO.create(clusterSettingEntity);
        clusterSettingId = clusterSettingEntity.getClusterSettingId();
        clusterEntity.getClusterSettingEntities().add(clusterSettingEntity);
        clusterDAO.merge(clusterEntity);
        clusterSettingDAO.merge(clusterSettingEntity);
    }

    @Override
    @Transactional
    public void refresh() {
        ClusterSettingEntity clusterSettingEntity = clusterSettingDAO.findByPK(clusterSettingId);
        clusterSettingDAO.refresh(clusterSettingEntity);
    }

    @Override
    public boolean canBeRemoved() {
        // TODO: Add check logic
        return true;
    }


    @Override
    @Transactional
    public void delete() throws AmbariException {
        removeEntities();
    }

    @Transactional
    protected void removeEntities() throws AmbariException {
        clusterSettingDAO.removeByPK(clusterSettingId);
    }

    // Refresh the cached reference on setters
    private ClusterSettingEntity getClusterSettingEntity() {
        return clusterSettingDAO.findByPK(clusterSettingId);
    }
}
