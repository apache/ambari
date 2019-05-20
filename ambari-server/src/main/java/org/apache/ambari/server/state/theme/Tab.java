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
public class Tab {
	@JsonProperty("display-name")
	private String displayName;
	@JsonProperty("name")
	private String name;
	@JsonProperty("layout")
	private TabLayout tabLayout;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TabLayout getTabLayout() {
    return tabLayout;
  }

  public void setTabLayout(TabLayout tabLayout) {
    this.tabLayout = tabLayout;
  }

  public void mergeWithParent(Tab parentTab) {
    // null name is not expected due to theme structure

    if (displayName == null) {
      displayName = parentTab.displayName;
    }
    if (tabLayout == null) {
      tabLayout = parentTab.tabLayout;
    } else {
      tabLayout.mergeWithParent(parentTab.tabLayout);
    }
  }
}