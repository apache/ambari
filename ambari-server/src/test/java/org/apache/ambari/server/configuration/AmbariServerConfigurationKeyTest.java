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

package org.apache.ambari.server.configuration;

import org.junit.Assert;
import org.junit.Test;

public class AmbariServerConfigurationKeyTest {

  @Test
  public void testTranslateNullCategory() {
    Assert.assertNull(AmbariServerConfigurationKey.translate(null, "some.property"));
  }

  @Test
  public void testTranslateNullPropertyName() {
    Assert.assertNull(AmbariServerConfigurationKey.translate(AmbariServerConfigurationCategory.LDAP_CONFIGURATION, null));
  }

  @Test
  public void testTranslateInvalidPropertyName() {
    Assert.assertNull(AmbariServerConfigurationKey.translate(AmbariServerConfigurationCategory.LDAP_CONFIGURATION, "invalid_property_name"));
  }

  @Test
  public void testTranslateExpected() {
    Assert.assertSame(AmbariServerConfigurationKey.LDAP_ENABLED,
        AmbariServerConfigurationKey.translate(AmbariServerConfigurationCategory.LDAP_CONFIGURATION, AmbariServerConfigurationKey.LDAP_ENABLED.key()));
  }

  @Test
  public void testTranslateRegex() {
    AmbariServerConfigurationKey keyWithRegex = AmbariServerConfigurationKey.TPROXY_ALLOWED_HOSTS;
    Assert.assertTrue(keyWithRegex.isRegex());

    Assert.assertSame(keyWithRegex,
        AmbariServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "ambari.tproxy.proxyuser.knox.hosts"));
    Assert.assertSame(keyWithRegex,
        AmbariServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "ambari.tproxy.proxyuser.not.knox.hosts"));

    AmbariServerConfigurationKey translatedKey = AmbariServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "ambari.tproxy.proxyuser.not.knox.groups");
    Assert.assertNotNull(translatedKey);
    Assert.assertNotSame(keyWithRegex, translatedKey);

    Assert.assertNull(AmbariServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "ambari.tproxy.proxyuser.not.knox.invalid"));
  }

}
