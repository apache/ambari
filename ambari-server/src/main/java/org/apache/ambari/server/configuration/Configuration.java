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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.orm.JPATableGenerationStrategy;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.Parallel;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


/**
 * Ambari configuration.
 * Reads properties from ambari.properties
 */
@Singleton
public class Configuration {

  @Inject
  private OsFamily osFamily;

  public static final String CONFIG_FILE = "ambari.properties";
  public static final String BOOTSTRAP_DIR = "bootstrap.dir";

  /**
   *  PREFIX_DIR is shared in ambari-agent.ini and should only be called by unit tests.
   *  For all server-side processing, it should be retrieved from <code>HostImpl.getPrefix()</code>
   */
  public static final String PREFIX_DIR = "/var/lib/ambari-agent/data";

  public static final String BOOTSTRAP_DIR_DEFAULT = "/var/run/ambari-server/bootstrap";
  public static final String VIEWS_DIR = "views.dir";
  public static final String VIEWS_DIR_DEFAULT = "/var/lib/ambari-server/resources/views";
  public static final String VIEWS_VALIDATE = "views.validate";
  public static final String VIEWS_VALIDATE_DEFAULT = "false";
  public static final String VIEWS_REMOVE_UNDEPLOYED = "views.remove.undeployed";
  public static final String VIEWS_REMOVE_UNDEPLOYED_DEFAULT = "false";
  public static final String WEBAPP_DIR = "webapp.dir";
  public static final String BOOTSTRAP_SCRIPT = "bootstrap.script";
  public static final String BOOTSTRAP_SCRIPT_DEFAULT = "/usr/bin/ambari_bootstrap";
  public static final String BOOTSTRAP_SETUP_AGENT_SCRIPT = "bootstrap.setup_agent.script";
  public static final String BOOTSTRAP_SETUP_AGENT_PASSWORD = "bootstrap.setup_agent.password";
  public static final String BOOTSTRAP_MASTER_HOSTNAME = "bootstrap.master_host_name";
  public static final String RECOMMENDATIONS_DIR = "recommendations.dir";
  public static final String RECOMMENDATIONS_DIR_DEFAULT = "/var/run/ambari-server/stack-recommendations";
  public static final String STACK_ADVISOR_SCRIPT = "stackadvisor.script";
  public static final String STACK_ADVISOR_SCRIPT_DEFAULT = "/var/lib/ambari-server/resources/scripts/stack_advisor.py";
  public static final String AMBARI_PYTHON_WRAP_KEY = "ambari.python.wrap";
  public static final String AMBARI_PYTHON_WRAP_DEFAULT = "ambari-python-wrap";
  public static final String API_AUTHENTICATE = "api.authenticate";
  public static final String API_USE_SSL = "api.ssl";
  public static final String API_CSRF_PREVENTION_KEY = "api.csrfPrevention.enabled";
  public static final String API_GZIP_COMPRESSION_ENABLED_KEY = "api.gzip.compression.enabled";
  public static final String API_GZIP_MIN_COMPRESSION_SIZE_KEY = "api.gzip.compression.min.size";
  public static final String AGENT_API_GZIP_COMPRESSION_ENABLED_KEY = "agent.api.gzip.compression.enabled";
  public static final String SRVR_TWO_WAY_SSL_KEY = "security.server.two_way_ssl";
  public static final String SRVR_TWO_WAY_SSL_PORT_KEY = "security.server.two_way_ssl.port";
  public static final String SRVR_ONE_WAY_SSL_PORT_KEY = "security.server.one_way_ssl.port";
  public static final String SRVR_KSTR_DIR_KEY = "security.server.keys_dir";
  public static final String SRVR_CRT_NAME_KEY = "security.server.cert_name";
  public static final String SRVR_CSR_NAME_KEY = "security.server.csr_name";
  public static final String SRVR_KEY_NAME_KEY = "security.server.key_name";
  public static final String KSTR_NAME_KEY = "security.server.keystore_name";
  public static final String KSTR_TYPE_KEY = "security.server.keystore_type";
  public static final String TSTR_NAME_KEY = "security.server.truststore_name";
  public static final String TSTR_TYPE_KEY = "security.server.truststore_type";
  public static final String SRVR_CRT_PASS_FILE_KEY = "security.server.crt_pass_file";
  public static final String SRVR_CRT_PASS_KEY = "security.server.crt_pass";
  public static final String SRVR_CRT_PASS_LEN_KEY = "security.server.crt_pass.len";
  public static final String PASSPHRASE_ENV_KEY = "security.server.passphrase_env_var";
  public static final String PASSPHRASE_KEY = "security.server.passphrase";
  public static final String SRVR_DISABLED_CIPHERS = "security.server.disabled.ciphers";
  public static final String SRVR_DISABLED_PROTOCOLS = "security.server.disabled.protocols";
  public static final String RESOURCES_DIR_KEY = "resources.dir";
  public static final String METADATA_DIR_PATH = "metadata.path";
  public static final String COMMON_SERVICES_DIR_PATH = "common.services.path";
  public static final String SERVER_VERSION_FILE = "server.version.file";
  public static final String SERVER_VERSION_KEY = "version";
  public static final String JAVA_HOME_KEY = "java.home";
  public static final String JDK_NAME_KEY = "jdk.name";
  public static final String JCE_NAME_KEY = "jce.name";
  public static final float  JDK_MIN_VERSION = 1.7f;
  public static final String CLIENT_SECURITY_KEY = "client.security";
  public static final String CLIENT_API_PORT_KEY = "client.api.port";
  public static final String CLIENT_API_SSL_PORT_KEY = "client.api.ssl.port";
  public static final String CLIENT_API_SSL_KSTR_DIR_NAME_KEY = "client.api.ssl.keys_dir";
  public static final String CLIENT_API_SSL_KSTR_NAME_KEY = "client.api.ssl.keystore_name";
  public static final String CLIENT_API_SSL_KSTR_TYPE_KEY = "client.api.ssl.keystore_type";
  public static final String CLIENT_API_SSL_TSTR_NAME_KEY = "client.api.ssl.truststore_name";
  public static final String CLIENT_API_SSL_TSTR_TYPE_KEY = "client.api.ssl.truststore_type";
  public static final String CLIENT_API_SSL_CRT_NAME_KEY = "client.api.ssl.cert_name";
  public static final String CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY = "client.api.ssl.cert_pass_file";
  public static final String CLIENT_API_SSL_CRT_PASS_KEY = "client.api.ssl.crt_pass";
  public static final String CLIENT_API_SSL_KEY_NAME_KEY = "client.api.ssl.key_name";
  public static final String ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY = "agent.auto.cache.update";
  public static final String ENABLE_AUTO_AGENT_CACHE_UPDATE_DEFAULT = "true";
  public static final String CHECK_REMOTE_MOUNTS_KEY = "agent.check.remote.mounts";
  public static final String CHECK_REMOTE_MOUNTS_DEFAULT = "false";
  public static final String CHECK_MOUNTS_TIMEOUT_KEY = "agent.check.mounts.timeout";
  public static final String CHECK_MOUNTS_TIMEOUT_DEFAULT = "0";
  public static final String SERVER_DB_NAME_KEY = "server.jdbc.database_name";
  public static final String SERVER_DB_NAME_DEFAULT = "ambari";
  public static final String REQUEST_READ_TIMEOUT = "views.request.read.timeout.millis";
  public static final String REQUEST_READ_TIMEOUT_DEFAULT= "10000";
  public static final String REQUEST_CONNECT_TIMEOUT = "views.request.connect.timeout.millis";
  public static final String REQUEST_CONNECT_TIMEOUT_DEFAULT = "5000";
  public static final String AMBARI_REQUEST_READ_TIMEOUT = "views.ambari.request.read.timeout.millis";
  public static final String AMBARI_REQUEST_READ_TIMEOUT_DEFAULT= "10000";
  public static final String AMBARI_REQUEST_CONNECT_TIMEOUT = "views.ambari.request.connect.timeout.millis";
  public static final String AMBARI_REQUEST_CONNECT_TIMEOUT_DEFAULT = "5000";
  public static final String SERVER_JDBC_POSTGRES_SCHEMA_NAME = "server.jdbc.postgres.schema";
  public static final String OJDBC_JAR_NAME_KEY = "db.oracle.jdbc.name";
  public static final String OJDBC_JAR_NAME_DEFAULT = "ojdbc6.jar";
  public static final String MYSQL_JAR_NAME_KEY = "db.mysql.jdbc.name";
  public static final String MYSQL_JAR_NAME_DEFAULT = "mysql-connector-java.jar";
  public static final String IS_LDAP_CONFIGURED = "ambari.ldap.isConfigured";
  public static final String LDAP_USE_SSL_KEY = "authentication.ldap.useSSL";
  public static final String LDAP_PRIMARY_URL_KEY = "authentication.ldap.primaryUrl";
  public static final String LDAP_SECONDARY_URL_KEY = "authentication.ldap.secondaryUrl";
  public static final String LDAP_BASE_DN_KEY = "authentication.ldap.baseDn";
  public static final String LDAP_BIND_ANONYMOUSLY_KEY = "authentication.ldap.bindAnonymously";
  public static final String LDAP_MANAGER_DN_KEY = "authentication.ldap.managerDn";
  public static final String LDAP_MANAGER_PASSWORD_KEY = "authentication.ldap.managerPassword";
  public static final String LDAP_DN_ATTRIBUTE_KEY = "authentication.ldap.dnAttribute";
  public static final String LDAP_USERNAME_ATTRIBUTE_KEY = "authentication.ldap.usernameAttribute";
  public static final String LDAP_USER_BASE_KEY = "authentication.ldap.userBase";
  public static final String LDAP_USER_OBJECT_CLASS_KEY = "authentication.ldap.userObjectClass";
  public static final String LDAP_GROUP_BASE_KEY = "authentication.ldap.groupBase";
  public static final String LDAP_GROUP_OBJECT_CLASS_KEY = "authentication.ldap.groupObjectClass";
  public static final String LDAP_GROUP_NAMING_ATTR_KEY = "authentication.ldap.groupNamingAttr";
  public static final String LDAP_GROUP_MEMEBERSHIP_ATTR_KEY = "authentication.ldap.groupMembershipAttr";
  public static final String LDAP_ADMIN_GROUP_MAPPING_RULES_KEY = "authorization.ldap.adminGroupMappingRules";
  public static final String LDAP_GROUP_SEARCH_FILTER_KEY = "authorization.ldap.groupSearchFilter";
  public static final String LDAP_REFERRAL_KEY = "authentication.ldap.referral";
  public static final String LDAP_PAGINATION_ENABLED_KEY = "authentication.ldap.pagination.enabled";
  public static final String SERVER_EC_CACHE_SIZE = "server.ecCacheSize";
  public static final String SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED = "server.hrcStatusSummary.cache.enabled";
  public static final String SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE = "server.hrcStatusSummary.cache.size";
  public static final String SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION = "server.hrcStatusSummary.cache.expiryDuration";
  public static final String SERVER_STALE_CONFIG_CACHE_ENABLED_KEY = "server.cache.isStale.enabled";
  public static final String SERVER_STALE_CONFIG_CACHE_EXPIRATION_KEY = "server.cache.isStale.expiration";
  public static final String SERVER_PERSISTENCE_TYPE_KEY = "server.persistence.type";
  public static final String SERVER_JDBC_USER_NAME_KEY = "server.jdbc.user.name";
  public static final String SERVER_JDBC_USER_PASSWD_KEY = "server.jdbc.user.passwd";
  public static final String SERVER_JDBC_DRIVER_KEY = "server.jdbc.driver";
  public static final String SERVER_JDBC_URL_KEY = "server.jdbc.url";
  public static final String SERVER_JDBC_PROPERTIES_PREFIX = "server.jdbc.properties.";

