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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;


/**
 * {@link UpgradeCatalog211} unit tests.
 */
public class UpgradeCatalog211Test extends EasyMockSupport {

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    Injector injector = initInjector();

    try {
      Provider<EntityManager> entityManagerProvider = initEntityManagerProvider();

      final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
      final OsFamily osFamily = createNiceMock(OsFamily.class);
      Configuration configuration = createNiceMock(Configuration.class);
      Connection connection = createNiceMock(Connection.class);
      Statement statement = createNiceMock(Statement.class);
      ResultSet resultSet = createNiceMock(ResultSet.class);
      expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
      expect(configuration.getDatabaseType()).andReturn(DatabaseType.DERBY).anyTimes();
      dbAccessor.getConnection();
      expectLastCall().andReturn(connection).anyTimes();
      connection.createStatement();
      expectLastCall().andReturn(statement).anyTimes();
      statement.executeQuery("SELECT COUNT(*) from ambari_sequences where sequence_name='hostcomponentstate_id_seq'");
      expectLastCall().andReturn(resultSet).atLeastOnce();

      ResultSet rs1 = createNiceMock(ResultSet.class);
      expect(rs1.next()).andReturn(Boolean.TRUE).once();

      statement.executeQuery(anyObject(String.class));
      expectLastCall().andReturn(rs1).anyTimes();

      Capture<String> queryCapture = EasyMock.newCapture();
      dbAccessor.executeQuery(capture(queryCapture));
      expectLastCall().once();

      dbAccessor.setColumnNullable("viewinstanceproperty", "value", true);
      expectLastCall().once();
      dbAccessor.setColumnNullable("viewinstancedata", "value", true);
      expectLastCall().once();

      // Create DDL sections with their own capture groups
      // Example: AlertSectionDDL alertSectionDDL = new AlertSectionDDL();

      // Execute any DDL schema changes
      // Example: alertSectionDDL.execute(dbAccessor);

      // Replay sections
      replayAll();

      AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor, osFamily, entityManagerProvider.get());
      Class<?> c = AbstractUpgradeCatalog.class;
      Field f = c.getDeclaredField("configuration");
      f.setAccessible(true);
      f.set(upgradeCatalog, configuration);

      f = UpgradeCatalog211.class.getDeclaredField("m_hcsId");
      f.setAccessible(true);
      f.set(upgradeCatalog, new AtomicLong(1001));

      upgradeCatalog.executeDDLUpdates();
      verifyAll();

      Assert.assertTrue(queryCapture.hasCaptured());
      Assert.assertTrue(queryCapture.getValue().contains("1001"));

