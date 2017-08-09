/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.configurer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;

import org.apache.ambari.logsearch.conf.LogSearchSslConfig;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.apache.ambari.logsearch.conf.LogSearchSslConfig.CREDENTIAL_STORE_PROVIDER_PATH;
import static org.apache.ambari.logsearch.conf.LogSearchSslConfig.LOGSEARCH_CERT_DEFAULT_FOLDER;

@Named
public class SslConfigurer {
  private static final Logger LOG = LoggerFactory.getLogger(SslConfigurer.class);
  
  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String KEYSTORE_TYPE_ARG = "javax.net.ssl.keyStoreType";
  private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String TRUSTSTORE_TYPE_ARG = "javax.net.ssl.trustStoreType";
  private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
  private static final String KEYSTORE_PASSWORD_PROPERTY_NAME = "logsearch_keystore_password";
  private static final String TRUSTSTORE_PASSWORD_PROPERTY_NAME = "logsearch_truststore_password";
  private static final String KEYSTORE_PASSWORD_FILE = "ks_pass.txt";
  private static final String TRUSTSTORE_PASSWORD_FILE = "ts_pass.txt";
  
  private static final String LOGSEARCH_CERT_FILENAME = "logsearch.crt";
  private static final String LOGSEARCH_KEYSTORE_FILENAME = "logsearch.jks";
  private static final String LOGSEARCH_KEYSTORE_PRIVATE_KEY = "logsearch.private.key";
  private static final String LOGSEARCH_KEYSTORE_PUBLIC_KEY = "logsearch.public.key";

  private static final String LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD = "bigdata";

  @Inject
  private LogSearchSslConfig logSearchSslConfig;
  
  private String getKeyStoreLocation() {
    return System.getProperty(KEYSTORE_LOCATION_ARG);
  }

  private String getKeyStorePassword() {
    return System.getProperty(KEYSTORE_PASSWORD_ARG);
  }

  private String getKeyStoreType() {
    return System.getProperty(KEYSTORE_TYPE_ARG, DEFAULT_KEYSTORE_TYPE);
  }
  
  private String getTrustStoreLocation() {
    return System.getProperty(TRUSTSTORE_LOCATION_ARG);
  }

