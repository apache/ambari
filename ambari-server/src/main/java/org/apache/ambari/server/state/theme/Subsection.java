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


@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subsection {
	@JsonProperty("row-index")
	private String rowIndex;
	@JsonProperty("name")
	private String name;
  @JsonProperty("display-name")
	private String displayName;
	@JsonProperty("column-span")
	private String columnSpan;
	@JsonProperty("row-span")
	private String rowSpan;
	@JsonProperty("column-index")
	private String columnIndex;
  @JsonProperty("border")
	private String border;


  public String getRowIndex() {
    return rowIndex;
  }

  public void setRowIndex(String rowIndex) {
    this.rowIndex = rowIndex;
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

  public String getRowSpan() {
    return rowSpan;
  }

  public void setRowSpan(String rowSpan) {
    this.rowSpan = rowSpan;
  }

  public String getColumnIndex() {
    return columnIndex;
  }

  public void setColumnIndex(String columnIndex) {
    this.columnIndex = columnIndex;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getBorder() {
    return border;
  }

  public void setBorder(String border) {
    this.border = border;
  }

  public boolean isRemoved() {
    return rowIndex == null && rowSpan == null && columnIndex == null && columnSpan == null;
  }

  public void mergeWithParent(Subsection parent) {
    if (rowSpan == null) {
      rowSpan = parent.rowSpan;
    }
    if (rowIndex == null) {
      rowIndex = parent.rowIndex;
    }
    if (columnSpan == null) {
      columnSpan = parent.columnSpan;
    }
    if (columnIndex == null) {
      columnIndex = parent.columnIndex;
    }
    if (displayName == null) {
      displayName = parent.displayName;
    }
    if (border == null) {
      border = parent.border;
    }
  }
}