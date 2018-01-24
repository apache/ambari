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

package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.RootServiceComponentConfiguration;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * AmbariServerLDAPConfigurationHandler handles Ambari server LDAP-specific configuration properties.
 */
@StaticallyInject
class AmbariServerLDAPConfigurationHandler extends AmbariServerConfigurationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerLDAPConfigurationHandler.class);

  @Inject
  private static LdapFacade ldapFacade;
  
  @Override
  public OperationResult performOperation(String categoryName, Map<String, String> properties,
                                          boolean mergeExistingProperties, String operation, Map<String, Object> operationParameters) throws SystemException {

    if (!AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName().equals(categoryName)) {
      throw new SystemException(String.format("Unexpected category name for Ambari server LDAP properties: %s", categoryName));
    }

    OperationType operationType = OperationType.translate(operation);
    if (operation == null) {
      throw new SystemException("Unexpected operation for Ambari server LDAP properties");
    }

    Map<String, String> ldapConfigurationProperties = new HashMap<>();

    // If we need to merge with the properties of an existing ldap-configuration property set, attempt
    // to retrieve if. If one does not exist, that is ok.
    if (mergeExistingProperties) {
      try {
        Map<String, RootServiceComponentConfiguration> _configurations = getConfigurations(categoryName);
        if (_configurations != null) {
          RootServiceComponentConfiguration _ldapProperties = _configurations.get(categoryName);

          if (_ldapProperties != null) {
            ldapConfigurationProperties.putAll(_ldapProperties.getProperties());
          }
        }
      } catch (NoSuchResourceException e) {
        // Ignore this. There is no existing ldap-configuration category and that is ok.
      }
    }

    if (properties != null) {
      ldapConfigurationProperties.putAll(properties);
    }

    AmbariLdapConfiguration ambariLdapConfiguration = new AmbariLdapConfiguration(ldapConfigurationProperties);

    boolean success = true;
    String message = null;
    Object resultData = null;

    try {
      switch (operationType) {
        case TEST_CONNECTION:
          LOGGER.debug("Testing connection to the LDAP server ...");
          ldapFacade.checkConnection(ambariLdapConfiguration);
          break;

        case TEST_ATTRIBUTES:
          LOGGER.debug("Testing LDAP attributes ....");
          Set<String> groups = ldapFacade.checkLdapAttributes(operationParameters, ambariLdapConfiguration);
          resultData = Collections.singletonMap("groups", groups);
          break;

        case DETECT_ATTRIBUTES:
          LOGGER.info("Detecting LDAP attributes ...");
          ambariLdapConfiguration = ldapFacade.detectAttributes(ambariLdapConfiguration);
          resultData = Collections.singletonMap("attributes", ambariLdapConfiguration.toMap());
          break;

        default:
          LOGGER.warn("No action provided ...");
          throw new IllegalArgumentException("No request action provided");
      }
    } catch (AmbariLdapException e) {
      success = false;
      message = determineCause(e);
      if (StringUtils.isEmpty(message)) {
        message = "An unexpected error has occurred.";
      }

      LOGGER.warn(String.format("Failed to perform %s: %s", operationType.name(), message), e);
    }

    return new OperationResult(operationType.getOperation(), success, message, resultData);
  }

  private String determineCause(Throwable throwable) {
    if (throwable == null) {
      return null;
    } else {
      Throwable cause = throwable.getCause();
      if ((cause == null) || (cause == throwable)) {
        return throwable.getMessage();
      } else {
        String message = determineCause(cause);
        return (message == null) ? throwable.getMessage() : message;
      }
    }
  }

  enum OperationType {
    TEST_CONNECTION("test-connection"),
    TEST_ATTRIBUTES("test-attributes"),
    DETECT_ATTRIBUTES("detect-attributes");

    private final String operation;

    OperationType(String operation) {
      this.operation = operation;
    }

    public String getOperation() {
      return operation;
    }

    public static OperationType translate(String operation) {
      if (!StringUtils.isEmpty(operation)) {
        operation = operation.trim();
        for (OperationType category : values()) {
          if (category.getOperation().equals(operation)) {
            return category;
          }
        }
      }

      throw new IllegalArgumentException(String.format("Invalid Ambari server configuration category name: %s", operation));
    }

    public static String translate(OperationType operation) {
      return (operation == null) ? null : operation.getOperation();
    }
  }
}
