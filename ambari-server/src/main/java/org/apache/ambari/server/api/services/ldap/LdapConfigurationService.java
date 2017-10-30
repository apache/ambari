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

package org.apache.ambari.server.api.services.ldap;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariConfigurationService;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.google.common.collect.Sets;

/**
 * Endpoint designated to LDAP specific operations.
 */
@StaticallyInject
@Path("/ldapconfigs/")
public class LdapConfigurationService extends AmbariConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigurationService.class);

  @Inject
  private static LdapFacade ldapFacade;

  @Inject
  private static AmbariLdapConfigurationFactory ambariLdapConfigurationFactory;


  @POST
  @ApiIgnore // until documented
  @Path("/validate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateConfiguration(LdapConfigurationRequest ldapConfigurationRequest) {

    // check if the user is authorized to perform the operation
    authorize();

    Set<String> groups = Sets.newHashSet();
    Object responseEntity = null;

    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));
    try {

      validateRequest(ldapConfigurationRequest);

      AmbariLdapConfiguration ambariLdapConfiguration = ambariLdapConfigurationFactory.createLdapConfiguration(
        ldapConfigurationRequest.getAmbariConfiguration().getData().iterator().next());

      LdapConfigOperation action = LdapConfigOperation.fromAction(ldapConfigurationRequest.getRequestInfo().getAction());
      switch (action) {

        case TEST_CONNECTION:

          LOGGER.info("Testing connection to the LDAP server ...");
          ldapFacade.checkConnection(ambariLdapConfiguration);

          break;
        case TEST_ATTRIBUTES:

          LOGGER.info("Testing LDAP attributes ....");
          groups = ldapFacade.checkLdapAttributes(ldapConfigurationRequest.getRequestInfo().getParameters(), ambariLdapConfiguration);
          responseEntity = groups;

          break;
        case DETECT_ATTRIBUTES:

          LOGGER.info("Detecting LDAP attributes ...");
          ambariLdapConfiguration = ldapFacade.detectAttributes(ambariLdapConfiguration);
          responseEntity = ambariLdapConfiguration;

          break;
        default:
          LOGGER.warn("No action provided ...");
          throw new IllegalArgumentException("No request action provided");
      }

    } catch (Exception e) {
      result.setResultStatus(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e));
      responseEntity = e.getMessage();
    }

    return Response.status(result.getStatus().getStatusCode()).entity(responseEntity).build();
  }

  private void setResult(Set<String> groups, Result result) {
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    resource.setProperty("groups", groups);
    result.getResultTree().addChild(resource, "payload");
  }

  private void validateRequest(LdapConfigurationRequest ldapConfigurationRequest) {
    String errMsg;

    if (null == ldapConfigurationRequest) {
      errMsg = "No ldap configuraiton request provided";
      LOGGER.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }

    if (null == ldapConfigurationRequest.getRequestInfo()) {
      errMsg = String.format("No request information provided. Request: [%s]", ldapConfigurationRequest);
      LOGGER.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }

    if (null == ldapConfigurationRequest.getAmbariConfiguration()
      || ldapConfigurationRequest.getAmbariConfiguration().getData().size() != 1) {
      errMsg = String.format("No / Invalid configuration data provided. Request: [%s]", ldapConfigurationRequest);
      LOGGER.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }
  }

  private void authorize() {
    try {
      Authentication authentication = AuthorizationHelper.getAuthentication();

      if (authentication == null || !authentication.isAuthenticated()) {
        throw new AuthorizationException("Authentication data is not available, authorization to perform the requested operation is not granted");
      }

      if (!AuthorizationHelper.isAuthorized(authentication, ResourceType.AMBARI, null, requiredAuthorizations())) {
        throw new AuthorizationException("The authenticated user does not have the appropriate authorizations to create the requested resource(s)");
      }
    } catch (AuthorizationException e) {
      LOGGER.error("Unauthorized operation.", e);
      throw new IllegalArgumentException("User is not authorized to perform the operation", e);
    }

  }

  private Set<RoleAuthorization> requiredAuthorizations() {
    return Sets.newHashSet(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
  }
}
