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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.HostUtils;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Ambari security.
 * Manages server and agent certificates
 */
@Singleton
public class CertificateManager {

  private static final Logger LOG = LoggerFactory.getLogger(CertificateManager.class);

  @Inject Configuration configs;

  private static final String GEN_SRVR_KEY = "openssl genrsa -des3 " +
      "-passout {0} -out {1}" + File.separator + "{2} 4096 ";
  private static final String GEN_SRVR_REQ = "openssl req -passin {0} " +
      "-new -key {1}" + File.separator + "{2} -out {1}" + File.separator + "{5} " +
      "-batch -subj /CN=Ambari-Agent-CA";
  private static final String SIGN_SRVR_CRT = "openssl ca -create_serial " +
    "-out {1}" + File.separator + "{3} -days 3650 -keyfile {1}" + File.separator + "{2} -passin {0} -selfsign " +
    "-extensions jdk7_ca -config {1}" + File.separator + "ca.config -batch " +
    "-infiles {1}" + File.separator + "{5}";
  static final String SERVER_IDENTITY_KEY_NAME = "server.key";
  static final String SERVER_IDENTITY_CSR_NAME = "server.csr";
  static final String SERVER_IDENTITY_CERT_NAME = "server.crt";
  private static final String GEN_SERVER_IDENTITY_KEY = "openssl genrsa -aes256 " +
      "-passout {0} -out {1}" + File.separator + SERVER_IDENTITY_KEY_NAME + " 2048";
  private static final String GEN_SERVER_IDENTITY_REQ = "openssl req -passin {0} " +
      "-new -key {1}" + File.separator + SERVER_IDENTITY_KEY_NAME +
      " -out {1}" + File.separator + SERVER_IDENTITY_CSR_NAME +
      " -batch -subj /CN={6} -addext subjectAltName={7}";
  private static final String SIGN_SERVER_IDENTITY_CRT = "openssl ca -config {1}" +
      File.separator + "ca.config -in {1}" + File.separator + SERVER_IDENTITY_CSR_NAME +
      " -out {1}" + File.separator + SERVER_IDENTITY_CERT_NAME +
      " -days 365 -extensions server_cert -batch -passin {0} -keyfile {1}" +
      File.separator + "{2} -cert {1}" + File.separator + "{3}";
  private static final String EXPRT_KSTR = "openssl pkcs12 -export" +
      " -in {1}" + File.separator + SERVER_IDENTITY_CERT_NAME +
      " -inkey {1}" + File.separator + SERVER_IDENTITY_KEY_NAME +
      " -certfile {1}" + File.separator + "{3} -out {1}" + File.separator + "{4}.new" +
      " -name ambari-server -passout {11} -passin {0}";
  private static final String CREATE_SERVER_TRUSTSTORE =
      "{8} -importcert -noprompt -alias ambari-agent-ca -file {1}" +
      File.separator + "{3} -keystore {1}" + File.separator + "{9}.new" +
      " -storetype PKCS12 -storepass:file {1}" + File.separator + "{10}";
  private static final String REVOKE_AGENT_CRT = "openssl ca " +
      "-config {0}" + File.separator + "ca.config -keyfile {0}" + File.separator + "{4} -revoke {0}" + File.separator + "{2} -batch " +
      "-passin {3} -cert {0}" + File.separator + "{5}";
  private static final String VERIFY_AGENT_CSR = "openssl req -in {0}" +
      File.separator + "{1} -noout -verify";
  private static final String SIGN_AGENT_CRT = "openssl ca -config " +
      "{0}" + File.separator + "ca.config -in {0}" + File.separator + "{1} -out {0}" + File.separator + "{2} " +
      "-extensions client_cert -batch -passin {3} -keyfile {0}" + File.separator + "{4} -cert {0}" + File.separator + "{5}";

  /**
   * Verify that root certificate exists, generate it otherwise.
   */
  private static final String SET_PERMISSIONS = "find %s -type f -exec chmod 700 {} +";

  private static final String SET_SERVER_PASS_FILE_PERMISSIONS = "chmod 600 %s";

