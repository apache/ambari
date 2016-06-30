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

package org.apache.ambari.view.huetoambarimigration.migration.configuration;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.ConfigurationModel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.web.WebHdfsFileSystem;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration check Implenetation class
 */

public class ConfigurationCheckImplementation {

  static final Logger logger = Logger.getLogger(ConfigurationCheckImplementation.class);

  private static String homeDir = System.getProperty("java.io.tmpdir") + "/";

  public static ConfigurationModel checkConfigurationForHue(String hueURL) throws IOException {

    URL url = null;
    int resonseCode = 0;
    ConfigurationModel hueHttpUrl = new ConfigurationModel();
    hueHttpUrl.setId(1);
    hueHttpUrl.setConfigParameter("hueHtttpUrl");
    url = new URL(hueURL);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
    connection.connect();
    resonseCode = connection.getResponseCode();
    if (resonseCode == 200) {
      hueHttpUrl.setConfigStatus("Success");
    } else {
      hueHttpUrl.setConfigStatus("Failed");
    }
    return hueHttpUrl;
  }

  public static ConfigurationModel checkHueDatabaseConnection(String hueDBDRiver, String hueJdbcUrl, String huedbUsername, String huedbPassword) throws IOException, PropertyVetoException, SQLException {

    ConfigurationModel configmodelHueDB = new ConfigurationModel();
    configmodelHueDB.setId(4);
    configmodelHueDB.setConfigParameter("huedb");
    Connection con = DataSourceHueDatabase.getInstance(hueDBDRiver, hueJdbcUrl, huedbUsername, huedbPassword).getConnection();
    configmodelHueDB.setConfigStatus("Success");
    return configmodelHueDB;
  }

  public static ConfigurationModel checkAmbariDatbaseConection(String ambariDBDriver, String ambariDBJdbcUrl, String ambariDbUsername, String ambariDbPassword) throws IOException, PropertyVetoException, SQLException {

    ConfigurationModel configmodelAmbariDB = new ConfigurationModel();
    configmodelAmbariDB.setId(5);
    configmodelAmbariDB.setConfigParameter("ambaridb");
    Connection con = DataSourceAmbariDatabase.getInstance(ambariDBDriver, ambariDBJdbcUrl, ambariDbUsername, ambariDbPassword).getConnection();
    configmodelAmbariDB.setConfigStatus("Success");
    return configmodelAmbariDB;
  }

  public static String getHomeDir() {
    return homeDir;
  }

  public static ConfigurationModel checkNamenodeURIConnectionforambari(String ambariServerNameNode) throws Exception {

    ConfigurationModel configmodelWebhdfsAmbari = new ConfigurationModel();
    configmodelWebhdfsAmbari.setId(6);
    configmodelWebhdfsAmbari.setConfigParameter("ambariwebhdfsurl");
    Configuration conf = new Configuration();
    conf.set("fs.hdfs.impl",
      org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
    conf.set("fs.file.impl",
      org.apache.hadoop.fs.LocalFileSystem.class.getName()
    );
    FileSystem fileSystem = FileSystem.get(new URI(ambariServerNameNode), conf);

    if (fileSystem instanceof WebHdfsFileSystem) {
      configmodelWebhdfsAmbari.setConfigStatus("Success");
    } else {
      configmodelWebhdfsAmbari.setConfigStatus("Failed");
      throw new Exception();
    }
    return configmodelWebhdfsAmbari;
  }

  public static ConfigurationModel checkNamenodeURIConnectionforHue(String hueServerNamenodeURI) throws Exception {

    ConfigurationModel configmodelWebhdfsHue = new ConfigurationModel();
    configmodelWebhdfsHue.setId(7);
    configmodelWebhdfsHue.setConfigParameter("huewebhdfsurl");
    Configuration conf = new Configuration();
    conf.set("fs.hdfs.impl",
      org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
    );
    conf.set("fs.file.impl",
      org.apache.hadoop.fs.LocalFileSystem.class.getName()
    );
    FileSystem fileSystem = FileSystem.get(new URI(hueServerNamenodeURI), conf);

    if (fileSystem instanceof WebHdfsFileSystem) {
      configmodelWebhdfsHue.setConfigStatus("Success");
    } else {
      configmodelWebhdfsHue.setConfigStatus("Failed");
      throw new Exception();
    }
    return configmodelWebhdfsHue;
  }
}
