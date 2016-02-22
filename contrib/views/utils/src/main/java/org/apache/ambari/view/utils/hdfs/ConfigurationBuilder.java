/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.hdfs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.ambari.view.utils.ambari.NoClusterAssociatedException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.azure.NativeAzureFileSystem;
import org.apache.hadoop.fs.azure.Wasb;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.web.WebHdfsFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Builds the Configuration of HDFS based on ViewContext.
 * Supports both directly specified properties and cluster associated
 * properties loading.
 */
public class ConfigurationBuilder {
  protected static final Logger LOG = LoggerFactory.getLogger(ConfigurationBuilder.class);
  public static final String CORE_SITE = "core-site";
  public static final String HDFS_SITE = "hdfs-site";

  public static final String DEFAULT_FS_INSTANCE_PROPERTY = "webhdfs.url";
  public static final String DEFAULT_FS_CLUSTER_PROPERTY  = "fs.defaultFS";

  public static final String NAMESERVICES_INSTANCE_PROPERTY = "webhdfs.nameservices";
  public static final String NAMESERVICES_CLUSTER_PROPERTY  = "dfs.nameservices";
  public static final String HA_NAMENODES_INSTANCE_PROPERTY = "webhdfs.ha.namenodes.list";

  public static final String HA_NAMENODES_CLUSTER_PROPERTY  = "dfs.ha.namenodes.%s";
  public static final String NAMENODE_RPC_NN1_INSTANCE_PROPERTY = "webhdfs.ha.namenode.rpc-address.nn1";
  public static final String NAMENODE_RPC_NN2_INSTANCE_PROPERTY = "webhdfs.ha.namenode.rpc-address.nn2";

  public static final String NAMENODE_RPC_NN_CLUSTER_PROPERTY   = "dfs.namenode.rpc-address.%s.%s";
  public static final String NAMENODE_HTTP_NN1_INSTANCE_PROPERTY = "webhdfs.ha.namenode.http-address.nn1";
  public static final String NAMENODE_HTTP_NN2_INSTANCE_PROPERTY = "webhdfs.ha.namenode.http-address.nn2";

  public static final String NAMENODE_HTTP_NN_CLUSTER_PROPERTY   = "dfs.namenode.http-address.%s.%s";
  public static final String FAILOVER_PROXY_PROVIDER_INSTANCE_PROPERTY = "webhdfs.client.failover.proxy.provider";
  public static final String FAILOVER_PROXY_PROVIDER_CLUSTER_PROPERTY  = "dfs.client.failover.proxy.provider.%s";

  public static final String UMASK_CLUSTER_PROPERTY = "fs.permissions.umask-mode";
  public static final String UMASK_INSTANCE_PROPERTY = "hdfs.umask-mode";

  private Configuration conf = new Configuration();
  private ViewContext context;
  private AmbariApi ambariApi = null;
  private AuthConfigurationBuilder authParamsBuilder;
  private Map<String, String> authParams;
  private URI defaultFsUri;
  /**
   * Constructor of ConfigurationBuilder based on ViewContext
   * @param context ViewContext
   */
  public ConfigurationBuilder(ViewContext context) {
    this.context = context;
    this.ambariApi = new AmbariApi(context);
    this.authParamsBuilder = new AuthConfigurationBuilder(context);
  }

  private void parseProperties() throws HdfsApiException {
    String defaultFS = getDefaultFS(context);

    try {

      if (isHAEnabled(defaultFS)) {
        copyHAProperties(defaultFS);

        LOG.info("HA HDFS cluster found.");
      } else {
        if (defaultFS.startsWith("hdfs://") && !hasPort(defaultFS)) {
          defaultFS = addPortIfMissing(defaultFS);
        }
      }

      defaultFsUri = new URI(defaultFS);

    } catch (URISyntaxException e) {
      throw new HdfsApiException("HDFS060 Invalid " + DEFAULT_FS_INSTANCE_PROPERTY +
          "='" + defaultFS + "' URI", e);
    }

    conf.set("fs.defaultFS", defaultFS);
    LOG.info(String.format("HdfsApi configured to connect to defaultFS='%s'", defaultFS));
  }

  private String getDefaultFS(ViewContext context) throws HdfsApiException {
    String defaultFS = getProperty(CORE_SITE, DEFAULT_FS_CLUSTER_PROPERTY, DEFAULT_FS_INSTANCE_PROPERTY);

    if (defaultFS == null || defaultFS.isEmpty())
      throw new HdfsApiException("HDFS070 fs.defaultFS is not configured");

    defaultFS = addProtocolIfMissing(defaultFS);
    return defaultFS;
  }

  private String getProperty(String type, String key, String instanceProperty) {
    String value;
    try {
      value = ambariApi.getCluster().getConfigurationValue(type, key);
    } catch (NoClusterAssociatedException e) {
      value = context.getProperties().get(instanceProperty);
    }
    return value;
  }

  private void copyPropertyIfExists(String type, String key) {
    String value;
    try {
      value = ambariApi.getCluster().getConfigurationValue(type, key);
      if (value != null) {
        conf.set(key, value);
        LOG.debug("set " + key + " = " + value);
      } else {
        LOG.debug("No such property " + type + "/" + key);
      }
    } catch (NoClusterAssociatedException e) {
      LOG.debug("No such property " + type + "/" + key);
    }
  }

