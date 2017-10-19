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
package org.apache.ambari.server.registry.json;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.exceptions.RegistryMpackVersionNotFoundException;
import org.apache.ambari.server.registry.RegistryMpack;
import org.apache.ambari.server.registry.RegistryMpackVersion;

import com.google.gson.annotations.SerializedName;

/**
 * JSON implementation of {@link RegistryMpack}
 */
public class JsonRegistryMpack implements RegistryMpack {

  @SerializedName("name")
  private String name;

  @SerializedName("displayName")
  private String displayName;

  @SerializedName("description")
  private String description;

  @SerializedName("logoUrl")
  private String logoUrl;

  @SerializedName("mpackVersions")
  private ArrayList<JsonRegistryMpackVersion> mpackVersions;

  @Override
  public String getMpackName() {
    return name;
  }

  @Override
  public String getMpackDisplayName() {return displayName;}

  @Override
  public String getMpackDescription() {
    return description;
  }

  @Override
  public String getMpackLogoUrl() {
    return logoUrl;
  }

  @Override
  public List<? extends RegistryMpackVersion> getMpackVersions() {
    return mpackVersions;
  }

  @Override
  public RegistryMpackVersion getMpackVersion(String mpackVersion)
    throws AmbariException {
    RegistryMpackVersion registryMpackVersion = null;
    if(mpackVersion == null || mpackVersion.isEmpty()) {
      throw new AmbariException(String.format("Registry mpack version cannot be null"));
    }
    for(RegistryMpackVersion rmv : getMpackVersions()) {
      if(rmv.getMpackVersion().equals(mpackVersion)) {
        registryMpackVersion = rmv;
      }
    }
    if(registryMpackVersion == null) {
      throw new RegistryMpackVersionNotFoundException(getMpackName(), mpackVersion);
    }
    return registryMpackVersion;
  }
}
