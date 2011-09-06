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
import java.net.UnknownHostException;
import java.util.Hashtable;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MulticastDNS {
  private static Log LOG = LogFactory.getLog(MulticastDNS.class);
  public static JmDNS jmdns;
  private InetAddress addr;
  private String svcType = "_zookeeper._tcp.local.";
  private String svcName;
  private int svcPort = 2181;
  private Hashtable<String, String> settings;
 
  public MulticastDNS() throws UnknownHostException {
    super();
    InetAddress addr = InetAddress.getLocalHost();
    String hostname = addr.getHostName();
    if(hostname.indexOf('.')>0) {
      hostname = hostname.substring(0, hostname.indexOf('.'));
    }
    svcName = hostname;
    settings = new Hashtable<String,String>();
    settings.put("host", svcName);
    settings.put("port", new Integer(svcPort).toString());
  }
  
  public MulticastDNS(String svcType, int svcPort) throws UnknownHostException {
    this();
    this.svcType = svcType;
    this.svcPort = svcPort;
  }
 
  public MulticastDNS(InetAddress addr, String svcType, int svcPort) throws UnknownHostException {
    this();
    this.addr = addr;
    this.svcType = svcType;
    this.svcPort = svcPort;
  }

  protected void handleRegisterCommand() throws IOException {
    if(jmdns==null) {
      if(addr!=null) {
        jmdns = JmDNS.create(this.addr);      
      } else {
        jmdns = JmDNS.create();
      }
    }
    ServiceInfo svcInfo = ServiceInfo.create(svcType, svcName, svcPort, 1, 1, settings);
    try {
      this.jmdns.registerService(svcInfo);
      LOG.info("Registered service '" + svcName + "' as: " + svcInfo);
    } catch (IOException e) {
      LOG.error("Failed to register service '" + svcName + "'");
    }
  }

  protected void handleUnregisterCommand() {
    this.jmdns.unregisterAllServices();
    try {
      this.jmdns.close();
    } catch (IOException e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
  }
 
}
