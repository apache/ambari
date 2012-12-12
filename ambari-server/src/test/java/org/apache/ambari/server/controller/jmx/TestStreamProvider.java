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

package org.apache.ambari.server.controller.jmx;

import org.apache.ambari.server.controller.utilities.StreamProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TestStreamProvider implements StreamProvider {

  protected static Map<String, String> FILE_MAPPING = new HashMap<String, String>();

  static {
    FILE_MAPPING.put("50070", "hdfs_namenode_jmx.json");
    FILE_MAPPING.put("50075", "hdfs_datanode_jmx.json");
    FILE_MAPPING.put("50030", "mapreduce_jobtracker_jmx.json");
    FILE_MAPPING.put("50060", "mapreduce_tasktracker_jmx.json");
    FILE_MAPPING.put("60010", "hdfs_namenode_jmx.json");
  }

  private String lastSpec;

  @Override
  public InputStream readFrom(String spec) throws IOException {
    lastSpec = spec;
    String filename = FILE_MAPPING.get(getPort(spec));
    if (filename == null) {
      throw new IOException("Can't find JMX source for " + spec);
    }
    return ClassLoader.getSystemResourceAsStream(filename);
  }

  public String getLastSpec() {
    return lastSpec;
  }

  private String getPort(String spec) {
    int n = spec.indexOf(":", 5);
    return spec.substring(n + 1, n + 6);
  }


}
