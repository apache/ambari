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
    logger.info("instance is: " + username);
    logger.info("hue username is : " + instance);

    //Reading the configuration file
    PigScriptMigrationImplementation pigsavedscriptmigration = new PigScriptMigrationImplementation();

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

    int maxcountforsavequery = 0, maxcountforpigsavedscript;
    String time = null, timetobeInorder = null;
    Long epochTime = null;
    String dirNameForPigScript, completeDirandFilePath, pigscriptFilename = "";
    int pigInstanceTableName;

    ArrayList<PigModel> dbpojoPigSavedscript = new ArrayList<PigModel>();

    try {
      connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();//connection to Hue DB
      dbpojoPigSavedscript = pigsavedscriptmigration.fetchFromHueDatabase(username, startDate, endDate, connectionHuedb, huedatabase);// Fetching pig script details from Hue DB

      for (int j = 0; j < dbpojoPigSavedscript.size(); j++) {
        logger.info("the query fetched from hue=" + dbpojoPigSavedscript.get(j).getScript());

      }


      /* If No pig Script has been fetched from Hue db according to our search criteria*/
      if (dbpojoPigSavedscript.size() == 0) {

        migrationresult.setIsNoQuerySelected("yes");
        migrationresult.setProgressPercentage(0);
        migrationresult.setNumberOfQueryTransfered(0);
        migrationresult.setTotalNoQuery(dbpojoPigSavedscript.size());
        getResourceManager(view).update(migrationresult, jobid);

        logger.info("no pig script has been selected from hue according to your criteria of searching");


      } else {

        connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();// connecting to ambari db
        connectionAmbaridb.setAutoCommit(false);
        logger.info("loop will continue for " + dbpojoPigSavedscript.size() + "times");

        //for each pig script found in Hue Database

        for (i = 0; i < dbpojoPigSavedscript.size(); i++) {


          float calc = ((float) (i + 1)) / dbpojoPigSavedscript.size() * 100;
          int progressPercentage = Math.round(calc);
          migrationresult.setIsNoQuerySelected("no");
          migrationresult.setProgressPercentage(progressPercentage);
          migrationresult.setNumberOfQueryTransfered(i + 1);
          migrationresult.setTotalNoQuery(dbpojoPigSavedscript.size());
          getResourceManager(view).update(migrationresult, jobid);

          logger.info("Loop No." + (i + 1));
          logger.info("________________");
          logger.info("the title of script:  " + dbpojoPigSavedscript.get(i).getTitle());

          pigInstanceTableName = pigsavedscriptmigration.fetchInstanceTablenamePigScript(connectionAmbaridb, instance, ambaridatabase);// finding the table name in ambari from the given instance

          maxcountforpigsavedscript = (pigsavedscriptmigration.fetchmaxIdforPigSavedScript(connectionAmbaridb, pigInstanceTableName, ambaridatabase) + 1);// maximum count of the primary key of pig Script table

          time = pigsavedscriptmigration.getTime();

          timetobeInorder = pigsavedscriptmigration.getTimeInorder();

          epochTime = pigsavedscriptmigration.getEpochTime();

          dirNameForPigScript = "/user/admin/pig/scripts/";

          pigscriptFilename = dbpojoPigSavedscript.get(i).getTitle() + "-" + time + ".pig";

          completeDirandFilePath = dirNameForPigScript + pigscriptFilename;

          pigsavedscriptmigration.writetPigScripttoLocalFile(dbpojoPigSavedscript.get(i).getScript(), dbpojoPigSavedscript.get(i).getTitle(), dbpojoPigSavedscript.get(i).getDt(), ConfigurationCheckImplementation.getHomeDir(), pigscriptFilename);

          pigsavedscriptmigration.insertRowForPigScript(completeDirandFilePath, maxcountforsavequery, maxcountforpigsavedscript, time, timetobeInorder, epochTime, dbpojoPigSavedscript.get(i).getTitle(), connectionAmbaridb, pigInstanceTableName, instance, i, ambaridatabase);

          if (view.getProperties().get("KerberoseEnabled").equals("y")) {
            pigsavedscriptmigration.putFileinHdfsSecured(ConfigurationCheckImplementation.getHomeDir() + pigscriptFilename, dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"));
          } else {
            pigsavedscriptmigration.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + pigscriptFilename, dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"));
          }

          logger.info(dbpojoPigSavedscript.get(i).getTitle() + "Migrated to Ambari");

          pigsavedscriptmigration.deletePigScriptLocalFile(ConfigurationCheckImplementation.getHomeDir(), pigscriptFilename);

        }
        connectionAmbaridb.commit();

      }


    } catch (SQLException e) {
      logger.error("Sql exception in ambari database", e);
      try {
        connectionAmbaridb.rollback();
        logger.info("rollback done");
      } catch (SQLException e1) {
        logger.error("Sql exception while doing roll back", e);
      }
    } catch (ClassNotFoundException e2) {
      logger.error("class not found exception", e2);
    } catch (ParseException e) {
      logger.error("ParseException: ", e);
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: ", e);
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("connection close exception: ", e);
        }
    }

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;


    migrationresult.setJobtype("hivehistoryquerymigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);


    logger.info("----------------------------------");
    logger.info("pig saved script Migration ends");
    logger.info("----------------------------------");

  }


}
