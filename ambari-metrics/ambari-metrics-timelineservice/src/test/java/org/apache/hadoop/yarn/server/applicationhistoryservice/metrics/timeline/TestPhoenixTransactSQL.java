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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.Condition;

public class TestPhoenixTransactSQL {
  @Test
  public void testConditionClause() throws Exception {
    Condition condition = new Condition(
      Arrays.asList("cpu_user", "mem_free"), "h1", "a1", "i1",
        1407959718L, 1407959918L, null, false);

    String preparedClause = condition.getConditionClause();
    String expectedClause = "METRIC_NAME IN (?, ?) AND HOSTNAME = ? AND " +
      "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);
  }


}
