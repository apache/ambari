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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit;

import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;

/**
 * Successful login audit event.
 */
@Immutable
public class LoginSucceededAuditEvent extends AbstractLoginAuditEvent {

  public static class LoginSucceededAuditEventBuilder
    extends AbstractLoginAuditEventBuilder<LoginSucceededAuditEvent, LoginSucceededAuditEventBuilder> {

    private LoginSucceededAuditEventBuilder() { }

    private List<String> roles;


    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Roles(")
        .append(StringUtils.join(roles, ","))
        .append("), Status(Login succeeded !)");
    }

    /**
     * Sets the list of roles possessed by the principal requesting access to a resource.
     * @param roles
     * @return this builder
     */
    public LoginSucceededAuditEventBuilder withRoles(List<String> roles) {
      this.roles = roles;

      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LoginSucceededAuditEvent newAuditEvent() {
      return new LoginSucceededAuditEvent(this);
    }

  }


  private LoginSucceededAuditEvent() { }

  /**
   * {@inheritDoc}
   */
  private LoginSucceededAuditEvent(LoginSucceededAuditEventBuilder builder) {
    super(builder);

  }

  /**
   * Returns an builder for {@link LoginSucceededAuditEvent}
   * @return a builder instance
   */
  public static LoginSucceededAuditEventBuilder builder() {
    return new LoginSucceededAuditEventBuilder();
  }
}
