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

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class KerberosKeytabDAO {
    @Inject
    Provider<EntityManager> entityManagerProvider;

    @Transactional
    public void create(KerberosKeytabEntity kerberosKeytabEntity) {
        entityManagerProvider.get().persist(kerberosKeytabEntity);
    }

    public void create(String keytabPath) {
        create(new KerberosKeytabEntity(keytabPath));
    }

    @Transactional
    public KerberosKeytabEntity merge(KerberosKeytabEntity kerberosKeytabEntity) {
        return entityManagerProvider.get().merge(kerberosKeytabEntity);
    }

    @Transactional
    public void remove(KerberosKeytabEntity kerberosKeytabEntity) {
        entityManagerProvider.get().remove(merge(kerberosKeytabEntity));
    }

    public void remove(String keytabPath) {
        KerberosKeytabEntity kke = find(keytabPath);
        if (kke != null) {
            remove(kke);
        }
    }

    @Transactional
    public void refresh(KerberosKeytabEntity kerberosKeytabEntity) {
        entityManagerProvider.get().refresh(kerberosKeytabEntity);
    }


    @RequiresSession
    public KerberosKeytabEntity find(String keytabPath) {
        return entityManagerProvider.get().find(KerberosKeytabEntity.class, keytabPath);
    }

    @RequiresSession
    public List<KerberosKeytabEntity> findAll() {
        TypedQuery<KerberosKeytabEntity> query = entityManagerProvider.get().
                createNamedQuery("KerberosKeytabEntity.findAll", KerberosKeytabEntity.class);

        return query.getResultList();
    }

    @RequiresSession
    public boolean exists(String keytabPath) {
        return find(keytabPath) != null;
    }

    @RequiresSession
    public Collection<KerberosKeytabEntity> findByHost(Long hostId) {
        TypedQuery<KerberosKeytabEntity> query = entityManagerProvider.get().
            createNamedQuery("KerberosKeytabEntity.findByHost", KerberosKeytabEntity.class);
        query.setParameter("hostId", hostId);
        return query.getResultList();
    }

    public Collection<KerberosKeytabEntity> findByHost(HostEntity hostEntity) {
        return findByHost(hostEntity.getHostId());
    }

    public void remove(List<KerberosKeytabEntity> entities) {
        if (entities != null) {
            for (KerberosKeytabEntity entity : entities) {
                entityManagerProvider.get().remove(entity);
            }
        }
    }
}
