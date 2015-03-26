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
          serviceName: 'HIVE'
        })
      ]);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayNName: 'HDFS'
        }),
        Em.Object.create({
          serviceName: 'HIVE',
          displayNName: 'Hive'
        }),
        Em.Object.create({
          serviceName: 'HBASE',
          displayNName: 'HBase'
        }),
        Em.Object.create({
          serviceName: 'KNOX',
          displayNName: 'Knox'
        }),
        Em.Object.create({
          serviceName: 'STORM',
          displayNName: 'Storm'
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

  describe('#getRangerPluginsStatus', function () {

    var data = {
        'Clusters': {
          'desired_configs': {
            'ranger-hdfs-plugin-properties': {
              'tag': 'version1'
            },
            'ranger-hive-plugin-properties': {
              'tag': 'version2'
            },
            'ranger-hbase-plugin-properties': {
              'tag': 'version3'
            }
          }
        }
      },
      cases = [
        {
          isPreviousRangerConfigsCallFailed: false,
          ajaxRequestSent: true,
          title: 'initial case'
        },
        {
          isPreviousRangerConfigsCallFailed: true,
          hdfsTag: 'version1',
          hiveTag: 'version2',
          hbaseTag: 'version3',
          ajaxRequestSent: true,
          title: 'previous call failed'
        },
        {
          isPreviousRangerConfigsCallFailed: false,
          hdfsTag: 'version2',
          hiveTag: 'version2',
          hbaseTag: 'version3',
          ajaxRequestSent: true,
          title: 'configs changed'
        },
        {
          isPreviousRangerConfigsCallFailed: false,
          hdfsTag: 'version1',
          hiveTag: 'version2',
          hbaseTag: 'version3',
          ajaxRequestSent: false,
          title: 'configs unchanged'
        }
      ];

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS'
        }),
        Em.Object.create({
          serviceName: 'HIVE'
        }),
        Em.Object.create({
          serviceName: 'HBASE'
        })
      ]);
    });

    afterEach(function () {
      App.ajax.send.restore();
      App.Service.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isPreviousRangerConfigsCallFailed', item.isPreviousRangerConfigsCallFailed);
        controller.get('rangerPlugins').findProperty('serviceName', 'HDFS').tag = item.hdfsTag;
        controller.get('rangerPlugins').findProperty('serviceName', 'HIVE').tag = item.hiveTag;
        controller.get('rangerPlugins').findProperty('serviceName', 'HBASE').tag = item.hbaseTag;
        controller.getRangerPluginsStatus(data);
        expect(App.ajax.send.calledOnce).to.equal(item.ajaxRequestSent);
      });
    });

  });

  describe('#getRangerPluginsStatusSuccess', function () {
    it('relevant plugin statuses are set', function () {
      controller.getRangerPluginsStatusSuccess({
        'items': [
          {
            'type': 'ranger-hdfs-plugin-properties',
            'properties': {
              'ranger-hdfs-plugin-enabled': 'Yes'
            }
          },
          {
            'type': 'ranger-hive-plugin-properties',
            'properties': {
              'ranger-hive-plugin-enabled': 'No'
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
      expect(controller.get('isPreviousRangerConfigsCallFailed')).to.be.false;
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HDFS').status).to.equal(Em.I18n.t('alerts.table.state.enabled'));
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HIVE').status).to.equal(Em.I18n.t('alerts.table.state.disabled'));
      expect(controller.get('rangerPlugins').findProperty('serviceName', 'HBASE').status).to.equal(Em.I18n.t('common.unknown'));
    });
  });

  describe('#getRangerPluginsStatusError', function () {

    it('should set isPreviousRangerConfigsCallFailed to true', function () {
      controller.getRangerPluginsStatusError();
      expect(controller.get('isPreviousRangerConfigsCallFailed')).to.be.true;
    });

  });

  describe("#loadWidgets()", function () {
    before(function () {
      controller = App.MainServiceInfoSummaryController.create();
      sinon.stub(App.ajax, 'send');
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make GET call", function () {
      controller.loadWidgets();
      expect(App.ajax.send.getCall(0).args[0].name).to.equal('widgets.layout.userDefined.get');
    });
  });

  describe("#loadWidgetsSuccessCallback()", function () {
    beforeEach(function () {
      controller = App.MainServiceInfoSummaryController.create();
      sinon.stub(App.widgetMapper, 'map');
      sinon.stub(controller, 'loadStackWidgetsLayout');
    });
    afterEach(function () {
      App.widgetMapper.map.restore();
      controller.loadStackWidgetsLayout.restore();
    });
    it("empty data", function () {
      controller.loadWidgetsSuccessCallback({items: []});
      expect(controller.loadStackWidgetsLayout.calledOnce).to.be.true;
    });
    it("filled data", function () {
      controller.loadWidgetsSuccessCallback({items: ['1']});
      expect(App.widgetMapper.map.calledWith('1')).to.be.true;
    });
  });

  describe("#loadStackWidgetsLayout()", function () {
    before(function () {
      controller = App.MainServiceInfoSummaryController.create();
      sinon.stub(App.ajax, 'send');
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make GET call", function () {
      controller.loadStackWidgetsLayout();
      expect(App.ajax.send.getCall(0).args[0].name).to.equal('widgets.layout.stackDefined.get');
    });
  });

  describe("#loadStackWidgetsLayoutSuccessCallback()", function () {
    before(function () {
      controller = App.MainServiceInfoSummaryController.create();
      sinon.stub(App.widgetMapper, 'map');
    });
    after(function () {
      App.widgetMapper.map.restore();
    });
    it("make GET call", function () {
      var data = {
        artifact_data: {
          layouts: [
            {
              section_name: 'S1_SUMMARY'
            }
          ]
        }
      };
      controller.set('content', Em.Object.create({serviceName: 'S1'}));
      controller.loadStackWidgetsLayoutSuccessCallback(data);
      expect(App.widgetMapper.map.calledWith({
        section_name: 'S1_SUMMARY'
      })).to.be.true;
    });
  });
});