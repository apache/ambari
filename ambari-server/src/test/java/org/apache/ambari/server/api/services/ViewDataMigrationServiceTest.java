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
package org.apache.ambari.server.api.services;

import junit.framework.Assert;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.ViewDataMigrationContextImpl;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * ViewDataMigrationService tests.
 */
public class ViewDataMigrationServiceTest {

  private static String viewName = "MY_VIEW";
  private static String instanceName = "INSTANCE1";
  private static String version1 = "1.0.0";
  private static String version2 = "2.0.0";

  private static String xml_view_with_migrator_v2 = "<view>\n" +
      "    <name>" + viewName + "</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>" + version2 + "</version>\n" +
      "    <data-version>1</data-version>\n" +
      "    <data-migrator-class>org.apache.ambari.server.api.services.ViewDataMigrationServiceTest$MyDataMigrator</data-migrator-class>\n" +
      "    <instance>\n" +
      "        <name>" + instanceName + "</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_view_with_migrator_v1 = "<view>\n" +
      "    <name>" + viewName + "</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>" + version1 + "</version>\n" +
      "    <instance>\n" +
      "        <name>" + instanceName + "</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    expect(viewRegistry.checkAdmin()).andReturn(true).anyTimes();
    viewRegistry.copyPrivileges(anyObject(ViewInstanceEntity.class), anyObject(ViewInstanceEntity.class));
    expectLastCall().anyTimes();
    replay(viewRegistry);
    ViewRegistry.initInstance(viewRegistry);
  }

  @Test
  public void testMigrateDataSameVersions() throws Exception {
    TestViewDataMigrationService service = new TestViewDataMigrationService(viewName, version2, instanceName);

    ViewDataMigrationContextImpl context = createNiceMock(ViewDataMigrationContextImpl.class);
    expect(context.getOriginDataVersion()).andReturn(42);
    expect(context.getCurrentDataVersion()).andReturn(42);
    replay(context);
    service.setMigrationContext(context);

    ViewDataMigrator migrator =  service.getViewDataMigrator(
        service.getViewInstanceEntity(viewName, version2, instanceName), context);

    Assert.assertTrue(migrator instanceof ViewDataMigrationService.CopyAllDataMigrator);
  }

  @Test
  public void testMigrateDataDifferentVersions() throws Exception {
    TestViewDataMigrationService service = new TestViewDataMigrationService(viewName, version2, instanceName);

    ViewDataMigrationContextImpl context = getViewDataMigrationContext();
    service.setMigrationContext(context);

    ViewDataMigrator migrator = createStrictMock(ViewDataMigrator.class);
    expect(migrator.beforeMigration()).andReturn(true);
    migrator.migrateEntity(anyObject(Class.class), anyObject(Class.class)); expectLastCall();
    migrator.migrateInstanceData(); expectLastCall();
    migrator.afterMigration(); expectLastCall();

    replay(migrator);
    service.setMigrator(migrator);

    service.migrateData(version1, instanceName);

    verify(migrator);
  }

  @Test
  public void testMigrateDataDifferentVersionsCancel() throws Exception {
    TestViewDataMigrationService service = new TestViewDataMigrationService(viewName, version2, instanceName);

    ViewDataMigrationContextImpl context = getViewDataMigrationContext();
    service.setMigrationContext(context);

    ViewDataMigrator migrator = createStrictMock(ViewDataMigrator.class);
    expect(migrator.beforeMigration()).andReturn(false);

    replay(migrator);
    service.setMigrator(migrator);

    thrown.expect(ViewDataMigrationException.class);
    service.migrateData(version1, instanceName);
  }

  private static ViewDataMigrationContextImpl getViewDataMigrationContext() {
    Map<String, Class> entities = Collections.<String, Class>singletonMap("MyEntityClass", Object.class);
    ViewDataMigrationContextImpl context = createNiceMock(ViewDataMigrationContextImpl.class);
    expect(context.getOriginDataVersion()).andReturn(2).anyTimes();
    expect(context.getCurrentDataVersion()).andReturn(1).anyTimes();
    expect(context.getOriginEntityClasses()).andReturn(entities).anyTimes();
    expect(context.getCurrentEntityClasses()).andReturn(entities).anyTimes();
    replay(context);
    return context;
  }

  //Migration service that avoids ViewRegistry and DB calls
  private static class TestViewDataMigrationService extends ViewDataMigrationService {

    private ViewDataMigrator migrator;
    private ViewDataMigrationContextImpl migrationContext;

    public TestViewDataMigrationService(String viewName, String viewVersion, String instanceName) {
      super(viewName, viewVersion, instanceName);
    }

    @Override
    protected ViewInstanceEntity getViewInstanceEntity(String viewName, String viewVersion, String instanceName) {
      ViewEntity viewEntity = createNiceMock(ViewEntity.class);
      expect(viewEntity.getViewName()).andReturn(viewName);
      expect(viewEntity.getVersion()).andReturn(viewVersion);

      replay(viewEntity);

      ViewInstanceEntity instanceEntity = createNiceMock(ViewInstanceEntity.class);
      expect(instanceEntity.getViewEntity()).andReturn(viewEntity);
      expect(instanceEntity.getViewName()).andReturn(viewName);
      expect(instanceEntity.getInstanceName()).andReturn(instanceName);

      try {
        ViewDataMigrator mockMigrator;
        if (migrator == null) {
          mockMigrator = createNiceMock(ViewDataMigrator.class);
        } else {
          mockMigrator = migrator;
        }
        expect(instanceEntity.getDataMigrator(anyObject(ViewDataMigrationContext.class))).
            andReturn(mockMigrator);
      } catch (Exception e) {
        e.printStackTrace();
      }

      replay(instanceEntity);
      return instanceEntity;
    }

    @Override
    protected ViewDataMigrationContextImpl getViewDataMigrationContext(ViewInstanceEntity instanceDefinition,
                                                                       ViewInstanceEntity originInstanceDefinition) {
      if (migrationContext == null) {
        ViewDataMigrationContextImpl contextMock = createNiceMock(ViewDataMigrationContextImpl.class);
        replay(contextMock);
        return contextMock;
      }
      return migrationContext;
    }

    public ViewDataMigrator getMigrator() {
      return migrator;
    }

    public void setMigrator(ViewDataMigrator migrator) {
      this.migrator = migrator;
    }

    public ViewDataMigrationContextImpl getMigrationContext() {
      return migrationContext;
    }

    public void setMigrationContext(ViewDataMigrationContextImpl migrationContext) {
      this.migrationContext = migrationContext;
    }
  }
}
