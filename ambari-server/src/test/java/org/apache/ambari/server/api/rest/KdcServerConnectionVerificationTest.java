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
package org.apache.ambari.server.api.rest;

import static org.apache.ambari.server.KdcServerConnectionVerification.ConnectionProtocol.TCP;
import static org.apache.ambari.server.KdcServerConnectionVerification.ConnectionProtocol.UDP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ambari.server.KdcServerConnectionVerification;
import org.apache.ambari.server.KdcServerConnectionVerification.ConnectionProtocol;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.kerby.kerberos.kerb.KrbErrorCode;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.kerberos.kerb.type.base.KrbError;
import org.apache.kerby.kerberos.kerb.type.base.KrbMessage;
import org.apache.kerby.kerberos.kerb.type.kdc.AsRep;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test for {@link KdcServerConnectionVerification}.
 */
public class KdcServerConnectionVerificationTest {

  private static final int KDC_TEST_PORT = 8090;

  private Configuration configuration;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    Properties configProps = new Properties();
    configProps.setProperty(Configuration.KDC_PORT.getKey(), Integer.toString(KDC_TEST_PORT));
    configuration = new Configuration(configProps);
  }

  @Test
  public void testValidateFailInvalidPort() {
    assertFalse(new KdcServerConnectionVerification(configuration).isKdcReachable("test-host:abcd"));
  }

  @Test
  public void testValidateSuccess() {
    TestKdcServerConnectionVerification verifier = successVerifier();

    assertTrue(verifier.isKdcReachable("test-host:11111"));
    assertProbeSettings(verifier, "test-host", 11111, TCP, 10_000);
  }

  @Test
  public void testValidateTcpSuccess() {
    TestKdcServerConnectionVerification verifier = successVerifier();

    assertTrue(verifier.isKdcReachable("test-host", 11111, TCP));
    assertProbeSettings(verifier, "test-host", 11111, TCP, 10_000);
  }

  @Test
  public void testValidateUdpSuccess() {
    TestKdcServerConnectionVerification verifier = successVerifier();

    assertTrue(verifier.isKdcReachable("test-host", 11111, UDP));
    assertProbeSettings(verifier, "test-host", 11111, UDP, 10_000);
  }

  @Test
  public void testDecodedKrbErrorProvesKdcIsReachable() {
    KrbError error = new KrbError();
    error.setErrorCode(KrbErrorCode.KDC_ERR_C_PRINCIPAL_UNKNOWN);
    error.setEtext("Unknown test principal");
    TestKdcServerConnectionVerification verifier = new TestKdcServerConnectionVerification(configuration, error);

    assertTrue(verifier.isKdcReachable("test-host", 11111, TCP));
  }

  @Test
  public void testGenericKerberosClientFailureDoesNotProveReachability() {
    TestKdcServerConnectionVerification verifier = failureVerifier(new KrbException("Malformed KDC response"));

    assertFalse(verifier.isKdcReachable("test-host", 11111, TCP));
  }

  @Test
  public void testLocallyGeneratedCodedKerberosFailureDoesNotProveReachability() {
    TestKdcServerConnectionVerification verifier = failureVerifier(
        new KrbException(KrbErrorCode.KDC_ERR_ETYPE_NOSUPP, "No local encryption implementation"));

    assertFalse(verifier.isKdcReachable("test-host", 11111, TCP));
  }

  @Test
  public void testUnknownRuntimeFailureDoesNotProveReachability() {
    TestKdcServerConnectionVerification verifier = failureVerifier(
        new RuntimeException("Unexpected client failure"));

    assertFalse(verifier.isKdcReachable("test-host", 11111, UDP));
  }

  @Test
  public void testRequestTimeoutCancelsProbe() {
    TestKdcServerConnectionVerification verifier = new TestKdcServerConnectionVerification(configuration) {
      @Override
      protected KrbMessage sendProbe(String server, int port, ConnectionProtocol connectionProtocol,
                                     int timeoutMillis) throws KrbException {
        recordProbe(server, port, connectionProtocol, timeoutMillis);
        try {
          Thread.sleep(60_000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new KrbException("Probe interrupted", e);
        }
        return new AsRep();
      }
    };
    verifier.setConnectionTimeout(1);

    assertFalse(verifier.isKdcReachable("test-host", 11111, TCP));
    assertProbeSettings(verifier, "test-host", 11111, TCP, 1_000);
  }

  @Test
  public void testTcpFailureFallsBackToUdp() {
    TestKdcServerConnectionVerification verifier = new TestKdcServerConnectionVerification(configuration) {
      @Override
      protected KrbMessage sendProbe(String server, int port, ConnectionProtocol connectionProtocol,
                                     int timeoutMillis) throws KrbException {
        recordProbe(server, port, connectionProtocol, timeoutMillis);
        if (connectionProtocol == TCP) {
          throw new KrbException("TCP unavailable");
        }
        KrbError error = new KrbError();
        error.setErrorCode(KrbErrorCode.KDC_ERR_C_PRINCIPAL_UNKNOWN);
        return error;
      }
    };

    assertTrue(verifier.isKdcReachable("test-host", 11111));
    assertEquals(List.of(TCP, UDP), verifier.getProtocols());
  }

  @Test
  public void testMalformedTcpEndpointIsRejected() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      Thread serverThread = new Thread(() -> sendMalformedResponse(serverSocket), "malformed-kdc-test-server");
      serverThread.start();

      KdcServerConnectionVerification verifier = new KdcServerConnectionVerification(configuration);
      verifier.setConnectionTimeout(2);
      assertFalse(verifier.isKdcReachable("127.0.0.1", serverSocket.getLocalPort(), TCP));

      serverThread.join(5_000);
      assertFalse(serverThread.isAlive());
    }
  }

  @Test
  public void testEmbeddedKdcAuthenticationAndReachability() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }

    SimpleKdcServer kdc = new SimpleKdcServer();
    kdc.setWorkDir(folder.newFolder("embedded-kdc"));
    kdc.setKdcRealm("EXAMPLE.COM");
    kdc.setKdcHost("127.0.0.1");
    kdc.setKdcTcpPort(port);
    kdc.setKdcUdpPort(port);
    kdc.setAllowTcp(true);
    kdc.setAllowUdp(true);
    kdc.init();
    kdc.start();
    try {
      kdc.createPrincipal("probe-user@EXAMPLE.COM", "correct-password");
      assertTrue(kdc.getKrbClient().requestTgt("probe-user@EXAMPLE.COM", "correct-password") != null);
      try {
        kdc.getKrbClient().requestTgt("probe-user@EXAMPLE.COM", "wrong-password");
        fail("Invalid KDC credentials unexpectedly authenticated");
      } catch (KrbException expected) {
        // Expected authentication failure.
      }

      KdcServerConnectionVerification verifier = new KdcServerConnectionVerification(configuration);
      verifier.setConnectionTimeout(5);
      assertTrue(verifier.isKdcReachable("127.0.0.1", port, TCP));
      assertTrue(verifier.isKdcReachable("127.0.0.1", port, UDP));
    } finally {
      kdc.stop();
    }
  }

  private TestKdcServerConnectionVerification successVerifier() {
    return new TestKdcServerConnectionVerification(configuration);
  }

  private TestKdcServerConnectionVerification failureVerifier(Throwable failure) {
    return new TestKdcServerConnectionVerification(configuration) {
      @Override
      protected KrbMessage sendProbe(String server, int port, ConnectionProtocol connectionProtocol,
                                     int timeoutMillis) throws KrbException {
        recordProbe(server, port, connectionProtocol, timeoutMillis);
        if (failure instanceof KrbException) {
          throw (KrbException) failure;
        }
        throw (RuntimeException) failure;
      }
    };
  }

  private static void assertProbeSettings(TestKdcServerConnectionVerification verifier, String host, int port,
                                          ConnectionProtocol protocol, int timeoutMillis) {
    assertEquals(host, verifier.getLastHost());
    assertEquals(port, verifier.getLastPort());
    assertEquals(protocol, verifier.getProtocols().get(verifier.getProtocols().size() - 1));
    assertEquals(timeoutMillis, verifier.getLastTimeoutMillis());
  }

  private static void sendMalformedResponse(ServerSocket serverSocket) {
    try (Socket socket = serverSocket.accept(); OutputStream output = socket.getOutputStream()) {
      output.write("not-a-kerberos-response".getBytes(StandardCharsets.US_ASCII));
    } catch (Exception ignored) {
      // The assertion on the verifier result covers this test server's observable behavior.
    }
  }

  private static class TestKdcServerConnectionVerification extends KdcServerConnectionVerification {
    private final List<ConnectionProtocol> protocols = new ArrayList<>();
    private final KrbMessage response;
    private String lastHost;
    private int lastPort;
    private int lastTimeoutMillis;

    TestKdcServerConnectionVerification(Configuration config) {
      this(config, new AsRep());
    }

    TestKdcServerConnectionVerification(Configuration config, KrbMessage response) {
      super(config);
      this.response = response;
    }

    @Override
    protected KrbMessage sendProbe(String server, int port, ConnectionProtocol connectionProtocol,
                                   int timeoutMillis) throws IOException, KrbException {
      recordProbe(server, port, connectionProtocol, timeoutMillis);
      return response;
    }

    void recordProbe(String server, int port, ConnectionProtocol connectionProtocol, int timeoutMillis) {
      protocols.add(connectionProtocol);
      lastHost = server;
      lastPort = port;
      lastTimeoutMillis = timeoutMillis;
    }

    String getLastHost() {
      return lastHost;
    }

    int getLastPort() {
      return lastPort;
    }

    int getLastTimeoutMillis() {
      return lastTimeoutMillis;
    }

    List<ConnectionProtocol> getProtocols() {
      return protocols;
    }
  }
}
