package org.apache.ambari.server.configuration;

import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * ComponentSSLConfiguration tests.
 */
public class ComponentSSLConfigurationTest {

  public static ComponentSSLConfiguration getConfiguration(String path, String pass, String type, boolean gangliaSSL, boolean nagiosSSL) {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.SSL_TRUSTSTORE_PATH_KEY, path);
    ambariProperties.setProperty(Configuration.SSL_TRUSTSTORE_PASSWORD_KEY, pass);
    ambariProperties.setProperty(Configuration.SSL_TRUSTSTORE_TYPE_KEY, type);
    ambariProperties.setProperty(Configuration.GANGLIA_HTTPS_KEY, Boolean.toString(gangliaSSL));
    ambariProperties.setProperty(Configuration.NAGIOS_HTTPS_KEY, Boolean.toString(nagiosSSL));

    Configuration configuration =  new TestConfiguration(ambariProperties);

    ComponentSSLConfiguration sslConfiguration = new ComponentSSLConfiguration();

    sslConfiguration.init(configuration);

    return sslConfiguration;
  }

  @Test
  public void testGetTruststorePath() throws Exception {
    ComponentSSLConfiguration sslConfiguration = getConfiguration("tspath", "tspass", "tstype", true, false);
    Assert.assertEquals("tspath", sslConfiguration.getTruststorePath());
  }

  @Test
  public void testGetTruststorePassword() throws Exception {
    ComponentSSLConfiguration sslConfiguration = getConfiguration("tspath", "tspass", "tstype", true, false);
    Assert.assertEquals("tspass", sslConfiguration.getTruststorePassword());
  }

  @Test
  public void testGetTruststoreType() throws Exception {
    ComponentSSLConfiguration sslConfiguration = getConfiguration("tspath", "tspass", "tstype", true, false);
    Assert.assertEquals("tstype", sslConfiguration.getTruststoreType());
  }

  @Test
  public void testIsGangliaSSL() throws Exception {
    ComponentSSLConfiguration sslConfiguration = getConfiguration("tspath", "tspass", "tstype", true, false);
    Assert.assertTrue(sslConfiguration.isGangliaSSL());
  }

  @Test
  public void testIsNagiosSSL() throws Exception {
    ComponentSSLConfiguration sslConfiguration = getConfiguration("tspath", "tspass", "tstype", true, false);
    Assert.assertFalse(sslConfiguration.isNagiosSSL());
  }

  private static class TestConfiguration extends Configuration {

    private TestConfiguration(Properties properties) {
      super(properties);
    }

    @Override
    protected void loadSSLParams() {
    }
  }
}
