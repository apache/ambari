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

import static org.easymock.EasyMock.createNiceMock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class CertGenerationTest {

  private static final int PASS_FILE_NAME_LEN = 20;
  private static final float MAX_PASS_LEN = 100;

  private static final Logger LOG = LoggerFactory.getLogger(CertGenerationTest.class);
  public static TemporaryFolder temp = new TemporaryFolder();

  private static Injector injector;

  private static CertificateManager certMan;
  private static String passFileName;
  private static int passLen;

  @Inject
  static void init(CertificateManager instance) {
    certMan = instance;
  }


  private static class SecurityModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Properties.class).toInstance(buildTestProperties());
      bind(Configuration.class).toConstructor(getConfigurationConstructor());
      bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      requestStaticInjection(CertGenerationTest.class);
    }
  }

  protected static Properties buildTestProperties() {
    try {
      temp.create();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Properties properties = new Properties();
    properties.setProperty(Configuration.SRVR_KSTR_DIR.getKey(),
      temp.getRoot().getAbsolutePath());
    passLen = 16 + (int) Math.abs((new Random().nextFloat() * (MAX_PASS_LEN - 16)));

    properties.setProperty(Configuration.SRVR_CRT_PASS_LEN.getKey(),
      String.valueOf(passLen));

    passFileName = RandomStringUtils.randomAlphabetic(PASS_FILE_NAME_LEN);
    properties.setProperty(Configuration.SRVR_CRT_PASS_FILE.getKey(), passFileName);
    properties.setProperty(
      Configuration.BOOTSTRAP_MASTER_HOSTNAME.getKey(), "ambari.example.test");

    return properties;
  }

  protected static Constructor<Configuration> getConfigurationConstructor() {
    try {
      return Configuration.class.getConstructor(Properties.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Expected constructor not found in Configuration.java", e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws IOException {


    injector = Guice.createInjector(new SecurityModule());
    certMan = injector.getInstance(CertificateManager.class);

    //Test using actual ca.config.
    try {
      File caConfig = new File("conf/unix/ca.config");
      File caConfigTest = new File(temp.getRoot().getAbsolutePath(), "ca.config");
      File newCertsDir = new File(temp.getRoot().getAbsolutePath(), "newcerts");
      newCertsDir.mkdirs();
      File indexTxt = new File(temp.getRoot().getAbsolutePath(), "index.txt");
      indexTxt.createNewFile();

      String content = IOUtils.toString(new FileInputStream(caConfig));
      content = content.replaceAll("/var/lib/ambari-server/keys/db", temp.getRoot().getAbsolutePath());
      IOUtils.write(content, new FileOutputStream(caConfigTest));
    } catch (IOException e) {
      e.printStackTrace();
      TestCase.fail();
    }

    certMan.initRootCert();
  }

  @AfterClass
  public static void tearDownAfterClass() throws IOException {
    temp.delete();
  }

  @Test
  public void testServerCertGen() throws Exception {
    File serverCrt = new File(temp.getRoot().getAbsoluteFile() + File.separator + Configuration.SRVR_CRT_NAME.getDefaultValue());
    Assert.assertTrue(serverCrt.exists());
  }

  @Test
  public void testServerKeyGen() throws Exception {
    File serverKey = new File(temp.getRoot().getAbsoluteFile() + File.separator + Configuration.SRVR_KEY_NAME.getDefaultValue());
    Assert.assertTrue(serverKey.exists());
  }

  @Test
  public void testServerKeystoreGen() throws Exception {
    File serverKeyStrore = new File(temp.getRoot().getAbsoluteFile() + File.separator + Configuration.KSTR_NAME.getDefaultValue());
    Assert.assertTrue(serverKeyStrore.exists());
  }

  @Test
  public void testServerKeystoreContainsServerIdentity() throws Exception {
    Configuration configuration = injector.getInstance(Configuration.class);
    File keystoreFile = new File(
      temp.getRoot(), Configuration.KSTR_NAME.getDefaultValue());
    KeyStore keystore = KeyStore.getInstance("PKCS12");
    try (FileInputStream stream = new FileInputStream(keystoreFile)) {
      keystore.load(stream, configuration.getConfigsMap()
        .get(Configuration.SRVR_CRT_PASS.getKey()).toCharArray());
    }

    Assert.assertTrue(keystore.isKeyEntry("ambari-server"));
  }

  @Test
  public void testTemporaryKeystorePasswordFileIsRemoved() throws Exception {
    File[] temporaryPasswordFiles = temp.getRoot().listFiles(
      (directory, name) -> name.startsWith(".ambari-keystore-pass-")
        && name.endsWith(".tmp"));
    Assert.assertNotNull(temporaryPasswordFiles);
    Assert.assertEquals(0, temporaryPasswordFiles.length);
  }

  @Test
  public void testServerTruststoreContainsAgentCertificateAuthority() throws Exception {
    Configuration configuration = injector.getInstance(Configuration.class);
    File truststoreFile = new File(
      temp.getRoot(), Configuration.TSTR_NAME.getDefaultValue());
    KeyStore truststore = KeyStore.getInstance("PKCS12");
    try (FileInputStream stream = new FileInputStream(truststoreFile)) {
      truststore.load(stream, configuration.getConfigsMap()
        .get(Configuration.SRVR_CRT_PASS.getKey()).toCharArray());
    }

    Assert.assertTrue(truststore.isCertificateEntry("ambari-agent-ca"));
  }

  @Test
  public void testServerIdentityCertificateContainsConfiguredHostname() throws Exception {
    File serverIdentityCertificate = new File(
      temp.getRoot(), CertificateManager.SERVER_IDENTITY_CERT_NAME);
    Assert.assertTrue(serverIdentityCertificate.isFile());

    X509Certificate certificate;
    try (FileInputStream stream = new FileInputStream(serverIdentityCertificate)) {
      certificate = (X509Certificate) CertificateFactory
        .getInstance("X.509").generateCertificate(stream);
    }
    Collection<List<?>> names = certificate.getSubjectAlternativeNames();
    Assert.assertNotNull(names);
    Assert.assertTrue(names.stream().anyMatch(name ->
      name.size() >= 2 && Integer.valueOf(2).equals(name.get(0))
        && "ambari.example.test".equals(name.get(1))));
  }

  @Test
  public void testSignedAgentCertificateCannotActAsCertificateAuthority()
      throws Exception {
    String hostname = "agent.example.test";
    File key = new File(temp.getRoot(), hostname + ".key");
    File request = new File(temp.getRoot(), hostname + ".csr");
    int requestExitCode = certMan.runCommand(String.format(
        "openssl req -new -newkey rsa:2048 -nodes -keyout %s -out %s "
            + "-subj /CN=%s -addext basicConstraints=critical,CA:true",
        key.getAbsolutePath(), request.getAbsolutePath(), hostname));
    Assert.assertEquals(0, requestExitCode);

    certMan.configs.getConfigsMap().put(
        Configuration.PASSPHRASE.getKey(), "passphrase");
    SignCertResponse response = certMan.signAgentCrt(hostname,
        FileUtils.readFileToString(request, StandardCharsets.UTF_8), "passphrase");
    Assert.assertEquals(SignCertResponse.OK_STATUS, response.getResult());

    File certificateFile = new File(temp.getRoot(), hostname + ".crt");
    X509Certificate certificate;
    try (FileInputStream stream = new FileInputStream(certificateFile)) {
      certificate = (X509Certificate) CertificateFactory
          .getInstance("X.509").generateCertificate(stream);
    }
    Assert.assertEquals(-1, certificate.getBasicConstraints());
    Assert.assertTrue(certificate.getExtendedKeyUsage()
        .contains("1.3.6.1.5.5.7.3.2"));
  }

  @Ignore // randomly fails on BAO (e.g. https://builds.apache.org/job/Ambari-branch-2.2/155/console)
  @Test
  public void testRevokeExistingAgentCert() throws Exception {

    Map<String,String> config = certMan.configs.getConfigsMap();
    config.put(Configuration.PASSPHRASE.getKey(),"passphrase");

    String agentHostname = "agent_hostname";
    SignCertResponse scr = certMan.signAgentCrt(agentHostname,
      "incorrect_agentCrtReqContent", "passphrase");
    //Revoke command wasn't executed
    Assert.assertFalse(scr.getMessage().contains("-revoke"));

    //Emulate existing agent certificate
    File fakeAgentCertFile = new File(temp.getRoot().getAbsoluteFile() +
      File.separator + agentHostname + ".crt");
    Assert.assertTrue(fakeAgentCertFile.exists());

    //Revoke command was executed
    scr = certMan.signAgentCrt(agentHostname,
      "incorrect_agentCrtReqContent", "passphrase");
    Assert.assertTrue(scr.getMessage().contains("-revoke"));
  }

  @Test
  public void testPassFileGen() throws Exception {

    File passFile = new File(temp.getRoot().getAbsolutePath() + File.separator
      + passFileName);

    Assert.assertTrue(passFile.exists());

    String pass = FileUtils.readFileToString(passFile, Charset.defaultCharset());

    Assert.assertEquals(pass.length(), passLen);

    if (ShellCommandUtil.LINUX) {
      String permissions = ShellCommandUtil.
        getUnixFilePermissions(passFile.getAbsolutePath());
      Assert.assertEquals(ShellCommandUtil.MASK_OWNER_ONLY_RW, permissions);
    }

  }
}
