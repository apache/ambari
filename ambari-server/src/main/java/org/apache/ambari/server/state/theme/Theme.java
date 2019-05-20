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

package org.apache.ambari.server.state.theme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Theme{
	@JsonProperty("description")
	private String description;
	@JsonProperty("name")
	private String name;
  @JsonProperty("configuration")
	private ThemeConfiguration themeConfiguration;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ThemeConfiguration getThemeConfiguration() {
    return themeConfiguration;
  }

  public void setThemeConfiguration(ThemeConfiguration themeConfiguration) {
    this.themeConfiguration = themeConfiguration;
  }

  public void mergeWithParent(Theme parent) {
    if (parent == null) {
      return;
    }

    if (name == null) {
      name = parent.name;
    }

    if (description == null) {
      description = parent.description;
    }

    if (themeConfiguration == null) {
      themeConfiguration = parent.themeConfiguration;
    } else {
      themeConfiguration.mergeWithParent(parent.themeConfiguration);
    }
  }
}