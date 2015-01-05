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

package org.apache.ambari.server.serveraction.kerberos;


import org.apache.ambari.server.AmbariException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Implementation of <code>KerberosOperationHandler</code> to created principal in Active Directory
 */
public class ADKerberosOperationHandler extends KerberosOperationHandler {

  private static Log LOG = LogFactory.getLog(ADKerberosOperationHandler.class);

  private static final String LDAP_CONTEXT_FACTORY_CLASS = "com.sun.jndi.ldap.LdapCtxFactory";

  private String adminPrincipal;
  private String adminPassword;
  private String realm;

  private String ldapUrl;
  private String principalContainerDn;

  private static final int ONELEVEL_SCOPE = SearchControls.ONELEVEL_SCOPE;
  private static final String LDAP_ATUH_MECH_SIMPLE = "simple";

  private LdapContext ldapContext;

  private SearchControls searchControls;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler.
   * This method in this class would always throw <code>AmabriException</code> reporting
   * ldapUrl is not provided.
   * Please use <code>open(KerberosCredential administratorCredentials, String defaultRealm,
   * String ldapUrl, String principalContainerDn)</code> for successful operation.
   * <p/>
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   *
   * @param administratorCredentials a KerberosCredential containing the administrative credentials
   *                                 for the relevant KDC
   * @param realm                    a String declaring the  Kerberos realm (or domain)
   */
  @Override
  public void open(KerberosCredential administratorCredentials, String realm)
    throws AmbariException {
    open(administratorCredentials, realm, null, null);
  }

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   *
   * @param administratorCredentials a KerberosCredential containing the administrative credentials
   *                                 for the relevant KDC
   * @param realm                    a String declaring the default Kerberos realm (or domain)
   * @param ldapUrl                  ldapUrl of ldap back end where principals would be created
   * @param principalContainerDn     DN of the container in ldap back end where principals would be created
   */
  @Override
  public void open(KerberosCredential administratorCredentials, String realm,
                   String ldapUrl, String principalContainerDn)
    throws AmbariException {
    if (administratorCredentials == null) {
      throw new AmbariException("admininstratorCredential not provided");
    }
    if (realm == null) {
      throw new AmbariException("realm not provided");
    }
    if (ldapUrl == null) {
      throw new AmbariException("ldapUrl not provided");
    }
    if (principalContainerDn == null) {
      throw new AmbariException("principalContainerDn not provided");
    }
    this.adminPrincipal = administratorCredentials.getPrincipal();
    this.adminPassword = administratorCredentials.getPassword();
    this.realm = realm;
    this.ldapUrl = ldapUrl;
    this.principalContainerDn = principalContainerDn;
    createLdapContext();
  }

  private void createLdapContext() throws AmbariException {
    LOG.info("Creating ldap context");

    Properties env = new Properties();
    env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY_CLASS);
    env.put(Context.PROVIDER_URL, ldapUrl);
    env.put(Context.SECURITY_PRINCIPAL, adminPrincipal);
    env.put(Context.SECURITY_CREDENTIALS, adminPassword);
    env.put(Context.SECURITY_AUTHENTICATION, LDAP_ATUH_MECH_SIMPLE);
    env.put(Context.REFERRAL, "follow");

    try {
      ldapContext = new InitialLdapContext(env, null);
    } catch (NamingException ne) {
      LOG.error("Can not created ldapContext", ne);
      throw new AmbariException("Can not created ldapContext", ne);
    }

    searchControls = new SearchControls();
    searchControls.setSearchScope(ONELEVEL_SCOPE);

