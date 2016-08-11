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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.annotations.Markdown;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.orm.JPATableGenerationStrategy;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.ambari.server.security.authorization.jwt.JwtAuthenticationProperties;
import org.apache.ambari.server.security.encryption.CertificateUtils;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.AmbariPath;
import org.apache.ambari.server.utils.Parallel;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Ambari configuration.
 * Reads properties from ambari.properties
 */
@Singleton
public class Configuration {

  @Inject
  private OsFamily osFamily;

  /**
   * The filename of the {@link Properties} file which contains all of the
   * configurations for Ambari.
   */
  private static final String CONFIG_FILE = "ambari.properties";

  /**
   * PREFIX_DIR is shared in ambari-agent.ini and should only be called by unit
   * tests. For all server-side processing, it should be retrieved from
   * <code>HostImpl.getPrefix()</code>
   */
  public static final String PREFIX_DIR = "/var/lib/ambari-agent/data";

  /**
   * The minimum JDK version supported by Ambari.
   */
  public static final float JDK_MIN_VERSION = 1.7f;

  /**
   *
   */
  private static final String LDAP_SYNC_MEMBER_REPLACE_PATTERN_DEFAULT = "";

  /**
   *
   */
  private static final String LDAP_SYNC_MEMBER_FILTER_DEFAULT = "";

  /**
   *
   */
  public static final String SERVER_JDBC_PROPERTIES_PREFIX = "server.jdbc.properties.";

  /**
   *
   */
  public static final String SERVER_PERSISTENCE_PROPERTIES_PREFIX = "server.persistence.properties.";

  /**
   *
   */
  public static final String HOSTNAME_MACRO = "{hostname}";

  /**
   *
   */
  public static final String JDBC_UNIT_NAME = "ambari-server";

  /**
   *
   */
  public static final String JDBC_LOCAL_URL = "jdbc:postgresql://localhost/";

  /**
   *
   */
  public static final String DEFAULT_DERBY_SCHEMA = "ambari";

  /**
   *
   */
  public static final String JDBC_IN_MEMORY_URL = String.format(
      "jdbc:derby:memory:myDB/%s;create=true", DEFAULT_DERBY_SCHEMA);

  /**
   *
   */
  public static final String JDBC_IN_MEMORY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

  /**
   *
   */
  public static final String JAVAX_SSL_TRUSTSTORE = "javax.net.ssl.trustStore";

  /**
   *
   */
  public static final String JAVAX_SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

  /**
   *
   */
  public static final String JAVAX_SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

  /**
   *
   */
  public static final String GLOBAL_CONFIG_TAG = "global";

  /**
   *
   */
  public static final String MAPREDUCE2_LOG4J_CONFIG_TAG = "mapreduce2-log4j";

  /**
   *
   */
  public static final String RCA_ENABLED_PROPERTY = "rca_enabled";

  /**
   * Threadpool sizing based on the number of available processors multiplied by
   * 2.
   */
  public static final int PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT = 2
      * Runtime.getRuntime().availableProcessors();

  /**
   * Threadpool sizing based on the number of available processors multiplied by
   * 4.
   */
  public static final int PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT = 4
      * Runtime.getRuntime().availableProcessors();

  /**
   *
   */
  private static final Set<String> dbConnectorPropertyNames = new HashSet<String>(Arrays.asList(
      "custom.mysql.jdbc.name", "custom.oracle.jdbc.name", "custom.postgres.jdbc.name",
      "custom.mssql.jdbc.name", "custom.hsqldb.jdbc.name", "custom.sqlanywhere.jdbc.name"));


  /**
   *
   */
  public static final String MASTER_KEY_ENV_PROP = "AMBARI_SECURITY_MASTER_KEY";

  /**
   *
   */
  public static final String MASTER_KEY_FILENAME_DEFAULT = "master";

  /**
   *
   */
  public static final String MASTER_KEYSTORE_FILENAME_DEFAULT = "credentials.jceks";

  /**
   * The directory on the ambari-server file system used for storing
   * ambari-agent bootstrap information.
   */
  public static final ConfigurationProperty<String> BOOTSTRAP_DIRECTORY = new ConfigurationProperty<>(
      "bootstrap.dir", AmbariPath.getPath("/var/run/ambari-server/bootstrap"));

  /**
   * The directory on the ambari-server file system used for expanding Views and
   * storing webapp work.
   */
  public static final ConfigurationProperty<String> VIEWS_DIRECTORY = new ConfigurationProperty<>(
      "views.dir", AmbariPath.getPath("/var/lib/ambari-server/resources/views"));

