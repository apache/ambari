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

package org.apache.ambari.logsearch.util;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLUtil {
  private static final Logger LOG = LoggerFactory.getLogger(SSLUtil.class);
  
  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String KEYSTORE_TYPE_ARG = "javax.net.ssl.keyStoreType";
  private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String TRUSTSTORE_TYPE_ARG = "javax.net.ssl.trustStoreType";
  private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
  
  private SSLUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static String getKeyStoreLocation() {
    return System.getProperty(KEYSTORE_LOCATION_ARG);
  }
  
  public static String getKeyStorePassword() {
    return System.getProperty(KEYSTORE_PASSWORD_ARG);
  }
  
  public static String getKeyStoreType() {
    return System.getProperty(KEYSTORE_TYPE_ARG, DEFAULT_KEYSTORE_TYPE);
  }
  
  public static String getTrustStoreLocation() {
    return System.getProperty(TRUSTSTORE_LOCATION_ARG);
  }
  
  public static String getTrustStorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD_ARG);
  }
  
  public static String getTrustStoreType() {
    return System.getProperty(TRUSTSTORE_TYPE_ARG, DEFAULT_TRUSTSTORE_TYPE);
  }
  
  public static boolean isKeyStoreSpecified() {
    return StringUtils.isNotEmpty(getKeyStoreLocation()) && StringUtils.isNotEmpty(getKeyStorePassword());
  }

  private static boolean isTrustStoreSpecified() {
    return StringUtils.isNotEmpty(getTrustStoreLocation()) && StringUtils.isNotEmpty(getTrustStorePassword());
  }
  
  public static SslContextFactory getSslContextFactory() {
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
  
  public static SSLContext getSSLContext() {
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

  /**
   * Put private key into in-memory keystore and write it to a file (JKS file)
   */
  public static void setKeyAndCertInKeystore(X509Certificate cert, KeyPair keyPair, KeyStore keyStore, String keyStoreLocation, char[] password)
    throws Exception {
    Certificate[] certChain = new Certificate[1];
    certChain[0] = cert;
    try (FileOutputStream fos = new FileOutputStream(keyStoreLocation)) {
      keyStore.setKeyEntry("logsearch.alias", keyPair.getPrivate(), password, certChain);
      keyStore.store(fos, password);
    } catch (Exception e) {
      LOG.error("Could not write certificate to Keystore");
      throw e;
    }
  }

  /**
   * Create in-memory keypair with bouncy castle
   */
  public static KeyPair createKeyPair(String encryptionType, int byteCount)
    throws NoSuchProviderException, NoSuchAlgorithmException {
    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator keyPairGenerator = createKeyPairGenerator(encryptionType, byteCount);
    return keyPairGenerator.genKeyPair();
  }

  /**
   * Generate X509 certificate if it does not exist
   */
  public static X509Certificate generateCertificate(String certificateLocation, KeyPair keyPair, String algorithm) throws Exception {
    try {
      File certFile = new File(certificateLocation);
      if (certFile.exists()) {
        LOG.info("Certificate file exists ({}), skip the generation.", certificateLocation);
        return getCertFile(certificateLocation);
      } else {
        Security.addProvider(new BouncyCastleProvider());
        X509Certificate cert = SSLUtil.createCert(keyPair, algorithm, InetAddress.getLocalHost().getCanonicalHostName());
        FileUtils.writeByteArrayToFile(certFile, cert.getEncoded());
        return cert;
      }
    } catch (Exception e) {
      LOG.error("Could not create certificate.");
      throw e;
    }
  }

  private static X509Certificate getCertFile(String location) throws Exception {
    try (FileInputStream fos = new FileInputStream(location)) {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) factory.generateCertificate(fos);
    } catch (Exception e) {
      LOG.error("Cannot read cert file. ('{}')", location);
      throw e;
    }
  }

  private static X509Certificate createCert(KeyPair keyPair, String signatureAlgoritm, String domainName)
    throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
    v3CertGen.setSerialNumber(BigInteger.valueOf(Math.abs(new SecureRandom().nextInt())));
    v3CertGen.setIssuerDN(new X509Principal("CN=" + domainName + ", OU=None, O=None L=None, C=None"));
    v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
    v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)));
    v3CertGen.setSubjectDN(new X509Principal("CN=" + domainName + ", OU=None, O=None L=None, C=None"));
    v3CertGen.setPublicKey(keyPair.getPublic());
    v3CertGen.setSignatureAlgorithm(signatureAlgoritm);
    return v3CertGen.generate(keyPair.getPrivate());
  }

  private static KeyPairGenerator createKeyPairGenerator(String algorithmIdentifier, int bitCount)
    throws NoSuchProviderException, NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithmIdentifier, BouncyCastleProvider.PROVIDER_NAME);
    kpg.initialize(bitCount);
    return kpg;
  }

}
