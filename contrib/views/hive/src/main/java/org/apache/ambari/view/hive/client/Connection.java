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

package org.apache.ambari.view.hive.client;

import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.shims.ShimLoader;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hive.service.auth.HiveAuthFactory;
import org.apache.hive.service.auth.KerberosSaslHelper;
import org.apache.hive.service.auth.PlainSaslHelper;
import org.apache.hive.service.auth.SaslQOP;
import org.apache.hive.service.cli.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds session
 */
public class Connection {
  private final static Logger LOG =
      LoggerFactory.getLogger(Connection.class);
  private String host;
  private int port;
  private Map<String, String> authParams;

  private TCLIService.Client client = null;
  private Map<String, TSessionHandle> sessHandles = null;
  private TProtocolVersion protocol = null;
  private TTransport transport;

  private DDLDelegator ddl;
  private String username;

  public Connection(String host, int port, Map<String, String> authParams, String username) throws HiveClientException {
    this.host = host;
    this.port = port;
    this.authParams = authParams;
    this.username = username;

    this.sessHandles = new HashMap<String, TSessionHandle>();

    openConnection();
    ddl = new DDLDelegator(this);
  }

  public DDLDelegator ddl() {
    return ddl;
  }

  public synchronized void openConnection() throws HiveClientException {
    try {
      transport = getTransport();
      transport.open();
      client = new TCLIService.Client(new TBinaryProtocol(transport));
    } catch (TTransportException e) {
      throw new HiveClientException("H020 Could not establish connecton to "
          + host + ":" + port + ": " + e.toString(), e);
    }
    LOG.info("Hive connection opened");
  }

  /**
   * Based on JDBC implementation of HiveConnection.createBinaryTransport
   *
   * @return transport
   * @throws HiveClientException
   */
  protected TTransport getTransport() throws HiveClientException, TTransportException {
    TTransport transport;
    boolean assumeSubject =
        Utils.HiveAuthenticationParams.AUTH_KERBEROS_AUTH_TYPE_FROM_SUBJECT.equals(authParams
            .get(Utils.HiveAuthenticationParams.AUTH_KERBEROS_AUTH_TYPE));
    try {
      if (!Utils.HiveAuthenticationParams.AUTH_SIMPLE.equalsIgnoreCase(authParams.get(Utils.HiveAuthenticationParams.AUTH_TYPE))) {
        // If Kerberos
        Map<String, String> saslProps = new HashMap<String, String>();
        SaslQOP saslQOP = SaslQOP.AUTH;
        if (authParams.containsKey(Utils.HiveAuthenticationParams.AUTH_PRINCIPAL)) {
          if (authParams.containsKey(Utils.HiveAuthenticationParams.AUTH_QOP)) {
            try {
              saslQOP = SaslQOP.fromString(authParams.get(Utils.HiveAuthenticationParams.AUTH_QOP));
            } catch (IllegalArgumentException e) {
              throw new HiveClientException("H040 Invalid " + Utils.HiveAuthenticationParams.AUTH_QOP +
                  " parameter. " + e.getMessage(), e);
            }
          }
          saslProps.put(Sasl.QOP, saslQOP.toString());
          saslProps.put(Sasl.SERVER_AUTH, "true");

          Configuration conf = new Configuration();
          conf.set("hadoop.security.authentication", "kerberos");
          UserGroupInformation.setConfiguration(conf);

          transport = KerberosSaslHelper.getKerberosTransport(
              authParams.get(Utils.HiveAuthenticationParams.AUTH_PRINCIPAL), host,
              HiveAuthFactory.getSocketTransport(host, port, 10000), saslProps,
              assumeSubject);
        } else {
          // If there's a delegation token available then use token based connection
          String tokenStr = getClientDelegationToken(authParams);
          if (tokenStr != null) {
            transport = KerberosSaslHelper.getTokenTransport(tokenStr,
                host, HiveAuthFactory.getSocketTransport(host, port, 10000), saslProps);
          } else {
            // we are using PLAIN Sasl connection with user/password
            String userName = getAuthParamDefault(Utils.HiveAuthenticationParams.AUTH_USER, getUsername());
            String passwd = getAuthParamDefault(Utils.HiveAuthenticationParams.AUTH_PASSWD, Utils.HiveAuthenticationParams.ANONYMOUS_USER);
            // Note: Thrift returns an SSL socket that is already bound to the specified host:port
            // Therefore an open called on this would be a no-op later
            // Hence, any TTransportException related to connecting with the peer are thrown here.
            // Bubbling them up the call hierarchy so that a retry can happen in openTransport,
            // if dynamic service discovery is configured.
            if (isSslConnection()) {
              // get SSL socket
              String sslTrustStore = authParams.get(Utils.HiveAuthenticationParams.SSL_TRUST_STORE);
              String sslTrustStorePassword = authParams.get(Utils.HiveAuthenticationParams.SSL_TRUST_STORE_PASSWORD);
              if (sslTrustStore == null || sslTrustStore.isEmpty()) {
                transport = HiveAuthFactory.getSSLSocket(host, port, 10000);
              } else {
                transport = HiveAuthFactory.getSSLSocket(host, port, 10000,
                    sslTrustStore, sslTrustStorePassword);
              }
            } else {
              // get non-SSL socket transport
              transport = HiveAuthFactory.getSocketTransport(host, port, 10000);
            }
            // Overlay the SASL transport on top of the base socket transport (SSL or non-SSL)
            transport = PlainSaslHelper.getPlainTransport(userName, passwd, transport);
          }
        }
      } else {
        //NOSASL
        return HiveAuthFactory.getSocketTransport(host, port, 10000);
      }
    } catch (SaslException e) {
      throw new HiveClientException("H040 Could not create secure connection to "
          + host + ": " + e.getMessage(), e);
    }
    return transport;
  }