  public static final String SERVER_HTTP_REQUEST_HEADER_SIZE = "server.http.request.header.size";
  public static final String SERVER_HTTP_RESPONSE_HEADER_SIZE = "server.http.response.header.size";
  public static final int SERVER_HTTP_REQUEST_HEADER_SIZE_DEFAULT = 64*1024;
  public static final int SERVER_HTTP_RESPONSE_HEADER_SIZE_DEFAULT = 64*1024;

  // Properties for stack upgrade (Rolling, Express)
  public static final String ROLLING_UPGRADE_MIN_STACK_KEY = "rolling.upgrade.min.stack";
  public static final String ROLLING_UPGRADE_MAX_STACK_KEY = "rolling.upgrade.max.stack";
  public static final String ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES_KEY = "rolling.upgrade.skip.packages.prefixes";
  public static final String ROLLING_UPGRADE_MIN_STACK_DEFAULT = "HDP-2.2";
  public static final String ROLLING_UPGRADE_MAX_STACK_DEFAULT = "";
  public static final String ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES_DEFAULT = "";
  public static final String STACK_UPGRADE_BYPASS_PRECHECKS_KEY = "stack.upgrade.bypass.prechecks";
  public static final String STACK_UPGRADE_BYPASS_PRECHECKS_DEFAULT = "false";

  public static final String SERVER_JDBC_CONNECTION_POOL = "server.jdbc.connection-pool";
  public static final String SERVER_JDBC_CONNECTION_POOL_MIN_SIZE = "server.jdbc.connection-pool.min-size";
  public static final String SERVER_JDBC_CONNECTION_POOL_MAX_SIZE = "server.jdbc.connection-pool.max-size";
  public static final String SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE = "server.jdbc.connection-pool.acquisition-size";
  public static final String SERVER_JDBC_CONNECTION_POOL_MAX_AGE = "server.jdbc.connection-pool.max-age";
  public static final String SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME = "server.jdbc.connection-pool.max-idle-time";
  public static final String SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS = "server.jdbc.connection-pool.max-idle-time-excess";
  public static final String SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL = "server.jdbc.connection-pool.idle-test-interval";
  public static final String SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS = "server.jdbc.connection-pool.acquisition-retry-attempts";
  public static final String SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY = "server.jdbc.connection-pool.acquisition-retry-delay";

  public static final String OPERATIONS_RETRY_ATTEMPTS_KEY = "server.operations.retry-attempts";
  public static final String OPERATIONS_RETRY_ATTEMPTS_DEFAULT = "0";
  public static final int RETRY_ATTEMPTS_LIMIT = 10;

  public static final String SERVER_JDBC_RCA_USER_NAME_KEY = "server.jdbc.rca.user.name";
  public static final String SERVER_JDBC_RCA_USER_PASSWD_KEY = "server.jdbc.rca.user.passwd";
  public static final String SERVER_JDBC_RCA_DRIVER_KEY = "server.jdbc.rca.driver";
  public static final String SERVER_JDBC_RCA_URL_KEY = "server.jdbc.rca.url";
  public static final String SERVER_JDBC_GENERATE_TABLES_KEY = "server.jdbc.generateTables";
  public static final String JDBC_UNIT_NAME = "ambari-server";
  public static final String JDBC_LOCAL_URL = "jdbc:postgresql://localhost/";
  public static final String JDBC_LOCAL_DRIVER = "org.postgresql.Driver";
  public static final String DEFAULT_DERBY_SCHEMA = "ambari";
  public static final String JDBC_IN_MEMORY_URL = String.format("jdbc:derby:memory:myDB/%s;create=true", DEFAULT_DERBY_SCHEMA);
  public static final String JDBC_IN_MEMROY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String HOSTNAME_MACRO = "{hostname}";
  public static final String JDBC_RCA_LOCAL_URL = "jdbc:postgresql://" + HOSTNAME_MACRO + "/ambarirca";
  public static final String JDBC_RCA_LOCAL_DRIVER = "org.postgresql.Driver";
  public static final String OS_FAMILY_KEY = "server.os_family";
  public static final String OS_VERSION_KEY = "server.os_type";
  public static final String SRVR_HOSTS_MAPPING = "server.hosts.mapping";
  // Command parameter names
  public static final String UPGRADE_FROM_STACK = "source_stack_version";
  public static final String UPGRADE_TO_STACK = "target_stack_version";
  public static final String SSL_TRUSTSTORE_PATH_KEY = "ssl.trustStore.path";
  public static final String SSL_TRUSTSTORE_PASSWORD_KEY = "ssl.trustStore.password";
  public static final String SSL_TRUSTSTORE_TYPE_KEY = "ssl.trustStore.type";
  public static final String JAVAX_SSL_TRUSTSTORE = "javax.net.ssl.trustStore";
  public static final String JAVAX_SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
  public static final String JAVAX_SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";
  public static final String SRVR_TWO_WAY_SSL_PORT_DEFAULT = "8441";
  public static final String SRVR_ONE_WAY_SSL_PORT_DEFAULT = "8440";
  public static final String SRVR_CRT_NAME_DEFAULT = "ca.crt";
  public static final String SRVR_KEY_NAME_DEFAULT = "ca.key";
  public static final String SRVR_CSR_NAME_DEFAULT = "ca.csr";
  public static final String KSTR_NAME_DEFAULT = "keystore.p12";
  public static final String KSTR_TYPE_DEFAULT = "PKCS12";
  // By default self-signed certificates are used and we can use keystore as truststore in PKCS12 format
  // When CA signed certificates are used truststore should be created in JKS format (truststore.jks)
  public static final String TSTR_NAME_DEFAULT = "keystore.p12";
  public static final String TSTR_TYPE_DEFAULT = "PKCS12";
  public static final String CLIENT_API_SSL_KSTR_NAME_DEFAULT = "https.keystore.p12";
  public static final String CLIENT_API_SSL_KSTR_TYPE_DEFAULT = "PKCS12";
  // By default self-signed certificates are used and we can use keystore as truststore in PKCS12 format
  // When CA signed certificates are used truststore should be created in JKS format (truststore.jks)
  public static final String CLIENT_API_SSL_TSTR_NAME_DEFAULT = "https.keystore.p12";
  public static final String CLIENT_API_SSL_TSTR_TYPE_DEFAULT = "PKCS12";
  public static final String CLIENT_API_SSL_CRT_PASS_FILE_NAME_DEFAULT = "https.pass.txt";
  public static final String CLIENT_API_SSL_KEY_NAME_DEFAULT = "https.key";
  public static final String CLIENT_API_SSL_CRT_NAME_DEFAULT = "https.crt";
  public static final String GLOBAL_CONFIG_TAG = "global";
  public static final String MAPREDUCE2_LOG4J_CONFIG_TAG = "mapreduce2-log4j";
  public static final String RCA_ENABLED_PROPERTY = "rca_enabled";
  public static final String HIVE_CONFIG_TAG = "hive-site";
  public static final String HIVE_METASTORE_PASSWORD_PROPERTY = "javax.jdo.option.ConnectionPassword";
  public static final String MASTER_KEY_PERSISTED = "security.master.key.ispersisted";
  public static final String MASTER_KEY_LOCATION = "security.master.key.location";
  public static final String MASTER_KEYSTORE_LOCATION = "security.master.keystore.location";
  public static final String MASTER_KEY_ENV_PROP = "AMBARI_SECURITY_MASTER_KEY";
  public static final String MASTER_KEY_FILENAME_DEFAULT = "master";
  public static final String MASTER_KEYSTORE_FILENAME_DEFAULT = "credentials.jceks";
  public static final String TEMPORARY_KEYSTORE_RETENTION_MINUTES = "security.temporary.keystore.retention.minutes";
  public static final long TEMPORARY_KEYSTORE_RETENTION_MINUTES_DEFAULT = 90;
  public static final String TEMPORARY_KEYSTORE_ACTIVELY_PURGE = "security.temporary.keystore.actibely.purge";
  public static final boolean TEMPORARY_KEYSTORE_ACTIVELY_PURGE_DEFAULT = true;

  // Alerts notifications properties
  public static final String AMBARI_DISPLAY_URL = "ambari.display.url";

  /**
   * Key for repo validation suffixes.
   */
  public static final String REPO_SUFFIX_KEY_UBUNTU = "repo.validation.suffixes.ubuntu";
  public static final String REPO_SUFFIX_KEY_DEFAULT = "repo.validation.suffixes.default";

  public static final String EXECUTION_SCHEDULER_CLUSTERED_KEY = "server.execution.scheduler.isClustered";
  public static final String EXECUTION_SCHEDULER_THREADS_KEY = "server.execution.scheduler.maxThreads";
  public static final String EXECUTION_SCHEDULER_CONNECTIONS_KEY = "server.execution.scheduler.maxDbConnections";
  public static final String EXECUTION_SCHEDULER_MISFIRE_TOLERATION_KEY = "server.execution.scheduler.misfire.toleration.minutes";
  public static final String EXECUTION_SCHEDULER_START_DELAY_KEY = "server.execution.scheduler.start.delay.seconds";
  public static final String EXECUTION_SCHEDULER_WAIT_KEY = "server.execution.scheduler.wait";
  public static final String DEFAULT_SCHEDULER_THREAD_COUNT = "5";
  public static final String DEFAULT_SCHEDULER_MAX_CONNECTIONS = "5";
  public static final String DEFAULT_EXECUTION_SCHEDULER_MISFIRE_TOLERATION = "480";
  public static final String DEFAULT_SCHEDULER_START_DELAY_SECONDS = "120";
  public static final String DEFAULT_EXECUTION_SCHEDULER_WAIT_SECONDS = "1";
  public static final String SERVER_TMP_DIR_KEY = "server.tmp.dir";
  public static final String SERVER_TMP_DIR_DEFAULT = "/var/lib/ambari-server/tmp";
  public static final String EXTERNAL_SCRIPT_TIMEOUT_KEY = "server.script.timeout";
  public static final String EXTERNAL_SCRIPT_TIMEOUT_DEFAULT = "5000";
  public static final String DEF_ARCHIVE_EXTENSION;
  public static final String DEF_ARCHIVE_CONTENT_TYPE;

  /**
   * Kerberos related configuration options
   */
  public static final String KDC_PORT_KEY = "default.kdcserver.port";
  public static final String KDC_PORT_KEY_DEFAULT = "88";
  public static final String KDC_CONNECTION_CHECK_TIMEOUT_KEY = "kdcserver.connection.check.timeout";
  public static final String KDC_CONNECTION_CHECK_TIMEOUT_DEFAULT = "10000";
  public static final String KERBEROS_KEYTAB_CACHE_DIR_KEY = "kerberos.keytab.cache.dir";
  public static final String KERBEROS_KEYTAB_CACHE_DIR_DEFAULT = "/var/lib/ambari-server/data/cache";
  public static final String KERBEROS_CHECK_JAAS_CONFIGURATION_KEY = "kerberos.check.jaas.configuration";
  public static final String KERBEROS_CHECK_JAAS_CONFIGURATION_DEFAULT = "false";

  /**
   * Recovery related configuration
   */
  public static final String RECOVERY_TYPE_KEY = "recovery.type";
  public static final String RECOVERY_TYPE_DEFAULT = "DEFAULT";
  public static final String RECOVERY_LIFETIME_MAX_COUNT_KEY = "recovery.lifetime_max_count";
  public static final String RECOVERY_LIFETIME_MAX_COUNT_DEFAULT = "12";
  public static final String RECOVERY_MAX_COUNT_KEY = "recovery.max_count";
  public static final String RECOVERY_MAX_COUNT_DEFAULT = "6";
  public static final String RECOVERY_WINDOW_IN_MIN_KEY = "recovery.window_in_minutes";
  public static final String RECOVERY_WINDOW_IN_MIN_DEFAULT = "60";
  public static final String RECOVERY_RETRY_GAP_KEY = "recovery.retry_interval";
  public static final String RECOVERY_RETRY_GAP_DEFAULT = "5";
  public static final String RECOVERY_DISABLED_COMPONENTS_KEY = "recovery.disabled_components";
  public static final String RECOVERY_ENABLED_COMPONENTS_KEY = "recovery.enabled_components";

