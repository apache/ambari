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

package org.apache.ambari.server.ldap;

import java.util.Map;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.domain.TestAmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.ads.LdapConnectionTemplateFactory;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Test for the GUICE LdapModule setup
 *
 * - checks the module's bindings (can the GUICE context be created properely)
 * - checks for specific instances in the GUICE context (re they constructed properly, what is the instance' scope)
 *
 * It's named functional test as it creates a GUICE context. ("Real" unit tests only mock a class' collaborators, and
 * are more lightweight)
 *
 * By default the test is ignored, as it connects to external LDAP instances, thus in different environments may fail
 */
@Ignore
public class LdapModuleFunctionalTest {

  private static final Logger LOG = LoggerFactory.getLogger(LdapModuleFunctionalTest.class);
  private static Injector injector;


  @BeforeClass
  public static void beforeClass() throws Exception {

    // overriding bindings for testing purposes
    Module testModule = Modules.override(new LdapModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        // override the configuration instance binding not to access the database
        bind(AmbariLdapConfiguration.class).toInstance(new TestAmbariLdapConfigurationFactory().createLdapConfiguration(getADProps()));
      }
    });

    injector = Guice.createInjector(testModule);
  }

  @Test
  public void shouldLdapTemplateBeInstantiated() throws Exception {
    // GIVEN
    // the injector is set up
    Assert.assertNotNull(injector);

    // WHEN
    LdapConnectionTemplateFactory ldapConnectionTemplateFactory = injector.getInstance(LdapConnectionTemplateFactory.class);
    AmbariLdapConfigurationFactory ambariLdapConfigurationFactory = injector.getInstance(AmbariLdapConfigurationFactory.class);
    AmbariLdapConfiguration ldapConfiguration = ambariLdapConfigurationFactory.createLdapConfiguration(getADProps());
    LdapConnectionTemplate template = ldapConnectionTemplateFactory.create(ldapConfiguration);

    // THEN
    Assert.assertNotNull(template);
    //template.authenticate(new Dn("cn=read-only-admin,dc=example,dc=com"), "password".toCharArray());

    Boolean success = template.execute(new ConnectionCallback<Boolean>() {
      @Override
      public Boolean doWithConnection(LdapConnection connection) throws LdapException {

        return connection.isConnected() && connection.isAuthenticated();
      }
    });

    Assert.assertTrue("Could not bind to the LDAP server", success);

  }


  private static Map<String, Object> getProps() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();

    ldapPropsMap.put(AmbariLdapConfigKeys.ANONYMOUS_BIND.key(), "true");
    ldapPropsMap.put(AmbariLdapConfigKeys.SERVER_HOST.key(), "ldap.forumsys.com");
    ldapPropsMap.put(AmbariLdapConfigKeys.SERVER_PORT.key(), "389");
    ldapPropsMap.put(AmbariLdapConfigKeys.BIND_DN.key(), "cn=read-only-admin,dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfigKeys.BIND_PASSWORD.key(), "password");
//    ldapPropsMap.put(AmbariLdapConfigKeys.USE_SSL.key(), "true");

    ldapPropsMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), SchemaConstants.PERSON_OC);
    ldapPropsMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), SchemaConstants.UID_AT);
    ldapPropsMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfigKeys.DN_ATTRIBUTE.key(), SchemaConstants.UID_AT);
//    ldapPropsMap.put(AmbariLdapConfigKeys.TRUST_STORE.key(), "custom");
    ldapPropsMap.put(AmbariLdapConfigKeys.TRUST_STORE_TYPE.key(), "JKS");
//    ldapPropsMap.put(AmbariLdapConfigKeys.TRUST_STORE_PATH.key(), "/Users/lpuskas/my_truststore/KeyStore.jks");


    return ldapPropsMap;
  }

  private static Map<String, Object> getADProps() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();



    return ldapPropsMap;
  }

  @Test
  public void testShouldDetectorsBeBound() throws Exception {
    // GIVEN

    // WHEN
    AttributeDetectorFactory f = injector.getInstance(AttributeDetectorFactory.class);

    // THEN
    Assert.assertNotNull(f);
    LOG.info(f.groupAttributeDetector().toString());
    LOG.info(f.userAttributDetector().toString());

  }
}