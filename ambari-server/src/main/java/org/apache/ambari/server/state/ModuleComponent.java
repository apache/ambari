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
package org.apache.ambari.server.state;

import java.util.Objects;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;

public class ModuleComponent {

  @SerializedName("id")
  private String id;
  @SerializedName("name")
  private String name;
  @SerializedName("category")
  private String category;
  @SerializedName("isExternal")
  private Boolean isExternal;
  @SerializedName("version")
  private String version;

  /**
   * The owning module for this module component.
   */
  @JsonIgnore
  private transient Module module;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  public Boolean getIsExternal() {
    return isExternal;
  }

  public void setIsExternal(Boolean isExternal) {
    this.isExternal = isExternal;
  }

  @JsonIgnore
  public Module getModule() {
    return module;
  }

  public void setModule(Module module) {
    this.module = module;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ModuleComponent that = (ModuleComponent) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (category != null ? !category.equals(that.category) : that.category != null) {
      return false;
    }
    if (isExternal != null ? !isExternal.equals(that.isExternal) : that.isExternal != null) {
      return false;
    }
    return version != null ? version.equals(that.version) : that.version == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(id, name, category, isExternal, version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
    .add("id", id)
    .add("name", name)
    .add("category", category)
    .add("isExternal", isExternal)
    .add("version", version)
    .toString();
  }
}
