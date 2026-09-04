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

import java.util.List;

import jakarta.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.BoardEntity;

import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class BoardDAO extends CrudDAO<BoardEntity, Long> {
  public BoardDAO() {
    super(BoardEntity.class);
  }

  @RequiresSession
  public List<BoardEntity> findVisibleByCluster(String clusterName) {
    TypedQuery<BoardEntity> query = entityManagerProvider.get()
        .createNamedQuery("BoardEntity.findVisibleByCluster", BoardEntity.class);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<BoardEntity> findPublicVisibleByCluster(String clusterName) {
    TypedQuery<BoardEntity> query = entityManagerProvider.get()
        .createNamedQuery("BoardEntity.findPublicVisibleByCluster", BoardEntity.class);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public BoardEntity findByIdAndCluster(long id, String clusterName) {
    TypedQuery<BoardEntity> query = entityManagerProvider.get()
        .createNamedQuery("BoardEntity.findByIdAndCluster", BoardEntity.class);
    query.setParameter("id", id);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public BoardEntity findByIdentAndCluster(String ident, String clusterName) {
    TypedQuery<BoardEntity> query = entityManagerProvider.get()
        .createNamedQuery("BoardEntity.findByIdentAndCluster", BoardEntity.class);
    query.setParameter("ident", ident);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public BoardEntity findByClusterGroupAndName(String clusterName, long groupId, String name) {
    TypedQuery<BoardEntity> query = entityManagerProvider.get()
        .createNamedQuery("BoardEntity.findByClusterGroupAndName", BoardEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("groupId", groupId);
    query.setParameter("name", name);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public BoardEntity findBuiltinByIdent(String ident) {
    TypedQuery<BoardEntity> query = entityManagerProvider.get()
        .createNamedQuery("BoardEntity.findBuiltinByIdent", BoardEntity.class);
    query.setParameter("ident", ident);
    return daoUtils.selectOne(query);
  }

  @Transactional
  public void create(BoardEntity entity) {
    super.create(entity);
  }
}
