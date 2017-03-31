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

package org.apache.ambari.tools.zk;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

/**
 * I'm a command line utility that provides functionality that the official zookeeper-client does not support.
 * E.g. I can set ACLs recursively on a znode.
 */
public class ZkMigrator {
  private static final int SESSION_TIMEOUT_MILLIS = 5000;
  private static final int CONNECTION_TIMEOUT_MILLIS = 30000;

  public static void main(String[] args) throws Exception {
    CommandLine cli = new DefaultParser().parse(options(), args);
    if (cli.hasOption("connection-string") && cli.hasOption("acl") && cli.hasOption("znode")) {
      setAcls(cli.getOptionValue("connection-string"), cli.getOptionValue("znode"), ZkAcl.parse(cli.getOptionValue("acl")));
    } else {
      printHelp();
    }
  }

  private static Options options() {
    return new Options()
      .addOption(Option.builder("h")
        .longOpt("help")
        .desc("print help")
        .build())
      .addOption(Option.builder("c")
        .longOpt("connection-string")
        .desc("zookeeper connection string")
        .hasArg()
        .argName("connection-string")
        .build())
      .addOption(Option.builder("a")
        .longOpt("acl")
        .desc("ACL of a znode in the following format <scheme:id:permission>")
        .hasArg()
        .argName("acl")
        .build())
      .addOption(Option.builder("z")
        .longOpt("znode")
        .desc("znode path")
        .hasArg()
        .argName("znode")
        .build());
  }

  private static void setAcls(String connectionString, String znode, ZkAcl acl) throws IOException, InterruptedException, KeeperException {
    ZooKeeper client = ZkConnection.open(connectionString, SESSION_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS);
    try {
      acl.setRecursivelyOn(client, ZkPathPattern.fromString(znode));
    } catch (KeeperException.NoNodeException e) {
      System.out.println("Could not set ACL on " + znode + ". Reason: " + e.getMessage());
    } finally {
      client.close();
    }
  }

  private static void printHelp() {
    System.out.println("Usage zkmigrator -connection-string <host:port> -acl <scheme:id:permission> -znode /path/to/znode");
    System.exit(1);
  }
}
