/**
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
package org.apache.ambari.metrics.core.loadsimulator.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlService {

  public static final int CONNECT_TIMEOUT = 20000;
  public static final int READ_TIMEOUT = 20000;
  private final String address;
  private HttpURLConnection conn;

  private UrlService(String address) {
    this.address = address;
  }

  /**
   * Returns a new UrlService connected to specified address.
   *
   * @param address
   * @return
   * @throws IOException
   */
  public static UrlService newConnection(String address) throws IOException {
    UrlService svc = new UrlService(address);
    svc.connect();

    return svc;
  }

  public HttpURLConnection connect() throws IOException {
    URL url = new URL(address);
    conn = (HttpURLConnection) url.openConnection();

    //TODO: make timeouts configurable
    conn.setConnectTimeout(CONNECT_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);
    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Accept", "*/*");

    return conn;
  }

  public String send(String payload) throws IOException {
    if (conn == null)
      throw new IllegalStateException("Cannot use unconnected UrlService");
    write(payload);

    return read();
  }

  private String read() throws IOException {
    StringBuilder response = new StringBuilder();

    BufferedReader br = new BufferedReader(new InputStreamReader(
      conn.getInputStream()));
    String line = null;
    while ((line = br.readLine()) != null) {
      response.append(line);
    }
    br.close();

    return response.toString();
  }

  private void write(String payload) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(),
      "UTF-8");
    writer.write(payload);
    writer.close();
  }

  public void disconnect() {
    conn.disconnect();
  }
}
