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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Implementation of <code>KerberosOperationHandler</code> to created principal in Active Directory
 */
public class ADKerberosOperationHandler extends KerberosOperationHandler {

  private static Log LOG = LogFactory.getLog(ADKerberosOperationHandler.class);

  private static final String LDAP_CONTEXT_FACTORY_CLASS = "com.sun.jndi.ldap.LdapCtxFactory";

  private String ldapUrl;
  private String principalContainerDn;

  private static final int ONE_LEVEL_SCOPE = SearchControls.ONELEVEL_SCOPE;
  private static final String LDAP_ATUH_MECH_SIMPLE = "simple";

  private LdapContext ldapContext;
  private SearchControls searchControls;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler.
   * This method in this class would always throw <code>KerberosOperationException</code> reporting
   * ldapUrl is not provided.
   * Use <code>open(KerberosCredential administratorCredentials, String defaultRealm,
   * String ldapUrl, String principalContainerDn)</code> for successful operation.
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   *
   * @param administratorCredentials a KerberosCredential containing the administrative credentials
   *                                 for the relevant KDC
   * @param realm                    a String declaring the  Kerberos realm (or domain)
   */
  @Override
  public void open(KerberosCredential administratorCredentials, String realm)
      throws KerberosOperationException {
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
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public void open(KerberosCredential administratorCredentials, String realm,
                   String ldapUrl, String principalContainerDn)
      throws KerberosOperationException {

    if (isOpen()) {
      close();
    }

    if (administratorCredentials == null) {
      throw new KerberosAdminAuthenticationException("administrator Credential not provided");
    }
    if (realm == null) {
      throw new KerberosRealmException("realm not provided");
    }
    if (ldapUrl == null) {
      throw new KerberosKDCConnectionException("ldapUrl not provided");
    }
    if (principalContainerDn == null) {
      throw new KerberosLDAPContainerException("principalContainerDn not provided");
    }

    setAdministratorCredentials(administratorCredentials);
    setDefaultRealm(realm);

    this.ldapUrl = ldapUrl;
    this.principalContainerDn = principalContainerDn;
    this.ldapContext = createLdapContext();
    this.searchControls = createSearchControls();

    setOpen(true);
  }

  /**
   * Closes and cleans up any resources used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used after this call.
   */
  @Override
  public void close() throws KerberosOperationException {
    if (ldapContext != null) {
      try {
        ldapContext.close();
      } catch (NamingException e) {
        throw new KerberosOperationException("Unexpected error", e);
      }
    }

    setOpen(false);
  }

  /**
   * Test to see if the specified principal exists in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to test
   * @return true if the principal exists; false otherwise
   * @throws KerberosOperationException
   */
  @Override
  public boolean principalExists(String principal) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }
    NamingEnumeration<SearchResult> searchResultEnum = null;
    try {
      searchResultEnum = ldapContext.search(
          principalContainerDn,
          "(userPrincipalName=" + principal + ")",
          searchControls);
      if (searchResultEnum.hasMore()) {
        return true;
      }
    } catch (NamingException ne) {
      throw new KerberosOperationException("can not check if principal exists: " + principal, ne);
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
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosOperationException
   */
  @Override
  public Integer createPrincipal(String principal, String password, boolean service)
      throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }
    if (password == null) {
      throw new KerberosOperationException("principal password is null");
    }

    // TODO: (rlevas) pass components and realm in separately (AMBARI-9122)
    String realm = getDefaultRealm();
    int atIndex = principal.indexOf("@");
    if (atIndex >= 0) {
      realm = principal.substring(atIndex + 1);
      principal = principal.substring(0, atIndex);
    }

    if (realm == null) {
      realm = "";
    }

    Attributes attributes = new BasicAttributes();

    Attribute objectClass = new BasicAttribute("objectClass");
    objectClass.add("user");
    attributes.put(objectClass);

    Attribute cn = new BasicAttribute("cn");
    cn.add(principal);
    attributes.put(cn);

    Attribute upn = new BasicAttribute("userPrincipalName");
    upn.add(String.format("%s@%s", principal, realm.toLowerCase()));
    attributes.put(upn);

    if (service) {
      Attribute spn = new BasicAttribute("servicePrincipalName");
      spn.add(principal);
      attributes.put(spn);
    }

    Attribute uac = new BasicAttribute("userAccountControl");  // userAccountControl
    uac.add("512");
    attributes.put(uac);

    Attribute passwordAttr = new BasicAttribute("unicodePwd");  // password
    String quotedPasswordVal = "\"" + password + "\"";
    try {
      passwordAttr.add(quotedPasswordVal.getBytes("UTF-16LE"));
    } catch (UnsupportedEncodingException ue) {
      throw new KerberosOperationException("Can not encode password with UTF-16LE", ue);
    }
    attributes.put(passwordAttr);

