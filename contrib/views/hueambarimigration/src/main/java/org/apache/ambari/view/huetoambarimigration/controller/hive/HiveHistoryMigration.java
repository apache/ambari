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


import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.controller.configurationcheck.ConfigurationCheck;
import org.apache.ambari.view.huetoambarimigration.controller.configurationcheck.ProgressBarStatus;
import org.apache.log4j.Logger;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;
import org.apache.ambari.view.huetoambarimigration.service.hive.HiveHistoryQueryImpl;

public class HiveHistoryMigration extends HttpServlet {


  private static final long serialVersionUID = 1031422249396784970L;
  ViewContext view;

  private String startDate;
  private String endDate;
  private String instance;
  private String username;

  @Override
  public void init(ServletConfig config) throws ServletException {

    super.init(config);
    ServletContext context = config.getServletContext();
    view = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    final Logger logger = Logger.getLogger(HiveHistoryMigration.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;

    /* fetching the variable from the client */
    username = req.getParameter("username");
    startDate = req.getParameter("startdate");
    endDate = req.getParameter("enddate");
    instance = req.getParameter("instance");

    logger.info("--------------------------------------");
    logger.info("Hive History query Migration started");
    logger.info("--------------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + username);
    logger.info("hue username is : " + instance);

    int maxCountOfAmbariDb, i = 0;
    String time = null;
    Long epochTime = null;
    String dirNameforHiveHistroy;

    HiveHistoryQueryImpl hiveHistoryQueryImpl = new HiveHistoryQueryImpl();// creating objects of HiveHistroy implementation

    String[] hiveQuery = new String[1000000];

    try {

      connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();

      hiveQuery = hiveHistoryQueryImpl.fetchFromHue(username, startDate, endDate, connectionHuedb);

		   /* if No hive query selected from Hue Database according to our search criteria */

      if (hiveQuery[i] == null) {

        logger.info("No queries has been selected acccording to your criteria");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<br>");
        out.println("<h4>No queries selected according to your criteria</h4>");

      } else {
        /* If Hive queries are selected based on our search criteria */

        connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();// connecting to ambari db
        connectionAmbaridb.setAutoCommit(false);

        // for each queries fetched from Hue database//

        for (i = 0; hiveQuery[i] != null; i++) {

          float calc = ((float) (i + 1)) / hiveQuery.length * 100;
          int progressPercentage = Math.round(calc);

          session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, progressPercentage);

          logger.info("_____________________");
          logger.info("Loop No." + (i + 1));
          logger.info("_____________________");
          logger.info("Hue query that has been fetched" + hiveQuery[i]);
          int id = 0;

          id = hiveHistoryQueryImpl.fetchInstanceTablename(view.getProperties().get("ambaridrivername"), connectionAmbaridb, instance); // feching table name according to the given instance name

          logger.info("Table name has been fetched from intance name");

          hiveHistoryQueryImpl.writetoFileQueryhql(hiveQuery[i], ConfFileReader.getHomeDir());// writing to .hql file to a temp file on local disk

          logger.info(".hql file created in Temp directory");

          hiveHistoryQueryImpl.writetoFileLogs(ConfFileReader.getHomeDir());// writing to logs file to a temp file on local disk

          logger.info("Log file created in Temp directory");

          maxCountOfAmbariDb = (hiveHistoryQueryImpl.fetchMaximumIdfromAmbaridb(view.getProperties().get("ambaridrivername"), connectionAmbaridb, id) + 1);// fetching the maximum count for ambari db to insert

          time = hiveHistoryQueryImpl.getTime();// getting the system current time.

          epochTime = hiveHistoryQueryImpl.getEpochTime();// getting system time as epoch format

          dirNameforHiveHistroy = "/user/admin/hive/jobs/hive-job-" + maxCountOfAmbariDb + "-" + time + "/";// creating the directory name

          logger.info("Directory name where .hql will be saved: " + dirNameforHiveHistroy);

          hiveHistoryQueryImpl.insertRowinAmbaridb(view.getProperties().get("ambaridrivername"), dirNameforHiveHistroy, maxCountOfAmbariDb, epochTime, connectionAmbaridb, id, instance, i);// inserting in ambari database

          if (view.getProperties().get("KerberoseEnabled").equals("y")) {

            logger.info("kerberose enabled");
            hiveHistoryQueryImpl.createDirKerberorisedSecured(dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"));// creating directory in kerborized secured hdfs
            logger.info("Directory created in hdfs");
            hiveHistoryQueryImpl.putFileinHdfsKerborizedSecured(ConfFileReader.getHomeDir() + "query.hql", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"));// copying the .hql file to kerborized hdfs
            hiveHistoryQueryImpl.putFileinHdfsKerborizedSecured(ConfFileReader.getHomeDir() + "logs", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"));// copying the log file to kerborized hdfs
          } else {

            logger.info("kerberose not enabled");
            hiveHistoryQueryImpl.createDir(dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"));// creating directory in hdfs
            logger.info("Directory created in hdfs");
            hiveHistoryQueryImpl.putFileinHdfs(ConfFileReader.getHomeDir() + "query.hql", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"));// copying the .hql file to hdfs
            hiveHistoryQueryImpl.putFileinHdfs(ConfFileReader.getHomeDir() + "logs", dirNameforHiveHistroy, view.getProperties().get("namenode_URI_Ambari"));// copying the log file to hdfs
          }

        }
        connectionAmbaridb.commit();

      }
    } catch (SQLException e) {
      logger.error("Sql exception in ambari database: ", e);
      try {
        connectionAmbaridb.rollback();
        logger.error("Sql statement are Rolledback");
      } catch (SQLException e1) {
        logger.error("Sql rollback exception in ambari database",
          e1);
      }
    } catch (ClassNotFoundException e) {
      logger.error("Class not found :- " ,e);
    } catch (ParseException e) {
      logger.error("Parse Exception : " ,e);
    } catch (URISyntaxException e) {
      logger.error("URI Syntax Exception: " ,e);
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: " ,e);
    } finally {
      if (connectionAmbaridb != null) try {
        connectionAmbaridb.close();
      } catch (SQLException e) {
        logger.error("Exception in closing the connection :" ,e);
      }
    }
    //deleteing the temprary files that are created while execution
    hiveHistoryQueryImpl.deleteFileQueryhql(ConfFileReader.getHomeDir());
    hiveHistoryQueryImpl.deleteFileQueryLogs(ConfFileReader.getHomeDir());

    session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, 0);
    logger.info("------------------------------");
    logger.info("Hive History query Migration Ends");
    logger.info("------------------------------");

    /* servlet returned to client */
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    out.println("<br>");
    out.println("<h4>" + i + " Query has been migrated to  " + instance + "</h4>");

  }

}
