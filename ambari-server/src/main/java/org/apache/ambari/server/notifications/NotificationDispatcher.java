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


/**
 * The {@link NotificationDispatcher} interface represents a mechanism for dispatching a
 * {@link Notification}.
 * <p/>
 * Dispatchers should, in general, be singletons. They should also invoke the
 * appropriate methods on {@link Notification#Callback} to indicate a success or
 * failure during dispatch.
 */
public interface NotificationDispatcher {

  /**
   * Gets the type of dispatcher. The type of each different dispatcher should
   * be unique.
   *
   * @return the dispatcher's type (never {@code null}).
   */
  public String getType();

  /**
   * Dispatches the specified notification.
   *
   * @param notification
   *          the notificationt to dispatch (not {@code null}).
   */
  public void dispatch(Notification notification);

}
