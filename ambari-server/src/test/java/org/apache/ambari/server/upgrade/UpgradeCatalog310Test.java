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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.List;

import org.apache.ambari.server.orm.DBAccessor;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

public class UpgradeCatalog310Test {
  private Injector injector;
  private DBAccessor dbAccessor;
  private UpgradeCatalog310 catalog;

  @Before
  public void setUp() {
    EasyMockSupport mocks = new EasyMockSupport();
    injector = mocks.createNiceMock(Injector.class);
    dbAccessor = mocks.createMock(DBAccessor.class);
    catalog = new UpgradeCatalog310(injector);
    catalog.dbAccessor = dbAccessor;
  }

  @Test
  public void testCreatesMissingMonitoringTables() throws Exception {
    expect(dbAccessor.tableExists(UpgradeCatalog310.DATASOURCE_TABLE)).andReturn(false);
    dbAccessor.createTable(eq(UpgradeCatalog310.DATASOURCE_TABLE), anyObject(List.class), eq("id"));
    expectLastCall();
    dbAccessor.addUniqueConstraint(UpgradeCatalog310.DATASOURCE_TABLE,
        UpgradeCatalog310.DATASOURCE_NAME_CLUSTER_CONSTRAINT, "cluster_name", "name");
    expectLastCall();
    dbAccessor.createIndex("idx_datasource_cluster", UpgradeCatalog310.DATASOURCE_TABLE, "cluster_name");
    expectLastCall();

    expect(dbAccessor.tableExists(UpgradeCatalog310.BOARD_TABLE)).andReturn(false);
    dbAccessor.createTable(eq(UpgradeCatalog310.BOARD_TABLE), anyObject(List.class), eq("id"));
    expectLastCall();
    dbAccessor.addUniqueConstraint(UpgradeCatalog310.BOARD_TABLE,
        UpgradeCatalog310.BOARD_CLUSTER_GROUP_NAME_CONSTRAINT, "cluster_name", "group_id", "name");
    expectLastCall();
    dbAccessor.createIndex("idx_board_ident", UpgradeCatalog310.BOARD_TABLE, "ident");
    expectLastCall();
    dbAccessor.createIndex("idx_board_cluster", UpgradeCatalog310.BOARD_TABLE, "cluster_name");
    expectLastCall();

    expect(dbAccessor.tableExists(UpgradeCatalog310.BOARD_PAYLOAD_TABLE)).andReturn(false);
    dbAccessor.createTable(eq(UpgradeCatalog310.BOARD_PAYLOAD_TABLE), anyObject(List.class), eq("id"));
    expectLastCall();
    dbAccessor.addFKConstraint(UpgradeCatalog310.BOARD_PAYLOAD_TABLE, "FK_board_payload_board", "id",
        UpgradeCatalog310.BOARD_TABLE, "id", true, false);
    expectLastCall();

    expect(dbAccessor.tableExists(UpgradeCatalog310.CHART_SHARE_TABLE)).andReturn(false);
    dbAccessor.createTable(eq(UpgradeCatalog310.CHART_SHARE_TABLE), anyObject(List.class), eq("id"));
    expectLastCall();
    dbAccessor.createIndex("idx_chart_share_create_at", UpgradeCatalog310.CHART_SHARE_TABLE, "create_at");
    expectLastCall();
    dbAccessor.createIndex("idx_chart_share_cluster", UpgradeCatalog310.CHART_SHARE_TABLE, "cluster");
    expectLastCall();

    replay(dbAccessor, injector);

    catalog.createDatasourceTable();
    catalog.createBoardTable();
    catalog.createBoardPayloadTable();
    catalog.createChartShareTable();

    verify(dbAccessor);
  }

  @Test
  public void testExistingMetricsTablesAreNotRecreated() throws Exception {
    expect(dbAccessor.tableExists(UpgradeCatalog310.DATASOURCE_TABLE)).andReturn(true);
    dbAccessor.dropUniqueConstraint(UpgradeCatalog310.DATASOURCE_TABLE,
        UpgradeCatalog310.OLD_DATASOURCE_NAME_CONSTRAINT, true);
    expectLastCall();
    dbAccessor.dropUniqueConstraint(UpgradeCatalog310.DATASOURCE_TABLE,
        UpgradeCatalog310.DATASOURCE_NAME_CLUSTER_CONSTRAINT, true);
    expectLastCall();
    dbAccessor.addUniqueConstraint(UpgradeCatalog310.DATASOURCE_TABLE,
        UpgradeCatalog310.DATASOURCE_NAME_CLUSTER_CONSTRAINT, "cluster_name", "name");
    expectLastCall();
    expect(dbAccessor.tableExists(UpgradeCatalog310.BOARD_TABLE)).andReturn(true);
    expect(dbAccessor.tableHasColumn(UpgradeCatalog310.BOARD_TABLE, "cluster_name")).andReturn(true);
    dbAccessor.dropUniqueConstraint(UpgradeCatalog310.BOARD_TABLE,
        UpgradeCatalog310.OLD_BOARD_GROUP_NAME_CONSTRAINT, true);
    expectLastCall();
    dbAccessor.dropUniqueConstraint(UpgradeCatalog310.BOARD_TABLE,
        UpgradeCatalog310.BOARD_CLUSTER_GROUP_NAME_CONSTRAINT, true);
    expectLastCall();
    dbAccessor.addUniqueConstraint(UpgradeCatalog310.BOARD_TABLE,
        UpgradeCatalog310.BOARD_CLUSTER_GROUP_NAME_CONSTRAINT, "cluster_name", "group_id", "name");
    expectLastCall();
    dbAccessor.createIndex("idx_board_cluster", UpgradeCatalog310.BOARD_TABLE, "cluster_name");
    expectLastCall();
    expect(dbAccessor.tableExists(UpgradeCatalog310.BOARD_PAYLOAD_TABLE)).andReturn(true);
    expect(dbAccessor.tableExists(UpgradeCatalog310.CHART_SHARE_TABLE)).andReturn(true);
    replay(dbAccessor, injector);

    catalog.createDatasourceTable();
    catalog.createBoardTable();
    catalog.createBoardPayloadTable();
    catalog.createChartShareTable();

    verify(dbAccessor);
  }
}
