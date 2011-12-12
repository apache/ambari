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

import org.apache.ambari.configuration.Configuration;

import com.google.inject.Inject;

public class DataStoreFactory {

  private final DataStore ds;
  
  @Inject
  DataStoreFactory(Configuration conf) throws IOException {
    URI uri = conf.getDataStore();
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
