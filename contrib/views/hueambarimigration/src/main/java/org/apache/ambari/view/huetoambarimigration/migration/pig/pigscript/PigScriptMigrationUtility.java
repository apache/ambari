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


package org.apache.ambari.view.huetoambarimigration.migration.pig.pigscript;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.PigModel;
import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.savedscriptqueryset.*;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.savedscriptqueryset.*;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.MigrationResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.apache.ambari.view.huetoambarimigration.migration.configuration.ConfigurationCheckImplementation;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;

public class PigScriptMigrationUtility {

  protected MigrationResourceManager resourceManager = null;

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager(ViewContext view) {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }


  public void pigScriptMigration(String username, String instance, String startDate, String endDate, ViewContext view, MigrationResponse migrationresult, String jobid) throws IOException, ItemNotFound {

    long startTime = System.currentTimeMillis();

    final Logger logger = Logger.getLogger(PigScriptMigrationUtility.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;

    logger.info("-------------------------------------");
    logger.info("pig saved script Migration started");
    logger.info("-------------------------------------");


    int i = 0;

    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + instance);
    logger.info("hue username is : " + username);

    //Reading the configuration file
    PigScriptMigrationImplementation pigsavedscriptmigration = new PigScriptMigrationImplementation();

    QuerySetHueDb huedatabase = null;

    if (view.getProperties().get("huedrivername").contains("mysql")) {
      huedatabase = new MysqlQuerySetHueDb();
    } else if (view.getProperties().get("huedrivername").contains("postgresql")) {
      huedatabase = new PostgressQuerySetHueDb();
    } else if (view.getProperties().get("huedrivername").contains("sqlite")) {

      huedatabase = new SqliteQuerySetHueDb();
    } else if (view.getProperties().get("huedrivername").contains("oracle")) {
      huedatabase = new OracleQuerySetHueDb();
    }

    QuerySetAmbariDB ambaridatabase = null;


    if (view.getProperties().get("ambaridrivername").contains("mysql")) {
      ambaridatabase = new MysqlQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("postgresql")) {
      ambaridatabase = new PostgressQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("oracle")) {
      ambaridatabase = new OracleQuerySetAmbariDB();
    }

    int maxcountforsavequery = 0, maxcountforpigsavedscript = 0;
    String time = null, timetobeInorder = null;
    Long epochTime = null;
    String dirNameForPigScript, completeDirandFilePath, pigscriptFilename = "";
    int pigInstanceTableName, sequence;

    ArrayList<PigModel> dbpojoPigSavedscript = new ArrayList<PigModel>();

    try {
      String[] usernames = username.split(",");
      int totalQueries = 0;
      for (int k = 0; k < usernames.length; k++) {
        connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();//connection to Hue DB
        username = usernames[k];
        migrationresult.setProgressPercentage(0);
        logger.info("Migration started for user " + username);

        dbpojoPigSavedscript = pigsavedscriptmigration.fetchFromHueDatabase(username, startDate, endDate, connectionHuedb, huedatabase);// Fetching pig script details from Hue DB
        totalQueries += dbpojoPigSavedscript.size();

        for (int j = 0; j < dbpojoPigSavedscript.size(); j++) {
          logger.info("the query fetched from hue=" + dbpojoPigSavedscript.get(j).getScript());

        }


        /* If No pig Script has been fetched from Hue db according to our search criteria*/
        if (dbpojoPigSavedscript.size() == 0) {

          logger.info("No queries has been selected for the user " + username + " between dates: " + startDate + " - " + endDate);
        } else {

          connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();// connecting to ambari db
          connectionAmbaridb.setAutoCommit(false);

          logger.info("loop will continue for " + dbpojoPigSavedscript.size() + "times");

          //for each pig script found in Hue Database

          pigInstanceTableName = pigsavedscriptmigration.fetchInstanceTablenamePigScript(connectionAmbaridb, instance, ambaridatabase);// finding the table name in ambari from the given instance

          sequence = pigsavedscriptmigration.fetchSequenceno(connectionAmbaridb, pigInstanceTableName, ambaridatabase);

          for (i = 0; i < dbpojoPigSavedscript.size(); i++) {


            float calc = ((float) (i + 1)) / dbpojoPigSavedscript.size() * 100;
            int progressPercentage = Math.round(calc);
            migrationresult.setProgressPercentage(progressPercentage);
            migrationresult.setNumberOfQueryTransfered(i + 1);
            getResourceManager(view).update(migrationresult, jobid);

            logger.info("Loop No." + (i + 1));
            logger.info("________________");
            logger.info("the title of script:  " + dbpojoPigSavedscript.get(i).getTitle());

            time = pigsavedscriptmigration.getTime();

            timetobeInorder = pigsavedscriptmigration.getTimeInorder();

            epochTime = pigsavedscriptmigration.getEpochTime();

            maxcountforpigsavedscript = i + sequence + 1;

            if (usernames[k].equals("all")) {
              username = dbpojoPigSavedscript.get(i).getUserName();
            }

            dirNameForPigScript = "/user/" + username + "/pig/scripts/";

            pigscriptFilename = dbpojoPigSavedscript.get(i).getTitle() + "-" + time + ".pig";

            completeDirandFilePath = dirNameForPigScript + pigscriptFilename;

            pigsavedscriptmigration.writetPigScripttoLocalFile(dbpojoPigSavedscript.get(i).getScript(), dbpojoPigSavedscript.get(i).getTitle(), dbpojoPigSavedscript.get(i).getDt(), ConfigurationCheckImplementation.getHomeDir(), pigscriptFilename);

            pigsavedscriptmigration.insertRowForPigScript(completeDirandFilePath, maxcountforsavequery, maxcountforpigsavedscript, time, timetobeInorder, epochTime, dbpojoPigSavedscript.get(i).getTitle(), connectionAmbaridb, pigInstanceTableName, instance, i, ambaridatabase, username);

            if (view.getProperties().get("KerberoseEnabled").equals("y")) {

              pigsavedscriptmigration.createDirPigScriptSecured(dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));
              pigsavedscriptmigration.putFileinHdfsSecured(ConfigurationCheckImplementation.getHomeDir() + pigscriptFilename, dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));
            } else {

              pigsavedscriptmigration.createDirPigScript(dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"), username);
              pigsavedscriptmigration.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + pigscriptFilename, dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"), username);
            }

            logger.info(dbpojoPigSavedscript.get(i).getTitle() + "Migrated to Ambari");

            pigsavedscriptmigration.deletePigScriptLocalFile(ConfigurationCheckImplementation.getHomeDir(), pigscriptFilename);

          }
          pigsavedscriptmigration.updateSequenceno(connectionAmbaridb, maxcountforpigsavedscript, pigInstanceTableName, ambaridatabase);
          connectionAmbaridb.commit();

        }
        logger.info("Migration completed for user " + username);
      }
      logger.info("Migration Completed");
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
    } catch (ParseException e) {
      logger.error("ParseException: ", e);
      migrationresult.setError("Parse Exception: " + e.getMessage());
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: ", e);
      migrationresult.setError("Property Veto Exception: " + e.getMessage());
    } catch (URISyntaxException e) {
      e.printStackTrace();
      migrationresult.setError("URISyntaxException: " + e.getMessage());
    } catch (Exception e) {
      logger.error("Generic Exception: ", e);
      migrationresult.setError("Exception: " + e.getMessage());
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("connection close exception: ", e);
          migrationresult.setError("Error Closing Connection: " + e.getMessage());
        }
      getResourceManager(view).update(migrationresult, jobid);
    }

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;


    migrationresult.setJobtype("pigsavedscriptmigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);


    logger.info("----------------------------------");
    logger.info("pig saved script Migration ends");
    logger.info("----------------------------------");

  }

}