  /**
   * Allow proxy calls to these hosts and ports only
   */
  public static final String PROXY_ALLOWED_HOST_PORTS = "proxy.allowed.hostports";
  public static final String PROXY_ALLOWED_HOST_PORTS_DEFAULT = "*:*";

  /**
   * This key defines whether stages of parallel requests are executed in
   * parallel or sequentally. Only stages from different requests
   * running on not interfering host sets may be executed in parallel.
   */
  public static final String PARALLEL_STAGE_EXECUTION_KEY = "server.stages.parallel";
  public static final String AGENT_TASK_TIMEOUT_KEY = "agent.task.timeout";
  public static final String AGENT_PACKAGE_INSTALL_TASK_TIMEOUT_KEY = "agent.package.install.task.timeout";

  /**
   * Max number of tasks that may be executed within a single stage.
   * This limitation is used for tasks that when executed in a 1000+ node cluster,
   * may DDOS servers providing downloadable resources
   */
  public static final String AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_KEY = "agent.package.parallel.commands.limit";
  public static final String AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_DEFAULT = "100";

  public static final String AGENT_TASK_TIMEOUT_DEFAULT = "900";
  public static final String AGENT_PACKAGE_INSTALL_TASK_TIMEOUT_DEFAULT = "1800";

  /**
   * Server side task (default) timeout value
   */
  public static final String SERVER_TASK_TIMEOUT_KEY = "server.task.timeout";
  public static final String SERVER_TASK_TIMEOUT_DEFAULT = "1200";

  public static final String CUSTOM_ACTION_DEFINITION_KEY = "custom.action.definitions";
  public static final String SHARED_RESOURCES_DIR_KEY = "shared.resources.dir";

  protected static final boolean SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT = true;
  protected static final long SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT = 10000L;
  protected static final long SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT = 30; //minutes

  private static final String CUSTOM_ACTION_DEFINITION_DEF_VALUE = "/var/lib/ambari-server/resources/custom_action_definitions";

  private static final long SERVER_EC_CACHE_SIZE_DEFAULT = 10000L;
  private static final String SERVER_STALE_CONFIG_CACHE_ENABLED_DEFAULT = "true";
  private static final String SERVER_STALE_CONFIG_CACHE_EXPIRATION_DEFAULT = "60";
  private static final String SERVER_JDBC_USER_NAME_DEFAULT = "ambari";
  private static final String SERVER_JDBC_USER_PASSWD_DEFAULT = "bigdata";
  private static final String SERVER_JDBC_RCA_USER_NAME_DEFAULT = "mapred";
  private static final String SERVER_JDBC_RCA_USER_PASSWD_DEFAULT = "mapred";
  private static final String SRVR_TWO_WAY_SSL_DEFAULT = "false";
  private static final String SRVR_KSTR_DIR_DEFAULT = ".";
  private static final String API_CSRF_PREVENTION_DEFAULT = "true";
  private static final String API_GZIP_COMPRESSION_ENABLED_DEFAULT = "true";
  private static final String API_GZIP_MIN_COMPRESSION_SIZE_DEFAULT = "10240";
  private static final String SRVR_CRT_PASS_FILE_DEFAULT = "pass.txt";
  private static final String SRVR_CRT_PASS_LEN_DEFAULT = "50";
  private static final String SRVR_DISABLED_CIPHERS_DEFAULT = "";
  private static final String SRVR_DISABLED_PROTOCOLS_DEFAULT = "";
  private static final String PASSPHRASE_ENV_DEFAULT = "AMBARI_PASSPHRASE";
  private static final String RESOURCES_DIR_DEFAULT = "/var/lib/ambari-server/resources/";
  private static final String SHARED_RESOURCES_DIR_DEFAULT = "/usr/lib/ambari-server/lib/ambari_commons/resources";
  private static final String ANONYMOUS_AUDIT_NAME_KEY = "anonymous.audit.name";

  private static final int CLIENT_API_PORT_DEFAULT = 8080;
  private static final int CLIENT_API_SSL_PORT_DEFAULT = 8443;
  private static final String LDAP_BIND_ANONYMOUSLY_DEFAULT = "true";
  private static final String LDAP_PAGINATION_ENABLED_DEFAULT = "true";

  /**
   * Indicator for sys prepped host
   * It is possible the some nodes are sys prepped and some are not. This can be enabled later
   * by agent over-writing global indicator from ambari-server
   */
  public static final String SYS_PREPPED_HOSTS_KEY = "packages.pre.installed";
  public static final String SYS_PREPPED_HOSTS_DEFAULT = "false";

  /**
   * !!! TODO: For embedded server only - should be removed later
   */
  private static final String LDAP_PRIMARY_URL_DEFAULT = "localhost:33389";
  private static final String LDAP_BASE_DN_DEFAULT = "dc=ambari,dc=apache,dc=org";
  private static final String LDAP_USERNAME_ATTRIBUTE_DEFAULT = "uid";
  private static final String LDAP_DN_ATTRIBUTE_DEFAULT = "dn";
  private static final String LDAP_USER_BASE_DEFAULT = "ou=people,dc=ambari,dc=apache,dc=org";
  private static final String LDAP_USER_OBJECT_CLASS_DEFAULT = "person";
  private static final String LDAP_GROUP_BASE_DEFAULT = "ou=groups,dc=ambari,dc=apache,dc=org";
  private static final String LDAP_GROUP_OBJECT_CLASS_DEFAULT = "group";
  private static final String LDAP_GROUP_NAMING_ATTR_DEFAULT = "cn";
  private static final String LDAP_GROUP_MEMBERSHIP_ATTR_DEFAULT = "member";
  private static final String LDAP_ADMIN_GROUP_MAPPING_RULES_DEFAULT = "Ambari Administrators";
  private static final String LDAP_GROUP_SEARCH_FILTER_DEFAULT = "";
  private static final String LDAP_REFERRAL_DEFAULT = "follow";

  /**
   * !!! TODO: for development purposes only, should be changed to 'false'
   */
  private static final String IS_LDAP_CONFIGURED_DEFAULT = "false";

  private static final String SERVER_PERSISTENCE_TYPE_DEFAULT = "local";
  private static final String SERVER_CONNECTION_MAX_IDLE_TIME = "server.connection.max.idle.millis";

  /**
   * Default for repo validation suffixes.
   */
  private static final String REPO_SUFFIX_DEFAULT = "/repodata/repomd.xml";
  private static final String REPO_SUFFIX_UBUNTU = "/dists/%s/Release";

  private static final String PARALLEL_STAGE_EXECUTION_DEFAULT = "true";

  private static final String CLIENT_THREADPOOL_SIZE_KEY = "client.threadpool.size.max";
  private static final int CLIENT_THREADPOOL_SIZE_DEFAULT = 25;
  private static final String AGENT_THREADPOOL_SIZE_KEY = "agent.threadpool.size.max";
  private static final int AGENT_THREADPOOL_SIZE_DEFAULT = 25;

  private static final String VIEW_EXTRACTION_THREADPOOL_MAX_SIZE_KEY = "view.extraction.threadpool.size.max";
  private static final int VIEW_EXTRACTION_THREADPOOL_MAX_SIZE_DEFAULT = 20;
  private static final String VIEW_EXTRACTION_THREADPOOL_CORE_SIZE_KEY = "view.extraction.threadpool.size.core";
  private static final int VIEW_EXTRACTION_THREADPOOL_CORE_SIZE_DEFAULT = 10;
  private static final String VIEW_EXTRACTION_THREADPOOL_TIMEOUT_KEY = "view.extraction.threadpool.timeout";
  private static final long VIEW_EXTRACTION_THREADPOOL_TIMEOUT_DEFAULT = 100000L;

  private static final String SERVER_HTTP_SESSION_INACTIVE_TIMEOUT = "server.http.session.inactive_timeout";

  // database pooling defaults
  private static final String DEFAULT_JDBC_POOL_MIN_CONNECTIONS = "5";
  private static final String DEFAULT_JDBC_POOL_MAX_CONNECTIONS = "32";
  private static final String DEFAULT_JDBC_POOL_ACQUISITION_SIZE = "5";
  private static final String DEFAULT_JDBC_POOL_MAX_IDLE_TIME_SECONDS = "14400";
  private static final String DEFAULT_JDBC_POOL_EXCESS_MAX_IDLE_TIME_SECONDS = "0";
  private static final String DEFAULT_JDBC_POOL_MAX_AGE_SECONDS = "0";
  private static final String DEFAULT_JDBC_POOL_IDLE_TEST_INTERVAL = "7200";
  private static final String DEFAULT_JDBC_POOL_ACQUISITION_RETRY_ATTEMPTS = "30";
  private static final String DEFAULT_JDBC_POOL_ACQUISITION_RETRY_DELAY = "1000";

  // Timeline Metrics Cache settings
  private static final String TIMELINE_METRICS_CACHE_DISABLE = "server.timeline.metrics.cache.disabled";
  private static final String TIMELINE_METRICS_CACHE_MAX_ENTRIES = "server.timeline.metrics.cache.max.entries";
  private static final String DEFAULT_TIMELINE_METRICS_CACHE_MAX_ENTRIES = "50";
  private static final String TIMELINE_METRICS_CACHE_TTL = "server.timeline.metrics.cache.entry.ttl.seconds";
  private static final String DEFAULT_TIMELINE_METRICS_CACHE_TTL = "3600";
  private static final String TIMELINE_METRICS_CACHE_IDLE_TIME = "server.timeline.metrics.cache.entry.idle.seconds";
  private static final String DEFAULT_TIMELINE_METRICS_CACHE_IDLE_TIME = "1800";
  private static final String TIMELINE_METRICS_REQUEST_READ_TIMEOUT = "server.timeline.metrics.cache.read.timeout.millis";
  private static final String DEFAULT_TIMELINE_METRICS_REQUEST_READ_TIMEOUT = "10000";
  private static final String TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT = "server.timeline.metrics.cache.interval.read.timeout.millis";
  private static final String DEFAULT_TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT = "10000";
  private static final String TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT = "server.timeline.metrics.cache.connect.timeout.millis";
  private static final String DEFAULT_TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT = "5000";
  private static final String TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL = "server.timeline.metrics.cache.catchup.interval";
  private static final String DEFAULT_TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL = "300000";
  private static final String TIMELINE_METRICS_CACHE_HEAP_PERCENT = "server.timeline.metrics.cache.heap.percent";
  private static final String DEFAULT_TIMELINE_METRICS_CACHE_HEAP_PERCENT = "15%";

  // Timeline Metrics SSL settings
  public static final String AMRABI_METRICS_HTTPS_ENABLED_KEY = "server.timeline.metrics.https.enabled";

  /**
   * Governs the use of {@link Parallel} to process {@link StageEntity}
   * instances into {@link Stage}.
   */
  protected static final String EXPERIMENTAL_CONCURRENCY_STAGE_PROCESSING_ENABLED = "experimental.concurrency.stage_processing.enabled";

  /**
   * The full path to the XML file that describes the different alert templates.
   */
  private static final String ALERT_TEMPLATE_FILE = "alerts.template.file";

  /**
   * The maximum number of threads which will handle published alert events.
   */
  public static final String ALERTS_EXECUTION_SCHEDULER_THREADS_KEY = "alerts.execution.scheduler.maxThreads";

  /**
   * The default core threads for handling published alert events
   */
  public static final String ALERTS_EXECUTION_SCHEDULER_THREADS_DEFAULT = "2";

  /**
   * If {@code true} then alert information is cached and not immediately
   * persisted in the database.
   */
  public static final String ALERTS_CACHE_ENABLED = "alerts.cache.enabled";

