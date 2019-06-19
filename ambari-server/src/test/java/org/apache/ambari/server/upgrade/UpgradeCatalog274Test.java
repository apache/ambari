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

import static org.apache.ambari.server.upgrade.UpgradeCatalog274.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog274.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog274.AMBARI_CONFIGURATION_TABLE;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.ambari.server.orm.DBAccessor;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

public class UpgradeCatalog274Test {

  private Injector injector;
  private DBAccessor dbAccessor;

  @Before
  public void init() {
    final EasyMockSupport easyMockSupport = new EasyMockSupport();
    injector = easyMockSupport.createNiceMock(Injector.class);
    dbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    DBAccessor.DBColumnInfo dbColumnInfo = new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN,
      String.class, 2000);

    final Capture<DBAccessor.DBColumnInfo> alterPropertyValueColumnCapture = newCapture(CaptureType.ALL);
    dbAccessor.getColumnInfo(eq(AMBARI_CONFIGURATION_TABLE), eq(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN));
    expectLastCall().andReturn(dbColumnInfo).once();

    dbAccessor.alterColumn(eq(AMBARI_CONFIGURATION_TABLE), capture(alterPropertyValueColumnCapture));
    expectLastCall().once();

    replay(dbAccessor, injector);

    UpgradeCatalog274 upgradeCatalog274 = new UpgradeCatalog274(injector);
    upgradeCatalog274.dbAccessor = dbAccessor;
    upgradeCatalog274.executeDDLUpdates();

    final DBAccessor.DBColumnInfo alterPropertyValueColumn = alterPropertyValueColumnCapture.getValue();
    Assert.assertEquals(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, alterPropertyValueColumn.getName());
    Assert.assertEquals(String.class, alterPropertyValueColumn.getType());
    Assert.assertEquals(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN, alterPropertyValueColumn.getLength());

    verify(dbAccessor);
  }
}
