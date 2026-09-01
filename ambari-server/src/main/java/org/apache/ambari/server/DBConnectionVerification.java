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

package org.apache.ambari.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

public final class DBConnectionVerification {
  static final int MAX_PASSWORD_BYTES = 1024 * 1024;

  private DBConnectionVerification() {
  }

  public static void main(String[] args) {
    System.exit(run(args, System.in, System.out));
  }

  static int run(String[] args, InputStream input, PrintStream output) {
    if (args.length != 3
        || args[0].isEmpty()
        || args[1].isEmpty()
        || args[2].isEmpty()) {
      output.println("Usage: <jdbc-url> <username> <jdbc-driver>");
      return 1;
    }

    char[] passwordCharacters = null;
    try {
      passwordCharacters = readPassword(input);
      Class.forName(args[2]);
      try (Connection ignored = args[0].contains("integratedSecurity=true")
          ? DriverManager.getConnection(args[0])
          : DriverManager.getConnection(
              args[0], args[1], new String(passwordCharacters))) {
        output.println("Connected to DB Successfully!");
        return 0;
      }
    } catch (Throwable error) {
      output.println(
          "ERROR: Unable to connect to the DB. Please check DB connection properties.");
      output.println(error.getClass().getSimpleName());
      return 1;
    } finally {
      if (passwordCharacters != null) {
        Arrays.fill(passwordCharacters, '\0');
      }
    }
  }

  private static char[] readPassword(InputStream input) throws IOException {
    DataInputStream data = new DataInputStream(input);
    int length = data.readInt();
    if (length < 0 || length > MAX_PASSWORD_BYTES) {
      throw new IOException("Invalid password length");
    }

    byte[] passwordBytes = new byte[length];
    try {
      data.readFully(passwordBytes);
      if (data.read() != -1) {
        throw new IOException("Unexpected data after password");
      }
      return decodePassword(passwordBytes);
    } finally {
      Arrays.fill(passwordBytes, (byte) 0);
    }
  }

  private static char[] decodePassword(byte[] password)
      throws CharacterCodingException {
    CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(password));
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
