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
    FILE_MAPPING.put("60010", "hbase_hbasemaster_jmx.json");
    FILE_MAPPING.put("60011", "hbase_hbasemaster_jmx_2.json");
    FILE_MAPPING.put("8088",  "resourcemanager_jmx.json");
    FILE_MAPPING.put("8480",  "hdfs_journalnode_jmx.json");
  }

  /**
   * Delay to simulate response time.
   */
  protected final long delay;

  private String lastSpec;

  private boolean isLastSpecUpdated;

  public TestStreamProvider() {
    delay = 0;
  }

  public TestStreamProvider(long delay) {
    this.delay = delay;
  }

  @Override
  public InputStream readFrom(String spec) throws IOException {
    if (!isLastSpecUpdated)
      lastSpec = spec;
    
    isLastSpecUpdated = false;
    String filename = FILE_MAPPING.get(getPort(spec));
    if (filename == null) {
      throw new IOException("Can't find JMX source for " + spec);
    }
    if (delay > 0) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        // do nothing
      }
    }

    return ClassLoader.getSystemResourceAsStream(filename);
  }

  public String getLastSpec() {
    return lastSpec;
  }

  private String getPort(String spec) {
    int colonIndex = spec.indexOf(":", 5);
    int slashIndex = spec.indexOf("/", colonIndex);

    return spec.substring(colonIndex + 1, slashIndex);
  }

  @Override
  public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
    lastSpec = spec + "?" + params;
    isLastSpecUpdated = true;
    return readFrom(spec);
  }
}
