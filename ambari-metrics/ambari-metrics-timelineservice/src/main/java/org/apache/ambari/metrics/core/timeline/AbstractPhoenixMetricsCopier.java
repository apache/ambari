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
package org.apache.ambari.metrics.core.timeline;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public abstract class AbstractPhoenixMetricsCopier implements Runnable {
  protected String inputTable;
  protected String outputTable;
  protected Set<String> metricNames;
  protected PhoenixHBaseAccessor hBaseAccessor;

  public AbstractPhoenixMetricsCopier(String inputTableName, String outputTableName, PhoenixHBaseAccessor hBaseAccessor, Set<String> metricNames) {
    this.inputTable = inputTableName;
    this.outputTable = outputTableName;
    this.hBaseAccessor = hBaseAccessor;
    this.metricNames = metricNames;
  }

  public void run(){
    for (String metricName : metricNames) {
//      TODO do batch selects
      //TODO limit start time
      String query = String.format("SELECT * FROM %s WHERE METRIC_NAME='%s' ORDER BY METRIC_NAME, SERVER_TIME", inputTable, metricName);
      try {
        Connection conn = hBaseAccessor.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
          addToResults(rs);
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    try {
      saveResults();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  protected abstract void saveResults() throws SQLException;

  protected abstract void addToResults(ResultSet rs) throws SQLException;
}
