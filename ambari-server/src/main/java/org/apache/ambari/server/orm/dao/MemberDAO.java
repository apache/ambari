/**
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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.MemberEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.UserEntity;

@Singleton
public class MemberDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public MemberEntity findByPK(Integer memberPK) {
    return entityManagerProvider.get().find(MemberEntity.class, memberPK);
  }

  @RequiresSession
  public List<MemberEntity> findAll() {
    final TypedQuery<MemberEntity> query = entityManagerProvider.get().createQuery("SELECT m FROM MemberEntity m", MemberEntity.class);
    return daoUtils.selectList(query);
  }

  public List<MemberEntity> findAllMembersByUser(UserEntity userEntity) {
    TypedQuery<MemberEntity> query = entityManagerProvider.get().createQuery("SELECT m FROM MemberEntity m WHERE m.user = :userEntity", MemberEntity.class);
    query.setParameter("userEntity", userEntity);
    return daoUtils.selectList(query);
  }

  @Transactional
  public void create(MemberEntity member) {
    entityManagerProvider.get().persist(member);
  }

  @Transactional
  public MemberEntity merge(MemberEntity member) {
    return entityManagerProvider.get().merge(member);
  }

  @Transactional
  public void remove(MemberEntity member) {
    entityManagerProvider.get().remove(merge(member));
  }

  @Transactional
  public void removeByPK(Integer memberPK) {
    remove(findByPK(memberPK));
  }
}
