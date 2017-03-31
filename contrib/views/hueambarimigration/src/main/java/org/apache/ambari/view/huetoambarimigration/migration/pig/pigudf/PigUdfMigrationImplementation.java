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

package org.apache.ambari.view.huetoambarimigration.migration.pig.pigudf;

import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.udfqueryset.QuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.udfqueryset.QuerySet;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.PigModel;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.sql.*;
import java.util.ArrayList;
import java.net.URI;


public class PigUdfMigrationImplementation {
    static final Logger logger = Logger.getLogger(PigUdfMigrationImplementation.class);
    final String USER_DIRECTORY = "/user";

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public int fetchSequenceno(Connection c, int id, QuerySetAmbariDB ambaridatabase) throws SQLException {

        String ds_id = new String();
        Statement stmt = null;
        PreparedStatement prSt = null;
        int sequencevalue=0;


        ResultSet rs = null;


        prSt = ambaridatabase.getSequenceNoFromAmbariSequence(c, id);

        logger.info("sql statement to fetch is from ambari instance:= =  " + prSt);

        rs = prSt.executeQuery();

        while (rs.next()) {
            sequencevalue = rs.getInt("sequence_value");
        }
        return sequencevalue;
    }

    public int fetchInstanceTablenamePigUdf(Connection c, String instance, QuerySetAmbariDB ambaridatabase) throws SQLException {

        String ds_id = new String();
        int id = 0;
        Statement stmt = null;
        PreparedStatement prSt = null;


        ResultSet rs = null;


        prSt = ambaridatabase.getTableIdFromInstanceName(c, instance);

        logger.info("sql statement to fetch is from ambari instance:= =  " + prSt);

        rs = prSt.executeQuery();

        while (rs.next()) {
            id = rs.getInt("id");
        }
        return id;

    }


    public void updateSequenceno(Connection c, int seqNo, int id, QuerySetAmbariDB ambaridatabase) throws SQLException, IOException {

        PreparedStatement prSt;
        prSt = ambaridatabase.updateSequenceNoInAmbariSequence(c, seqNo, id);
        logger.info("The actual insert statement is " + prSt);
        prSt.executeUpdate();
        logger.info("adding revert sql hive history");
    }

    public void insertRowForPigUdf(int maxcount, String path, String fileName, Connection c, int tableId, QuerySetAmbariDB ambaridatabase, String username) throws SQLException, IOException {


        PreparedStatement prSt = null;
        prSt = ambaridatabase.insertToPigUdf(c, tableId, Integer.toString(maxcount), fileName, username, path);
        prSt.executeUpdate();

    }


    public ArrayList<PigModel> fetchFromHueDatabase(String username, Connection connection, QuerySet huedatabase) throws ClassNotFoundException, SQLException, IOException {
        int id = 0;
        int i = 0;
        ResultSet rs1;

        ArrayList<PigModel> pigArrayList = new ArrayList<PigModel>();
        try {
            connection.setAutoCommit(false);
            PreparedStatement prSt = null;
            ResultSet rs;
            if (username.equals("all")) {
            } else {

                prSt = huedatabase.getUseridfromUserName(connection, username);

                rs = prSt.executeQuery();

                while (rs.next()) {
                    id = rs.getInt("id");
                }
            }


            if (username.equals("all")) {
                prSt = huedatabase.getAllQueries(connection);
            } else {
                prSt = huedatabase.getUserQueries(connection, id);
            }

            rs1 = prSt.executeQuery();


            // rs1 = statement.executeQuery("select url, file_name, owner_id from pig_udf");
            while (rs1.next()) {

                PigModel pojopig = new PigModel();
                String url = rs1.getString("url");
                String fileName = rs1.getString("file_name");
                int ownerId = rs1.getInt("owner_id");
                String ownerName = username;
                if(username.equals("all")){
                    ResultSet rs2 = huedatabase.getUserNamefromUserId(connection, ownerId).executeQuery();
                    while (rs2.next()) {
                        ownerName = rs2.getString("username");
                    }
                }
                logger.info("UDF ownwer name is "+ownerName);
                pojopig.setUrl(url);
                pojopig.setFileName(fileName);
                pojopig.setUserName(ownerName);
                pigArrayList.add(pojopig);
                i++;
            }
            connection.commit();

        } catch (SQLException e) {
            logger.error("SQLException", e);
            connection.rollback();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                logger.error("SQLException", e);
            }
        }