    Set<String> userSearchAttributes = new HashSet<String>();
    userSearchAttributes.add("cn");
    searchControls.setReturningAttributes(userSearchAttributes.toArray(
      new String[userSearchAttributes.size()]));
  }

  /**
   * Closes and cleans up any resources used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used after this call.
   */
  @Override
  public void close() {
    try {
      if (ldapContext != null) {
        ldapContext.close();
      }
    } catch (NamingException ne) {
      // ignored, nothing we could do about it
    }

  }

  /**
   * Maps Keberos realm name to AD dc tree syntaz
   *
   * @param realm kerberos realm name
   * @return mapped dc tree string
   */
  private static String realmToDcs(String realm) {
    if (realm == null || realm.isEmpty()) {
      return realm;
    }
    String[] tokens = realm.split("\\.");
    StringBuilder sb = new StringBuilder();
    int len = tokens.length;
    if (len > 0) {
      sb.append("dc=").append(tokens[0]);
    }
    for (int i = 1; i < len; i++)   {
      sb.append(",").append("dc=").append(tokens[i]);
    }
    return sb.toString();
  }

  /**
   * Test to see if the specified principal exists in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to test
   * @return true if the principal exists; false otherwise
   * @throws AmbariException
   */
  @Override
  public boolean principalExists(String principal) throws AmbariException {
    if (principal == null) {
      throw new AmbariException("principal is null");
    }
    NamingEnumeration<SearchResult> searchResultEnum = null;
    try {
      searchResultEnum = ldapContext.search(
        principalContainerDn,
        "(cn=" + principal + ")",
        searchControls);
      if (searchResultEnum.hasMore()) {
        return true;
      }
    } catch (NamingException ne) {
      throw new AmbariException("can not check if principal exists: " + principal, ne);
    } finally {
      try {
        if (searchResultEnum != null) {
          searchResultEnum.close();
        }
      } catch (NamingException ne) {
        // ignore, we can not do anything about it
      }
    }
    return false;
  }

  /**
   * Creates a new principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to add
   * @param password  a String containing the password to use when creating the principal
   * @return an Integer declaring the generated key number
   * @throws AmbariException
   */
  @Override
  public Integer createServicePrincipal(String principal, String password)
    throws AmbariException {
    if (principal == null) {
      throw new AmbariException("principal is null");
    }
    if (password == null) {
      throw new AmbariException("principal password is null");
    }
    Attributes attributes = new BasicAttributes();

    Attribute objectClass = new BasicAttribute("objectClass");
    objectClass.add("user");
    attributes.put(objectClass);

    Attribute cn = new BasicAttribute("cn");
    cn.add(principal);
    attributes.put(cn);

    Attribute upn = new BasicAttribute("userPrincipalName");
    upn.add(principal + "@" + realm.toLowerCase());
    attributes.put(upn);

    Attribute spn = new BasicAttribute("servicePrincipalName");
    spn.add(principal);
    attributes.put(spn);

    Attribute uac = new BasicAttribute("userAccountControl");  // userAccountControl
    uac.add("512");
    attributes.put(uac);

    Attribute passwordAttr = new BasicAttribute("unicodePwd");  // password
    String quotedPasswordVal = "\"" + password + "\"";
    try {
      passwordAttr.add(quotedPasswordVal.getBytes("UTF-16LE"));
    } catch (UnsupportedEncodingException ue) {
      throw new AmbariException("Can not encode password with UTF-16LE", ue);
    }
    attributes.put(passwordAttr);

    try {
      Name name = new CompositeName().add("cn=" + principal + "," + principalContainerDn);
      ldapContext.createSubcontext(name, attributes);
    } catch (NamingException ne) {
      throw new AmbariException("Can not created principal : " + principal, ne);
    }
    return 0;
  }

  /**
   * Updates the password for an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @return an Integer declaring the new key number
   * @throws AmbariException
   */
  @Override
  public Integer setPrincipalPassword(String principal, String password) throws AmbariException {
    if (principal == null) {
      throw new AmbariException("principal is null");
    }
    if (password == null) {
      throw new AmbariException("principal password is null");
    }
    if (!principalExists(principal)) {
      if (password == null) {
        throw new AmbariException("principal not found : " + principal);
      }
    }
    try {
      createLdapContext();

      ModificationItem[] mods = new ModificationItem[1];
      String quotedPasswordVal = "\"" + password + "\"";
      mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
        new BasicAttribute("UnicodePwd", quotedPasswordVal.getBytes("UTF-16LE")));
      ldapContext.modifyAttributes(
        new CompositeName().add("cn=" + principal + "," + principalContainerDn),
        mods);
    } catch (NamingException ne) {
      throw new AmbariException("Can not set password for principal : " + principal, ne);
    } catch (UnsupportedEncodingException ue) {
      throw new AmbariException("Unsupported encoding UTF-16LE", ue);
    }
    return 0;
  }

  /**
   * Removes an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to remove
   * @return true if the principal was successfully removed; otherwise false
   * @throws AmbariException
   */
  @Override
  public boolean removeServicePrincipal(String principal) throws AmbariException {
    if (principal == null) {
      throw new AmbariException("principal is null");
    }
    if (!principalExists(principal)) {
      return false;
    }
    try {
      Name name = new CompositeName().add("cn=" + principal + "," + principalContainerDn);
      ldapContext.destroySubcontext(name);
    } catch (NamingException ne) {
      throw new AmbariException("Can not remove principal: " + principal);
    }
    return true;
  }

  /**
   * Implementation of main method to illustrate the use of operations on this class
   *
   * @param args not used here
   * @throws Throwable
   */
  public static void main(String[] args) throws Throwable {

    // SSL Certificate of AD should have been imported into truststore when that certificate
    // is not issued by trusted authority. This is typical with self signed certificated in
    // development environment
    System.setProperty("javax.net.ssl.trustStore",
      "/tmp/workspace/ambari/apache-ambari-rd/cacerts");

    ADKerberosOperationHandler handler = new ADKerberosOperationHandler();

    KerberosCredential kc = new KerberosCredential(
      "Administrator@knox.com", "hadoop", null);  // null keytab

    handler.open(kc, "KNOX.COM",
      "ldaps://dillwin12.knox.com:636", "ou=service accounts,dc=knox,dc=com");

    // does the princial already exist?
    System.out.println("Principal exists: " + handler.principalExists("nn/c1508.ambari.apache.org"));

    //create principal
    handler.createServicePrincipal("nn/c1508.ambari.apache.org", "welcome");

    //update the password
    handler.setPrincipalPassword("nn/c1508.ambari.apache.org", "welcome10");

    // remove the principal
    // handler.removeServicePrincipal("nn/c1508.ambari.apache.org");

    handler.close();

  }

}