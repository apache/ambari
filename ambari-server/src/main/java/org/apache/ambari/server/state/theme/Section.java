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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section {
	@JsonProperty("subsections")
	private List<Subsection> subsections;
	@JsonProperty("display-name")
	private String displayName;
	@JsonProperty("row-index")
	private String rowIndex;
	@JsonProperty("section-rows")
	private String sectionRows;
	@JsonProperty("name")
	private String name;
	@JsonProperty("column-span")
	private String columnSpan;
	@JsonProperty("section-columns")
	private String sectionColumns;
	@JsonProperty("column-index")
	private String columnIndex;
	@JsonProperty("row-span")
	private String rowSpan;

  public List<Subsection> getSubsections() {
    return subsections;
  }

  public void setSubsections(List<Subsection> subsections) {
    this.subsections = subsections;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRowIndex() {
    return rowIndex;
  }

  public void setRowIndex(String rowIndex) {
    this.rowIndex = rowIndex;
  }

  public String getSectionRows() {
    return sectionRows;
  }

  public void setSectionRows(String sectionRows) {
    this.sectionRows = sectionRows;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getColumnSpan() {
    return columnSpan;
  }

  public void setColumnSpan(String columnSpan) {
    this.columnSpan = columnSpan;
  }

  public String getSectionColumns() {
    return sectionColumns;
  }

  public void setSectionColumns(String sectionColumns) {
    this.sectionColumns = sectionColumns;
  }

  public String getColumnIndex() {
    return columnIndex;
  }

  public void setColumnIndex(String columnIndex) {
    this.columnIndex = columnIndex;
  }

  public String getRowSpan() {
    return rowSpan;
  }

  public void setRowSpan(String rowSpan) {
    this.rowSpan = rowSpan;
  }

  public boolean isRemoved() {
    return columnIndex == null && columnSpan == null &&
      subsections == null && displayName == null &&
      rowIndex == null && rowSpan == null &&
      sectionRows == null && sectionColumns == null;
  }

  public void mergeWithParent(Section parentSection) {
    if (displayName == null) {
      displayName = parentSection.displayName;
    }
    if (rowIndex == null) {
      rowIndex = parentSection.rowIndex;
    }
    if (rowSpan == null) {
      rowSpan = parentSection.rowSpan;
    }
    if (sectionRows == null) {
      sectionRows = parentSection.sectionRows;
    }
    if (columnIndex == null) {
      columnIndex = parentSection.columnIndex;
    }
    if (columnSpan == null) {
      columnSpan = parentSection.columnSpan;
    }
    if (sectionColumns == null) {
      sectionColumns = parentSection.sectionColumns;
    }
    if (subsections == null) {
      subsections = parentSection.subsections;
    }else if (parentSection.subsections != null) {
      subsections = mergeSubsections(parentSection.subsections, subsections);
    }
  }

  private List<Subsection> mergeSubsections(List<Subsection> parentSubsections, List<Subsection> childSubsections) {

    Map<String, Subsection> mergedSubsections = new HashMap<String, Subsection>();
    for (Subsection parentSubsection : parentSubsections) {
      mergedSubsections.put(parentSubsection.getName(), parentSubsection);
    }

    for (Subsection childSubsection : childSubsections) {
      if (childSubsection.getName() != null) {
        if (childSubsection.isRemoved()) {
          mergedSubsections.remove(childSubsection.getName());
        } else {
          Subsection parentSection = mergedSubsections.get(childSubsection.getName());
          childSubsection.mergeWithParent(parentSection);
          mergedSubsections.put(childSubsection.getName(), childSubsection);
        }
      }
    }
    return new ArrayList<Subsection>(mergedSubsections.values());

  }
}