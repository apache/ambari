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
package org.apache.hadoop.metrics2.sink.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * Helpers to handle server addresses
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class Servers {
  /**
   * This class is not intended to be instantiated
   */
  private Servers() {}

  /**
   * Parses a space and/or comma separated sequence of server specifications
   * of the form <i>hostname</i> or <i>hostname:port</i>.  If
   * the specs string is null, defaults to localhost:defaultPort.
   *
   * @param specs   server specs (see description)
   * @param defaultPort the default port if not specified
   * @return a list of InetSocketAddress objects.
   */
  public static List<InetSocketAddress> parse(String specs, int defaultPort) {
    List<InetSocketAddress> result = new ArrayList<InetSocketAddress>();
    if (specs == null) {
      result.add(new InetSocketAddress("localhost", defaultPort));
    } else {
      String[] specStrings = specs.split("[ ,]+");
      for (String specString : specStrings) {
        result.add(createSocketAddr(specString, defaultPort));
      }
    }
    return result;
  }

  /**
   * @param host
   * @param port
   * @return a InetSocketAddress created with the specified host and port
   */
  private static InetSocketAddress createSocketAddr(String target, int defaultPort) {
    String helpText = "";
    if (target == null) {
      throw new IllegalArgumentException("Target address cannot be null." + helpText);
    }
    boolean hasScheme = target.contains("://");
    URI uri = null;
    try {
      uri = hasScheme ? URI.create(target) : URI.create("dummyscheme://" + target);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Does not contain a valid host:port authority: " + target + helpText);
    }

    String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1) {
      port = defaultPort;
    }
    String path = uri.getPath();

    if ((host == null) || (port < 0) || (!hasScheme && path != null && !path.isEmpty())) {
      throw new IllegalArgumentException("Does not contain a valid host:port authority: " + target + helpText);
    }
    return createSocketAddrForHost(host, port);
  }

  /**
   * @param host
   * @param port
   * @return a InetSocketAddress created with the specified host and port
   */
  private static InetSocketAddress createSocketAddrForHost(String host, int port) {
    InetSocketAddress addr;
    try {
      InetAddress iaddr = InetAddress.getByName(host);
      iaddr = InetAddress.getByAddress(host, iaddr.getAddress());
      addr = new InetSocketAddress(iaddr, port);
    } catch (UnknownHostException e) {
      addr = InetSocketAddress.createUnresolved(host, port);
    }
    return addr;
  }

}
