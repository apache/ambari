/**
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

package org.apache.ambari.server.configuration;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ Configuration.class })
public class ConfigurationTest {
  public TemporaryFolder temp = new TemporaryFolder();
  private Injector injector;

  @Inject
  private Configuration config;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.injectMembers(this);
    temp.create();
  }

  @After
  public void teardown() throws AmbariException {
    temp.delete();
  }

  /**
   * ambari.properties doesn't contain "security.server.two_way_ssl" option
   * @throws Exception
   */
  @Test
  public void testDefaultTwoWayAuthNotSet() throws Exception {
    Assert.assertFalse(config.getTwoWaySsl());
  }

  /**
   * ambari.properties contains "security.server.two_way_ssl=true" option
   * @throws Exception
   */
  @Test
  public void testTwoWayAuthTurnedOn() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty("security.server.two_way_ssl", "true");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertTrue(conf.getTwoWaySsl());
  }

  /**
   * ambari.properties contains "security.server.two_way_ssl=false" option
   * @throws Exception
   */
  @Test
  public void testTwoWayAuthTurnedOff() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty("security.server.two_way_ssl", "false");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertFalse(conf.getTwoWaySsl());
  }

  @Test
  public void testGetClientSSLApiPort() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.CLIENT_API_SSL_PORT_KEY, "6666");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertEquals(6666, conf.getClientSSLApiPort());
    conf = new Configuration();
    Assert.assertEquals(8443, conf.getClientSSLApiPort());
  }

  @Test
  public void testGetClientHTTPSSettings() throws IOException {

    File passFile = File.createTempFile("https.pass.", "txt");
    passFile.deleteOnExit();

    String password = "pass12345";

    FileUtils.writeStringToFile(passFile, password);

    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.API_USE_SSL, "true");
    ambariProperties.setProperty(
        Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY,
        passFile.getParent());
    ambariProperties.setProperty(
        Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY,
        passFile.getName());


    String oneWayPort = RandomStringUtils.randomNumeric(4);
    String twoWayPort = RandomStringUtils.randomNumeric(4);

    ambariProperties.setProperty(Configuration.SRVR_TWO_WAY_SSL_PORT_KEY, twoWayPort.toString());
    ambariProperties.setProperty(Configuration.SRVR_ONE_WAY_SSL_PORT_KEY, oneWayPort.toString());

    Configuration conf = new Configuration(ambariProperties);
    Assert.assertTrue(conf.getApiSSLAuthentication());

    //Different certificates for two-way SSL and HTTPS
    Assert.assertFalse(conf.getConfigsMap().get(Configuration.KSTR_NAME_KEY).
      equals(conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_KSTR_NAME_KEY)));
    Assert.assertFalse(conf.getConfigsMap().get(Configuration.SRVR_CRT_NAME_KEY).
      equals(conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_CRT_NAME_KEY)));

    Assert.assertEquals("https.keystore.p12", conf.getConfigsMap().get(
      Configuration.CLIENT_API_SSL_KSTR_NAME_KEY));
    Assert.assertEquals(passFile.getName(), conf.getConfigsMap().get(
      Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY));
    Assert.assertEquals(password, conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_CRT_PASS_KEY));
    Assert.assertEquals(Integer.parseInt(twoWayPort), conf.getTwoWayAuthPort());
    Assert.assertEquals(Integer.parseInt(oneWayPort), conf.getOneWayAuthPort());

  }

  @Test
  public void testLoadSSLParams_unencrypted() throws IOException {
    Properties ambariProperties = new Properties();
    String unencrypted = "fake-unencrypted-password";
    String encrypted = "fake-encrypted-password";
    ambariProperties.setProperty(Configuration.SSL_TRUSTSTORE_PASSWORD_KEY, unencrypted);
    Configuration conf = spy(new Configuration(ambariProperties));
    doReturn(null).when(conf).readPasswordFromStore(anyString());
    conf.loadSSLParams();
    Assert.assertEquals(System.getProperty(conf.JAVAX_SSL_TRUSTSTORE_PASSWORD, "unknown"), unencrypted);
  }

  @Test
  public void testLoadSSLParams_encrypted() throws IOException {
    Properties ambariProperties = new Properties();
    String unencrypted = "fake-unencrypted-password";
    String encrypted = "fake-encrypted-password";
    ambariProperties.setProperty(Configuration.SSL_TRUSTSTORE_PASSWORD_KEY, unencrypted);
    Configuration conf = spy(new Configuration(ambariProperties));
    doReturn(encrypted).when(conf).readPasswordFromStore(anyString());
    conf.loadSSLParams();
    Assert.assertEquals(System.getProperty(conf.JAVAX_SSL_TRUSTSTORE_PASSWORD, "unknown"), encrypted);
  }

  @Test
  public void testGetRcaDatabasePassword_fromStore() {
    String serverJdbcRcaUserPasswdKey = "key";
    String encrypted = "password";

    Properties properties = new Properties();
    properties.setProperty(Configuration.SERVER_JDBC_RCA_USER_PASSWD_KEY, serverJdbcRcaUserPasswdKey);
    Configuration conf = spy(new Configuration(properties));
    doReturn(encrypted).when(conf).readPasswordFromStore(serverJdbcRcaUserPasswdKey);

    Assert.assertEquals(encrypted, conf.getRcaDatabasePassword());
  }

  @Test
  public void testGetRcaDatabasePassword_fromFile() {
    Configuration conf = spy(new Configuration(new Properties()));
    Assert.assertEquals("mapred", conf.getRcaDatabasePassword());
  }

  @Test
  public void testGetLocalDatabaseUrl() {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty("server.jdbc.database_name", "ambaritestdatabase");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertEquals(conf.getLocalDatabaseUrl(), Configuration.JDBC_LOCAL_URL.concat("ambaritestdatabase"));
  }

  @Test
  public void testNoNewlineInPassword() throws Exception {
    Properties ambariProperties = new Properties();
    File f = temp.newFile("password.dat");
    FileOutputStream fos = new FileOutputStream(f);
    fos.write("ambaritest\r\n".getBytes());
    fos.close();
    String passwordFile = temp.getRoot().getAbsolutePath()
      + System.getProperty("file.separator") + "password.dat";

    ambariProperties.setProperty(Configuration.SERVER_JDBC_USER_PASSWD_KEY,
      passwordFile);

    Configuration conf = new Configuration(ambariProperties);
    PowerMock.stub(PowerMock.method(Configuration.class,
      "readPasswordFromStore")).toReturn(null);

    Assert.assertEquals("ambaritest", conf.getDatabasePassword());
  }

  @Test
  public void testGetAmbariProperties() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty("name", "value");
    Configuration conf = new Configuration(ambariProperties);
    mockStatic(Configuration.class);
    Method[] methods = MemberMatcher.methods(Configuration.class, "readConfigFile");
    PowerMock.expectPrivate(Configuration.class, methods[0]).andReturn(ambariProperties);
    replayAll();
    Map<String, String> props = conf.getAmbariProperties();
    verifyAll();
    Assert.assertEquals("value", props.get("name"));
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test()
  public void testGetLocalDatabaseUrlThrowException() {
    Properties ambariProperties = new Properties();
    Configuration conf = new Configuration(ambariProperties);
    exception.expect(RuntimeException.class);
    exception.expectMessage("Server DB Name is not configured!");
    conf.getLocalDatabaseUrl();
  }

  @Test()
  public void testServerPoolSizes() {
    Properties ambariProperties = new Properties();
    Configuration conf = new Configuration(ambariProperties);

    Assert.assertEquals(25, conf.getClientThreadPoolSize());
    Assert.assertEquals(25, conf.getAgentThreadPoolSize());

    Assert.assertEquals(10, conf.getViewExtractionThreadPoolCoreSize());
    Assert.assertEquals(20, conf.getViewExtractionThreadPoolMaxSize());
    Assert.assertEquals(100000L, conf.getViewExtractionThreadPoolTimeout());

    ambariProperties = new Properties();
    ambariProperties.setProperty("client.threadpool.size.max", "4");
    ambariProperties.setProperty("agent.threadpool.size.max", "82");

    ambariProperties.setProperty("view.extraction.threadpool.size.core", "83");
    ambariProperties.setProperty("view.extraction.threadpool.size.max", "56");
    ambariProperties.setProperty("view.extraction.threadpool.timeout", "6000");

    conf = new Configuration(ambariProperties);

    Assert.assertEquals(4, conf.getClientThreadPoolSize());
    Assert.assertEquals(82, conf.getAgentThreadPoolSize());

    Assert.assertEquals(83, conf.getViewExtractionThreadPoolCoreSize());
    Assert.assertEquals(56, conf.getViewExtractionThreadPoolMaxSize());
    Assert.assertEquals(6000L, conf.getViewExtractionThreadPoolTimeout());
  }

  @Test
  public void testGetLdapServerProperties_WrongManagerPassword() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.LDAP_MANAGER_PASSWORD_KEY, "somePassword");
    final Configuration configuration = new Configuration(ambariProperties);

    final LdapServerProperties ldapProperties = configuration.getLdapServerProperties();
    // if it's not a store alias and is not a file, it should be ignored
    Assert.assertNull(ldapProperties.getManagerPassword());
  }

  @Test
  public void testGetLdapServerProperties() throws Exception {
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);

    final File passwordFile = temp.newFile("ldap-password.dat");
    final FileOutputStream fos = new FileOutputStream(passwordFile);
    fos.write("ambaritest\r\n".getBytes());
    fos.close();
    final String passwordFilePath = temp.getRoot().getAbsolutePath() + File.separator + "ldap-password.dat";

    ambariProperties.setProperty(Configuration.LDAP_PRIMARY_URL_KEY, "1");
    ambariProperties.setProperty(Configuration.LDAP_SECONDARY_URL_KEY, "2");
    ambariProperties.setProperty(Configuration.LDAP_USE_SSL_KEY, "true");
    ambariProperties.setProperty(Configuration.LDAP_BIND_ANONYMOUSLY_KEY, "true");
    ambariProperties.setProperty(Configuration.LDAP_MANAGER_DN_KEY, "5");
    ambariProperties.setProperty(Configuration.LDAP_MANAGER_PASSWORD_KEY, passwordFilePath);
    ambariProperties.setProperty(Configuration.LDAP_BASE_DN_KEY, "7");
    ambariProperties.setProperty(Configuration.LDAP_USERNAME_ATTRIBUTE_KEY, "8");
    ambariProperties.setProperty(Configuration.LDAP_USER_BASE_KEY, "9");
    ambariProperties.setProperty(Configuration.LDAP_USER_OBJECT_CLASS_KEY, "10");
    ambariProperties.setProperty(Configuration.LDAP_GROUP_BASE_KEY, "11");
    ambariProperties.setProperty(Configuration.LDAP_GROUP_OBJECT_CLASS_KEY, "12");
    ambariProperties.setProperty(Configuration.LDAP_GROUP_MEMEBERSHIP_ATTR_KEY, "13");
    ambariProperties.setProperty(Configuration.LDAP_GROUP_NAMING_ATTR_KEY, "14");
    ambariProperties.setProperty(Configuration.LDAP_ADMIN_GROUP_MAPPING_RULES_KEY, "15");
    ambariProperties.setProperty(Configuration.LDAP_GROUP_SEARCH_FILTER_KEY, "16");

    final LdapServerProperties ldapProperties = configuration.getLdapServerProperties();

    Assert.assertEquals("1", ldapProperties.getPrimaryUrl());
    Assert.assertEquals("2", ldapProperties.getSecondaryUrl());
    Assert.assertEquals(true, ldapProperties.isUseSsl());
    Assert.assertEquals(true, ldapProperties.isAnonymousBind());
    Assert.assertEquals("5", ldapProperties.getManagerDn());
    Assert.assertEquals("ambaritest", ldapProperties.getManagerPassword());
    Assert.assertEquals("7", ldapProperties.getBaseDN());
    Assert.assertEquals("8", ldapProperties.getUsernameAttribute());
    Assert.assertEquals("9", ldapProperties.getUserBase());
    Assert.assertEquals("10", ldapProperties.getUserObjectClass());
    Assert.assertEquals("11", ldapProperties.getGroupBase());
    Assert.assertEquals("12", ldapProperties.getGroupObjectClass());
    Assert.assertEquals("13", ldapProperties.getGroupMembershipAttr());
    Assert.assertEquals("14", ldapProperties.getGroupNamingAttr());
    Assert.assertEquals("15", ldapProperties.getAdminGroupMappingRules());
    Assert.assertEquals("16", ldapProperties.getGroupSearchFilter());
  }
}
