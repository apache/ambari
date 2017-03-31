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


package org.apache.ambari.view.huetoambarimigration.migration.hive.historyquery;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.migration.InitiateJobMigration;
import org.apache.ambari.view.huetoambarimigration.migration.configuration.ConfigurationCheckImplementation;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.HiveModel;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationModel;
import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.historyqueryset.MysqlQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.historyqueryset.OracleQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.historyqueryset.PostgressQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.historyqueryset.QuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.hive.historyqueryset.*;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.MigrationResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;


public class HiveHistoryMigrationUtility {


  protected MigrationResourceManager resourceManager = null;

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager(ViewContext view) {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }


  public void hiveHistoryQueryMigration(String username, String instance, String startDate, String endDate, ViewContext view, MigrationResponse migrationresult, String jobid) throws IOException, ItemNotFound {

    InitiateJobMigration migrationservice = new InitiateJobMigration();

    long startTime = System.currentTimeMillis();

    final Logger logger = Logger.getLogger(HiveHistoryMigrationUtility.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;


    logger.info(System.getProperty("java.class.path"));

    logger.info("--------------------------------------");
    logger.info("hive History query Migration started");
    logger.info("--------------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + username);
    logger.info("hue username is : " + instance);

    MigrationModel model = new MigrationModel();

    int maxCountOfAmbariDb = 0, i = 0, sequence;
    String time = null;
    Long epochTime = null;
    String dirNameforHiveHistroy;
    ArrayList<HiveModel> dbpojoHiveHistoryQuery;

    HiveHistoryQueryMigrationImplementation hiveHistoryQueryImpl = new HiveHistoryQueryMigrationImplementation();// creating objects of HiveHistroy implementation

    QuerySetHueDB huedatabase = null;

    /*instanciang queryset
    * according to driver name
    */

    if (view.getProperties().get("huedrivername").contains("mysql")) {
      huedatabase = new MysqlQuerySetHueDB();
    } else if (view.getProperties().get("huedrivername").contains("postgresql")) {
      huedatabase = new PostgressQuerySetHueDB();
    } else if (view.getProperties().get("huedrivername").contains("sqlite")) {
      huedatabase = new SqliteQuerySetHueDB();
    } else if (view.getProperties().get("huedrivername").contains("oracle")) {
      huedatabase = new OracleQuerySetHueDB();
    }


    QuerySetAmbariDB ambaridatabase = null;


    if (view.getProperties().get("ambaridrivername").contains("mysql")) {
      ambaridatabase = new MysqlQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("postgresql")) {
      ambaridatabase = new PostgressQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("oracle")) {
      ambaridatabase = new OracleQuerySetAmbariDB();
    }


    try {
      String[] usernames = username.split(",");
      int totalQueries = 0;
      for (int k = 0; k < usernames.length; k++) {
        connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();
        username = usernames[k];
        migrationresult.setProgressPercentage(0);
        logger.info("Migration started for user " + username);
        dbpojoHiveHistoryQuery = hiveHistoryQueryImpl.fetchFromHue(username, startDate, endDate, connectionHuedb, huedatabase);
        totalQueries += dbpojoHiveHistoryQuery.size();

        for (int j = 0; j < dbpojoHiveHistoryQuery.size(); j++) {
          logger.info("the query fetched from hue" + dbpojoHiveHistoryQuery.get(j).getQuery());

        }

             /* if No migration query selected from Hue Database according to our search criteria */

        if (dbpojoHiveHistoryQuery.size() == 0) {
          logger.info("No queries has been selected for the user " + username + " between dates: " + startDate + " - " + endDate);

        } else {
          /* If hive queries are selected based on our search criteria */

          connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();// connecting to ambari db
          connectionAmbaridb.setAutoCommit(false);

          // for each queries fetched from Hue database//

          //
          int id = 0;

          id = hiveHistoryQueryImpl.fetchInstanceTablename(connectionAmbaridb, instance, ambaridatabase); // feching table name according to the given instance name
          sequence = hiveHistoryQueryImpl.fetchSequenceno(connectionAmbaridb, id, ambaridatabase);
          //
          for (i = 0; i < dbpojoHiveHistoryQuery.size(); i++) {

            float calc = ((float) (i + 1)) / dbpojoHiveHistoryQuery.size() * 100;
            int progressPercentage = Math.round(calc);
            migrationresult.setProgressPercentage(progressPercentage);
            migrationresult.setNumberOfQueryTransfered(i + 1);
            getResourceManager(view).update(migrationresult, jobid);

            logger.info("_____________________");
            logger.info("Loop No." + (i + 1));
            logger.info("_____________________");
            logger.info("Hue query that has been fetched" + dbpojoHiveHistoryQuery.get(i).getQuery());

            logger.info("Table name has been fetched from intance name");

            hiveHistoryQueryImpl.writetoFileQueryhql(dbpojoHiveHistoryQuery.get(i).getQuery(), ConfigurationCheckImplementation.getHomeDir());// writing to .hql file to a temp file on local disk

            logger.info(".hql file created in Temp directory");

            hiveHistoryQueryImpl.writetoFileLogs(ConfigurationCheckImplementation.getHomeDir());// writing to logs file to a temp file on local disk

            logger.info("Log file created in Temp directory");

            maxCountOfAmbariDb = i + sequence + 1;

            time = hiveHistoryQueryImpl.getTime();// getting the system current time.

            epochTime = hiveHistoryQueryImpl.getEpochTime();// getting system time as epoch format

            if (usernames[k].equals("all")) {
              username = dbpojoHiveHistoryQuery.get(i).getOwnerName();
            }

            dirNameforHiveHistroy = "/user/" + username + "/hive/jobs/hive-job-" + maxCountOfAmbariDb + "-" + time + "/";// creating the directory name

            logger.info("Directory name where .hql will be saved: " + dirNameforHiveHistroy);

            String versionName = hiveHistoryQueryImpl.getAllHiveVersionInstance(connectionAmbaridb, ambaridatabase, instance);

            hiveHistoryQueryImpl.insertRowinAmbaridb(dirNameforHiveHistroy, maxCountOfAmbariDb, epochTime, connectionAmbaridb, id, instance, i, ambaridatabase, versionName, username, dbpojoHiveHistoryQuery.get(i).getJobStatus());// inserting in ambari database

            if (view.getProperties().get("KerberoseEnabled").equals("y")) {

              logger.info("kerberose enabled");
              hiveHistoryQueryImpl.createDirKerberorisedSecured(dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));// creating directory in kerborized secured hdfs
              logger.info("Directory created in hdfs");
              hiveHistoryQueryImpl.putFileinHdfsKerborizedSecured(ConfigurationCheckImplementation.getHomeDir() + "query.hql", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));// copying the .hql file to kerborized hdfs
              hiveHistoryQueryImpl.putFileinHdfsKerborizedSecured(ConfigurationCheckImplementation.getHomeDir() + "logs", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));// copying the log file to kerborized hdfs
            } else {

              logger.info("kerberose not enabled");
              hiveHistoryQueryImpl.createDir(dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"), username);// creating directory in hdfs
              logger.info("Directory created in hdfs");
              hiveHistoryQueryImpl.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + "query.hql", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"), username);// copying the .hql file to hdfs
              hiveHistoryQueryImpl.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + "logs", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"), username);// copying the log file to hdfs
            }

          }
          hiveHistoryQueryImpl.updateSequenceno(connectionAmbaridb, maxCountOfAmbariDb, id, ambaridatabase);
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
      logger.error("Sql exception in ambari database: ", e);
      migrationresult.setError("SQL Exception: " + e.getMessage());
      try {
        connectionAmbaridb.rollback();
        model.setIfSuccess(false);
        logger.error("Sql statement are Rolledback");
      } catch (SQLException e1) {
        logger.error("Sql rollback exception in ambari database",
                e1);
      }
    } catch (ClassNotFoundException e) {
      logger.error("Class not found :- ", e);
      migrationresult.setError("Class Not Found: " + e.getMessage());
    } catch (ParseException e) {
      logger.error("Parse Exception : ", e);
      migrationresult.setError("Parse Exception: " + e.getMessage());
    } catch (URISyntaxException e) {
      logger.error("URI Syntax Exception: ", e);
      migrationresult.setError("URI Syntax Exception: " + e.getMessage());
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: ", e);
      migrationresult.setError("Property Veto Exception: " + e.getMessage());
    } catch (ItemNotFound itemNotFound) {
      itemNotFound.printStackTrace();
      migrationresult.setError("Item Not Found: " + itemNotFound.getMessage());
    } catch (Exception e) {
      logger.error("Generic Exception: ", e);
      migrationresult.setError("Exception: " + e.getMessage());
    } finally {
      if (connectionAmbaridb != null) try {
        connectionAmbaridb.close();
      } catch (SQLException e) {
        logger.error("Exception in closing the connection :", e);
        migrationresult.setError("Exception in closing the connection: " + e.getMessage());
      }
      getResourceManager(view).update(migrationresult, jobid);
    }
    //deleteing the temprary files that are created while execution
    hiveHistoryQueryImpl.deleteFileQueryhql(ConfigurationCheckImplementation.getHomeDir());
    hiveHistoryQueryImpl.deleteFileQueryLogs(ConfigurationCheckImplementation.getHomeDir());

    //session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, 0);
    logger.info("------------------------------");
    logger.info("hive History query Migration Ends");
    logger.info("------------------------------");

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    migrationresult.setJobtype("hivehistoryquerymigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);


  }

}