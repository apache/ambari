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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.*;

import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.ViewContext;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.sql.*;
import java.util.*;

@RunWith(EasyMockRunner.class)
public class QueryResourceProviderTest extends EasyMockSupport {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @TestSubject
    private QueryResourceProvider provider = new QueryResourceProvider();

    @Mock
    private ViewContext viewContext;

    @Mock
    private HAWQDataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet rs;

    @Mock
    private ResultSetMetaData rsMetaData;

    @Test
    public void testGetResources() throws Exception {

        expect(viewContext.getProperties()).andReturn(prepareViewContextProperties()).anyTimes();

        expect(dataSource.getConnection("host1", "1234", "gpadmin-user", "gpadmin-password")).andReturn(connection);
        expect(connection.createStatement()).andReturn(statement);
        expect(statement.executeQuery(anyString())).andReturn(rs);
        expect(rs.getMetaData()).andReturn(rsMetaData);
        expect(rsMetaData.getColumnCount()).andReturn(2);

        // row 1
        expect(rs.next()).andReturn(true);
        // mock only 2 columns
        expect(rsMetaData.getColumnName(1)).andReturn("procpid");
        expect(rs.getObject(1)).andReturn(10);
        expect(rsMetaData.getColumnName(2)).andReturn("text");
        expect(rs.getObject(2)).andReturn("text-10");

        // row 2
        expect(rs.next()).andReturn(true);
        // mock only 2 columns
        expect(rsMetaData.getColumnName(1)).andReturn("procpid");
        expect(rs.getObject(1)).andReturn(20);
        expect(rsMetaData.getColumnName(2)).andReturn("text");
        expect(rs.getObject(2)).andReturn("text-20");

        expect(rs.next()).andReturn(false);
        rs.close();
        expectLastCall().once();
        statement.close();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();

        replayAll();
        Set<QueryResource> results = provider.getResources(null);

        assertEquals(2, results.size());
        Set<String> ids = new HashSet<>();
        ids.add("10");
        ids.add("20");
        for (QueryResource query : results) {
            String id = query.getId();
            ids.remove(id);
            assertEquals(id, query.getAttributes().get("procpid").toString());
            assertEquals("text-" + id, query.getAttributes().get("text"));
        }
        assertTrue(ids.isEmpty());

        verifyAll();
    }


    @Test
    public void testGetResources_MissingUserName() throws Exception {
        thrown.expect(SystemException.class);
        thrown.expectMessage("HAWQ User Name property is not specified for the view instance.");

        Map<String, String> props = prepareViewContextProperties();
        props.remove("hawq.user.name");
        expect(viewContext.getProperties()).andReturn(props).anyTimes();
        replayAll();

        provider.getResources(null);
    }

    @Test
    public void testGetResources_MissingUserPassword() throws Exception {
        thrown.expect(SystemException.class);
        thrown.expectMessage("HAWQ User Password property is not specified for the view instance.");

        Map<String, String> props = prepareViewContextProperties();
        props.remove("hawq.user.password");
        expect(viewContext.getProperties()).andReturn(props).anyTimes();
        replayAll();

        provider.getResources(null);
    }

    @Test
    public void testGetResources_MissingHostName() throws Exception {
        thrown.expect(SystemException.class);
        thrown.expectMessage("HAWQ Master Host property is not specified for the view instance.");

        Map<String, String> props = prepareViewContextProperties();
        props.remove("hawq.master.host");
        expect(viewContext.getProperties()).andReturn(props).anyTimes();
        replayAll();

        provider.getResources(null);
    }

    @Test
    public void testGetResources_MissingHostPort() throws Exception {
        thrown.expect(SystemException.class);
        thrown.expectMessage("HAWQ Master Port property is not specified for the view instance.");

        Map<String, String> props = prepareViewContextProperties();
        props.remove("hawq.master.port");
        expect(viewContext.getProperties()).andReturn(props).anyTimes();
        replayAll();

        provider.getResources(null);
    }

    @Test
    public void testGetResources_ExceptionOnExecute() throws Exception {
        thrown.expect(SystemException.class);
        thrown.expectMessage("Can't get current queries.");

        expect(viewContext.getProperties()).andReturn(prepareViewContextProperties()).anyTimes();

        expect(dataSource.getConnection("host1", "1234", "gpadmin-user", "gpadmin-password")).andReturn(connection);
        expect(connection.createStatement()).andReturn(statement);
        expect(statement.executeQuery(anyString())).andStubThrow(new SQLException("test message"));
        statement.close();
        expectLastCall().once();
        connection.close();
        expectLastCall().once();
        replayAll();

        provider.getResources(null);
    }

    private Map<String, String> prepareViewContextProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("hawq.user.name", "gpadmin-user");
        props.put("hawq.user.password", "gpadmin-password");
        props.put("hawq.master.host", "host1");
        props.put("hawq.master.port", "1234");
        return props;
    }
}
