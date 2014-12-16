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

import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile.DATA_FILE_NAME;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * KerberosServerAction is an abstract class to be implemented by Kerberos-related
 * {@link org.apache.ambari.server.serveraction.ServerAction} implementations.
 * <p/>
 * This class provides helper methods used to get common properties from the command parameters map
 * and iterate through the Kerberos identity metadata file
 * (see {@link org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile}).
 */
public abstract class KerberosServerAction extends AbstractServerAction {
  /**
   * A (command parameter) property name used to hold the absolute path to the directory that is to
   * be used to store transient data while the request is being processed.  This is expected to be
   * a temporary directory.
   */
  public static final String DATA_DIRECTORY = "data_directory";

  /**
   * A (command parameter) property name used to hold encrypted data representing the KDC
   * administrator credentials
   */
  public static final String ADMINISTRATOR_CREDENTIAL = "kerberos_admin_credential";

  /**
   * A (command parameter) property name used to hold the default Kerberos realm value.
   */
  public static final String DEFAULT_REALM = "default_realm";

  /**
   * A (command parameter) property name used to hold the relevant KDC type value.  See
   * {@link org.apache.ambari.server.serveraction.kerberos.KDCType} for valid values
   */
  public static final String KDC_TYPE = "kdc_type";

  /*
   * Kerberos action shared data entry names
   */
  private static final String PRINCIPAL_PASSWORD_MAP = "principal_password_map";

  private static final Logger LOG = LoggerFactory.getLogger(KerberosServerAction.class);

  /**
   * The Cluster that this ServerAction implementation is executing on
   */
  @Inject
  private Clusters clusters = null;

