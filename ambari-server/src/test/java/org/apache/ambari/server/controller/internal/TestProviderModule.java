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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.jdbc.TestJDBCResourceProvider;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.DBHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Module to plug in the JDBC resource provider.
 */
public class TestProviderModule implements ProviderModule {
  private static final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();
  private static final Map<Resource.Type, List<PropertyProvider>> propertyProviders = new HashMap<Resource.Type, List<PropertyProvider>>();

  static {

    for (Resource.Type type : Resource.Type.values()) {
      resourceProviders.put(type, new TestJDBCResourceProvider(
          DBHelper.CONNECTION_FACTORY,
          type,
          PropertyHelper.getPropertyIds(type),
          PropertyHelper.getKeyPropertyIds(type)));
    }

    propertyProviders.put(Resource.Type.Cluster, new LinkedList<PropertyProvider>());
    propertyProviders.put(Resource.Type.Service, new LinkedList<PropertyProvider>());
    propertyProviders.put(Resource.Type.Component, new LinkedList<PropertyProvider>());
    propertyProviders.put(Resource.Type.Host, new LinkedList<PropertyProvider>());

    List<PropertyProvider> providers = new LinkedList<PropertyProvider>();

    propertyProviders.put(Resource.Type.Component, providers);

    providers = new LinkedList<PropertyProvider>();

    propertyProviders.put(Resource.Type.HostComponent, providers);
  }

  @Override
  public ResourceProvider getResourceProvider(Resource.Type type) {
    return resourceProviders.get(type);
  }

  @Override
  public List<PropertyProvider> getPropertyProviders(Resource.Type type) {
    return propertyProviders.get(type);
  }
}
