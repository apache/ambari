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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * CompatibleRepositoryVersionResourceProvider tests.
 */
public class CompatibleRepositoryVersionResourceProviderTest {

  private static Injector injector;

  private static String jsonStringRedhat6 = "[{\"OperatingSystems\":{\"os_type\":\"redhat6\"},\"repositories\":[]}]";

  @Before
  public void before() throws Exception {
    final AmbariMetaInfo ambariMetaInfo = EasyMock.createMock(AmbariMetaInfo.class);
    final ClusterVersionDAO clusterVersionDAO = EasyMock.createMock(ClusterVersionDAO.class);

    StackEntity hdp11Stack = new StackEntity();
    hdp11Stack.setStackName("HDP");
    hdp11Stack.setStackVersion("1.1");

    RepositoryVersionEntity entity1 = new RepositoryVersionEntity();
    entity1.setDisplayName("name1");
    entity1.setOperatingSystems(jsonStringRedhat6);
    entity1.setStack(hdp11Stack);
    entity1.setVersion("1.1.1.1");

    StackEntity hdp22Stack = new StackEntity();
    hdp22Stack.setStackName("HDP");
    hdp22Stack.setStackVersion("2.2");

    RepositoryVersionEntity entity2 = new RepositoryVersionEntity();
    entity2.setDisplayName("name2");
    entity2.setOperatingSystems(jsonStringRedhat6);
    entity2.setStack(hdp22Stack);
    entity2.setVersion("2.2.2.2");

    final RepositoryVersionDAO repoVersionDAO = EasyMock.createMock(RepositoryVersionDAO.class);

    StackId stackId11 = new StackId("HDP", "1.1");
    StackId stackId22 = new StackId("HDP", "2.2");

    expect(repoVersionDAO.findByStack(stackId11)).andReturn(Collections.singletonList(entity1)).atLeastOnce();
    expect(repoVersionDAO.findByStack(stackId22)).andReturn(Collections.singletonList(entity2)).atLeastOnce();
    replay(repoVersionDAO);

    final StackInfo stack1 = new StackInfo() {
      @Override
      public Map<String, UpgradePack> getUpgradePacks() {
        Map<String, UpgradePack> map = new HashMap<String, UpgradePack>();

        UpgradePack pack1 = new UpgradePack() {

          @Override
          public String getName() {
            return "pack1";
          }

          @Override
          public String getTarget() {
            return "1.1.*.*";
          }
        };

        final UpgradePack pack2 = new UpgradePack() {
          @Override
          public String getName() {
            return "pack2";
          }

          @Override
          public String getTarget() {
            return "2.2.*.*";
          }

          @Override
          public String getTargetStack() {
            return "HDP-2.2";
          }
        };

        map.put("pack1", pack1);
        map.put("pack2", pack2);
        return map;
      }
    };

    final StackInfo stack2 = new StackInfo() {
      @Override
      public Map<String, UpgradePack> getUpgradePacks() {
        Map<String, UpgradePack> map = new HashMap<String, UpgradePack>();

        UpgradePack pack = new UpgradePack() {
          @Override
          public String getName() {
            return "pack2";
          }

          @Override
          public String getTarget() {
            return "2.2.*.*";
          }
        };

        map.put("pack2", pack);
        return map;
      }
    };



    InMemoryDefaultTestModule injectorModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        super.configure();
        bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
        bind(ClusterVersionDAO.class).toInstance(clusterVersionDAO);
        bind(RepositoryVersionDAO.class).toInstance(repoVersionDAO);
        requestStaticInjection(CompatibleRepositoryVersionResourceProvider.class);
      };
    };
    injector = Guice.createInjector(injectorModule);

    expect(ambariMetaInfo.getUpgradePacks("HDP", "1.1")).andReturn(stack1.getUpgradePacks());
    expect(ambariMetaInfo.getUpgradePacks("HDP", "2.2")).andReturn(stack2.getUpgradePacks());

    replay(ambariMetaInfo);

    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  @Test
  public void testGetResources() throws Exception {
    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest(
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
    Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    // !!! non-compatible, within stack
    Assert.assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    CompatibleRepositoryVersionResourceProvider compatibleProvider = new CompatibleRepositoryVersionResourceProvider(null);

    getRequest = PropertyHelper.getReadRequest(
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
    predicateStackName = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    predicateStackVersion = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    // !!! compatible, across stack
    Assert.assertEquals(2, compatibleProvider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

  }

}