  /**
   * The time after which cached alert information is flushed to the database.
   */
  public static final String ALERTS_CACHE_FLUSH_INTERVAL = "alerts.cache.flush.interval";

  /**
   * The default time, in minutes, that cached alert information is flushed to
   * the database.
   */
  public static final String ALERTS_CACHE_FLUSH_INTERVAL_DEFAULT = "10";

  /**
   * The size of the alert cache.
   */
  public static final String ALERTS_CACHE_SIZE = "alerts.cache.size";

  /**
   * The default size of the alerts cache.
   */
  public static final String ALERTS_CACHE_SIZE_DEFAULT = "50000";

  /**
   * For HTTP Response header configuration for Ambari Server UI
   */
  public static final String HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY = "http.strict-transport-security";
  public static final String HTTP_STRICT_TRANSPORT_HEADER_VALUE_DEFAULT = "max-age=31536000";
  public static final String HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY = "http.x-frame-options";
  public static final String HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT = "DENY";
  public static final String HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY = "http.x-xss-protection";
  public static final String HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT = "1; mode=block";

  /**
   *   For HTTP Response header configuration for Ambari Views
   */
  public static final String VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY = "views.http.strict-transport-security";
  public static final String VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE_DEFAULT = "max-age=31536000";
  public static final String VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY = "views.http.x-frame-options";
  public static final String VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT = "SAMEORIGIN";
  public static final String VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY = "views.http.x-xss-protection";
  public static final String VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT = "1; mode=block";

  /**
   * For Agent Stack Install retry configuration
   */
  public static final String AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY_KEY = "agent.stack.retry.on_repo_unavailability";
  public static final String AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY_DEFAULT = "false";
  public static final String AGENT_STACK_RETRY_COUNT_KEY = "agent.stack.retry.tries";
  public static final String AGENT_STACK_RETRY_COUNT_DEFAULT = "5";

  private static final Logger LOG = LoggerFactory.getLogger(
      Configuration.class);

  private Properties properties;
  private JsonObject hostChangesJson;
  private Map<String, String> configsMap;
  private Map<String, String> agentConfigsMap;
  private CredentialProvider credentialProvider = null;
  private volatile boolean credentialProviderInitialized = false;
  private Map<String, String> customDbProperties = null;

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
    agentConfigsMap.put(CHECK_REMOTE_MOUNTS_KEY, properties.getProperty(
      CHECK_REMOTE_MOUNTS_KEY, CHECK_REMOTE_MOUNTS_DEFAULT));
    agentConfigsMap.put(CHECK_MOUNTS_TIMEOUT_KEY, properties.getProperty(
      CHECK_MOUNTS_TIMEOUT_KEY, CHECK_MOUNTS_TIMEOUT_DEFAULT));

    agentConfigsMap.put(ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY, properties.getProperty(
        ENABLE_AUTO_AGENT_CACHE_UPDATE_KEY, ENABLE_AUTO_AGENT_CACHE_UPDATE_DEFAULT));

