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

package org.apache.hms.common.conf;

/**
 * General HMS configuration parameter definitions
 *
 */
public class CommonConfigurationKeys {

  /** Location of zookeeper servers */
  public static final String ZOOKEEPER_ADDRESS_KEY = "hms.zookeeper.address";
  /** Default location of zookeeper servers */
  public static final String ZOOKEEPER_ADDRESS_DEFAULT = "localhost:2181";
  
  /** Path to zookeeper cluster root */
  public static final String ZOOKEEPER_CLUSTER_ROOT_KEY = "hms.zookeeper.cluster.path";
  /** Default location of zookeeper cluster root */
  public static final String ZOOKEEPER_CLUSTER_ROOT_DEFAULT = "/clusters";
  
  /** Path to zookeeper command queue */
  public static final String ZOOKEEPER_COMMAND_QUEUE_PATH_KEY = "hms.zookeeper.command.queue.path";
  /** Default location of zookeeper command queue */
  public static final String ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT = "/cmdqueue";
  
  /** Path to zookeeper live controller queue */
  public static final String ZOOKEEPER_LIVE_CONTROLLER_PATH_KEY = "hms.zookeeper.live.controller.path";
  /** Default location of zookeeper live controller queue */
  public static final String ZOOKEEPER_LIVE_CONTROLLER_PATH_DEFAULT = "/livecontrollers";
  
  /** Path to zookeeper lock queue */
  public static final String ZOOKEEPER_LOCK_QUEUE_PATH_KEY = "hms.zookeeper.lock.queue.path";
  /** Default location of zookeeper lock queue */
  public static final String ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT = "/locks";
 
  /** Reference key for path to nodes manifest */
  public static final String ZOOKEEPER_NODES_MANIFEST_KEY = "hms.nodes.manifest.path";
  /** Default location of nodes manifest */
  public static final String ZOOKEEPER_NODES_MANIFEST_PATH_DEFAULT = "/nodes-manifest";
  
  /** Zeroconf zookeeper type */
  public static final String ZEROCONF_ZOOKEEPER_TYPE = "_zookeeper._tcp.local.";
  
  /** Path to zookeeper status qeueue */
  public static final String ZOOKEEPER_STATUS_QUEUE_PATH_KEY = "hms.zookeeper.status.queue.path";
  /** Default location of zookeeper status queue */
  public static final String ZOOKEEPER_STATUS_QUEUE_PATH_DEFAULT = "/status";
  
  /** Path to zookeeper software manifest */
  public static final String ZOOKEEPER_SOFTWARE_MANIFEST_KEY = "hms.software.manifest.path";
  
  /** Default location of software manifest */
  public static final String ZOOKEEPER_SOFTWARE_MANIFEST_PATH_DEFAULT = "/software-manifest";
  
  /** Path to zookeeper config blueprint */
  public static final String ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT = "/config-blueprint";
}
