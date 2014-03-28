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

package org.apache.ambari.server.view;

import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.orm.entities.ViewEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Module which allows for discovery of view resource providers.
 * This module wraps another module and delegates to it for
 * any resource types not defined in a view.
 */
public class ViewProviderModule implements ProviderModule {
  /**
   * Mapping of view resource type to resource provider.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders;

  /**
   * The delegate provider module.
   */
  private final ProviderModule providerModule;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view provider module.
   *
   * @param providerModule     the delegate provider module
   * @param resourceProviders  the map of view resource types to resource providers
   */
  private ViewProviderModule(ProviderModule providerModule,
                            Map<Resource.Type, ResourceProvider> resourceProviders) {
    this.providerModule = providerModule;
    this.resourceProviders = resourceProviders;
  }


  // ----- ProviderModule ----------------------------------------------------

  @Override
  public ResourceProvider getResourceProvider(Resource.Type type) {

    if (resourceProviders.containsKey(type)) {
      return resourceProviders.get(type);
    }
    return providerModule.getResourceProvider(type);
  }

  @Override
  public List<PropertyProvider> getPropertyProviders(Resource.Type type) {
    return providerModule.getPropertyProviders(type);
  }


  // ----- helper methods -----------------------------------------

  /**
   * Factory method to get a view provider module.
   *
   * @param module  the delegate provider module
   *
   * @return a view provider module
   */
  public static ViewProviderModule getViewProviderModule(ProviderModule module) {
    Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

    ViewRegistry registry = ViewRegistry.getInstance();
    for (ViewEntity definition : registry.getDefinitions()) {
      for (Resource.Type type : definition.getViewResourceTypes()){
        ResourceProvider provider = definition.getResourceProvider(type);
        resourceProviders.put(type, provider);
      }
    }
    return new ViewProviderModule(module, resourceProviders);
  }
}
