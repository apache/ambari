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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.kerberos.client.KdcConfig;
import org.apache.directory.kerberos.client.KdcConnection;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
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

  /**
   * UDP connection timeout in seconds.
   */
  private int udpTimeout = 10;

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
   * Given a host and port, checks if server is reachable meaning that we
   * can communicate with it.  First we attempt to connect via TCP and if
   * that is unsuccessful, attempt via UDP. It is important to understand that
   * we are not validating credentials, only attempting to communicate with server
   * process for the give host and port.
   *
   * @param server KDC server IP or hostname
   * @param port	 KDC port
   * @return	true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachable(String server, int port) {
    return isKdcReachableViaTCP(server, port) || isKdcReachableViaUDP(server, port);
  }

  /**
   * Attempt to connect to KDC server over TCP.
   *
   * @param server KDC server IP or hostname
   * @param port	 KDC server port
   * @return	true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachableViaTCP(String server, int port) {
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
   * Attempt to communicate with KDC server over UDP.
   * @param server KDC hostname or IP address
   * @param port   KDC server port
   * @return  true if communication is successful; false otherwise
   */
  public boolean isKdcReachableViaUDP(final String server, final int port) {
    int timeoutMillis = udpTimeout * 1000;
    final KdcConfig config = KdcConfig.getDefaultConfig();
    config.setHostName(server);
    config.setKdcPort(port);
    config.setUseUdp(true);
    config.setTimeout(timeoutMillis);

    final KdcConnection connection = getKdcUdpConnection(config);
    FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        try {
          // we are only testing whether we can communicate with server and not
          // validating credentials
          connection.getTgt("noUser@noRealm", "noPassword");
        } catch (KerberosException e) {
          // unfortunately, need to look at msg as error 60 is a generic error code
          return ! (e.getErrorCode() == ErrorType.KRB_ERR_GENERIC.getValue() &&
                    e.getMessage().contains("TimeOut"));
          //todo: evaluate other error codes to provide better information
          //todo: as there may be other error codes where we should return false
        } catch (Exception e) {
          // some bad unexpected thing occurred
          throw new RuntimeException(e);
        }
        return true;
      }
    });

    new Thread(future, "ambari-kdc-verify").start();
    Boolean result;
    try {
      // timeout after specified timeout
      result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      LOG.error("Interrupted while trying to communicate with KDC server over UDP");
      result = false;
      future.cancel(true);
    } catch (ExecutionException e) {
      LOG.error("An unexpected exception occurred while attempting to communicate with the KDC server over UDP", e);
      result = false;
    } catch (TimeoutException e) {
      LOG.error("Timeout occurred while attempting to to communicate with KDC server over UDP");
      result = false;
      future.cancel(true);
    }

    return result;
  }

  /**
   * Get a KDC UDP connection for the given configuration.
   * This has been extracted into it's own method primarily
   * for unit testing purposes.
   *
   * @param config KDC connection configuration
   * @return new KDC connection
   */
  protected KdcConnection getKdcUdpConnection(KdcConfig config) {
    return new KdcConnection(config);
  }

  /**
   * Set the UDP connection timeout.
   * This is the amount of time that we will attempt to read data from UDP connection.
   *
   * @param timeoutSeconds  timeout in seconds
   */
  public void setUdpTimeout(int timeoutSeconds) {
    udpTimeout = (timeoutSeconds < 1) ? 1 : timeoutSeconds;
  }

  /**
   * Get the UDP timeout value.
   *
   * @return the UDP connection timeout value in seconds
   */
  public int getUdpTimeout() {
    return udpTimeout;
  }

  /**
   * Parses port number from given string.
   * @param port port number string
   * @throws NumberFormatException if given string cannot be parsed
   * @throws IllegalArgumentException if given string is null or empty
   * @return parsed port number
   */
  private int parsePort(String port) {
    if (StringUtils.isEmpty(port)) {
      throw new IllegalArgumentException("Port number must be non-empty, non-null positive integer");
    }
    return Integer.parseInt(port);
  }
}
