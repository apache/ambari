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
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MITKerberosOperationHandler is an implementation of a KerberosOperationHandler providing
 * functionality specifically for an MIT KDC. See http://web.mit.edu/kerberos.
 * <p/>
 * It is assumed that a MIT Kerberos client is installed and that the kdamin shell command is
 * available
 */
public class MITKerberosOperationHandler extends KerberosOperationHandler {
  private final static Logger LOG = LoggerFactory.getLogger(MITKerberosOperationHandler.class);


  @Override
  public void open(KerberosCredential administratorCredentials, String defaultRealm) throws AmbariException {
    setAdministratorCredentials(administratorCredentials);
    setDefaultRealm(defaultRealm);
  }

  @Override
  public void close() throws AmbariException {
    // There is nothing to do here.
  }

  /**
   * Test to see if the specified principal exists in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the presence of the specified principal.
   *
   * @param principal a String containing the principal to test
   * @return true if the principal exists; false otherwise
   * @throws AmbariException
   */
  @Override
  public boolean principalExists(String principal)
      throws AmbariException {

    if (principal == null) {
      return false;
    } else {
      // Create the KAdmin query to execute:
      String query = String.format("get_principal %s", principal);

      try {
        ShellCommandUtil.Result result = invokeKAdmin(query);

        if (result != null) {
          if (result.isSuccessful()) {
            String stdOut = result.getStdout();

            // If there is data from STDOUT, see if the following string exists:
            //    Principal: <principal>
            return (stdOut != null) && stdOut.contains(String.format("Principal: %s", principal));
          } else {
            LOG.warn("Failed to query for principal {}:\n\tExitCode: {}\n\tSTDOUT: {}\n\tSTDERR: {}",
                principal, result.getExitCode(), result.getStdout(), result.getStderr());
            throw new AmbariException(String.format("Failed to query for principal %s", principal));
          }
        } else {
          String message = String.format("Failed to query for principal %s - Unknown reason", principal);
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } catch (AmbariException e) {
        LOG.error(String.format("Failed to query for principal %s", principal), e);
        throw e;
      }
    }
  }


  /**
   * Creates a new principal in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal add
   * @param password  a String containing the password to use when creating the principal
   * @return true if the principal was successfully created; otherwise false
   * @throws AmbariException
   */
  @Override
  public boolean createServicePrincipal(String principal, String password)
      throws AmbariException {

    if ((principal == null) || principal.isEmpty()) {
      throw new AmbariException("Failed to create new principal - no principal specified");
    } else {
      // Create the kdamin query:  add_principal <-randkey|-pw <password>> <principal>
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append("add_principal");

      // If a password was not supplied, have the KDC generate a random key, else use the supplied
      // password
      if ((password == null) || password.isEmpty()) {
        queryBuilder.append(" -randkey");
      } else {
        queryBuilder.append(" -pw ");
        queryBuilder.append(password);
      }

      queryBuilder.append(" ");
      queryBuilder.append(principal);

      try {
        ShellCommandUtil.Result result = invokeKAdmin(queryBuilder.toString());

        if (result != null) {
          if (result.isSuccessful()) {
            String stdOut = result.getStdout();

            // If there is data from STDOUT, see if the following string exists:
            //    Principal "<principal>" created
            return (stdOut != null) && stdOut.contains(String.format("Principal \"%s\" created", principal));
          } else {
            LOG.warn("Failed to create service principal for {}:\n\tExitCode: {}\n\tSTDOUT: {}\n\tSTDERR: {}",
                principal, result.getExitCode(), result.getStdout(), result.getStderr());
            throw new AmbariException(String.format("Failed to create service principal for %s", principal));
          }
        } else {
          String message = String.format("Failed to create service principal for %s - Unknown reason", principal);
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } catch (AmbariException e) {
        LOG.error(String.format("Failed to create new principal for %s", principal), e);
        throw e;
      }
    }
  }

  /**
   * Updates the password for an existing principal in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the exit code to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @return true if the password was successfully updated; otherwise false
   * @throws AmbariException
   */
  @Override
  public boolean setPrincipalPassword(String principal, String password) throws AmbariException {
    if ((principal == null) || principal.isEmpty()) {
      throw new AmbariException("Failed to set password - no principal specified");
    } else {
      // Create the kdamin query:  change_password <-randkey|-pw <password>> <principal>
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append("change_password");

      // If a password was not supplied, have the KDC generate a random key, else use the supplied
      // password
      if ((password == null) || password.isEmpty()) {
        queryBuilder.append(" -randkey");
      } else {
        queryBuilder.append(" -pw ");
        queryBuilder.append(password);
      }

      queryBuilder.append(" ");
      queryBuilder.append(principal);

      try {
        ShellCommandUtil.Result result = invokeKAdmin(queryBuilder.toString());

        if (result != null) {
          if (result.isSuccessful()) {
            return true;
          } else {
            LOG.warn("Failed to set password for {}:\n\tExitCode: {}\n\tSTDOUT: {}\n\tSTDERR: {}",
                principal, result.getExitCode(), result.getStdout(), result.getStderr());
            throw new AmbariException(String.format("Failed to update password for %s", principal));
          }
        } else {
          String message = String.format("Failed to set password for %s - Unknown reason", principal);
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } catch (AmbariException e) {
        LOG.error(String.format("Failed to set password for %s", principal), e);
        throw e;
      }
    }
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
    if ((principal == null) || principal.isEmpty()) {
      throw new AmbariException("Failed to remove new principal - no principal specified");
    } else {
      try {
        ShellCommandUtil.Result result = invokeKAdmin(String.format("delete_principal -force %s", principal));

        if (result != null) {
          if (result.isSuccessful()) {
            String stdOut = result.getStdout();

            // If there is data from STDOUT, see if the following string exists:
            //    Principal "<principal>" created
            return (stdOut != null) && !stdOut.contains("Principal does not exist");
          } else {
            LOG.warn("Failed to remove service principal for {}:\n\tExitCode: {}\n\tSTDOUT: {}\n\tSTDERR: {}",
                principal, result.getExitCode(), result.getStdout(), result.getStderr());
            throw new AmbariException(String.format("Failed to remove service principal for %s", principal));
          }
        } else {
          String message = String.format("Failed to remove service principal for %s - Unknown reason", principal);
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } catch (AmbariException e) {
        LOG.error(String.format("Failed to remove new principal for %s", principal), e);
        throw e;
      }
    }
  }

  /**
   * Invokes the kadmin shell command to issue queries
   *
   * @param query a String containing the query to send to the kdamin command
   * @return a ShellCommandUtil.Result containing the result of the operation
   * @throws AmbariException
   */
  private ShellCommandUtil.Result invokeKAdmin(String query)
      throws AmbariException {
    ShellCommandUtil.Result result = null;

    if ((query != null) && !query.isEmpty()) {
      KerberosCredential administratorCredentials = getAdministratorCredentials();
      String defaultRealm = getDefaultRealm();

      List<String> command = new ArrayList<String>();
      File tempKeytabFile = null;

      try {
        String adminPrincipal = (administratorCredentials == null)
            ? null
            : administratorCredentials.getPrincipal();

        if ((adminPrincipal == null) || adminPrincipal.isEmpty()) {
          // Set the kdamin interface to be kadmin.local
          command.add("kadmin.local");
        } else {
          String adminPassword = administratorCredentials.getPassword();
          String adminKeyTab = administratorCredentials.getKeytab();

          // Set the kdamin interface to be kadmin
          command.add("kadmin");

          // Add the administrative principal
          command.add("-p");
          command.add(adminPrincipal);

          if ((adminKeyTab != null) && !adminKeyTab.isEmpty()) {
            tempKeytabFile = createKeytabFile(adminKeyTab);

            if (tempKeytabFile != null) {
              // Add keytab file administrative principal
              command.add("-k");
              command.add("-t");
              command.add(tempKeytabFile.getAbsolutePath());
            }
          } else if (adminPassword != null) {
            // Add password for administrative principal
            command.add("-w");
            command.add(adminPassword);
          }
        }

        if ((defaultRealm != null) && !defaultRealm.isEmpty()) {
          // Add default realm clause
          command.add("-r");
          command.add(defaultRealm);
        }

        // Add kadmin query
        command.add("-q");
        command.add(query.replace("\"", "\\\""));

        result = executeCommand(command.toArray(new String[command.size()]));
      } finally {
        // If a temporary keytab file was created, clean it up.
        if (tempKeytabFile != null) {
          if (!tempKeytabFile.delete()) {
            tempKeytabFile.deleteOnExit();
          }
        }
      }
    }

    return result;
  }


}
