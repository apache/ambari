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
package org.apache.ambari.server.security.encryption;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class containing methods to works with certificates
 */
public class CertificateUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CertificateUtils.class);

  /**
   * Get RSA public key from X.509 certificate file
   * @param filePath path to certificate file
   * @return RSA public key
   * @throws IOException
   * @throws CertificateException
   */
  public static RSAPublicKey getPublicKeyFromFile(String filePath) throws IOException, CertificateException {
    String pemString = FileUtils.readFileToString(new File(filePath));
    return getPublicKeyFromString(pemString);
  }

  /**
   * Get RSA public key from X.509 certificate string (full crt file content, including header and footer)
   * @param certificateString certificate string
   * @return RSA public key
   * @throws CertificateException
   * @throws UnsupportedEncodingException
   */
  public static RSAPublicKey getPublicKeyFromString(String certificateString)
    throws CertificateException, UnsupportedEncodingException {

    CertificateFactory fact = CertificateFactory.getInstance("X.509");
    ByteArrayInputStream is = new ByteArrayInputStream(
      certificateString.getBytes("UTF8"));

    X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
    return (RSAPublicKey)cer.getPublicKey();
  }
}
