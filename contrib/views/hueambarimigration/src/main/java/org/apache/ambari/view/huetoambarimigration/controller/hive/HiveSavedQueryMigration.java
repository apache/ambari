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


package org.apache.ambari.view.huetoambarimigration.controller.hive;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.controller.configurationcheck.ProgressBarStatus;
import org.apache.log4j.Logger;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.model.*;
import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;
import org.apache.ambari.view.huetoambarimigration.service.hive.HiveSavedQueryImpl;

public class HiveSavedQueryMigration extends HttpServlet {

  private static final long serialVersionUID = 1031422249396784970L;

  ViewContext view;
  private String startDate;
  private String endDate;
  private String instance;
  private String userName;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    ServletContext context = config.getServletContext();
    view = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    final Logger logger = Logger.getLogger(HiveSavedQueryMigration.class);

    Connection connectionAmbaridb = null;
    Connection connectionHuedb = null;

    /* fetching from servlet */
    userName = req.getParameter("username");
    startDate = req.getParameter("startdate");
    endDate = req.getParameter("enddate");
    instance = req.getParameter("instance");

    int i = 0;

    logger.info("-------------------------------------");
    logger.info("Hive saved query Migration started");
    logger.info("-------------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + instance);
    logger.info("hue username is : " + userName);

    HiveSavedQueryImpl hivesavedqueryimpl = new HiveSavedQueryImpl();/* creating Implementation object  */

    int maxcountForHivehistroryAmbaridb, maxCountforSavequeryAmbaridb;
    String time = null;
    Long epochtime = null;
    String dirNameforHiveSavedquery;
    ArrayList<PojoHive> dbpojoHiveSavedQuery = new ArrayList<PojoHive>();

    try {

      connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection(); /* fetching connection to hue DB */

      dbpojoHiveSavedQuery = hivesavedqueryimpl.fetchFromHuedb(userName, startDate, endDate, connectionHuedb); /* fetching data from hue db and storing it in to a model */

      if (dbpojoHiveSavedQuery.size() == 0) /* if no data has been fetched from hue db according to search criteria */ {

        logger.info("no Hive saved query has been selected from hue according to your criteria of searching");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<br>");
        out.println("<h4>No queries selected according to your criteria</h4>");

      } else {

        connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();/* connecting to ambari DB */
        connectionAmbaridb.setAutoCommit(false);

        for (i = 0; i < dbpojoHiveSavedQuery.size(); i++) {

          logger.info("_____________________");
          logger.info("Loop No." + (i + 1));
          logger.info("_____________________");

          float calc = ((float) (i + 1)) / dbpojoHiveSavedQuery.size() * 100;
          int progressPercentage = Math.round(calc);

          session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, progressPercentage);

          logger.info("query fetched from hue:-  " + dbpojoHiveSavedQuery.get(i).getQuery());

          int tableIdSavedQuery = hivesavedqueryimpl.fetchInstancetablenameForSavedqueryHive(view.getProperties().get("ambaridrivername"), connectionAmbaridb, instance); /* fetching the instance table name for hive saved query  from the given instance name */

          int tableIdHistoryHive = hivesavedqueryimpl.fetchInstanceTablenameHiveHistory(view.getProperties().get("ambaridrivername"), connectionAmbaridb, instance); /* fetching the instance table name for hive history query from the given instance name */

          logger.info("Table name are fetched from instance name.");

          hivesavedqueryimpl.writetoFilequeryHql(dbpojoHiveSavedQuery.get(i).getQuery(), ConfFileReader.getHomeDir()); /* writing hive query to a local file*/

          hivesavedqueryimpl.writetoFileLogs(ConfFileReader.getHomeDir());/* writing logs to localfile */

          logger.info(".hql and logs file are saved in temporary directory");

          maxcountForHivehistroryAmbaridb = (hivesavedqueryimpl.fetchMaxdsidFromHiveHistory(view.getProperties().get("ambaridrivername"), connectionAmbaridb, tableIdHistoryHive) + 1);/* fetching the maximum ds_id from hive history table*/

          maxCountforSavequeryAmbaridb = (hivesavedqueryimpl.fetchMaxidforSavedQueryHive(view.getProperties().get("ambaridrivername"), connectionAmbaridb, tableIdSavedQuery) + 1);/* fetching the maximum ds_id from hive saved query table*/

          time = hivesavedqueryimpl.getTime();/* getting system time */

          epochtime = hivesavedqueryimpl.getEpochTime();/* getting epoch time */


          dirNameforHiveSavedquery = "/user/admin/hive/jobs/hive-job-" + maxcountForHivehistroryAmbaridb + "-"
            + time + "/"; // creating hdfs directory name

          logger.info("Directory will be creted in HDFS" + dirNameforHiveSavedquery);


          hivesavedqueryimpl.insertRowHiveHistory(view.getProperties().get("ambaridrivername"), dirNameforHiveSavedquery, maxcountForHivehistroryAmbaridb, epochtime, connectionAmbaridb, tableIdHistoryHive, instance, i);// inserting to hive history table

          logger.info("Row inserted in Hive History table.");

          if (view.getProperties().get("KerberoseEnabled").equals("y")) {

            logger.info("Kerberose Enabled");
            hivesavedqueryimpl.createDirHiveSecured(dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// creating directory in hdfs in kerborized cluster
            hivesavedqueryimpl.putFileinHdfsSecured(ConfFileReader.getHomeDir() + "query.hql", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting .hql file in hdfs in kerberoroized cluster
            hivesavedqueryimpl.putFileinHdfsSecured(ConfFileReader.getHomeDir() + "logs", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting logs file in hdfs in kerberoroized cluster

          } else {

            logger.info("Kerberose Not Enabled");
            hivesavedqueryimpl.createDirHive(dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// creating directory in hdfs
            hivesavedqueryimpl.putFileinHdfs(ConfFileReader.getHomeDir() + "query.hql", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting .hql file in hdfs directory
            hivesavedqueryimpl.putFileinHdfs(ConfFileReader.getHomeDir() + "logs", dirNameforHiveSavedquery, view.getProperties().get("namenode_URI_Ambari"));// putting logs file in hdfs
          }

          //inserting into hived saved query table
          hivesavedqueryimpl.insertRowinSavedQuery(view.getProperties().get("ambaridrivername"), maxCountforSavequeryAmbaridb, dbpojoHiveSavedQuery.get(i).getDatabase(), dirNameforHiveSavedquery, dbpojoHiveSavedQuery.get(i).getQuery(), dbpojoHiveSavedQuery.get(i).getOwner(), connectionAmbaridb, tableIdSavedQuery, instance, i);

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


    hivesavedqueryimpl.deleteFileQueryhql(ConfFileReader.getHomeDir());
    hivesavedqueryimpl.deleteFileQueryLogs(ConfFileReader.getHomeDir());
    session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, 0);

    logger.info("-------------------------------");
    logger.info("Hive saved query Migration end");
    logger.info("--------------------------------");

    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    out.println("<br>");
    out.println("<h4>" + i + " Saved query has been migrated to  " + instance + "</h4>");
  }
}




