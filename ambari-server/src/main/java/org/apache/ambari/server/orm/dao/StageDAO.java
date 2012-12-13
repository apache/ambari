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

package org.apache.ambari.server.orm.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.utils.StageUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;

public class StageDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Transactional
  public StageEntity findByPK(StageEntityPK stageEntityPK) {
    return entityManagerProvider.get().find(StageEntity.class, stageEntityPK);
  }

  public long getLastRequestId() {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT max(stage.requestId) FROM StageEntity stage", Long.class);
    Long result = daoUtils.selectSingle(query);
    if (result != null) {
      return result;
    } else {
      return 0;
    }
  }

  @Transactional
  public StageEntity findByActionId(String actionId) {
    long[] ids = StageUtils.getRequestStage(actionId);
    StageEntityPK pk = new StageEntityPK();
    pk.setRequestId(ids[0]);
    pk.setStageId(ids[1]);
    return findByPK(pk);
  }

  @Transactional
  public List<StageEntity> findByRequestId(long requestId) {
    TypedQuery<StageEntity> query = entityManagerProvider.get().createQuery("SELECT stage " +
        "FROM StageEntity stage " +
        "WHERE stage.requestId=?1 " +
        "ORDER BY stage.stageId", StageEntity.class);
    return daoUtils.selectList(query, requestId);
  }

  @Transactional
  public List<StageEntity> findByCommandStatuses(Collection<HostRoleStatus> statuses) {
//    TypedQuery<StageEntity> query = entityManagerProvider.get().createQuery("SELECT stage " +
//        "FROM StageEntity stage JOIN stage.hostRoleCommands command " +
//        "WHERE command.status IN ?1 " +
//        "ORDER BY stage.requestId, stage.stageId", StageEntity.class);
    TypedQuery<StageEntity> query = entityManagerProvider.get().createQuery("SELECT stage " +
          "FROM StageEntity stage WHERE stage.stageId IN (SELECT hrce.stageId FROM " +
          "HostRoleCommandEntity hrce WHERE stage.requestId = hrce.requestId and hrce.status IN ?1 ) " +
          "ORDER BY stage.requestId, stage.stageId", StageEntity.class);
    return daoUtils.selectList(query, statuses);
  }

  @Transactional
  public void create(StageEntity stageEntity) {
    entityManagerProvider.get().persist(stageEntity);
  }

  @Transactional
  public StageEntity merge(StageEntity stageEntity) {
    return entityManagerProvider.get().merge(stageEntity);
  }

  @Transactional
  public void remove(StageEntity stageEntity) {
    entityManagerProvider.get().remove(merge(stageEntity));
  }

  @Transactional
  public void removeByPK(StageEntityPK stageEntityPK) {
    remove(findByPK(stageEntityPK));
  }

}
