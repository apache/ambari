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
package org.apache.ambari.server.registry.json;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.registry.RegistryScenario;
import org.apache.ambari.server.registry.RegistryScenarioMpack;

import com.google.gson.annotations.SerializedName;


/**
 * JSON implementation of a {@link RegistryScenario}
 */
public class JsonRegistryScenario implements RegistryScenario {
  @SerializedName("name")
  private String name;

  @SerializedName("description")
  private String description;

  @SerializedName("scenarioMpacks")
  private ArrayList<JsonRegistryScenarioMpack> scenarioMpacks;

  @Override
  public String getScenarioName() {
    return name;
  }

  @Override
  public String getScenarioDescription() {
    return description;
  }

  @Override
  public List<? extends RegistryScenarioMpack> getScenarioMpacks() {
    return scenarioMpacks;
  }
}
