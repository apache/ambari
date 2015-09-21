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


var App = require('app');
require('controllers/main/admin/security');


describe('App.MainAdminSecurityController', function () {

  var controller = App.MainAdminSecurityController.create({
    getServiceConfigsFromServer: function () {
    } ,
    services: [{serviceName: 'HDFS'}]
  });

  describe('#setServiceTagNames()', function () {
    var testCases = [
      {
        title: 'configs is empty object',
        content: {
          secureService: {},
          configs: {}
        },
        result: undefined
      },
      {
        title: 'secureService.sites is null',
        content: {
          secureService: {
            sites: null
          },
          configs: {
            site1: {}
          }
        },
        result: undefined
      },
      {
        title: 'secureService.sites doesn\'t contain required config tag',
        content: {
          secureService: {
            sites: []
          },
          configs: {
            site1: {}
          }
        },
        result: undefined
      },
      {
        title: 'secureService.sites contains required config tag',
        content: {
          secureService: {
            sites: ['site1']
          },
          configs: {
            site1: {
              tag: 'tag1'
            }
          }
        },
        result: {
          siteName: 'site1',
          tagName: 'tag1',
          newTagName: null,
          configs: {}
        }
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.setServiceTagNames(test.content.secureService, test.content.configs)).to.eql(test.result);
      });
    });
  });

  describe('#getSecurityStatusFromServerSuccessCallback()', function () {

    beforeEach(function () {
      sinon.spy(controller, 'showSecurityErrorPopup');
      sinon.spy(controller, 'getServiceConfigsFromServer');
    });
    afterEach(function () {
      controller.showSecurityErrorPopup.restore();
      controller.getServiceConfigsFromServer.restore();
    });

    it('desired_configs is empty', function () {
      var data = {Clusters: {
        desired_configs: {}
      }};
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.showSecurityErrorPopup.called).to.equal(true);
    });

    it('cluster-env is missing', function () {
      var data = {Clusters: {
        desired_configs: {
          'hdfs-site': {}
        }
      }};
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.showSecurityErrorPopup.called).to.equal(true);
    });

    it('cluster-env and hdfs-site are correct', function () {
      var data = {Clusters: {
        desired_configs: {
          'hdfs-site': {
            tag: 1
          },
          'cluster-env': {
            tag: 2
          },
          'hadoop-env': {
            tag: 3
          }
        }
      }};
      controller.getSecurityStatusFromServerSuccessCallback(data);
      expect(controller.get('tag.cluster-env')).to.equal(2);
      expect(controller.get('tag.hdfs-site')).to.equal(1);
      expect(controller.getServiceConfigsFromServer.called).to.equal(true);
    });
  });


  describe('#setNnHaStatus()', function () {

    beforeEach(function () {
      sinon.stub(App.db, "setIsNameNodeHa", Em.K);
    });
    afterEach(function () {
      App.db.setIsNameNodeHa.restore();
    });


    it('hdfsConfigs is null', function () {
      var hdfsConfigs = null;
      controller.setNnHaStatus(hdfsConfigs);
      expect(App.db.setIsNameNodeHa.withArgs('false').called).to.equal(true);
    });

    it('"dfs.nameservices" is absent in hdfsConfigs', function () {
      var hdfsConfigs = {};
      controller.setNnHaStatus(hdfsConfigs);
      expect(App.db.setIsNameNodeHa.withArgs('false').called).to.equal(true);
    });

    it('namenodesKey is absent in hdfsConfigs', function () {
      var hdfsConfigs = {
        'dfs.nameservices': 'key'
      };
      controller.setNnHaStatus(hdfsConfigs);
      expect(App.db.setIsNameNodeHa.withArgs('false').called).to.equal(true);
    });

    it('namenodesKey is present in hdfsConfigs', function () {
      var hdfsConfigs = {
        'dfs.nameservices': 'key',
        'dfs.ha.namenodes.key': 'true'
      };
      controller.setNnHaStatus(hdfsConfigs);
      expect(App.db.setIsNameNodeHa.withArgs('true').called).to.equal(true);
    });
  });

  describe('#loadUsers()', function () {

    beforeEach(function () {
      sinon.stub(App.db, "setSecureUserInfo", Em.K);
    });
    afterEach(function () {
      App.db.setSecureUserInfo.restore();
    });

    it('if defaultUserNameMap is empty then serviceUsers stays the same', function () {
      var configs = {};
      controller.set('serviceUsers', []);
      controller.set('userNameMap', {});
      controller.loadUsers(configs);
      expect(controller.get('serviceUsers')).to.be.empty;
    });

    it('if user config value is missing then use default', function () {
      var configs = {};
      controller.set('serviceUsers', []);
      controller.set('userNameMap', {
        test_user: {defaultValue: 'test', siteName: 'test-env', serviceName: 'TEST'
      }});
      controller.loadUsers(configs);
      expect(controller.get('serviceUsers')).to.eql([
        {
          "name": "test_user",
          "value": "test"
        }
      ]);
    });

    it('user config value has value', function () {
      var configs = {
        'test_user': 'config-value'
      };
      controller.set('serviceUsers', []);
      controller.set('defaultUserNameMap', {
        test_user: {defaultValue: 'test', siteName: 'test-env', serviceName: 'TEST'
        }});
      controller.loadUsers(configs);
      expect(controller.get('serviceUsers')).to.eql([
        {
          "name": "test_user",
          "value": "config-value"
        }
      ]);
    });
  });
});