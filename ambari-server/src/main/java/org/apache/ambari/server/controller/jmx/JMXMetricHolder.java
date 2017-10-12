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

package org.apache.ambari.server.controller.jmx;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 *
 */
public final class JMXMetricHolder {
  private static final String NAME_KEY = "name";

  private List<Map<String, Object>> beans;

  public List<Map<String, Object>> getBeans() {
    return beans;
  }

  public void setBeans(List<Map<String, Object>> beans) {
    this.beans = beans;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();

    for (Map<String, Object> map : beans) {
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        stringBuilder.append("    ").append(entry).append("\n");
      }
    }
    return stringBuilder.toString();
  }

  public List<Object> findAll(List<String> properties) {
    return properties.stream()
      .map(this::find)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());
  }

  public Optional<Object> find(String property) {
    String propertyName = property.split("/")[0];
    String propertyValue = property.split("/")[1];
    return beans.stream()
      .filter(each -> propertyName.equals(name(each)))
      .map(each -> each.get(propertyValue))
      .filter(Objects::nonNull)
      .findFirst();
  }

  private String name(Map<String, Object> bean) {
    return bean.containsKey(NAME_KEY) ? (String) bean.get(NAME_KEY) : null;
  }
}