  public void initRootCert() {
    LOG.info("Initialization of root certificate");
    boolean certExists = isCertExists();
    boolean managedCertificateLayout = usesManagedCertificateLayout();
    LOG.info("Certificate exists:" + certExists);

    if (!certExists) {
      if (!managedCertificateLayout) {
        throw new IllegalStateException(
            "Custom Server certificate layout is incomplete; provision the configured "
                + "certificate, keystore, and truststore before starting Ambari Server");
      }
      generateCertificateAuthority();
    }

    if (!managedCertificateLayout) {
      validateCustomCertificateLayout();
    } else {
      String serverHostname = getServerHostname();
      File identityCertificate = new File(
          configs.getProperty(Configuration.SRVR_KSTR_DIR), SERVER_IDENTITY_CERT_NAME);
      File keystore = new File(configs.getProperty(Configuration.SRVR_KSTR_DIR),
          configs.getProperty(Configuration.KSTR_NAME));
      File truststore = new File(configs.getProperty(Configuration.SRVR_KSTR_DIR),
          configs.getProperty(Configuration.TSTR_NAME));
      File certificateAuthority = new File(
          configs.getProperty(Configuration.SRVR_KSTR_DIR),
          configs.getProperty(Configuration.SRVR_CRT_NAME));
      validateManagedCertificateAuthority(certificateAuthority);
      if (!isServerIdentityMaterialValid(identityCertificate, keystore,
          truststore, certificateAuthority, serverHostname,
          configs.getConfigsMap().get(Configuration.SRVR_CRT_PASS.getKey()))) {
        generateServerIdentityCertificate(serverHostname);
      }
    }
  }

  /**
   * Checks root certificate state.
   * @return "true" if certificate exists
   */
  private boolean isCertExists() {

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    File certFile = new File(srvrKstrDir + File.separator + srvrCrtName);
    LOG.debug("srvrKstrDir = {}", srvrKstrDir);
    LOG.debug("srvrCrtName = {}", srvrCrtName);
    LOG.debug("certFile = {}", certFile.getAbsolutePath());

    return certFile.exists();
  }


