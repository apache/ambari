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
import static org.apache.ambari.server.upgrade.UpgradeCatalog274.APPID_PROPERTY_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog274.HDFS_SERVICE_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog274.NAMENODE_APP_ID;
import static org.apache.ambari.server.upgrade.UpgradeCatalog274.NAMENODE_COMPONENT_NAME;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ParameterizedSource;
import org.apache.ambari.server.state.alert.ScriptSource;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
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

  @Test
  public void testUpdateNameNodeAlertsAppId() {
    AlertDefinitionDAO alertDefinitionDAO = createMock(AlertDefinitionDAO.class);
    AmbariManagementController ambariManagementController = createMock(AmbariManagementController.class);
    AlertDefinitionFactory alertDefinitionFactory = new AlertDefinitionFactory();
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);

    expect(injector.getInstance(AlertDefinitionDAO.class)).andReturn(alertDefinitionDAO);
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(ambariManagementController);
    expect(injector.getInstance(AlertDefinitionFactory.class)).andReturn(alertDefinitionFactory);

    String sourceJson1 = "{\"path\":\"file.py\",\"parameters\":[{\"name\":\"appId\",\"value\":\"NAMENODE\"},{\"name\":\"metricName\",\"value\":\"jvm.JvmMetrics.MemHeapUsedM\"}],\"type\":\"SCRIPT\"}";
    String sourceJson2 = "{\"path\":\"file.py\",\"parameters\":[{\"name\":\"appId\",\"value\":\"Namenode\"},{\"name\":\"metricName\",\"value\":\"jvm.JvmMetrics.MemHeapUsedM\"}],\"type\":\"SCRIPT\"}";

    AlertDefinitionEntity alertDefinitionEntity1 = new AlertDefinitionEntity();
    alertDefinitionEntity1.setSource(sourceJson1);
    alertDefinitionEntity1.setSourceType(SourceType.SCRIPT);
    alertDefinitionEntity1.setClusterId(1L);
    alertDefinitionEntity1.setDefinitionId(1L);
    alertDefinitionEntity1.setScheduleInterval(1);

    AlertDefinitionEntity alertDefinitionEntity2 = new AlertDefinitionEntity();
    alertDefinitionEntity2.setSource(sourceJson2);
    alertDefinitionEntity2.setSourceType(SourceType.SCRIPT);
    alertDefinitionEntity2.setClusterId(1L);
    alertDefinitionEntity2.setDefinitionId(1L);
    alertDefinitionEntity2.setScheduleInterval(1);

    expect(ambariManagementController.getClusters()).andReturn(clusters);
    expect(clusters.getClusters()).andReturn(new HashMap(){{put("cl1", cluster);}});
    expect(cluster.getClusterId()).andReturn(1L);
    expect(cluster.getServices()).andReturn(new HashMap(){{put("HDFS", null);}});
    expect(alertDefinitionDAO.findByServiceComponent(1L, HDFS_SERVICE_NAME, NAMENODE_COMPONENT_NAME))
        .andReturn(new ArrayList(){{add(alertDefinitionEntity1); add(alertDefinitionEntity2);}});

    Capture<AlertDefinitionEntity> alertDefinitionEntityCapture = newCapture(CaptureType.ALL);
    expect(alertDefinitionDAO.merge(capture(alertDefinitionEntityCapture))).andReturn(new AlertDefinitionEntity()).times(2);

    replay(alertDefinitionDAO,ambariManagementController, clusters, cluster, injector);
    UpgradeCatalog274 upgradeCatalog274 = new UpgradeCatalog274(injector);
    upgradeCatalog274.updateNameNodeAlertsAppId();

    verify(alertDefinitionDAO,ambariManagementController, clusters, cluster, injector);

    Assert.assertTrue(alertDefinitionEntityCapture.hasCaptured());
    List<AlertDefinitionEntity> resultAlertDefinitionEntities = alertDefinitionEntityCapture.getValues();
    Assert.assertEquals(2, resultAlertDefinitionEntities.size());
    for (AlertDefinitionEntity resultAlertDefinitionEntity : resultAlertDefinitionEntities) {
      String resultSource = resultAlertDefinitionEntity.getSource();
      Assert.assertNotNull(resultSource);

      ScriptSource resultScriptSource = (ScriptSource) alertDefinitionFactory.getGson().fromJson(resultSource, Source.class);
      boolean checked =  false;
      for (ParameterizedSource.AlertParameter alertParameter : resultScriptSource.getParameters()) {
        if (APPID_PROPERTY_NAME.equals(alertParameter.getName())) {
          Assert.assertEquals(NAMENODE_APP_ID, alertParameter.getValue());
          checked = true;
        }
      }
      Assert.assertTrue("No property with appropriate name (appId) was found", checked);
    }
  }
}
