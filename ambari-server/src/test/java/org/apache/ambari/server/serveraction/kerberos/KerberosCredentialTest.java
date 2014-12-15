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

package org.apache.ambari.server.serveraction.kerberos;

import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.eclipse.persistence.internal.helper.Helper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class KerberosCredentialTest {

  @Test
  public void testFromMap() throws Exception {
    KerberosCredential kerberosCredential;
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put(KerberosCredential.KEY_NAME_PRINCIPAL, "admin/admin@EXAMPLE.COM");
    attributes.put(KerberosCredential.KEY_NAME_KEYTAB, "bogus_base64-encoded_data");

    attributes.put("kerberos_admin/" + KerberosCredential.KEY_NAME_PRINCIPAL, "admin/admin@FOOBAR.COM");
    attributes.put("kerberos_admin/" + KerberosCredential.KEY_NAME_PASSWORD, "t0p_s3cr3t");

    // Test with an empty prefix
    kerberosCredential = KerberosCredential.fromMap(attributes, "");
    Assert.assertNotNull(kerberosCredential);
    Assert.assertEquals("admin/admin@EXAMPLE.COM", kerberosCredential.getPrincipal());
    Assert.assertNull(kerberosCredential.getPassword());
    Assert.assertEquals("bogus_base64-encoded_data", kerberosCredential.getKeytab());

    // Test with a NULL prefix
    kerberosCredential = KerberosCredential.fromMap(attributes, null);
    Assert.assertNotNull(kerberosCredential);
    Assert.assertEquals("admin/admin@EXAMPLE.COM", kerberosCredential.getPrincipal());
    Assert.assertNull(kerberosCredential.getPassword());
    Assert.assertEquals("bogus_base64-encoded_data", kerberosCredential.getKeytab());

    // Test with a prefix
    kerberosCredential = KerberosCredential.fromMap(attributes, "kerberos_admin/");
    Assert.assertNotNull(kerberosCredential);
    Assert.assertEquals("admin/admin@FOOBAR.COM", kerberosCredential.getPrincipal());
    Assert.assertEquals("t0p_s3cr3t", kerberosCredential.getPassword());
    Assert.assertNull(kerberosCredential.getKeytab());

    // Test with a prefix that does not resolve to any existing keys
    kerberosCredential = KerberosCredential.fromMap(attributes, "invalid/");
    Assert.assertNull(kerberosCredential);
  }

  @Test
  public void testEncryptAndDecrypt() throws Exception {
    byte[] key = "This is my key".getBytes();
    KerberosCredential credential;
    String cipherText;
    KerberosCredential decryptedCredential;

    credential = new KerberosCredential("admin/admin@FOOBAR.COM", "t0p_s3cr3t", null);
    cipherText = credential.encrypt(key);
    Assert.assertNotNull(cipherText);

    // Test a successful case
    decryptedCredential = KerberosCredential.decrypt(cipherText, key);
    Assert.assertNotNull(decryptedCredential);
    Assert.assertEquals(credential.getPrincipal(), decryptedCredential.getPrincipal());
    Assert.assertEquals(credential.getPassword(), decryptedCredential.getPassword());
    Assert.assertEquals(credential.getKeytab(), decryptedCredential.getKeytab());

    // Test an invalid key
    try {
      decryptedCredential = KerberosCredential.decrypt(cipherText, "not the key".getBytes());
      Assert.fail("Should have thrown AmbariException");
    } catch (AmbariException e) {
      // this is expected
    }

    // Test an invalid cipher text
    try {
      decryptedCredential = KerberosCredential.decrypt("I am not encrypted data", key);
      Assert.fail("Should have thrown AmbariException");
    } catch (AmbariException e) {
      // this is expected
    }
  }
}