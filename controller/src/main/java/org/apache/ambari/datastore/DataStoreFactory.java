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
package org.apache.ambari.datastore;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DataStoreFactory {

  private final DataStore ds;
  
  @Inject
  DataStoreFactory(@Named("data.store") String dataStore) throws IOException {
    URI uri;
    try {
      uri = new URI(dataStore);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Bad data store URI: " + dataStore, e);
    }
    String scheme = uri.getScheme();
    if ("zk".equals(scheme)) {
      String auth = uri.getAuthority();
      ds = new ZookeeperDS(auth);
    } else if ("test".equals(scheme)) {
      ds = new StaticDataStore();
    } else {
      throw new IllegalArgumentException("Unknown data store " + scheme);
    }
  }
  
  public DataStore getInstance() {
    return ds;
  }
}
