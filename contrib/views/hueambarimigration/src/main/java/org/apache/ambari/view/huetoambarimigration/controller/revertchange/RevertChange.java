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

package org.apache.ambari.view.huetoambarimigration.controller.revertchange;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.controller.configurationcheck.ProgressBarStatus;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;


public class RevertChange extends HttpServlet {

  private static final long serialVersionUID = 1L;
  ViewContext view;

  @Override
  public void init(ServletConfig config) throws ServletException {

    super.init(config);
    ServletContext context = config.getServletContext();
    view = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
  }

  public boolean stringtoDatecompare(String datefromservlet,
                                     String datefromfile) throws ParseException {

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    Date date1 = formatter.parse(datefromservlet);
    Date date2 = formatter.parse(datefromfile);
    if (date1.compareTo(date2) < 0) {
      return true;
    } else {
      return false;
    }

  }

  public void removedir(final String dir, final String namenodeuri)
    throws IOException, URISyntaxException {

    try {
      UserGroupInformation ugi = UserGroupInformation
        .createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          Configuration conf = new Configuration();
          conf.set("fs.hdfs.impl",
            org.apache.hadoop.hdfs.DistributedFileSystem.class
              .getName());
          conf.set("fs.file.impl",
            org.apache.hadoop.fs.LocalFileSystem.class
              .getName());
          conf.set("fs.defaultFS", namenodeuri);
          conf.set("hadoop.job.ugi", "hdfs");

          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          fs.delete(src, true);
          return null;
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void doGet(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {

    final Logger logger = Logger.getLogger(RevertChange.class);

    logger.info("------------------------------");
    logger.info("Reverting the changes Start:");
    logger.info("------------------------------");

    HttpSession session = request.getSession(true);
    String revertDate = request.getParameter("revertdate");
    String instance = request.getParameter("instance");

    logger.info("Revert Date " + revertDate);
    logger.info("instance name " + instance);

    BufferedReader br = null;
    Connection connectionAmbariDatabase = null;

    try {
      connectionAmbariDatabase = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();
      connectionAmbariDatabase.setAutoCommit(false);

      Statement stmt = null;
      stmt = connectionAmbariDatabase.createStatement();
      SAXBuilder builder = new SAXBuilder();
      File xmlFile = new File(ConfFileReader.getHomeDir() + "RevertChange.xml");
      try {

        Document document = (Document) builder.build(xmlFile);
        Element rootNode = document.getRootElement();
        List list = rootNode.getChildren("RevertRecord");

        for (int i = 0; i < list.size(); i++) {

          float calc = ((float) (i + 1)) / list.size() * 100;
          int progressPercentage = Math.round(calc);
          session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, progressPercentage);

          Element node = (Element) list.get(i);

          if (node.getChildText("instance").equals(instance)) {

            if (stringtoDatecompare(revertDate, node.getChildText("datetime").toString())) {

              String sql = node.getChildText("query");
              logger.info(sql);
              stmt.executeUpdate(sql);
              removedir(node.getChildText("dirname").toString(), view.getProperties().get("namenode_URI_Ambari"));
              logger.info(node.getChildText("dirname").toString()+" deleted");

            }

          }

        }

        connectionAmbariDatabase.commit();

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<br>");
        out.println("<h4>" + " The change has been revert back for "
          + instance + "</h4>");

        session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, 0);

        logger.info("------------------------------");
        logger.info("Reverting the changes End");
        logger.info("------------------------------");

      } catch (IOException e) {
        logger.error("IOException: ",e);
      } catch (ParseException e) {
        logger.error("ParseException: ",e);
      } catch (JDOMException e) {
        logger.error("JDOMException: ",e);
      } catch (URISyntaxException e) {
        logger.error("URISyntaxException:  ",e);
      }
    } catch (SQLException e1) {
      logger.error("SqlException  ",e1);
      try {
        connectionAmbariDatabase.rollback();
        logger.info("Rollback done");
      } catch (SQLException e2) {
        logger.error("SqlException in Rollback  ",e2);
      }
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: ",e);
    }

  }

}
