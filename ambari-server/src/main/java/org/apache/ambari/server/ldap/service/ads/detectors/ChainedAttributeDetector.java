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

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Attribute detector implementation that performs the attribute detection on a configured set of attribute detectors.
 * (it implements the composite design pattern)
 */
@Singleton
public class ChainedAttributeDetector implements AttributeDetector<Entry> {

  private static final Logger LOG = LoggerFactory.getLogger(ChainedAttributeDetector.class);

  /**
   * The set of detectors this instance delegates to
   */
  private final Set<AttributeDetector> detectors;

  @Inject
  public ChainedAttributeDetector(Set<AttributeDetector> detectors) {
    this.detectors = detectors;
  }

  @Override
  public void collect(Entry entry) {
    for (AttributeDetector detector : detectors) {
      LOG.info("Collecting information for the detector: [{}]", detector);
      detector.collect(entry);
    }
  }

  @Override
  public Map<String, String> detect() {
    Map<String, String> detectedAttributes = Maps.newHashMap();
    for (AttributeDetector detector : detectors) {
      LOG.info("Detecting ldap configuration value using the detector: [{}]", detector);
      detectedAttributes.putAll(detector.detect());
    }
    return detectedAttributes;
  }

  @Override
  public String toString() {
    return "ChainedAttributeDetector{" +
      "detectors=" + detectors +
      '}';
  }
}
