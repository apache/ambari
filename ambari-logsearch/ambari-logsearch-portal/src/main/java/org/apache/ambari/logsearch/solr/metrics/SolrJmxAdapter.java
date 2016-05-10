/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.solr.metrics;

import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.management.OperatingSystemMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class SolrJmxAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(SolrJmxAdapter.class);

  private static final String JMX_SERVICE_URL = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";

  private final JMXServiceURL jmxServiceUrl;

  private JMXConnector jmxConnector;
  private MBeanServerConnection conn;

  public SolrJmxAdapter(String host, int port) throws MalformedURLException {
    String url = String.format(JMX_SERVICE_URL, host, port);
    jmxServiceUrl = new JMXServiceURL(url);
  }

  public double getProcessCpuLoad() throws MalformedObjectNameException {
    ObjectName objectName = new ObjectName("java.lang:type=OperatingSystem");
    OperatingSystemMXBean mxBean = JMX.newMXBeanProxy(conn, objectName, OperatingSystemMXBean.class);
    return mxBean.getProcessCpuLoad();
  }

  public Map<String, Long> getMemoryData() throws MalformedObjectNameException {
    Map<String, Long> memoryData = new HashMap<>();
    
    ObjectName objectName = new ObjectName("java.lang:type=Memory");
    MemoryMXBean mxBean = JMX.newMXBeanProxy(conn, objectName, MemoryMXBean.class);
    
    memoryData.put("heapMemoryUsed", mxBean.getHeapMemoryUsage().getUsed());
    memoryData.put("heapMemoryCommitted", mxBean.getHeapMemoryUsage().getCommitted());
    memoryData.put("heapMemoryMax", mxBean.getHeapMemoryUsage().getMax());
    memoryData.put("nonHeapMemoryUsed", mxBean.getNonHeapMemoryUsage().getUsed());
    memoryData.put("nonHeapMemoryCommitted", mxBean.getNonHeapMemoryUsage().getCommitted());
    memoryData.put("nonHeapMemoryMax", mxBean.getNonHeapMemoryUsage().getMax());
    
    return memoryData;
  }

  public long getIndexSize() throws Exception {
    long indexSize = 0;

    ObjectName objectNamePattern = new ObjectName(
        "solr/*shard*replica*:type=/replication,id=org.apache.solr.handler.ReplicationHandler");
    Set<ObjectName> objectNames = conn.queryNames(objectNamePattern, null);
    for (ObjectName objectName : objectNames) {
      String indexSizeString = (String) conn.getAttribute(objectName, "indexSize");
      indexSize += getIndexSizeInBytes(indexSizeString);
    }

    return indexSize;
  }

  private long getIndexSizeInBytes(String indexSizeString) {
    String[] tokens = indexSizeString.split(" ");
    double number = Double.parseDouble(tokens[0]);

    long multiplier = 0;
    switch (tokens[1]) {
      case "bytes":
        multiplier = 1;
        break;
      case "KB":
        multiplier = 1024;
        break;
      case "MB":
        multiplier = 1024 * 1024;
        break;
      case "GB":
        multiplier = 1024 * 1024 * 1024;
        break;
      default:
        throw new IllegalArgumentException("Unknown unit: " + tokens[1]);
    }
    
    return (long)(number * multiplier);
  }

  public void reConnect() throws IOException {
    if (jmxConnector != null) {
      try {
        jmxConnector.close();
      } catch (IOException e) {
        LOG.info("Could not close jmxConnector", e);
      }
    }

    connect();
  }

  public void connect() throws IOException {
    jmxConnector = JMXConnectorFactory.connect(jmxServiceUrl);
    conn = jmxConnector.getMBeanServerConnection();
  }
}
