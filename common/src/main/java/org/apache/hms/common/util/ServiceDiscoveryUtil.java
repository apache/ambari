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

package org.apache.hms.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServiceDiscoveryUtil implements ServiceListener {
  private static Log LOG = LogFactory.getLog(ServiceDiscoveryUtil.class);
  public static JmDNS jmdns;
  private InetAddress addr;
  private Map<String, String> map;
  private String type;
  
  /** Number of milliseconds to wait for service info resolution before giving up */
  private final static int SERVICE_RESOLUTION_TIMEOUT = 10000;
  
  public ServiceDiscoveryUtil() throws IOException {
    map = new HashMap<String, String>();
    type = "_zookeeper._tcp.local.";
  }
  
  public ServiceDiscoveryUtil(String type) throws IOException {
    this();
    this.type = type;
  }
  
  public ServiceDiscoveryUtil(InetAddress addr, String type) throws IOException {
    this();
    this.addr = addr;
    this.type = type;
  }
  
  public void start() throws IOException {
    if(addr!=null) {
      jmdns = JmDNS.create(this.addr);      
    } else {
      jmdns = JmDNS.create();
    }
    jmdns.addServiceListener(type, this);    
  }
  
  /**
   * Add a service.
   */
  public void serviceAdded(final ServiceEvent event) {
    String name = event.getName();
    LOG.info("Add: " + name + " Type: " + event.getType());
    new Thread() {
      public void run() {
        jmdns.requestServiceInfo(event.getType(), event.getName(), SERVICE_RESOLUTION_TIMEOUT);
      }
    }.start();
  }

  /**
   * Remove a service.
   */
  public void serviceRemoved(ServiceEvent event) {
    String name = event.getName();
    map.remove(name);
    LOG.info("Remove: " + name + " Type: " + event.getType());
  }
  
  /**
   * Resolve a service.
   */
  public synchronized void serviceResolved(ServiceEvent event) {
    String name = event.getName();
    String type = event.getType();
    if(type.equals(this.type)) {
      ServiceInfo info = event.getInfo();
      StringBuffer buf = new StringBuffer();
      String delimiter = "";
      for(String addr : info.getHostAddresses()) {
        buf.append(delimiter);
        buf.append(addr);
        buf.append(':');
        buf.append(info.getPort());
        delimiter = ",";
      }
      LOG.debug("Resolved: " + buf.toString());
      map.put(name, buf.toString());
    }
  }
  
  public Collection<String> resolve() {
    return (Collection<String>) map.values();
  }
  
  public void close() throws IOException {
    jmdns.close();
  }
}
