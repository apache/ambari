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

import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;


/**
 * Atlas needs to migrate existing data from TitanDB to JanusGraph.
 * To migrate existing data to JanusGraph the property atlas.migration.data.filename needs to be present in Atlas applicaton.properties.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.INFORMATIONAL_WARNING)
public class AtlasMigrationPropertyCheck extends AbstractCheckDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasMigrationPropertyCheck.class);
    private static final String serviceName = "ATLAS";

    /**
     * Default constructor
     */
    public AtlasMigrationPropertyCheck(){  super(CheckDescription.ATLAS_MIGRATION_PROPERTY_CHECK); }

    /**
     * {@inheritDoc}
     */
    public Set<String> getApplicableServices() { return Sets.newHashSet(serviceName); }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
      String atlasMigrationProperty = getProperty(request,"application-properties","atlas.migration.data.filename");
      if(StringUtils.isBlank(atlasMigrationProperty)) {
        LOG.info("The property atlas.migration.data.filename is not found in application-properties, need to add the property before upgrade.");
        prerequisiteCheck.getFailedOn().add(serviceName);
        prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
        prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
      } else if (atlasMigrationProperty.contains("/etc/atlas/conf")) {
          LOG.info("The property atlas.migration.data.filename is found in application-properties, but it contains atlas conf path ie /etc/atlas/conf. Avoid using conf path for this property.");
          prerequisiteCheck.getFailedOn().add(serviceName);
          prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
          prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
      }
    }
}