    configsMap = new HashMap<String, String>();
    configsMap.putAll(agentConfigsMap);
    configsMap.put(AMBARI_PYTHON_WRAP_KEY, properties.getProperty(
        AMBARI_PYTHON_WRAP_KEY, AMBARI_PYTHON_WRAP_DEFAULT));
    configsMap.put(SRVR_TWO_WAY_SSL_KEY, properties.getProperty(
        SRVR_TWO_WAY_SSL_KEY, SRVR_TWO_WAY_SSL_DEFAULT));
    configsMap.put(SRVR_TWO_WAY_SSL_PORT_KEY, properties.getProperty(
        SRVR_TWO_WAY_SSL_PORT_KEY, SRVR_TWO_WAY_SSL_PORT_DEFAULT));
    configsMap.put(SRVR_ONE_WAY_SSL_PORT_KEY, properties.getProperty(
        SRVR_ONE_WAY_SSL_PORT_KEY, SRVR_ONE_WAY_SSL_PORT_DEFAULT));
    configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(
        SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
    configsMap.put(SRVR_CRT_NAME_KEY, properties.getProperty(
        SRVR_CRT_NAME_KEY, SRVR_CRT_NAME_DEFAULT));
    configsMap.put(SRVR_KEY_NAME_KEY, properties.getProperty(
      SRVR_KEY_NAME_KEY, SRVR_KEY_NAME_DEFAULT));
    configsMap.put(SRVR_CSR_NAME_KEY, properties.getProperty(
      SRVR_CSR_NAME_KEY, SRVR_CSR_NAME_DEFAULT));
    configsMap.put(KSTR_NAME_KEY, properties.getProperty(
        KSTR_NAME_KEY, KSTR_NAME_DEFAULT));
    configsMap.put(KSTR_TYPE_KEY, properties.getProperty(
        KSTR_TYPE_KEY, KSTR_TYPE_DEFAULT));
    configsMap.put(TSTR_NAME_KEY, properties.getProperty(
        TSTR_NAME_KEY, TSTR_NAME_DEFAULT));
    configsMap.put(TSTR_TYPE_KEY, properties.getProperty(
        TSTR_TYPE_KEY, TSTR_TYPE_DEFAULT));
    configsMap.put(SRVR_CRT_PASS_FILE_KEY, properties.getProperty(
        SRVR_CRT_PASS_FILE_KEY, SRVR_CRT_PASS_FILE_DEFAULT));
    configsMap.put(PASSPHRASE_ENV_KEY, properties.getProperty(
        PASSPHRASE_ENV_KEY, PASSPHRASE_ENV_DEFAULT));
    configsMap.put(PASSPHRASE_KEY, System.getenv(configsMap.get(
        PASSPHRASE_ENV_KEY)));
    configsMap.put(RESOURCES_DIR_KEY, properties.getProperty(
        RESOURCES_DIR_KEY, RESOURCES_DIR_DEFAULT));
    configsMap.put(SRVR_CRT_PASS_LEN_KEY, properties.getProperty(
        SRVR_CRT_PASS_LEN_KEY, SRVR_CRT_PASS_LEN_DEFAULT));
    configsMap.put(SRVR_DISABLED_CIPHERS, properties.getProperty(
        SRVR_DISABLED_CIPHERS, SRVR_DISABLED_CIPHERS_DEFAULT));
    configsMap.put(SRVR_DISABLED_PROTOCOLS, properties.getProperty(
        SRVR_DISABLED_PROTOCOLS, SRVR_DISABLED_PROTOCOLS_DEFAULT));

    configsMap.put(CLIENT_API_SSL_KSTR_DIR_NAME_KEY, properties.getProperty(
      CLIENT_API_SSL_KSTR_DIR_NAME_KEY, configsMap.get(SRVR_KSTR_DIR_KEY)));
    configsMap.put(CLIENT_API_SSL_KSTR_NAME_KEY, properties.getProperty(
      CLIENT_API_SSL_KSTR_NAME_KEY, CLIENT_API_SSL_KSTR_NAME_DEFAULT));
    configsMap.put(CLIENT_API_SSL_KSTR_TYPE_KEY, properties.getProperty(
        CLIENT_API_SSL_KSTR_TYPE_KEY, CLIENT_API_SSL_KSTR_TYPE_DEFAULT));
    configsMap.put(CLIENT_API_SSL_TSTR_NAME_KEY, properties.getProperty(
        CLIENT_API_SSL_TSTR_NAME_KEY, CLIENT_API_SSL_TSTR_NAME_DEFAULT));
    configsMap.put(CLIENT_API_SSL_TSTR_TYPE_KEY, properties.getProperty(
        CLIENT_API_SSL_TSTR_TYPE_KEY, CLIENT_API_SSL_TSTR_TYPE_DEFAULT));
    configsMap.put(CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, properties.getProperty(
      CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, CLIENT_API_SSL_CRT_PASS_FILE_NAME_DEFAULT));
    configsMap.put(CLIENT_API_SSL_KEY_NAME_KEY, properties.getProperty(
      CLIENT_API_SSL_KEY_NAME_KEY, CLIENT_API_SSL_KEY_NAME_DEFAULT));
    configsMap.put(CLIENT_API_SSL_CRT_NAME_KEY, properties.getProperty(
      CLIENT_API_SSL_CRT_NAME_KEY, CLIENT_API_SSL_CRT_NAME_DEFAULT));
    configsMap.put(JAVA_HOME_KEY, properties.getProperty(
        JAVA_HOME_KEY));
    configsMap.put(PARALLEL_STAGE_EXECUTION_KEY, properties.getProperty(
            PARALLEL_STAGE_EXECUTION_KEY, PARALLEL_STAGE_EXECUTION_DEFAULT));
    configsMap.put(SERVER_TMP_DIR_KEY, properties.getProperty(
            SERVER_TMP_DIR_KEY, SERVER_TMP_DIR_DEFAULT));
    configsMap.put(EXTERNAL_SCRIPT_TIMEOUT_KEY, properties.getProperty(
            EXTERNAL_SCRIPT_TIMEOUT_KEY, EXTERNAL_SCRIPT_TIMEOUT_DEFAULT));

    configsMap.put(SHARED_RESOURCES_DIR_KEY, properties.getProperty(
       SHARED_RESOURCES_DIR_KEY, SHARED_RESOURCES_DIR_DEFAULT));

    configsMap.put(KDC_PORT_KEY, properties.getProperty(
        KDC_PORT_KEY, KDC_PORT_KEY_DEFAULT));

    configsMap.put(AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_KEY, properties.getProperty(
            AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_KEY, AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_DEFAULT));
    configsMap.put(PROXY_ALLOWED_HOST_PORTS, properties.getProperty(
        PROXY_ALLOWED_HOST_PORTS, PROXY_ALLOWED_HOST_PORTS_DEFAULT));

    File passFile = new File(configsMap.get(SRVR_KSTR_DIR_KEY) + File.separator
        + configsMap.get(SRVR_CRT_PASS_FILE_KEY));
    String password = null;

    if (!passFile.exists()) {
      LOG.info("Generation of file with password");
      try {
        password = RandomStringUtils.randomAlphanumeric(Integer
            .parseInt(configsMap.get(SRVR_CRT_PASS_LEN_KEY)));
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
    configsMap.put(SRVR_CRT_PASS_KEY, password);

    if (getApiSSLAuthentication()) {
      LOG.info("API SSL Authentication is turned on.");
      File httpsPassFile = new File(configsMap.get(CLIENT_API_SSL_KSTR_DIR_NAME_KEY)
        + File.separator + configsMap.get(CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY));

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
        LOG.error("Run \"ambari-server setup-https\" or set " + Configuration.API_USE_SSL + " = false.");
        throw new RuntimeException("Error reading certificate password from " +
          "file " + httpsPassFile.getAbsolutePath());

      }

      configsMap.put(CLIENT_API_SSL_CRT_PASS_KEY, password);
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
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
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
   * Loads trusted certificates store properties
   */
  protected void loadSSLParams(){
    if (properties.getProperty(SSL_TRUSTSTORE_PATH_KEY) != null) {
      System.setProperty(JAVAX_SSL_TRUSTSTORE, properties.getProperty(SSL_TRUSTSTORE_PATH_KEY));
    }
    if (properties.getProperty(SSL_TRUSTSTORE_PASSWORD_KEY) != null) {
      String ts_password = readPasswordFromStore(
              properties.getProperty(SSL_TRUSTSTORE_PASSWORD_KEY));
      if (ts_password != null) {
        System.setProperty(JAVAX_SSL_TRUSTSTORE_PASSWORD, ts_password);
      } else {
        System.setProperty(JAVAX_SSL_TRUSTSTORE_PASSWORD,
                properties.getProperty(SSL_TRUSTSTORE_PASSWORD_KEY));
      }
    }
    if (properties.getProperty(SSL_TRUSTSTORE_TYPE_KEY) != null) {
      System.setProperty(JAVAX_SSL_TRUSTSTORE_TYPE, properties.getProperty(SSL_TRUSTSTORE_TYPE_KEY));
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
    String fileName = properties.getProperty(VIEWS_DIR, VIEWS_DIR_DEFAULT);
    return new File(fileName);
  }

  /**
   * Determine whether or not view validation is enabled.
   *
   * @return true if view validation is enabled
   */
  public boolean isViewValidationEnabled() {
    return Boolean.parseBoolean(properties.getProperty(VIEWS_VALIDATE, VIEWS_VALIDATE_DEFAULT));
  }

  /**
   * Determine whether or not a view that has been undeployed (archive deleted) should be removed from the database.
   *
   * @return true if undeployed views should be removed
   */
  public boolean isViewRemoveUndeployedEnabled() {
    return Boolean.parseBoolean(properties.getProperty(VIEWS_REMOVE_UNDEPLOYED, VIEWS_REMOVE_UNDEPLOYED_DEFAULT));
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
    String fileName = properties.getProperty(BOOTSTRAP_DIR, BOOTSTRAP_DIR_DEFAULT);
    return new File(fileName);
  }

  public String getBootStrapScript() {
    return properties.getProperty(BOOTSTRAP_SCRIPT, BOOTSTRAP_SCRIPT_DEFAULT);
  }

  public String getBootSetupAgentScript() {
    return properties.getProperty(BOOTSTRAP_SETUP_AGENT_SCRIPT,
        "/usr/lib/python2.6/site-packages/ambari_server/setupAgent.py");
  }

  public String getBootSetupAgentPassword() {
    String pass = configsMap.get(PASSPHRASE_KEY);

    if (null != pass) {
      return pass;
    }

    // fallback
    return properties.getProperty(BOOTSTRAP_SETUP_AGENT_PASSWORD, "password");
  }

  public File getRecommendationsDir() {
    String fileName = properties.getProperty(RECOMMENDATIONS_DIR, RECOMMENDATIONS_DIR_DEFAULT);
    return new File(fileName);
  }

  public String areHostsSysPrepped(){
    return properties.getProperty(SYS_PREPPED_HOSTS_KEY, SYS_PREPPED_HOSTS_DEFAULT);
  }

  public String getStackAdvisorScript() {
    return properties.getProperty(STACK_ADVISOR_SCRIPT, STACK_ADVISOR_SCRIPT_DEFAULT);
  }

  public String getRollingUpgradeMinStack() {
    return properties.getProperty(ROLLING_UPGRADE_MIN_STACK_KEY, ROLLING_UPGRADE_MIN_STACK_DEFAULT);
  }

  public String getRollingUpgradeMaxStack() {
    return properties.getProperty(ROLLING_UPGRADE_MAX_STACK_KEY, ROLLING_UPGRADE_MAX_STACK_DEFAULT);
  }

  /**
   * @return a list of prefixes. Packages whose name starts with any of these
   * prefixes, should be skipped during upgrade.
   */
  public List<String> getRollingUpgradeSkipPackagesPrefixes() {
    String propertyValue = properties.getProperty(ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES_KEY,
            ROLLING_UPGRADE_SKIP_PACKAGES_PREFIXES_DEFAULT);
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
    return Boolean.parseBoolean(properties.getProperty(STACK_UPGRADE_BYPASS_PRECHECKS_KEY, STACK_UPGRADE_BYPASS_PRECHECKS_DEFAULT));
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
    return "true".equalsIgnoreCase(properties.getProperty(API_CSRF_PREVENTION_KEY, API_CSRF_PREVENTION_DEFAULT));
  }

  /**
   * Gets client security type
   * @return appropriate ClientSecurityType
   */
  public ClientSecurityType getClientSecurityType() {
    return ClientSecurityType.fromString(properties.getProperty(CLIENT_SECURITY_KEY));
  }

  public void setClientSecurityType(ClientSecurityType type) {
    properties.setProperty(CLIENT_SECURITY_KEY, type.toString());
  }

  public void setLdap(String host, String userClass, String userNameAttr, String groupClass, String groupName, String groupMember,
      String baseDN, boolean anon, String managerDN, String managerPass) {
    properties.setProperty(LDAP_PRIMARY_URL_KEY, host);
    properties.setProperty(LDAP_USER_OBJECT_CLASS_KEY, userClass);
    properties.setProperty(LDAP_USERNAME_ATTRIBUTE_KEY, userNameAttr);
    properties.setProperty(LDAP_GROUP_OBJECT_CLASS_KEY, groupClass);
    properties.setProperty(LDAP_GROUP_NAMING_ATTR_KEY, groupName);
    properties.setProperty(LDAP_GROUP_MEMEBERSHIP_ATTR_KEY, groupMember);
    properties.setProperty(LDAP_BASE_DN_KEY, baseDN);
    properties.setProperty(LDAP_BIND_ANONYMOUSLY_KEY, String.valueOf(anon));
    properties.setProperty(LDAP_MANAGER_DN_KEY, managerDN);
    properties.setProperty(LDAP_MANAGER_PASSWORD_KEY, managerPass);
  }

  public String getWebAppDir() {
    LOG.info("Web App DIR test " + properties.getProperty(WEBAPP_DIR));
    return properties.getProperty(WEBAPP_DIR, "web");
  }

  /**
   * Get the file that will be used for host mapping.
   * @return null if such a file is not present, value if present.
   */
  public String getHostsMapFile() {
    LOG.info("Hosts Mapping File " + properties.getProperty(SRVR_HOSTS_MAPPING));
    return properties.getProperty(SRVR_HOSTS_MAPPING);
  }

  /**
   * Gets ambari stack-path
   * @return String
   */
  public String getMetadataPath() {
    return properties.getProperty(METADATA_DIR_PATH);
  }

  /**
   * Gets ambari common services path
   * @return String
   */
  public String getCommonServicesPath() {
    return properties.getProperty(COMMON_SERVICES_DIR_PATH);
  }

  public String getServerVersionFilePath() {
    return properties.getProperty(SERVER_VERSION_FILE);
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
   * Check to see if the API should be authenticated or not
   * @return false if not, true if the authentication is enabled.
   */
  public boolean getApiAuthentication() {
    return ("true".equals(properties.getProperty(API_AUTHENTICATE, "false")));
  }

  /**
   * Gets ssl api port
   * @return int
   */
  public int getClientSSLApiPort() {
    return Integer.parseInt(properties.getProperty(CLIENT_API_SSL_PORT_KEY,
                                                   String.valueOf(CLIENT_API_SSL_PORT_DEFAULT)));
  }

  /**
   * Check to see if the API should be authenticated via ssl or not
   * @return false if not, true if ssl needs to be used.
   */
  public boolean getApiSSLAuthentication() {
    return ("true".equals(properties.getProperty(API_USE_SSL, "false")));
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
    return properties.getProperty(HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY, HTTP_STRICT_TRANSPORT_HEADER_VALUE_DEFAULT);
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
    return properties.getProperty(HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY, HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT);
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
    return properties.getProperty(HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY, HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT);
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
    return properties.getProperty(VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY, VIEWS_HTTP_STRICT_TRANSPORT_HEADER_VALUE_DEFAULT);
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
    return properties.getProperty(VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY, VIEWS_HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT);
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
    return properties.getProperty(VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY, VIEWS_HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT);
  }

  /**
   * Check to see if two-way SSL auth should be used between server and agents
   * or not
   *
   * @return true two-way SSL authentication is enabled
   */
  public boolean getTwoWaySsl() {
    return ("true".equals(properties.getProperty(SRVR_TWO_WAY_SSL_KEY,
      SRVR_TWO_WAY_SSL_DEFAULT)));
  }

  /**
   * Check to see if the API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isApiGzipped() {
    return "true".equalsIgnoreCase(properties.getProperty(
        API_GZIP_COMPRESSION_ENABLED_KEY,
        API_GZIP_COMPRESSION_ENABLED_DEFAULT));
  }

  /**
   * Check to see if the agent API responses should be compressed via gzip or not
   * @return false if not, true if gzip compression needs to be used.
   */
  public boolean isAgentApiGzipped() {
    return "true".equalsIgnoreCase(properties.getProperty(
      AGENT_API_GZIP_COMPRESSION_ENABLED_KEY,
      API_GZIP_COMPRESSION_ENABLED_DEFAULT));
  }

  /**
   * Check to see if the API responses should be compressed via gzip or not
   * Content will only be compressed if content length is either unknown or
   * greater this value
   * @return false if not, true if ssl needs to be used.
   */
  public String getApiGzipMinSize() {
    return properties.getProperty(API_GZIP_MIN_COMPRESSION_SIZE_KEY,
      API_GZIP_MIN_COMPRESSION_SIZE_DEFAULT);
  }

  /**
   * Check persistence type Ambari Server should use. Possible values:
   * in-memory - use in-memory Derby database to store data
   * local - use local Postgres instance
   * remote - use provided jdbc driver name and url to connect to database
   */
  public PersistenceType getPersistenceType() {
    String value = properties.getProperty(SERVER_PERSISTENCE_TYPE_KEY, SERVER_PERSISTENCE_TYPE_DEFAULT);
    return PersistenceType.fromString(value);
  }

  public String getDatabaseDriver() {
    if (getPersistenceType() != PersistenceType.IN_MEMORY) {
      return properties.getProperty(SERVER_JDBC_DRIVER_KEY, JDBC_LOCAL_DRIVER);
    } else {
      return JDBC_IN_MEMROY_DRIVER;
    }
  }

  public String getDatabaseUrl() {
    if (getPersistenceType() != PersistenceType.IN_MEMORY) {
      String URI = properties.getProperty(SERVER_JDBC_URL_KEY);
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
    String dbName = properties.getProperty(SERVER_DB_NAME_KEY);
    if(dbName == null || dbName.isEmpty()) {
      throw new RuntimeException("Server DB Name is not configured!");
    }

    return JDBC_LOCAL_URL + dbName;
  }

  public String getDatabaseUser() {
    return properties.getProperty(SERVER_JDBC_USER_NAME_KEY, SERVER_JDBC_USER_NAME_DEFAULT);
  }

  public String getDatabasePassword() {
    String passwdProp = properties.getProperty(SERVER_JDBC_USER_PASSWD_KEY);
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
      return readPasswordFromFile(passwdProp, SERVER_JDBC_USER_PASSWD_DEFAULT);
    }
  }

  public String getRcaDatabaseDriver() {
    return properties.getProperty(SERVER_JDBC_RCA_DRIVER_KEY, JDBC_RCA_LOCAL_DRIVER);
  }

  public String getRcaDatabaseUrl() {
    return properties.getProperty(SERVER_JDBC_RCA_URL_KEY, JDBC_RCA_LOCAL_URL);
  }

  public String getRcaDatabaseUser() {
    return properties.getProperty(SERVER_JDBC_RCA_USER_NAME_KEY, SERVER_JDBC_RCA_USER_NAME_DEFAULT);
  }

  public String getRcaDatabasePassword() {
    String passwdProp = properties.getProperty(SERVER_JDBC_RCA_USER_PASSWD_KEY);
    if (passwdProp != null) {
      String dbpasswd = readPasswordFromStore(passwdProp);
      if (dbpasswd != null) {
        return dbpasswd;
      }
    }
    return readPasswordFromFile(passwdProp, SERVER_JDBC_RCA_USER_PASSWD_DEFAULT);
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

    ldapServerProperties.setPrimaryUrl(properties.getProperty(
        LDAP_PRIMARY_URL_KEY, LDAP_PRIMARY_URL_DEFAULT));
    ldapServerProperties.setSecondaryUrl(properties.getProperty(
        LDAP_SECONDARY_URL_KEY));
    ldapServerProperties.setUseSsl("true".equalsIgnoreCase(properties.
        getProperty(LDAP_USE_SSL_KEY)));
    ldapServerProperties.setAnonymousBind("true".
        equalsIgnoreCase(properties.getProperty(LDAP_BIND_ANONYMOUSLY_KEY,
            LDAP_BIND_ANONYMOUSLY_DEFAULT)));
    ldapServerProperties.setManagerDn(properties.getProperty(
        LDAP_MANAGER_DN_KEY));
    String ldapPasswordProperty = properties.getProperty(LDAP_MANAGER_PASSWORD_KEY);
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
    ldapServerProperties.setBaseDN(properties.getProperty
        (LDAP_BASE_DN_KEY, LDAP_BASE_DN_DEFAULT));
    ldapServerProperties.setUsernameAttribute(properties.
        getProperty(LDAP_USERNAME_ATTRIBUTE_KEY, LDAP_USERNAME_ATTRIBUTE_DEFAULT));

    ldapServerProperties.setUserBase(properties.getProperty(
      LDAP_USER_BASE_KEY, LDAP_USER_BASE_DEFAULT));
    ldapServerProperties.setUserObjectClass(properties.getProperty(
      LDAP_USER_OBJECT_CLASS_KEY, LDAP_USER_OBJECT_CLASS_DEFAULT));
    ldapServerProperties.setDnAttribute(properties.getProperty(
      LDAP_DN_ATTRIBUTE_KEY, LDAP_DN_ATTRIBUTE_DEFAULT));

    ldapServerProperties.setGroupBase(properties.
        getProperty(LDAP_GROUP_BASE_KEY, LDAP_GROUP_BASE_DEFAULT));
    ldapServerProperties.setGroupObjectClass(properties.
        getProperty(LDAP_GROUP_OBJECT_CLASS_KEY, LDAP_GROUP_OBJECT_CLASS_DEFAULT));
    ldapServerProperties.setGroupMembershipAttr(properties.getProperty(
        LDAP_GROUP_MEMEBERSHIP_ATTR_KEY, LDAP_GROUP_MEMBERSHIP_ATTR_DEFAULT));
    ldapServerProperties.setGroupNamingAttr(properties.
        getProperty(LDAP_GROUP_NAMING_ATTR_KEY, LDAP_GROUP_NAMING_ATTR_DEFAULT));
    ldapServerProperties.setAdminGroupMappingRules(properties.getProperty(
        LDAP_ADMIN_GROUP_MAPPING_RULES_KEY, LDAP_ADMIN_GROUP_MAPPING_RULES_DEFAULT));
    ldapServerProperties.setGroupSearchFilter(properties.getProperty(
        LDAP_GROUP_SEARCH_FILTER_KEY, LDAP_GROUP_SEARCH_FILTER_DEFAULT));
    ldapServerProperties.setReferralMethod(properties.getProperty(
      LDAP_REFERRAL_KEY, LDAP_REFERRAL_DEFAULT));
    ldapServerProperties.setPaginationEnabled("true".equalsIgnoreCase(
      properties.getProperty(LDAP_PAGINATION_ENABLED_KEY, LDAP_PAGINATION_ENABLED_DEFAULT)));

    if (properties.containsKey(LDAP_GROUP_BASE_KEY) ||
        properties.containsKey(LDAP_GROUP_OBJECT_CLASS_KEY) ||
        properties.containsKey(LDAP_GROUP_MEMEBERSHIP_ATTR_KEY) ||
        properties.containsKey(LDAP_GROUP_NAMING_ATTR_KEY) ||
        properties.containsKey(LDAP_ADMIN_GROUP_MAPPING_RULES_KEY) ||
        properties.containsKey(LDAP_GROUP_SEARCH_FILTER_KEY)) {
      ldapServerProperties.setGroupMappingEnabled(true);
    }

    return ldapServerProperties;
  }

  public boolean isLdapConfigured() {
    return Boolean.parseBoolean(properties.getProperty(IS_LDAP_CONFIGURED, IS_LDAP_CONFIGURED_DEFAULT));
  }

  public String getServerOsType() {
    return properties.getProperty(OS_VERSION_KEY, "");
  }

  public String getServerOsFamily() {
    return properties.getProperty(OS_FAMILY_KEY, "");
  }

  public String getMasterHostname(String defaultValue) {
    return properties.getProperty(BOOTSTRAP_MASTER_HOSTNAME, defaultValue);
  }

  public int getClientApiPort() {
    return Integer.parseInt(properties.getProperty(CLIENT_API_PORT_KEY, String.valueOf(CLIENT_API_PORT_DEFAULT)));
  }

  public String getOjdbcJarName() {
	return properties.getProperty(OJDBC_JAR_NAME_KEY, OJDBC_JAR_NAME_DEFAULT);
  }

  public String getJavaHome() {
    return properties.getProperty(JAVA_HOME_KEY);
  }

  public String getJDKName() {
    return properties.getProperty(JDK_NAME_KEY);
  }

  public String getJCEName() {
    return properties.getProperty(JCE_NAME_KEY);
  }

  public String getServerDBName() {
	return properties.getProperty(SERVER_DB_NAME_KEY, SERVER_DB_NAME_DEFAULT);
  }

  public String getMySQLJarName() {
	return properties.getProperty(MYSQL_JAR_NAME_KEY, MYSQL_JAR_NAME_DEFAULT);
  }

  public JPATableGenerationStrategy getJPATableGenerationStrategy() {
    return JPATableGenerationStrategy.fromString(System.getProperty(SERVER_JDBC_GENERATE_TABLES_KEY));
  }

  public int getConnectionMaxIdleTime() {
    return Integer.parseInt(properties.getProperty
      (SERVER_CONNECTION_MAX_IDLE_TIME, String.valueOf("900000")));
  }

  /**
   * @return the name to be used for audit information if there is no
   * logged-in user.  Default is '_anonymous'.
   */
  public String getAnonymousAuditName() {
    return properties.getProperty(ANONYMOUS_AUDIT_NAME_KEY, "_anonymous");
  }

  public boolean isMasterKeyPersisted() {
    File masterKeyFile = getMasterKeyLocation();
    return (masterKeyFile != null) && masterKeyFile.exists();
  }

  public File getServerKeyStoreDirectory() {
    String path = properties.getProperty(SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT);
    return ((path == null) || path.isEmpty())
        ? new File(".")
        : new File(path);
  }

  /**
   * Returns a File pointing where master key file is expected to be
   * <p/>
   * The master key file is named 'master'.  The directory that this file is to be found in is
   * calculated by obtaining the directory path assigned to the Ambari property
   * 'security.master.key.location'; else if that value is empty, then the directory is determined
   * by calling {@link #getServerKeyStoreDirectory()}.
   * <p/>
   * If it exists, this file contains the key used to decrypt values stored in the master keystore.
   *
   * @return a File that points to the master key file
   * @see #getServerKeyStoreDirectory()
   * @see #MASTER_KEY_FILENAME_DEFAULT
   */
  public File getMasterKeyLocation() {
    File location;
    String path = properties.getProperty(MASTER_KEY_LOCATION);

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
   * The master keystore file is named 'credentials.jceks'.  The directory that this file is to be
   * found in is calculated by obtaining the directory path assigned to the Ambari property
   * 'security.master.keystore.location'; else if that value is empty, then the directory is determined
   * by calling {@link #getServerKeyStoreDirectory()}.
   * <p/>
   * The location is calculated by obtaining the Ambari property directory path assigned to the key
   * 'security.master.keystore.location'. If that value is empty, then the directory is determined
   * by {@link #getServerKeyStoreDirectory()}.
   *
   * @return a File that points to the master keystore file
   * @see #getServerKeyStoreDirectory()
   * @see #MASTER_KEYSTORE_FILENAME_DEFAULT
   */
  public File getMasterKeyStoreLocation() {
    File location;
    String path = properties.getProperty(MASTER_KEYSTORE_LOCATION);

    if (StringUtils.isEmpty(path)) {
      location = new File(getServerKeyStoreDirectory(), MASTER_KEYSTORE_FILENAME_DEFAULT);
      LOG.debug("Value of {} is not set, using {}", MASTER_KEYSTORE_LOCATION, location.getAbsolutePath());
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
    String value = properties.getProperty(TEMPORARY_KEYSTORE_RETENTION_MINUTES);

    if(StringUtils.isEmpty(value)) {
      LOG.debug("Value of {} is not set, using default value ({})",
          TEMPORARY_KEYSTORE_RETENTION_MINUTES, TEMPORARY_KEYSTORE_RETENTION_MINUTES_DEFAULT);
      minutes = TEMPORARY_KEYSTORE_RETENTION_MINUTES_DEFAULT;
    }
    else {
      try {
        minutes = Long.parseLong(value);
        LOG.debug("Value of {} is {}", TEMPORARY_KEYSTORE_RETENTION_MINUTES, value);
      } catch (NumberFormatException e) {
        LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
            TEMPORARY_KEYSTORE_RETENTION_MINUTES, value, TEMPORARY_KEYSTORE_RETENTION_MINUTES_DEFAULT);
        minutes = TEMPORARY_KEYSTORE_RETENTION_MINUTES_DEFAULT;
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
    String value = properties.getProperty(TEMPORARY_KEYSTORE_ACTIVELY_PURGE);

    if (StringUtils.isEmpty(value)) {
      LOG.debug("Value of {} is not set, using default value ({})",
          TEMPORARY_KEYSTORE_ACTIVELY_PURGE, TEMPORARY_KEYSTORE_ACTIVELY_PURGE_DEFAULT);
      return TEMPORARY_KEYSTORE_ACTIVELY_PURGE_DEFAULT;
    } else if ("true".equalsIgnoreCase(value)) {
      LOG.debug("Value of {} is {}", TEMPORARY_KEYSTORE_ACTIVELY_PURGE, value);
      return true;
    } else if ("false".equalsIgnoreCase(value)) {
      LOG.debug("Value of {} is {}", TEMPORARY_KEYSTORE_ACTIVELY_PURGE, value);
      return false;
    } else {
      LOG.warn("Value of {} should be either \"true\" or \"false\" but is \"{}\", falling back to default value ({})",
          TEMPORARY_KEYSTORE_ACTIVELY_PURGE, value, TEMPORARY_KEYSTORE_ACTIVELY_PURGE_DEFAULT);
      return TEMPORARY_KEYSTORE_ACTIVELY_PURGE_DEFAULT;
    }
  }

  public String getSrvrDisabledCiphers() {
    String disabledCiphers = properties.getProperty(SRVR_DISABLED_CIPHERS,
                                                    properties.getProperty(SRVR_DISABLED_CIPHERS,
                                                                           SRVR_DISABLED_CIPHERS_DEFAULT));
    return disabledCiphers.trim();
  }

  public String getSrvrDisabledProtocols() {
    String disabledProtocols = properties.getProperty(SRVR_DISABLED_PROTOCOLS,
                                                      properties.getProperty(SRVR_DISABLED_PROTOCOLS,
                                                                             SRVR_DISABLED_PROTOCOLS_DEFAULT));
    return disabledProtocols.trim();
  }

  public int getOneWayAuthPort() {
    return Integer.parseInt(properties.getProperty(SRVR_ONE_WAY_SSL_PORT_KEY,
      String.valueOf(SRVR_ONE_WAY_SSL_PORT_DEFAULT)));
  }

  public int getTwoWayAuthPort() {
    return Integer.parseInt(properties.getProperty(SRVR_TWO_WAY_SSL_PORT_KEY,
                                                   String.valueOf(SRVR_TWO_WAY_SSL_PORT_DEFAULT)));
  }

  /**
   * @return custom properties for database connections
   */
  public Map<String,String> getDatabaseCustomProperties() {
    if (null != customDbProperties) {
      return customDbProperties;
    }

    customDbProperties = new HashMap<String, String>();

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.startsWith(SERVER_JDBC_PROPERTIES_PREFIX)) {
        customDbProperties.put(key.substring(SERVER_JDBC_PROPERTIES_PREFIX.length()), val);
      }
    }

    return customDbProperties;
  }

  /**
   * @return Custom property for request header size
   */
  public int getHttpRequestHeaderSize() {
    return Integer.parseInt(properties.getProperty(
        SERVER_HTTP_REQUEST_HEADER_SIZE, String.valueOf(SERVER_HTTP_REQUEST_HEADER_SIZE_DEFAULT)));
  }

  /**
   * @return Custom property for response header size
   */
  public int getHttpResponseHeaderSize() {
    return Integer.parseInt(properties.getProperty(
        SERVER_HTTP_RESPONSE_HEADER_SIZE, String.valueOf(SERVER_HTTP_RESPONSE_HEADER_SIZE_DEFAULT)));
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
    String stringValue = properties.getProperty(SERVER_EC_CACHE_SIZE);
    long value = SERVER_EC_CACHE_SIZE_DEFAULT;
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
    String stringValue = properties.getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED);
    boolean value = SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT;
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
    String stringValue = properties.getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT;
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
    String stringValue = properties.getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT;
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
    String stringValue =
      properties.getProperty(SERVER_STALE_CONFIG_CACHE_ENABLED_KEY,
        SERVER_STALE_CONFIG_CACHE_ENABLED_DEFAULT);
    return "true".equalsIgnoreCase(stringValue);
  }

  /**
   * @return expiration time of stale config cache
   */
  public Integer staleConfigCacheExpiration() {
    return Integer.parseInt(properties.getProperty(SERVER_STALE_CONFIG_CACHE_EXPIRATION_KEY,
        SERVER_STALE_CONFIG_CACHE_EXPIRATION_DEFAULT));
  }

  /**
   * @return a string array of suffixes used to validate repo URLs.
   */
  public String[] getRepoValidationSuffixes(String osType) {
    String repoSuffixes;

    if(osFamily.isUbuntuFamily(osType)) {
      repoSuffixes = properties.getProperty(REPO_SUFFIX_KEY_UBUNTU,
          REPO_SUFFIX_UBUNTU);
    } else {
      repoSuffixes = properties.getProperty(REPO_SUFFIX_KEY_DEFAULT,
          REPO_SUFFIX_DEFAULT);
    }

    return repoSuffixes.split(",");
  }


  public String isExecutionSchedulerClusterd() {
    return properties.getProperty(EXECUTION_SCHEDULER_CLUSTERED_KEY, "false");
  }

  public String getExecutionSchedulerThreads() {
    return properties.getProperty(EXECUTION_SCHEDULER_THREADS_KEY,
                                  DEFAULT_SCHEDULER_THREAD_COUNT);
  }

  public Integer getRequestReadTimeout() {
    return Integer.parseInt(properties.getProperty(REQUEST_READ_TIMEOUT,
      REQUEST_READ_TIMEOUT_DEFAULT));
  }

  public Integer getRequestConnectTimeout() {
    return Integer.parseInt(properties.getProperty(REQUEST_CONNECT_TIMEOUT,
                                                   REQUEST_CONNECT_TIMEOUT_DEFAULT));
  }

  /**
   * @return The read timeout value for views when trying to access ambari apis
   */
  public Integer getViewAmbariRequestReadTimeout() {
    return Integer.parseInt(properties.getProperty(AMBARI_REQUEST_READ_TIMEOUT,
      AMBARI_REQUEST_READ_TIMEOUT_DEFAULT));
  }

  /**
   * @return The connection timeout value for views when trying to connect to ambari apis
   */
  public Integer getViewAmbariRequestConnectTimeout() {
    return Integer.parseInt(properties.getProperty(AMBARI_REQUEST_CONNECT_TIMEOUT,
      AMBARI_REQUEST_CONNECT_TIMEOUT_DEFAULT));
  }

  public String getExecutionSchedulerConnections() {
    return properties.getProperty(EXECUTION_SCHEDULER_CONNECTIONS_KEY,
      DEFAULT_SCHEDULER_MAX_CONNECTIONS);
  }

  public Long getExecutionSchedulerMisfireToleration() {
    String limit = properties.getProperty
      (EXECUTION_SCHEDULER_MISFIRE_TOLERATION_KEY,
        DEFAULT_EXECUTION_SCHEDULER_MISFIRE_TOLERATION);
    return Long.parseLong(limit);
  }

  public Integer getExecutionSchedulerStartDelay() {
    String delay = properties.getProperty(EXECUTION_SCHEDULER_START_DELAY_KEY,
                                          DEFAULT_SCHEDULER_START_DELAY_SECONDS);
    return Integer.parseInt(delay);
  }

  public Long getExecutionSchedulerWait() {

    String stringValue = properties.getProperty(
      EXECUTION_SCHEDULER_WAIT_KEY, DEFAULT_EXECUTION_SCHEDULER_WAIT_SECONDS);
    Long sleepTime = Long.parseLong(DEFAULT_EXECUTION_SCHEDULER_WAIT_SECONDS);
    if (stringValue != null) {
      try {
        sleepTime = Long.valueOf(stringValue);
      } catch (NumberFormatException ignored) {
        LOG.warn("Value of {} ({}) should be a number, " +
          "falling back to default value ({})", EXECUTION_SCHEDULER_WAIT_KEY,
          stringValue, DEFAULT_EXECUTION_SCHEDULER_WAIT_SECONDS);
      }

    }

    if (sleepTime > 60) {
      LOG.warn("Value of {} ({}) should be a number between 1 adn 60, " +
          "falling back to maximum value ({})",
        EXECUTION_SCHEDULER_WAIT_KEY, sleepTime, 60);
      sleepTime = 60L;
    }
    return sleepTime*1000;
  }

  public Integer getExternalScriptTimeout() {
    return Integer.parseInt(properties.getProperty(EXTERNAL_SCRIPT_TIMEOUT_KEY, EXTERNAL_SCRIPT_TIMEOUT_DEFAULT));
  }

  public boolean getParallelStageExecution() {
    return "true".equalsIgnoreCase(configsMap.get(PARALLEL_STAGE_EXECUTION_KEY));
  }

  public String getCustomActionDefinitionPath() {
    return properties.getProperty(CUSTOM_ACTION_DEFINITION_KEY,
                                  CUSTOM_ACTION_DEFINITION_DEF_VALUE);
  }

  public int getAgentPackageParallelCommandsLimit() {
    int value = Integer.parseInt(properties.getProperty(
            AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_KEY,
            AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT_DEFAULT));
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
    String key = isPackageInstallationTask ? AGENT_PACKAGE_INSTALL_TASK_TIMEOUT_KEY : AGENT_TASK_TIMEOUT_KEY;
    String defaultValue = isPackageInstallationTask ? AGENT_PACKAGE_INSTALL_TASK_TIMEOUT_DEFAULT : AGENT_TASK_TIMEOUT_DEFAULT;
    String value = properties.getProperty(key, defaultValue);
    if (StringUtils.isNumeric(value)) {
      return value;
    } else {
      LOG.warn(String.format("Value of %s (%s) should be a number, " +
          "falling back to default value (%s)",
          key, value, defaultValue));
      return defaultValue;
    }
  }

  /**
   * @return default server-side task timeout in seconds.
   */
  public Integer getDefaultServerTaskTimeout() {
    String value = properties.getProperty(SERVER_TASK_TIMEOUT_KEY, SERVER_TASK_TIMEOUT_DEFAULT);
    if (StringUtils.isNumeric(value)) {
      return Integer.parseInt(value);
    } else {
      LOG.warn("Value of {} ({}) should be a number, falling back to default value ({})",
          SERVER_TASK_TIMEOUT_KEY, value, SERVER_TASK_TIMEOUT_DEFAULT);
      return Integer.parseInt(SERVER_TASK_TIMEOUT_DEFAULT);
    }
  }

  public String getResourceDirPath() {
    return properties.getProperty(RESOURCES_DIR_KEY, RESOURCES_DIR_DEFAULT);
  }

  public String getSharedResourcesDirPath(){
      return properties.getProperty(SHARED_RESOURCES_DIR_KEY, SHARED_RESOURCES_DIR_DEFAULT);
  }

  public String getServerJDBCPostgresSchemaName() {
    return properties.getProperty(SERVER_JDBC_POSTGRES_SCHEMA_NAME, "");
  }

  /**
   * @return max thread pool size for clients, default 25
   */
  public int getClientThreadPoolSize() {
    return Integer.parseInt(properties.getProperty(
        CLIENT_THREADPOOL_SIZE_KEY, String.valueOf(CLIENT_THREADPOOL_SIZE_DEFAULT)));
  }

  /**
   * @return max thread pool size for agents, default 25
   */
  public int getAgentThreadPoolSize() {
    return Integer.parseInt(properties.getProperty(
        AGENT_THREADPOOL_SIZE_KEY, String.valueOf(AGENT_THREADPOOL_SIZE_DEFAULT)));
  }

  /**
   * Get the view extraction thread pool max size.
   *
   * @return the view extraction thread pool max size
   */
  public int getViewExtractionThreadPoolMaxSize() {
    return Integer.parseInt(properties.getProperty(
        VIEW_EXTRACTION_THREADPOOL_MAX_SIZE_KEY, String.valueOf(VIEW_EXTRACTION_THREADPOOL_MAX_SIZE_DEFAULT)));
  }

  /**
   * Get the view extraction thread pool core size.
   *
   * @return the view extraction thread pool core size
   */
  public int getViewExtractionThreadPoolCoreSize() {
    return Integer.parseInt(properties.getProperty(
        VIEW_EXTRACTION_THREADPOOL_CORE_SIZE_KEY, String.valueOf(VIEW_EXTRACTION_THREADPOOL_CORE_SIZE_DEFAULT)));
  }

  /**
   * Get the view extraction thread pool timeout.
   *
   * @return the view extraction thread pool timeout
   */
  public long getViewExtractionThreadPoolTimeout() {
    return Long.parseLong(properties.getProperty(
        VIEW_EXTRACTION_THREADPOOL_TIMEOUT_KEY, String.valueOf(VIEW_EXTRACTION_THREADPOOL_TIMEOUT_DEFAULT)));
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
    return Integer.parseInt(properties.getProperty(
      SERVER_HTTP_SESSION_INACTIVE_TIMEOUT,
      "1800"));
  }

  /**
   * Gets the location of the XML alert template file which contains the
   * velocity templates for outbound notifications.
   *
   * @return the location of the template file, or {@code null} if not defined.
   */
  public String getAlertTemplateFile() {
    return properties.getProperty(ALERT_TEMPLATE_FILE);
  }

  /**
   * @return max thread pool size for AlertEventPublisher, default 2
   */
  public int getAlertEventPublisherPoolSize() {
    return Integer.parseInt(properties.getProperty(
      ALERTS_EXECUTION_SCHEDULER_THREADS_KEY, ALERTS_EXECUTION_SCHEDULER_THREADS_DEFAULT));
  }

  /**
   * Get the node recovery type DEFAULT|AUTO_START|FULL
   * @return
   */
  public String getNodeRecoveryType() {
    return properties.getProperty(RECOVERY_TYPE_KEY, RECOVERY_TYPE_DEFAULT);
  }

  /**
   * Get configured max count of recovery attempt allowed per host component in a window
   * This is reset when agent is restarted.
   * @return
   */
  public String getNodeRecoveryMaxCount() {
    return properties.getProperty(RECOVERY_MAX_COUNT_KEY, RECOVERY_MAX_COUNT_DEFAULT);
  }

  /**
   * Get configured max lifetime count of recovery attempt allowed per host component.
   * This is reset when agent is restarted.
   * @return
   */
  public String getNodeRecoveryLifetimeMaxCount() {
    return properties.getProperty(RECOVERY_LIFETIME_MAX_COUNT_KEY, RECOVERY_LIFETIME_MAX_COUNT_DEFAULT);
  }

  /**
   * Get configured window size in minutes
   * @return
   */
  public String getNodeRecoveryWindowInMin() {
    return properties.getProperty(RECOVERY_WINDOW_IN_MIN_KEY, RECOVERY_WINDOW_IN_MIN_DEFAULT);
  }

  /**
   * Get the components for which recovery is disabled
   * @return
   */
  public String getDisabledComponents() {
    return properties.getProperty(RECOVERY_DISABLED_COMPONENTS_KEY, "");
  }

  /**
   * Get the components for which recovery is enabled
   * @return
   */
  public String getEnabledComponents() {
    return properties.getProperty(RECOVERY_ENABLED_COMPONENTS_KEY, "");
  }

  /**
   * Get the configured retry gap between tries per host component
   * @return
   */
  public String getNodeRecoveryRetryGap() {
    return properties.getProperty(RECOVERY_RETRY_GAP_KEY, RECOVERY_RETRY_GAP_DEFAULT);
  }

  /**
   * Gets the default KDC port to use when no port is specified in KDC hostname
   *
   * @return the default KDC port to use.
   */
  public String getDefaultKdcPort() {
    return properties.getProperty(KDC_PORT_KEY, KDC_PORT_KEY_DEFAULT);
  }

  /**
   * Gets the inactivity timeout value, in milliseconds, for socket connection
   * made to KDC Server for its reachability verification.
   *
   * @return the timeout value as configured in {@code ambari.properties}
   * 				 or {@code 10000 ms} for default.
   */
  public int getKdcConnectionCheckTimeout() {
    return Integer.parseInt(properties.getProperty(
      KDC_CONNECTION_CHECK_TIMEOUT_KEY, KDC_CONNECTION_CHECK_TIMEOUT_DEFAULT));
  }

  /**
   * Gets the directory where Ambari is to store cached keytab files.
   *
   * @return a File containing the path to the directory to use to store cached keytab files
   */
  public File getKerberosKeytabCacheDir() {
    String fileName = properties.getProperty(KERBEROS_KEYTAB_CACHE_DIR_KEY, KERBEROS_KEYTAB_CACHE_DIR_DEFAULT);
    return new File(fileName);
  }

  /**
   * Determine whether or not ambari server credentials validation is enabled.
   *
   * @return true if ambari server credentials check is enabled
   */
  public boolean isKerberosJaasConfigurationCheckEnabled() {
    return Boolean.parseBoolean(properties.getProperty(
      KERBEROS_CHECK_JAAS_CONFIGURATION_KEY,
      KERBEROS_CHECK_JAAS_CONFIGURATION_DEFAULT));
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
    String connectionPoolType = properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL, ConnectionPoolType.INTERNAL.getName());

    if (connectionPoolType.equals(ConnectionPoolType.C3P0.getName())) {
      return ConnectionPoolType.C3P0;
    }

    return ConnectionPoolType.INTERNAL;
  }

  /**
   * Gets the minimum number of connections that should always exist in the
   * connection pool.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_MIN_CONNECTIONS}
   */
  public int getConnectionPoolMinimumSize() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_MIN_SIZE, DEFAULT_JDBC_POOL_MIN_CONNECTIONS));
  }

  /**
   * Gets the maximum number of connections that should even exist in the
   * connection pool.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_MAX_CONNECTIONS}
   */
  public int getConnectionPoolMaximumSize() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_MAX_SIZE, DEFAULT_JDBC_POOL_MAX_CONNECTIONS));
  }

