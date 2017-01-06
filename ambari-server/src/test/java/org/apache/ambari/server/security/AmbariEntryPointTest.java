package org.apache.ambari.server.security;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.core.AuthenticationException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

public class AmbariEntryPointTest extends EasyMockSupport {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCommenceDefault() throws Exception {
    testCommence(null);
  }

  @Test
  public void testCommenceKerberosAuthenticationEnabled() throws Exception {
    testCommence(Boolean.TRUE);
  }

  @Test
  public void testCommenceKerberosAuthenticationNotEnabled() throws Exception {
    testCommence(Boolean.FALSE);
  }

  private void testCommence(Boolean kerberosAuthenticationEnabled) throws IOException, ServletException {
    HttpServletRequest request = createStrictMock(HttpServletRequest.class);
    HttpServletResponse response = createStrictMock(HttpServletResponse.class);
    AuthenticationException exception = createStrictMock(AuthenticationException.class);

    if (Boolean.TRUE == kerberosAuthenticationEnabled) {
      response.setHeader("WWW-Authenticate", "Negotiate");
      expectLastCall().once();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication requested");
      expectLastCall().once();
    } else {
      expect(exception.getMessage()).andReturn("message").once();
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "message");
      expectLastCall().once();
    }

    replayAll();


    Properties properties = new Properties();
    if (kerberosAuthenticationEnabled != null) {
      properties.setProperty(Configuration.KERBEROS_AUTH_ENABLED.getKey(), kerberosAuthenticationEnabled.toString());
      properties.setProperty(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), temporaryFolder.newFile().getAbsolutePath());
    }
    AmbariEntryPoint entryPoint = new AmbariEntryPoint(new Configuration(properties));
    entryPoint.commence(request, response, exception);

    verifyAll();

  }

}