  /**
   *
   */
  public static final ConfigurationProperty<String> VIEWS_VALIDATE = new ConfigurationProperty<>(
      "views.validate", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> VIEWS_REMOVE_UNDEPLOYED = new ConfigurationProperty<>(
      "views.remove.undeployed", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> WEBAPP_DIRECTORY = new ConfigurationProperty<>(
      "webapp.dir", "web");

  /**
   *
   */
  public static final ConfigurationProperty<String> BOOTSTRAP_SCRIPT = new ConfigurationProperty<>(
      "bootstrap.script", AmbariPath.getPath("/usr/bin/ambari_bootstrap"));

  /**
   *
   */
  public static final ConfigurationProperty<String> BOOTSTRAP_SETUP_AGENT_SCRIPT = new ConfigurationProperty<>(
      "bootstrap.setup_agent.script",
      AmbariPath.getPath("/usr/lib/python2.6/site-packages/ambari_server/setupAgent.py"));

  /**
   *
   */
  public static final ConfigurationProperty<String> BOOTSTRAP_SETUP_AGENT_PASSWORD = new ConfigurationProperty<>(
      "bootstrap.setup_agent.password", "password");

  /**
   *
   */
  public static final ConfigurationProperty<String> BOOTSTRAP_MASTER_HOSTNAME = new ConfigurationProperty<>(
      "bootstrap.master_host_name", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> RECOMMENDATIONS_ARTIFACTS_LIFETIME = new ConfigurationProperty<>(
      "recommendations.artifacts.lifetime", "1w");

  /**
   *
   */
  public static final ConfigurationProperty<String> RECOMMENDATIONS_DIR = new ConfigurationProperty<>(
      "recommendations.dir", AmbariPath.getPath("/var/run/ambari-server/stack-recommendations"));

  /**
   *
   */
  public static final ConfigurationProperty<String> STACK_ADVISOR_SCRIPT = new ConfigurationProperty<>(
      "stackadvisor.script",
      AmbariPath.getPath("/var/lib/ambari-server/resources/scripts/stack_advisor.py"));

  /**
   *
   */
  public static final ConfigurationProperty<String> AMBARI_PYTHON_WRAP = new ConfigurationProperty<>(
      "ambari.python.wrap", "ambari-python-wrap");

  /**
   *
   */
  public static final ConfigurationProperty<String> API_AUTHENTICATED_USER = new ConfigurationProperty<>(
      "api.authenticated.user", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> API_USE_SSL = new ConfigurationProperty<>(
      "api.ssl", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> API_CSRF_PREVENTION = new ConfigurationProperty<>(
      "api.csrfPrevention.enabled", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> API_GZIP_COMPRESSION_ENABLED = new ConfigurationProperty<>(
      "api.gzip.compression.enabled", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> API_GZIP_MIN_COMPRESSION_SIZE = new ConfigurationProperty<>(
      "api.gzip.compression.min.size", "10240");

  /**
   *
   */
  public static final ConfigurationProperty<String> AGENT_API_GZIP_COMPRESSION_ENABLED = new ConfigurationProperty<>(
      "agent.api.gzip.compression.enabled", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> AGENT_USE_SSL = new ConfigurationProperty<>(
      "agent.ssl", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_AGENT_HOSTNAME_VALIDATE = new ConfigurationProperty<>(
      "security.agent.hostname.validate", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_TWO_WAY_SSL = new ConfigurationProperty<>(
      "security.server.two_way_ssl", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_TWO_WAY_SSL_PORT = new ConfigurationProperty<>(
      "security.server.two_way_ssl.port", "8441");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_ONE_WAY_SSL_PORT = new ConfigurationProperty<>(
      "security.server.one_way_ssl.port", "8440");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_KSTR_DIR = new ConfigurationProperty<>(
      "security.server.keys_dir", ".");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_CRT_NAME = new ConfigurationProperty<>(
      "security.server.cert_name", "ca.crt");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_CSR_NAME = new ConfigurationProperty<>(
      "security.server.csr_name", "ca.csr");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_KEY_NAME = new ConfigurationProperty<>(
      "security.server.key_name", "ca.key");

  /**
   *
   */
  public static final ConfigurationProperty<String> KSTR_NAME = new ConfigurationProperty<>(
      "security.server.keystore_name", "keystore.p12");

  /**
   * By default self-signed certificates are used and we can use keystore as
   * truststore in PKCS12 format When CA signed certificates are used truststore
   * should be created in JKS format (truststore.jks)
   */
  public static final ConfigurationProperty<String> KSTR_TYPE = new ConfigurationProperty<>(
      "security.server.keystore_type", "PKCS12");

  /**
   *
   */
  public static final ConfigurationProperty<String> TSTR_NAME = new ConfigurationProperty<>(
      "security.server.truststore_name", "keystore.p12");

  /**
   * By default self-signed certificates are used and we can use keystore as
   * truststore in PKCS12 format When CA signed certificates are used truststore
   * should be created in JKS format (truststore.jks)
   */
  public static final ConfigurationProperty<String> TSTR_TYPE = new ConfigurationProperty<>(
      "security.server.truststore_type", "PKCS12");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_CRT_PASS_FILE = new ConfigurationProperty<>(
      "security.server.crt_pass_file", "pass.txt");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_CRT_PASS = new ConfigurationProperty<>(
      "security.server.crt_pass", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_CRT_PASS_LEN = new ConfigurationProperty<>(
      "security.server.crt_pass.len", "50");

  /**
   *
   */
  public static final ConfigurationProperty<String> PASSPHRASE_ENV = new ConfigurationProperty<>(
      "security.server.passphrase_env_var", "AMBARI_PASSPHRASE");

  /**
   *
   */
  public static final ConfigurationProperty<String> PASSPHRASE = new ConfigurationProperty<>(
      "security.server.passphrase", "AMBARI_PASSPHRASE");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_DISABLED_CIPHERS = new ConfigurationProperty<>(
      "security.server.disabled.ciphers", "");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_DISABLED_PROTOCOLS = new ConfigurationProperty<>(
      "security.server.disabled.protocols", "");

  /**
   *
   */
  public static final ConfigurationProperty<String> RESOURCES_DIR = new ConfigurationProperty<>(
      "resources.dir", AmbariPath.getPath("/var/lib/ambari-server/resources/"));

  /**
   *
   */
  public static final ConfigurationProperty<String> METADATA_DIR_PATH = new ConfigurationProperty<>(
      "metadata.path", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> COMMON_SERVICES_DIR_PATH = new ConfigurationProperty<>(
      "common.services.path", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> EXTENSIONS_DIR_PATH = new ConfigurationProperty<>(
      "extensions.path", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> MPACKS_STAGING_DIR_PATH = new ConfigurationProperty<>(
      "mpacks.staging.path", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_VERSION_FILE = new ConfigurationProperty<>(
      "server.version.file", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_VERSION = new ConfigurationProperty<>(
      "version", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> JAVA_HOME = new ConfigurationProperty<>(
      "java.home", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> JDK_NAME = new ConfigurationProperty<>(
      "jdk.name", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> JCE_NAME = new ConfigurationProperty<>(
      "jce.name", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_SECURITY = new ConfigurationProperty<>(
      "client.security", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_PORT = new ConfigurationProperty<>(
      "client.api.port", "8080");

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_PORT = new ConfigurationProperty<>(
      "client.api.ssl.port", "8443");

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KSTR_DIR_NAME = new ConfigurationProperty<>(
      "client.api.ssl.keys_dir", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KSTR_NAME = new ConfigurationProperty<>(
      "client.api.ssl.keystore_name", "https.keystore.p12");

  /**
   * By default self-signed certificates are used and we can use keystore as
   * truststore in PKCS12 format When CA signed certificates are used truststore
   * should be created in JKS format (truststore.jks)
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KSTR_TYPE = new ConfigurationProperty<>(
      "client.api.ssl.keystore_type", "PKCS12");

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_TSTR_NAME = new ConfigurationProperty<>(
      "client.api.ssl.truststore_name", "https.keystore.p12");

  /**
   * By default self-signed certificates are used and we can use keystore as
   * truststore in PKCS12 format When CA signed certificates are used truststore
   * should be created in JKS format (truststore.jks)
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_TSTR_TYPE = new ConfigurationProperty<>(
      "client.api.ssl.truststore_type", "PKCS12");

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_CRT_NAME = new ConfigurationProperty<>(
      "client.api.ssl.cert_name", "https.crt");

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_CRT_PASS_FILE_NAME = new ConfigurationProperty<>(
      "client.api.ssl.cert_pass_file", "https.pass.txt");

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_CRT_PASS = new ConfigurationProperty<>(
      "client.api.ssl.crt_pass", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> CLIENT_API_SSL_KEY_NAME = new ConfigurationProperty<>(
      "client.api.ssl.key_name", "https.key");

  /**
   *
   */
  public static final ConfigurationProperty<String> ENABLE_AUTO_AGENT_CACHE_UPDATE = new ConfigurationProperty<>(
      "agent.auto.cache.update", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> CHECK_REMOTE_MOUNTS = new ConfigurationProperty<>(
      "agent.check.remote.mounts", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> CHECK_MOUNTS_TIMEOUT = new ConfigurationProperty<>(
      "agent.check.mounts.timeout", "0");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_DB_NAME = new ConfigurationProperty<>(
      "server.jdbc.database_name", "ambari");

  /**
   *
   */
  public static final ConfigurationProperty<String> REQUEST_READ_TIMEOUT = new ConfigurationProperty<>(
      "views.request.read.timeout.millis", "10000");

  /**
   *
   */
  public static final ConfigurationProperty<String> REQUEST_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "views.request.connect.timeout.millis", "5000");

  /**
   *
   */
  public static final ConfigurationProperty<String> AMBARI_REQUEST_READ_TIMEOUT = new ConfigurationProperty<>(
      "views.ambari.request.read.timeout.millis", "45000");

  /**
   *
   */
  public static final ConfigurationProperty<String> AMBARI_REQUEST_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "views.ambari.request.connect.timeout.millis", "30000");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_POSTGRES_SCHEMA_NAME = new ConfigurationProperty<>(
      "server.jdbc.postgres.schema", "");

  /**
   *
   */
  public static final ConfigurationProperty<String> OJDBC_JAR_NAME = new ConfigurationProperty<>(
      "db.oracle.jdbc.name", "ojdbc6.jar");

  /**
   *
   */
  public static final ConfigurationProperty<String> MYSQL_JAR_NAME = new ConfigurationProperty<>(
      "db.mysql.jdbc.name", "mysql-connector-java.jar");

  /**
   * For development purposes only, should be changed to 'false'
   */
  public static final ConfigurationProperty<String> IS_LDAP_CONFIGURED = new ConfigurationProperty<>(
      "ambari.ldap.isConfigured", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_USE_SSL = new ConfigurationProperty<>(
      "authentication.ldap.useSSL", "false");

  /**
   * The default value is used for embedded purposes only.
   */
  public static final ConfigurationProperty<String> LDAP_PRIMARY_URL = new ConfigurationProperty<>(
      "authentication.ldap.primaryUrl", "localhost:33389");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_SECONDARY_URL = new ConfigurationProperty<>(
      "authentication.ldap.secondaryUrl", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_BASE_DN = new ConfigurationProperty<>(
      "authentication.ldap.baseDn", "dc=ambari,dc=apache,dc=org");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_BIND_ANONYMOUSLY = new ConfigurationProperty<>(
      "authentication.ldap.bindAnonymously", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_MANAGER_DN = new ConfigurationProperty<>(
      "authentication.ldap.managerDn", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_MANAGER_PASSWORD = new ConfigurationProperty<>(
      "authentication.ldap.managerPassword", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_DN_ATTRIBUTE = new ConfigurationProperty<>(
      "authentication.ldap.dnAttribute", "dn");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_USERNAME_ATTRIBUTE = new ConfigurationProperty<>(
      "authentication.ldap.usernameAttribute", "uid");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_USER_BASE = new ConfigurationProperty<>(
      "authentication.ldap.userBase", "ou=people,dc=ambari,dc=apache,dc=org");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_USER_OBJECT_CLASS = new ConfigurationProperty<>(
      "authentication.ldap.userObjectClass", "person");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_GROUP_BASE = new ConfigurationProperty<>(
      "authentication.ldap.groupBase", "ou=groups,dc=ambari,dc=apache,dc=org");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_GROUP_OBJECT_CLASS = new ConfigurationProperty<>(
      "authentication.ldap.groupObjectClass", "group");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_GROUP_NAMING_ATTR = new ConfigurationProperty<>(
      "authentication.ldap.groupNamingAttr", "cn");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_GROUP_MEMEBERSHIP_ATTR = new ConfigurationProperty<>(
      "authentication.ldap.groupMembershipAttr", "member");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_ADMIN_GROUP_MAPPING_RULES = new ConfigurationProperty<>(
      "authorization.ldap.adminGroupMappingRules", "Ambari Administrators");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_ADMIN_GROUP_MAPPING_MEMBER_ATTR = new ConfigurationProperty<>(
      "authorization.ldap.adminGroupMappingMemberAttr", "Ambari Administrators");

  /**
   * When authentication through LDAP is enabled then Ambari Server uses this
   * filter to lookup the user in LDAP based on the provided ambari user name.
   *
   * If it is not set then
   * {@code (&({usernameAttribute}={0})(objectClass={userObjectClass}))} is
   * used.
   */
  public static final ConfigurationProperty<String> LDAP_USER_SEARCH_FILTER = new ConfigurationProperty<>(
      "authentication.ldap.userSearchFilter",
      "(&({usernameAttribute}={0})(objectClass={userObjectClass}))");

  /**
   * This configuration controls whether the use of alternate user search filter
   * is enabled. If the default LDAP user search filter is not able to find the
   * authenticating user in LDAP than Ambari can fall back an alternative user
   * search filter if this functionality is enabled.
   *
   * If it is not set then the default
   */
  public static final ConfigurationProperty<String> LDAP_ALT_USER_SEARCH_ENABLED = new ConfigurationProperty<>(
      "authentication.ldap.alternateUserSearchEnabled", "false");

  /**
   * When authentication through LDAP is enabled Ambari Server uses this filter
   * by default to lookup the user in LDAP when the user provides beside user
   * name additional information. There might be cases when
   * {@link #LDAP_USER_SEARCH_FILTER} may match multiple users in LDAP. In such
   * cases the user is prompted to provide additional info, e.g. the domain he
   * or she wants ot log in upon login beside the username. This filter will be
   * used by Ambari Server to lookup users in LDAP if the login name the user
   * logs in contains additional information beside ambari user name.
   * <p>
   * Note: Currently the use of alternate user search filter is triggered only
   * if the user login name is in the username@domain format (e.g.
   * user1@x.y.com) which is the userPrincipalName format used in AD.
   * </p>
   */
  public static final ConfigurationProperty<String> LDAP_ALT_USER_SEARCH_FILTER = new ConfigurationProperty<>(
      "authentication.ldap.alternateUserSearchFilter",
      "(&(userPrincipalName={0})(objectClass={userObjectClass}))");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_GROUP_SEARCH_FILTER = new ConfigurationProperty<>(
      "authorization.ldap.groupSearchFilter", "");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_REFERRAL = new ConfigurationProperty<>(
      "authentication.ldap.referral", "follow");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_PAGINATION_ENABLED = new ConfigurationProperty<>(
      "authentication.ldap.pagination.enabled", "true");

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_SYCN_USER_MEMBER_REPLACE_PATTERN = new ConfigurationProperty<>(
      "authentication.ldap.sync.userMemberReplacePattern",
      LDAP_SYNC_MEMBER_REPLACE_PATTERN_DEFAULT);

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_SYCN_GROUP_MEMBER_REPLACE_PATTERN = new ConfigurationProperty<>(
      "authentication.ldap.sync.groupMemberReplacePattern",
      LDAP_SYNC_MEMBER_REPLACE_PATTERN_DEFAULT);

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_SYCN_USER_MEMBER_FILTER = new ConfigurationProperty<>(
      "authentication.ldap.sync.userMemberFilter",
      LDAP_SYNC_MEMBER_FILTER_DEFAULT);

  /**
   *
   */
  public static final ConfigurationProperty<String> LDAP_SYCN_GROUP_MEMBER_FILTER = new ConfigurationProperty<>(
      "authentication.ldap.sync.groupMemberFilter",
      LDAP_SYNC_MEMBER_FILTER_DEFAULT);


  public static final ConfigurationProperty<Long> SERVER_EC_CACHE_SIZE = new ConfigurationProperty<>(
      "server.ecCacheSize", 10000L);

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED = new ConfigurationProperty<>(
      "server.hrcStatusSummary.cache.enabled", Boolean.TRUE);

  /**
   *
   */
  public static final ConfigurationProperty<Long> SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE = new ConfigurationProperty<>(
      "server.hrcStatusSummary.cache.size", 10000L);

  /**
   * The value is specified in {@link TimeUnit#MINUTES}.
   */
  public static final ConfigurationProperty<Long> SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION = new ConfigurationProperty<>(
      "server.hrcStatusSummary.cache.expiryDuration", 30L);

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> SERVER_STALE_CONFIG_CACHE_ENABLED = new ConfigurationProperty<>(
      "server.cache.isStale.enabled", Boolean.TRUE);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_STALE_CONFIG_CACHE_EXPIRATION = new ConfigurationProperty<>(
      "server.cache.isStale.expiration", 600);

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_PERSISTENCE_TYPE = new ConfigurationProperty<>(
      "server.persistence.type", "local");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_USER_NAME = new ConfigurationProperty<>(
      "server.jdbc.user.name", "ambari");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_USER_PASSWD = new ConfigurationProperty<>(
      "server.jdbc.user.passwd", "bigdata");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_DRIVER = new ConfigurationProperty<>(
      "server.jdbc.driver", "org.postgresql.Driver");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_URL = new ConfigurationProperty<>(
      "server.jdbc.url", null);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_HTTP_REQUEST_HEADER_SIZE = new ConfigurationProperty<>(
      "server.http.request.header.size", 64 * 1024);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_HTTP_RESPONSE_HEADER_SIZE = new ConfigurationProperty<>(
      "server.http.response.header.size", 64 * 1024);

  /**
   *
   */
  public static final ConfigurationProperty<String> ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES = new ConfigurationProperty<>(
      "rolling.upgrade.skip.packages.prefixes", "");

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> STACK_UPGRADE_BYPASS_PRECHECKS = new ConfigurationProperty<>(
      "stack.upgrade.bypass.prechecks", Boolean.FALSE);

  /**
   * If a host is shutdown or ambari-agent is stopped, then Ambari Server will still keep waiting til the task timesout,
   * say 10-20 mins. If the host comes back online and ambari-agent is started, then need this retry property
   * to be greater; ideally, it should be greater than 2 * command_timeout in order to retry at least
   * 3 times in that amount of mins.
   * Suggested value is 15-30 mins.
   */
  public static final ConfigurationProperty<Integer> STACK_UPGRADE_AUTO_RETRY_TIMEOUT_MINS = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.timeout.mins", 0);

  /**
   * If the stack.upgrade.auto.retry.timeout.mins property is positive, then run RetryUpgradeActionService every x
   * seconds.
   */
  public static final ConfigurationProperty<Integer> STACK_UPGRADE_AUTO_RETRY_CHECK_INTERVAL_SECS = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.check.interval.secs", 20);

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose custom command name contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   */
  public static final ConfigurationProperty<String> STACK_UPGRADE_AUTO_RETRY_CUSTOM_COMMAND_NAMES_TO_IGNORE = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.command.names.to.ignore",
      "\"ComponentVersionCheckAction\",\"FinalizeUpgradeAction\"");

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose command details contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   */
  public static final ConfigurationProperty<String> STACK_UPGRADE_AUTO_RETRY_COMMAND_DETAILS_TO_IGNORE = new ConfigurationProperty<>(
      "stack.upgrade.auto.retry.command.details.to.ignore", "\"Execute HDFS Finalize\"");

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> JWT_AUTH_ENABLED = new ConfigurationProperty<>(
      "authentication.jwt.enabled", Boolean.FALSE);

  /**
   *
   */
  public static final ConfigurationProperty<String> JWT_AUTH_PROVIDER_URL = new ConfigurationProperty<>(
      "authentication.jwt.providerUrl", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> JWT_PUBLIC = new ConfigurationProperty<>(
      "authentication.jwt.publicKey", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> JWT_AUDIENCES = new ConfigurationProperty<>(
      "authentication.jwt.audiences", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> JWT_COOKIE_NAME = new ConfigurationProperty<>(
      "authentication.jwt.cookieName", "hadoop-jwt");

  /**
   *
   */
  public static final ConfigurationProperty<String> JWT_ORIGINAL_URL_QUERY_PARAM = new ConfigurationProperty<>(
      "authentication.jwt.originalUrlParamName", "originalUrl");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_CONNECTION_POOL = new ConfigurationProperty<>(
      "server.jdbc.connection-pool", ConnectionPoolType.INTERNAL.getName());

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MIN_SIZE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.min-size", 5);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_SIZE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-size", 32);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.acquisition-size", 5);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_AGE = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-age", 0);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-idle-time", 14400);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.max-idle-time-excess", 0);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.idle-test-interval", 7200);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.acquisition-retry-attempts", 30);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY = new ConfigurationProperty<>(
      "server.jdbc.connection-pool.acquisition-retry-delay", 1000);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> OPERATIONS_RETRY_ATTEMPTS = new ConfigurationProperty<>(
      "server.operations.retry-attempts", 0);

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_USER_NAME = new ConfigurationProperty<>(
      "server.jdbc.rca.user.name", "mapred");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_USER_PASSWD = new ConfigurationProperty<>(
      "server.jdbc.rca.user.passwd", "mapred");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_DRIVER = new ConfigurationProperty<>(
      "server.jdbc.rca.driver", "org.postgresql.Driver");

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_JDBC_RCA_URL = new ConfigurationProperty<>(
      "server.jdbc.rca.url", "jdbc:postgresql://" + HOSTNAME_MACRO + "/ambarirca");

  /**
   *
   */
  public static final ConfigurationProperty<JPATableGenerationStrategy> SERVER_JDBC_GENERATE_TABLES = new ConfigurationProperty<>(
      "server.jdbc.generateTables", JPATableGenerationStrategy.NONE);

  /**
   *
   */
  public static final ConfigurationProperty<String> OS_FAMILY = new ConfigurationProperty<>(
      "server.os_family", "");

  /**
   *
   */
  public static final ConfigurationProperty<String> OS_VERSION = new ConfigurationProperty<>(
      "server.os_type", "");

  /**
   *
   */
  public static final ConfigurationProperty<String> SRVR_HOSTS_MAPPING = new ConfigurationProperty<>(
      "server.hosts.mapping", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> SSL_TRUSTSTORE_PATH = new ConfigurationProperty<>(
      "ssl.trustStore.path", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> SSL_TRUSTSTORE_PASSWORD = new ConfigurationProperty<>(
      "ssl.trustStore.password", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> SSL_TRUSTSTORE_TYPE = new ConfigurationProperty<>(
      "ssl.trustStore.type", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> MASTER_KEY_LOCATION = new ConfigurationProperty<>(
      "security.master.key.location", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> MASTER_KEYSTORE_LOCATION = new ConfigurationProperty<>(
      "security.master.keystore.location", null);

  /**
   *
   */
  public static final ConfigurationProperty<Long> TEMPORARYSTORE_RETENTION_MINUTES = new ConfigurationProperty<>(
      "security.temporary.keystore.retention.minutes", 90L);

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> TEMPORARYSTORE_ACTIVELY_PURGE = new ConfigurationProperty<>(
      "security.temporary.keystore.actibely.purge", Boolean.TRUE);

  /**
   * The URL to use when creating messages which should include the Ambari
   * Server URL.
   */
  public static final ConfigurationProperty<String> AMBARI_DISPLAY_URL = new ConfigurationProperty<>(
      "ambari.display.url", null);

  /**
   * Key for repo validation suffixes.
   */
  public static final ConfigurationProperty<String> REPO_SUFFIX_KEY_UBUNTU = new ConfigurationProperty<>(
      "repo.validation.suffixes.ubuntu", "/dists/%s/Release");

  /**
   *
   */
  public static final ConfigurationProperty<String> REPO_SUFFIX_KEY_DEFAULT = new ConfigurationProperty<>(
      "repo.validation.suffixes.default", "/repodata/repomd.xml");

  /**
   *
   */
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_CLUSTERED = new ConfigurationProperty<>(
      "server.execution.scheduler.isClustered", "false");

  /**
   *
   */
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_THREADS = new ConfigurationProperty<>(
      "server.execution.scheduler.maxThreads", "5");

  /**
   *
   */
  public static final ConfigurationProperty<String> EXECUTION_SCHEDULER_CONNECTIONS = new ConfigurationProperty<>(
      "server.execution.scheduler.maxDbConnections", "5");

  /**
   *
   */
  public static final ConfigurationProperty<Long> EXECUTION_SCHEDULER_MISFIRE_TOLERATION = new ConfigurationProperty<>(
      "server.execution.scheduler.misfire.toleration.minutes", 480L);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> EXECUTION_SCHEDULER_START_DELAY = new ConfigurationProperty<>(
      "server.execution.scheduler.start.delay.seconds", 120);

  /**
   * The time that the executions schduler will wait before checking for new
   * commands to schedule. Measure in {@link TimeUnit#SECONDS}.
   */
  public static final ConfigurationProperty<Long> EXECUTION_SCHEDULER_WAIT = new ConfigurationProperty<>(
      "server.execution.scheduler.wait", 1L);

  /**
   *
   */
  public static final ConfigurationProperty<String> SERVER_TMP_DIR = new ConfigurationProperty<>(
      "server.tmp.dir", AmbariPath.getPath("/var/lib/ambari-server/tmp"));

  /**
   *
   */
  public static final ConfigurationProperty<Integer> EXTERNAL_SCRIPT_TIMEOUT = new ConfigurationProperty<>(
      "server.script.timeout", 5000);

  public static final String DEF_ARCHIVE_EXTENSION;
  public static final String DEF_ARCHIVE_CONTENT_TYPE;

  /**
   *
   */
  public static final ConfigurationProperty<String> KDC_PORT = new ConfigurationProperty<>(
      "default.kdcserver.port", "88");

  /**
   *
   */
  public static final ConfigurationProperty<Integer> KDC_CONNECTION_CHECK_TIMEOUT = new ConfigurationProperty<>(
      "kdcserver.connection.check.timeout", 10000);

  /**
   *
   */
  public static final ConfigurationProperty<String> KERBEROSTAB_CACHE_DIR = new ConfigurationProperty<>(
      "kerberos.keytab.cache.dir", AmbariPath.getPath("/var/lib/ambari-server/data/cache"));

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> KERBEROS_CHECK_JAAS_CONFIGURATION = new ConfigurationProperty<>(
      "kerberos.check.jaas.configuration", Boolean.FALSE);

  /**
   *
   */
  public static final ConfigurationProperty<String> RECOVERY_TYPE = new ConfigurationProperty<>(
      "recovery.type", null);

  /**
   *
   */
  public static final ConfigurationProperty<String> RECOVERY_LIFETIME_MAX_COUNT = new ConfigurationProperty<>(
      "recovery.lifetime_max_count", null);

  public static final ConfigurationProperty<String> RECOVERY_MAX_COUNT = new ConfigurationProperty<>(
      "recovery.max_count", null);

  public static final ConfigurationProperty<String> RECOVERY_WINDOW_IN_MIN = new ConfigurationProperty<>(
      "recovery.window_in_minutes", null);

  public static final ConfigurationProperty<String> RECOVERY_RETRY_GAP = new ConfigurationProperty<>(
      "recovery.retry_interval", null);

  public static final ConfigurationProperty<String> RECOVERY_DISABLED_COMPONENTS = new ConfigurationProperty<>(
      "recovery.disabled_components", null);

  public static final ConfigurationProperty<String> RECOVERY_ENABLED_COMPONENTS = new ConfigurationProperty<>(
      "recovery.enabled_components", null);

  /**
   * Allow proxy calls to these hosts and ports only
   */
  public static final ConfigurationProperty<String> PROXY_ALLOWED_HOST_PORTS = new ConfigurationProperty<>(
      "proxy.allowed.hostports", "*:*");

  /**
   * This key defines whether stages of parallel requests are executed in
   * parallel or sequentally. Only stages from different requests
   * running on not interfering host sets may be executed in parallel.
   */
  public static final ConfigurationProperty<Boolean> PARALLEL_STAGE_EXECUTION = new ConfigurationProperty<>(
      "server.stages.parallel", Boolean.TRUE);

  /**
   *
   */
  public static final ConfigurationProperty<Long> AGENT_TASK_TIMEOUT = new ConfigurationProperty<>(
      "agent.task.timeout", 900L);

  /**
   *
   */
  public static final ConfigurationProperty<Long> AGENT_PACKAGE_INSTALL_TASK_TIMEOUT = new ConfigurationProperty<>(
      "agent.package.install.task.timeout", 1800L);

  /**
   * Max number of tasks that may be executed within a single stage.
   * This limitation is used for tasks that when executed in a 1000+ node cluster,
   * may DDOS servers providing downloadable resources
   */
  public static final ConfigurationProperty<Integer> AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT = new ConfigurationProperty<>(
      "agent.package.parallel.commands.limit", 100);

  /**
   * Server side task (default) timeout value
   */
  public static final ConfigurationProperty<Integer> SERVER_TASK_TIMEOUT = new ConfigurationProperty<>(
      "server.task.timeout", 1200);

  /**
   *
   */
  public static final ConfigurationProperty<String> CUSTOM_ACTION_DEFINITION = new ConfigurationProperty<>(
      "custom.action.definitions",
      AmbariPath.getPath("/var/lib/ambari-server/resources/custom_action_definitions"));

  /**
   *
   */
  public static final ConfigurationProperty<String> SHARED_RESOURCES_DIR = new ConfigurationProperty<>(
      "shared.resources.dir",
      AmbariPath.getPath("/usr/lib/ambari-server/lib/ambari_commons/resources"));

  /**
   *
   */
  public static final ConfigurationProperty<String> ANONYMOUS_AUDIT_NAME = new ConfigurationProperty<>(
      "anonymous.audit.name", "_anonymous");

  /**
   * Indicator for sys prepped host
   * It is possible the some nodes are sys prepped and some are not. This can be enabled later
   * by agent over-writing global indicator from ambari-server
   */
  public static final ConfigurationProperty<String> SYS_PREPPED_HOSTS = new ConfigurationProperty<>(
      "packages.pre.installed", "false");

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> BLUEPRINT_SKIP_INSTALL_TASKS = new ConfigurationProperty<>(
      "blueprint.skip_install_tasks", Boolean.FALSE);

  /**
   *
   */
  private static final String LDAP_ADMIN_GROUP_MAPPING_MEMBER_ATTR_DEFAULT = "";

  /**
   *
   */
  public static final ConfigurationProperty<Integer> SERVER_CONNECTION_MAX_IDLE_TIME = new ConfigurationProperty<>(
      "server.connection.max.idle.millis", 900000);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> CLIENT_THREADPOOL_SIZE = new ConfigurationProperty<>(
      "client.threadpool.size.max", 25);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> AGENT_THREADPOOL_SIZE = new ConfigurationProperty<>(
      "agent.threadpool.size.max", 25);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> VIEW_EXTRACTION_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "view.extraction.threadpool.size.max", 20);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> VIEW_EXTRACTION_THREADPOOL_CORE_SIZE = new ConfigurationProperty<>(
      "view.extraction.threadpool.size.core", 10);

  /**
   *
   */
  public static final ConfigurationProperty<Long> VIEW_EXTRACTION_THREADPOOL_TIMEOUT = new ConfigurationProperty<>(
      "view.extraction.threadpool.timeout", 100000L);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> VIEW_REQUEST_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "view.request.threadpool.size.max", 0);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> VIEW_REQUEST_THREADPOOL_TIMEOUT = new ConfigurationProperty<>(
      "view.request.threadpool.timeout", 2000);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> PROPERTY_PROVIDER_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "server.property-provider.threadpool.size.max", PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> PROPERTY_PROVIDER_THREADPOOL_CORE_SIZE = new ConfigurationProperty<>(
      "server.property-provider.threadpool.size.core",
      PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> PROPERTY_PROVIDER_THREADPOOL_WORKER_QUEUE_SIZE = new ConfigurationProperty<>(
      "server.property-provider.threadpool.worker.size", Integer.MAX_VALUE);

  /**
   *
   */
  public static final ConfigurationProperty<Long> PROPERTY_PROVIDER_THREADPOOL_COMPLETION_TIMEOUT = new ConfigurationProperty<>(
      "server.property-provider.threadpool.completion.timeout", 5000L);

  /**
   * The time, in {@link TimeUnit#SECONDS}, that HTTP requests remain valid when
   * inactive.
   */
  public static final ConfigurationProperty<Integer> SERVER_HTTP_SESSION_INACTIVE_TIMEOUT = new ConfigurationProperty<>(
      "server.http.session.inactive_timeout", 1800);

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> TIMELINE_METRICS_CACHE_DISABLE = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.disabled", Boolean.FALSE);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_CACHE_TTL = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.entry.ttl.seconds", 3600);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_CACHE_IDLE_TIME = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.entry.idle.seconds", 1800);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_REQUEST_READ_TIMEOUT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.read.timeout.millis", 10000);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.interval.read.timeout.millis", 10000);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.connect.timeout.millis", 5000);

  /**
   *
   */
  public static final ConfigurationProperty<Long> TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.catchup.interval", 300000L);

  /**
   *
   */
  public static final ConfigurationProperty<String> TIMELINE_METRICS_CACHE_HEAP_PERCENT = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.heap.percent", "15%");

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> TIMELINE_METRICS_CACHE_USE_CUSTOM_SIZING_ENGINE = new ConfigurationProperty<>(
      "server.timeline.metrics.cache.use.custom.sizing.engine", Boolean.TRUE);

  /**
   * Timeline Metrics SSL settings
   */
  public static final ConfigurationProperty<Boolean> AMBARI_METRICS_HTTPS_ENABLED = new ConfigurationProperty<>(
      "server.timeline.metrics.https.enabled", Boolean.FALSE);

  /**
   * Governs the use of {@link Parallel} to process {@link StageEntity}
   * instances into {@link Stage}.
   */
  public static final ConfigurationProperty<Boolean> EXPERIMENTAL_CONCURRENCY_STAGE_PROCESSING_ENABLED = new ConfigurationProperty<>(
      "experimental.concurrency.stage_processing.enabled", Boolean.FALSE);

  /**
   * The full path to the XML file that describes the different alert templates.
   */
  @Markdown(description="The full path to the XML file that describes the different alert templates.")
  public static final ConfigurationProperty<String> ALERT_TEMPLATE_FILE = new ConfigurationProperty<>(
      "alerts.template.file", null);

  /**
   * The maximum number of threads which will handle published alert events.
   */
  public static final ConfigurationProperty<Integer> ALERTS_EXECUTION_SCHEDULER_THREADS = new ConfigurationProperty<>(
      "alerts.execution.scheduler.maxThreads", 2);

  /**
   * If {@code true} then alert information is cached and not immediately
   * persisted in the database.
   */
  public static final ConfigurationProperty<Boolean> ALERTS_CACHE_ENABLED = new ConfigurationProperty<>(
      "alerts.cache.enabled", Boolean.FALSE);

  /**
   * The time after which cached alert information is flushed to the database.
   * Measure in {@link TimeUnit#MINUTES}.
   */
  public static final ConfigurationProperty<Integer> ALERTS_CACHE_FLUSH_INTERVAL = new ConfigurationProperty<>(
      "alerts.cache.flush.interval", 10);

  /**
   * The size of the alert cache.
   */
  public static final ConfigurationProperty<Integer> ALERTS_CACHE_SIZE = new ConfigurationProperty<>(
      "alerts.cache.size", 50000);

  public static final ConfigurationProperty<String> HTTP_STRICT_TRANSPORT_HEADER_VALUE = new ConfigurationProperty<>(
      "http.strict-transport-security", "max-age=31536000");

  /**
   *
   */
  public static final ConfigurationProperty<String> HTTP_X_FRAME_OPTIONS_HEADER_VALUE = new ConfigurationProperty<>(
      "http.x-frame-options", "DENY");

  /**
   *
   */
  public static final ConfigurationProperty<String> HTTP_X_XSS_PROTECTION_HEADER_VALUE = new ConfigurationProperty<>(
      "http.x-xss-protection", "1; mode=block");

  /**
   *
   */
  public static final ConfigurationProperty<String> VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.strict-transport-security", "max-age=31536000");

  /**
   *
   */
  public static final ConfigurationProperty<String> VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.x-frame-options", "SAMEORIGIN");

  /**
   *
   */
  public static final ConfigurationProperty<String> VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE = new ConfigurationProperty<>(
      "views.http.x-xss-protection", "1; mode=block");

  /**
   * The connection timeout for reading version definitions.
   */
  public static final ConfigurationProperty<Integer> VERSION_DEFINITION_CONNECT_TIMEOUT = new ConfigurationProperty<>(
      "server.version_definition.connect.timeout.millis", 5000);

  /**
   * The read timeout for reading version definitions.
   */
  public static final ConfigurationProperty<Integer> VERSION_DEFINITION_READ_TIMEOUT = new ConfigurationProperty<>(
      "server.version_definition.read.timeout.millis", 5000);

  /**
   *
   */
  public static final ConfigurationProperty<Boolean> AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY = new ConfigurationProperty<>(
      "agent.stack.retry.on_repo_unavailability", Boolean.FALSE);

  /**
   *
   */
  public static final ConfigurationProperty<Integer> AGENT_STACK_RETRY_COUNT = new ConfigurationProperty<>(
      "agent.stack.retry.tries", 5);

  /**
   * Main switch for audit log feature
   */
  public static final ConfigurationProperty<Boolean> AUDIT_LOG_ENABLED = new ConfigurationProperty<>(
      "auditlog.enabled", Boolean.TRUE);

  /**
   * Audit logger capacity
   */
  public static final ConfigurationProperty<Integer> AUDIT_LOGGER_CAPACITY = new ConfigurationProperty<>(
      "auditlog.logger.capacity", 10000);

  /**
   *
   */
  public static final ConfigurationProperty<String> ALERTS_SNMP_DISPATCH_UDP_PORT = new ConfigurationProperty<>(
      "alerts.snmp.dispatcher.udp.port", null);

  /**
   * The amount of time, in {@link TimeUnit#MINUTES}, that the
   * {@link MetricsRetrievalService} will cache retrieved metric data.
   */
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_CACHE_TIMEOUT = new ConfigurationProperty<>(
      "metrics.retrieval-service.cache.timeout", 30);

  /**
   * The priorty of the {@link Thread}s used by the
   * {@link MetricsRetrievalService}. This is a value in between
   * {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}.
   */
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREAD_PRIORITY = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.thread.priority", Thread.NORM_PRIORITY);

  /**
   * The maximum size of the threadpool for the {@link MetricsRetrievalService}.
   * This value is only applicable if the
   * {@link #METRIC_RETRIEVAL_SERVICE_THREADPOOL_WORKER_QUEUE_SIZE} is small
   * enough to trigger the {@link ThreadPoolExecutor} to create new threads.
   */
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREADPOOL_MAX_SIZE = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.threadpool.size.max",
      PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT);

  /**
   * The core size of the threadpool for the {@link MetricsRetrievalService}.
   */
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREADPOOL_CORE_SIZE = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.threadpool.size.core",
      PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT);

  /**
   * The size of the worker queue for the {@link MetricsRetrievalService}. The
   * larger this queue is, the less likely it will be to create more threads
   * beyond the core size.
   */
  public static final ConfigurationProperty<Integer> METRIC_RETRIEVAL_SERVICE_THREADPOOL_WORKER_QUEUE_SIZE = new ConfigurationProperty<>(
      "server.metrics.retrieval-service.threadpool.worker.size",
      10 * METRIC_RETRIEVAL_SERVICE_THREADPOOL_MAX_SIZE.getDefaultValue());

  /**
   * The number of tasks that can be queried from the database at once
   * In the case of more tasks, multiple queries are issued
   * @return
   */
  public static final ConfigurationProperty<Integer> TASK_ID_LIST_LIMIT = new ConfigurationProperty<>(
      "task.query.parameterlist.size", 999);

  private static final Logger LOG = LoggerFactory.getLogger(
    Configuration.class);

  private Properties properties;
  private JsonObject hostChangesJson;
  private Map<String, String> configsMap;
  private Map<String, String> agentConfigsMap;
  private CredentialProvider credentialProvider = null;
  private volatile boolean credentialProviderInitialized = false;
  private Properties customDbProperties = null;
  private Properties customPersistenceProperties = null;
  private Long configLastModifiedDateForCustomJDBC = 0L;
  private Long configLastModifiedDateForCustomJDBCToRemove = 0L;
  private Map<String, String> databaseConnectorNames = new HashMap<>();
  private Map<String, String> databasePreviousConnectorNames = new HashMap<>();

  static {
    if (System.getProperty("os.name").contains("Windows")) {
      DEF_ARCHIVE_EXTENSION = ".zip";
      DEF_ARCHIVE_CONTENT_TYPE = "application/zip";
    }
    else {
      DEF_ARCHIVE_EXTENSION = ".tar.gz";
      DEF_ARCHIVE_CONTENT_TYPE = "application/x-ustar";
    }
  }

  /**
   * The {@link DatabaseType} enum represents the database being used.
   */
  public enum DatabaseType {
    POSTGRES("postgres"),
    ORACLE("oracle"),
    MYSQL("mysql"),
    DERBY("derby"),
    SQL_SERVER("sqlserver"),
    SQL_ANYWHERE("sqlanywhere");

    private static final Map<String, DatabaseType> m_mappedTypes =
      new HashMap<String, Configuration.DatabaseType>(5);

    static {
      for (DatabaseType databaseType : EnumSet.allOf(DatabaseType.class)) {
        m_mappedTypes.put(databaseType.getName(), databaseType);
      }
    }

    /**
     * The JDBC URL type name.
     */
    private String m_databaseType;

    /**
     * Constructor.
     *
     */
    private DatabaseType(String databaseType) {
      m_databaseType = databaseType;
    }

    /**
     * Gets an internal name for this database type.
     *
     * @return the internal name for this database type.
     */
    public String getName() {
      return m_databaseType;
    }

    public DatabaseType get(String databaseTypeName) {
      return m_mappedTypes.get(databaseTypeName);
    }
  }

  /**
   * The {@link ConnectionPoolType} is used to define which pooling mechanism
   * JDBC should use.
   */
  public enum ConnectionPoolType {
    INTERNAL("internal"), C3P0("c3p0");

    /**
     * The connection pooling name.
     */
    private String m_name;

    /**
     * Constructor.
     *
     * @param name
     */
    private ConnectionPoolType(String name) {
      m_name = name;
    }

    /**
     * Gets an internal name for this connection pool type.
     *
     * @return the internal name for this connection pool type.
     */
    public String getName() {
      return m_name;
    }
  }

  public Configuration() {
    this(readConfigFile());
  }

  /**
   * This constructor is called from default constructor and
   * also from most tests.
   * @param properties properties to use for testing and in production using
   * the Conf object.
   */
  public Configuration(Properties properties) {
    this.properties = properties;

    agentConfigsMap = new HashMap<String, String>();
    agentConfigsMap.put(CHECK_REMOTE_MOUNTS.getKey(), getProperty(CHECK_REMOTE_MOUNTS));
    agentConfigsMap.put(CHECK_MOUNTS_TIMEOUT.getKey(), getProperty(CHECK_MOUNTS_TIMEOUT));
    agentConfigsMap.put(ENABLE_AUTO_AGENT_CACHE_UPDATE.getKey(), getProperty(ENABLE_AUTO_AGENT_CACHE_UPDATE));

    configsMap = new HashMap<String, String>();
    configsMap.putAll(agentConfigsMap);
    configsMap.put(AMBARI_PYTHON_WRAP.getKey(), getProperty(AMBARI_PYTHON_WRAP));
    configsMap.put(SRVR_AGENT_HOSTNAME_VALIDATE.getKey(), getProperty(SRVR_AGENT_HOSTNAME_VALIDATE));
    configsMap.put(SRVR_TWO_WAY_SSL.getKey(), getProperty(SRVR_TWO_WAY_SSL));
    configsMap.put(SRVR_TWO_WAY_SSL_PORT.getKey(), getProperty(SRVR_TWO_WAY_SSL_PORT));
    configsMap.put(SRVR_ONE_WAY_SSL_PORT.getKey(), getProperty(SRVR_ONE_WAY_SSL_PORT));
    configsMap.put(SRVR_KSTR_DIR.getKey(), getProperty(SRVR_KSTR_DIR));
    configsMap.put(SRVR_CRT_NAME.getKey(), getProperty(SRVR_CRT_NAME));
    configsMap.put(SRVR_KEY_NAME.getKey(), getProperty(SRVR_KEY_NAME));
    configsMap.put(SRVR_CSR_NAME.getKey(), getProperty(SRVR_CSR_NAME));
    configsMap.put(KSTR_NAME.getKey(), getProperty(KSTR_NAME));
    configsMap.put(KSTR_TYPE.getKey(), getProperty(KSTR_TYPE));
    configsMap.put(TSTR_NAME.getKey(), getProperty(TSTR_NAME));
    configsMap.put(TSTR_TYPE.getKey(), getProperty(TSTR_TYPE));
    configsMap.put(SRVR_CRT_PASS_FILE.getKey(), getProperty(SRVR_CRT_PASS_FILE));
    configsMap.put(PASSPHRASE_ENV.getKey(), getProperty(PASSPHRASE_ENV));
    configsMap.put(PASSPHRASE.getKey(), System.getenv(configsMap.get(PASSPHRASE_ENV.getKey())));
    configsMap.put(RESOURCES_DIR.getKey(), getProperty(RESOURCES_DIR));
    configsMap.put(SRVR_CRT_PASS_LEN.getKey(), getProperty(SRVR_CRT_PASS_LEN));
    configsMap.put(SRVR_DISABLED_CIPHERS.getKey(), getProperty(SRVR_DISABLED_CIPHERS));
    configsMap.put(SRVR_DISABLED_PROTOCOLS.getKey(), getProperty(SRVR_DISABLED_PROTOCOLS));

    configsMap.put(CLIENT_API_SSL_KSTR_DIR_NAME.getKey(),
        properties.getProperty(CLIENT_API_SSL_KSTR_DIR_NAME.getKey(),
            configsMap.get(SRVR_KSTR_DIR.getKey())));

    configsMap.put(CLIENT_API_SSL_KSTR_NAME.getKey(), getProperty(CLIENT_API_SSL_KSTR_NAME));
    configsMap.put(CLIENT_API_SSL_KSTR_TYPE.getKey(), getProperty(CLIENT_API_SSL_KSTR_TYPE));
    configsMap.put(CLIENT_API_SSL_TSTR_NAME.getKey(), getProperty(CLIENT_API_SSL_TSTR_NAME));
    configsMap.put(CLIENT_API_SSL_TSTR_TYPE.getKey(), getProperty(CLIENT_API_SSL_TSTR_TYPE));
    configsMap.put(CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(), getProperty(CLIENT_API_SSL_CRT_PASS_FILE_NAME));
    configsMap.put(CLIENT_API_SSL_KEY_NAME.getKey(),getProperty(CLIENT_API_SSL_KEY_NAME));
    configsMap.put(CLIENT_API_SSL_CRT_NAME.getKey(), getProperty(CLIENT_API_SSL_CRT_NAME));
    configsMap.put(JAVA_HOME.getKey(), getProperty(JAVA_HOME));
    configsMap.put(PARALLEL_STAGE_EXECUTION.getKey(), getProperty(PARALLEL_STAGE_EXECUTION));
    configsMap.put(SERVER_TMP_DIR.getKey(), getProperty(SERVER_TMP_DIR));
    configsMap.put(EXTERNAL_SCRIPT_TIMEOUT.getKey(), getProperty(EXTERNAL_SCRIPT_TIMEOUT));
    configsMap.put(SHARED_RESOURCES_DIR.getKey(), getProperty(SHARED_RESOURCES_DIR));
    configsMap.put(KDC_PORT.getKey(), getProperty(KDC_PORT));
    configsMap.put(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT.getKey(), getProperty(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT));
    configsMap.put(PROXY_ALLOWED_HOST_PORTS.getKey(), getProperty(PROXY_ALLOWED_HOST_PORTS));

    File passFile = new File(
        configsMap.get(SRVR_KSTR_DIR.getKey()) + File.separator
            + configsMap.get(SRVR_CRT_PASS_FILE.getKey()));

    String password = null;

    if (!passFile.exists()) {
      LOG.info("Generation of file with password");
      try {
        password = RandomStringUtils.randomAlphanumeric(Integer
            .parseInt(configsMap.get(SRVR_CRT_PASS_LEN.getKey())));
        FileUtils.writeStringToFile(passFile, password);
        ShellCommandUtil.setUnixFilePermissions(
          ShellCommandUtil.MASK_OWNER_ONLY_RW, passFile.getAbsolutePath());
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(
          "Error reading certificate password from file");
      }
    } else {
      LOG.info("Reading password from existing file");
      try {
        password = FileUtils.readFileToString(passFile);
        password = password.replaceAll("\\p{Cntrl}", "");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    configsMap.put(SRVR_CRT_PASS.getKey(), password);

    if (getApiSSLAuthentication()) {
      LOG.info("API SSL Authentication is turned on.");
      File httpsPassFile = new File(configsMap.get(CLIENT_API_SSL_KSTR_DIR_NAME.getKey())
          + File.separator + configsMap.get(CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey()));

      if (httpsPassFile.exists()) {
        LOG.info("Reading password from existing file");
        try {
          password = FileUtils.readFileToString(httpsPassFile);
          password = password.replaceAll("\\p{Cntrl}", "");
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException("Error reading certificate password from" +
            " file " + httpsPassFile.getAbsolutePath());
        }
      } else {
        LOG.error("There is no keystore for https UI connection.");
        LOG.error("Run \"ambari-server setup-https\" or set " + Configuration.API_USE_SSL.getKey()
            + " = false.");
        throw new RuntimeException("Error reading certificate password from " +
          "file " + httpsPassFile.getAbsolutePath());

      }

      configsMap.put(CLIENT_API_SSL_CRT_PASS.getKey(), password);
    }

    loadSSLParams();
  }

  /**
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * Gets a copy of all of the configuration properties that back this
   * {@link Configuration} instance.
   *
   * @return a copy of all of the properties.
   */
  public Properties getProperties() {
    return new Properties(properties);
  }

  /**
   * Gets the value for the specified {@link ConfigurationProperty}. If the
   * value hasn't been set then the default value as specified in
   * {@link ConfigurationProperty#getDefaultValue()} will be returned.
   *
   * @param configurationProperty
   * @return
   */
  public <T> String getProperty(ConfigurationProperty<T> configurationProperty) {
    String defaultStringValue = null;
    if (null != configurationProperty.getDefaultValue()) {
      defaultStringValue = String.valueOf(configurationProperty.getDefaultValue());
    }

    return properties.getProperty(configurationProperty.getKey(), defaultStringValue);
  }

  public void setProperty(ConfigurationProperty<String> configurationProperty, String value) {
    properties.setProperty(configurationProperty.getKey(), value);
  }

  /**
   * Loads trusted certificates store properties
   */
  protected void loadSSLParams(){
    if (getProperty(SSL_TRUSTSTORE_PATH) != null) {
      System.setProperty(JAVAX_SSL_TRUSTSTORE, getProperty(SSL_TRUSTSTORE_PATH));
    }
    if (getProperty(SSL_TRUSTSTORE_PASSWORD) != null) {
      String ts_password = readPasswordFromStore(
          getProperty(SSL_TRUSTSTORE_PASSWORD));
      if (ts_password != null) {
        System.setProperty(JAVAX_SSL_TRUSTSTORE_PASSWORD, ts_password);
      } else {
        System.setProperty(JAVAX_SSL_TRUSTSTORE_PASSWORD,
            getProperty(SSL_TRUSTSTORE_PASSWORD));
      }
    }
    if (getProperty(SSL_TRUSTSTORE_TYPE) != null) {
      System.setProperty(JAVAX_SSL_TRUSTSTORE_TYPE, getProperty(SSL_TRUSTSTORE_TYPE));
    }
  }

  private synchronized void loadCredentialProvider() {
    if (!credentialProviderInitialized) {
      try {
        credentialProvider = new CredentialProvider(null,
          getMasterKeyLocation(),
          isMasterKeyPersisted(),
          getMasterKeyStoreLocation());
      } catch (Exception e) {
        LOG.info("Credential provider creation failed. Reason: " + e.getMessage());
        if (LOG.isDebugEnabled()) {
          e.printStackTrace();
        }
        credentialProvider = null;
      }
      credentialProviderInitialized = true;
    }
  }

  /**
   * Find, read, and parse the configuration file.
   * @return the properties that were found or empty if no file was found
   */
  private static Properties readConfigFile() {
    Properties properties = new Properties();

    //Get property file stream from classpath
    InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

    if (inputStream == null) {
      throw new RuntimeException(CONFIG_FILE + " not found in classpath");
    }

    // load the properties
    try {
      properties.load(inputStream);
      inputStream.close();
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + CONFIG_FILE + " found in classpath.", fnf);
    } catch (IOException ie) {
      throw new IllegalArgumentException("Can't read configuration file " +
        CONFIG_FILE, ie);
    }

    return properties;
  }

  public Map<String, String> getDatabaseConnectorNames() {
    File file = new File(Configuration.class.getClassLoader().getResource(CONFIG_FILE).getPath());
    Long currentConfigLastModifiedDate = file.lastModified();
    Properties properties = null;
    if (currentConfigLastModifiedDate.longValue() != configLastModifiedDateForCustomJDBC.longValue()) {
      LOG.info("Ambari properties config file changed.");
      if (configLastModifiedDateForCustomJDBC != null) {
        properties = readConfigFile();
      } else {
        properties = this.properties;
      }

      for (String propertyName : dbConnectorPropertyNames) {
        String propertyValue = properties.getProperty(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
          databaseConnectorNames.put(propertyName.replace(".", "_"), propertyValue);
        }
      }

      configLastModifiedDateForCustomJDBC = currentConfigLastModifiedDate;
    }

    return databaseConnectorNames;
  }

  public Map<String, String> getPreviousDatabaseConnectorNames() {
    File file = new File(Configuration.class.getClassLoader().getResource(CONFIG_FILE).getPath());
    Long currentConfigLastModifiedDate = file.lastModified();
    Properties properties = null;
    if (currentConfigLastModifiedDate.longValue() != configLastModifiedDateForCustomJDBCToRemove.longValue()) {
      LOG.info("Ambari properties config file changed.");
      if (configLastModifiedDateForCustomJDBCToRemove != null) {
        properties = readConfigFile();
      } else {
        properties = this.properties;
      }

      for (String propertyName : dbConnectorPropertyNames) {
        propertyName = "previous." + propertyName;
        String propertyValue = properties.getProperty(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
          databasePreviousConnectorNames.put(propertyName.replace(".", "_"), propertyValue);
        }
      }

      configLastModifiedDateForCustomJDBCToRemove = currentConfigLastModifiedDate;
    }

    return databasePreviousConnectorNames;
  }

  public JsonObject getHostChangesJson(String hostChangesFile) {
    if (hostChangesJson == null) {
      hostChangesJson = readFileToJSON(hostChangesFile);
    }
    return hostChangesJson;
  }

  private JsonObject readFileToJSON (String file) {

    // Read from File to String
    JsonObject jsonObject = new JsonObject();

    try {
      JsonParser parser = new JsonParser();
      JsonElement jsonElement = parser.parse(new FileReader(file));
      jsonObject = jsonElement.getAsJsonObject();
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("No file " + file, e);
    }

    return jsonObject;
  }

  /**
   * Get the views directory.
   *
   * @return the views directory
   */
  public File getViewsDir() {
    String fileName = getProperty(VIEWS_DIRECTORY);
    return new File(fileName);
  }

  /**
   * Determine whether or not view validation is enabled.
   *
   * @return true if view validation is enabled
   */
  public boolean isViewValidationEnabled() {
    return Boolean.parseBoolean(getProperty(VIEWS_VALIDATE));
  }

  /**
   * Determine whether or not a view that has been undeployed (archive deleted) should be removed from the database.
   *
   * @return true if undeployed views should be removed
   */
  public boolean isViewRemoveUndeployedEnabled() {
    return Boolean.parseBoolean(getProperty(VIEWS_REMOVE_UNDEPLOYED));
  }

  /**
   * @return conventional Java version number, e.g. 7.
   * Integer is used here to simplify comparisons during usage.
   * If java version is not supported, returns -1
   */
  public int getJavaVersion() {
    String versionStr = System.getProperty("java.version");
    if (versionStr.startsWith("1.6")) {
      return 6;
    } else if (versionStr.startsWith("1.7")) {
      return 7;
    } else if (versionStr.startsWith("1.8")) {
      return 8;
    } else { // Some unsupported java version
      return -1;
    }
  }

  public File getBootStrapDir() {
    String fileName = getProperty(BOOTSTRAP_DIRECTORY);
    return new File(fileName);
  }

  public String getBootStrapScript() {
    return getProperty(BOOTSTRAP_SCRIPT);
  }

  public String getBootSetupAgentScript() {
    return getProperty(BOOTSTRAP_SETUP_AGENT_SCRIPT);
  }

  public String getBootSetupAgentPassword() {
    String pass = configsMap.get(PASSPHRASE.getKey());

    if (null != pass) {
      return pass;
    }

    // fallback
    return getProperty(BOOTSTRAP_SETUP_AGENT_PASSWORD);
  }

  public File getRecommendationsDir() {
    String fileName = getProperty(RECOMMENDATIONS_DIR);
    return new File(fileName);
  }

  public String getRecommendationsArtifactsLifetime() {
    return getProperty(RECOMMENDATIONS_ARTIFACTS_LIFETIME);
  }

  public String areHostsSysPrepped(){
    return getProperty(SYS_PREPPED_HOSTS);
  }

  public boolean skipInstallTasks(){
    String skipInstallCommandsProperty = getProperty(BLUEPRINT_SKIP_INSTALL_TASKS);
    return Boolean.parseBoolean(areHostsSysPrepped()) && Boolean.parseBoolean(skipInstallCommandsProperty);
  }

  public String getStackAdvisorScript() {
    return getProperty(STACK_ADVISOR_SCRIPT);
  }

  /**
   * @return a list of prefixes. Packages whose name starts with any of these
   * prefixes, should be skipped during upgrade.
   */
  public List<String> getRollingUpgradeSkipPackagesPrefixes() {
    String propertyValue = getProperty(ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES);
    ArrayList<String> res = new ArrayList<>();
    for (String prefix : propertyValue.split(",")) {
      if (! prefix.isEmpty()) {
        res.add(prefix.trim());
      }
    }
    return res;
  }

  /**
   * Determine whether or not a Rolling/Express upgrade can bypass the PreChecks. Default value should be false.
   *
   * @return true if RU/EU can bypass PreChecks, otherwise, false.
   */
  public boolean isUpgradePrecheckBypass() {
    return Boolean.parseBoolean(getProperty(STACK_UPGRADE_BYPASS_PRECHECKS));
  }

  /**
   * During stack upgrade, can auto-retry failures for up to x mins. This is useful to improve the robustness in unstable environments.
   * Suggested value is 0-30 mins.
   * @return
   */
  public int getStackUpgradeAutoRetryTimeoutMins() {
    Integer result = NumberUtils.toInt(getProperty(STACK_UPGRADE_AUTO_RETRY_TIMEOUT_MINS));
    return result >= 0 ? result : 0;
  }

  /**
   * If the stack.upgrade.auto.retry.timeout.mins property is positive, then run RetryUpgradeActionService every x
   * seconds.
   * @return Number of seconds between runs of {@link org.apache.ambari.server.state.services.RetryUpgradeActionService}
   */
  public int getStackUpgradeAutoRetryCheckIntervalSecs() {
    Integer result = NumberUtils.toInt(getProperty(STACK_UPGRADE_AUTO_RETRY_CHECK_INTERVAL_SECS));
    return result >= 0 ? result : 0;
  }

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose custom command name contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   * @return
   */
  public List<String> getStackUpgradeAutoRetryCustomCommandNamesToIgnore() {
    String value = getProperty(STACK_UPGRADE_AUTO_RETRY_CUSTOM_COMMAND_NAMES_TO_IGNORE);
    List<String> list = convertCSVwithQuotesToList(value);
    listToLowerCase(list);
    return list;
  }

  /**
   * If auto-retry during stack upgrade is enabled, skip any tasks whose command details contains at least one
   * of the strings in the following CSV property. Note that values have to be enclosed in quotes and separated by commas.
   * @return
   */
  public List<String> getStackUpgradeAutoRetryCommandDetailsToIgnore() {
    String value = getProperty(STACK_UPGRADE_AUTO_RETRY_COMMAND_DETAILS_TO_IGNORE);
    List<String> list = convertCSVwithQuotesToList(value);
    listToLowerCase(list);
    return list;
  }

  /**
   * Convert quoted elements separated by commas into a list. Values cannot contain double quotes or commas.
   * @param value, e.g., String with value "a","b","c" => ["a", "b", "c"]
   * @return List of parsed values, or empty list if no values exist.
   */
  private List<String> convertCSVwithQuotesToList(String value) {
    List<String> list = new ArrayList<>();
    if (StringUtils.isNotEmpty(value)) {
      if (value.indexOf(",") >= 0) {
        for (String e : value.split(",")) {
          e = StringUtils.stripStart(e, "\"");
          e = StringUtils.stripEnd(e, "\"");
          list.add(e);
        }
      } else {
        list.add(value);
      }
    }
    return list;
  }

  /**
   * Convert the elements of a list to lowercase.
   * @param list
   */
  private void listToLowerCase(List<String> list) {
    if (list == null) {
      return;
    }
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toLowerCase());
    }
  }

  /**
   * Get the map with server config parameters.
   * Keys - public constants of this class
   * @return the map with server config parameters
   */
  public Map<String, String> getConfigsMap() {
    return configsMap;
  }

  /**
   * Get the map with server config parameters related to agent configuration.
   * Keys - public constants of this class
   * @return the map with server config parameters related to agent configuration
   */
  public Map<String, String> getAgentConfigsMap() {
    return agentConfigsMap;
  }

  /**
   * Checks if CSRF protection enabled
   * @return true if CSRF protection filter should be enabled
   */
  public boolean csrfProtectionEnabled() {
    return Boolean.parseBoolean(getProperty(API_CSRF_PREVENTION));
  }

  /**
   * Gets client security type
   * @return appropriate ClientSecurityType
   */
  public ClientSecurityType getClientSecurityType() {
    return ClientSecurityType.fromString(getProperty(CLIENT_SECURITY));
  }

  public void setClientSecurityType(ClientSecurityType type) {
    setProperty(CLIENT_SECURITY, type.toString());
  }

  public String getWebAppDir() {
    return getProperty(WEBAPP_DIRECTORY.getKey());
  }

  /**
   * Get the file that will be used for host mapping.
   * @return null if such a file is not present, value if present.
   */
  public String getHostsMapFile() {
    LOG.info("Hosts Mapping File " + getProperty(SRVR_HOSTS_MAPPING));
    return getProperty(SRVR_HOSTS_MAPPING);
  }

  /**
   * Gets ambari stack-path
   * @return String
   */
  public String getMetadataPath() {
    return getProperty(METADATA_DIR_PATH);
  }

  /**
   * Gets ambari common services path
   * @return String
   */
  public String getCommonServicesPath() {
    return getProperty(COMMON_SERVICES_DIR_PATH);
  }

  /**
   * Gets ambari extensions-path
   * @return String
   */
  public String getExtensionsPath() {
    return getProperty(EXTENSIONS_DIR_PATH);
  }

  /**
   * Gets ambari management packs staging directory
   * @return String
   */
  public String getMpacksStagingPath() {
    return getProperty(MPACKS_STAGING_DIR_PATH);
  }


  public String getServerVersionFilePath() {
    return getProperty(SERVER_VERSION_FILE);
  }

  /**
   * Gets ambari server version
   * @return version String
   */
  public String getServerVersion() {
    try {
      return FileUtils.readFileToString(new File(getServerVersionFilePath())).trim();
    } catch (IOException e) {
      LOG.error("Unable to read server version file", e);
    }
    return null;
  }

  /**
   * Gets the username of the default user assumed to be executing API calls.
   * <p/>
   * If this value is <code>null</code> or empty then no default user is set and one must be
   * specified when issuing API calls.
   *
   * @return the username of a user.
   */
  public String getDefaultApiAuthenticatedUser() {
    return properties.getProperty(API_AUTHENTICATED_USER.getKey());
  }

  /**
   * Gets ssl api port
   * @return int
   */
  public int getClientSSLApiPort() {
    return Integer.parseInt(getProperty(CLIENT_API_SSL_PORT));
  }

  /**
   * Check to see if the API should be authenticated via ssl or not
   * @return false if not, true if ssl needs to be used.
   */
  public boolean getApiSSLAuthentication() {
    return Boolean.parseBoolean(getProperty(API_USE_SSL));
  }

  /**
   * Check to see if the Agent should be authenticated via ssl or not
   * @return false if not, true if ssl needs to be used.
   */
  public boolean getAgentSSLAuthentication() {
    return Boolean.parseBoolean(getProperty(AGENT_USE_SSL));
  }

  /**
   * Get the value that should be set for the <code>Strict-Transport-Security</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>max-age=31536000; includeSubDomains</code>. For example:
   * <p/>
   * <code>
   * Strict-Transport-Security: max-age=31536000; includeSubDomains
   * </code>
   * <p/>
   * This value may be ignored when {@link #getApiSSLAuthentication()} is <code>false</code>.
   *
   * @return the Strict-Transport-Security value - null or "" indicates that the value is not set
   */
  public String getStrictTransportSecurityHTTPResponseHeader() {
    return getProperty(HTTP_STRICT_TRANSPORT_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-Frame-Options</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>DENY</code>. For example:
   * <p/>
   * <code>
   * X-Frame-Options: DENY
   * </code>
   *
   * @return the X-Frame-Options value - null or "" indicates that the value is not set
   */
  public String getXFrameOptionsHTTPResponseHeader() {
    return getProperty(HTTP_X_FRAME_OPTIONS_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-XSS-Protection</code> HTTP response header for Ambari Server UI.
   * <p/>
   * By default this will be <code>1; mode=block</code>. For example:
   * <p/>
   * <code>
   * X-XSS-Protection: 1; mode=block
   * </code>
   *
   * @return the X-XSS-Protection value - null or "" indicates that the value is not set
   */
  public String getXXSSProtectionHTTPResponseHeader() {
    return getProperty(HTTP_X_XSS_PROTECTION_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>Strict-Transport-Security</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>max-age=31536000; includeSubDomains</code>. For example:
   * <p/>
   * <code>
   * Strict-Transport-Security: max-age=31536000; includeSubDomains
   * </code>
   * <p/>
   * This value may be ignored when {@link #getApiSSLAuthentication()} is <code>false</code>.
   *
   * @return the Strict-Transport-Security value - null or "" indicates that the value is not set
   */
  public String getViewsStrictTransportSecurityHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-Frame-Options</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>DENY</code>. For example:
   * <p/>
   * <code>
   * X-Frame-Options: DENY
   * </code>
   *
   * @return the X-Frame-Options value - null or "" indicates that the value is not set
   */
  public String getViewsXFrameOptionsHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE);
  }

  /**
   * Get the value that should be set for the <code>X-XSS-Protection</code> HTTP response header for Ambari Views.
   * <p/>
   * By default this will be <code>1; mode=block</code>. For example:
   * <p/>
   * <code>
   * X-XSS-Protection: 1; mode=block
   * </code>
   *
   * @return the X-XSS-Protection value - null or "" indicates that the value is not set
   */
  public String getViewsXXSSProtectionHTTPResponseHeader() {
    return getProperty(VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE);
  }

  /**
   * Check to see if the hostname of the agent is to be validated as a proper hostname or not
   *
   * @return true if agent hostnames should be checked as a valid hostnames; otherwise false
   */
  public boolean validateAgentHostnames() {
    return Boolean.parseBoolean(getProperty(SRVR_AGENT_HOSTNAME_VALIDATE));
  }

  /**
   * Check to see if two-way SSL auth should be used between server and agents
   * or not
   *
   * @return true two-way SSL authentication is enabled
   */
  public boolean getTwoWaySsl() {
    return Boolean.parseBoolean(getProperty(SRVR_TWO_WAY_SSL));
  }

  /**
   * Check to see if the API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isApiGzipped() {
    return Boolean.parseBoolean(getProperty(API_GZIP_COMPRESSION_ENABLED));
  }

  /**
   * Check to see if the agent API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isAgentApiGzipped() {
    return Boolean.parseBoolean(getProperty(AGENT_API_GZIP_COMPRESSION_ENABLED));
  }

  /**
   * Check to see if the API responses should be compressed via gzip or not
   * Content will only be compressed if content length is either unknown or
   * greater this value
   * @return false if not, true if ssl needs to be used.
   */
  public String getApiGzipMinSize() {
    return getProperty(API_GZIP_MIN_COMPRESSION_SIZE);
  }

  /**
   * Check persistence type Ambari Server should use. Possible values:
   * in-memory - use in-memory Derby database to store data
   * local - use local Postgres instance
   * remote - use provided jdbc driver name and url to connect to database
   */
  public PersistenceType getPersistenceType() {
    String value = getProperty(SERVER_PERSISTENCE_TYPE);
    return PersistenceType.fromString(value);
  }

  public String getDatabaseDriver() {
    if (getPersistenceType() != PersistenceType.IN_MEMORY) {
      return getProperty(SERVER_JDBC_DRIVER);
    } else {
      return JDBC_IN_MEMORY_DRIVER;
    }
  }

  public String getDatabaseUrl() {
    if (getPersistenceType() != PersistenceType.IN_MEMORY) {
      String URI = getProperty(SERVER_JDBC_URL);
      if (URI != null) {
        return URI;
      } else {
        return getLocalDatabaseUrl();
      }
    } else {
      return JDBC_IN_MEMORY_URL;
    }
  }

  public String getLocalDatabaseUrl() {
    String dbName = properties.getProperty(SERVER_DB_NAME.getKey());
    if(dbName == null || dbName.isEmpty()) {
      throw new RuntimeException("Server DB Name is not configured!");
    }

    return JDBC_LOCAL_URL + dbName;
  }

  public String getDatabaseUser() {
    return getProperty(SERVER_JDBC_USER_NAME);
  }

  public String getDatabasePassword() {
    String passwdProp = properties.getProperty(SERVER_JDBC_USER_PASSWD.getKey());
    String dbpasswd = null;
    boolean isPasswordAlias = false;
    if (CredentialProvider.isAliasString(passwdProp)) {
      dbpasswd = readPasswordFromStore(passwdProp);
      isPasswordAlias =true;
    }

    if (dbpasswd != null) {
      return dbpasswd;
    } else if (dbpasswd == null && isPasswordAlias) {
      LOG.error("Can't read db password from keystore. Please, check master key was set correctly.");
      throw new RuntimeException("Can't read db password from keystore. Please, check master key was set correctly.");
    } else {
      return readPasswordFromFile(passwdProp, SERVER_JDBC_USER_PASSWD.getDefaultValue());
    }
  }

  public String getRcaDatabaseDriver() {
    return getProperty(SERVER_JDBC_RCA_DRIVER);
  }

  public String getRcaDatabaseUrl() {
    return getProperty(SERVER_JDBC_RCA_URL);
  }

  public String getRcaDatabaseUser() {
    return getProperty(SERVER_JDBC_RCA_USER_NAME);
  }

  public String getRcaDatabasePassword() {
    String passwdProp = properties.getProperty(SERVER_JDBC_RCA_USER_PASSWD.getKey());
    if (passwdProp != null) {
      String dbpasswd = readPasswordFromStore(passwdProp);
      if (dbpasswd != null) {
        return dbpasswd;
      }
    }
    return readPasswordFromFile(passwdProp, SERVER_JDBC_RCA_USER_PASSWD.getDefaultValue());
  }

  private String readPasswordFromFile(String filePath, String defaultPassword) {
    if (filePath == null) {
      LOG.debug("DB password file not specified - using default");
      return defaultPassword;
    } else {
      LOG.debug("Reading password from file {}", filePath);
      String password;
      try {
        password = FileUtils.readFileToString(new File(filePath));
        password = StringUtils.chomp(password);
      } catch (IOException e) {
        throw new RuntimeException("Unable to read database password", e);
      }
      return password;
    }
  }

  String readPasswordFromStore(String aliasStr) {
    String password = null;
    loadCredentialProvider();
    if (credentialProvider != null) {
      char[] result = null;
      try {
        result = credentialProvider.getPasswordForAlias(aliasStr);
      } catch (AmbariException e) {
        LOG.error("Error reading from credential store.");
        e.printStackTrace();
      }
      if (result != null) {
        password = new String(result);
      } else {
        LOG.error("Cannot read password for alias = " + aliasStr);
      }
    }
    return password;
  }

  /**
   * Gets parameters of LDAP server to connect to
   * @return LdapServerProperties object representing connection parameters
   */
  public LdapServerProperties getLdapServerProperties() {
    LdapServerProperties ldapServerProperties = new LdapServerProperties();

    ldapServerProperties.setPrimaryUrl(getProperty(LDAP_PRIMARY_URL));
    ldapServerProperties.setSecondaryUrl(getProperty(LDAP_SECONDARY_URL));
    ldapServerProperties.setUseSsl(Boolean.parseBoolean(getProperty(LDAP_USE_SSL)));
    ldapServerProperties.setAnonymousBind(Boolean.parseBoolean(getProperty(LDAP_BIND_ANONYMOUSLY)));
    ldapServerProperties.setManagerDn(getProperty(LDAP_MANAGER_DN));
    String ldapPasswordProperty = getProperty(LDAP_MANAGER_PASSWORD);
    String ldapPassword = null;
    if (CredentialProvider.isAliasString(ldapPasswordProperty)) {
      ldapPassword = readPasswordFromStore(ldapPasswordProperty);
    }
    if (ldapPassword != null) {
      ldapServerProperties.setManagerPassword(ldapPassword);
    } else {
      if (ldapPasswordProperty != null && new File(ldapPasswordProperty).exists()) {
        ldapServerProperties.setManagerPassword(readPasswordFromFile(ldapPasswordProperty, ""));
      }
    }

    ldapServerProperties.setBaseDN(getProperty(LDAP_BASE_DN));
    ldapServerProperties.setUsernameAttribute(getProperty(LDAP_USERNAME_ATTRIBUTE));
    ldapServerProperties.setUserBase(getProperty(LDAP_USER_BASE));
    ldapServerProperties.setUserObjectClass(getProperty(LDAP_USER_OBJECT_CLASS));
    ldapServerProperties.setDnAttribute(getProperty(LDAP_DN_ATTRIBUTE));
    ldapServerProperties.setGroupBase(getProperty(LDAP_GROUP_BASE));
    ldapServerProperties.setGroupObjectClass(getProperty(LDAP_GROUP_OBJECT_CLASS));
    ldapServerProperties.setGroupMembershipAttr(getProperty(LDAP_GROUP_MEMEBERSHIP_ATTR));
    ldapServerProperties.setGroupNamingAttr(getProperty(LDAP_GROUP_NAMING_ATTR));
    ldapServerProperties.setAdminGroupMappingRules(getProperty(LDAP_ADMIN_GROUP_MAPPING_RULES));
    ldapServerProperties.setAdminGroupMappingMemberAttr(getProperty(LDAP_ADMIN_GROUP_MAPPING_MEMBER_ATTR_DEFAULT));
    ldapServerProperties.setUserSearchFilter(getProperty(LDAP_USER_SEARCH_FILTER));
    ldapServerProperties.setAlternateUserSearchFilter(getProperty(LDAP_ALT_USER_SEARCH_FILTER));
    ldapServerProperties.setGroupSearchFilter(getProperty(LDAP_GROUP_SEARCH_FILTER));
    ldapServerProperties.setReferralMethod(getProperty(LDAP_REFERRAL));
    ldapServerProperties.setSyncUserMemberReplacePattern(getProperty(LDAP_SYCN_USER_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncGroupMemberReplacePattern(getProperty(LDAP_SYCN_GROUP_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncUserMemberFilter(getProperty(LDAP_SYCN_USER_MEMBER_FILTER));
    ldapServerProperties.setSyncGroupMemberFilter(getProperty(LDAP_SYCN_GROUP_MEMBER_FILTER));
    ldapServerProperties.setPaginationEnabled(
        Boolean.parseBoolean(getProperty(LDAP_PAGINATION_ENABLED)));

    if (properties.containsKey(LDAP_GROUP_BASE) || properties.containsKey(LDAP_GROUP_OBJECT_CLASS)
        || properties.containsKey(LDAP_GROUP_MEMEBERSHIP_ATTR)
        || properties.containsKey(LDAP_GROUP_NAMING_ATTR)
        || properties.containsKey(LDAP_ADMIN_GROUP_MAPPING_RULES)
        || properties.containsKey(LDAP_GROUP_SEARCH_FILTER)) {
      ldapServerProperties.setGroupMappingEnabled(true);
    }

    return ldapServerProperties;
  }

  public boolean isLdapConfigured() {
    return Boolean.parseBoolean(getProperty(IS_LDAP_CONFIGURED));
  }

  public String getServerOsType() {
    return getProperty(OS_VERSION);
  }

  public String getServerOsFamily() {
    return getProperty(OS_FAMILY);
  }

  public String getMasterHostname(String defaultValue) {
    return properties.getProperty(BOOTSTRAP_MASTER_HOSTNAME.getKey(), defaultValue);
  }

  public int getClientApiPort() {
    return Integer.parseInt(getProperty(CLIENT_API_PORT));
  }

  public String getOjdbcJarName() {
    return getProperty(OJDBC_JAR_NAME);
  }

  public String getJavaHome() {
    return getProperty(JAVA_HOME);
  }

  public String getJDKName() {
    return getProperty(JDK_NAME);
  }

  public String getJCEName() {
    return getProperty(JCE_NAME);
  }

  public String getServerDBName() {
    return getProperty(SERVER_DB_NAME);
  }

  public String getMySQLJarName() {
    return getProperty(MYSQL_JAR_NAME);
  }

  public JPATableGenerationStrategy getJPATableGenerationStrategy() {
    return JPATableGenerationStrategy.fromString(
        System.getProperty(SERVER_JDBC_GENERATE_TABLES.getKey()));
  }

  public int getConnectionMaxIdleTime() {
    return Integer.parseInt(getProperty(SERVER_CONNECTION_MAX_IDLE_TIME));
  }

  /**
   * @return the name to be used for audit information if there is no
   * logged-in user.  Default is '_anonymous'.
   */
  public String getAnonymousAuditName() {
    return getProperty(ANONYMOUS_AUDIT_NAME);
  }

  public boolean isMasterKeyPersisted() {
    File masterKeyFile = getMasterKeyLocation();
    return (masterKeyFile != null) && masterKeyFile.exists();
  }

  public File getServerKeyStoreDirectory() {
    String path = getProperty(SRVR_KSTR_DIR);
    return ((path == null) || path.isEmpty())
      ? new File(".")
      : new File(path);
  }

  /**
   * Returns a File pointing where master key file is expected to be
   * <p/>
   * The master key file is named 'master'. The directory that this file is to
   * be found in is calculated by obtaining the directory path assigned to the
   * Ambari property 'security.master.key.location'; else if that value is
   * empty, then the directory is determined by calling
   * {@link #getServerKeyStoreDirectory()}.
   * <p/>
   * If it exists, this file contains the key used to decrypt values stored in
   * the master keystore.
   *
   * @return a File that points to the master key file
   * @see #getServerKeyStoreDirectory()
   * @see #MASTER_KEY_FILENAME_DEFAULT
   */
  public File getMasterKeyLocation() {
    File location;
    String path = getProperty(MASTER_KEY_LOCATION);

    if (StringUtils.isEmpty(path)) {
      location = new File(getServerKeyStoreDirectory(), MASTER_KEY_FILENAME_DEFAULT);
      LOG.debug("Value of {} is not set, using {}", MASTER_KEY_LOCATION, location.getAbsolutePath());
    } else {
      location = new File(path, MASTER_KEY_FILENAME_DEFAULT);
      LOG.debug("Value of {} is {}", MASTER_KEY_LOCATION, location.getAbsolutePath());
    }

    return location;
  }

  /**
   * Returns the location of the master keystore file.
   * <p/>
   * The master keystore file is named 'credentials.jceks'. The directory that
   * this file is to be found in is calculated by obtaining the directory path
   * assigned to the Ambari property 'security.master.keystore.location'; else
   * if that value is empty, then the directory is determined by calling
   * {@link #getServerKeyStoreDirectory()}.
   * <p/>
   * The location is calculated by obtaining the Ambari property directory path
   * assigned to the key 'security.master.keystore.location'. If that value is
   * empty, then the directory is determined by
   * {@link #getServerKeyStoreDirectory()}.
   *
   * @return a File that points to the master keystore file
   * @see #getServerKeyStoreDirectory()
   * @see #MASTER_KEYSTORE_FILENAME_DEFAULT
   */
  public File getMasterKeyStoreLocation() {
    File location;
    String path = getProperty(MASTER_KEYSTORE_LOCATION);

    if (StringUtils.isEmpty(path)) {
      location = new File(getServerKeyStoreDirectory(), MASTER_KEYSTORE_FILENAME_DEFAULT);
      LOG.debug("Value of {} is not set, using {}", MASTER_KEYSTORE_LOCATION,
          location.getAbsolutePath());
    } else {
      location = new File(path, MASTER_KEYSTORE_FILENAME_DEFAULT);
      LOG.debug("Value of {} is {}", MASTER_KEYSTORE_LOCATION, location.getAbsolutePath());
    }

    return location;
  }

  /**
   * Gets the temporary keystore retention time in minutes.
   * <p/>
   * This value is retrieved from the Ambari property named 'security.temporary.keystore.retention.minutes'.
   * If not set, the default value of 90 (minutes) will be returned.
   *
   * @return a timeout value (in minutes)
   */
  public long getTemporaryKeyStoreRetentionMinutes() {
    long minutes;
    String value = getProperty(TEMPORARYSTORE_RETENTION_MINUTES);

    if(StringUtils.isEmpty(value)) {
      LOG.debug("Value of {} is not set, using default value ({})",
          TEMPORARYSTORE_RETENTION_MINUTES.getKey(),
          TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue());

      minutes = TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue();
    }
    else {
      try {
        minutes = Long.parseLong(value);
        LOG.debug("Value of {} is {}", TEMPORARYSTORE_RETENTION_MINUTES, value);
      } catch (NumberFormatException e) {
        LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
            TEMPORARYSTORE_RETENTION_MINUTES.getKey(), value,
            TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue());
        minutes = TEMPORARYSTORE_RETENTION_MINUTES.getDefaultValue();
      }
    }

    return minutes;
  }

  /**
   * Gets a boolean value indicating whether to actively purge the temporary keystore when the retention
   * time expires (true) or to passively purge when credentials are queried (false).
   * <p/>
   * This value is retrieved from the Ambari property named 'security.temporary.keystore.actibely.purge'.
   * If not set, the default value of true.
   *
   * @return a Boolean value declaring whether to actively (true) or passively (false) purge the temporary keystore
   */
  public boolean isActivelyPurgeTemporaryKeyStore() {
    String value = getProperty(TEMPORARYSTORE_ACTIVELY_PURGE);

    if (StringUtils.isEmpty(value)) {
      LOG.debug("Value of {} is not set, using default value ({})",
          TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue());
      return TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue();
    } else if ("true".equalsIgnoreCase(value)) {
      LOG.debug("Value of {} is {}", TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), value);
      return true;
    } else if ("false".equalsIgnoreCase(value)) {
      LOG.debug("Value of {} is {}", TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), value);
      return false;
    } else {
      LOG.warn("Value of {} should be either \"true\" or \"false\" but is \"{}\", falling back to default value ({})",
          TEMPORARYSTORE_ACTIVELY_PURGE.getKey(), value,
          TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue());
      return TEMPORARYSTORE_ACTIVELY_PURGE.getDefaultValue();
    }
  }

  public String getSrvrDisabledCiphers() {
    String disabledCiphers = getProperty(SRVR_DISABLED_CIPHERS);
    return disabledCiphers.trim();
  }

  public String getSrvrDisabledProtocols() {
    String disabledProtocols = getProperty(SRVR_DISABLED_PROTOCOLS);
    return disabledProtocols.trim();
  }

  public int getOneWayAuthPort() {
    return Integer.parseInt(getProperty(SRVR_ONE_WAY_SSL_PORT));
  }

  public int getTwoWayAuthPort() {
    return Integer.parseInt(getProperty(SRVR_TWO_WAY_SSL_PORT));
  }

  /**
   * Gets all properties that begin with {@value #SERVER_JDBC_PROPERTIES_PREFIX}
   * , removing the prefix. The properties are then pre-pending with
   * {@code eclipselink.jdbc.property.} before being returned.
   * <p/>
   * These properties are used to pass JDBC driver-specific connection
   * properties to EclipseLink.
   * <p/>
   * server.jdbc.properties.loginTimeout ->
   * eclipselink.jdbc.property.loginTimeout <br/>
   * server.jdbc.properties.oraclecustomname ->
   * eclipselink.jdbc.property.oraclecustomname
   *
   * @return custom properties for database connections
   */
  public Properties getDatabaseCustomProperties() {
    if (null != customDbProperties) {
      return customDbProperties;
    }

    customDbProperties = new Properties();

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.startsWith(SERVER_JDBC_PROPERTIES_PREFIX)) {
        key = "eclipselink.jdbc.property." + key.substring(SERVER_JDBC_PROPERTIES_PREFIX.length());
        customDbProperties.put(key, val);
      }
    }

    return customDbProperties;
  }

  /**
   * Gets all properties that begin with
   * {@value #SERVER_PERSISTENCE_PROPERTIES_PREFIX} , removing the prefix. These
   * properties are used to pass JPA-specific properties to the persistence
   * provider (such as EclipseLink).
   * <p/>
   * server.persistence.properties.eclipselink.jdbc.batch-writing.size=25 ->
   * eclipselink.jdbc.batch-writing.size=25
   *
   * @return custom properties for database connections
   */
  public Properties getPersistenceCustomProperties() {
    if (null != customPersistenceProperties) {
      return customPersistenceProperties;
    }

    customPersistenceProperties = new Properties();

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.startsWith(SERVER_PERSISTENCE_PROPERTIES_PREFIX)) {
        key = key.substring(SERVER_PERSISTENCE_PROPERTIES_PREFIX.length());
        customPersistenceProperties.put(key, val);
      }
    }

    return customPersistenceProperties;
  }

  /**
   * @return Custom property for request header size
   */
  public int getHttpRequestHeaderSize() {
    return Integer.parseInt(getProperty(SERVER_HTTP_REQUEST_HEADER_SIZE));
  }

  /**
   * @return Custom property for response header size
   */
  public int getHttpResponseHeaderSize() {
    return Integer.parseInt(getProperty(SERVER_HTTP_RESPONSE_HEADER_SIZE));
  }

  public Map<String, String> getAmbariProperties() {

    Properties properties = readConfigFile();
    Map<String, String> ambariPropertiesMap = new HashMap<String, String>();

    for(String key : properties.stringPropertyNames()) {
      ambariPropertiesMap.put(key, properties.getProperty(key));
    }
    return ambariPropertiesMap;
  }

  public long getExecutionCommandsCacheSize() {
    String stringValue = getProperty(SERVER_EC_CACHE_SIZE);
    long value = SERVER_EC_CACHE_SIZE.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Long.valueOf(stringValue);
      } catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * Caching of host role command status summary can be enabled/disabled
   * through the {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED} config property.
   * This method returns the value of {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED}
   * config property. If this config property is not defined than returns the default defined by {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT}.
   * @return true if caching is to be enabled otherwise false.
   */
  public boolean getHostRoleCommandStatusSummaryCacheEnabled() {
    String stringValue = getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED);
    boolean value = SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Boolean.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * In order to avoid the cache storing host role command status summary objects exhaust
   * memory we set a max record number allowed for the cache. This limit can be configured
   * through {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE} config property. The method returns
   * the value of this config property. If this config property is not defined than
   * the default value specified by {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT} is returned.
   * @return the upper limit for the number of cached host role command summaries.
   */
  public long getHostRoleCommandStatusSummaryCacheSize() {
    String stringValue = getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Long.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * As a safety measure the cache storing host role command status summaries should auto expire after a while.
   * The expiry duration is specified through the {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION} config property
   * expressed in minutes. The method returns the value of this config property. If this config property is not defined than
   * the default value specified by {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT}
   * @return the cache expiry duration in minutes
   */
  public long getHostRoleCommandStatusSummaryCacheExpiryDuration() {
    String stringValue = getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION.getDefaultValue();
    if (stringValue != null) {
      try {
        value = Long.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }



  /**
   * @return whether staleConfig's flag is cached.
   */
  public boolean isStaleConfigCacheEnabled() {
    return Boolean.parseBoolean(getProperty(SERVER_STALE_CONFIG_CACHE_ENABLED));
  }

  /**
   * @return expiration time of stale config cache
   */
  public Integer staleConfigCacheExpiration() {
    return Integer.parseInt(getProperty(SERVER_STALE_CONFIG_CACHE_EXPIRATION));
  }

  /**
   * @return a string array of suffixes used to validate repo URLs.
   */
  public String[] getRepoValidationSuffixes(String osType) {
    String repoSuffixes;

    if(osFamily.isUbuntuFamily(osType)) {
      repoSuffixes = getProperty(REPO_SUFFIX_KEY_UBUNTU);
    } else {
      repoSuffixes = getProperty(REPO_SUFFIX_KEY_DEFAULT);
    }

    return repoSuffixes.split(",");
  }


  public String isExecutionSchedulerClusterd() {
    return getProperty(EXECUTION_SCHEDULER_CLUSTERED);
  }

  public String getExecutionSchedulerThreads() {
    return getProperty(EXECUTION_SCHEDULER_THREADS);
  }

  public Integer getRequestReadTimeout() {
    return Integer.parseInt(getProperty(REQUEST_READ_TIMEOUT));
  }

  public Integer getRequestConnectTimeout() {
    return Integer.parseInt(getProperty(REQUEST_CONNECT_TIMEOUT));
  }

  /**
   * @return The read timeout value for views when trying to access ambari apis
   */
  public Integer getViewAmbariRequestReadTimeout() {
    return Integer.parseInt(getProperty(AMBARI_REQUEST_READ_TIMEOUT));
  }

  /**
   * @return The connection timeout value for views when trying to connect to ambari apis
   */
  public Integer getViewAmbariRequestConnectTimeout() {
    return Integer.parseInt(getProperty(AMBARI_REQUEST_CONNECT_TIMEOUT));
  }

  public String getExecutionSchedulerConnections() {
    return getProperty(EXECUTION_SCHEDULER_CONNECTIONS);
  }

  public Long getExecutionSchedulerMisfireToleration() {
    return Long.parseLong(getProperty(EXECUTION_SCHEDULER_MISFIRE_TOLERATION));
  }

  public Integer getExecutionSchedulerStartDelay() {
    return Integer.parseInt(getProperty(EXECUTION_SCHEDULER_START_DELAY));
  }

  public Long getExecutionSchedulerWait() {

    String stringValue = getProperty(EXECUTION_SCHEDULER_WAIT);
    Long sleepTime = EXECUTION_SCHEDULER_WAIT.getDefaultValue();
    if (stringValue != null) {
      try {
        sleepTime = Long.valueOf(stringValue);
      } catch (NumberFormatException ignored) {
        LOG.warn("Value of {} ({}) should be a number, " +
            "falling back to default value ({})", EXECUTION_SCHEDULER_WAIT.getKey(), stringValue,
            EXECUTION_SCHEDULER_WAIT.getDefaultValue());
      }

    }

    if (sleepTime > 60) {
      LOG.warn("Value of {} ({}) should be a number between 1 adn 60, " +
          "falling back to maximum value ({})",
          EXECUTION_SCHEDULER_WAIT, sleepTime, 60);
      sleepTime = 60L;
    }
    return sleepTime*1000;
  }

  public Integer getExternalScriptTimeout() {
    return Integer.parseInt(getProperty(EXTERNAL_SCRIPT_TIMEOUT));
  }

  public boolean getParallelStageExecution() {
    return Boolean.parseBoolean(configsMap.get(PARALLEL_STAGE_EXECUTION.getKey()));
  }

  public String getCustomActionDefinitionPath() {
    return getProperty(CUSTOM_ACTION_DEFINITION);
  }

  public int getAgentPackageParallelCommandsLimit() {
    int value = Integer.parseInt(getProperty(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT));
    if (value < 1) {
      value = 1;
    }
    return value;
  }

  /**
   * @param isPackageInstallationTask true, if task is for installing packages
   * @return default task timeout in seconds (string representation). This value
   *         is used at python (agent) code.
   */
  public String getDefaultAgentTaskTimeout(boolean isPackageInstallationTask) {
    ConfigurationProperty<Long> configurationProperty = isPackageInstallationTask
        ? AGENT_PACKAGE_INSTALL_TASK_TIMEOUT
        : AGENT_TASK_TIMEOUT;

    String key = configurationProperty.getKey();
    Long defaultValue = configurationProperty.getDefaultValue();
    String value = getProperty(configurationProperty);
    if (StringUtils.isNumeric(value)) {
      return value;
    } else {
      LOG.warn(String.format("Value of %s (%s) should be a number, " +
          "falling back to default value (%s)",
        key, value, defaultValue));

      return String.valueOf(defaultValue);
    }
  }

  /**
   * @return default server-side task timeout in seconds.
   */
  public Integer getDefaultServerTaskTimeout() {
    String value = getProperty(SERVER_TASK_TIMEOUT);
    if (StringUtils.isNumeric(value)) {
      return Integer.parseInt(value);
    } else {
      LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
          SERVER_TASK_TIMEOUT.getKey(), value, SERVER_TASK_TIMEOUT.getDefaultValue());
      return SERVER_TASK_TIMEOUT.getDefaultValue();
    }
  }

  public String getResourceDirPath() {
    return getProperty(RESOURCES_DIR);
  }

  public String getSharedResourcesDirPath(){
    return getProperty(SHARED_RESOURCES_DIR);
  }

  public String getServerJDBCPostgresSchemaName() {
    return getProperty(SERVER_JDBC_POSTGRES_SCHEMA_NAME);
  }

  /**
   * @return max thread pool size for clients, default 25
   */
  public int getClientThreadPoolSize() {
    return Integer.parseInt(getProperty(CLIENT_THREADPOOL_SIZE));
  }

  /**
   * @return max thread pool size for agents, default 25
   */
  public int getAgentThreadPoolSize() {
    return Integer.parseInt(getProperty(AGENT_THREADPOOL_SIZE));
  }

  /**
   * Get the view extraction thread pool max size.
   *
   * @return the view extraction thread pool max size
   */
  public int getViewExtractionThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(VIEW_EXTRACTION_THREADPOOL_MAX_SIZE));
  }

  /**
   * Get the view extraction thread pool core size.
   *
   * @return the view extraction thread pool core size
   */
  public int getViewExtractionThreadPoolCoreSize() {
    return Integer.parseInt(getProperty(VIEW_EXTRACTION_THREADPOOL_CORE_SIZE));
  }

  /**
   * Get the maximum number of threads that will be allocated to fulfilling view
   * requests.
   *
   * @return the maximum number of threads that will be allocated for requests
   *         to load views.
   */
  public int getViewRequestThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(VIEW_REQUEST_THREADPOOL_MAX_SIZE));
  }

  /**
   * Get the time, in ms, that a request to a view will wait for an available
   * thread to handle the request before returning an error.
   *
   * @return the time that requests for a view should wait for an available
   *         thread.
   */
  public int getViewRequestThreadPoolTimeout() {
    return Integer.parseInt(getProperty(VIEW_REQUEST_THREADPOOL_TIMEOUT));
  }

  /**
   * Get property-providers' thread pool core size.
   *
   * @return the property-providers' thread pool core size
   */
  public int getPropertyProvidersThreadPoolCoreSize() {
    return Integer.parseInt(getProperty(PROPERTY_PROVIDER_THREADPOOL_CORE_SIZE));
  }

  /**
   * Get property-providers' thread pool max size.
   *
   * @return the property-providers' thread pool max size
   */
  public int getPropertyProvidersThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(PROPERTY_PROVIDER_THREADPOOL_MAX_SIZE));
  }

  /**
   * Get property-providers' worker queue size. This will return
   * {@link Integer#MAX_VALUE} if not specified which will allow an unbounded
   * queue and essentially a fixed core threadpool size.
   *
   * @return the property-providers' worker queue size.
   */
  public int getPropertyProvidersWorkerQueueSize() {
    return Integer.parseInt(getProperty(PROPERTY_PROVIDER_THREADPOOL_WORKER_QUEUE_SIZE));
  }

  /**
   * Get property-providers' timeout value in milliseconds for waiting on the
   * completion of submitted {@link Callable}s. This will return {@value 5000}
   * if not specified.
   *
   * @return the property-providers' completion srevice timeout, in millis.
   */
  public long getPropertyProvidersCompletionServiceTimeout() {
    return Long.parseLong(getProperty(PROPERTY_PROVIDER_THREADPOOL_COMPLETION_TIMEOUT));
  }

  /**
   * Get the view extraction thread pool timeout.
   *
   * @return the view extraction thread pool timeout
   */
  public long getViewExtractionThreadPoolTimeout() {
    return Integer.parseInt(getProperty(VIEW_EXTRACTION_THREADPOOL_TIMEOUT));
  }

  /**
   * Gets the inactivity timeout value, in seconds, for sessions created in
   * Jetty by Spring Security. Without this timeout value, each request to the
   * REST APIs will create new sessions that are never reaped since their
   * default time is -1.
   *
   * @return the time value or {@code 1800} seconds for default.
   */
  public int getHttpSessionInactiveTimeout() {
    return Integer.parseInt(getProperty(SERVER_HTTP_SESSION_INACTIVE_TIMEOUT));
  }

  /**
   * Gets the location of the XML alert template file which contains the
   * velocity templates for outbound notifications.
   *
   * @return the location of the template file, or {@code null} if not defined.
   */
  public String getAlertTemplateFile() {
    return StringUtils.strip(getProperty(ALERT_TEMPLATE_FILE));
  }

  /**
   * @return max thread pool size for AlertEventPublisher, default 2
   */
  public int getAlertEventPublisherPoolSize() {
    return Integer.parseInt(getProperty(ALERTS_EXECUTION_SCHEDULER_THREADS));
  }

  /**
   * Get the node recovery type DEFAULT|AUTO_START|FULL
   * @return
   */
  public String getNodeRecoveryType() {
    return getProperty(RECOVERY_TYPE);
  }

  /**
   * Get configured max count of recovery attempt allowed per host component in a window
   * This is reset when agent is restarted.
   * @return
   */
  public String getNodeRecoveryMaxCount() {
    return getProperty(RECOVERY_MAX_COUNT);
  }

  /**
   * Get configured max lifetime count of recovery attempt allowed per host component.
   * This is reset when agent is restarted.
   * @return
   */
  public String getNodeRecoveryLifetimeMaxCount() {
    return getProperty(RECOVERY_LIFETIME_MAX_COUNT);
  }

  /**
   * Get configured window size in minutes
   * @return
   */
  public String getNodeRecoveryWindowInMin() {
    return getProperty(RECOVERY_WINDOW_IN_MIN);
  }

  /**
   * Get the components for which recovery is disabled
   * @return
   */
  public String getRecoveryDisabledComponents() {
    return getProperty(RECOVERY_DISABLED_COMPONENTS);
  }

  /**
   * Get the components for which recovery is enabled
   * @return
   */
  public String getRecoveryEnabledComponents() {
    return getProperty(RECOVERY_ENABLED_COMPONENTS);
  }

  /**
   * Get the configured retry gap between tries per host component
   * @return
   */
  public String getNodeRecoveryRetryGap() {
    return getProperty(RECOVERY_RETRY_GAP);
  }

  /**

  /**
   * Gets the default KDC port to use when no port is specified in KDC hostname
   *
   * @return the default KDC port to use.
   */
  public String getDefaultKdcPort() {
    return getProperty(KDC_PORT);
  }

  /**
   * Gets the inactivity timeout value, in milliseconds, for socket connection
   * made to KDC Server for its reachability verification.
   *
   * @return the timeout value as configured in {@code ambari.properties}
   * 				 or {@code 10000 ms} for default.
   */
  public int getKdcConnectionCheckTimeout() {
    return Integer.parseInt(getProperty(KDC_CONNECTION_CHECK_TIMEOUT));
  }

  /**
   * Gets the directory where Ambari is to store cached keytab files.
   *
   * @return a File containing the path to the directory to use to store cached keytab files
   */
  public File getKerberosKeytabCacheDir() {
    return new File(getProperty(KERBEROSTAB_CACHE_DIR));
  }

  /**
   * Determine whether or not ambari server credentials validation is enabled.
   *
   * @return true if ambari server credentials check is enabled
   */
  public boolean isKerberosJaasConfigurationCheckEnabled() {
    return Boolean.parseBoolean(getProperty(KERBEROS_CHECK_JAAS_CONFIGURATION));
  }

  /**
   * Gets the type of database by examining the {@link #getDatabaseUrl()} JDBC
   * URL.
   *
   * @return the database type (never {@code null}).
   * @throws RuntimeException
   *           if there no known database type.
   */
  public DatabaseType getDatabaseType() {
    String dbUrl = getDatabaseUrl();
    DatabaseType databaseType;

    if (dbUrl.contains(DatabaseType.POSTGRES.getName())) {
      databaseType = DatabaseType.POSTGRES;
    } else if (dbUrl.contains(DatabaseType.ORACLE.getName())) {
      databaseType = DatabaseType.ORACLE;
    } else if (dbUrl.contains(DatabaseType.MYSQL.getName())) {
      databaseType = DatabaseType.MYSQL;
    } else if (dbUrl.contains(DatabaseType.DERBY.getName())) {
      databaseType = DatabaseType.DERBY;
    } else if (dbUrl.contains(DatabaseType.SQL_SERVER.getName())) {
      databaseType = DatabaseType.SQL_SERVER;
    } else if (dbUrl.contains(DatabaseType.SQL_ANYWHERE.getName())) {
      databaseType = DatabaseType.SQL_ANYWHERE;
    } else {
      throw new RuntimeException(
        "The database type could be not determined from the JDBC URL "
          + dbUrl);
    }

    return databaseType;
  }

  /**
   * Gets the schema name of database
   *
   * @return the database schema name (can return {@code null} for any DB besides Postgres, MySQL, Oracle).
   */
  public String getDatabaseSchema() {
    DatabaseType databaseType = getDatabaseType();
    String databaseSchema;

    if (databaseType.equals(DatabaseType.POSTGRES)) {
      databaseSchema = getServerJDBCPostgresSchemaName();
    } else if (databaseType.equals(DatabaseType.MYSQL)) {
      databaseSchema = getServerDBName();
    } else if (databaseType.equals(DatabaseType.ORACLE)) {
      databaseSchema = getDatabaseUser();
    } else if (databaseType.equals(DatabaseType.DERBY)) {
      databaseSchema = DEFAULT_DERBY_SCHEMA;
    } else {
      databaseSchema = null;
    }

    return databaseSchema;
  }

  /**
   * Gets the type of connection pool that EclipseLink should use.
   *
   * @return default of {@link ConnectionPoolType#INTERNAL}.
   */
  public ConnectionPoolType getConnectionPoolType(){
    String connectionPoolType = getProperty(SERVER_JDBC_CONNECTION_POOL);

    if (connectionPoolType.equals(ConnectionPoolType.C3P0.getName())) {
      return ConnectionPoolType.C3P0;
    }

    return ConnectionPoolType.INTERNAL;
  }

  /**
   * Gets the minimum number of connections that should always exist in the
   * connection pool.
   *
   * @return default of {@value #SERVER_JDBC_CONNECTION_POOL_MIN_SIZE}
   */
  public int getConnectionPoolMinimumSize() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MIN_SIZE));
  }

  /**
   * Gets the maximum number of connections that should even exist in the
   * connection pool.
   *
   * @return default of {@value #SERVER_JDBC_CONNECTION_POOL_MAX_SIZE}
   */
  public int getConnectionPoolMaximumSize() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_SIZE));
  }

  /**
   * Gets the maximum amount of time in seconds any connection, whether its been
   * idle or active, should even be in the pool. This will terminate the
   * connection after the expiration age and force new connections to be opened.
   *
   * @return default of {@value #SERVER_JDBC_CONNECTION_POOL_MAX_AGE}
   */
  public int getConnectionPoolMaximumAge() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_AGE));
  }

  /**
   * Gets the maximum amount of time in seconds that an idle connection can
   * remain in the pool. This should always be greater than the value returned
   * from {@link #getConnectionPoolMaximumExcessIdle()}
   *
   * @return default of {@value #SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME}
   */
  public int getConnectionPoolMaximumIdle() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME));
  }

  /**
   * Gets the maximum amount of time in seconds that connections beyond the
   * minimum pool size should remain in the pool. This should always be less
   * than than the value returned from {@link #getConnectionPoolMaximumIdle()}
   *
   * @return default of
   *         {@value #SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS}
   */
  public int getConnectionPoolMaximumExcessIdle() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS));
  }

  /**
   * Gets the number of connections that should be retrieved when the pool size
   * must increase. It's wise to set this higher than 1 since the assumption is
   * that a pool that needs to grow should probably grow by more than 1.
   *
   * @return default of {@value #SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE}
   */
  public int getConnectionPoolAcquisitionSize() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE));
  }

  /**
   * Gets the number of times connections should be retried to be acquired from
   * the database before giving up.
   *
   * @return default of
   *         {@value #SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS}
   */
  public int getConnectionPoolAcquisitionRetryAttempts() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS));
  }

  /**
   * Gets the delay in milliseconds between connection acquire attempts.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_ACQUISITION_RETRY_DELAY}
   */
  public int getConnectionPoolAcquisitionRetryDelay() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY));
  }


  /**
   * Gets the number of seconds in between testing each idle connection in the
   * connection pool for validity.
   *
   * @return default of {@value #SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL}
   */
  public int getConnectionPoolIdleTestInternval() {
    return Integer.parseInt(getProperty(SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL));
  }

  /**
   * Sets a property on the configuration.
   *
   * @param key
   *          the key (not {@code null}).
   * @param value
   *          the value, or {@code null} to remove it.
   */
  public void setProperty(String key, String value) {
    if (null == value) {
      properties.remove(key);
    } else {
      properties.setProperty(key, value);
    }
  }

  /**
   * Eviction time for entries in metrics cache.
   */
  public int getMetricCacheTTLSeconds() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_CACHE_TTL));
  }

  /**
   * Max time to idle for entries in the cache.
   */
  public int getMetricCacheIdleSeconds() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_CACHE_IDLE_TIME));
  }

  /**
   * Separate timeout settings for metrics cache.
   * @return milliseconds
   */
  public int getMetricsRequestReadTimeoutMillis() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_REQUEST_READ_TIMEOUT));
  }

  /**
   * Separate timeout settings for metrics cache.
   * Timeout on reads for update requests made for smaller time intervals.
   *
   * @return milliseconds
   */
  public int getMetricsRequestIntervalReadTimeoutMillis() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT));
  }

  /**
   * Separate timeout settings for metrics cache.
   * @return milliseconds
   */
  public int getMetricsRequestConnectTimeoutMillis() {
    return Integer.parseInt(getProperty(TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT));
  }

  /**
   * Diable metrics caching.
   * @return true / false
   */
  public boolean isMetricsCacheDisabled() {
    return Boolean.parseBoolean(getProperty(TIMELINE_METRICS_CACHE_DISABLE));
  }

  /**
   * Constant fudge factor subtracted from the cache update requests to
   * account for unavailability of data on the trailing edge due to buffering.
   */
  public Long getMetricRequestBufferTimeCatchupInterval() {
    return Long.parseLong(getProperty(TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL));
  }

  /**
   * Percentage of total heap allocated to metrics cache, default is 15%.
   * Default heap setting for the server is 2 GB so max allocated heap size
   * for this cache is 300 MB.
   */
  public String getMetricsCacheManagerHeapPercent() {
    String percent = getProperty(TIMELINE_METRICS_CACHE_HEAP_PERCENT);
    return percent.trim().endsWith("%") ? percent.trim() : percent.trim() + "%";
  }

  /**
   * Allow disabling custom sizing engine.
   */
  public boolean useMetricsCacheCustomSizingEngine() {
    return Boolean.parseBoolean(getProperty(TIMELINE_METRICS_CACHE_USE_CUSTOM_SIZING_ENGINE));
  }

  /**
   * Get set of properties desribing SSO configuration (JWT)
   */
  public JwtAuthenticationProperties getJwtProperties() {
    boolean enableJwt = Boolean.valueOf(getProperty(JWT_AUTH_ENABLED));

    if (enableJwt) {
      String providerUrl = getProperty(JWT_AUTH_PROVIDER_URL);
      if (providerUrl == null) {
        LOG.error("JWT authentication provider URL not specified. JWT auth will be disabled.", providerUrl);
        return null;
      }
      String publicKeyPath = getProperty(JWT_PUBLIC);
      if (publicKeyPath == null) {
        LOG.error("Public key pem not specified for JWT auth provider {}. JWT auth will be disabled.", providerUrl);
        return null;
      }
      try {
        RSAPublicKey publicKey = CertificateUtils.getPublicKeyFromFile(publicKeyPath);
        JwtAuthenticationProperties jwtProperties = new JwtAuthenticationProperties();
        jwtProperties.setAuthenticationProviderUrl(providerUrl);
        jwtProperties.setPublicKey(publicKey);

        jwtProperties.setCookieName(getProperty(JWT_COOKIE_NAME));
        jwtProperties.setAudiencesString(getProperty(JWT_AUDIENCES));
        jwtProperties.setOriginalUrlQueryParam(getProperty(JWT_ORIGINAL_URL_QUERY_PARAM));

        return jwtProperties;

      } catch (IOException e) {
        LOG.error("Unable to read public certificate file. JWT auth will be disabled.", e);
        return null;
      } catch (CertificateException e) {
        LOG.error("Unable to parse public certificate file. JWT auth will be disabled.", e);
        return null;
      }
    } else {
      return null;
    }

  }

  /**
   * Ambari server temp dir
   * @return server temp dir
   */
  public String getServerTempDir() {
    return getProperty(SERVER_TMP_DIR);
  }

  /**
   * Gets whether to use experiemental concurrent processing to convert
   * {@link StageEntity} instances into {@link Stage} instances. The default is
   * {@code false}.
   *
   * @return {code true} if the experimental feature is enabled, {@code false}
   *         otherwise.
   */
  @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
  public boolean isExperimentalConcurrentStageProcessingEnabled() {
    return Boolean.parseBoolean(getProperty(EXPERIMENTAL_CONCURRENCY_STAGE_PROCESSING_ENABLED));
  }

  /**
   * If {@code true}, then alerts processed by the {@link AlertReceivedListener}
   * will not write alert data to the database on every event. Instead, data
   * like timestamps and text will be kept in a cache and flushed out
   * periodically to the database.
   * <p/>
   * The default value is {@code false}.
   *
   * @return {@code true} if the cache is enabled, {@code false} otherwise.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public boolean isAlertCacheEnabled() {
    return Boolean.parseBoolean(getProperty(ALERTS_CACHE_ENABLED));
  }

  /**
   * Gets the interval at which cached alert data is written out to the
   * database, if enabled.
   *
   * @return the cache flush interval, or
   *         {@value #ALERTS_CACHE_FLUSH_INTERVAL_DEFAULT} if not set.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public int getAlertCacheFlushInterval() {
    return Integer.parseInt(getProperty(ALERTS_CACHE_FLUSH_INTERVAL));
  }

  /**
   * Gets the size of the alerts cache, if enabled.
   *
   * @return the cache flush interval, or {@value #ALERTS_CACHE_SIZE_DEFAULT} if
   *         not set.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public int getAlertCacheSize() {
    return Integer.parseInt(getProperty(ALERTS_CACHE_SIZE));
  }

  /**
   * Get the ambari display URL
   * @return
   */
  public String getAmbariDisplayUrl() {
    return getProperty(AMBARI_DISPLAY_URL);
  }


  /**
   * @return number of retry attempts for api and blueprint operations
   */
  public int getOperationsRetryAttempts() {
    final int RETRY_ATTEMPTS_LIMIT = 10;
    String property = getProperty(OPERATIONS_RETRY_ATTEMPTS);
    Integer attempts = Integer.valueOf(property);
    if (attempts < 0) {
      LOG.warn("Invalid operations retry attempts number ({}), should be [0,{}]. Value reset to default {}",
          attempts, RETRY_ATTEMPTS_LIMIT, OPERATIONS_RETRY_ATTEMPTS.getDefaultValue());
      attempts = OPERATIONS_RETRY_ATTEMPTS.getDefaultValue();
    } else if (attempts > RETRY_ATTEMPTS_LIMIT) {
      LOG.warn("Invalid operations retry attempts number ({}), should be [0,{}]. Value set to {}",
        attempts, RETRY_ATTEMPTS_LIMIT, RETRY_ATTEMPTS_LIMIT);
      attempts = RETRY_ATTEMPTS_LIMIT;
    }
    if (attempts > 0) {
      LOG.info("Operations retry enabled. Number of retry attempts: {}", attempts);
    }
    return attempts;
  }

  /**
   * @return the connect timeout used when loading a version definition URL.
   */
  public int getVersionDefinitionConnectTimeout() {
    return NumberUtils.toInt(getProperty(VERSION_DEFINITION_CONNECT_TIMEOUT));
  }
  /**
   * @return the read timeout used when loading a version definition URL
   */
  public int getVersionDefinitionReadTimeout() {
    return NumberUtils.toInt(getProperty(VERSION_DEFINITION_READ_TIMEOUT));
  }

  public String getAgentStackRetryOnInstallCount(){
    return getProperty(AGENT_STACK_RETRY_COUNT);
  }

  public String isAgentStackRetryOnInstallEnabled(){
    return getProperty(AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY);
  }

  public boolean isAuditLogEnabled() {
    return Boolean.parseBoolean(getProperty(AUDIT_LOG_ENABLED));
  }

  /**
   * @return the capacity of async audit logger
   */
  public int getAuditLoggerCapacity() {
    return NumberUtils.toInt(getProperty(AUDIT_LOGGER_CAPACITY));
  }

  /**
   * Customized UDP port for SNMP dispatcher
   * @return Integer if property exists else null
   */
  public Integer getSNMPUdpBindPort() {
    String udpPort = getProperty(ALERTS_SNMP_DISPATCH_UDP_PORT);
    return StringUtils.isEmpty(udpPort) ? null : Integer.parseInt(udpPort);
  }

  public boolean isLdapAlternateUserSearchEnabled() {
    return Boolean.parseBoolean(getProperty(LDAP_ALT_USER_SEARCH_ENABLED));
  }

  /**
   * Gets the hosts/ports that proxy calls are allowed to be made to.
   *
   * @return
   */
  public String getProxyHostAndPorts() {
    return getProperty(PROXY_ALLOWED_HOST_PORTS);
  }

  /**
   * Gets the number of minutes that data cached by the
   * {@link MetricsRetrievalService} is kept. The longer this value is, the
   * older the data will be when a user first logs in. After that first login,
   * data will be updated by the {@link MetricsRetrievalService} as long as
   * incoming REST requests are made.
   * <p/>
   * It is recommended that this value be longer rather than shorter since the
   * performance benefit of the cache greatly outweighs the data loaded after
   * first login.
   *
   * @return the number of minutes, defaulting to 30 if not specified.
   */
  public int getMetricsServiceCacheTimeout() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_CACHE_TIMEOUT));
  }

  /**
   * Gets the priority of the {@link Thread}s used by the
   * {@link MetricsRetrievalService}. This will be a value within the range of
   * {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}.
   *
   * @return the thread proprity.
   */
  public int getMetricsServiceThreadPriority() {
    int priority = Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREAD_PRIORITY));
    if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
      priority = Thread.NORM_PRIORITY;
    }

    return priority;
  }

  /**
   * Gets the core pool size used for the {@link MetricsRetrievalService}.
   *
   * @return the core pool size or
   *         {@value #PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT} if not
   *         specified.
   */
  public int getMetricsServiceThreadPoolCoreSize() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREADPOOL_CORE_SIZE));
  }

  /**
   * Gets the max pool size used for the {@link MetricsRetrievalService}.
   * Threads will only be increased up to this value of the worker queue is
   * exhauseted and rejects the new task.
   *
   * @return the max pool size, or
   *         {@value PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT} if not
   *         specified.
   * @see #getMetricsServiceWorkerQueueSize()
   */
  public int getMetricsServiceThreadPoolMaxSize() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREADPOOL_MAX_SIZE));
  }

  /**
   * Gets the queue size of the worker queue for the
   * {@link MetricsRetrievalService}.
   *
   * @return the worker queue size, or {@code 10 *}
   *         {@link #getMetricsServiceThreadPoolMaxSize()} if not specified.
   */
  public int getMetricsServiceWorkerQueueSize() {
    return Integer.parseInt(getProperty(METRIC_RETRIEVAL_SERVICE_THREADPOOL_WORKER_QUEUE_SIZE));
  }

  /**
   * Returns the number of tasks that can be queried from the database at once
   * In the case of more tasks, multiple queries are issued
   * @return
   */
  public int getTaskIdListLimit() {
    return Integer.parseInt(getProperty(TASK_ID_LIST_LIMIT));
  }

  /**
   * Generates a markdown table which includes:
   * <ul>
   * <li>Property key name</li>
   * <li>Default value</li>
   * <li>Description</li>
   * <ul>
   *
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    Set<ConfigurationProperty<?>> orderedProperties = new TreeSet<>();

    StringBuilder buffer = new StringBuilder("| Property Name | Default | Description |");
    buffer.append(System.lineSeparator());
    buffer.append("| --- | --- | --- |");
    buffer.append(System.lineSeparator());

    Field[] fields = Configuration.class.getFields();
    for (Field field : fields) {
      if (field.getType() != ConfigurationProperty.class) {
        continue;
      }

      ConfigurationProperty<?> configurationProperty = (ConfigurationProperty<?>) field.get(null);
      orderedProperties.add(configurationProperty);
    }

    for (ConfigurationProperty<?> configurationProperty : orderedProperties){
      Markdown markdown = configurationProperty.getClass().getAnnotation(Markdown.class);

      buffer.append("| ");
      buffer.append(configurationProperty.getKey());
      buffer.append(" | ");
      buffer.append(configurationProperty.getDefaultValue());
      buffer.append(" | ");

      String description = StringUtils.EMPTY;
      if (markdown != null && StringUtils.isNotBlank(markdown.description())) {
        description = markdown.description();
      }

      buffer.append(description);
      buffer.append(" |");
      buffer.append(System.lineSeparator());
    }

    System.out.println(buffer.toString());
  }

  /**
   * The {@link ConfigurationProperty} class is used to wrap an Ambari property
   * key, type, and default value.
   *
   * @param <T>
   */
  public static class ConfigurationProperty<T> implements Comparable<ConfigurationProperty<?>> {

    private final String m_key;
    private final T m_defaultValue;

    /**
     * Constructor.
     *
     * @param key
     *          the property key name (not {@code null}).
     * @param defaultValue
     *          the default value or {@code null} for none.
     */
    private ConfigurationProperty(String key, T defaultValue) {
      m_key = key;
      m_defaultValue = defaultValue;

    }

    /**
     * Gets the key.
     *
     * @return the key (never {@code null}).
     */
    public String getKey(){
      return m_key;
    }

    /**
     * Gets the default value for this key if its undefined.
     *
     * @return
     */
    public T getDefaultValue() {
      return m_defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return m_key.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ConfigurationProperty<?> other = (ConfigurationProperty<?>) obj;
      return StringUtils.equals(this.m_key, other.m_key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return m_key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ConfigurationProperty<?> o) {
      return this.m_key.compareTo(o.m_key);
    }
  }
}
