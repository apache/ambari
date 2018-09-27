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


import static org.apache.ambari.server.upgrade.UpgradeCatalog280.HOST_COMPONENT_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog280.LAST_LIVE_STATE_COLUMN;
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

public class UpgradeCatalog280Test {

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

    Capture<DBAccessor.DBColumnInfo> perBatchLimitColumn = newCapture(CaptureType.ALL);
    dbAccessor.addColumn(eq("requestschedule"), capture(perBatchLimitColumn));
    expectLastCall().once();

    dbAccessor.dropColumn(eq(HOST_COMPONENT_STATE_TABLE), eq(LAST_LIVE_STATE_COLUMN));
    expectLastCall().once();


    replay(dbAccessor, injector);

    UpgradeCatalog280 upgradeCatalog280 = new UpgradeCatalog280(injector);
    upgradeCatalog280.dbAccessor = dbAccessor;
    upgradeCatalog280.executeDDLUpdates();

    DBAccessor.DBColumnInfo capturedBlueprintProvisioningStateColumn =
        perBatchLimitColumn.getValue();
    Assert.assertEquals("batch_toleration_limit_per_batch",
        capturedBlueprintProvisioningStateColumn.getName());
    Assert.assertEquals(null, capturedBlueprintProvisioningStateColumn.getDefaultValue());
    Assert.assertEquals(Short.class, capturedBlueprintProvisioningStateColumn.getType());

    verify(dbAccessor);
  }
}
