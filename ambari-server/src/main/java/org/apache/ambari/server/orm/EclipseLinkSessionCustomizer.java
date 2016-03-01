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
package org.apache.ambari.server.orm;

import javax.activation.DataSource;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.platform.database.MySQLPlatform;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.Session;

/**
 * The {@link EclipseLinkSessionCustomizer} is used as a way to quickly override
 * the way that EclipseLink interacts with the database. Some possible uses of
 * this class are:
 * <ul>
 * <li>Setting runtime analysis properties such as log levels and profilers</li>
 * <li>Providing a custom {@link DataSource} via {@link JNDIConnector}</li>
 * <li>Changing JDBC driver properties.</li>
 * </ul>
 * For example:
 *
 * <pre>
 * DatabaseLogin login = (DatabaseLogin) session.getDatasourceLogin();
 * ComboPooledDataSource source = new ComboPooledDataSource();
 * source.setDriverClass(login.getDriverClassName());
 * source.setMaxConnectionAge(100);
 * ...
 * login.setConnector(new JNDIConnector(source));
 *
 * <pre>
 */
public class EclipseLinkSessionCustomizer implements SessionCustomizer {

  /**
   * {@inheritDoc}
   * <p/>
   * This class exists for quick customization purposes.
   */
  @Override
  public void customize(Session session) throws Exception {
    //Override transaction isolation level for MySQL to match EclipseLink shared cache behavior
    DatabaseLogin databaseLogin = (DatabaseLogin) session.getDatasourceLogin();
    if (databaseLogin.getDatasourcePlatform() instanceof MySQLPlatform) {
      databaseLogin.setTransactionIsolation(DatabaseLogin.TRANSACTION_READ_COMMITTED);
    }
  }
}
