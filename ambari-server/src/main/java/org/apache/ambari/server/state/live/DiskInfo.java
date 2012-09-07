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

package org.apache.ambari.server.state.live;

/**
 * Information about a mounted disk on a given node
 */
public class DiskInfo {

  /**
   * Name of device
   * For example: /dev/sda, /dev/sdb, etc
   */
  String device;

  /**
   * Filesystem Type
   * For example: ext3, tmpfs, swap, etc
   */
  String fsType;

  /**
   * Path at which the device is mounted on
   */
  String mountPath;

  /**
   * Capacity of the disk in bytes
   */
  long totalCapacityBytes;
  
  /**
   * Current capacity in bytes
   */
  long currentCapacityBytes;

}
