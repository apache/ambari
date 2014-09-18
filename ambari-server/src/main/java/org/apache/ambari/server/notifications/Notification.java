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
package org.apache.ambari.server.notifications;

import java.util.List;

/**
 * The {@link Notification} class is a generic way to relay content through an
 * {@link NotificationDispatcher}.
 */
public class Notification {

  /**
   *
   */
  public String Subject;

  /**
   * The main content of the notification.
   */
  public String Body;

  /**
   * An optional callback implementation that the dispatcher can use to report
   * success/failure on delivery.
   */
  public DispatchCallback Callback;

  /**
   * An option list of unique IDs that will identify the origins of this
   * notification.
   */
  public List<String> CallbackIds;

  /**
   * Constructor.
   *
   */
  public Notification() {
  }

}
