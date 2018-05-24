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

package org.apache.ambari.server.api.services.mpackadvisor;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Abstract stack advisor response POJO.
 */

public abstract class MpackAdvisorResponse {

  private int id;

  @JsonProperty("Versions")
  private Version version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Version getVersion() {
    return version;
  }

  public void setVersion(Version version) {
    this.version = version;
  }

  public static class Version {
    @JsonProperty("stack_name")
    private String stackName;

    @JsonProperty("stack_version")
    private String stackVersion;

    public String getStackName() {
      return stackName;
    }

    public void setStackName(String stackName) {
      this.stackName = stackName;
    }

    public String getStackVersion() {
      return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
    }
  }

}
