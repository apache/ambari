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

public class HiveSavedQueryMigrationUtility {



  protected MigrationResourceManager resourceManager = null;

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

    int i = 0;

    logger.info("-------------------------------------");
    logger.info("hive saved query Migration started");
    logger.info("-------------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + instance);
    logger.info("hue username is : " + username);

    HiveSavedQueryMigrationImplementation hivesavedqueryimpl = new HiveSavedQueryMigrationImplementation();/* creating Implementation object  */

    QuerySet huedatabase=null;

    if(view.getProperties().get("huedrivername").contains("mysql"))
    {
      huedatabase=new MysqlQuerySet();
    }
    else if(view.getProperties().get("huedrivername").contains("postgresql"))
    {
      huedatabase=new PostgressQuerySet();
    }
    else if(view.getProperties().get("huedrivername").contains("sqlite"))
    {
     huedatabase=new SqliteQuerySet();
    }
    else if (view.getProperties().get("huedrivername").contains("oracle"))
    {
      huedatabase=new OracleQuerySet();
    }


    QuerySetAmbariDB ambaridatabase=null;


    if(view.getProperties().get("ambaridrivername").contains("mysql"))
    {
      ambaridatabase=new MysqlQuerySetAmbariDB();
    }
    else if(view.getProperties().get("ambaridrivername").contains("postgresql"))
    {
      ambaridatabase=new PostgressQuerySetAmbariDB();
    }
    else if (view.getProperties().get("ambaridrivername").contains("oracle"))
    {
      ambaridatabase= new OracleQuerySetAmbariDB();
    }

    int maxcountForHivehistroryAmbaridb, maxCountforSavequeryAmbaridb;
    String time = null;
    Long epochtime = null;
    String dirNameforHiveSavedquery;
    ArrayList<HiveModel> dbpojoHiveSavedQuery = new ArrayList<HiveModel>();

    try {

      connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection(); /* fetching connection to hue DB */

      dbpojoHiveSavedQuery = hivesavedqueryimpl.fetchFromHuedb(username, startDate, endDate, connectionHuedb,huedatabase); /* fetching data from hue db and storing it in to a model */


      for(int j=0;j<dbpojoHiveSavedQuery.size();j++)
      {
        logger.info("the query fetched from hue"+dbpojoHiveSavedQuery.get(j).getQuery());

      }


      if (dbpojoHiveSavedQuery.size() == 0) /* if no data has been fetched from hue db according to search criteria */ {

        migrationresult.setIsNoQuerySelected("yes");
        migrationresult.setProgressPercentage(0);
        migrationresult.setNumberOfQueryTransfered(0);
        migrationresult.setTotalNoQuery(dbpojoHiveSavedQuery.size());
        getResourceManager(view).update(migrationresult, jobid);
        logger.info("No queries has been selected acccording to your criteria");

        logger.info("no hive saved query has been selected from hue according to your criteria of searching");


      } else {

        connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();/* connecting to ambari DB */
        connectionAmbaridb.setAutoCommit(false);

        for (i = 0; i < dbpojoHiveSavedQuery.size(); i++) {

          logger.info("_____________________");
          logger.info("Loop No." + (i + 1));
          logger.info("_____________________");

          float calc = ((float) (i + 1)) / dbpojoHiveSavedQuery.size() * 100;
          int progressPercentage = Math.round(calc);

          migrationresult.setIsNoQuerySelected("no");
          migrationresult.setProgressPercentage(progressPercentage);
          migrationresult.setNumberOfQueryTransfered(i+1);
          migrationresult.setTotalNoQuery(dbpojoHiveSavedQuery.size());
          getResourceManager(view).update(migrationresult, jobid);




          logger.info("query fetched from hue:-  " + dbpojoHiveSavedQuery.get(i).getQuery());

          int tableIdSavedQuery = hivesavedqueryimpl.fetchInstancetablenameForSavedqueryHive(connectionAmbaridb, instance,ambaridatabase); /* fetching the instance table name for migration saved query  from the given instance name */

          int tableIdHistoryHive = hivesavedqueryimpl.fetchInstanceTablenameHiveHistory(connectionAmbaridb, instance,ambaridatabase); /* fetching the instance table name for migration history query from the given instance name */

          logger.info("Table name are fetched from instance name.");

          hivesavedqueryimpl.writetoFilequeryHql(dbpojoHiveSavedQuery.get(i).getQuery(), ConfigurationCheckImplementation.getHomeDir()); /* writing migration query to a local file*/

          hivesavedqueryimpl.writetoFileLogs(ConfigurationCheckImplementation.getHomeDir());/* writing logs to localfile */

          logger.info(".hql and logs file are saved in temporary directory");

          maxcountForHivehistroryAmbaridb = (hivesavedqueryimpl.fetchMaxdsidFromHiveHistory( connectionAmbaridb, tableIdHistoryHive,ambaridatabase) + 1);/* fetching the maximum ds_id from migration history table*/

          maxCountforSavequeryAmbaridb = (hivesavedqueryimpl.fetchMaxidforSavedQueryHive(connectionAmbaridb, tableIdSavedQuery,ambaridatabase) + 1);/* fetching the maximum ds_id from migration saved query table*/

          time = hivesavedqueryimpl.getTime();/* getting system time */

          epochtime = hivesavedqueryimpl.getEpochTime();/* getting epoch time */

          dirNameforHiveSavedquery = "/user/admin/migration/jobs/migration-job-" + maxcountForHivehistroryAmbaridb + "-"
            + time + "/"; // creating hdfs directory name

          logger.info("Directory will be creted in HDFS" + dirNameforHiveSavedquery);

          hivesavedqueryimpl.insertRowHiveHistory(dirNameforHiveSavedquery,maxcountForHivehistroryAmbaridb,epochtime,connectionAmbaridb,tableIdHistoryHive,instance,i,ambaridatabase);// inserting to migration history table

          logger.info("Row inserted in hive History table.");

          if (view.getProperties().get("KerberoseEnabled").equals("y")) {

            logger.info("Kerberose Enabled");
            hivesavedqueryimpl.createDirHiveSecured(dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// creating directory in hdfs in kerborized cluster
            hivesavedqueryimpl.putFileinHdfsSecured(ConfigurationCheckImplementation.getHomeDir() + "query.hql", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting .hql file in hdfs in kerberoroized cluster
            hivesavedqueryimpl.putFileinHdfsSecured(ConfigurationCheckImplementation.getHomeDir() + "logs", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting logs file in hdfs in kerberoroized cluster

          } else {

            logger.info("Kerberose Not Enabled");
            hivesavedqueryimpl.createDirHive(dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// creating directory in hdfs
            hivesavedqueryimpl.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + "query.hql", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting .hql file in hdfs directory
            hivesavedqueryimpl.putFileinHdfs(ConfigurationCheckImplementation.getHomeDir() + "logs", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting logs file in hdfs
          }

          //inserting into hived saved query table
          //6.
          hivesavedqueryimpl.insertRowinSavedQuery(maxCountforSavequeryAmbaridb, dbpojoHiveSavedQuery.get(i).getDatabase(), dirNameforHiveSavedquery, dbpojoHiveSavedQuery.get(i).getQuery(), dbpojoHiveSavedQuery.get(i).getOwner(), connectionAmbaridb, tableIdSavedQuery, instance, i,ambaridatabase);

        }
        connectionAmbaridb.commit();

      }


    } catch (SQLException e) {

      logger.error("SQL exception: ", e);
      try {
        connectionAmbaridb.rollback();
        logger.info("roll back done");
      } catch (SQLException e1) {
        logger.error("Rollback error: ", e1);

      }
    } catch (ClassNotFoundException e1) {
      logger.error("Class not found : " , e1);
    } catch (ParseException e) {
      logger.error("ParseException: " , e);
    } catch (URISyntaxException e) {
      logger.error("URISyntaxException: " , e);
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException:" , e);
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("Error in connection close", e);
        }
    }


    hivesavedqueryimpl.deleteFileQueryhql(ConfigurationCheckImplementation.getHomeDir());
    hivesavedqueryimpl.deleteFileQueryLogs(ConfigurationCheckImplementation.getHomeDir());

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    MigrationModel model=new MigrationModel();

    migrationresult.setJobtype("hivesavedquerymigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);



    logger.info("-------------------------------");
    logger.info("hive saved query Migration end");
    logger.info("--------------------------------");

    return model;

  }
}




