package org.apache.ambari.server.orm.dao;

import com.google.inject.Provider;
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

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;

public class KerberosKeytabPrincipalDAOTest {

    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);

    @Mock(type = MockType.STRICT)
    private Provider<EntityManager> entityManagerProvider;

    @Mock(type = MockType.STRICT)
    private EntityManager entityManager;

    @TestSubject
    private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO = new KerberosKeytabPrincipalDAO();

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