  private String getTrustStorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD_ARG);
  }

  private String getTrustStoreType() {
    return System.getProperty(TRUSTSTORE_TYPE_ARG, DEFAULT_TRUSTSTORE_TYPE);
  }

  public boolean isKeyStoreSpecified() {
    return StringUtils.isNotEmpty(getKeyStoreLocation());
  }

  private boolean isTrustStoreSpecified() {
    return StringUtils.isNotEmpty(getTrustStoreLocation());
  }
  
  public SslContextFactory getSslContextFactory() {
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(getKeyStoreLocation());
    sslContextFactory.setKeyStorePassword(getKeyStorePassword());
    sslContextFactory.setKeyStoreType(getKeyStoreType());
    if (isTrustStoreSpecified()) {
      sslContextFactory.setTrustStorePath(getTrustStoreLocation());
      sslContextFactory.setTrustStorePassword(getTrustStorePassword());
      sslContextFactory.setTrustStoreType(getTrustStoreType());
    }
    
    return sslContextFactory;
  }

  public SSLContext getSSLContext() {
    SslContextFactory sslContextFactory = getSslContextFactory();
    
    try {
      sslContextFactory.start();
      return sslContextFactory.getSslContext();
    } catch (Exception e) {
      LOG.error("Could not create SSL Context", e);
      return null;
    } finally {
      try {
        sslContextFactory.stop();
      } catch (Exception e) {
        LOG.error("Could not stop sslContextFactory", e);
      }
    }
  }

  private String getPasswordFromFile(String fileName) {
    try {
      File pwdFile = new File(LOGSEARCH_CERT_DEFAULT_FOLDER, fileName);
      if (!pwdFile.exists()) {
        FileUtils.writeStringToFile(pwdFile, LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD);
        return LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD;
      } else {
        return FileUtils.readFileToString(pwdFile);
      }
    } catch (Exception e) {
      LOG.warn("Exception occurred during read/write password file for keystore/truststore.", e);
      return null;
    }
  }

  private String getPasswordFromCredentialStore(String propertyName) {
    try {
      String providerPath = logSearchSslConfig.getCredentialStoreProviderPath();
      if (StringUtils.isEmpty(providerPath)) {
        return null;
      }
      
      Configuration config = new Configuration();
      config.set(CREDENTIAL_STORE_PROVIDER_PATH, providerPath);
      char[] passwordChars = config.getPassword(propertyName);
      return (ArrayUtils.isNotEmpty(passwordChars)) ? new String(passwordChars) : null;
    } catch (Exception e) {
      LOG.warn(String.format("Could not load password %s from credential store, using default password", propertyName), e);
      return null;
    }
  }

  private String getPassword(String propertyName, String fileName) {
    String credentialStorePassword = getPasswordFromCredentialStore(propertyName);
    if (credentialStorePassword != null) {
      return credentialStorePassword;
    }
    
    String filePassword = getPasswordFromFile(fileName);
    if (filePassword != null) {
      return filePassword;
    }
    
    return LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD;
  }

  /**
   * Put private key into in-memory keystore and write it to a file (JKS file)
   */
  private void setKeyAndCertInKeystore(X509Certificate cert, KeyPair keyPair, KeyStore keyStore, String keyStoreLocation, char[] password)
    throws Exception {
    Certificate[] certChain = new Certificate[1];
    certChain[0] = cert;
    try (FileOutputStream fos = new FileOutputStream(keyStoreLocation)) {
      keyStore.setKeyEntry("logsearch.alias", keyPair.getPrivate(), password, certChain);
      keyStore.store(fos, password);
    } catch (Exception e) {
      LOG.error("Could not write certificate to Keystore", e);
      throw e;
    }
  }

  /**
   * Create in-memory keypair with bouncy castle
   */
  private KeyPair createKeyPair(String encryptionType, int byteCount)
    throws NoSuchProviderException, NoSuchAlgorithmException {
    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator keyPairGenerator = createKeyPairGenerator(encryptionType, byteCount);
    return keyPairGenerator.genKeyPair();
  }

  /**
   * Generate X509 certificate if it does not exist
   */
  private X509Certificate generateCertificate(String certificateLocation, KeyPair keyPair, String algorithm) throws Exception {
    try {
      File certFile = new File(certificateLocation);
      if (certFile.exists()) {
        LOG.info("Certificate file exists ({}), skip the generation.", certificateLocation);
        return getCertFile(certificateLocation);
      } else {
        Security.addProvider(new BouncyCastleProvider());
        X509Certificate cert = createCert(keyPair, algorithm, InetAddress.getLocalHost().getCanonicalHostName());
        FileUtils.writeByteArrayToFile(certFile, cert.getEncoded());
        return cert;
      }
    } catch (Exception e) {
      LOG.error("Could not create certificate.", e);
      throw e;
    }
  }

  private void ensureStorePassword(String locationArg, String pwdArg, String propertyName, String fileName) {
    if (StringUtils.isNotEmpty(System.getProperty(locationArg)) && StringUtils.isEmpty(System.getProperty(pwdArg))) {
      String password = getPassword(propertyName, fileName);
      System.setProperty(pwdArg, password);
    }
  }
  
  public void ensureStorePasswords() {
    ensureStorePassword(KEYSTORE_LOCATION_ARG, KEYSTORE_PASSWORD_ARG, KEYSTORE_PASSWORD_PROPERTY_NAME, KEYSTORE_PASSWORD_FILE);
    ensureStorePassword(TRUSTSTORE_LOCATION_ARG, TRUSTSTORE_PASSWORD_ARG, TRUSTSTORE_PASSWORD_PROPERTY_NAME, TRUSTSTORE_PASSWORD_FILE);
  }

  private X509Certificate getCertFile(String location) throws Exception {
    try (FileInputStream fos = new FileInputStream(location)) {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) factory.generateCertificate(fos);
    } catch (Exception e) {
      LOG.error("Cannot read cert file. ('" + location + "')", e);
      throw e;
    }
  }

  private X509Certificate createCert(KeyPair keyPair, String signatureAlgoritm, String domainName)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, OperatorCreationException, CertificateException, IOException {
    
    RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
    
    AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgoritm);
    AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
    BcContentSignerBuilder sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
    
    ASN1InputStream publicKeyStream = new ASN1InputStream(rsaPublicKey.getEncoded());
    SubjectPublicKeyInfo pubKey = SubjectPublicKeyInfo.getInstance(publicKeyStream.readObject());
    publicKeyStream.close();
    
    X509v3CertificateBuilder v3CertBuilder = new X509v3CertificateBuilder(
        new X500Name("CN=" + domainName + ", OU=None, O=None L=None, C=None"),
        BigInteger.valueOf(Math.abs(new SecureRandom().nextInt())),
        new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
        new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)),
        new X500Name("CN=" + domainName + ", OU=None, O=None L=None, C=None"),
        pubKey);
    
    RSAKeyParameters keyParams = new RSAKeyParameters(true, rsaPrivateKey.getPrivateExponent(), rsaPrivateKey.getModulus());
    ContentSigner contentSigner = sigGen.build(keyParams);
    
    X509CertificateHolder certificateHolder = v3CertBuilder.build(contentSigner);
    
    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");
    return certConverter.getCertificate(certificateHolder);
  }

  private KeyPairGenerator createKeyPairGenerator(String algorithmIdentifier, int bitCount)
    throws NoSuchProviderException, NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithmIdentifier, BouncyCastleProvider.PROVIDER_NAME);
    kpg.initialize(bitCount);
    return kpg;
  }

  /**
   * Create keystore with keys and certificate (only if the keystore does not exist or if you have no permissions on the keystore file)
   */
  public void loadKeystore() {
    try {
      String certFolder = logSearchSslConfig.getCertFolder();
      String certAlgorithm = logSearchSslConfig.getCertAlgorithm();
      String certLocation = String.format("%s/%s", LOGSEARCH_CERT_DEFAULT_FOLDER, LOGSEARCH_CERT_FILENAME);
      String keyStoreLocation = StringUtils.isNotEmpty(getKeyStoreLocation()) ? getKeyStoreLocation()
        : String.format("%s/%s", LOGSEARCH_CERT_DEFAULT_FOLDER, LOGSEARCH_KEYSTORE_FILENAME);
      char[] password = StringUtils.isNotEmpty(getKeyStorePassword()) ?
        getKeyStorePassword().toCharArray() : LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD.toCharArray();
      boolean keyStoreFileExists = new File(keyStoreLocation).exists();
      if (!keyStoreFileExists) {
        FileUtil.createDirectory(certFolder);
        LOG.warn("Keystore file ('{}') does not exist, creating new one. " +
          "If the file exists, make sure you have proper permissions on that.", keyStoreLocation);
        if (isKeyStoreSpecified() && !"JKS".equalsIgnoreCase(getKeyStoreType())) {
          throw new RuntimeException(String.format("Keystore does not exist. Only JKS keystore can be auto generated. (%s)", keyStoreLocation));
        }
        LOG.info("SSL keystore is not specified. Generating it with certificate ... (using default format: JKS)");
        Security.addProvider(new BouncyCastleProvider());
        KeyPair keyPair = createKeyPair("RSA", 2048);
        File privateKeyFile = new File(String.format("%s/%s", certFolder, LOGSEARCH_KEYSTORE_PRIVATE_KEY));
        if (!privateKeyFile.exists()) {
          FileUtils.writeByteArrayToFile(privateKeyFile, keyPair.getPrivate().getEncoded());
        }
        File file = new File(String.format("%s/%s", certFolder, LOGSEARCH_KEYSTORE_PUBLIC_KEY));
        if (!file.exists()) {
          FileUtils.writeByteArrayToFile(file, keyPair.getPublic().getEncoded());
        }
        X509Certificate cert = generateCertificate(certLocation, keyPair, certAlgorithm);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password);
        setKeyAndCertInKeystore(cert, keyPair, keyStore, keyStoreLocation, password);
        FileUtil.setPermissionOnDirectory(certFolder, "600");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
