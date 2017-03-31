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

package org.apache.ambari.view.hive20.internal.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

/**
 *
 */
public class ColumnInfo {
  private String name;
  private String type;
  private Integer precision;
  private Integer scale;
  private String comment;

  private ColumnInfo(){
    // for json de-serialization
  }

  public ColumnInfo(String name, String type, Integer precision, Integer scale, String comment) {
    this.name = name;
    this.type = type;
    this.precision = precision;
    this.scale = scale;
    this.comment = comment;
  }

  public ColumnInfo(String name, String type, String comment) {
    this(name, type, null, null, comment);
  }

  public ColumnInfo(String name, String type, Integer precision, String comment) {
    this(name, type, precision, null, comment);
  }

  public ColumnInfo(String name, String type, Integer precision, Integer scale) {
    this(name, type, precision, scale, null);
  }

  public ColumnInfo(String name, String type, Integer precision) {
    this(name, type, precision, null, null);
  }

  public ColumnInfo(String name, String type) {
    this(name, type, null, null, null);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public Integer getPrecision() {
    return precision;
  }

  public void setPrecision(Integer precision) {
    this.precision = precision;
  }

  public Integer getScale() {
    return scale;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public String getComment() {
    return comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnInfo that = (ColumnInfo) o;
    return ((name == that.name) || (name != null && name.equalsIgnoreCase(that.name))) &&
        ((type == that.type) || (type != null && type.equalsIgnoreCase(that.type))) &&
        Objects.equals(precision, that.precision) &&
        Objects.equals(scale, that.scale) &&
        Objects.equals(comment, that.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ColumnInfo{");
    sb.append("name='").append(name).append('\'');
    sb.append(", type='").append(type).append('\'');
    sb.append(", precision=").append(precision);
    sb.append(", scale=").append(scale);
    sb.append(", comment='").append(comment).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