  private boolean isSslConnection() {
    return "true".equalsIgnoreCase(authParams.get(Utils.HiveAuthenticationParams.USE_SSL));
  }

  // Lookup the delegation token. First in the connection URL, then Configuration
  private String getClientDelegationToken(Map<String, String> jdbcConnConf) throws HiveClientException {
    String tokenStr = null;
    if (Utils.HiveAuthenticationParams.AUTH_TOKEN.equalsIgnoreCase(jdbcConnConf.get(Utils.HiveAuthenticationParams.AUTH_TYPE))) {
      // check delegation token in job conf if any
      try {
        tokenStr = ShimLoader.getHadoopShims().
            getTokenStrForm(HiveAuthFactory.HS2_CLIENT_TOKEN);
      } catch (IOException e) {
        throw new HiveClientException("H050 Error reading token", e);
      }
    }
    return tokenStr;
  }

  private String getAuthParamDefault(String key, String defaultValue) {
    if (authParams.containsKey(key)) {
      return authParams.get(key);
    }
    return defaultValue;
  }

  public synchronized TSessionHandle openSession() throws HiveClientException {
    return openSession(null);
  }

  public synchronized TSessionHandle openSession(String forcedTag) throws HiveClientException {
    TOpenSessionResp openResp = new HiveCall<TOpenSessionResp>(this) {
      @Override
      public TOpenSessionResp body() throws HiveClientException {
        TOpenSessionReq openReq = new TOpenSessionReq();
        try {
          return client.OpenSession(openReq);
        } catch (TException e) {
          throw new HiveClientException("H060 Unable to open Hive session", e);
        }

      }
    }.call();
    Utils.verifySuccess(openResp.getStatus(), "H070 Unable to open Hive session");

    if (protocol == null)
      protocol = openResp.getServerProtocolVersion();
    LOG.info("Hive session opened");

    TSessionHandle sessionHandle = openResp.getSessionHandle();
    String tag;
    if (forcedTag == null)
      tag = Hex.encodeHexString(sessionHandle.getSessionId().getGuid());
    else
      tag = forcedTag;

    sessHandles.put(tag, sessionHandle);

    return sessionHandle;
  }

  public TSessionHandle getSessionByTag(String tag) throws HiveClientException {
    TSessionHandle sessionHandle = sessHandles.get(tag);
    if (sessionHandle == null) {
      throw new HiveClientException("E030 Session with provided tag not found", null);
    }
    return sessionHandle;
  }

  public TSessionHandle getOrCreateSessionByTag(String tag) throws HiveClientException {
    try {
      return getSessionByTag(tag);
    } catch (HiveClientException e) {
      return openSession(tag);
    }
  }

  public void invalidateSessionByTag(String tag) throws HiveClientException {
    TSessionHandle sessionHandle = getSessionByTag(tag);
    closeSession(sessionHandle);
    sessHandles.remove(tag);
  }

  private synchronized void closeSession(TSessionHandle sessHandle) throws HiveClientException {
    if (sessHandle == null) return;
    TCloseSessionReq closeReq = new TCloseSessionReq(sessHandle);
    TCloseSessionResp closeResp = null;
    try {
      closeResp = client.CloseSession(closeReq);
      Utils.verifySuccess(closeResp.getStatus(), "H080 Unable to close Hive session");
    } catch (TException e) {
      throw new HiveClientException("H090 Unable to close Hive session", e);
    }
    LOG.info("Hive session closed");
  }