  /**
   * Gets the maximum amount of time in seconds any connection, whether its been
   * idle or active, should even be in the pool. This will terminate the
   * connection after the expiration age and force new connections to be opened.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_MAX_AGE_SECONDS}
   */
  public int getConnectionPoolMaximumAge() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_MAX_AGE, DEFAULT_JDBC_POOL_MAX_AGE_SECONDS));
  }

  /**
   * Gets the maximum amount of time in seconds that an idle connection can
   * remain in the pool. This should always be greater than the value returned
   * from {@link #getConnectionPoolMaximumExcessIdle()}
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_MAX_IDLE_TIME_SECONDS}
   */
  public int getConnectionPoolMaximumIdle() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME,
        DEFAULT_JDBC_POOL_MAX_IDLE_TIME_SECONDS));
  }

  /**
   * Gets the maximum amount of time in seconds that connections beyond the
   * minimum pool size should remain in the pool. This should always be less
   * than than the value returned from {@link #getConnectionPoolMaximumIdle()}
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_EXCESS_MAX_IDLE_TIME_SECONDS}
   */
  public int getConnectionPoolMaximumExcessIdle() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS,
        DEFAULT_JDBC_POOL_EXCESS_MAX_IDLE_TIME_SECONDS));
  }

  /**
   * Gets the number of connections that should be retrieved when the pool size
   * must increase. It's wise to set this higher than 1 since the assumption is
   * that a pool that needs to grow should probably grow by more than 1.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_ACQUISITION_SIZE}
   */
  public int getConnectionPoolAcquisitionSize() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE,
        DEFAULT_JDBC_POOL_ACQUISITION_SIZE));
  }

  /**
   * Gets the number of times connections should be retried to be acquired from
   * the database before giving up.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_ACQUISITION_RETRY_ATTEMPTS}
   */
  public int getConnectionPoolAcquisitionRetryAttempts() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS,
        DEFAULT_JDBC_POOL_ACQUISITION_RETRY_ATTEMPTS));
  }

  /**
   * Gets the delay in milliseconds between connection acquire attempts.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_ACQUISITION_RETRY_DELAY}
   */
  public int getConnectionPoolAcquisitionRetryDelay() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY,
        DEFAULT_JDBC_POOL_ACQUISITION_RETRY_DELAY));
  }


  /**
   * Gets the number of seconds in between testing each idle connection in the
   * connection pool for validity.
   *
   * @return default of {@value #DEFAULT_JDBC_POOL_IDLE_TEST_INTERVAL}
   */
  public int getConnectionPoolIdleTestInternval() {
    return Integer.parseInt(properties.getProperty(
        SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL,
        DEFAULT_JDBC_POOL_IDLE_TEST_INTERVAL));
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
   * Max allowed entries in metrics cache.
   * @deprecated Ehcache only supports either a max heap bytes or entries.
   */
  @Deprecated
  public int getMetricCacheMaxEntries() {
    return Integer.parseInt(properties.getProperty(TIMELINE_METRICS_CACHE_MAX_ENTRIES,
      DEFAULT_TIMELINE_METRICS_CACHE_MAX_ENTRIES));
  }

  /**
   * Eviction time for entries in metrics cache.
   */
  public int getMetricCacheTTLSeconds() {
    return Integer.parseInt(properties.getProperty(TIMELINE_METRICS_CACHE_TTL,
      DEFAULT_TIMELINE_METRICS_CACHE_TTL));
  }

  /**
   * Max time to idle for entries in the cache.
   */
  public int getMetricCacheIdleSeconds() {
    return Integer.parseInt(properties.getProperty(TIMELINE_METRICS_CACHE_IDLE_TIME,
        DEFAULT_TIMELINE_METRICS_CACHE_IDLE_TIME));
  }

  /**
   * Separate timeout settings for metrics cache.
   * @return milliseconds
   */
  public int getMetricsRequestReadTimeoutMillis() {
    return Integer.parseInt(properties.getProperty(TIMELINE_METRICS_REQUEST_READ_TIMEOUT,
      DEFAULT_TIMELINE_METRICS_REQUEST_READ_TIMEOUT));
  }

  /**
   * Separate timeout settings for metrics cache.
   * Timeout on reads for update requests made for smaller time intervals.
   *
   * @return milliseconds
   */
  public int getMetricsRequestIntervalReadTimeoutMillis() {
    return Integer.parseInt(properties.getProperty(TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT,
      DEFAULT_TIMELINE_METRICS_REQUEST_INTERVAL_READ_TIMEOUT));
  }

  /**
   * Separate timeout settings for metrics cache.
   * @return milliseconds
   */
  public int getMetricsRequestConnectTimeoutMillis() {
    return Integer.parseInt(properties.getProperty(TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT,
      DEFAULT_TIMELINE_METRICS_REQUEST_CONNECT_TIMEOUT));
  }

  /**
   * Diable metrics caching.
   * @return true / false
   */
  public boolean isMetricsCacheDisabled() {
    return Boolean.parseBoolean(properties.getProperty(TIMELINE_METRICS_CACHE_DISABLE, "false"));
  }

  /**
   * Constant fudge factor subtracted from the cache update requests to
   * account for unavailability of data on the trailing edge due to buffering.
   */
  public Long getMetricRequestBufferTimeCatchupInterval() {
    return Long.parseLong(properties.getProperty(TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL,
      DEFAULT_TIMELINE_METRICS_REQUEST_CATCHUP_INTERVAL));
  }

  /**
   * Percentage of total heap allocated to metrics cache, default is 15%.
   * Default heap setting for the server is 2 GB so max allocated heap size
   * for this cache is 300 MB.
   */
  public String getMetricsCacheManagerHeapPercent() {
    String percent = properties.getProperty(TIMELINE_METRICS_CACHE_HEAP_PERCENT,
      DEFAULT_TIMELINE_METRICS_CACHE_HEAP_PERCENT);

    return percent.trim().endsWith("%") ? percent.trim() : percent.trim() + "%";
  }

  /**
   * Ambari server temp dir
   * @return server temp dir
   */
  public String getServerTempDir() {
    return properties.getProperty(SERVER_TMP_DIR_KEY, SERVER_TMP_DIR_DEFAULT);
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
    return Boolean.parseBoolean(properties.getProperty(
        EXPERIMENTAL_CONCURRENCY_STAGE_PROCESSING_ENABLED, Boolean.FALSE.toString()));
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
    return Boolean.parseBoolean(
        properties.getProperty(ALERTS_CACHE_ENABLED, Boolean.FALSE.toString()));
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
    return Integer.parseInt(
        properties.getProperty(ALERTS_CACHE_FLUSH_INTERVAL, ALERTS_CACHE_FLUSH_INTERVAL_DEFAULT));
  }

  /**
   * Gets the size of the alerts cache, if enabled.
   *
   * @return the cache flush interval, or {@value #ALERTS_CACHE_SIZE_DEFAULT} if
   *         not set.
   */
  @Experimental(feature = ExperimentalFeature.ALERT_CACHING)
  public int getAlertCacheSize() {
    return Integer.parseInt(properties.getProperty(ALERTS_CACHE_SIZE, ALERTS_CACHE_SIZE_DEFAULT));
  }

  /**
   * Get the ambari display URL
   * @return
   */
  public String getAmbariDisplayUrl() {
    return properties.getProperty(AMBARI_DISPLAY_URL, null);
  }


  /**
   * @return number of retry attempts for api and blueprint operations
   */
  public int getOperationsRetryAttempts() {
    String property = properties.getProperty(OPERATIONS_RETRY_ATTEMPTS_KEY, OPERATIONS_RETRY_ATTEMPTS_DEFAULT);
    Integer attempts = Integer.valueOf(property);
    if (attempts < 0) {
      LOG.warn("Invalid operations retry attempts number ({}), should be [0,{}]. Value reset to default {}",
          attempts, RETRY_ATTEMPTS_LIMIT, OPERATIONS_RETRY_ATTEMPTS_DEFAULT);
      attempts = Integer.valueOf(OPERATIONS_RETRY_ATTEMPTS_DEFAULT);
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

  public String getAgentStackRetryOnInstallCount(){
    return properties.getProperty(AGENT_STACK_RETRY_COUNT_KEY, AGENT_STACK_RETRY_COUNT_DEFAULT);
  }

  public String isAgentStackRetryOnInstallEnabled(){
    return properties.getProperty(AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY_KEY, AGENT_STACK_RETRY_ON_REPO_UNAVAILABILITY_DEFAULT);
  }
}
