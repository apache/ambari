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

package org.apache.ambari.view.hive.client;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ConnectionPool {
  private final static Logger LOG =
      LoggerFactory.getLogger(ConnectionPool.class);

  private static Map<String, Connection> viewSingletonObjects = new HashMap<String, Connection>();
  /**
   * Returns HdfsApi object specific to instance
   * @param context View Context instance
   * @return Hdfs business delegate object
   */
  public static Connection getConnection(ViewContext context) {
    if (!viewSingletonObjects.containsKey(context.getInstanceName()))
      viewSingletonObjects.put(context.getInstanceName(), connectToHive(context));
    return viewSingletonObjects.get(context.getInstanceName());
  }

  private static Connection connectToHive(ViewContext context) {
    try {
      return new Connection(getHiveHost(context), Integer.valueOf(getHivePort(context)), getHiveAuthParams(context));
    } catch (HiveClientException e) {
      throw new ServiceFormattedException("Couldn't open connection to Hive: " + e.toString(), e);
    }
  }

  public static void setInstance(ViewContext context, Connection api) {
    viewSingletonObjects.put(context.getInstanceName(), api);
  }

  private static String getHiveHost(ViewContext context) {
    return context.getProperties().get("hive.host");
  }

  private static String getHivePort(ViewContext context) {
    return context.getProperties().get("hive.port");
  }

  private static Map<String, String> getHiveAuthParams(ViewContext context) {
    String auth = context.getProperties().get("hive.auth");
    Map<String, String> params = new HashMap<String, String>();
    if (auth == null || auth.isEmpty()) {
      auth = "auth=NOSASL";
    }
    for(String param : auth.split(";")) {
      String[] keyvalue = param.split("=");
      if (keyvalue.length != 2) {
        LOG.error("Can not parse authentication param " + param + " in " + auth);
        continue;
      }
      params.put(keyvalue[0], keyvalue[1]);
    }
    return params;
  }
}
