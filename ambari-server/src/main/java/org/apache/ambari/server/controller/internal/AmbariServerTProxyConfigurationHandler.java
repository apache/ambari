/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * AmbariServerTProxyConfigurationHandler is an {@link AmbariServerConfigurationHandler} implementation
 * handing changes to the tproxy (trusted proxy) configuration
 */
@Singleton
public class AmbariServerTProxyConfigurationHandler extends AmbariServerConfigurationHandler {

  @Inject
  public AmbariServerTProxyConfigurationHandler(AmbariConfigurationDAO ambariConfigurationDAO, AmbariEventPublisher publisher, Configuration ambariConfiguration) {
    super(ambariConfigurationDAO, publisher, ambariConfiguration);
  }
}
