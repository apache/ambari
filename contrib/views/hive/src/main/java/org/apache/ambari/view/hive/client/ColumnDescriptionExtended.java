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

package org.apache.ambari.view.hive.client;

public class ColumnDescriptionExtended implements ColumnDescription {
  private String name;
  private String type;
  private int position;
  private String comment;
  private boolean partitioned;
  private boolean sortedBy;
  private boolean clusteredBy;

  private ColumnDescriptionExtended(String name, String type, String comment, boolean partitioned,
                                   boolean sortedBy, boolean clusteredBy, int position) {
    setName(name);
    setType(type);
    setPosition(position);
    setComment(comment);
    setPartitioned(partitioned);
    setSortedBy(sortedBy);
    setClusteredBy(clusteredBy);
  }

  public static ColumnDescription createExtendedColumnDescription(String name, String type, String comment,
                                                                  boolean partitioned, boolean sortedBy, boolean clusteredBy,
                                                                  int position) {
    return new ColumnDescriptionExtended(name, type, comment, partitioned, sortedBy, clusteredBy, position);
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

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public boolean isPartitioned() {
    return partitioned;
  }

  public void setPartitioned(boolean partitioned) {
    this.partitioned = partitioned;
  }

  public boolean isSortedBy() {
    return sortedBy;
  }

  public void setSortedBy(boolean sortedBy) {
    this.sortedBy = sortedBy;
  }

  public boolean isClusteredBy() {
    return clusteredBy;
  }

  public void setClusteredBy(boolean clusteredBy) {
    this.clusteredBy = clusteredBy;
  }

}
