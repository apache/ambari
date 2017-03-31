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


package org.apache.ambari.view.huetoambarimigration.migration.hive.savedquery;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.HiveModel;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationModel;
import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.savedqueryset.MysqlQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.savedqueryset.OracleQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.savedqueryset.PostgressQuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.savedqueryset.QuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.hive.savedqueryset.*;
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
import java.util.HashSet;

public class HiveSavedQueryMigrationUtility {


  protected MigrationResourceManager resourceManager = null;
  private static final String SAVEDQUERYTABLE = "ds_savedquery";
  private static final String FILETABLE = "ds_fileresourceitem";
  private static final String UDFTABLE = "ds_udf";
  private static final String SEQ = "id_seq";
  private static final String SAVEDQUERYSEQUENCE = "org.apache.ambari.view.%hive%.resources.savedQueries.SavedQuery";
  private static final String FILERESOURCESEQUENCE = "org.apache.ambari.view.%hive%.resources.resources.FileResourceItem";
  private static final String UDFSEQUENCE = "org.apache.ambari.view.%hive%.resources.udfs.UDF";

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager(ViewContext view) {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }

  public MigrationModel hiveSavedQueryMigration(String username, String instance, String startDate, String endDate, ViewContext view, MigrationResponse migrationresult, String jobid) throws IOException, ItemNotFound {

    long startTime = System.currentTimeMillis();

    final Logger logger = Logger.getLogger(HiveSavedQueryMigrationUtility.class);

    Connection connectionAmbaridb = null;
    Connection connectionHuedb = null;

    int i = 0, j = 0;
    String sequenceName = "";

    logger.info("-------------------------------------");
    logger.info("hive saved query Migration started");
    logger.info("-------------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + instance);
    logger.info("hue username is : " + username);

    HiveSavedQueryMigrationImplementation hivesavedqueryimpl = new HiveSavedQueryMigrationImplementation();/* creating Implementation object  */

    QuerySetHueDb huedatabase = null;

    if (view.getProperties().get("huedrivername").contains("mysql")) {
      huedatabase = new MysqlQuerySetHueDb();
      logger.info("Hue database is MySQL");
    } else if (view.getProperties().get("huedrivername").contains("postgresql")) {
      huedatabase = new PostgressQuerySetHueDb();
      logger.info("Hue database is Postgres");
    } else if (view.getProperties().get("huedrivername").contains("sqlite")) {
      huedatabase = new SqliteQuerySetHueDb();
      logger.info("Hue database is SQLite");
    } else if (view.getProperties().get("huedrivername").contains("oracle")) {
      huedatabase = new OracleQuerySetHueDb();
      logger.info("Hue database is Oracle");
    }


    QuerySetAmbariDB ambaridatabase = null;


    if (view.getProperties().get("ambaridrivername").contains("mysql")) {
      ambaridatabase = new MysqlQuerySetAmbariDB();
      logger.info("Ambari database is MySQL");
    } else if (view.getProperties().get("ambaridrivername").contains("postgresql")) {
      ambaridatabase = new PostgressQuerySetAmbariDB();
      logger.info("Ambari database is PostGres");
    } else if (view.getProperties().get("ambaridrivername").contains("oracle")) {
      ambaridatabase = new OracleQuerySetAmbariDB();
      logger.info("Ambari database is Oracle");
    }

    int maxCountforFileResourceAmbaridb = 0, maxCountforUdfAmbaridb = 0, maxCountforSavequeryAmbaridb = 0;
    String time = null;
    Long epochtime = null;
    String dirNameforHiveSavedquery;
    ArrayList<HiveModel> dbpojoHiveSavedQuery = new ArrayList<HiveModel>();
    HashSet<String> udfSet = new HashSet<>();


    try {
      String[] usernames = username.split(",");
      int totalQueries = 0;
      for (int l = 0; l < usernames.length; l++) {
        connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection(); /* fetching connection to hue DB */
        logger.info("Hue database connection successful");

        username = usernames[l];
        migrationresult.setProgressPercentage(0);
        dbpojoHiveSavedQuery = hivesavedqueryimpl.fetchFromHuedb(username, startDate, endDate, connectionHuedb, huedatabase); /* fetching data from hue db and storing it in to a model */
        totalQueries += dbpojoHiveSavedQuery.size();

        logger.info("Migration started for user " + username);
        logger.info("Queries fetched from hue..");

        for (i = 0; i < dbpojoHiveSavedQuery.size(); i++) {
          logger.info("the query fetched from hue" + dbpojoHiveSavedQuery.get(i).getQuery());

        }


        if (dbpojoHiveSavedQuery.size() == 0) /* if no data has been fetched from hue db according to search criteria */ {

          logger.info("No queries has been selected for the user " + username + " between dates: " + startDate + " - " + endDate);
        } else {

          connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();/* connecting to ambari DB */
          connectionAmbaridb.setAutoCommit(false);

          int tableIdSavedQuery = hivesavedqueryimpl.fetchInstancetablename(connectionAmbaridb, instance, ambaridatabase, SAVEDQUERYSEQUENCE); /* fetching the instance table name for migration saved query  from the given instance name */
          int tableIdFileResource = hivesavedqueryimpl.fetchInstancetablename(connectionAmbaridb, instance, ambaridatabase, FILERESOURCESEQUENCE);
          int tableIdUdf = hivesavedqueryimpl.fetchInstancetablename(connectionAmbaridb, instance, ambaridatabase, UDFSEQUENCE);
          sequenceName = SAVEDQUERYTABLE + "_" + tableIdSavedQuery + "_" + SEQ;
          int savedQuerySequence = hivesavedqueryimpl.fetchSequenceno(connectionAmbaridb, ambaridatabase, sequenceName);
          sequenceName = FILETABLE + "_" + tableIdFileResource + "_" + SEQ;
          int fileResourceSequence = hivesavedqueryimpl.fetchSequenceno(connectionAmbaridb, ambaridatabase, sequenceName);
          sequenceName = UDFTABLE + "_" + tableIdUdf + "_" + SEQ;
          int udfSequence = hivesavedqueryimpl.fetchSequenceno(connectionAmbaridb, ambaridatabase, sequenceName);

          for (i = 0; i < dbpojoHiveSavedQuery.size(); i++) {

            logger.info("_____________________");
            logger.info("Loop No." + (i + 1));
            logger.info("_____________________");

            float calc = ((float) (i + 1)) / dbpojoHiveSavedQuery.size() * 100;
            int progressPercentage = Math.round(calc);

            migrationresult.setProgressPercentage(progressPercentage);
            migrationresult.setNumberOfQueryTransfered(i + 1);
            getResourceManager(view).update(migrationresult, jobid);

            logger.info("query fetched from hue:-  " + dbpojoHiveSavedQuery.get(i).getQuery());

            logger.info("Table name are fetched from instance name.");

            hivesavedqueryimpl.writetoFilequeryHql(dbpojoHiveSavedQuery.get(i).getQuery(), ConfigurationCheckImplementation.getHomeDir()); /* writing migration query to a local file*/

            hivesavedqueryimpl.writetoFileLogs(ConfigurationCheckImplementation.getHomeDir());/* writing logs to localfile */

            logger.info(".hql and logs file are saved in temporary directory");

            maxCountforSavequeryAmbaridb = i + savedQuerySequence + 1;

            time = hivesavedqueryimpl.getTime();/* getting system time */

            if (usernames[l].equals("all")) {
              username = dbpojoHiveSavedQuery.get(i).getOwnerName();
            }

            dirNameforHiveSavedquery = "/user/" + username + "/hive/scripts/hive-query-" + maxCountforSavequeryAmbaridb + "-"
                    + time + "/"; // creating hdfs directory name

            logger.info("Directory will be creted in HDFS" + dirNameforHiveSavedquery);

            logger.info("Row inserted in hive History table.");

            if (view.getProperties().get("KerberoseEnabled").equals("y")) {

              logger.info("Kerberose Enabled");
              hivesavedqueryimpl.createDirHiveSecured(dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));// creating directory in hdfs in kerborized cluster
              hivesavedqueryimpl.putFileinHdfsSecured(ConfigurationCheckImplementation.getHomeDir() + "query.hql", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));// putting .hql file in hdfs in kerberoroized cluster
              hivesavedqueryimpl.putFileinHdfsSecured(ConfigurationCheckImplementation.getHomeDir() + "logs", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));// putting logs file in hdfs in kerberoroized cluster

            } else {
              logger.info("Kerberose Not Enabled");
              hivesavedqueryimpl.createDirHive(dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"), username);// creating directory in hdfs
              hivesavedqueryimpl.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + "query.hql", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"), username);// putting .hql file in hdfs directory
              hivesavedqueryimpl.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + "logs", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"), username);// putting logs file in hdfs
            }

            //inserting into hived saved query table
            //6.
            hivesavedqueryimpl.insertRowinSavedQuery(maxCountforSavequeryAmbaridb, dbpojoHiveSavedQuery.get(i).getDatabase(), dirNameforHiveSavedquery, dbpojoHiveSavedQuery.get(i).getQuery(), dbpojoHiveSavedQuery.get(i).getQueryTitle(), connectionAmbaridb, tableIdSavedQuery, instance, i, ambaridatabase, username);
            //check if udfs needs to be migrated
            if (dbpojoHiveSavedQuery.get(i).getFilePaths() != null) {
              for (int k = 0; k < dbpojoHiveSavedQuery.get(i).getFilePaths().size(); k++) {
                String filePath = dbpojoHiveSavedQuery.get(i).getFilePaths().get(k);
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length());
                //check of a udf is alread present (udf name and owner name should be the same)
                if (!hivesavedqueryimpl.checkUdfExists(connectionAmbaridb, fileName, username, tableIdFileResource, ambaridatabase, udfSet)) {
                  udfSet.add(fileName + username);
                  maxCountforFileResourceAmbaridb = j + fileResourceSequence + 1;
                  maxCountforUdfAmbaridb = j + udfSequence + 1;
                  String absoluteFilePath = view.getProperties().get("namenode_URI_Ambari") + filePath;
                  hivesavedqueryimpl.insertUdf(connectionAmbaridb, tableIdFileResource, tableIdUdf, maxCountforFileResourceAmbaridb, maxCountforUdfAmbaridb, dbpojoHiveSavedQuery.get(i).getUdfClasses().get(k), fileName, dbpojoHiveSavedQuery.get(i).getUdfNames().get(k), username, absoluteFilePath, ambaridatabase);
                  j = j + 1;
                }
              }
            }


          }
          sequenceName = SAVEDQUERYTABLE + "_" + tableIdSavedQuery + "_" + SEQ;
          hivesavedqueryimpl.updateSequenceno(connectionAmbaridb, maxCountforSavequeryAmbaridb, sequenceName, ambaridatabase);
          sequenceName = FILETABLE + "_" + tableIdFileResource + "_" + SEQ;
          hivesavedqueryimpl.updateSequenceno(connectionAmbaridb, maxCountforFileResourceAmbaridb, sequenceName, ambaridatabase);
          sequenceName = UDFTABLE + "_" + tableIdUdf + "_" + SEQ;
          hivesavedqueryimpl.updateSequenceno(connectionAmbaridb, maxCountforUdfAmbaridb, sequenceName, ambaridatabase);
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
      logger.error("SQL exception: ", e);
      migrationresult.setError("SQL Exception: " + e.getMessage());
      try {
        connectionAmbaridb.rollback();
        logger.info("roll back done");
      } catch (SQLException e1) {
        logger.error("Rollback error: ", e1);

      }
    } catch (ClassNotFoundException e1) {
      logger.error("Class not found : ", e1);
      migrationresult.setError("Class not found Exception: " + e1.getMessage());
    } catch (ParseException e) {
      logger.error("ParseException: ", e);
      migrationresult.setError("Parse Exception: " + e.getMessage());
    } catch (URISyntaxException e) {
      logger.error("URISyntaxException: ", e);
      migrationresult.setError("URI Syntax Exception: " + e.getMessage());
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException:", e);
      migrationresult.setError("Property Veto Exception: " + e.getMessage());
    } catch (Exception e) {
      logger.error("Generic Exception: ", e);
      migrationresult.setError("Exception: " + e.getMessage());
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("Error in connection close", e);
          migrationresult.setError("Error in closing connection: " + e.getMessage());
        }
      getResourceManager(view).update(migrationresult, jobid);
    }


    hivesavedqueryimpl.deleteFileQueryhql(ConfigurationCheckImplementation.getHomeDir());
    hivesavedqueryimpl.deleteFileQueryLogs(ConfigurationCheckImplementation.getHomeDir());

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    MigrationModel model = new MigrationModel();

    migrationresult.setJobtype("hivesavedquerymigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);


    logger.info("-------------------------------");
    logger.info("hive saved query Migration end");
    logger.info("--------------------------------");

    return model;

  }
}