  private void copyHAProperties(String defaultFS) throws URISyntaxException, HdfsApiException {
    URI uri = new URI(defaultFS);
    String nameservice = uri.getHost();

    copyClusterProperty(NAMESERVICES_CLUSTER_PROPERTY, NAMESERVICES_INSTANCE_PROPERTY);
    String namenodeIDs = copyClusterProperty(String.format(HA_NAMENODES_CLUSTER_PROPERTY, nameservice),
                                             HA_NAMENODES_INSTANCE_PROPERTY);

    String[] namenodes = namenodeIDs.split(",");
    if (namenodes.length != 2) {
      throw new HdfsApiException("HDFS080 " + HA_NAMENODES_INSTANCE_PROPERTY + " namenodes count is not exactly 2");
    }
    //NN1
    copyClusterProperty(String.format(NAMENODE_RPC_NN_CLUSTER_PROPERTY, nameservice, namenodes[0]),
                        NAMENODE_RPC_NN1_INSTANCE_PROPERTY);
    copyClusterProperty(String.format(NAMENODE_HTTP_NN_CLUSTER_PROPERTY, nameservice, namenodes[0]),
                        NAMENODE_HTTP_NN1_INSTANCE_PROPERTY);

    //NN2
    copyClusterProperty(String.format(NAMENODE_RPC_NN_CLUSTER_PROPERTY, nameservice, namenodes[1]),
                        NAMENODE_RPC_NN2_INSTANCE_PROPERTY);
    copyClusterProperty(String.format(NAMENODE_HTTP_NN_CLUSTER_PROPERTY, nameservice, namenodes[1]),
                        NAMENODE_HTTP_NN2_INSTANCE_PROPERTY);

    copyClusterProperty(String.format(FAILOVER_PROXY_PROVIDER_CLUSTER_PROPERTY, nameservice),
                        FAILOVER_PROXY_PROVIDER_INSTANCE_PROPERTY);
  }

  private String copyClusterProperty(String propertyName, String instancePropertyName) {
    String value = getProperty(HDFS_SITE, propertyName, instancePropertyName);
    conf.set(propertyName, value);
    LOG.debug("set " + propertyName + " = " + value);
    return value;
  }

  private boolean isHAEnabled(String defaultFS) throws URISyntaxException {
    URI uri = new URI(defaultFS);
    String nameservice = uri.getHost();
    String namenodeIDs = getProperty(HDFS_SITE, String.format(HA_NAMENODES_CLUSTER_PROPERTY, nameservice),
                                     HA_NAMENODES_INSTANCE_PROPERTY);
    return namenodeIDs != null;
  }

  private static boolean hasPort(String url) throws URISyntaxException {
    URI uri = new URI(url);
    return uri.getPort() != -1;
  }

  protected static String addPortIfMissing(String defaultFs) throws URISyntaxException {
    if (!hasPort(defaultFs)) {
      defaultFs = defaultFs + ":50070";
    }

    return defaultFs;
  }

  protected static String addProtocolIfMissing(String defaultFs) {
    if (!defaultFs.matches("^[^:]+://.*$")) {
      defaultFs = "webhdfs://" + defaultFs;
    }

    return defaultFs;
  }

  /**
   * Set properties relevant to authentication parameters to HDFS Configuration
   * @param authParams list of auth params of View
   */
  public void setAuthParams(Map<String, String> authParams) {
    String auth = authParams.get("auth");
    if (auth != null) {
      conf.set("hadoop.security.authentication", auth);
    }
  }

  /**
   * Build the HDFS configuration
   * @return configured HDFS Configuration object
   * @throws HdfsApiException if configuration parsing failed
   */
  public Configuration buildConfig() throws HdfsApiException {
    parseProperties();
    setAuthParams(buildAuthenticationConfig());

    String umask = context.getProperties().get(UMASK_INSTANCE_PROPERTY);
    if(umask != null && !umask.isEmpty()) conf.set(UMASK_CLUSTER_PROPERTY,umask);

    conf.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
    conf.set("fs.webhdfs.impl", WebHdfsFileSystem.class.getName());
    conf.set("fs.file.impl", LocalFileSystem.class.getName());

    configureWASB();

    return conf;
  }

  /**
   * Fill Azure Blob Storage properties if wasb:// scheme configured
   */
  public void configureWASB() {
    LOG.debug("defaultFsUri.getScheme() == " + defaultFsUri.getScheme());
    if (defaultFsUri.getScheme().equals("wasb")) {
      conf.set("fs.AbstractFileSystem.wasb.impl", Wasb.class.getName());
      conf.set("fs.wasb.impl", NativeAzureFileSystem.class.getName());

      String account = defaultFsUri.getHost();
      LOG.debug("WASB account == " + account);
      copyPropertyIfExists(CORE_SITE, "fs.azure.account.key." + account);
      copyPropertyIfExists(CORE_SITE, "fs.azure.account.keyprovider." + account);
      copyPropertyIfExists(CORE_SITE, "fs.azure.shellkeyprovider.script");
    }
  }

  /**
   * Builds the authentication configuration
   * @return map of HDFS auth params for view
   * @throws HdfsApiException
   */
  public Map<String, String> buildAuthenticationConfig() throws HdfsApiException {
    if (authParams == null) {
      authParams = authParamsBuilder.build();
    }
    return authParams;
  }
}
