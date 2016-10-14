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

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;

import org.junit.Test;

public class UpgradeCatalog300Test {

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method showHcatDeletedUserMessage = UpgradeCatalog300.class.getDeclaredMethod("showHcatDeletedUserMessage");

   UpgradeCatalog300 upgradeCatalog300 = createMockBuilder(UpgradeCatalog300.class)
            .addMockedMethod(showHcatDeletedUserMessage)
            .addMockedMethod(addNewConfigurationsFromXml)
            .createMock();


    upgradeCatalog300.addNewConfigurationsFromXml();
    upgradeCatalog300.showHcatDeletedUserMessage();


    replay(upgradeCatalog300);

    upgradeCatalog300.executeDMLUpdates();

    verify(upgradeCatalog300);
  }

}
