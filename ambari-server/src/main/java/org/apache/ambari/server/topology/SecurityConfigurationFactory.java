/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import com.google.common.base.Enums;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.ambari.server.orm.dao.KerberosDescriptorDAO;
import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;
import org.apache.ambari.server.state.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class SecurityConfigurationFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigurationFactory.class);

  public static final String SECURITY_PROPERTY_ID = "security";
  public static final String TYPE_PROPERTY_ID = "type";
  public static final String KERBEROS_DESCRIPTOR_PROPERTY_ID = "kerberos_descriptor";
  public static final String KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID = "kerberos_descriptor_reference";

  @Inject
  protected Gson jsonSerializer;

  @Inject
  private KerberosDescriptorDAO kerberosDescriptorDAO;

  @Inject
  private KerberosDescriptorFactory kerberosDescriptorFactory;

  public SecurityConfigurationFactory() {
  }

  protected SecurityConfigurationFactory(Gson jsonSerializer, KerberosDescriptorDAO kerberosDescriptorDAO, KerberosDescriptorFactory kerberosDescriptorFactory) {
    this.jsonSerializer = jsonSerializer;
    this.kerberosDescriptorDAO = kerberosDescriptorDAO;
    this.kerberosDescriptorFactory = kerberosDescriptorFactory;
  }

  /**
   * Creates and also validates SecurityConfiguration based on properties parsed from request Json.
   *
   * @param properties Security properties from Json parsed into a Map
   * @param persistEmbeddedDescriptor whether to save embedded descriptor or not
   * @return
   */
  public SecurityConfiguration createSecurityConfigurationFromRequest(Map<String, Object> properties, boolean
    persistEmbeddedDescriptor) {

    SecurityConfiguration securityConfiguration = null;

    LOGGER.debug("Creating security configuration from properties: {}", properties);
    Map<String, Object> securityProperties = (Map<String, Object>) properties.get(SECURITY_PROPERTY_ID);

    if (securityProperties == null) {
      LOGGER.debug("No security information properties provided, returning null");
      return securityConfiguration;
    }

    String securityTypeString = Strings.emptyToNull((String) securityProperties.get(TYPE_PROPERTY_ID));
    if (securityTypeString == null) {
      LOGGER.error("Type is missing from security block.");
      throw new IllegalArgumentException("Type missing from security block.");
    }

    SecurityType securityType = Enums.getIfPresent(SecurityType.class, securityTypeString).orNull();
    if (securityType == null) {
      LOGGER.error("Unsupported security type specified: {}", securityType);
      throw new IllegalArgumentException("Invalid security type specified: " + securityTypeString);
    }

    if (securityType == SecurityType.KERBEROS) {

      // get security information from the request propertie if any
      String descriptorReference = Strings.emptyToNull((String)
          securityProperties.get(KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID));

      Object descriptorJsonMap = securityProperties.get(KERBEROS_DESCRIPTOR_PROPERTY_ID);

      if (descriptorReference != null && descriptorJsonMap != null) {
        LOGGER.error("Both kerberos descriptor and kerberos descriptor reference are set in the security configuration!");
        throw new IllegalArgumentException("Usage of properties : " + KERBEROS_DESCRIPTOR_PROPERTY_ID + " and "
            + KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID + " at the same time, is not allowed.");
      }

      String descriptorText = null;

      if (descriptorJsonMap != null) { // this means the reference is null
        LOGGER.debug("Found embedded descriptor: {}", descriptorJsonMap);
        descriptorText = jsonSerializer.<Map<String, Object>>toJson(descriptorJsonMap, Map.class);
        if (persistEmbeddedDescriptor) {
          descriptorReference = persistKerberosDescriptor(descriptorText);
        }
        securityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS, descriptorReference, descriptorText);
      } else if (descriptorReference != null) { // this means the reference is not null
        LOGGER.debug("Found descriptor reference: {}", descriptorReference);
        securityConfiguration = loadSecurityConfigurationByReference(descriptorReference);
      } else {
        LOGGER.debug("There is no security descriptor found in the request");
        securityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS);
      }
    } else {
      LOGGER.debug("There is no security configuration found in the request");
      securityConfiguration = new SecurityConfiguration(SecurityType.NONE);
    }
    return securityConfiguration;
  }

  public SecurityConfiguration loadSecurityConfigurationByReference(String reference) {
    SecurityConfiguration securityConfiguration = null;
    LOGGER.debug("Loading security configuration by reference: {}", reference);

    if (reference == null) {
      LOGGER.error("No security configuration reference provided!");
      throw new IllegalArgumentException("No security configuration reference provided!");
    }

    KerberosDescriptorEntity descriptorEntity = kerberosDescriptorDAO.findByName(reference);

    if (descriptorEntity == null) {
      LOGGER.error("No security configuration found for the reference: {}", reference);
      throw new IllegalArgumentException("No security configuration found for the reference: " + reference);
    }

    securityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS, reference, descriptorEntity.getKerberosDescriptorText());

    return securityConfiguration;

  }

  private String persistKerberosDescriptor(String descriptor) {
    LOGGER.debug("Generating new kerberos descriptor reference ...");
    String kdReference = generateKerberosDescriptorReference();

    KerberosDescriptor kerberosDescriptor = kerberosDescriptorFactory.createKerberosDescriptor(kdReference, descriptor);

    LOGGER.debug("Persisting kerberos descriptor ...");
    kerberosDescriptorDAO.create(kerberosDescriptor.toEntity());
    return kdReference;
  }

  // generates a unique name for the kerberos descriptor for further reference
  private String generateKerberosDescriptorReference() {
    String kdReference = UUID.randomUUID().toString();
    LOGGER.debug("Generated new kerberos descriptor reference: {}", kdReference);
    return kdReference;
  }


}
