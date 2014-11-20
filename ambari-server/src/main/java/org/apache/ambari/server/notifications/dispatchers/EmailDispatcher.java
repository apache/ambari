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

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.ambari.server.notifications.DispatchCredentials;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Recipient;
import org.apache.ambari.server.state.alert.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * The {@link EmailDispatcher} class is used to dispatch {@link Notification}
 * via JavaMail. This class currently does not attempt to reuse an existing
 * {@link Session} or {@link Transport} since each {@link Notification} could
 * have a different target server with different properties.
 * <p/>
 * In the future, this class could keep various {@link Transport} instances open
 * on a {@link Timer}, but those instances would need to be hashed so that the
 * proper instance is retrieved from the properties of the incoming
 * {@link Notification}.
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

    if (null == notification.DispatchProperties) {
      LOG.error("Unable to dispatch an email notification that does not contain SMTP properties");

      if (null != notification.Callback) {
        notification.Callback.onFailure(notification.CallbackIds);
      }

      return;
    }

    // convert properties to JavaMail properties
    Properties properties = new Properties();
    for (Entry<String, String> entry : notification.DispatchProperties.entrySet()) {
      properties.put(entry.getKey(), entry.getValue());
    }

    // notifications must have recipients
    if (null == notification.Recipients) {
      LOG.error("Unable to dispatch an email notification that does not have recipients");

      if (null != notification.Callback) {
        notification.Callback.onFailure(notification.CallbackIds);
      }

      return;
    }

    // create a simple email authentication for username/password
    final Session session;
    EmailAuthenticator authenticator = null;

    if (null != notification.Credentials) {
      authenticator = new EmailAuthenticator(notification.Credentials);
    }

    session = Session.getInstance(properties, authenticator);

    try {
      // !!! at some point in the future we can worry about multipart
      MimeMessage message = new MimeMessage(session);

      for (Recipient recipient : notification.Recipients) {
        InternetAddress address = new InternetAddress(recipient.Identifier);
        message.addRecipient(RecipientType.TO, address);
      }

      message.setSubject(notification.Subject);
      message.setText(notification.Body, "UTF-8", "html");

      Transport.send(message);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Successfully dispatched email to {}",
            notification.Recipients);
      }

      // callback to inform the interested parties about the successful dispatch
      if (null != notification.Callback) {
        notification.Callback.onSuccess(notification.CallbackIds);
      }
    } catch (Exception exception) {
      LOG.error("Unable to dispatch notification via Email", exception);

      // callback failure
      if (null != notification.Callback) {
        notification.Callback.onFailure(notification.CallbackIds);
      }
    } finally {
      try {
        session.getTransport().close();
      } catch (MessagingException me) {
        LOG.warn("Dispatcher unable to close SMTP transport", me);
      }
    }
  }

  /**
   * The {@link EmailAuthenticator} class is used to provide a username and
   * password combination to an SMTP server.
   */
  private static final class EmailAuthenticator extends Authenticator{

    private final DispatchCredentials m_credentials;

    /**
     * Constructor.
     *
     * @param credentials
     */
    private EmailAuthenticator(DispatchCredentials credentials) {
      m_credentials = credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(m_credentials.UserName,
          m_credentials.Password);
    }
  }
}
