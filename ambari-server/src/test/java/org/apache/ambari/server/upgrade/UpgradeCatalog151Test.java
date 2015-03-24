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

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * UpgradeCatalog151 tests.
 */
public class UpgradeCatalog151Test {


  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);

    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    dbAccessor.createTable(eq("viewmain"), EasyMock.<List<DBAccessor.DBColumnInfo>>anyObject(), eq("view_name"));
    dbAccessor.createTable(eq("viewinstancedata"), EasyMock.<List<DBAccessor.DBColumnInfo>>anyObject(),
        eq("view_name"), eq("view_instance_name"), eq("name"));
    dbAccessor.createTable(eq("viewinstance"), EasyMock.<List<DBAccessor.DBColumnInfo>>anyObject(),
        eq("view_name"), eq("name"));
    dbAccessor.createTable(eq("viewinstanceproperty"), EasyMock.<List<DBAccessor.DBColumnInfo>>anyObject(),
        eq("view_name"), eq("view_instance_name"), eq("name"));
    dbAccessor.createTable(eq("viewparameter"), EasyMock.<List<DBAccessor.DBColumnInfo>>anyObject(),
        eq("view_name"), eq("name"));
    dbAccessor.createTable(eq("viewresource"), EasyMock.<List<DBAccessor.DBColumnInfo>>anyObject(),
        eq("view_name"), eq("name"));

    dbAccessor.addFKConstraint("viewparameter", "FK_viewparam_view_name", "view_name", "viewmain", "view_name", true);
    dbAccessor.addFKConstraint("viewresource", "FK_viewres_view_name", "view_name", "viewmain", "view_name", true);
    dbAccessor.addFKConstraint("viewinstance", "FK_viewinst_view_name", "view_name", "viewmain", "view_name", true);

    replay(dbAccessor, configuration);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog151 upgradeCatalog = (UpgradeCatalog151) getUpgradeCatalog(dbAccessor);

    upgradeCatalog.executeDMLUpdates();
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("1.5.1", upgradeCatalog.getTargetVersion());
  }

  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };
    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog151.class);
  }
  
  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("1.5.0", upgradeCatalog.getSourceVersion());
  }  
}
