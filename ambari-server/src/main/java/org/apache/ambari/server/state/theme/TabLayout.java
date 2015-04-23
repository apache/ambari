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



import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabLayout {
	@JsonProperty("tab-rows")
	private String tabRows;
	@JsonProperty("sections")
	private List<Section> sections;
	@JsonProperty("tab-columns")
	private String tabColumns;

  public String getTabRows() {
    return tabRows;
  }

  public void setTabRows(String tabRows) {
    this.tabRows = tabRows;
  }

  public List<Section> getSections() {
    return sections;
  }

  public void setSections(List<Section> sections) {
    this.sections = sections;
  }

  public String getTabColumns() {
    return tabColumns;
  }

  public void setTabColumns(String tabColumns) {
    this.tabColumns = tabColumns;
  }

  public void mergeWithParent(TabLayout parent) {
    if (tabColumns == null) {
      tabColumns = parent.tabColumns;
    }
    
    if (tabRows == null) {
      tabRows = parent.tabRows;
    }

    if (sections == null) {
      sections = parent.sections;
    }else if (parent.sections != null) {
      sections = mergedSections(parent.sections, sections);
    }
  }

  private List<Section> mergedSections(List<Section> parentSections, List<Section> childSections) {
    Map<String, Section> mergedSections = new HashMap<String, Section>();
    for (Section parentSection : parentSections) {
      mergedSections.put(parentSection.getName(), parentSection);
    }

    for (Section childSection : childSections) {
      if (childSection.getName() != null) {
        if (childSection.isRemoved()) {
          mergedSections.remove(childSection.getName());
        } else {
          Section parentSection = mergedSections.get(childSection.getName());
          childSection.mergeWithParent(parentSection);
          mergedSections.put(childSection.getName(), childSection);
        }
      }
    }
    return new ArrayList<Section>(mergedSections.values());

  }
}