      // Verify sections
      // Example: alertSectionDDL.verify(dbAccessor);
    } finally {
      destroyInjector(injector);
    }
  }

  @Test
  public void testExecutePreDMLUpdates() throws Exception {

    final UpgradeCatalog211 upgradeCatalog211 = createMockBuilder(UpgradeCatalog211.class)
        // Add mocked methods. Example: .addMockedMethod(cleanupStackUpdates)
        .createMock();

    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(UpgradeCatalog211.class).toInstance(upgradeCatalog211);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    setInjector(upgradeCatalog211, injector);

    replayAll();

    injector.getInstance(UpgradeCatalog211.class).executePreDMLUpdates();

    verifyAll();
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final OsFamily osFamily = createNiceMock(OsFamily.class);

    final Cluster cluster = createMock(Cluster.class);

    final Clusters clusters = createMock(Clusters.class);
    expect(clusters.getClusters())
        .andReturn(Collections.singletonMap("c1", cluster));

    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    expect(controller.getClusters())
        .andReturn(clusters)
        .once();

    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(controller);
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(OsFamily.class).toInstance(osFamily);
      }
    });

    Method addNewConfigurationsFromXml =
        AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");

    Method updateKerberosConfigurations =
        UpgradeCatalog211.class.getDeclaredMethod("updateKerberosConfigurations", Cluster.class);

    UpgradeCatalog211 upgradeCatalog211 = createMockBuilder(UpgradeCatalog211.class)
        .addMockedMethod(addNewConfigurationsFromXml)
        .addMockedMethod(updateKerberosConfigurations)
        .createMock();

    setInjector(upgradeCatalog211, injector);

    upgradeCatalog211.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog211.updateKerberosConfigurations(anyObject(Cluster.class));
    expectLastCall().once();

    replayAll();

    upgradeCatalog211.executeDMLUpdates();

    verifyAll();
  }

  @Test
  public void testUpdateKerberosConfiguration() throws Exception {
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final OsFamily osFamily = createNiceMock(OsFamily.class);

    final Map<String, String> propertiesKerberosEnv = new HashMap<String, String>() {
      {
        put("create_attributes_template", "create_attributes_template content");
        put("realm", "EXAMPLE.COM");
        put("container_dn", "");
        put("ldap_url", "");
        put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5");
        put("kdc_host", "c6407.ambari.apache.org");
        put("admin_server_host", "c6407.ambari.apache.org");
        put("kdc_type", "mit-kdc");
      }
    };

    final Config configKerberosEnv = createNiceMock(Config.class);
    expect(configKerberosEnv.getProperties()).andReturn(propertiesKerberosEnv).anyTimes();
    expect(configKerberosEnv.getTag()).andReturn("tag1").anyTimes();

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("kerberos-env")).andReturn(configKerberosEnv).once();

    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(controller);
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(OsFamily.class).toInstance(osFamily);
      }
    });

    /* *********************************************************
     * Expects for updateConfigurationPropertiesForCluster
     * **** */
    expect(cluster.getConfigsByType("kerberos-env"))
        .andReturn(Collections.singletonMap("tag1", configKerberosEnv))
        .once();

    expect(cluster.getDesiredConfigByType("kerberos-env"))
        .andReturn(configKerberosEnv)
        .once();

    Capture<ConfigurationRequest> captureCR = EasyMock.newCapture();
    Capture<Cluster> clusterCapture = newCapture();
    Capture<String> typeCapture = newCapture();
    Capture<Map<String, String>> propertiesCapture = newCapture();
    Capture<String> tagCapture = newCapture();
    Capture<Map<String, Map<String, String>>> attributesCapture = newCapture();


    expect(controller.createConfig(capture(clusterCapture), capture(typeCapture),
        capture(propertiesCapture), capture(tagCapture), capture(attributesCapture) ))
        .andReturn(createNiceMock(Config.class))
        .once();

    /* ****
     * Expects for updateConfigurationPropertiesForCluster (end)
     * ********************************************************* */

    replayAll();

    injector.getInstance(UpgradeCatalog211.class).updateKerberosConfigurations(cluster);

    verifyAll();

    Map<String, String> capturedCRProperties = propertiesCapture.getValue();
    Assert.assertNotNull(capturedCRProperties);
    Assert.assertFalse(capturedCRProperties.containsKey("create_attributes_template"));
    Assert.assertTrue(capturedCRProperties.containsKey("ad_create_attributes_template"));

    for (String property : propertiesKerberosEnv.keySet()) {
      if ("create_attributes_template".equals(property)) {
        Assert.assertEquals("create_attributes_template/ad_create_attributes_template", propertiesKerberosEnv.get(property), capturedCRProperties.get("ad_create_attributes_template"));
      } else {
        Assert.assertEquals(property, propertiesKerberosEnv.get(property), capturedCRProperties.get(property));
      }
    }
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final OsFamily osFamily = createNiceMock(OsFamily.class);
    Provider<EntityManager> entityManagerProvider = initEntityManagerProvider();

    replayAll();

    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor, osFamily, entityManagerProvider.get());

    Assert.assertEquals("2.1.0", upgradeCatalog.getSourceVersion());

    verifyAll();
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final OsFamily osFamily = createNiceMock(OsFamily.class);
    Provider<EntityManager> entityManagerProvider = initEntityManagerProvider();

    replayAll();

    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor, osFamily, entityManagerProvider.get());

    Assert.assertEquals("2.1.1", upgradeCatalog.getTargetVersion());

    verifyAll();
  }

  private Provider<EntityManager> initEntityManagerProvider() {
    Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);

    EntityManager entityManager = createNiceMock(EntityManager.class);
    expect(entityManagerProvider.get())
        .andReturn(entityManager)
        .anyTimes();

    return entityManagerProvider;
  }

  private Injector initInjector() {
    Injector injector;

    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);

    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    stackDAO.find("HDP", "2.2.0");

    return injector;
  }

  private void destroyInjector(Injector injector) {
    injector.getInstance(PersistService.class).stop();
  }

  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor, final OsFamily osFamily, final EntityManager entityManager) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
      }
    };

    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog211.class);
  }

  private void setInjector(UpgradeCatalog211 upgradeCatalog211, Injector injector) throws NoSuchFieldException, IllegalAccessException {
    Field fieldInjector = AbstractUpgradeCatalog.class.getDeclaredField("injector");
    if (fieldInjector != null) {
      fieldInjector.set(upgradeCatalog211, injector);
    }
  }

  // *********** Inner Classes that represent sections of the DDL ***********
  // ************************************************************************

  /**
   * Example *SectionDDL class
   */
  /*
  class AlertSectionDDL implements SectionDDL {
    HashMap<String, Capture<String>> stringCaptures;
    HashMap<String, Capture<Class>> classCaptures;


    public AlertSectionDDL() {
      stringCaptures = new HashMap<String, Capture<String>>();
      classCaptures = new HashMap<String, Capture<Class>>();

      Capture<String> textCaptureC = EasyMock.newCapture();
      Capture<String> textCaptureH = EasyMock.newCapture();
      Capture<Class>  classFromC = EasyMock.newCapture();
      Capture<Class>  classFromH = EasyMock.newCapture();
      Capture<Class>  classToC = EasyMock.newCapture();
      Capture<Class>  classToH = EasyMock.newCapture();

      stringCaptures.put("textCaptureC", textCaptureC);
      stringCaptures.put("textCaptureH", textCaptureH);
      classCaptures.put("classFromC", classFromC);
      classCaptures.put("classFromH", classFromH);
      classCaptures.put("classToC", classToC);
      classCaptures.put("classToH", classToH);
    }

    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<String> textCaptureC = stringCaptures.get("textCaptureC");
      Capture<String> textCaptureH = stringCaptures.get("textCaptureH");
      Capture<Class>  classFromC = classCaptures.get("classFromC");
      Capture<Class>  classFromH = classCaptures.get("classFromH");
      Capture<Class>  classToC = classCaptures.get("classToC");
      Capture<Class>  classToH = classCaptures.get("classToH");

      dbAccessor.changeColumnType(eq("alert_current"), capture(textCaptureC), capture(classFromC), capture(classToC));
      dbAccessor.changeColumnType(eq("alert_history"), capture(textCaptureH), capture(classFromH), capture(classToH));
    }

    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      Capture<String> textCaptureC = stringCaptures.get("textCaptureC");
      Capture<String> textCaptureH = stringCaptures.get("textCaptureH");
      Capture<Class>  classFromC = classCaptures.get("classFromC");
      Capture<Class>  classFromH = classCaptures.get("classFromH");
      Capture<Class>  classToC = classCaptures.get("classToC");
      Capture<Class>  classToH = classCaptures.get("classToH");

      Assert.assertEquals("latest_text", textCaptureC.getValue());
      Assert.assertEquals(String.class, classFromC.getValue());
      Assert.assertEquals(char[].class, classToC.getValue());

      Assert.assertEquals("alert_text", textCaptureH.getValue());
      Assert.assertEquals(String.class, classFromH.getValue());
      Assert.assertEquals(char[].class, classToH.getValue());
    }
  }
  */
}
