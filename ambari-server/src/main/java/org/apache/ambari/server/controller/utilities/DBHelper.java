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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.jdbc.SQLiteConnectionFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class DBHelper {
  private static String DB_FILE_NAME = System.getProperty("ambariapi.dbfile", "src/test/resources/data.db");

  public static final ConnectionFactory CONNECTION_FACTORY = new SQLiteConnectionFactory(DB_FILE_NAME);

  private static final Map<String, String> HOSTS = readHosts();

  public static Map<String, String> getHosts() {
    return HOSTS;
  }

  private static Map<String, String> readHosts() {
    Map<String, String> hosts = new HashMap<String, String>();

    try {
      Connection connection = CONNECTION_FACTORY.getConnection();

      try {
        String sql = "select attributes from hosts";

        Statement statement = connection.createStatement();

        ResultSet rs = statement.executeQuery(sql);

        ObjectMapper mapper = new ObjectMapper();

        while (rs.next()) {
          String attributes = rs.getString(1);

          if (!attributes.startsWith("[]")) {
            try {
              Map<String, String> attributeMap = mapper.readValue(attributes, new TypeReference<Map<String, String>>() {
              });
              hosts.put(attributeMap.get("privateFQDN"), attributeMap.get("publicFQDN"));
            } catch (IOException e) {
              throw new IllegalStateException("Can't read hosts " + attributes, e);
            }
          }
        }

        statement.close();
      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("Can't access DB.", e);
    }

    return hosts;
  }
}
