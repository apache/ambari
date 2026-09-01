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

package org.apache.ambari.tools.credential;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CredentialStoreCreateTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void createsAndOverwritesCredentialFromStandardInput() throws Exception {
    String providerPath = newProviderPath();
    char[] original = "line one\nline two\0\u4e2d\u6587".toCharArray();

    assertEquals(0, run(providerPath, original, false));
    assertArrayEquals(original, readCredential(providerPath, "alias"));

    char[] replacement = "replacement".toCharArray();
    assertEquals(1, run(providerPath, replacement, false));
    assertEquals(0, run(providerPath, replacement, true));
    assertArrayEquals(replacement, readCredential(providerPath, "alias"));
  }

  @Test
  public void rejectsTruncatedInputWithoutChangingTheStore() throws Exception {
    String providerPath = newProviderPath();
    assertEquals(0, run(providerPath, "original".toCharArray(), false));

    byte[] truncated = new byte[] {0, 0, 0, 5, 'x'};
    assertEquals(
        1,
        CredentialStoreCreate.run(
            arguments(providerPath, true),
            new ByteArrayInputStream(truncated),
            new PrintStream(new ByteArrayOutputStream())));
    assertArrayEquals(
        "original".toCharArray(), readCredential(providerPath, "alias"));
  }

  @Test
  public void clearsPartiallyReadCredentialBytesWhenInputIsTruncated() {
    TrackingTruncatedInputStream input = new TrackingTruncatedInputStream();

    assertEquals(
        1,
        CredentialStoreCreate.run(
            new String[] {"create", "alias", "-provider", "jceks://file/store"},
            input,
            new PrintStream(new ByteArrayOutputStream())));

    assertArrayEquals(new byte[5], input.credentialBuffer);
  }

  @Test
  public void rejectsValueArgumentAndDoesNotCreateAStore() throws Exception {
    File store = temporaryFolder.newFolder().toPath().resolve("store.jceks").toFile();
    String providerPath = "jceks://file" + store.getAbsolutePath();
    String[] arguments = {
      "create", "alias", "-value", "not-on-argv", "-provider", providerPath
    };

    assertEquals(
        1,
        CredentialStoreCreate.run(
            arguments,
            frame("ignored".toCharArray()),
            new PrintStream(new ByteArrayOutputStream())));
    assertNull(readCredential(providerPath, "alias"));
  }

  private String newProviderPath() throws Exception {
    File store = temporaryFolder.newFolder().toPath().resolve("store.jceks").toFile();
    return "jceks://file" + store.getAbsolutePath();
  }

  private int run(String providerPath, char[] credential, boolean overwrite)
      throws Exception {
    return CredentialStoreCreate.run(
        arguments(providerPath, overwrite),
        frame(credential),
        new PrintStream(new ByteArrayOutputStream()));
  }

  private String[] arguments(String providerPath, boolean overwrite) {
    if (overwrite) {
      return new String[] {"create", "alias", "-provider", providerPath, "-f"};
    }
    return new String[] {"create", "alias", "-provider", providerPath};
  }

  private ByteArrayInputStream frame(char[] credential) throws Exception {
    byte[] value = new String(credential).getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    DataOutputStream data = new DataOutputStream(output);
    data.writeInt(value.length);
    data.write(value);
    data.close();
    return new ByteArrayInputStream(output.toByteArray());
  }

  private char[] readCredential(String providerPath, String alias) throws Exception {
    Configuration configuration = new Configuration();
    configuration.set(
        CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerPath);
    List<CredentialProvider> providers =
        CredentialProviderFactory.getProviders(configuration);
    CredentialProvider.CredentialEntry entry =
        providers.get(0).getCredentialEntry(alias);
    return entry == null ? null : entry.getCredential();
  }

  private static final class TrackingTruncatedInputStream extends InputStream {
    private final byte[] value = new byte[] {0, 0, 0, 5, 'x'};
    private int position;
    private byte[] credentialBuffer;

    @Override
    public int read() {
      return position < value.length ? value[position++] & 0xff : -1;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
      credentialBuffer = buffer;
      if (position >= value.length) {
        return -1;
      }
      int count = Math.min(length, value.length - position);
      System.arraycopy(value, position, buffer, offset, count);
      position += count;
      return count;
    }
  }
}
