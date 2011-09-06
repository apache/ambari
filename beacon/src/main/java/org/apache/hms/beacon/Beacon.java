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

package org.apache.hms.beacon;

import java.io.IOException;
import java.net.UnknownHostException;
import org.apache.hms.common.util.DaemonWatcher;
import org.apache.hms.common.util.MulticastDNS;

/**
 * 
 * HMS Beacon broadcast ZooKeeper server location by using MulticastDNS.
 * This utility runs on the same node as ZooKeeper server.
 *
 */
public class Beacon extends MulticastDNS {
  public Beacon() throws UnknownHostException {
    super();
  }
 
  public Beacon(String svcType, int svcPort) throws UnknownHostException {
    super(svcType, svcPort);  
  }
  
  /**
   * Register Zookeeper host location in MulticastDNS
   * @throws IOException
   */
  public void start() throws IOException {
    handleRegisterCommand();
  }
  
  /**
   * Remove Zookeeper host location from MulticastDNS
   * @throws IOException
   */
  public void stop() throws IOException {
    handleUnregisterCommand();
  }
 
  public static void main(String[] args) throws IOException {
    DaemonWatcher.createInstance(System.getProperty("PID"), 9101);
    try {
      final Beacon helper = new Beacon("_zookeeper._tcp.local.", 2181);
      try {
        helper.start();
      } catch(Throwable t) {
        helper.stop();
        throw t;
      }
    } catch(Throwable e) {
      DaemonWatcher.bailout(1);      
    }
  }
}