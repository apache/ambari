/*
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
package org.apache.ambari.server.orm.dao;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Provider;

public class KerberosKeytabPrincipalDAOTest {

    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);

    @Mock(type = MockType.STRICT)
    private Provider<EntityManager> entityManagerProvider;

    @Mock(type = MockType.STRICT)
    private EntityManager entityManager;

    @TestSubject
    private final KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO = new KerberosKeytabPrincipalDAO();

    @Before
    public void before() {
        reset(entityManagerProvider);
        expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
        replay(entityManagerProvider);
    }

    @Test
    public void testFindOrCreate() {
        HostEntity hostEntity = new HostEntity();
        hostEntity.setHostName("h1");
        hostEntity.setHostId(1L);

        KerberosKeytabEntity kke = new KerberosKeytabEntity();
        kke.setKeytabPath("/some/path");

        KerberosPrincipalEntity kpe = new KerberosPrincipalEntity();
        kpe.setPrincipalName("test@EXAMPLE.COM");

        List<KerberosKeytabPrincipalEntity> keytabList = new ArrayList<>();
        keytabList.add(null);

        kerberosKeytabPrincipalDAO.findOrCreate(kke, hostEntity, kpe, keytabList);
    }
}