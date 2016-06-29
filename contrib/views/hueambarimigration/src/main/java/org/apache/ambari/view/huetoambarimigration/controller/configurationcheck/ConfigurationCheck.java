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


package org.apache.ambari.view.huetoambarimigration.controller.configurationcheck;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.Connection;

import org.apache.ambari.view.ViewContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.ambari.view.huetoambarimigration.service.*;
import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;
import org.apache.log4j.Logger;


public class ConfigurationCheck extends HttpServlet {
  private static final long serialVersionUID = 1L;

  ViewContext view;

  @Override
  public void init(ServletConfig config) throws ServletException {

    super.init(config);
    ServletContext context = config.getServletContext();
    view = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    final Logger logger = Logger.getLogger(ConfigurationCheck.class);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    out.println("<table class=\"table\">");
    out.println("<thead><tr><th>Service</th><th>Status</th></tr></thead>");
    out.println("<tbody>");

    if (ConfFileReader.checkConfigurationForHue(view.getProperties().get("Hue_URL"))) {
      logger.info("Hue URl connection:- Success");
      out.println("<tr class=\"success\">");
      out.println("<td><h6>" + "Ambari" + "</h6></td>");
      out.println("<td><h6>" + "OK" + "</h6></td>");
      out.println("</tr>");
    } else {
      logger.info("Hue URl connection:- Failed");
      out.println("<tr class=\"danger\">");
      out.println("<td><h6>" + "Ambari" + "</h6></td>");
      out.println("<td><h6>" + "ERROR" + "</h6></td>");
      out.println("</tr>");
    }

    if (ConfFileReader.checkConfigurationForAmbari(view.getProperties().get("Ambari_URL"))) {

      logger.info("Ambari URl connection:- Success");
      out.println("<tr class=\"success\">");
      out.println("<td><h6>" + "Hue" + "</h6></td>");
      out.println("<td><h6>" + "OK" + "</h6></td>");
      out.println("</tr>");

    } else {

      logger.info("Ambari URl connection:- Failed");
      out.println("<tr class=\"danger\">");
      out.println("<td><h6>" + "Hue" + "</h6></td>");
      out.println("<td><h6>" + "ERROR" + "</h6></td>");
      out.println("</tr>");

    }

    if (ConfFileReader.checkAmbariDatbaseConection(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword"))) {

      logger.info("Ambari Database connection:- Success");
      out.println("<tr class=\"success\">");
      out.println("<td><h6>" + "Ambari Database" + "</h6></td>");
      out.println("<td><h6>" + "OK" + "</h6></td>");
      out.println("</tr>");

    } else {

      logger.info("Ambari Database connection:- Failed");
      out.println("<tr class=\"danger\">");
      out.println("<td><h6>" + "Ambari Database" + "</h6></td>");
      out.println("<td><h6>" + "ERROR" + "</h6></td>");
      out.println("</tr>");

    }
    if (ConfFileReader.checkHueDatabaseConnection(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword"))) {

      logger.info("Hue Database connection:- Success");
      out.println("<tr class=\"success\">");
      out.println("<td><h6>" + "Hue Database" + "</h6></td>");
      out.println("<td><h6>" + "OK" + "</h6></td>");
      out.println("</tr>");

    } else {

      logger.info("Hue Database connection:- Failed");
      out.println("<tr class=\"danger\">");
      out.println("<td><h6>" + "Hue Database" + "</h6></td>");
      out.println("<td><h6>" + "ERROR" + "</h6></td>");
      out.println("</tr>");

    }

    try {

      if (ConfFileReader.checkNamenodeURIConnectionforambari(view.getProperties().get("namenode_URI_Ambari"))) {

        logger.info("Web hdfs Access to ambari:- Success");
        out.println("<tr class=\"success\">");
        out.println("<td><h6>" + "namenodeURIAmbari" + "</h6></td>");
        out.println("<td><h6>" + "OK" + "</h6></td>");
        out.println("</tr>");

      } else {

        logger.info("Web hdfs Access to ambari:- Failed");
        out.println("<tr class=\"danger\">");
        out.println("<td><h6>" + "namenodeURIAmbari" + "</h6></td>");
        out.println("<td><h6>" + "ERROR" + "</h6></td>");
        out.println("</tr>");

      }
    } catch (URISyntaxException e) {
      logger.error("Error in accessing Webhdfs of Ambari: ", e);
    }

    try {
      if (ConfFileReader.checkNamenodeURIConnectionforHue(view.getProperties().get("namenode_URI_Hue"))) {

        logger.info("Web hdfs Access to hue:- Success");
        out.println("<tr class=\"success\">");
        out.println("<td><h6>" + "namenodeURIHue" + "</h6></td>");
        out.println("<td><h6>" + "OK" + "</h6></td>");
        out.println("</tr>");

      } else {

        logger.info("Web hdfs Access to hue:- Failed");
        out.println("<tr class=\"danger\">");
        out.println("<td><h6>" + "namenodeURIHue" + "</h6></td>");
        out.println("<td><h6>" + "ERROR" + "</h6></td>");
        out.println("</tr>");

      }
    } catch (URISyntaxException e) {
      logger.error("Error in accessing Webhdfs of Hue: " , e);
    }

    out.println("</tbody></table>");

  }


}
