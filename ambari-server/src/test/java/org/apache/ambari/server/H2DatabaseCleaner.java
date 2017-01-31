/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server;

import static org.eclipse.persistence.config.PersistenceUnitProperties.DEFAULT_CREATE_JDBC_FILE_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.commons.collections.CollectionUtils;

import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class H2DatabaseCleaner {
  private static final String SEQ_INSERT_PREFIX = "INSERT INTO ambari_sequences";
  private static List<String> seqInsertStatements;

  public static void clearDatabaseAndStopPersistenceService(Injector injector) throws AmbariException, SQLException {
    clearDatabase(injector.getProvider(EntityManager.class).get());
    injector.getInstance(PersistService.class).stop();
  }

  public static void clearDatabase(EntityManager entityManager) throws AmbariException, SQLException {
    clearDatabase(entityManager, Configuration.JDBC_IN_MEMORY_URL,
      Configuration.JDBC_IN_MEMORY_USER, Configuration.JDBC_IN_MEMORY_PASSWORD);
  }

  private static List<String> collectSequenceInserts() {
    try {
      ArrayList<String> statementList = new ArrayList<>();
      for (String s : Files.readAllLines(Paths.get(DEFAULT_CREATE_JDBC_FILE_NAME), Charset.defaultCharset())) {
        if (s.startsWith(SEQ_INSERT_PREFIX)) {
          statementList.add(s);
        }
      }
      return statementList;
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }

  //TODO all tests this method is used in should be modified to remove hardcoded IDs
  public static void resetSequences(Injector injector) {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    try {
      if (dbAccessor.tableExists("ambari_sequences")) {
        if (seqInsertStatements == null) {
          seqInsertStatements = collectSequenceInserts();
        }
        if (!CollectionUtils.isEmpty(seqInsertStatements)) {
          dbAccessor.truncateTable("ambari_sequences");

          for (String insert : seqInsertStatements) {
            dbAccessor.executeUpdate(insert);
          }
        }

      }
    } catch (SQLException ignored) {
    }
  }

  public static void clearDatabase(EntityManager entityManager, String dbURL, String dbUser, String dbPass) throws SQLException {
    Connection connection = DriverManager.getConnection(dbURL, dbUser, dbPass);
    Statement s = connection.createStatement();

    try {
      // Disable FK
      s.execute("SET REFERENTIAL_INTEGRITY FALSE");

      entityManager.getTransaction().begin();
      // Truncate tables for all entities
      for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
        Query query = entityManager.createQuery("DELETE FROM " + entity.getName() + " em");
        query.executeUpdate();
//        final String tableName = entity.getBindableJavaType().getAnnotation(Table.class).name();
//        s.executeUpdate("TRUNCATE TABLE " + tableName);
      }

      entityManager.getTransaction().commit();

      // Enable FK
      s.execute("SET REFERENTIAL_INTEGRITY TRUE");

      //reset shared cache
      entityManager.getEntityManagerFactory().getCache().evictAll();
    } finally {
      s.close();
      connection.close();
    }
  }
}
