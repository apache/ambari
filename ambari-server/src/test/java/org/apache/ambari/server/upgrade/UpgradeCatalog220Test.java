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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * UpgradeCatalog220 tests.
 */
public class UpgradeCatalog220Test {


  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);

    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    Capture<DBAccessor.DBColumnInfo> columnCapture = new Capture<DBAccessor.DBColumnInfo>();

    dbAccessor.alterColumn(eq("host_role_command"), capture(columnCapture));
    expectLastCall();


    replay(dbAccessor, configuration);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration);

    assertTrue(columnCapture.getValue().isNullable());
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog220 upgradeCatalog = (UpgradeCatalog220) getUpgradeCatalog(dbAccessor);

    upgradeCatalog.executeDMLUpdates();
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.2.0", upgradeCatalog.getTargetVersion());
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("2.1.0", upgradeCatalog.getSourceVersion());
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
    return injector.getInstance(UpgradeCatalog220.class);
  }


}
