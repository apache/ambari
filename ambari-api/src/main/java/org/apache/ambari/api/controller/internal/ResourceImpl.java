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

package org.apache.ambari.api.controller.internal;

import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * Default resource implementation.
 */
public class ResourceImpl implements Resource {
  private final Type type;
  private final Map<String, Map<String, String>> categories = new HashMap<String, Map<String, String>>();

  public ResourceImpl(Type type) {
    this.type = type;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Map<String, Map<String, String>> getCategories() {
    return categories;
  }

  @Override
  public void setProperty(PropertyId id, String value) {
    String category = id.getCategory();

    Map<String, String> properties = categories.get(category);

    if (properties == null) {
      properties = new HashMap<String, String>();
      categories.put(category, properties);
    }

    properties.put(id.getName(), value);
  }

  @Override
  public void setProperty(PropertyId id, Integer value) {
    setProperty(id, value.toString());
  }

  @Override
  public void setProperty(PropertyId id, Float value) {
    setProperty(id, value.toString());
  }

  @Override
  public void setProperty(PropertyId id, Double value) {
    setProperty(id, value.toString());
  }

  @Override
  public void setProperty(PropertyId id, Long value) {
    setProperty(id, value.toString());
  }

  @Override
  public String getPropertyValue(PropertyId id) {

    Map<String, String> properties = categories.get(id.getCategory());

    if (properties != null) {
      return properties.get(id.getName());
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Resource : ").append(type).append("\n");
    for (Map.Entry<String, Map<String, String>> catEntry : categories.entrySet()) {
      for (Map.Entry<String, String> propEntry : catEntry.getValue().entrySet()) {
        sb.append("    ").append(catEntry.getKey()).append(".").append(propEntry.getKey()).append(" : ").append(propEntry.getValue()).append("\n");
      }
    }
    return sb.toString();
  }
}
