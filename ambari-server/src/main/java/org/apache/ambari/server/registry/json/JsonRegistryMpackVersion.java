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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.registry.RegistryMpackDependency;
import org.apache.ambari.server.registry.RegistryMpackVersion;
import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.Mpack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * JSON implemenation of {@link RegistryMpackVersion}
 */
public class JsonRegistryMpackVersion implements RegistryMpackVersion {
  @SerializedName("version")
  private String version;

  @SerializedName("mpackUri")
  private String mpackUri;

  @SerializedName("docUri")
  private String docUri;

  @SerializedName("dependencies")
  private ArrayList<JsonRegistryMpackDependency> dependencies;

  private Mpack mpack = null;

  @Override
  public String getMpackVersion() {
    return version;
  }

  @Override
  public String getMpackUri() {
    return mpackUri;
  }

  @Override
  public String getMpackDocUri() {
    return docUri;
  }

  @Override
  public List<? extends RegistryMpackDependency> getDependencies() {
    return dependencies;
  }

  private final static Logger LOG = LoggerFactory.getLogger(JsonRegistryMpackVersion.class);


  @Override
  public List<Module> getModules() {

    if(mpack == null) {
      try {
        URL url = new URL(mpackUri);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        Gson gson = new Gson();
        mpack = gson.fromJson(reader, Mpack.class);
      } catch (MalformedURLException e) {
        LOG.warn("Failed to get list of modules, malformed mpack uri {}", mpackUri);
      }
      catch (IOException e) {
        LOG.warn("Failed to get list of modules, mpack uri {} cannot be read", mpackUri);
      }
    }
    return mpack == null? null: mpack.getModules();
  }
}
