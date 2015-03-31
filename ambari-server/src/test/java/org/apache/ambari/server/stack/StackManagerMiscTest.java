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

package org.apache.ambari.server.stack;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.Test;

/**
 * StackManager Misc unit tests.
 */
public class StackManagerMiscTest  {

  @Test
  public void testCycleDetection() throws Exception {
    MetainfoDAO metaInfoDao = createNiceMock(MetainfoDAO.class);
    StackDAO stackDao = createNiceMock(StackDAO.class);
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    OsFamily osFamily = createNiceMock(OsFamily.class);
    StackEntity stackEntity = createNiceMock(StackEntity.class);

    expect(
        stackDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(stackEntity).atLeastOnce();

    replay(actionMetadata, stackDao, metaInfoDao, osFamily);

    try {
      String stacksCycle1 = ClassLoader.getSystemClassLoader().getResource("stacks_with_cycle").getPath();

      StackManager stackManager = new StackManager(new File(stacksCycle1),
          null, osFamily, metaInfoDao, actionMetadata, stackDao);

      fail("Expected exception due to cyclic stack");
    } catch (AmbariException e) {
      // expected
      assertEquals("Cycle detected while parsing stack definition", e.getMessage());
    }
    try {
      String stacksCycle2 = ClassLoader.getSystemClassLoader().getResource(
          "stacks_with_cycle2").getPath();

      StackManager stackManager = new StackManager(new File(stacksCycle2),
          null, osFamily, metaInfoDao, actionMetadata, stackDao);

      fail("Expected exception due to cyclic stack");
    } catch (AmbariException e) {
      // expected
      assertEquals("Cycle detected while parsing stack definition", e.getMessage());
    }
  }

  /**
   * This test ensures the service status check is added into the action metadata when
   * the stack has no parent and is the only stack in the stack family
   */
  @Test
  public void testGetServiceInfoFromSingleStack() throws Exception {
    MetainfoDAO metaInfoDao = createNiceMock(MetainfoDAO.class);
    StackDAO stackDao = createNiceMock(StackDAO.class);
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    OsFamily  osFamily = createNiceMock(OsFamily.class);
    StackEntity stackEntity = createNiceMock(StackEntity.class);

    // ensure that service check is added for HDFS
    actionMetadata.addServiceCheckAction("HDFS");

    expect(
        stackDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(stackEntity).atLeastOnce();

    replay(metaInfoDao, stackDao, actionMetadata, osFamily);

    String singleStack = ClassLoader.getSystemClassLoader().getResource("single_stack").getPath();

    StackManager stackManager = new StackManager(new File(singleStack.replace(
        StackManager.PATH_DELIMITER, File.separator)),
        null, osFamily, metaInfoDao, actionMetadata, stackDao);


    Collection<StackInfo> stacks = stackManager.getStacks();
    assertEquals(1, stacks.size());
    assertNotNull(stacks.iterator().next().getService("HDFS"));

    verify(metaInfoDao, stackDao, actionMetadata, osFamily);
  }
}
