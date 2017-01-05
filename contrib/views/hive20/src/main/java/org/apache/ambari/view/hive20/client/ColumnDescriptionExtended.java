/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.client;

public class ColumnDescriptionExtended implements ColumnDescription {
  private String name;
  private String type;
  private int position;
  private String comment;
  private boolean partitioned;
  private boolean sortedBy;
  private boolean clusteredBy;

  public ColumnDescriptionExtended(String name, String type, String comment, boolean partitioned,
                                   boolean sortedBy, boolean clusteredBy, int position) {
    this.name = name;
    this.type = type;
    this.comment = comment;
    this.partitioned = partitioned;
    this.sortedBy = sortedBy;
    this.clusteredBy = clusteredBy;
    this.position = position;
  }

  public ColumnDescription createShortColumnDescription() {
    return new ColumnDescriptionShort(getName(), getType(), getPosition());
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public int getPosition() {
    return position;
  }

  public String getComment() {
    return comment;
  }

  public boolean isPartitioned() {
    return partitioned;
  }

  public boolean isSortedBy() {
    return sortedBy;
  }

  public boolean isClusteredBy() {
    return clusteredBy;
  }


}
