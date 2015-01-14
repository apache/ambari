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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Properties;

import org.apache.ambari.server.KdcServerConnectionVerification;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.annotation.ExpectedException;

/**
 * Test for {@link KdcServerConnectionVerification}
 */
public class KdcServerConnectionVerificationTest  {

  private static Log LOG = LogFactory.getLog(KdcServerConnectionVerificationTest.class);

  private KdcServerConnectionVerification kdcConnectionVerifier;
  private Properties configProps;
  private Configuration configuration;

  private static ServerSocket serverSocket = null;
  private static boolean serverStop = false;

  private static final int KDC_TEST_PORT = 8090;
  // Some dummy port to test a non-listening KDC server
  private static final int DUMMY_KDC_PORT = 11234;

  @BeforeClass
  public static void beforeClass() throws Exception {
    createSocketServer(KDC_TEST_PORT);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    closeServerSocket();
  }
  
  @Before
  public void before() throws Exception {
    configProps = new Properties();
    configProps.setProperty(Configuration.KDC_PORT_KEY, Integer.toString(KDC_TEST_PORT));
    configuration = new Configuration(configProps);
    kdcConnectionVerifier = new KdcServerConnectionVerification(configuration);     
  }

  @Test
  public void testWithPortSuccess() throws Exception {
    assertTrue(kdcConnectionVerifier.isKdcReachable(String.format("localhost:%d", KDC_TEST_PORT)));
  }

  @Test
  public void testWithoutPortSuccess() throws Exception {
    assertTrue(kdcConnectionVerifier.isKdcReachable("localhost"));
  }

  @Test
  public void testWithoutPortFailure() throws Exception {
    // Assumption: test machine has no KDC so nothing listening on port DUMMY_KDC_PORT
    configProps.setProperty(Configuration.KDC_PORT_KEY, Integer.toString(DUMMY_KDC_PORT));
    assertFalse(kdcConnectionVerifier.isKdcReachable("localhost"));
  }

  @Test
  public void testWithPortFailure() throws Exception {
    assertFalse(kdcConnectionVerifier.isKdcReachable("localhost:8091"));
  }


  @Test
  @ExpectedException(NumberFormatException.class)
  public void testPortParsingFailure() throws Exception {
    assertFalse(kdcConnectionVerifier.isKdcReachable("localhost:abc"));
  }

  /**
   * Socket server for test
   * We need a separate thread as accept() is a blocking call
   */
  private static class SocketThread extends Thread {
    public void run() {
      while (serverSocket != null && !serverStop) {
        try {
          serverSocket.accept();
        } catch (SocketException se) {
          LOG.debug("SocketException during tearDown. Can be safely ignored");
        } catch (IOException e) {
          LOG.error("Unexpected exception while accepting connection request");
        }
      }

    }
  }

  private static void createSocketServer(int port) throws Exception {
    serverSocket = new ServerSocket(port);
    new SocketThread().start();
  }

  private static void closeServerSocket() throws Exception {
    serverStop = true;
    try{
      serverSocket.close();
    } catch (IOException ioe) {
      LOG.debug("IOException during tearDown. Can be safely ignored");
    }
  }
}
