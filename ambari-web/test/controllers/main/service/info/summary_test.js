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
require('controllers/main/service/info/summary');
function getController() {
  return App.MainServiceInfoSummaryController.create();
}

describe('App.MainServiceInfoSummaryController', function () {

  var controller;

  beforeEach(function () {
    controller = App.MainServiceInfoSummaryController.create();
  });

  describe('#setRangerPlugins', function () {

    var cases = [
      {
        isLoaded: true,
        isRangerPluginsArraySet: false,
        expectedIsRangerPluginsArraySet: true,
        title: 'cluster loaded, ranger plugins array not set'
      },
      {
        isLoaded: false,
        isRangerPluginsArraySet: false,
        expectedIsRangerPluginsArraySet: false,
        title: 'cluster not loaded, ranger plugins array not set'
      },
      {
        isLoaded: false,
        isRangerPluginsArraySet: true,
        expectedIsRangerPluginsArraySet: true,
        title: 'cluster not loaded, ranger plugins array set'
      },
      {
        isLoaded: true,
        isRangerPluginsArraySet: true,
        expectedIsRangerPluginsArraySet: true,
        title: 'cluster loaded, ranger plugins array set'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS'
        }),
        Em.Object.create({
          serviceName: 'YARN'
        }),
        Em.Object.create({
          serviceName: 'HIVE'
        })
      ]);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayName: 'HDFS',
          configTypes: {
            'ranger-hdfs-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'HIVE',
          displayName: 'Hive',
          configTypes: {
            'hive-env': {}
          }
        }),
        Em.Object.create({
          serviceName: 'HBASE',
          displayName: 'HBase',
          configTypes: {
            'ranger-hbase-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'KNOX',
          displayName: 'Knox',
          configTypes: {
            'ranger-knox-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'STORM',
          displayName: 'Storm',
          configTypes: {
            'ranger-storm-plugin-properties': {}
          }
        }),
        Em.Object.create({
          serviceName: 'YARN',
          displayName: 'YARN',
          configTypes: {}
        })
      ]);
    });

    afterEach(function () {
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isRangerPluginsArraySet', item.isRangerPluginsArraySet);
        App.set('router.clusterController.isLoaded', item.isLoaded);
        expect(controller.get('isRangerPluginsArraySet')).to.equal(item.expectedIsRangerPluginsArraySet);
        expect(controller.get('rangerPlugins').filterProperty('isDisplayed').mapProperty('serviceName').sort()).to.eql(['HDFS', 'HIVE']);
      });
    });

  });

  describe('#getRangerPluginsStatusSuccess', function () {

    beforeEach(function () {
      controller.getRangerPluginsStatusSuccess({
        'items': [
          {
            'type': 'ranger-hdfs-plugin-properties',
            'properties': {
              'ranger-hdfs-plugin-enabled': 'Yes'
            }
          },
          {
            'type': 'hive-env',
            'properties': {
              'hive_security_authorization': 'Ranger'
            }
          },
          {
            'type': 'ranger-hbase-plugin-properties',
            'properties': {
              'ranger-hbase-plugin-enabled': ''
            }
          }
        ]
      });
    });

    it('isPreviousRangerConfigsCallFailed is false', function () {
      expect(controller.get('isPreviousRangerConfigsCallFailed')).to.be.false;
    });
    it('rangerPlugins.HDFS status is valid', function () {
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HDFS').status).to.equal(Em.I18n.t('alerts.table.state.enabled'));
    });
    it('rangerPlugins.HIVE status is valid', function () {
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HIVE').status).to.equal(Em.I18n.t('alerts.table.state.enabled'));
    });
    it('rangerPlugins.HBASE status is valid', function () {
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HBASE').status).to.equal(Em.I18n.t('common.unknown'));
    });
  });

  describe('#getRangerPluginsStatusError', function () {

    it('should set isPreviousRangerConfigsCallFailed to true', function () {
      controller.getRangerPluginsStatusError();
      expect(controller.get('isPreviousRangerConfigsCallFailed')).to.be.true;
    });

  });

  describe('#updateRangerPluginsStatus', function () {

    it('should call ajax send request', function () {
      controller.updateRangerPluginsStatus();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#getRangerPluginsStatus', function () {

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns([
        {
          serviceName: 'HDFS'
        }
      ]);
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    it('should call ajax send request', function () {
      var data = {
        Clusters: {
          desired_configs: {
            'ranger-hdfs-plugin-properties': {
              tag: 1
            }
          }
        }
      };
      controller.getRangerPluginsStatus(data);
      expect(controller.get('rangerPlugins').filterProperty('isDisplayed').length).to.equal(1);
      expect(controller.get('rangerPlugins').filterProperty('isDisplayed', false).length).to.equal(8);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#startFlumeAgent', function () {

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationPopup', function (callback) {
          return callback()
        }
      );
      sinon.stub(controller, 'sendFlumeAgentCommandToServer');
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('should show confirmation popup', function () {

      controller.startFlumeAgent({context: Em.Object.create({
        status: 'NOT_RUNNING'
      })});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.sendFlumeAgentCommandToServer.calledOnce).to.be.true;
    });
  });

  describe('#stopFlumeAgent', function () {

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationPopup', function (callback) {
          return callback()
        }
      );
      sinon.stub(controller, 'sendFlumeAgentCommandToServer');
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('should show confirmation popup', function () {

      controller.stopFlumeAgent({context: Em.Object.create({
          status: 'RUNNING'
        })});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.sendFlumeAgentCommandToServer.calledOnce).to.be.true;
    });
  });

  describe('#sendFlumeAgentCommandToServer', function () {

    it('should call ajax send request', function () {
      controller.sendFlumeAgentCommandToServer('', '', Em.Object.create({name: 'n', hostName: 'h'}));
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#commandSuccessCallback", function () {
    var mock = Em.Object.create({
      showPopup: Em.K,
      dataLoading: function () {
        return {
          done: function (callback) {
            return callback(1)
          }
        }
      }
    });

    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'showPopup');
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it("should show popup", function () {
      controller.commandSuccessCallback();
      expect(mock.showPopup.calledOnce).to.be.true;
    });
  });

  describe('#gotoConfigs', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(Em.Object.create({routeToConfigs: false}));
      sinon.stub(App.router, 'transitionTo');
    });

    afterEach(function () {
      App.router.get.restore();
      App.router.transitionTo.restore();
    });

    it('should go to configs route', function () {
      controller.gotoConfigs();
      expect(App.router.get.calledTwice).to.be.true;
      expect(App.router.transitionTo.calledOnce).to.be.true;
    });
  });

  describe('#goToView', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'route');
    });

    afterEach(function () {
      App.router.route.restore();
    });

    it('should go to view', function () {
      controller.goToView({context: Em.Object.create({internalAmbariUrl: 'url'})});
      expect(App.router.route.calledWith('url')).to.be.true;
    });
  });

  describe('#showServiceAlertsPopup', function () {

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show');
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('should show modal popup', function () {
      controller.showServiceAlertsPopup({
        context: Em.Object.create({
          displayName: 'n',
          componentName: 'c'
        })
      });
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe("#setHiveEndPointsValue", function () {
    var configs = [
      {
        type: 'hive-interactive-site',
        properties: {
          'hive.server2.active.passive.ha.registry.namespace': 'ns1'
        }
      },
      {
        type: 'hive-site',
        properties: {
          'hive.server2.support.dynamic.service.discovery': true,
          'hive.zookeeper.quorum': 'zc',
          'hive.server2.zookeeper.namespace': 'ns1'
        }
      }
    ];
    var mock = Em.Object.create({
      getCurrentConfigsBySites: function () {
        return {
          done: function (callback) {
            return callback(configs)
          }
        }
      }
    });

    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(App.MasterComponent, 'find').returns([
        Em.Object.create({
          totalCount: 1,
          componentName: 'HIVE_SERVER',
          displayName: 'HIVE_SERVER_INTERACTIVE'
        }),
        Em.Object.create({
          totalCount: 1,
          componentName: 'HIVE_SERVER_INTERACTIVE',
          displayName: 'HIVE_SERVER_INTERACTIVE'
        })
      ]);
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'HIVE_SERVER_INTERACTIVE'
        }),
        Em.Object.create({
          componentName: 'HIVE_SERVER_INTERACTIVE'
        })
      ]);
    });

    afterEach(function () {
      App.router.get.restore();
      App.MasterComponent.find.restore();
      App.HostComponent.find.restore();
    });

    it("should set hive endpoints value", function () {
      controller.setHiveEndPointsValue();
      expect(JSON.stringify(controller.get('hiveServerEndPoints'))).to.equal('[{"isVisible":true,"componentName":"HIVE_SERVER","label":"HIVE_SERVER_INTERACTIVE JDBC URL","value":"jdbc:hive2://zc/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=ns1","tooltipText":"JDBC connection string for HIVE_SERVER_INTERACTIVE"},{"isVisible":true,"componentName":"HIVE_SERVER_INTERACTIVE","label":"HIVE_SERVER_INTERACTIVE JDBC URL","value":"jdbc:hive2://zc/;serviceDiscoveryMode=zooKeeperHA;zooKeeperNamespace=ns1","tooltipText":"JDBC connection string for HIVE_SERVER_INTERACTIVE"}]');
    });
  });
});