  public synchronized void closeConnection() throws HiveClientException {
    if (client == null) return;
    try {

      for(Iterator<Map.Entry<String, TSessionHandle>> it = sessHandles.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<String, TSessionHandle> entry = it.next();
        try {
          closeSession(entry.getValue());
        } catch (HiveClientException e) {
          LOG.error("Unable to close Hive session: " + e.getMessage());
        } finally {
          it.remove();
        }
      }

    } finally {
      transport.close();
      transport = null;
      client = null;
      protocol = null;
    }
    LOG.info("Connection to Hive closed");
  }

  /**
   * Execute query
   * @param cmd query
   * @param async wait till query finish?
   * @return handle of operation
   * @throws HiveClientException
   */
  public TOperationHandle execute(final TSessionHandle session, final String cmd, final boolean async) throws HiveClientException {
    TOperationHandle handle = null;

    String[] commands = cmd.split(";");
    for(int i=0; i<commands.length; i++) {
      final String oneCmd = commands[i];
      final boolean lastCommand = i == commands.length-1;

      TExecuteStatementResp execResp = new HiveCall<TExecuteStatementResp>(this) {
        @Override
        public TExecuteStatementResp body() throws HiveClientException {

          TExecuteStatementReq execReq = null;
          execReq = new TExecuteStatementReq(session, oneCmd);

          // only last command should be asynchronous and return some results
          // all previous commands are supposed to be set properties entries
          if (lastCommand) {
            execReq.setRunAsync(async);
          } else {
            execReq.setRunAsync(false);
          }
          execReq.setConfOverlay(new HashMap<String, String>());
          try {
            return client.ExecuteStatement(execReq);
          } catch (TException e) {
            throw new HiveClientException("H100 Unable to submit statement " + cmd, e);
          }

        }
      }.call();

      Utils.verifySuccess(execResp.getStatus(), "H110 Unable to submit statement");
      //TODO: check if status have results
      handle = execResp.getOperationHandle();
    }
    if (handle == null) {
      throw new HiveClientException("H120 Empty command given", null);
    }
    return handle;
  }

  public TOperationHandle executeAsync(TSessionHandle session, String cmd) throws HiveClientException {
    return execute(session, cmd, true);
  }

  public TOperationHandle executeSync(TSessionHandle session, String cmd) throws HiveClientException {
    return execute(session, cmd, false);
  }

  public String getLogs(TOperationHandle handle) {
    LogsCursor results = new LogsCursor(this, handle);
    results.reset(); // we have to read from FIRST line, to get
    // logs from beginning on every call this function
    List<String> logLineList = results.getValuesInColumn(0);
    StringBuilder log = new StringBuilder();
    for (String line : logLineList) {
      log.append(line);
      log.append('\n');
    }
    return log.toString();
  }

  public Cursor getResults(TOperationHandle handle) {
    Cursor cursor = new Cursor(this, handle);
    cursor.reset(); // we have to read from FIRST line, to get
    // logs from beginning on every call this function
    return cursor;
  }

  /**
   * Retrieve status of operation
   * @param operationHandle handle
   * @return thrift status response object
   * @throws HiveClientException
   */
  public TGetOperationStatusResp getOperationStatus(final TOperationHandle operationHandle) throws HiveClientException {
    return new HiveCall<TGetOperationStatusResp>(this) {
      @Override
      public TGetOperationStatusResp body() throws HiveClientException {

        TGetOperationStatusReq statusReq = new TGetOperationStatusReq(operationHandle);
        try {
          return client.GetOperationStatus(statusReq);
        } catch (TException e) {
          throw new HiveClientException("H130 Unable to fetch operation status", e);
        }

      }
    }.call();
  }

  /**
   * Cancel operation
   * @param operationHandle operation handle
   */
  public void cancelOperation(final TOperationHandle operationHandle) throws HiveClientException {
    TCancelOperationResp cancelResp = new HiveCall<TCancelOperationResp>(this) {
      @Override
      public TCancelOperationResp body() throws HiveClientException {
        TCancelOperationReq cancelReq = new TCancelOperationReq(operationHandle);
        try {
          return client.CancelOperation(cancelReq);
        } catch (TException e) {
          throw new HiveClientException("H140 Unable to cancel operation", null);
        }
      }
    }.call();
    Utils.verifySuccess(cancelResp.getStatus(), "H150 Unable to cancel operation");
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public TCLIService.Client getClient() {
    return client;
  }

  public void setClient(TCLIService.Client client) {
    this.client = client;
  }

  public TProtocolVersion getProtocol() {
    return protocol;
  }

  public void setProtocol(TProtocolVersion protocol) {
    this.protocol = protocol;
  }

  public Map<String, String> getAuthParams() {
    return authParams;
  }

  public void setAuthParams(Map<String, String> authParams) {
    this.authParams = authParams;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
