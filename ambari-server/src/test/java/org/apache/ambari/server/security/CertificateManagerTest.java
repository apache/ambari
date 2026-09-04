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

package org.apache.ambari.server.security;

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class CertificateManagerTest extends EasyMockSupport {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testTemporaryKeystorePasswordFileIsRemovedOnFailure()
      throws Exception {
    File directory = folder.newFolder();
    CertificateManager certificateManager = new CertificateManager();
    Method createTemporaryPasswordFile = CertificateManager.class
        .getDeclaredMethod("createTemporaryKeystorePasswordFile",
            String.class, String.class);
    createTemporaryPasswordFile.setAccessible(true);

    try {
      createTemporaryPasswordFile.invoke(certificateManager,
          directory.getAbsolutePath(), "missing-password-file");
      Assert.fail("Expected a missing password source to fail");
    } catch (InvocationTargetException e) {
      Assert.assertTrue(e.getCause() instanceof IllegalStateException);
    }

    File[] temporaryPasswordFiles = directory.listFiles(
        (parent, name) -> name.startsWith(".ambari-keystore-pass-")
            && name.endsWith(".tmp"));
    Assert.assertNotNull(temporaryPasswordFiles);
    Assert.assertEquals(0, temporaryPasswordFiles.length);
  }

  @Test
  public void testSignAgentCrt() throws Exception {
    Injector injector = getInjector();

    File directory = folder.newFolder();

    String hostname = "host1.example.com";

    Map<String, String> configurationMap = new HashMap<>();
    configurationMap.put(Configuration.SRVR_KSTR_DIR.getKey(), directory.getAbsolutePath());
    configurationMap.put(Configuration.SRVR_CRT_PASS.getKey(), "server_cert_pass");
    configurationMap.put(Configuration.SRVR_CRT_NAME.getKey(), "server_cert_name");
    configurationMap.put(Configuration.SRVR_KEY_NAME.getKey(), "server_key_name");
    configurationMap.put(Configuration.SRVR_CRT_PASS_FILE.getKey(), "pass.txt");
    configurationMap.put(Configuration.PASSPHRASE.getKey(), "passphrase");

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(true).once();
    expect(configuration.getConfigsMap()).andReturn(configurationMap).anyTimes();

    Method runCommand = CertificateManager.class.getDeclaredMethod("runCommand", String.class);

    final File agentCrtFile = new File(directory, String.format("%s.crt", hostname));

    String expectedCommand = String.format("openssl ca -config %s/ca.config -in %s/%s.csr -out %s -extensions client_cert -batch -passin file:%s/pass.txt -keyfile %s/%s -cert %s/%s",
        directory.getAbsolutePath(),
        directory.getAbsolutePath(),
        hostname,
        agentCrtFile.getAbsolutePath(),
        directory.getAbsolutePath(),
        directory.getAbsolutePath(),
        configurationMap.get(Configuration.SRVR_KEY_NAME.getKey()),
        directory.getAbsolutePath(),
        configurationMap.get(Configuration.SRVR_CRT_NAME.getKey()));
    String expectedVerifyCommand = String.format(
        "openssl req -in %s/%s.csr -noout -verify",
        directory.getAbsolutePath(), hostname);

    CertificateManager certificateManager = createMockBuilder(CertificateManager.class)
        .addMockedMethod(runCommand)
        .createMock();
    expect(certificateManager.runCommand(expectedVerifyCommand)).andReturn(0).once();
    expect(certificateManager.runCommand(expectedCommand))
        .andAnswer(new IAnswer<Integer>() {
          @Override
          public Integer answer() throws Throwable {
            return (agentCrtFile.createNewFile()) ? 0 : 1;
          }
        })
        .once();

    injector.injectMembers(certificateManager);

    replayAll();

    SignCertResponse response = certificateManager.signAgentCrt(hostname, "crtContent", "passphrase");

    verifyAll();

    Assert.assertEquals(SignCertResponse.OK_STATUS, response.getResult());
  }

  @Test
  public void testInvalidReplacementCsrDoesNotRevokeExistingCertificate()
      throws Exception {
    Injector injector = getInjector();
    File directory = folder.newFolder();
    String hostname = "host1.example.com";
    File existingCertificate = new File(directory, hostname + ".crt");
    Assert.assertTrue(existingCertificate.createNewFile());

    Map<String, String> configurationMap = new HashMap<>();
    configurationMap.put(Configuration.SRVR_KSTR_DIR.getKey(),
        directory.getAbsolutePath());
    configurationMap.put(Configuration.SRVR_CRT_PASS_FILE.getKey(), "pass.txt");
    configurationMap.put(Configuration.SRVR_CRT_NAME.getKey(), "ca.crt");
    configurationMap.put(Configuration.SRVR_KEY_NAME.getKey(), "ca.key");
    configurationMap.put(Configuration.PASSPHRASE.getKey(), "passphrase");

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(true).once();
    expect(configuration.getConfigsMap()).andReturn(configurationMap).anyTimes();

    Method runCommand = CertificateManager.class.getDeclaredMethod(
        "runCommand", String.class);
    CertificateManager certificateManager = createMockBuilder(CertificateManager.class)
        .addMockedMethod(runCommand)
        .createMock();
    String verifyCommand = String.format(
        "openssl req -in %s/%s.csr -noout -verify",
        directory.getAbsolutePath(), hostname);
    expect(certificateManager.runCommand(verifyCommand)).andReturn(1).once();
    injector.injectMembers(certificateManager);
    replayAll();

    SignCertResponse response = certificateManager.signAgentCrt(
        hostname, "not a valid CSR", "passphrase");

    Assert.assertEquals(SignCertResponse.ERROR_STATUS, response.getResult());
    Assert.assertEquals("The agent certificate request is invalid",
        response.getMessage());
    Assert.assertTrue(existingCertificate.isFile());
    verifyAll();
  }

  @Test
  public void testSignAgentCrtInvalidHostname() throws Exception {
    Injector injector = getInjector();

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(true).once();

    replayAll();

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);

    SignCertResponse response = certificateManager.signAgentCrt("hostname; echo \"hello\" > /tmp/hello.txt;", "crtContent", "passphrase");

    verifyAll();

    Assert.assertEquals(SignCertResponse.ERROR_STATUS, response.getResult());
    Assert.assertEquals("The agent hostname is not a valid hostname", response.getMessage());
  }

  @Test
  public void testSignAgentCrtBadPassphrase() throws Exception {
    Injector injector = getInjector();

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(true).once();
    expect(configuration.getConfigsMap()).andReturn(Collections.singletonMap(Configuration.PASSPHRASE.getKey(), "some_passphrase")).once();

    replayAll();

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);

    SignCertResponse response = certificateManager.signAgentCrt("host1.example.com", "crtContent", "passphrase");

    verifyAll();

    Assert.assertEquals(SignCertResponse.ERROR_STATUS, response.getResult());
    Assert.assertEquals("Incorrect passphrase from the agent", response.getMessage());
  }

  @Test
  public void testSignAgentCrtMissingPassphrase() throws Exception {
    Injector injector = getInjector();

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(true).once();
    expect(configuration.getConfigsMap()).andReturn(
        Collections.singletonMap(Configuration.PASSPHRASE.getKey(), "some_passphrase")).once();

    replayAll();

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);

    SignCertResponse response = certificateManager.signAgentCrt(
        "host1.example.com", "crtContent", null);

    verifyAll();

    Assert.assertEquals(SignCertResponse.ERROR_STATUS, response.getResult());
    Assert.assertEquals("Incorrect passphrase from the agent", response.getMessage());
  }

  @Test
  public void testSignAgentCrtInvalidHostnameIgnoreBadPassphrase() throws Exception {
    Injector injector = getInjector();

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(false).once();
    expect(configuration.getConfigsMap()).andReturn(Collections.singletonMap(Configuration.PASSPHRASE.getKey(), "some_passphrase")).once();

    replayAll();

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);

    SignCertResponse response = certificateManager.signAgentCrt("hostname; echo \"hello\" > /tmp/hello.txt;", "crtContent", "passphrase");

    verifyAll();

    Assert.assertEquals(SignCertResponse.ERROR_STATUS, response.getResult());
    Assert.assertEquals("Incorrect passphrase from the agent", response.getMessage());
  }

  @Test
  public void testUnsafeAgentHostnameUsesStablePrivateFileName() {
    String prefix = CertificateManager.getAgentCertificateFilePrefix(
        "../../agent name;with shell syntax");

    Assert.assertTrue(prefix.startsWith("agent-"));
    Assert.assertEquals(70, prefix.length());
    Assert.assertFalse(prefix.contains("/"));
    Assert.assertEquals(prefix, CertificateManager.getAgentCertificateFilePrefix(
        "../../agent name;with shell syntax"));
  }

  @Test
  public void testSignAgentCrtFailsWhenRequestCannotBeWritten() throws Exception {
    Injector injector = getInjector();
    File invalidKeystoreDirectory = folder.newFile("keys");
    Map<String, String> configurationMap = new HashMap<>();
    configurationMap.put(Configuration.SRVR_KSTR_DIR.getKey(),
        invalidKeystoreDirectory.getAbsolutePath());
    configurationMap.put(Configuration.SRVR_CRT_PASS_FILE.getKey(), "pass.txt");
    configurationMap.put(Configuration.SRVR_CRT_NAME.getKey(), "ca.crt");
    configurationMap.put(Configuration.SRVR_KEY_NAME.getKey(), "ca.key");
    configurationMap.put(Configuration.PASSPHRASE.getKey(), "passphrase");

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.validateAgentHostnames()).andReturn(true).once();
    expect(configuration.getConfigsMap()).andReturn(configurationMap).anyTimes();
    replayAll();

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);
    SignCertResponse response = certificateManager.signAgentCrt(
        "host1.example.com", "crtContent", "passphrase");

    Assert.assertEquals(SignCertResponse.ERROR_STATUS, response.getResult());
    Assert.assertEquals("Unable to write the agent certificate request",
        response.getMessage());
    verifyAll();
  }

  @Test
  public void testGetCACertificateChain() throws IOException {
    Injector injector = getInjector();

    File directory = folder.newFolder();
    String caCertFileName = "myca.crt";
    String caCertChainFileName = "myca_chain.pem";

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.getProperty(Configuration.SRVR_KSTR_DIR)).andReturn(directory.getAbsolutePath()).anyTimes();
    expect(configuration.getProperty(Configuration.SRVR_CRT_NAME)).andReturn(caCertFileName).anyTimes();
    expect(configuration.getProperty(Configuration.SRVR_CRT_CHAIN_NAME)).andReturn(caCertChainFileName).anyTimes();

    final File caCertFile = new File(directory, caCertFileName);
    final File caCertChainFile = new File(directory, caCertChainFileName);

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);

    replayAll();

    String content;

    // Only the CA certificate file is available, this is the fallback option
    Files.write(caCertFile.toPath(), Collections.singleton(caCertFile.getAbsolutePath()));
    content = certificateManager.getCACertificateChainContent();
    Assert.assertEquals(caCertFile.getAbsolutePath(), content.trim());

    // The CA certificate chain file is available, this is the preferred option
    Files.write(caCertChainFile.toPath(), Collections.singleton(caCertChainFile.getAbsolutePath()));
    content = certificateManager.getCACertificateChainContent();
    Assert.assertEquals(caCertChainFile.getAbsolutePath(), content.trim());

    verifyAll();
  }

  @Test
  public void testIncompleteCustomCertificateLayoutIsRejected() throws Exception {
    Injector injector = getInjector();
    File directory = folder.newFolder();
    Map<String, String> configurationMap = new HashMap<>();
    configurationMap.put(Configuration.SRVR_KSTR_DIR.getKey(), directory.getAbsolutePath());
    configurationMap.put(Configuration.SRVR_CRT_NAME.getKey(), "custom-ca.crt");

    Configuration configuration = injector.getInstance(Configuration.class);
    expect(configuration.getConfigsMap()).andReturn(configurationMap).once();
    expect(configuration.getProperty(Configuration.SRVR_CRT_NAME))
        .andReturn("custom-ca.crt").once();

    CertificateManager certificateManager = new CertificateManager();
    injector.injectMembers(certificateManager);
    replayAll();

    try {
      certificateManager.initRootCert();
      Assert.fail("Expected incomplete custom certificate layout to be rejected");
    } catch (IllegalStateException e) {
      Assert.assertTrue(e.getMessage().contains("Custom Server certificate layout"));
    }

    verifyAll();
  }

  private Injector getInjector() {
    return Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(createMock(Configuration.class));
      }
    });
  }

}
