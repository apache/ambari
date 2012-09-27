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

package org.apache.ambari.server.orm;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.RoleDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

@Singleton
public class OrmTestHelper {

  @Inject
  public Provider<EntityManager> entityManagerProvider;
  @Inject
  public Injector injector;
  @Inject
  public UserDAO userDAO;
  @Inject
  public RoleDAO roleDAO;

  public EntityManager getEntityManager() {
    return entityManagerProvider.get();
  }

  /**
   * creates some test data
    */
  @Transactional
  public void createDefaultData() {

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");

    HostEntity host1 = new HostEntity();
    HostEntity host2 = new HostEntity();
    HostEntity host3 = new HostEntity();

    host1.setHostName("test_host1");
    host2.setHostName("test_host2");
    host3.setHostName("test_host3");
    host1.setIp("192.168.0.1");
    host2.setIp("192.168.0.2");
    host3.setIp("192.168.0.3");

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    hostEntities.add(host1);
    hostEntities.add(host2);

    clusterEntity.setHostEntities(hostEntities);

    //both sides of relation should be set when modifying in runtime
    host1.setClusterEntity(clusterEntity);
    host2.setClusterEntity(clusterEntity);

    HostStateEntity hostStateEntity1 = new HostStateEntity();
    hostStateEntity1.setCurrentState("TEST_STATE1");
    hostStateEntity1.setHostEntity(host1);
    HostStateEntity hostStateEntity2 = new HostStateEntity();
    hostStateEntity2.setCurrentState("TEST_STATE2");
    hostStateEntity2.setHostEntity(host2);
    host1.setHostStateEntity(hostStateEntity1);
    host2.setHostStateEntity(hostStateEntity2);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setServiceName("HDFS");
    clusterServiceEntity.setClusterEntity(clusterEntity);
    List<ClusterServiceEntity> clusterServiceEntities = new ArrayList<ClusterServiceEntity>();
    clusterServiceEntities.add(clusterServiceEntity);
    clusterEntity.setClusterServiceEntities(clusterServiceEntities);

    getEntityManager().persist(host1);
    getEntityManager().persist(host2);
    getEntityManager().persist(clusterEntity);
    getEntityManager().persist(hostStateEntity1);
    getEntityManager().persist(hostStateEntity2);
    getEntityManager().persist(clusterServiceEntity);

  }

  @Transactional
  public void createTestUsers() {
    PasswordEncoder encoder = injector.getInstance(PasswordEncoder.class);

    RoleEntity adminRole = new RoleEntity();
    adminRole.setRoleName("admin");

    UserEntity admin = new UserEntity();
    admin.setUserName("administrator");
    admin.setUserPassword(encoder.encode("admin"));

    Set<RoleEntity> roles = new HashSet<RoleEntity>();
    Set<UserEntity> users = new HashSet<UserEntity>();

    roles.add(adminRole);
    users.add(admin);

    admin.setRoleEntities(roles);
    adminRole.setUserEntities(users);

    userDAO.create(admin);
    roleDAO.create(adminRole);

    UserEntity userWithoutRoles = new UserEntity();
    userWithoutRoles.setUserName("userWithoutRoles");
    userWithoutRoles.setUserPassword(encoder.encode("test"));
    userDAO.create(userWithoutRoles);

  }

  @Transactional
  public void performTransactionMarkedForRollback() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterDAO.removeByName("test_cluster1");
    getEntityManager().getTransaction().setRollbackOnly();
  }

  public int getClusterSizeByHostName(String hostName) {

    Query query = getEntityManager().createQuery(
            "SELECT host2 from HostEntity host join host.clusterEntity cluster join cluster.hostEntities host2 where host.hostName=:hostName");
    query.setParameter("hostName", hostName);

    Collection hosts = query.getResultList();

    return hosts.size();
  }

}