        return pigArrayList;

    }

    public void createDirPigUdfSecured(final String dir, final String namenodeuri,final String username,final String principalName)
            throws IOException, URISyntaxException {
        try {
            final Configuration conf = new Configuration();

            conf.set("fs.hdfs.impl",
                    org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
            );
            conf.set("fs.file.impl",
                    org.apache.hadoop.fs.LocalFileSystem.class.getName()
            );
            conf.set("fs.defaultFS", namenodeuri);
            conf.set("hadoop.security.authentication", "Kerberos");
            UserGroupInformation.setConfiguration(conf);
            UserGroupInformation proxyUser ;
            proxyUser = UserGroupInformation.createRemoteUser(principalName);
            UserGroupInformation ugi = UserGroupInformation.createProxyUser("hdfs", proxyUser);
            ugi.doAs(new PrivilegedExceptionAction<Boolean>() {

                public Boolean run() throws Exception {
                    URI uri = new URI(dir);
                    FileSystem fs = FileSystem.get(conf);
                    Path src = new Path(dir);
                    Boolean b = fs.mkdirs(src);

                    String[] subDirs = dir.split("/");
                    String dirPath = USER_DIRECTORY;
                    for(int i=2;i<subDirs.length;i++) {
                        dirPath += "/"+subDirs[i];
                        fs.setOwner(new Path(dirPath), username, username);
                    }
                    return b;
                }
            });
        } catch (Exception e) {
            logger.error("Exception in Webhdfs", e);
        }
    }


    public void createDirPigUdf(final String dir, final String namenodeuri,final String username)
            throws IOException, URISyntaxException {

        try {
            final Configuration conf = new Configuration();

            conf.set("fs.hdfs.impl",
                    org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
            );
            conf.set("fs.file.impl",
                    org.apache.hadoop.fs.LocalFileSystem.class.getName()
            );
            conf.set("fs.defaultFS", namenodeuri);
            conf.set("hadoop.job.ugi", "hdfs");
            conf.set("hadoop.security.authentication", "Kerberos");

            UserGroupInformation.setConfiguration(conf);
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser("hdfs");

            ugi.doAs(new PrivilegedExceptionAction<Void>() {

                public Void run() throws Exception {

                    URI uri = new URI(dir);
                    FileSystem fs = FileSystem.get(conf);
                    Path src = new Path(dir);
                    fs.mkdirs(src);

                    String[] subDirs = dir.split("/");
                    String dirPath = USER_DIRECTORY;
                    for(int i=2;i<subDirs.length;i++) {
                        dirPath += "/"+subDirs[i];
                        fs.setOwner(new Path(dirPath), username, username);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("Webhdfs: ", e);
        }
    }


    public void copyFileBetweenHdfs(final String source, final String dest, final String nameNodeuriAmbari,final String username)
            throws IOException {

        try {
            UserGroupInformation ugi = UserGroupInformation
                    .createRemoteUser("hdfs");

            ugi.doAs(new PrivilegedExceptionAction<Void>() {

                public Void run() throws Exception {

                    Configuration confAmbari = new Configuration();
                    confAmbari.set("fs.defaultFS", nameNodeuriAmbari);
                    confAmbari.set("hadoop.job.ugi", "hdfs");
                    FileSystem fileSystemAmbari = FileSystem.get(confAmbari);

                    Configuration confHue = new Configuration();
                    confHue.set("fs.defaultFS", nameNodeuriAmbari);
                    confHue.set("hadoop.job.ugi", "hdfs");
                    FileSystem fileSystemHue = FileSystem.get(confHue);

                    String filename = source.substring(
                            source.lastIndexOf('/') + 1, source.length());
                    String dest1;
                    if (dest.charAt(dest.length() - 1) != '/') {
                        dest1 = dest + "/" + filename;
                    } else {
                        dest1 = dest + filename;
                    }

                    Path path1 = new Path(source);
                    FSDataInputStream in1 = fileSystemHue.open(path1);

                    Path path = new Path(dest1);

                    FSDataOutputStream out = fileSystemAmbari.create(path);

                    byte[] b = new byte[1024];
                    int numBytes = 0;
                    while ((numBytes = in1.read(b)) > 0) {
                        out.write(b, 0, numBytes);
                    }
                    in1.close();
                    out.close();
                    fileSystemAmbari.setOwner(path, username, username);
                    fileSystemHue.close();
                    fileSystemAmbari.close();
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("Webhdfs exception: ", e);
        }

    }

    public void copyFileBetweenHdfsSecured(final String source, final String dest, final String nameNodeuriAmbari, final String username,final String pricipalName)
            throws IOException {

        try {

            final Configuration confAmbari = new Configuration();
            confAmbari.set("fs.defaultFS", nameNodeuriAmbari);
            confAmbari.set("hadoop.job.ugi", "hdfs");

            final Configuration confHue = new Configuration();
            confHue.set("fs.defaultFS", nameNodeuriAmbari);
            confHue.set("hadoop.job.ugi", "hdfs");

            confAmbari.set("hadoop.security.authentication", "Kerberos");
            confHue.set("hadoop.security.authentication", "Kerberos");

            UserGroupInformation.setConfiguration(confAmbari);
            UserGroupInformation proxyUser ;
            proxyUser = UserGroupInformation.createRemoteUser(pricipalName);
            UserGroupInformation ugi = UserGroupInformation.createProxyUser("hdfs", proxyUser);

            ugi.doAs(new PrivilegedExceptionAction<Void>() {

                public Void run() throws Exception {

                    FileSystem fileSystemAmbari = FileSystem.get(confAmbari);

                    FileSystem fileSystemHue = FileSystem.get(confHue);

                    String filename = source.substring(
                            source.lastIndexOf('/') + 1, source.length());
                    String dest1;
                    if (dest.charAt(dest.length() - 1) != '/') {
                        dest1 = dest + "/" + filename;
                    } else {
                        dest1 = dest + filename;
                    }

                    Path path1 = new Path(source);
                    FSDataInputStream in1 = fileSystemHue.open(path1);

                    Path path = new Path(dest1);

                    FSDataOutputStream out = fileSystemAmbari.create(path);
                    byte[] b = new byte[1024];
                    int numBytes = 0;
                    while ((numBytes = in1.read(b)) > 0) {
                        out.write(b, 0, numBytes);
                    }
                    in1.close();
                    out.close();
                    fileSystemAmbari.setOwner(path, username, username);
                    fileSystemHue.close();
                    fileSystemAmbari.close();
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("Webhdfs exception: ", e);
        }

    }


}
