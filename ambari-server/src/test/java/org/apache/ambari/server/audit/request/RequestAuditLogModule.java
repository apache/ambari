package org.apache.ambari.server.audit.request;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.easymock.EasyMock;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class RequestAuditLogModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<RequestAuditEventCreator> auditLogEventCreatorBinder = Multibinder.newSetBinder(binder(), RequestAuditEventCreator.class);
    auditLogEventCreatorBinder.addBinding().to(AllPostAndPutCreator.class);
    auditLogEventCreatorBinder.addBinding().to(AllGetCreator.class);
    auditLogEventCreatorBinder.addBinding().to(PutHostComponentCreator.class);

    bind(AuditLogger.class).toInstance(EasyMock.createStrictMock(AuditLoggerDefaultImpl.class));
    bind(RequestAuditLogger.class).to(RequestAuditLoggerImpl.class);
  }
}
