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
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Injector;

public class UpgradeCatalog275Test {

  @Test
  public void testRemoveDfsHAInitial() {
    Injector injector = createNiceMock(Injector.class);
    BlueprintDAO blueprintDAO = createMock(BlueprintDAO.class);

    BlueprintConfigEntity blueprintConfigEntity = new BlueprintConfigEntity();
    blueprintConfigEntity.setType("hadoop-env");
    blueprintConfigEntity.setConfigData("{\"dfs_ha_initial_namenode_standby\":\"%HOSTGROUP::master_2%\"," +
                                          "\"dfs_ha_initial_namenode_active\":\"u1602.ambari.apache.org\"}");

    List<BlueprintConfigEntity> blueprintConfigurations = Collections.singletonList(blueprintConfigEntity);

    BlueprintEntity blueprintEntity = new BlueprintEntity();
    blueprintEntity.setConfigurations(blueprintConfigurations);

    List<BlueprintEntity> blueprintEntityList = Collections.singletonList(blueprintEntity);

    expect(injector.getInstance(BlueprintDAO.class)).andReturn(blueprintDAO);
    expect(blueprintDAO.findAll()).andReturn(blueprintEntityList);

    Capture<BlueprintEntity> blueprintEntityCapture = Capture.newInstance();
    expect(blueprintDAO.merge(capture(blueprintEntityCapture))).andReturn(null);

    replay(injector, blueprintDAO);

    UpgradeCatalog275 upgradeCatalog275 = new UpgradeCatalog275(injector);
    upgradeCatalog275.removeDfsHAInitial();

    verify(injector, blueprintDAO);

    Assert.assertNotNull(blueprintEntityCapture.getValues());
    Assert.assertEquals(1, blueprintEntityCapture.getValues().size());

    BlueprintEntity blueprintEntityToMerge = blueprintEntityCapture.getValue();

    Collection<BlueprintConfigEntity> resultConfigurations = blueprintEntityToMerge.getConfigurations();
    for (BlueprintConfigEntity resultConfiguration : resultConfigurations) {
      if (resultConfiguration.getType().equals("hadoop-env")) {
        String configData = resultConfiguration.getConfigData();

        Map<String, String> typeProperties = UpgradeCatalog275.GSON.<Map<String, String>>fromJson(
          configData, Map.class);
        Assert.assertEquals(0, typeProperties.size());
        return;
      }
    }
    Assert.fail("No \"hadoop-env\" config type was found in result configuration");
  }
}
