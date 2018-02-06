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
package org.apache.ambari.server.topology.validators;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.GPLLicenseNotAcceptedException;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Properties that indicate usage of GPL software are
 * allowed with explicit approval from user.
 */
public class GplPropertiesValidator implements TopologyValidator {

  private static final Logger LOG = LoggerFactory.getLogger(GplPropertiesValidator.class);

  private static final String CORE_SITE = "core-site";
  static final String LZO_CODEC_CLASS_PROPERTY_NAME = "io.compression.codec.lzo.class";
  static final String CODEC_CLASSES_PROPERTY_NAME = "io.compression.codecs";
  static final String LZO_CODEC_CLASS = "com.hadoop.compression.lzo.LzoCodec";

  private static final Set<String> PROPERTY_NAMES = ImmutableSet.of(
    LZO_CODEC_CLASS_PROPERTY_NAME,
    CODEC_CLASSES_PROPERTY_NAME
  );

  private static final String GPL_LICENSE_ERROR_MESSAGE =
    "Your Ambari server has not been configured to download LZO GPL software. " +
    "Please refer to documentation to configure Ambari before proceeding.";

  private final Configuration configuration;

  @Inject
  public GplPropertiesValidator(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    // need to reject blueprints that have LZO enabled if the Ambari Server hasn't been configured for it
    boolean gplEnabled = configuration.getGplLicenseAccepted();

    if (gplEnabled) {
      LOG.info("GPL license accepted, skipping config check");
      return;
    }

    // we don't want to include default stack properties so we can't use full properties
    Map<String, Map<String, String>> clusterConfigurations = topology.getConfiguration().getProperties();

    if (clusterConfigurations != null) {
      for (Map.Entry<String, Map<String, String>> configEntry : clusterConfigurations.entrySet()) {
        String configType = configEntry.getKey();
        if (!CORE_SITE.equals(configType)) {
          continue;
        }

        Map<String, String> properties = configEntry.getValue();
        if (properties == null) {
          continue;
        }

        for (String propertyName : PROPERTY_NAMES) {
          String propertyValue = properties.get(propertyName);
          if (propertyValue != null && propertyValue.contains(LZO_CODEC_CLASS)) {
            throw new GPLLicenseNotAcceptedException(GPL_LICENSE_ERROR_MESSAGE);
          }
        }
      }
    }
  }
}