  /**
   * Runs os command
   *
   * @return command execution exit code
   */
  protected int runCommand(String command) {
    try {
      Process process = Runtime.getRuntime().exec(command);
      Thread errorLogger = new Thread(
          () -> logCommandOutput(process.getErrorStream()), "ambari-openssl-stderr");
      errorLogger.setDaemon(true);
      errorLogger.start();
      logCommandOutput(process.getInputStream());
      int exitCode = process.waitFor();
      errorLogger.join();
      ShellCommandUtil.logOpenSslExitCode(command, exitCode);
      return exitCode;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Interrupted while executing {}",
          ShellCommandUtil.hideOpenSslPassword(command), e);
    } catch (IOException e) {
      LOG.warn("Unable to execute {}",
          ShellCommandUtil.hideOpenSslPassword(command), e);
    }
    return -1;
  }

  private void logCommandOutput(InputStream stream) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        stream, Charset.forName("UTF8")))) {
      String line;
      while ((line = reader.readLine()) != null) {
        LOG.info(line);
      }
    } catch (IOException e) {
      LOG.warn("Unable to read certificate command output", e);
    }
  }

  private void generateCertificateAuthority() {
    LOG.info("Generation of Agent certificate authority");

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    String srvrCsrName = configsMap.get(Configuration.SRVR_CSR_NAME.getKey());
    String srvrKeyName = configsMap.get(Configuration.SRVR_KEY_NAME.getKey());
    String srvrCrtPassSource = getServerCertificatePasswordSource(configsMap);

    Object[] scriptArgs = {srvrCrtPassSource, srvrKstrDir, srvrKeyName,
        srvrCrtName, null, srvrCsrName};

    String command = MessageFormat.format(GEN_SRVR_KEY, scriptArgs);
    runRequiredCommand(command, "generate the Agent CA private key");

    command = MessageFormat.format(GEN_SRVR_REQ, scriptArgs);
    runRequiredCommand(command, "generate the Agent CA request");

    command = MessageFormat.format(SIGN_SRVR_CRT, scriptArgs);
    runRequiredCommand(command, "sign the Agent CA certificate");
  }

  private void generateServerIdentityCertificate(String serverHostname) {
    LOG.info("Generating Server identity certificate for {}", serverHostname);

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    String srvrKeyName = configsMap.get(Configuration.SRVR_KEY_NAME.getKey());
    String kstrName = configsMap.get(Configuration.KSTR_NAME.getKey());
    String tstrName = configsMap.get(Configuration.TSTR_NAME.getKey());
    String srvrCrtPassFile = configsMap.get(Configuration.SRVR_CRT_PASS_FILE.getKey());
    String srvrCrtPassSource = getServerCertificatePasswordSource(configsMap);
    String subjectAlternativeName = InetAddresses.isInetAddress(serverHostname)
        ? "IP:" + serverHostname : "DNS:" + serverHostname;
    File temporaryKeystore = new File(srvrKstrDir, kstrName + ".new");

    String keytool = new File(
        new File(System.getProperty("java.home"), "bin"), "keytool").getAbsolutePath();
    Object[] scriptArgs = {srvrCrtPassSource, srvrKstrDir, srvrKeyName,
        srvrCrtName, kstrName, null, serverHostname, subjectAlternativeName,
        keytool, tstrName, srvrCrtPassFile, null};

    String command = MessageFormat.format(GEN_SERVER_IDENTITY_KEY, scriptArgs);
    runRequiredCommand(command, "generate the Server identity private key");

    command = MessageFormat.format(GEN_SERVER_IDENTITY_REQ, scriptArgs);
    runRequiredCommand(command, "generate the Server identity request");

    command = MessageFormat.format(SIGN_SERVER_IDENTITY_CRT, scriptArgs);
    runRequiredCommand(command, "sign the Server identity certificate");

    File temporaryKeystorePassword = createTemporaryKeystorePasswordFile(
        srvrKstrDir, srvrCrtPassFile);
    scriptArgs[11] = "file:" + temporaryKeystorePassword.getAbsolutePath();
    FileUtils.deleteQuietly(temporaryKeystore);
    try {
      command = MessageFormat.format(EXPRT_KSTR, scriptArgs);
      runRequiredCommand(command, "export the Server identity keystore");
    } catch (RuntimeException e) {
      FileUtils.deleteQuietly(temporaryKeystore);
      throw e;
    } finally {
      FileUtils.deleteQuietly(temporaryKeystorePassword);
    }

    File temporaryTruststore = new File(srvrKstrDir, tstrName + ".new");
    FileUtils.deleteQuietly(temporaryTruststore);
    command = MessageFormat.format(CREATE_SERVER_TRUSTSTORE, scriptArgs);
    runRequiredCommand(command, "create the Agent certificate truststore");

    File keystore = new File(srvrKstrDir, kstrName);
    File truststore = new File(srvrKstrDir, tstrName);
    try {
      Files.move(temporaryKeystore.toPath(), keystore.toPath(),
          StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      Files.move(temporaryTruststore.toPath(), truststore.toPath(),
          StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      FileUtils.deleteQuietly(temporaryKeystore);
      FileUtils.deleteQuietly(temporaryTruststore);
      throw new IllegalStateException(
          "Unable to publish the Server identity keystore", e);
    }

    command = String.format(SET_PERMISSIONS, srvrKstrDir);
    runRequiredCommand(command, "secure the Server certificate files");

    command = String.format(SET_SERVER_PASS_FILE_PERMISSIONS,
        srvrKstrDir + File.separator + srvrCrtPassFile);
    runRequiredCommand(command, "secure the Server certificate password file");
  }

  private void runRequiredCommand(String command, String operation) {
    int exitCode = runCommand(command);
    if (exitCode != 0) {
      throw new IllegalStateException(
          "Unable to " + operation + "; OpenSSL exited with " + exitCode);
    }
  }

  private String getServerCertificatePasswordSource(Map<String, String> configsMap) {
    return "file:" + new File(
        configsMap.get(Configuration.SRVR_KSTR_DIR.getKey()),
        configsMap.get(Configuration.SRVR_CRT_PASS_FILE.getKey())).getAbsolutePath();
  }

  private File createTemporaryKeystorePasswordFile(String directory,
      String passwordFileName) {
    File passwordFile = new File(directory, passwordFileName);
    File temporaryPasswordFile = null;
    try {
      byte[] password = Files.readAllBytes(passwordFile.toPath());
      temporaryPasswordFile = Files.createTempFile(
          new File(directory).toPath(), ".ambari-keystore-pass-", ".tmp",
          PosixFilePermissions.asFileAttribute(
              PosixFilePermissions.fromString("rw-------"))).toFile();
      Files.write(temporaryPasswordFile.toPath(), password);
      return temporaryPasswordFile;
    } catch (IOException e) {
      FileUtils.deleteQuietly(temporaryPasswordFile);
      throw new IllegalStateException(
          "Unable to prepare the Server keystore password source", e);
    }
  }

  private boolean usesManagedCertificateLayout() {
    return Configuration.SRVR_CRT_NAME.getDefaultValue().equals(
        configs.getProperty(Configuration.SRVR_CRT_NAME))
        && Configuration.SRVR_KEY_NAME.getDefaultValue().equals(
            configs.getProperty(Configuration.SRVR_KEY_NAME))
        && Configuration.KSTR_NAME.getDefaultValue().equals(
            configs.getProperty(Configuration.KSTR_NAME))
        && Configuration.KSTR_TYPE.getDefaultValue().equalsIgnoreCase(
            configs.getProperty(Configuration.KSTR_TYPE))
        && Configuration.TSTR_NAME.getDefaultValue().equals(
            configs.getProperty(Configuration.TSTR_NAME))
        && Configuration.TSTR_TYPE.getDefaultValue().equalsIgnoreCase(
            configs.getProperty(Configuration.TSTR_TYPE));
  }

  private void validateCustomCertificateLayout() {
    File directory = new File(configs.getProperty(Configuration.SRVR_KSTR_DIR));
    File keystore = new File(directory, configs.getProperty(Configuration.KSTR_NAME));
    File truststore = new File(directory, configs.getProperty(Configuration.TSTR_NAME));
    if (!keystore.isFile() || !truststore.isFile()) {
      throw new IllegalStateException(
          "Custom Server certificate layout is incomplete; provision the configured "
              + "certificate, keystore, and truststore before starting Ambari Server");
    }
  }

  private void validateManagedCertificateAuthority(File certificateAuthorityFile) {
    try (FileInputStream stream = new FileInputStream(certificateAuthorityFile)) {
      X509Certificate certificateAuthority = (X509Certificate) CertificateFactory
          .getInstance("X.509").generateCertificate(stream);
      certificateAuthority.checkValidity();
      if (certificateAuthority.getBasicConstraints() < 0) {
        throw new IllegalStateException(
            "Managed Agent certificate authority is not a CA certificate: "
                + certificateAuthorityFile);
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Managed Agent certificate authority is invalid or expired; replace it and "
              + "re-enroll all Agents: " + certificateAuthorityFile, e);
    }
  }

  private String getServerHostname() {
    try {
      String hostname = configs.getMasterHostname(
          InetAddress.getLocalHost().getCanonicalHostName());
      if (StringUtils.isBlank(hostname)
          || (!InetAddresses.isInetAddress(hostname)
              && !HostUtils.isValidHostname(hostname))) {
        throw new IllegalStateException(
            "Invalid bootstrap.master_host_name: " + hostname);
      }
      return hostname;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to resolve the Ambari Server hostname", e);
    }
  }

  private boolean isServerIdentityMaterialValid(File certificateFile,
      File keystoreFile, File truststoreFile, File certificateAuthorityFile,
      String hostname, String password) {
    if (!certificateFile.isFile() || !keystoreFile.isFile()
        || !truststoreFile.isFile() || !certificateAuthorityFile.isFile()
        || StringUtils.isEmpty(password)) {
      return false;
    }
    int expectedType = InetAddresses.isInetAddress(hostname) ? 7 : 2;
    try (FileInputStream stream = new FileInputStream(certificateFile)) {
      X509Certificate certificate = (X509Certificate) CertificateFactory
          .getInstance("X.509").generateCertificate(stream);
      certificate.checkValidity(Date.from(
          Instant.now().plus(30, ChronoUnit.DAYS)));
      Collection<List<?>> subjectAlternativeNames =
          certificate.getSubjectAlternativeNames();
      if (subjectAlternativeNames != null) {
        for (List<?> name : subjectAlternativeNames) {
          if (name.size() >= 2 && Integer.valueOf(expectedType).equals(name.get(0))
              && subjectAlternativeNameMatches(
                  hostname, String.valueOf(name.get(1)), expectedType)) {
            return keystoreContainsIdentity(keystoreFile, password, certificate)
                && truststoreContainsCertificateAuthority(
                    truststoreFile, password, certificateAuthorityFile);
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Unable to validate existing Server identity certificate {}",
          certificateFile, e);
    }
    return false;
  }

  private boolean keystoreContainsIdentity(File keystoreFile, String password,
      X509Certificate identityCertificate) throws Exception {
    KeyStore keystore = KeyStore.getInstance(
        configs.getProperty(Configuration.KSTR_TYPE));
    try (FileInputStream stream = new FileInputStream(keystoreFile)) {
      keystore.load(stream, password.toCharArray());
    }
    Enumeration<String> aliases = keystore.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (keystore.isKeyEntry(alias)
          && identityCertificate.equals(keystore.getCertificate(alias))) {
        return true;
      }
    }
    return false;
  }

  private boolean truststoreContainsCertificateAuthority(File truststoreFile,
      String password, File certificateAuthorityFile) throws Exception {
    X509Certificate certificateAuthority;
    try (FileInputStream stream = new FileInputStream(certificateAuthorityFile)) {
      certificateAuthority = (X509Certificate) CertificateFactory
          .getInstance("X.509").generateCertificate(stream);
    }
    KeyStore truststore = KeyStore.getInstance(
        configs.getProperty(Configuration.TSTR_TYPE));
    try (FileInputStream stream = new FileInputStream(truststoreFile)) {
      truststore.load(stream, password.toCharArray());
    }
    Enumeration<String> aliases = truststore.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (truststore.isCertificateEntry(alias)
          && certificateAuthority.equals(truststore.getCertificate(alias))) {
        return true;
      }
    }
    return false;
  }

  private static boolean subjectAlternativeNameMatches(
      String expected, String actual, int type) {
    if (type == 7) {
      return InetAddresses.forString(expected).equals(InetAddresses.forString(actual));
    }
    return expected.equalsIgnoreCase(actual);
  }

  /**
   * Returns server's PEM-encoded CA chain file content
   * @return string server's PEM-encoded CA chain file content
   */
  public String getCACertificateChainContent() {
    String serverCertDir = configs.getProperty(Configuration.SRVR_KSTR_DIR);

    // Attempt to send the explicit CA certificate chain file.
    String serverCertChainName = configs.getProperty(Configuration.SRVR_CRT_CHAIN_NAME);
    File certChainFile = new File(serverCertDir, serverCertChainName);
    if(certChainFile.exists()) {
      try {
        return new String(Files.readAllBytes(certChainFile.toPath()), StandardCharsets.UTF_8);
      } catch (IOException e) {
        LOG.error(e.getMessage());
      }
    }

    // Fall back to the original way things were done and send the server's SSL certificate as the
    // Certificate chain file.
    String serverCertName = configs.getProperty(Configuration.SRVR_CRT_NAME);
    File certFile = new File(serverCertDir, serverCertName);
    if(certFile.canRead()) {
      try {
        return new String(Files.readAllBytes(certFile.toPath()), StandardCharsets.UTF_8);
      } catch (IOException e) {
        LOG.error(e.getMessage());
      }
    }

    // If all else fails, send nothing...
    return null;
  }

  /**
   * Signs agent certificate
   * Adds agent certificate to server keystore
   * @return string with agent signed certificate content
   */
  public synchronized SignCertResponse signAgentCrt(String agentHostname, String agentCrtReqContent, String passphraseAgent) {
    SignCertResponse response = new SignCertResponse();
    LOG.info("Signing agent certificate");

    // Ensure the hostname is not empty or null...
    agentHostname = StringUtils.trim(agentHostname);

    if(StringUtils.isEmpty(agentHostname)) {
      LOG.warn("The agent hostname is missing");
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("The agent hostname is missing");
      return response;
    }

    // Optionally check the supplied hostname to make sure it is a valid hostname.
    // By default, this feature is turned on.  If this check is not desired (maybe the validation
    // rules are too strict), the feature may be turned off by setting the following
    // property in the ambari.properties file:
    //
    //    security.agent.hostname.validate = "false"
    //
    if(configs.validateAgentHostnames()) {
      LOG.info("Validating agent hostname: {}", agentHostname);
      if(!HostUtils.isValidHostname(agentHostname)) {
        LOG.warn("The agent hostname is not a valid hostname");
        response.setResult(SignCertResponse.ERROR_STATUS);
        response.setMessage("The agent hostname is not a valid hostname");
        return response;
      }
    }
    else {
      LOG.info("Skipping validation of agent hostname: {}", agentHostname);
    }

    LOG.info("Verifying passphrase");

    String passphraseSrvr = configs.getConfigsMap().get(Configuration.
        PASSPHRASE.getKey());

    if (StringUtils.isBlank(passphraseSrvr) || passphraseAgent == null ||
        !MessageDigest.isEqual(
            passphraseSrvr.trim().getBytes(StandardCharsets.UTF_8),
            passphraseAgent.trim().getBytes(StandardCharsets.UTF_8))) {
      LOG.warn("Incorrect passphrase from the agent");
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("Incorrect passphrase from the agent");
      return response;
    }

    if (StringUtils.isBlank(agentCrtReqContent)) {
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("The agent certificate request is missing");
      return response;
    }

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtPassSource = getServerCertificatePasswordSource(configsMap);
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    String srvrKeyName = configsMap.get(Configuration.SRVR_KEY_NAME.getKey());
    String agentFilePrefix = getAgentCertificateFilePrefix(agentHostname);
    String agentCrtReqName = agentFilePrefix + ".csr";
    String agentCrtName = agentFilePrefix + ".crt";

    Object[] scriptArgs = {srvrKstrDir, agentCrtReqName, agentCrtName,
        srvrCrtPassSource, srvrKeyName, srvrCrtName};

    File agentCrtReqFile = new File(srvrKstrDir + File.separator +
        agentCrtReqName);
    try {
      FileUtils.writeStringToFile(agentCrtReqFile, agentCrtReqContent,
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error("Unable to write Agent certificate request for {}", agentHostname, e);
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("Unable to write the agent certificate request");
      return response;
    }

    String verifyCommand = MessageFormat.format(VERIFY_AGENT_CSR, scriptArgs);
    int verifyExitCode = runCommand(verifyCommand);
    if (verifyExitCode != 0) {
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("The agent certificate request is invalid");
      return response;
    }

    // Revoke only after the replacement CSR has been parsed and its signature verified.
    File agentCrtFile = new File(srvrKstrDir + File.separator + agentCrtName);
    if (agentCrtFile.exists()) {
      LOG.info("Revoking of " + agentHostname + " certificate.");
      String revokeCommand = MessageFormat.format(REVOKE_AGENT_CRT, scriptArgs);
      int revokeExitCode = runCommand(revokeCommand);
      if (revokeExitCode != 0) {
        response.setResult(SignCertResponse.ERROR_STATUS);
        response.setMessage(ShellCommandUtil.getOpenSslCommandResult(
            revokeCommand, revokeExitCode));
        return response;
      }
    }

    String command = MessageFormat.format(SIGN_AGENT_CRT, scriptArgs);

    LOG.debug(ShellCommandUtil.hideOpenSslPassword(command));

    int commandExitCode = runCommand(command); // ssl command execution
    if (commandExitCode != 0) {
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
      //LOG.warn(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
      return response;
    }

    String agentCrtContent = "";
    try {
      agentCrtContent = FileUtils.readFileToString(agentCrtFile,
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error("Error reading signed agent certificate", e);
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("Error reading signed agent certificate");
      return response;
    }
    response.setResult(SignCertResponse.OK_STATUS);
    response.setSignedCa(agentCrtContent);
    //LOG.info(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
    return response;
  }

  static String getAgentCertificateFilePrefix(String hostname) {
    if (hostname.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,199}")
        && !hostname.contains("..")) {
      return hostname;
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(hostname.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder("agent-");
      for (byte item : digest) {
        result.append(String.format("%02x", item & 0xff));
      }
      return result.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
