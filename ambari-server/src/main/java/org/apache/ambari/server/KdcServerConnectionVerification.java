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

package org.apache.ambari.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.kerby.kerberos.kerb.KrbCodec;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.common.KrbUtil;
import org.apache.kerby.kerberos.kerb.type.KerberosTime;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionType;
import org.apache.kerby.kerberos.kerb.type.base.KrbError;
import org.apache.kerby.kerberos.kerb.type.base.KrbMessage;
import org.apache.kerby.kerberos.kerb.type.base.KrbMessageType;
import org.apache.kerby.kerberos.kerb.type.base.PrincipalName;
import org.apache.kerby.kerberos.kerb.type.kdc.AsReq;
import org.apache.kerby.kerberos.kerb.type.kdc.KdcOptions;
import org.apache.kerby.kerberos.kerb.type.kdc.KdcReqBody;
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
 * Uses it to make sure host can talk to specified KDC Server
 * </li>
 * <p/>
 * <li>Ambari Server:
 * Uses it for connection check, like agent, and also validates
 * the credentials provided on Server side.
 * </li>
 * </ul>
 * </p>
 */
@Singleton
public class KdcServerConnectionVerification {

  private static final Logger LOG = LoggerFactory.getLogger(KdcServerConnectionVerification.class);
  private static final int MAX_TCP_RESPONSE_BYTES = 4 * 1024 * 1024;
  private static final int MAX_UDP_RESPONSE_BYTES = 65_507;
  private static final String PROBE_PRINCIPAL = "noUser@noRealm";
  private static final String PROBE_REALM = "noRealm";

  private Configuration config;

  /**
   * The connection timeout in seconds.
   */
  private int connectionTimeout = 10;

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
      if (kdcDetails.length == 1) {
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
   * @param port   KDC port
   * @return true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachable(String server, int port) {
    boolean success = isKdcReachable(server, port, ConnectionProtocol.TCP) || isKdcReachable(server, port, ConnectionProtocol.UDP);

    if (!success) {
      LOG.error("Failed to connect to the KDC at {}:{} using either TCP or UDP", server, port);
    }

    return success;
  }

  /**
   * Attempt to communicate with KDC server over a specified communication protocol (TCP or UDP).
   *
   * @param server         KDC hostname or IP address
   * @param port           KDC server port
   * @param connectionProtocol the type of connection to use
   * @return true if communication is successful; false otherwise
   */
  public boolean isKdcReachable(final String server, final int port, final ConnectionProtocol connectionProtocol) {
    int timeoutMillis = connectionTimeout * 1000;

    FutureTask<Boolean> future = new FutureTask<>(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        Boolean success;

        try {
          KrbMessage response = sendProbe(server, port, connectionProtocol, timeoutMillis);
          success = response.getMsgType() == KrbMessageType.AS_REP ||
              response.getMsgType() == KrbMessageType.KRB_ERROR;

          if (response instanceof KrbError) {
            KrbError error = (KrbError) response;
            String message = String.format("Received a valid Kerberos error while testing connectivity to the KDC:\n" +
              "**** Host:  %s:%d (%s)\n" +
              "**** Error: %s\n" +
              "**** Code:  %d (%s)",
            server, port, connectionProtocol.name(), error.getEtext(),
            error.getErrorCode().getValue(), error.getErrorCode().getMessage());

            LOG.info(message);
          } else {
            LOG.info("Received Kerberos {} while testing connectivity to the KDC at {}:{} over {}",
                response.getMsgType(), server, port, connectionProtocol.name());
          }
        } catch (IOException | KrbException e) {
          LOG.info(String.format("Received Kerberos probe failure while testing connectivity to the KDC: %s\n" +
              "**** Host: %s:%d (%s)", e.getLocalizedMessage(), server, port, connectionProtocol.name()), e);
          success = false;
        } catch (Throwable e) {
          LOG.info(String.format("Received Exception while testing connectivity to the KDC: %s\n**** Host: %s:%d (%s)",
            e.getLocalizedMessage(), server, port, connectionProtocol.name()), e);

          // some bad unexpected thing occurred
          throw new RuntimeException(e);
        }

        return success;
      }
    });

    new Thread(future, "ambari-kdc-verify").start();
    Boolean result;
    try {
      // timeout after specified timeout
      result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);

