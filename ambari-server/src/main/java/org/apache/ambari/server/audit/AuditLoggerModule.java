/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit;

import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.audit.request.RequestAuditLogger;
import org.apache.ambari.server.audit.request.RequestAuditLoggerImpl;
import org.apache.ambari.server.audit.request.eventcreator.AlertGroupEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.AlertTargetEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.BlueprintEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.BlueprintExportEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ComponentEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ConfigurationChangeEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.CredentialEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.DefaultEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.GroupEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.HostEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.MemberEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.PrivilegeEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.RecommendationIgnoreEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.RepositoryEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.RepositoryVersionEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.RequestEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ServiceConfigDownloadEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ServiceEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.UnauthorizedEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.UpgradeEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.UpgradeItemEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.UserEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ValidationIgnoreEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ViewInstanceEventCreator;
import org.apache.ambari.server.audit.request.eventcreator.ViewPrivilegeEventCreator;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class AuditLoggerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AuditLogger.class).to(AsyncAuditLogger.class);

    // set AuditLoggerDefaultImpl to be used by AsyncAuditLogger
    bind(AuditLogger.class).annotatedWith(Names.named(AsyncAuditLogger.InnerLogger)).to(AuditLoggerDefaultImpl.class);

    // binding for audit event creators
    Multibinder<RequestAuditEventCreator> auditLogEventCreatorBinder = Multibinder.newSetBinder(binder(), RequestAuditEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(DefaultEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ComponentEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ServiceEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(UnauthorizedEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ConfigurationChangeEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(UserEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(GroupEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(MemberEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(PrivilegeEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(BlueprintExportEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ServiceConfigDownloadEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(BlueprintEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ViewInstanceEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ViewPrivilegeEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(RepositoryEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(RepositoryVersionEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(AlertGroupEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(AlertTargetEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(HostEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(UpgradeEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(UpgradeItemEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(RecommendationIgnoreEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(ValidationIgnoreEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(CredentialEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(RequestEventCreator.class);

    bind(RequestAuditLogger.class).to(RequestAuditLoggerImpl.class);

  }
}
