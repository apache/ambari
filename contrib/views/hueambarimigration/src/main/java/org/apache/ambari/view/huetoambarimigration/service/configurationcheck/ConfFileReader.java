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

package org.apache.ambari.view.huetoambarimigration.service.configurationcheck;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import javax.ws.rs.core.Context;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.URLStreamProvider;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;

import org.apache.ambari.view.huetoambarimigration.model.*;
import org.apache.hadoop.hdfs.web.WebHdfsFileSystem;
import org.apache.log4j.Logger;

public class ConfFileReader {

  static final Logger logger = Logger.getLogger(ConfFileReader.class);

  private static String homeDir = System.getProperty("java.io.tmpdir")+"/";

  public static boolean checkConfigurationForHue(String hueURL) {

    URL url = null;
    int resonseCode = 0;
    try {
      url = new URL(hueURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
      connection.connect();
      resonseCode = connection.getResponseCode();


    } catch (MalformedURLException e) {

      logger.error("Error in accessing the URL:" , e);

    } catch (ProtocolException e) {

      logger.error("Error in protocol: ", e);
    } catch (IOException e) {

      logger.error("IO Exception while establishing connection:",e);
    }

    return resonseCode == 200 ;
  }

  public static boolean checkConfigurationForAmbari(String ambariURL) {


    URL url = null;
    int responseCode = 0;
    try {
      url = new URL(ambariURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
      connection.connect();
      responseCode = connection.getResponseCode();

    } catch (MalformedURLException e) {
      logger.error("Error in accessing the URL: " , e);

    } catch (ProtocolException e) {
      logger.error("Error in protocol: ", e);
    } catch (IOException e) {
      logger.error("IO Exception while establishing connection: ",e);
    }
    return responseCode == 200 ;


  }

  public static boolean checkHueDatabaseConnection(String hueDBDRiver, String hueJdbcUrl, String huedbUsername, String huedbPassword) throws IOException {

    try {
      Connection con = DataSourceHueDatabase.getInstance(hueDBDRiver, hueJdbcUrl, huedbUsername, huedbPassword).getConnection();
    }
    catch (Exception e) {

      logger.error("Sql exception in acessing Hue Database: " ,e);
      return false;
    }

    return true;

  }

  public static boolean checkAmbariDatbaseConection(String ambariDBDriver, String ambariDBJdbcUrl, String ambariDbUsername, String ambariDbPassword) throws IOException {


    try {

      Connection con = DataSourceAmbariDatabase.getInstance(ambariDBDriver, ambariDBJdbcUrl, ambariDbUsername, ambariDbPassword).getConnection();


    } catch (Exception e) {

      logger.error("Sql exception in acessing Ambari Database: " ,e);

      return false;
    }

    return true;

  }

  public static String getHomeDir() {
    return homeDir;
  }

  public static void setHomeDir(String homeDir) {
    ConfFileReader.homeDir = homeDir;
  }

  public static boolean checkNamenodeURIConnectionforambari(String ambariServerNameNode) throws IOException, URISyntaxException {


    Configuration conf = new Configuration();
    conf.set("fs.hdfs.impl",
      org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
    conf.set("fs.file.impl",
      org.apache.hadoop.fs.LocalFileSystem.class.getName()
    );

    FileSystem fileSystem = FileSystem.get(new URI(ambariServerNameNode), conf);


    if (fileSystem instanceof WebHdfsFileSystem) {

      return true;

    } else {

      return false;
    }


  }

  public static boolean checkNamenodeURIConnectionforHue(String hueServerNamenodeURI) throws IOException, URISyntaxException {

    Configuration conf = new Configuration();
    conf.set("fs.hdfs.impl",
      org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
    );
    conf.set("fs.file.impl",
      org.apache.hadoop.fs.LocalFileSystem.class.getName()
    );

    FileSystem fileSystem = FileSystem.get(new URI(hueServerNamenodeURI), conf);


    if (fileSystem instanceof WebHdfsFileSystem) {

      return true;
    } else {

      return false;
    }


  }


}
