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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;

/**
 * Creates a Hadoop credential entry from a length-prefixed UTF-8 value on stdin.
 */
public final class CredentialStoreCreate {
  static final int MAX_CREDENTIAL_BYTES = 1024 * 1024;

  private CredentialStoreCreate() {
  }

  public static void main(String[] args) {
    System.exit(run(args, System.in, System.err));
  }

  static int run(String[] args, InputStream input, PrintStream error) {
    boolean overwrite = args.length == 5 && "-f".equals(args[4]);
    if ((args.length != 4 && !overwrite)
        || !"create".equals(args[0])
        || args[1].isEmpty()
        || !"-provider".equals(args[2])
        || args[3].isEmpty()) {
      error.println(
          "Usage: create <alias> -provider <provider-path> [-f]");
      return 1;
    }

    byte[] credentialBytes = null;
    char[] credential = null;
    try {
      credentialBytes = readCredential(input);
      credential = decodeCredential(credentialBytes);
      Configuration configuration = new Configuration();
      configuration.set(
          CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, args[3]);
      List<CredentialProvider> providers =
          CredentialProviderFactory.getProviders(configuration);
      if (providers.isEmpty()) {
        error.println("No credential provider is available for the requested path");
        return 1;
      }

      CredentialProvider provider = providers.get(0);
      CredentialProvider.CredentialEntry existing =
          provider.getCredentialEntry(args[1]);
      if (existing != null && !overwrite) {
        error.println("Credential alias already exists; use -f to overwrite it");
        return 1;
      }
      if (existing != null) {
        provider.deleteCredentialEntry(args[1]);
      }
      provider.createCredentialEntry(args[1], credential);
      provider.flush();
      return 0;
    } catch (IOException | RuntimeException e) {
      error.println(
          "Credential store update failed: " + e.getClass().getSimpleName());
      return 1;
    } finally {
      if (credentialBytes != null) {
        Arrays.fill(credentialBytes, (byte) 0);
      }
      if (credential != null) {
        Arrays.fill(credential, '\0');
      }
    }
  }

  private static byte[] readCredential(InputStream input) throws IOException {
    DataInputStream data = new DataInputStream(input);
    int length = data.readInt();
    if (length < 0 || length > MAX_CREDENTIAL_BYTES) {
      throw new IOException("Invalid credential length");
    }
    byte[] credential = new byte[length];
    try {
      data.readFully(credential);
      if (data.read() == -1) {
        return credential;
      }
    } catch (IOException | RuntimeException e) {
      Arrays.fill(credential, (byte) 0);
      throw e;
    }
    Arrays.fill(credential, (byte) 0);
    throw new IOException("Unexpected data after credential");
  }

  private static char[] decodeCredential(byte[] credential)
      throws CharacterCodingException {
    CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(credential));
    try {
      char[] result = new char[decoded.remaining()];
      decoded.get(result);
      return result;
    } finally {
      if (decoded.hasArray()) {
        Arrays.fill(decoded.array(), '\0');
      }
    }
  }
}
