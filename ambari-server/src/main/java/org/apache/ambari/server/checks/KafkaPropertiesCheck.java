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
package org.apache.ambari.server.checks;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.utils.VersionUtils;

import com.google.inject.Singleton;
import com.google.common.collect.Lists;

/**
 * Check Kafka configuration properties before upgrade.
 *
 * Property to check:
 *  inter.broker.protocol.version
 *  log.message.format.version
 *
 * These configurations must exist and have a value (the value for both should be aligned with the Kafka version on the current stack).
 *
 * For inter.broker.protocol.version :
 * value is not empty
 * value is set to current Kafka version in the stack (e.g. for HDP 2.6.x value should be 0.10.1, HDP 2.5.x should be 0.10.0, HDP 2.3x - 2.4x should be 0.9.0)
 *
 * For log.message.format.version:
 * value is not empty (version can vary from current stack version)
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT)
public class KafkaPropertiesCheck extends AbstractCheckDescriptor {
  private static String KAFKA_BROKER_CONFIG = "kafka-broker";
  private static String KAFKA_SERVICE_NAME = "KAFKA";
  private static String MIN_APPLICABLE_STACK_VERSION = "2.6.5";

  private interface KafkaProperties{
    String INTER_BROKER_PROTOCOL_VERSION = "inter.broker.protocol.version";
    String LOG_MESSAGE_FORMAT_VERSION = "log.message.format.version";
    List<String> ALL_PROPERTIES = Arrays.asList(INTER_BROKER_PROTOCOL_VERSION, LOG_MESSAGE_FORMAT_VERSION);
  }

  /**
   * Constructor.
   */
  public KafkaPropertiesCheck() {
    super(CheckDescription.KAFKA_PROPERTIES_VALIDATION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CheckQualification> getQualifications() {
    return Lists.<CheckQualification> newArrayList(new KafkaPropertiesMinVersionQualification());
  }


  private String getKafkaServiceVersion(Cluster cluster)throws AmbariException{
    ServiceInfo serviceInfo = ambariMetaInfo.get().getStack(cluster.getCurrentStackVersion()).getService(KAFKA_SERVICE_NAME);

    if (serviceInfo == null) {
      return null;
    }

    String[] version = serviceInfo.getVersion().split(Pattern.quote("."));
    if (version.length < 3) {
      return null;
    }
    return String.format("%s.%s.%s", version[0], version[1], version[2]);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    LinkedHashSet<String> failedProperties = new LinkedHashSet<>();

    for (String propertyName: KafkaProperties.ALL_PROPERTIES){
      String propertyValue = getProperty(cluster, KAFKA_BROKER_CONFIG, propertyName);

      if (propertyValue == null) {
        failedProperties.add(propertyName);
      } else if (propertyName.equals(KafkaProperties.INTER_BROKER_PROTOCOL_VERSION)) {
        String stackKafkaVersion = getKafkaServiceVersion(cluster);

        if (stackKafkaVersion != null && !stackKafkaVersion.equals(propertyValue)) {
          failedProperties.add(String.format("%s (expected value \"%s\", actual \"%s\")",
            propertyName, stackKafkaVersion, propertyValue));
        }
      }
    }

    if (!failedProperties.isEmpty()){
      prerequisiteCheck.setFailedOn(failedProperties);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
    }
  }

  /**
   * Stack version, to which check should be applicable
   */
  private class KafkaPropertiesMinVersionQualification implements CheckQualification {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(PrereqCheckRequest request) {
      String minApplicableStackVersion = MIN_APPLICABLE_STACK_VERSION;
      String targetStackVersion =  request.getTargetRepositoryVersion().getVersion();

      return  VersionUtils.compareVersions(targetStackVersion, minApplicableStackVersion) >= 0;
    }
  }
}
