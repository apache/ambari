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
package org.apache.ambari.server.events.listeners.hostcomponents;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.agent.HeartbeatProcessor.ComponentVersionStructuredOut;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link VersionReportedListener} is used to respond to Ambari events which
 * deal with host components reporting events on startup and registration.
 */
@Singleton
@EagerSingleton
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
public class VersionReportedListener {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(VersionReportedListener.class);

  /**
   * Constructor.
   *
   * @param ambariEventPublisher
   * @param lockFactory
   */
  @Inject
  public VersionReportedListener(VersionEventPublisher versionEventPublisher) {
    versionEventPublisher.register(this);
  }

  /**
   * Handles the {@link HostComponentVersionAdvertisedEvent} which is fired when
   * a component responds with {@link ComponentVersionStructuredOut}. This
   * usually happens on start commands and on agent registration.
   */
  @Subscribe
  public void onVersionReportedEvent(HostComponentVersionAdvertisedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    try {
      ServiceComponentHost sch = event.getServiceComponentHost();
      ComponentVersionStructuredOut stdOut = event.getStructuredOutput();

      String mpackVersion = stdOut.mpackVersion;
      String version = stdOut.version;

      if (StringUtils.isBlank(version)) {
        version = "UNKNOWN";
      }

      if (StringUtils.isBlank(mpackVersion)) {
        mpackVersion = "UNKNOWN";
      }

      String currentVersion = sch.getVersion();
      String currentMpackVersion = sch.getMpackVersion();

      if (!StringUtils.equals(mpackVersion, currentMpackVersion)
          || !StringUtils.equals(version, currentVersion)) {
        try {
          sch.setVersions(mpackVersion, version);
        } catch (AmbariException ambariException) {
          LOG.error("Unable to update the reported version for {} on {}",
              sch.getServiceComponentName(), sch.getHostName(), ambariException);
        }
      }
    } catch (Exception exception) {
      LOG.error("Unable to handle version event {}", event, exception);
    }
  }
}
