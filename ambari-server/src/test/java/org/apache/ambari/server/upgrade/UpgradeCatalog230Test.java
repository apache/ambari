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
package org.apache.ambari.server.upgrade;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * UpgradeCatalog230 tests.
 */
public class UpgradeCatalog230Test extends EasyMockSupport {

  private Injector injector;

  @Before
  public void setup() {
    resetAll();

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        binder.bind(DaoUtils.class).toInstance(createNiceMock(DaoUtils.class));
        binder.bind(PermissionDAO.class).toInstance(createMock(PermissionDAO.class));
        binder.bind(ResourceTypeDAO.class).toInstance(createMock(ResourceTypeDAO.class));
        binder.bind(RoleAuthorizationDAO.class).toInstance(createMock(RoleAuthorizationDAO.class));
      }
    };

    injector = Guice.createInjector(module);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = injector.getInstance(DBAccessor.class);

    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    Capture<DBAccessor.DBColumnInfo> columnCapture = EasyMock.newCapture();
    Capture<DBAccessor.DBColumnInfo> columnCaptureUserType = EasyMock.newCapture();
    Capture<DBAccessor.DBColumnInfo> columnCapturePermissionLabel = EasyMock.newCapture();
    Capture<List<DBAccessor.DBColumnInfo>> columnsCaptureRoleAuthorization = EasyMock.newCapture();
    Capture<List<DBAccessor.DBColumnInfo>> columnsCapturePermissionRoleAuthorization = EasyMock.newCapture();

    dbAccessor.alterColumn(eq("host_role_command"), capture(columnCapture));
    expectLastCall();

    dbAccessor.executeQuery("UPDATE users SET user_type='LDAP' WHERE ldap_user=1");
    expectLastCall();

    dbAccessor.addUniqueConstraint("users", "UNQ_users_0", "user_name", "user_type");
    expectLastCall();

    dbAccessor.addColumn(eq("users"), capture(columnCaptureUserType));
    expectLastCall();

    dbAccessor.addColumn(eq("adminpermission"), capture(columnCapturePermissionLabel));
    expectLastCall();

    dbAccessor.createTable(eq("roleauthorization"), capture(columnsCaptureRoleAuthorization), eq("authorization_id"));
    expectLastCall();

    dbAccessor.createTable(eq("permission_roleauthorization"), capture(columnsCapturePermissionRoleAuthorization), eq("permission_id"), eq("authorization_id"));
    expectLastCall();

    dbAccessor.addFKConstraint("permission_roleauthorization", "FK_permission_roleauth_pid",
        "permission_id", "adminpermission", "permission_id", false);
    expectLastCall();

    dbAccessor.addFKConstraint("permission_roleauthorization", "FK_permission_roleauth_aid",
        "authorization_id", "roleauthorization", "authorization_id", false);
    expectLastCall();

    replayAll();
    AbstractUpgradeCatalog upgradeCatalog = injector.getInstance(UpgradeCatalog230.class);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verifyAll();

    assertTrue(columnCapture.getValue().isNullable());

    assertEquals(columnCaptureUserType.getValue().getName(), "user_type");
    assertEquals(columnCaptureUserType.getValue().getType(), String.class);
    assertEquals(columnCaptureUserType.getValue().getLength(), null);
    assertEquals(columnCaptureUserType.getValue().getDefaultValue(), "LOCAL");
    assertEquals(columnCaptureUserType.getValue().isNullable(), true);

    assertEquals(columnCapturePermissionLabel.getValue().getName(), "permission_label");
    assertEquals(columnCapturePermissionLabel.getValue().getType(), String.class);
    assertEquals(columnCapturePermissionLabel.getValue().getLength(), Integer.valueOf(255));
    assertEquals(columnCapturePermissionLabel.getValue().isNullable(), true);

    List<DBAccessor.DBColumnInfo> columnInfos;
    DBAccessor.DBColumnInfo columnInfo;

    // Verify roleauthorization table
    columnInfos = columnsCaptureRoleAuthorization.getValue();
    assertEquals(2, columnInfos.size());

    columnInfo = columnInfos.get(0);
    assertEquals("authorization_id", columnInfo.getName());
    assertEquals(String.class, columnInfo.getType());
    assertEquals(Integer.valueOf(100), columnInfo.getLength());

    columnInfo = columnInfos.get(1);
    assertEquals("authorization_name", columnInfo.getName());
    assertEquals(String.class, columnInfo.getType());
    assertEquals(Integer.valueOf(255), columnInfo.getLength());

    // Verify permission_roleauthorization table
    columnInfos = columnsCapturePermissionRoleAuthorization.getValue();
    assertEquals(2, columnInfos.size());

    columnInfo = columnInfos.get(0);
    assertEquals("permission_id", columnInfo.getName());
    assertEquals(Long.class, columnInfo.getType());
    assertEquals(null, columnInfo.getLength());

    columnInfo = columnInfos.get(1);
    assertEquals("authorization_id", columnInfo.getName());
    assertEquals(String.class, columnInfo.getType());
    assertEquals(Integer.valueOf(100), columnInfo.getLength());
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    final DBAccessor dbAccessor = injector.getInstance(DBAccessor.class);
    UpgradeCatalog230 upgradeCatalog = injector.getInstance(UpgradeCatalog230.class);

    final ResourceTypeEntity ambariResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(ambariResourceTypeEntity.getId()).andReturn(1).anyTimes();

    final ResourceTypeEntity clusterResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(clusterResourceTypeEntity.getId()).andReturn(2).anyTimes();

    final ResourceTypeEntity viewResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(viewResourceTypeEntity.getId()).andReturn(3).anyTimes();

    final ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("AMBARI")).andReturn(ambariResourceTypeEntity).anyTimes();
    expect(resourceTypeDAO.findByName("CLUSTER")).andReturn(clusterResourceTypeEntity).anyTimes();
    expect(resourceTypeDAO.findByName("VIEW")).andReturn(viewResourceTypeEntity).anyTimes();

    final PermissionEntity viewUserPermissionEntity = createMock(PermissionEntity.class);
    expect(viewUserPermissionEntity.getId()).andReturn(1).anyTimes();

    final PermissionEntity ambariAdministratorPermissionEntity = createMock(PermissionEntity.class);
    expect(ambariAdministratorPermissionEntity.getId()).andReturn(2).anyTimes();

    final PermissionEntity clusterUserPermissionEntity = createMock(PermissionEntity.class);
    expect(clusterUserPermissionEntity.getId()).andReturn(3).anyTimes();

    final PermissionEntity clusterOperatorPermissionEntity = createMock(PermissionEntity.class);
    expect(clusterOperatorPermissionEntity.getId()).andReturn(4).anyTimes();

    final PermissionEntity clusterAdministratorPermissionEntity = createMock(PermissionEntity.class);
    expect(clusterAdministratorPermissionEntity.getId()).andReturn(5).anyTimes();

    final PermissionEntity serviceAdministratorPermissionEntity = createMock(PermissionEntity.class);
    expect(serviceAdministratorPermissionEntity.getId()).andReturn(6).anyTimes();

    final PermissionEntity serviceOperatorPermissionEntity = createMock(PermissionEntity.class);
    expect(serviceOperatorPermissionEntity.getId()).andReturn(7).anyTimes();

    final PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("VIEW.USER", viewResourceTypeEntity))
        .andReturn(viewUserPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR", ambariResourceTypeEntity))
        .andReturn(ambariAdministratorPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.USER", clusterResourceTypeEntity))
        .andReturn(clusterUserPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", clusterResourceTypeEntity))
        .andReturn(clusterOperatorPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.ADMINISTRATOR", clusterResourceTypeEntity))
        .andReturn(clusterAdministratorPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("SERVICE.ADMINISTRATOR", clusterResourceTypeEntity))
        .andReturn(serviceAdministratorPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("SERVICE.OPERATOR", clusterResourceTypeEntity))
        .andReturn(serviceOperatorPermissionEntity)
        .anyTimes();

    String updateQueryPattern;

    // Set permission labels
    updateQueryPattern = "UPDATE adminpermission SET permission_label='%s' WHERE permission_id=%d";
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        "Ambari Administrator", PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION)))
        .andReturn(1).once();
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        "Cluster User", PermissionEntity.CLUSTER_USER_PERMISSION)))
        .andReturn(1).once();
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        "Cluster Administrator", PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION)))
        .andReturn(1).once();
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        "View User", PermissionEntity.VIEW_USER_PERMISSION)))
        .andReturn(1).once();

    // Update permissions names
    updateQueryPattern = "UPDATE adminpermission SET permission_name='%s' WHERE permission_id=%d";
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME, PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION)))
        .andReturn(1).once();
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        PermissionEntity.CLUSTER_USER_PERMISSION_NAME, PermissionEntity.CLUSTER_USER_PERMISSION)))
        .andReturn(1).once();
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION_NAME, PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION)))
        .andReturn(1).once();
    expect(dbAccessor.executeUpdate(String.format(updateQueryPattern,
        PermissionEntity.VIEW_USER_PERMISSION_NAME, PermissionEntity.VIEW_USER_PERMISSION)))
        .andReturn(1).once();

    RoleAuthorizationEntity roleAuthorization = createMock(RoleAuthorizationEntity.class);

    RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);
    expect(roleAuthorizationDAO.findById(anyString())).andReturn(roleAuthorization).anyTimes();

    Collection<RoleAuthorizationEntity> authorizations = new ArrayList<RoleAuthorizationEntity>();

    expect(ambariAdministratorPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();
    expect(clusterAdministratorPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();
    expect(clusterOperatorPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();
    expect(serviceAdministratorPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();
    expect(serviceOperatorPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();
    expect(clusterUserPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();
    expect(viewUserPermissionEntity.getAuthorizations()).andReturn(authorizations).atLeastOnce();

    expect(permissionDAO.merge(ambariAdministratorPermissionEntity)).andReturn(ambariAdministratorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(clusterAdministratorPermissionEntity)).andReturn(clusterAdministratorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(clusterOperatorPermissionEntity)).andReturn(clusterOperatorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(serviceAdministratorPermissionEntity)).andReturn(serviceAdministratorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(serviceOperatorPermissionEntity)).andReturn(serviceOperatorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(clusterUserPermissionEntity)).andReturn(clusterUserPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(viewUserPermissionEntity)).andReturn(viewUserPermissionEntity).atLeastOnce();

    replayAll();
    upgradeCatalog.executeDMLUpdates();
    verifyAll();
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    UpgradeCatalog upgradeCatalog = injector.getInstance(UpgradeCatalog230.class);
    Assert.assertEquals("2.3.0", upgradeCatalog.getTargetVersion());
  }

  @Test
  public void testGetSourceVersion() {
    UpgradeCatalog upgradeCatalog = injector.getInstance(UpgradeCatalog230.class);
    Assert.assertEquals("2.2.1", upgradeCatalog.getSourceVersion());
  }

}
