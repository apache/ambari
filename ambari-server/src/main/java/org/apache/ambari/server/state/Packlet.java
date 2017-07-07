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

import com.google.gson.annotations.SerializedName;


public class Packlet {
  @SerializedName("type")
  private String type;
  @SerializedName("name")
  private String name;
  @SerializedName("version")
  private String version;
  @SerializedName("service-id")
  private String serviceId;
  @SerializedName("source_dir")
  private String sourceDir;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  public String getSourceDir() {
    return sourceDir;
  }

  public void setSourceDir(String sourceDir) {
    this.sourceDir = sourceDir;
  }


  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
    result = prime * result + ((sourceDir == null) ? 0 : sourceDir.hashCode());
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

    Packlet other = (Packlet) obj;

    if (type != other.type) {
      return false;
    }

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }

    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }

    if (serviceId == null) {
      if (other.serviceId != null) {
        return false;
      }
    } else if (!serviceId.equals(other.serviceId)) {
      return false;
    }

    if (sourceDir == null) {
      if (other.sourceDir != null) {
        return false;
      }
    } else if (!sourceDir.equals(other.sourceDir)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("type=").append(type).append(", ");
    sb.append("name=").append(name).append(", ");
    sb.append("version=").append(version).append(", ");
    sb.append("service id=").append(serviceId).append(", ");
    sb.append("source directory=").append(sourceDir).append(", ");
    sb.append('}');
    return sb.toString();
  }

}
