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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the state of an mpack.
 */
public class Mpack {

  private Long mpackId;

  private Long registryId;

  @SerializedName("name")
  private String name;

  @SerializedName("version")
  private String version;

  @SerializedName("description")
  private String description;

  @SerializedName("prerequisites")
  private HashMap<String, String> prerequisites;

  @SerializedName("packlets")
  private ArrayList<Packlet> packlets;

  @SerializedName("stack-id")
  private String stackId;

  private String mpackUri;

  public Long getMpackId() {
    return mpackId;
  }

  public void setMpackId(Long mpackId) {
    this.mpackId = mpackId;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public HashMap<String, String> getPrerequisites() {
    return prerequisites;
  }

  public void setPrerequisites(HashMap<String, String> prerequisites) {
    this.prerequisites = prerequisites;
  }

  public ArrayList<Packlet> getPacklets() {
    return packlets;
  }

  public void setPacklets(ArrayList<Packlet> packlets) {
    this.packlets = packlets;
  }


  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mpackId == null) ? 0 : mpackId.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((registryId == null) ? 0 : registryId.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((prerequisites == null) ? 0 : prerequisites.hashCode());
    result = prime * result + ((packlets == null) ? 0 : packlets.hashCode());
    result = prime * result + ((stackId == null) ? 0 : stackId.hashCode());
    result = prime * result + ((mpackUri == null) ? 0 : mpackUri.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    Mpack other = (Mpack) obj;

    if (name != other.name) {
      return false;
    }

    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }

    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }

    if (mpackId == null) {
      if (other.mpackId != null) {
        return false;
      }
    } else if (!mpackId.equals(other.mpackId)) {
      return false;
    }

    if (registryId == null) {
      if (other.registryId != null) {
        return false;
      }
    } else if (!registryId.equals(other.registryId)) {
      return false;
    }

    if (prerequisites == null) {
      if (other.prerequisites != null) {
        return false;
      }
    } else if (!prerequisites.equals(other.prerequisites)) {
      return false;
    }

    if (packlets == null) {
      if (other.packlets != null) {
        return false;
      }
    } else if (!packlets.equals(other.packlets)) {
      return false;
    }

    if (mpackUri == null) {
      if (other.mpackUri != null) {
        return false;
      }
    } else if (!mpackUri.equals(other.mpackUri)) {
      return false;
    }

    if (stackId == null) {
      if (other.stackId != null) {
        return false;
      }
    } else if (!stackId.equals(other.stackId)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("name=").append(name).append(", ");
    sb.append("mpackId=").append(mpackId).append(", ");
    sb.append("version=").append(version).append(", ");
    sb.append("stackid=").append(stackId).append(", ");
    sb.append("registryId=").append(registryId).append(", ");
    sb.append("description=").append(description).append(", ");
    sb.append("prereq=").append(prerequisites.toString()).append(", ");
    sb.append("packlets=").append(packlets.toString()).append(", ");
        sb.append('}');
    return sb.toString();
  }

  public void copyFrom(Mpack mpack) {
    if (this.name == null)
      this.name = mpack.getName();
    if (this.mpackId == null)
      this.mpackId = mpack.getMpackId();
    if (this.version == null)
      this.version = mpack.getVersion();
    if (this.stackId == null)
      this.stackId = mpack.getStackId();
    if (this.registryId == null)
      this.registryId = mpack.getRegistryId();
    if (this.description == null)
      this.description = mpack.getDescription();
    if (this.prerequisites == null)
      this.prerequisites = mpack.getPrerequisites();
    if (this.packlets == null)
      this.packlets = mpack.getPacklets();

  }
}
