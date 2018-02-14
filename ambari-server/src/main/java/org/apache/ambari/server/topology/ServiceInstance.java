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

package org.apache.ambari.server.topology;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ServiceInstance implements Configurable {
  private String name;
  private String type;
  private Configuration configuration = new Configuration();
  private MpackInstance mpackInstance;

  public ServiceInstance() { }

  public ServiceInstance(String name, String type, Configuration configuration) {
    this.name = name;
    this.type = type;
    this.configuration = configuration;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  @JsonIgnore
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  @JsonIgnore
  public MpackInstance getMpackInstance() {
    return mpackInstance;
  }

  @JsonIgnore
  void setMpackInstance(MpackInstance mpackInstance) {
    this.mpackInstance = mpackInstance;
  }
}