    try {
      Name name = new CompositeName().add("cn=" + principal + "," + principalContainerDn);
      ldapContext.createSubcontext(name, attributes);
    } catch (NamingException ne) {
      throw new KerberosOperationException("Can not create principal : " + principal, ne);
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
   * @throws KerberosOperationException
   */
  @Override
  public Integer setPrincipalPassword(String principal, String password) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }
    if (password == null) {
      throw new KerberosOperationException("principal password is null");
    }
    try {
      if (!principalExists(principal)) {
        throw new KerberosOperationException("principal not found : " + principal);
      }
    } catch (KerberosOperationException e) {
      e.printStackTrace();
    }
    try {
      ModificationItem[] mods = new ModificationItem[1];
      String quotedPasswordVal = "\"" + password + "\"";
      mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
          new BasicAttribute("UnicodePwd", quotedPasswordVal.getBytes("UTF-16LE")));
      ldapContext.modifyAttributes(
          new CompositeName().add("cn=" + principal + "," + principalContainerDn),
          mods);
    } catch (NamingException ne) {
      throw new KerberosOperationException("Can not set password for principal : " + principal, ne);
    } catch (UnsupportedEncodingException ue) {
      throw new KerberosOperationException("Unsupported encoding UTF-16LE", ue);
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
   * @throws KerberosOperationException
   */
  @Override
  public boolean removePrincipal(String principal) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }
    try {
      if (!principalExists(principal)) {
        return false;
      }
    } catch (KerberosOperationException e) {
      e.printStackTrace();
    }
    try {
      Name name = new CompositeName().add("cn=" + principal + "," + principalContainerDn);
      ldapContext.destroySubcontext(name);
    } catch (NamingException ne) {
      throw new KerberosOperationException("Can not remove principal: " + principal);
    }

    return true;
  }

  @Override
  public boolean testAdministratorCredentials() throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    // If this KerberosOperationHandler was successfully opened, successful authentication has
    // already occurred.
    return true;
  }

  /**
   * Helper method to create the LDAP context needed to interact with the Active Directory.
   *
   * @return the relevant LdapContext
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  protected LdapContext createLdapContext() throws KerberosOperationException {
    KerberosCredential administratorCredentials = getAdministratorCredentials();

    Properties properties = new Properties();
    properties.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY_CLASS);
    properties.put(Context.PROVIDER_URL, ldapUrl);
    properties.put(Context.SECURITY_PRINCIPAL, administratorCredentials.getPrincipal());
    properties.put(Context.SECURITY_CREDENTIALS, administratorCredentials.getPassword());
    properties.put(Context.SECURITY_AUTHENTICATION, LDAP_ATUH_MECH_SIMPLE);
    properties.put(Context.REFERRAL, "follow");
    properties.put("java.naming.ldap.factory.socket", TrustingSSLSocketFactory.class.getName());

    try {
      return createInitialLdapContext(properties, null);
    } catch (CommunicationException e) {
      String message = String.format("Failed to communicate with the Active Directory at %s: %s", ldapUrl, e.getMessage());
      LOG.warn(message, e);
      throw new KerberosKDCConnectionException(message, e);
    } catch (AuthenticationException e) {
      String message = String.format("Failed to authenticate with the Active Directory at %s: %s", ldapUrl, e.getMessage());
      LOG.warn(message, e);
      throw new KerberosAdminAuthenticationException(message, e);
    } catch (NamingException e) {
      String error = e.getMessage();

      if ((error != null) && !error.isEmpty()) {
        String message = String.format("Failed to communicate with the Active Directory at %s: %s", ldapUrl, e.getMessage());
        LOG.warn(message, e);

        if (error.startsWith("Cannot parse url:")) {
          throw new KerberosKDCConnectionException(message, e);
        } else {
          throw new KerberosOperationException(message, e);
        }
      } else {
        throw new KerberosOperationException("Unexpected error condition", e);
      }
    }
  }

  /**
   * Helper method to create the LDAP context needed to interact with the Active Directory.
   * <p/>
   * This is mainly used to help with building mocks for test cases.
   *
   * @param properties environment used to create the initial DirContext.
   *                   Null indicates an empty environment.
   * @param controls   connection request controls for the initial context.
   *                   If null, no connection request controls are used.
   * @return the relevant LdapContext
   * @throws NamingException if a naming exception is encountered
   */
  protected LdapContext createInitialLdapContext(Properties properties, Control[] controls)
      throws NamingException {
    return new InitialLdapContext(properties, controls);
  }

  /**
   * Helper method to create the SearchControls instance
   *
   * @return the relevant SearchControls
   */
  protected SearchControls createSearchControls() {
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(ONE_LEVEL_SCOPE);
    searchControls.setReturningAttributes(new String[]{"cn"});
    return searchControls;
  }

}