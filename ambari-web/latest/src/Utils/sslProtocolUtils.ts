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

/**
 * SSL Protocol Utilities - Following Ember.js setProtocol patterns
 * Implements sophisticated SSL detection logic for different services and components
 */

interface ProtocolConfig {
  type?: string;
  checks?: Array<{
    site: string;
    property: string;
    desired: string;
  }>;
}

interface ConfigProperty {
  type: string;
  properties: { [key: string]: string };
}

/**
 * Services that support HTTPS with specific SSL configurations
 * Following Ember.js servicesSupportsHttps pattern
 */
export const SERVICES_SUPPORTS_HTTPS = ["HDFS", "HBASE", "PINOT"];

/**
 * Check if desired condition is met for SSL configuration
 * Following Ember.js meetDesired pattern
 */
const meetDesired = (
  configProperties: ConfigProperty[],
  configType: string,
  property: string,
  desiredState: string
): boolean => {
  const currentConfig = configProperties.find(config => config.type === configType);
  
  if (!currentConfig) {
    return false;
  }
  
  const currentPropertyValue = currentConfig.properties[property];
  
  if ('NOT_EXIST' === desiredState) {
    return currentPropertyValue == null || currentPropertyValue === undefined;
  }
  if ('EXIST' === desiredState) {
    return currentPropertyValue != null && currentPropertyValue !== undefined;
  }
  return desiredState === currentPropertyValue;
};

/**
 * Reverse protocol type for fallback logic
 * Following Ember.js reverseType pattern
 */
const reverseType = (type: string): string => {
  if ('https' === type) {
    return 'http';
  }
  if ('http' === type) {
    return 'https';
  }
  return '';
};

/**
 * Main protocol determination function following Ember.js setProtocol logic
 * Determines protocol based on service configuration and SSL settings
 */
export const setProtocol = (
  configProperties: ConfigProperty[],
  protocolConfig?: ProtocolConfig
): string => {
  // Check global Hadoop SSL setting
  let hadoopSslEnabled = false;
  
  if (configProperties && configProperties.length > 0) {
    const hdfsSite = configProperties.find(config => config.type === 'hdfs-site');
    hadoopSslEnabled = !!(hdfsSite && 
                         hdfsSite.properties && 
                         hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY');
  }

  // If no protocol config, use global Hadoop SSL setting
  if (!protocolConfig) {
    return hadoopSslEnabled ? 'https' : 'http';
  }

  const protocolType = protocolConfig.type;

  // Explicit protocol type overrides
  if ('HTTPS_ONLY' === protocolType) {
    return 'https';
  }
  if ('HTTP_ONLY' === protocolType) {
    return 'http';
  }

  // If no checks defined, use global Hadoop SSL setting
  const checks = protocolConfig.checks;
  if (!checks) {
    return hadoopSslEnabled ? 'https' : 'http';
  }

  // Process protocol checks
  const protocolTypeLower = protocolType?.toLowerCase() || 'http';
  let failedChecks = 0;
  
  checks.forEach(check => {
    const configType = check.site;
    const property = check.property;
    const desiredState = check.desired;
    const checkMet = meetDesired(configProperties, configType, property, desiredState);
    
    if (!checkMet) {
      failedChecks++;
    }
  });

  // If any checks failed, reverse the protocol type
  return failedChecks > 0 ? reverseType(protocolTypeLower) : protocolTypeLower;
};

/**
 * Service-specific SSL configuration patterns
 * Based on Ember.js quick_links.js FIXTURES
 */
export const SERVICE_SSL_CONFIGS = {
  HDFS: {
    supportsHttps: true,
    httpConfig: 'dfs.namenode.http-address',
    httpsConfig: 'dfs.namenode.https-address',
    site: 'hdfs-site',
    defaultHttpPort: 50070,
    defaultHttpsPort: 50470
  },
  YARN: {
    supportsHttps: true,
    httpConfig: 'yarn.resourcemanager.webapp.address',
    httpsConfig: 'yarn.resourcemanager.webapp.https.address',
    site: 'yarn-site',
    defaultHttpPort: 8088,
    defaultHttpsPort: 8090
  },
  MAPREDUCE2: {
    supportsHttps: true,
    httpConfig: 'mapreduce.jobhistory.webapp.address',
    httpsConfig: 'mapreduce.jobhistory.webapp.https.address',
    site: 'mapred-site',
    defaultHttpPort: 19888,
    defaultHttpsPort: 19888
  },
  HBASE: {
    supportsHttps: false,
    httpConfig: 'hbase.master.info.port',
    site: 'hbase-site',
    defaultHttpPort: 60010
  },
  RANGER: {
    supportsHttps: true,
    httpConfig: 'http.service.port',
    httpsConfig: 'https.service.port',
    site: 'ranger-site',
    defaultHttpPort: 6080,
    defaultHttpsPort: 6182,
    specialExternalUrl: 'policymgr_external_url', // Special Ranger handling
    externalUrlSite: 'admin-properties'
  },
  ATLAS: {
    supportsHttps: true,
    httpConfig: 'atlas.server.http.port',
    httpsConfig: 'atlas.server.https.port',
    site: 'application-properties',
    defaultHttpPort: 21000,
    defaultHttpsPort: 21443
  },
  AMBARI_METRICS: {
    supportsHttps: false,
    httpConfig: 'port',
    site: 'ams-grafana-ini',
    defaultHttpPort: 3000,
    defaultHttpsPort: 3000
  },
  ACCUMULO: {
    supportsHttps: true,
    httpConfig: 'monitor.port.client',
    httpsConfig: 'monitor.port.client', // Same property
    site: 'accumulo-site',
    defaultHttpPort: 50095,
    defaultHttpsPort: 50095
  },
  LOGSEARCH: {
    supportsHttps: true,
    httpConfig: 'logsearch_ui_port',
    httpsConfig: 'logsearch_ui_port', // Same property
    site: 'logsearch-env',
    defaultHttpPort: 61888,
    defaultHttpsPort: 61888
  },
  STORM: {
    supportsHttps: false,
    httpConfig: 'ui.port',
    site: 'storm-site',
    defaultHttpPort: 8744
  },
  FALCON: {
    supportsHttps: false,
    httpConfig: 'falcon_port',
    site: 'falcon-env',
    defaultHttpPort: 15000
  },
  SPARK: {
    supportsHttps: false,
    httpConfig: 'spark.history.ui.port',
    site: 'spark-defaults',
    defaultHttpPort: 18080
  },
  PINOT: {
    supportsHttps: true,
    httpConfig: 'controller.access.protocols.http.port',
    httpsConfig: 'controller.access.protocols.https.port',
    site: 'pinot-controller-conf',
    defaultHttpPort: 9000,
    defaultHttpsPort: 8444,
    sslCheckProperty: 'enable.tls',
    sslCheckSite: 'pinot-common-conf'
  }
};

/**
 * Get SSL configuration for a specific service
 */
export const getServiceSSLConfig = (serviceName: string) => {
  return SERVICE_SSL_CONFIGS[serviceName as keyof typeof SERVICE_SSL_CONFIGS] || null;
};

/**
 * Determine if service supports HTTPS
 */
export const serviceSupportsHttps = (serviceName: string): boolean => {
  const config = getServiceSSLConfig(serviceName);
  return config?.supportsHttps || false;
};

/**
 * Get appropriate protocol for a service based on its configuration
 * Following Ember.js service-specific protocol logic
 */
export const getServiceProtocol = (
  _serviceName: string,
  configProperties: ConfigProperty[],
  protocolConfig?: ProtocolConfig
): string => {
  return setProtocol(configProperties, protocolConfig);
};