  /**
   * Given a (command parameter) Map and a property name, attempts to safely retrieve the requested
   * data.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @param propertyName      a String declaring the name of the item from commandParameters to retrieve
   * @return a String or null, depending on the property value and if it existed in commandParameters
   */
  protected static String getCommandParameterValue(Map<String, String> commandParameters, String propertyName) {
    return ((commandParameters == null) || (propertyName == null)) ? null : commandParameters.get(propertyName);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "default_realm" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a String indicating the default realm or null (if not found or set)
   */
  protected static String getDefaultRealm(Map<String, String> commandParameters) {
    return getCommandParameterValue(commandParameters, DEFAULT_REALM);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "kdc_type" property.
   * <p/>
   * If not found, {@link org.apache.ambari.server.serveraction.kerberos.KDCType#MIT_KDC} will be
   * returned as a default value.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a KDCType or null (if not found or set)
   */
  protected static KDCType getKDCType(Map<String, String> commandParameters) {
    String kdcType = getCommandParameterValue(commandParameters, KDC_TYPE);

    return ((kdcType == null) || kdcType.isEmpty())
        ? KDCType.MIT_KDC
        : KDCType.translate(kdcType);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "data_directory" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a String indicating the data directory or null (if not found or set)
   */
  protected static String getDataDirectoryPath(Map<String, String> commandParameters) {
    return getCommandParameterValue(commandParameters, DATA_DIRECTORY);
  }

  /**
   * Sets the shared principal-to-password Map used to store principals and generated password for
   * use within the current request context.
   *
   * @param requestSharedDataContext a Map to be used as shared data among all ServerActions related
   *                                 to a given request
   * @param principalPasswordMap     A Map of principals and password to store
   */
  protected static void setPrincipalPasswordMap(Map<String, Object> requestSharedDataContext,
                                                Map<String, String> principalPasswordMap) {
    if (requestSharedDataContext != null) {
      requestSharedDataContext.put(PRINCIPAL_PASSWORD_MAP, principalPasswordMap);
    }
  }

  /**
   * Gets the shared principal-to-password Map used to store principals and generated password for
   * use within the current request context.
   * <p/>
   * If the requested Map is not found in requestSharedDataContext, one will be created and stored,
   * ensuring that a Map will always be returned, assuming requestSharedDataContext is not null.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return A Map of principals-to-password
   */
  protected static Map<String, String> getPrincipalPasswordMap(Map<String, Object> requestSharedDataContext) {
    if (requestSharedDataContext == null) {
      return null;
    } else {
      Object map = requestSharedDataContext.get(PRINCIPAL_PASSWORD_MAP);

      if (map == null) {
        map = new HashMap<String, String>();
        requestSharedDataContext.put(PRINCIPAL_PASSWORD_MAP, map);
      }

      return (Map<String, String>) map;
    }
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "data_directory" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a String indicating the data directory or null (if not found or set)
   */
  protected KerberosCredential getAdministratorCredential(Map<String, String> commandParameters) throws AmbariException {
    Cluster cluster = clusters.getCluster(getExecutionCommand().getClusterName());

    if(cluster == null)
      throw new AmbariException("Failed get the Cluster object");

    // Create the key like we did when we encrypted the data, based on the Cluster objects hashcode.
    byte[] key = Integer.toHexString(cluster.hashCode()).getBytes();
    return KerberosCredential.decrypt(getCommandParameterValue(commandParameters, ADMINISTRATOR_CREDENTIAL), key);
  }

  /**
   * Attempts to safely retrieve a property with the specified name from the this action's relevant
   * command parameters Map.
   *
   * @param propertyName a String declaring the name of the item from commandParameters to retrieve
   * @return the value of the requested property, or null if not found or set
   */
  protected String getCommandParameterValue(String propertyName) {
    return getCommandParameterValue(getCommandParameters(), propertyName);
  }

  /**
   * Attempts to safely retrieve the "data_directory" property from the this action's relevant
   * command parameters Map.
   *
   * @return a String indicating the data directory or null (if not found or set)
   */
  protected String getDataDirectoryPath() {
    return getDataDirectoryPath(getCommandParameters());
  }

  /**
   * Iterates through the Kerberos identity metadata from the
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile} and calls the
   * implementing class to handle each identity found.
   * <p/>
   * Using the "data_directory" value from this action's command parameters map, creates a
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFileReader} to parse
   * the relative index.dat file and iterate through its "records".  Each "record" is process using
   * {@link #processRecord(java.util.Map, String, KerberosOperationHandler, java.util.Map)}.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this operation
   * @throws AmbariException
   */
  protected CommandReport processIdentities(Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    CommandReport commandReport = null;
    Map<String, String> commandParameters = getCommandParameters();

    if (commandParameters != null) {
      // Grab the relevant data from this action's command parameters map
      KerberosCredential administratorCredential = getAdministratorCredential(commandParameters);
      String defaultRealm = getDefaultRealm(commandParameters);
      KDCType kdcType = getKDCType(commandParameters);
      String dataDirectoryPath = getDataDirectoryPath(commandParameters);

      if (dataDirectoryPath != null) {
        File dataDirectory = new File(dataDirectoryPath);

        if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
          String message = String.format("Failed to process the identities, the data directory does not exist: %s",
              dataDirectory.getAbsolutePath());
          LOG.error(message);
          throw new AmbariException(message);
        }
        // The "index" file is expected to be in the specified data directory and named "index.dat"
        File indexFile = new File(dataDirectory, DATA_FILE_NAME);

        if (!indexFile.canRead()) {
          String message = String.format("Failed to process the identities, cannot read the index file: %s",
              indexFile.getAbsolutePath());
          LOG.error(message);
          throw new AmbariException(message);
        }
        // Create the data file reader to parse and iterate through the records
        KerberosActionDataFileReader reader = null;
        KerberosOperationHandler handler = KerberosOperationHandlerFactory.getKerberosOperationHandler(kdcType);

        if (handler == null) {
          String message = String.format("Failed to process the identities, cannot read the index file: %s",
              indexFile.getAbsolutePath());
          LOG.error(message);
          throw new AmbariException(message);
        }

        handler.open(administratorCredential, defaultRealm);

        try {
          reader = new KerberosActionDataFileReader(indexFile);
          for (Map<String, String> record : reader) {
            // Process the current record
            commandReport = processRecord(record, defaultRealm, handler, requestSharedDataContext);

            // If the principal processor returns a CommandReport, than it is time to stop since
            // an error condition has probably occurred, else all is assumed to be well.
            if (commandReport != null) {
              break;
            }
          }
        } catch (AmbariException e) {
          // Catch this separately from IOException since the reason it was thrown was not the same
          // Note: AmbariException is an IOException, so there may be some confusion
          throw new AmbariException(e.getMessage(), e);
        } catch (IOException e) {
          String message = String.format("Failed to process the identities, cannot read the index file: %s",
              indexFile.getAbsolutePath());
          LOG.error(message, e);
          throw new AmbariException(message, e);
        } finally {
          if (reader != null) {
            // The reader needs to be closed, if it fails to close ignore the exception since
            // there is little we can or care to do about it now.
            try {
              reader.close();
            } catch (IOException e) {
              // Ignore this...
            }
          }

          // The KerberosOperationHandler needs to be closed, if it fails to close ignore the
          // exception since there is little we can or care to do about it now.
          try {
            handler.close();
          } catch (AmbariException e) {
            // Ignore this...
          }
        }
      }
    }

    // If commandReport is null, we can assume this operation was a success, so return a successful
    // CommandReport; else return the previously created CommandReport.
    return (commandReport == null)
        ? createCommandReport(0, HostRoleStatus.COMPLETED, "{}", null, null)
        : commandReport;
  }

  /**
   * Processes an identity as necessary.
   * <p/>
   * This method is called from {@link #processIdentities(Map)} for each
   * identity "record" found in the Kerberos identity metadata file. After processing, it is expected
   * that the return value is null on success and a CommandReport (indicating the error) on failure.
   *
   * @param identityRecord           a Map containing the data for the current identity record
   * @param evaluatedPrincipal       a String indicating the relevant principal
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport, indicating an error condition; or null, indicating a success condition
   * @throws AmbariException if an error occurs while processing the identity record
   */
  protected abstract CommandReport processIdentity(Map<String, String> identityRecord,
                                                   String evaluatedPrincipal,
                                                   KerberosOperationHandler operationHandler,
                                                   Map<String, Object> requestSharedDataContext)
      throws AmbariException;

  /**
   * Process and prepares an identity record to be handled by the implementing class.
   * <p/>
   * Given the data from the record Map, attempts to replace variables in the principal pattern to
   * generate a concrete principal value to further process. This "evaluated principal" is then passed to
   * {@link #processIdentity(java.util.Map, String, KerberosOperationHandler, java.util.Map)}
   * to be handled as needed.
   *
   * @param record                   a Map containing the data for the current identity record
   * @param defaultRealm             a String declaring the default Kerberos realm
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport, indicating an error condition; or null, indicating a success condition
   * @throws AmbariException if an error occurs while processing the identity record
   */
  private CommandReport processRecord(Map<String, String> record, String defaultRealm,
                                      KerberosOperationHandler operationHandler,
                                      Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    CommandReport commandReport = null;

    if (record != null) {
      String principal = record.get(KerberosActionDataFile.PRINCIPAL);
      String host = record.get(KerberosActionDataFile.HOSTNAME);

      if (principal != null) {
        // Evaluate the principal "pattern" found in the record to generate the "evaluated p[rincipal"
        // by replacing the _HOST and _REALM variables.
        String evaluatedPrincipal = principal.replace("_HOST", host).replace("_REALM", defaultRealm);
        commandReport = processIdentity(record, evaluatedPrincipal, operationHandler, requestSharedDataContext);
      }
    }

    return commandReport;
  }
}
