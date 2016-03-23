/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.ambari.server.audit.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

/**
 * Default audit logger implementation, logs messages into log file
 */
@Singleton
public class AuditLoggerDefaultImpl implements AuditLogger {

  private static final Logger LOG = LoggerFactory.getLogger("audit");

  /**
   * {@inheritDoc}
   */
  @Override
  @Subscribe
  public void log(AuditEvent event) {
    Date date = new Date(event.getTimestamp());
    //2016-03-11T10:42:36.376Z
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    LOG.info("{}, {}", dateFormat.format(date), event.getAuditMessage());
  }
}
