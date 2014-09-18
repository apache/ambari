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
package org.apache.ambari.server.notifications.dispatchers;

import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.state.alert.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * The {@link EmailDispatcher} class is used to dispatch {@link Notification}
 * via JavaMail.
 */
@Singleton
public class EmailDispatcher implements NotificationDispatcher {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(EmailDispatcher.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return TargetType.EMAIL.name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatch(Notification notification) {
    LOG.info("Sending email: {}", notification);

    // callback to inform the interested parties about the successful dispatch
    if (null != notification.Callback) {
      notification.Callback.onSuccess(notification.CallbackIds);
    }
  }
}
