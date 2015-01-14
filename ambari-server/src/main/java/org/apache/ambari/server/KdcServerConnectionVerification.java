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

package org.apache.ambari.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Utility class which checks connection to Kerberos Server.
 * <p>
 * It has two potential clients.
 * <ul>
 * <li>Ambari Agent: 
 * 		Uses it to make sure host can talk to specified KDC Server
 * </li>
 * 
 * <li>Ambari Server: 
 * 		Uses it for connection check, like agent, and also validates
 * 		the credentials provided on Server side.
 * </li>
 * </ul>
 * </p>
 */
@Singleton
public class KdcServerConnectionVerification {

  private static Logger LOG = LoggerFactory.getLogger(KdcServerConnectionVerification.class);

  private Configuration config;
  
  @Inject
  public KdcServerConnectionVerification(Configuration config) {
    this.config = config;
  }

  
  /**
   * Given server IP or hostname, checks if server is reachable i.e.
   * we can make a socket connection to it. Hostname may contain port
   * number separated by a colon. 
   * 
   * @param kdcHost KDC server IP or hostname (with optional port number)
   * @return true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachable(String kdcHost) {
    try {
      if (kdcHost == null || kdcHost.isEmpty()) {
        throw new IllegalArgumentException("Invalid hostname for KDC server");
      }
      String[] kdcDetails = kdcHost.split(":");
      if (kdcDetails.length == 1)  {
        return isKdcReachable(kdcDetails[0], parsePort(config.getDefaultKdcPort()));
      } else {
        return isKdcReachable(kdcDetails[0], parsePort(kdcDetails[1]));
      }
    } catch (Exception e) {
      LOG.error("Exception while checking KDC reachability: " + e);
      return false;
    }
  }
  /**
   * Given server IP or hostname, checks if server is reachable i.e.
   * we can make a socket connection to it.
   * 
   * @param server KDC server IP or hostname
   * @param port	 KDC port
   * @return	true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachable(String server, Integer port) {
    Socket socket = null;
    try {
      socket = new Socket();
      socket.connect(new InetSocketAddress(server, port), config.getKdcConnectionCheckTimeout());
    } catch (UnknownHostException e) {
      LOG.error("Unable to resolve Kerberos Server hostname");
      return false;
    } catch (IOException e) {
      LOG.error("Unable to connect to Kerberos Server");
      return false;
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
          LOG.debug("Error while closing socket connection to Kerberos Server. Can be ignored.");
        }
      }
    }

    return true;
  }

  /**
   * Parses port number from given string.
   * @param port port number string
   * @throws NumberFormatException if given string cannot be parsed
   * @throws IllegalArgumentException if given string is null or empty
   * @return parsed port number
   */
  private final int parsePort(String port) {
    if (StringUtils.isEmpty(port)) {
      throw new IllegalArgumentException("Port number must be non-empty, non-null positive integer");
    }
    return Integer.parseInt(port);
  }

}
