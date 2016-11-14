/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.apache.ambari.view.hawq;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.ambari.view.SystemException;
import org.easymock.EasyMockSupport;
import org.easymock.TestSubject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.sql.Connection;
import java.sql.SQLException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.powermock.api.easymock.PowerMock.*;
import static org.powermock.api.easymock.PowerMock.expectNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HAWQDataSource.class, ComboPooledDataSource.class})
public class HAWQDataSourceTest extends EasyMockSupport {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @TestSubject
    private HAWQDataSource datasource = new HAWQDataSource();

    @Test
    public void testGetConnection() throws Exception {

        // first invocation, creates new CPDS
        ComboPooledDataSource cpds1 = createMock(ComboPooledDataSource.class);
        expectNew(ComboPooledDataSource.class).andReturn(cpds1);
        cpds1.setDriverClass("org.postgresql.Driver");
        expectLastCall().once();
        cpds1.setJdbcUrl("jdbc:postgresql://host:port/template1");
        expectLastCall().once();
        cpds1.setUser("user");
        expectLastCall().once();
        cpds1.setPassword("password");
        expectLastCall().once();
        cpds1.setInitialPoolSize(0);
        expectLastCall().once();
        cpds1.setMinPoolSize(0);
        expectLastCall().once();
        cpds1.setMaxPoolSize(1);
        expectLastCall().once();
        cpds1.setMaxIdleTime(600);
        expectLastCall().once();
        cpds1.setAcquireRetryAttempts(5);
        expectLastCall().once();
        cpds1.setAcquireRetryDelay(1000);
        expectLastCall().once();

        Connection conn1 = createMock(Connection.class);
        expect(cpds1.getConnection()).andReturn(conn1);

        // second invocation, closes old CPDS, creates a new one

        cpds1.close();
        expectLastCall().once();

        ComboPooledDataSource cpds2 = createStrictMock(ComboPooledDataSource.class);
        expectNew(ComboPooledDataSource.class).andReturn(cpds2);
        cpds2.setDriverClass("org.postgresql.Driver");
        expectLastCall().once();
        cpds2.setJdbcUrl("jdbc:postgresql://host:port/template1");
        expectLastCall().once();
        cpds2.setUser("user2");
        expectLastCall().once();
        cpds2.setPassword("password");
        expectLastCall().once();
        cpds2.setInitialPoolSize(0);
        expectLastCall().once();
        cpds2.setMinPoolSize(0);
        expectLastCall().once();
        cpds2.setMaxPoolSize(1);
        expectLastCall().once();
        cpds2.setMaxIdleTime(600);
        expectLastCall().once();
        cpds2.setAcquireRetryAttempts(5);
        expectLastCall().once();
        cpds2.setAcquireRetryDelay(1000);
        expectLastCall().once();

        Connection conn2 = createMock(Connection.class);
        expect(cpds2.getConnection()).andReturn(conn2);

        // third invocation, only return the same connection
        expect(cpds2.getConnection()).andReturn(conn2);

        replay(cpds1, ComboPooledDataSource.class);
        replay(cpds2, ComboPooledDataSource.class);
        replay(conn1);
        replay(conn2);

        Connection connection1 = datasource.getConnection("host", "port", "user", "password");
        Connection connection2 = datasource.getConnection("host", "port", "user2", "password");
        Connection connection3 = datasource.getConnection("host", "port", "user2", "password");
        assertNotEquals(connection1, connection2);
        assertEquals(connection2, connection3);
    }

    @Test(expected = SystemException.class)
    public void testGetConnection_FailToLoadDriver() throws Exception {
        ComboPooledDataSource cpds = createMock(ComboPooledDataSource.class);
        expectNew(ComboPooledDataSource.class).andReturn(cpds);
        cpds.setDriverClass("org.postgresql.Driver");
        expectLastCall().andThrow(new RuntimeException("driver not found"));
        replay(cpds, ComboPooledDataSource.class);
        datasource.getConnection("host", "port", "user", "password");
    }

    @Test(expected = SQLException.class)
    public void testGetConnection_FailToGetConnection() throws Exception {
        ComboPooledDataSource cpds1 = createMock(ComboPooledDataSource.class);
        expectNew(ComboPooledDataSource.class).andReturn(cpds1);
        cpds1.setDriverClass("org.postgresql.Driver");
        expectLastCall().once();
        cpds1.setJdbcUrl("jdbc:postgresql://host:port/template1");
        expectLastCall().once();
        cpds1.setUser("user");
        expectLastCall().once();
        cpds1.setPassword("password");
        expectLastCall().once();
        cpds1.setInitialPoolSize(0);
        expectLastCall().once();
        cpds1.setMinPoolSize(0);
        expectLastCall().once();
        cpds1.setMaxPoolSize(1);
        expectLastCall().once();
        cpds1.setMaxIdleTime(600);
        expectLastCall().once();
        cpds1.setAcquireRetryAttempts(5);
        expectLastCall().once();
        cpds1.setAcquireRetryDelay(1000);
        expectLastCall().once();

        Connection conn1 = createMock(Connection.class);
        expect(cpds1.getConnection()).andThrow(new SQLException("fail"));
        replay(cpds1, ComboPooledDataSource.class);
        replay(conn1);
        datasource.getConnection("host", "port", "user", "password");
    }
}
