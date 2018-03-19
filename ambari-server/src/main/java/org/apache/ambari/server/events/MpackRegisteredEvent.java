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
package org.apache.ambari.server.events;

/**
 * The {@link MpackRegisteredEvent} is fired when a management pack is registerd
 * with Ambari.
 */
public class MpackRegisteredEvent extends AmbariEvent {
  /**
   * The management pack ID.
   */
  protected final long m_mpackId;

  /**
   * Constructor.
   *
   * @param eventType
   * @param clusterId
   */
  public MpackRegisteredEvent(long m_mpackId) {
    super(AmbariEventType.MPACK_REGISTERED);
    this.m_mpackId = m_mpackId;
  }

  /**
   * Gets the management pack ID that the event belongs to.
   *
   * @return the ID of the management pack.
   */
  public long getMpackIdId() {
    return m_mpackId;
  }
}
