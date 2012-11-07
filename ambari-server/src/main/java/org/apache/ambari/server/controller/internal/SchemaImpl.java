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

import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple schema implementation.
 */
public class SchemaImpl implements Schema {
  /**
   * The associated resource provider.
   */
  private final ResourceProvider resourceProvider;

  /**
   * The list of associated property providers.
   */
  private final List<PropertyProvider> propertyProviders;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a new schema for the given providers.
   *
   * @param resourceProvider   the resource provider
   * @param propertyProviders  the property providers
   */
  public SchemaImpl(ResourceProvider resourceProvider,
                    List<PropertyProvider> propertyProviders) {
    this.resourceProvider = resourceProvider;
    this.propertyProviders = propertyProviders;
  }


  // ----- Schema ------------------------------------------------------------

  @Override
  public PropertyId getKeyPropertyId(Resource.Type type) {
    return resourceProvider.getKeyPropertyIds().get(type);
  }

  @Override
  public Map<String, Set<String>> getCategories() {
    Map<String, Set<String>> categories = new HashMap<String, Set<String>>();

    for (PropertyId propertyId : getPropertyIds()) {
      final String category = propertyId.getCategory();
      Set<String> properties = categories.get(category);
      if (properties == null) {
        properties = new HashSet<String>();
        categories.put(category, properties);
      }
      properties.add(propertyId.getName());
    }
    return categories;
  }


  // ----- helper methods ----------------------------------------------------

  private Set<PropertyId> getPropertyIds() {
    Set<PropertyId> propertyIds = new HashSet<PropertyId>(resourceProvider.getPropertyIds());
    if (propertyProviders != null) {
      for (PropertyProvider propertyProvider : propertyProviders) {
        propertyIds.addAll(propertyProvider.getPropertyIds());
      }
    }
    return propertyIds;
  }
}
