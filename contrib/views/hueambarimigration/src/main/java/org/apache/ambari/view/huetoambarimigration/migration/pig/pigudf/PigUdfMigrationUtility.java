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

package org.apache.ambari.view.huetoambarimigration.migration.pig.pigudf;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.udfqueryset.MysqlQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.udfqueryset.OracleQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.udfqueryset.PostgressQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.udfqueryset.QuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.udfqueryset.*;
import org.apache.ambari.view.huetoambarimigration.migration.pig.pigudf.PigUdfMigrationImplementation;
import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.MigrationResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.PigModel;
import org.apache.log4j.Logger;
import org.apache.ambari.view.ViewContext;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;


public class PigUdfMigrationUtility {

  protected MigrationResourceManager resourceManager = null;

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager(ViewContext view) {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }


  public void pigUdfMigration(String username, String instance, ViewContext view, MigrationResponse migrationresult, String jobid) throws IOException, ItemNotFound {

    long startTime = System.currentTimeMillis();

    final Logger logger = Logger.getLogger(PigUdfMigrationUtility.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;

    logger.info("-------------------------------------");
    logger.info("pig udf Migration started");
    logger.info("-------------------------------------");


    int i = 0;

    logger.info("instance is: " + username);
    logger.info("hue username is : " + instance);

    //Reading the configuration file
    PigUdfMigrationImplementation pigudfmigration = new PigUdfMigrationImplementation();

    QuerySet huedatabase = null;

    if (view.getProperties().get("huedrivername").contains("mysql")) {
      huedatabase = new MysqlQuerySet();
    } else if (view.getProperties().get("huedrivername").contains("postgresql")) {
      huedatabase = new PostgressQuerySet();
    } else if (view.getProperties().get("huedrivername").contains("sqlite")) {

      huedatabase = new SqliteQuerySet();
    } else if (view.getProperties().get("huedrivername").contains("oracle")) {
      huedatabase = new OracleQuerySet();
    }

    QuerySetAmbariDB ambaridatabase = null;


    if (view.getProperties().get("ambaridrivername").contains("mysql")) {
      ambaridatabase = new MysqlQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("postgresql")) {
      ambaridatabase = new PostgressQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("oracle")) {
      ambaridatabase = new OracleQuerySetAmbariDB();
    }

    int maxcountforpigudf = 0;
    String dirNameForPigUdf = "";
    int pigInstanceTableId, sequence;

    ArrayList<PigModel> dbpojoPigUdf = new ArrayList<PigModel>();

    try {
      String[] usernames = username.split(",");
      int totalQueries = 0;
      for (int k = 0; k < usernames.length; k++) {
        connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();//connection to Hue DB
        username = usernames[k];
        migrationresult.setProgressPercentage(0);
        logger.info("Migration started for user " + username);
        dbpojoPigUdf = pigudfmigration.fetchFromHueDatabase(username, connectionHuedb, huedatabase);// Fetching pig script details from Hue DB
        totalQueries += dbpojoPigUdf.size();

        for (int j = 0; j < dbpojoPigUdf.size(); j++) {
          logger.info("jar fetched from hue=" + dbpojoPigUdf.get(j).getFileName());

        }


          /* If No pig Script has been fetched from Hue db according to our search criteria*/
        if (dbpojoPigUdf.size() == 0) {

          logger.info("No queries has been selected for the user " + username);
        } else {

          connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();// connecting to ambari db
          connectionAmbaridb.setAutoCommit(false);

          logger.info("loop will continue for " + dbpojoPigUdf.size() + "times");

          //for each pig udf found in Hue Database

          pigInstanceTableId = pigudfmigration.fetchInstanceTablenamePigUdf(connectionAmbaridb, instance, ambaridatabase);// finding the table name in ambari from the given instance

          sequence = pigudfmigration.fetchSequenceno(connectionAmbaridb, pigInstanceTableId, ambaridatabase);

          for (i = 0; i < dbpojoPigUdf.size(); i++) {


            float calc = ((float) (i + 1)) / dbpojoPigUdf.size() * 100;
            int progressPercentage = Math.round(calc);
            migrationresult.setProgressPercentage(progressPercentage);
            migrationresult.setNumberOfQueryTransfered(i + 1);
            getResourceManager(view).update(migrationresult, jobid);

            logger.info("Loop No." + (i + 1));
            logger.info("________________");
            logger.info("jar name:  " + dbpojoPigUdf.get(i).getFileName());

            maxcountforpigudf = i + sequence + 1;


            String ownerName = dbpojoPigUdf.get(i).getUserName();
            String filePath = dbpojoPigUdf.get(i).getUrl();
            String fileName = dbpojoPigUdf.get(i).getFileName();
            if (usernames[k].equals("all")) {
              username = dbpojoPigUdf.get(i).getUserName();
            }
            dirNameForPigUdf = "/user/" + username + "/pig/udf/";
            String ambariNameNodeUri = view.getProperties().get("namenode_URI_Ambari");
            String dirAndFileName = ambariNameNodeUri + dirNameForPigUdf + fileName;

            if (view.getProperties().get("KerberoseEnabled").equals("y")) {
              pigudfmigration.createDirPigUdfSecured(dirNameForPigUdf, ambariNameNodeUri, ownerName, view.getProperties().get("PrincipalUserName"));
              pigudfmigration.copyFileBetweenHdfsSecured(filePath, dirNameForPigUdf, ambariNameNodeUri, ownerName, view.getProperties().get("PrincipalUserName"));
            } else {
              pigudfmigration.createDirPigUdf(dirNameForPigUdf, ambariNameNodeUri, ownerName);
              pigudfmigration.copyFileBetweenHdfs(filePath, dirNameForPigUdf, ambariNameNodeUri, ownerName);
            }

            pigudfmigration.insertRowForPigUdf(maxcountforpigudf, dirAndFileName, fileName, connectionAmbaridb, pigInstanceTableId, ambaridatabase, ownerName);
            logger.info(dbpojoPigUdf.get(i).getFileName() + "Migrated to Ambari");

          }
          pigudfmigration.updateSequenceno(connectionAmbaridb, maxcountforpigudf, pigInstanceTableId, ambaridatabase);
          connectionAmbaridb.commit();
        }
        logger.info("Migration completed for user " + username);
      }
      migrationresult.setFlag(1);
      if (totalQueries == 0) {
        migrationresult.setNumberOfQueryTransfered(0);
        migrationresult.setTotalNoQuery(0);
      } else {
        migrationresult.setNumberOfQueryTransfered(totalQueries);
        migrationresult.setTotalNoQuery(totalQueries);
        migrationresult.setProgressPercentage(100);
      }
      getResourceManager(view).update(migrationresult, jobid);
    } catch (SQLException e) {
      logger.error("Sql exception in ambari database", e);
      migrationresult.setError("SQL Exception: " + e.getMessage());
      try {
        connectionAmbaridb.rollback();
        logger.info("rollback done");
      } catch (SQLException e1) {
        logger.error("Sql exception while doing roll back", e1);
      }
    } catch (ClassNotFoundException e2) {
      logger.error("class not found exception", e2);
      migrationresult.setError("Class Not Found Exception: " + e2.getMessage());
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: ", e);
      migrationresult.setError("Property Veto Exception: " + e.getMessage());
    } catch (URISyntaxException e) {
      e.printStackTrace();
      migrationresult.setError("URI Syntax Exception: " + e.getMessage());
    } catch (Exception e) {
      logger.error("Generic Exception: ", e);
      migrationresult.setError("Exception: " + e.getMessage());
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("connection close exception: ", e);
          migrationresult.setError("Error in closing connection: " + e.getMessage());
        }
      getResourceManager(view).update(migrationresult, jobid);
    }

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;


    migrationresult.setJobtype("pigudfmigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);


    logger.info("----------------------------------");
    logger.info("pig udf Migration ends");
    logger.info("----------------------------------");

  }

}