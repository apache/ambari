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

package org.apache.ambari.api.controller.utilities;

import org.apache.ambari.api.controller.internal.ClusterControllerImpl;
import org.apache.ambari.api.controller.internal.SchemaImpl;
import org.apache.ambari.api.controller.jdbc.ConnectionFactory;
import org.apache.ambari.api.controller.jdbc.JDBCResourceProvider;
import org.apache.ambari.api.controller.jdbc.SQLiteConnectionFactory;
import org.apache.ambari.api.controller.spi.ClusterController;
import org.apache.ambari.api.controller.spi.PropertyProvider;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.spi.ResourceProvider;
import org.apache.ambari.api.controller.spi.Schema;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Temporary class to bootstrap a cluster controller.  TODO : Replace this global state with injection.
 */
public class ClusterControllerHelper {
  private static ClusterController controller;

  public static final ConnectionFactory CONNECTION_FACTORY = new SQLiteConnectionFactory();


  public static synchronized ClusterController getClusterController() {

    if (controller == null) {
      controller = new ClusterControllerImpl(getResourceSchemas());
    }
    return controller;
  }

  private static Map<Resource.Type, Schema> getResourceSchemas() {
    Map<Resource.Type, Schema> schemas = new HashMap<Resource.Type, Schema>();

    schemas.put(Resource.Type.Cluster, getResourceSchema(Resource.Type.Cluster));

    return schemas;
  }

  private static Schema getResourceSchema(Resource.Type type) {

    ResourceProvider resourceProvider =  JDBCResourceProvider.create(CONNECTION_FACTORY, type);
    List<PropertyProvider> propertyProviders = new LinkedList<PropertyProvider>();

    return new SchemaImpl(type, resourceProvider, propertyProviders, Properties.getKeyPropertyIds(type));
  }
}
