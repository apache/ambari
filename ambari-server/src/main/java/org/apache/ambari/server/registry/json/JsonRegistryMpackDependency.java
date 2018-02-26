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
package org.apache.ambari.server.registry.json;

import org.apache.ambari.server.registry.RegistryMpackDependency;

import com.google.gson.annotations.SerializedName;

/**
 * JSON implementation of {@link RegistryMpackDependency}
 */
public class JsonRegistryMpackDependency implements RegistryMpackDependency {

  @SerializedName("id")
  private String id;

  @SerializedName("name")
  private String name;

  @SerializedName("minVersion")
  private String minVersion;

  @SerializedName("maxVersion")
  private String maxVersion;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getMinVersion() {
    return minVersion;
  }

  @Override
  public String getMaxVersion() {
    return maxVersion;
  }
}
