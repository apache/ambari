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
package org.apache.ambari.server.orm.entities;

import org.junit.Assert;
import org.junit.Test;

public class MonitoringEntityTest {
  @Test
  public void testDatasourceCreateDoesNotRewriteCompatibilityJson() {
    DatasourceEntity entity = new DatasourceEntity() {
      @Override
      protected long currentEpochSeconds() {
        return 200L;
      }
    };
    String settings = "{\"unknown\":{\"order\":[3,1,2]}}";
    String http = "{\"url\":\"http://localhost:8428\",\"future\":true}";
    String auth = "{\"mode\":\"custom\",\"future_secret\":\"value\"}";
    entity.setPluginType("prometheus");
    entity.setSettings(settings);
    entity.setHttp(http);
    entity.setAuth(auth);

    entity.onCreate();

    Assert.assertEquals("prometheus", entity.getCategory());
    Assert.assertEquals(settings, entity.getSettings());
    Assert.assertEquals(http, entity.getHttp());
    Assert.assertEquals(auth, entity.getAuth());
    Assert.assertEquals(Long.valueOf(200L), entity.getCreatedAt());
    Assert.assertEquals(Long.valueOf(200L), entity.getUpdatedAt());
  }

  @Test
  public void testDatasourceCreatePreservesImportedAuditTimestamps() {
    DatasourceEntity entity = new DatasourceEntity() {
      @Override
      protected long currentEpochSeconds() {
        return 200L;
      }
    };
    entity.setCreatedAt(10L);
    entity.setUpdatedAt(20L);

    entity.onCreate();

    Assert.assertEquals(Long.valueOf(10L), entity.getCreatedAt());
    Assert.assertEquals(Long.valueOf(20L), entity.getUpdatedAt());
  }

  @Test
  public void testBoardCreatePreservesImportedAuditTimestamps() {
    BoardEntity entity = new BoardEntity() {
      @Override
      protected long currentEpochSeconds() {
        return 200L;
      }
    };
    entity.setCreateAt(10L);
    entity.setUpdateAt(20L);

    entity.onCreate();

    Assert.assertEquals(Long.valueOf(10L), entity.getCreateAt());
    Assert.assertEquals(Long.valueOf(20L), entity.getUpdateAt());
  }
}