      if (result) {
        LOG.info(String.format("Successfully connected to the KDC server at %s:%d over %s",
            server, port, connectionProtocol.name()));
      } else {
        LOG.warn(String.format("Failed to connect to the KDC server at %s:%d over %s",
            server, port, connectionProtocol.name()));
      }
    } catch (InterruptedException e) {
      String message = String.format("Interrupted while trying to communicate with KDC server at %s:%d over %s",
          server, port, connectionProtocol.name());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message);
      }

      result = false;
      future.cancel(true);
    } catch (ExecutionException e) {
      String message = String.format("An unexpected exception occurred while attempting to communicate with the KDC server at %s:%d over %s",
          server, port, connectionProtocol.name());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message);
      }

      result = false;
    } catch (TimeoutException e) {
      String message = String.format("Timeout occurred while attempting to to communicate with KDC server at %s:%d over %s",
          server, port, connectionProtocol.name());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message);
      }

      result = false;
      future.cancel(true);
    }

    return result;
  }

  protected KrbMessage sendProbe(String server, int port, ConnectionProtocol connectionProtocol,
                                 int timeoutMillis) throws IOException, KrbException {
    byte[] request = KrbCodec.encode(createProbeRequest());
    byte[] response = ConnectionProtocol.TCP == connectionProtocol
        ? exchangeTcp(server, port, timeoutMillis, request)
        : exchangeUdp(server, port, timeoutMillis, request);
    return KrbCodec.decodeMessage(ByteBuffer.wrap(response));
  }

  private byte[] exchangeTcp(String server, int port, int timeoutMillis, byte[] request) throws IOException {
    InetSocketAddress address = new InetSocketAddress(server, port);
    try (Socket socket = new Socket()) {
      socket.connect(address, timeoutMillis);
      socket.setSoTimeout(timeoutMillis);
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(request.length);
      output.write(request);
      output.flush();

      DataInputStream input = new DataInputStream(socket.getInputStream());
      int responseLength = input.readInt();
      if (responseLength < 1 || responseLength > MAX_TCP_RESPONSE_BYTES) {
        throw new IOException("Invalid KDC response length: " + responseLength);
      }
      byte[] response = new byte[responseLength];
      input.readFully(response);
      return response;
    }
  }

  private byte[] exchangeUdp(String server, int port, int timeoutMillis, byte[] request) throws IOException {
    InetSocketAddress address = new InetSocketAddress(server, port);
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.connect(address);
      socket.setSoTimeout(timeoutMillis);
      socket.send(new DatagramPacket(request, request.length));

      byte[] responseBuffer = new byte[MAX_UDP_RESPONSE_BYTES];
      DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
      socket.receive(response);
      if (response.getLength() < 1) {
        throw new IOException("The KDC returned an empty response");
      }
      return Arrays.copyOf(response.getData(), response.getLength());
    }
  }

  private AsReq createProbeRequest() {
    long now = System.currentTimeMillis();
    KdcReqBody requestBody = new KdcReqBody();
    requestBody.setKdcOptions(new KdcOptions());
    requestBody.setCname(new PrincipalName(PROBE_PRINCIPAL));
    requestBody.setRealm(PROBE_REALM);
    requestBody.setSname(KrbUtil.makeTgsPrincipal(PROBE_REALM));
    requestBody.setTill(new KerberosTime(now + KerberosTime.MINUTE));
    requestBody.setNonce((int) now);
    requestBody.setEtypes(Arrays.asList(
        EncryptionType.AES256_CTS_HMAC_SHA1_96,
        EncryptionType.AES128_CTS_HMAC_SHA1_96));

    AsReq request = new AsReq();
    request.setReqBody(requestBody);
    return request;
  }

  /**
   * Set the connection timeout.
   * This is the amount of time that we will attempt to read data from connection.
   *
   * @param timeoutSeconds timeout in seconds
   */
  public void setConnectionTimeout(int timeoutSeconds) {
    connectionTimeout = (timeoutSeconds < 1) ? 1 : timeoutSeconds;
  }

  /**
   * Get the timeout value.
   *
   * @return the connection timeout value in seconds
   */
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * Parses port number from given string.
   *
   * @param port port number string
   * @return parsed port number
   * @throws NumberFormatException    if given string cannot be parsed
   * @throws IllegalArgumentException if given string is null or empty
   */
  protected int parsePort(String port) {
    if (StringUtils.isEmpty(port)) {
      throw new IllegalArgumentException("Port number must be non-empty, non-null positive integer");
    }
    return Integer.parseInt(port);
  }

  /**
   * A connection protocol to use to for connecting to the KDC
   */
  public enum ConnectionProtocol {
    TCP,
    UDP
  }
}
