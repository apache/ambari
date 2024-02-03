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

App = require('app');

require('controllers/main/service/widgets/create/wizard_controller');


describe('App.WidgetWizardController', function () {
  var controller;

  beforeEach(function () {
    controller = App.WidgetWizardController.create({
      content: Em.Object.create()
    });
  });

  /**
   * tests the function with following hierarchical queue scenario
   *                   root 
   *                    |
   *                  queue1
   *                 /     \
   *             queue2   queue3
   *                  
   */
  describe("#substitueQueueMetrics", function () {
    beforeEach(function () {
      controller = App.WidgetWizardController.create();
      sinon.stub(App.YARNService, 'find', function () {
        return Em.Object.create({
          'allQueueNames': ["root", "root/queue1", "root/queue1/queue2", "root/queue1/queue3"]
        });
      });
    });
    afterEach(function () {
      App.YARNService.find.restore();
    });


    var testCases = [
      {
        msg: 'AMS Queue metric with regex as name and regex as path should be replaced with actual metric name and path of all existing queues',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            level: 'COMPONENT',
            name: 'yarn.QueueMetrics.Queue=(.+).AppsFailed',
            point_in_time: false,
            service_name: 'YARN',
            temporal: true,
            type: 'GANGLIA',
            widget_id: 'metrics/yarn/Queue/$1.replaceAll("([.])","/")/AppsFailed'
          }
        ],
        expectedResult: [
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/AppsFailed"
          },
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.queue1.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/queue1/AppsFailed"
          },
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.queue1.queue2.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/queue1/queue2/AppsFailed"
          },
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.queue1.queue3.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/queue1/queue3/AppsFailed"
          }
        ]
      },
      {
        msg: 'JMX Queue metric with regex as name and regex as path should be replaced with actual metric name and path of all existing queues',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/$1.replaceAll(",q(\d+)=","/").substring(1)/AppsFailed'
          }
        ],
        expectedResult: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/AppsFailed'
          },
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root,q1=queue1.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/queue1/AppsFailed'
          },
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root,q1=queue1,q2=queue2.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/queue1/queue2/AppsFailed'
          },
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root,q1=queue1,q2=queue3.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/queue1/queue3/AppsFailed'
          }
        ]
      },
      {
        msg: 'AMS Queue metric without regex in name and path should retain same name and path',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            level: 'COMPONENT',
            name: 'yarn.QueueMetrics.Queue.Clustermetrics.AppsFailed',
            point_in_time: false,
            service_name: 'YARN',
            temporal: true,
            type: 'GANGLIA',
            widget_id: 'metrics/yarn/Queue/Clustermetrics/AppsFailed'
          }
        ],
        expectedResult: [
          {
            component_name: 'RESOURCEMANAGER',
            level: 'COMPONENT',
            name: 'yarn.QueueMetrics.Queue.Clustermetrics.AppsFailed',
            point_in_time: false,
            service_name: 'YARN',
            temporal: true,
            type: 'GANGLIA',
            widget_id: 'metrics/yarn/Queue/Clustermetrics/AppsFailed'
          }
        ]
      },
      {
        msg: 'JMX Queue metric without regex in name and path should retain same name and path',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics.clusterMetric.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/clusterMetric/AppsFailed'
          }
        ],
        expectedResult: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics.clusterMetric.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/clusterMetric/AppsFailed'
          }
        ]
      }
    ];
    testCases.forEach(function (_testCase) {
      it(_testCase.msg, function () {
        var result = controller.substitueQueueMetrics(_testCase.inputMetrics);
        expect(JSON.stringify(result)).to.equal(JSON.stringify(_testCase.expectedResult));
      });
    });
  });

  describe('#loadMap', function() {

    describe('should load widget service', function() {
      var loadWidgetService = false;
      var loadWidgetType = false;
      var loadLayout = false;
      var checker = {
        load: function(arg) {
          switch (arg) {
            case 'widgetService':
              loadWidgetService = true;
              break;
            case 'widgetType':
              loadWidgetType = true;
              break;
            case 'layout':
              loadLayout = true;
              break;
          }
        }
      };

      beforeEach(function () {
        controller.loadMap['1'][0].callback.call(checker);
      });

      it('widget service is loaded', function () {
        expect(loadWidgetService).to.be.true;
      });

      it('widget type is loaded', function () {
        expect(loadWidgetType).to.be.true;
      });

      it('layout is loaded', function () {
        expect(loadLayout).to.be.true;
      });
    });

    describe('should load widget data', function() {
      var widgetProperties = false;
      var widgetValues = false;
      var widgetMetrics = false;
      var expressions = false;
      var dataSets = false;
      var templateValue = false;
      var checker = {
        load: function(arg) {
          switch (arg) {
            case 'widgetProperties':
              widgetProperties = true;
              break;
            case 'widgetValues':
              widgetValues = true;
              break;
            case 'widgetMetrics':
              widgetMetrics = true;
              break;
            case 'expressions':
              expressions = true;
              break;
            case 'dataSets':
              dataSets = true;
              break;
            case 'templateValue':
              templateValue = true;
              break;
          }
        }
      };

      beforeEach(function () {
        controller.loadMap['2'][0].callback.call(checker);
      });

      it('widget properties are loaded', function () {
        expect(widgetProperties).to.be.true;
      });

      it('widget values are loaded', function () {
        expect(widgetValues).to.be.true;
      });

      it('widget metrics is loaded', function () {
        expect(widgetMetrics).to.be.true;
      });

      it('expressions are loaded', function () {
        expect(expressions).to.be.true;
      });

      it('data sets are loaded', function () {
        expect(dataSets).to.be.true;
      });

      it('template value is loaded', function () {
        expect(templateValue).to.be.true;
      });
    });

    describe('should load all metrics', function() {
      var loadAllMetrics = false;

      var checker = {
        loadAllMetrics: function() {
          loadAllMetrics = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['2'][1].callback.call(checker);
      });

      it('all metrics are loaded', function () {
        expect(loadAllMetrics).to.be.true;
      });
    });
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'saveClusterStatus');
    });

    afterEach(function() {
      controller.saveClusterStatus.restore();
    });

    it("should set current step", function() {
      controller.setCurrentStep('1', '0', false);
      expect(controller.saveClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#saveClusterStatus()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("should set cluster status", function() {
      controller.saveClusterStatus('status');
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#save()", function () {

    it("should save properties", function() {
      controller.save('name1', 'value1');
      expect(controller.get('content.name1')).to.equal('value1');
    });
  });

  describe("#loadAllMetrics()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(controller, 'getDBProperty');
      sinon.spy(controller, 'loadAllMetricsFromServer');
    });

    afterEach(function() {
      this.mock.restore();
      controller.loadAllMetricsFromServer.restore();
    });

    it("should call 'loadAllMetricsFromServer'", function() {
      this.mock.returns([]);
      controller.loadAllMetrics();
      expect(controller.getDBProperty.calledOnce).to.be.true;
      expect(controller.loadAllMetricsFromServer.calledOnce).to.be.true;
    });

    it("should set all metrics", function() {
      this.mock.returns(['m']);
      controller.loadAllMetrics();
      expect(controller.getDBProperty.calledOnce).to.be.true;
      expect(controller.get('content.isMetricsLoaded')).to.be.true;
      expect(controller.get('content.allMetrics')).to.eql(['m']);
    });
  });

  describe("#insertHostComponentCriteria()", function () {

    it("host component criteria for NAMENODE", function() {
      var metricObj = {component_name: 'NAMENODE'};
      controller.insertHostComponentCriteria(metricObj);
      expect(metricObj.host_component_criteria).to.equal('host_components/metrics/dfs/FSNamesystem/HAState=active');
    });

    it("host component criteria for RESOURCEMANAGER", function() {
      var metricObj = {component_name: 'RESOURCEMANAGER'};
      controller.insertHostComponentCriteria(metricObj);
      expect(metricObj.host_component_criteria).to.equal('host_components/HostRoles/ha_state=ACTIVE');
    });

    it("host component criteria for HBASE_MASTER", function() {
      var metricObj = {component_name: 'HBASE_MASTER'};
      controller.insertHostComponentCriteria(metricObj);
      expect(metricObj.host_component_criteria).to.equal('host_components/metrics/hbase/master/IsActiveMaster=true');
    });

    it("host component criteria default component", function() {
      var metricObj = {component_name: 'HIVE'};
      controller.insertHostComponentCriteria(metricObj);
      expect(metricObj.host_component_criteria).to.equal(' ');
    });
  });

  describe("#postWidgetDefinition()", function () {

    it("should send ajax request", function() {
      controller.postWidgetDefinition({});
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#addWidgetToLayout()", function () {
    var mockService = {
      saveWidgetLayout: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mockService);
      sinon.spy(mockService, 'saveWidgetLayout');
    });

    afterEach(function() {
      App.router.get.restore();
      mockService.saveWidgetLayout.restore();
    });

    it("should save widget layout", function () {
      var newWidget = {id: 2},
        layout = {
          widgets: [
            {
              id: 1
            }
          ]
        };
      controller.addWidgetToLayout(newWidget, layout);
      expect(mockService.saveWidgetLayout.calledOnce).to.be.true;
    });
  });

  describe("#loadLayoutByName()", function () {

    it("should send ajax request", function() {
      controller.loadLayoutByName('name');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#clearAllSteps()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({});
    });

    afterEach(function() {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });

    it("should clear install options and set cluster content", function() {
      controller.clearAllSteps();
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
      expect(controller.getCluster.calledOnce).to.be.true;
      expect(controller.get('content.cluster')).to.eql({});
    });
  });

  describe("#clearTasksData()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'saveTasksStatuses');
      sinon.stub(controller, 'saveRequestIds');
      sinon.stub(controller, 'saveTasksRequestIds');
    });

    afterEach(function() {
      controller.saveTasksStatuses.restore();
      controller.saveRequestIds.restore();
      controller.saveTasksRequestIds.restore();
    });

    it("should clear tasks data", function() {
      controller.clearTasksData();
      expect(controller.saveTasksStatuses.calledWith(undefined)).to.be.true;
      expect(controller.saveRequestIds.calledWith(undefined)).to.be.true;
      expect(controller.saveTasksRequestIds.calledWith(undefined)).to.be.true;
    });
  });

  describe("#cancel()", function () {

    beforeEach(function() {
      sinon.stub(App.ModalPopup, 'show');
    });

    afterEach(function() {
      App.ModalPopup.show.restore();
    });

    it("should show alert popup", function() {
      controller.cancel();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe("#finishWizard()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'finish');
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      controller.finish.restore();
      App.clusterStatus.setClusterStatus.restore();
    });

    it("should show alert popup", function() {
      controller.finishWizard();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#finish()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setCurrentStep');
      sinon.stub(controller, 'save');
      sinon.stub(controller, 'resetDbNamespace');
    });

    afterEach(function() {
      controller.setCurrentStep.restore();
      controller.save.restore();
      controller.resetDbNamespace.restore();
    });

    it("should clear tasks data", function() {
      controller.finish();
      expect(controller.setCurrentStep.calledWith('1', false, true)).to.be.true;
      expect(controller.save.calledWith('widgetType', '')).to.be.true;
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });
  });

  describe("#loadAllMetricsFromServerCallback()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(['c1']);
      sinon.stub(App.YARNService, 'find', function () {
        return Em.Object.create({
          'allQueueNames': ["root", "root/queue1", "root/queue1/queue2", "root/queue1/queue3"]
        });
      });
      sinon.stub(controller, 'save');
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.YARNService.find.restore();
      controller.save.restore();
    });

    it("should save all metrics", function() {
      var result = [{
        component_name: "c1",
        host_component_criteria: " ",
        level: "HOSTCOMPONENT",
        name: "m1",
        point_in_time: 1602063606,
        service_name: "s1",
        temporal: 1000,
        type: "GANGLIA",
        widget_id: "w1"
      }];
      var json = {
        items: [
          {
            StackServices: {
              service_name: 's1'
            },
            artifacts: [
              {
                artifact_data: {
                  s1: {
                    'c1': {
                      'HOSTCOMPONENT': [
                        {
                          type: 'Ganglia',
                          metrics: {
                            default: {
                              'w1': {
                                pointInTime: 1602063606,
                                temporal: 1000,
                                name: 'm1'
                              }
                            }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            ]
          }
        ]
      };
      controller.loadAllMetricsFromServerCallback(json);
      expect(controller.save.calledWith('allMetrics', result)).to.be.true;
    });
  });

  describe("#postWidgetDefinitionSuccessCallback()", function () {
    var data = {
      resources: [
        {
          WidgetInfo: {}
        }
      ]
    };

    beforeEach(function () {
      sinon.stub(controller, 'loadLayoutByName').returns({
        done: function (callback) {
          callback({
            items: [
              {
                WidgetLayoutInfo: {
                  widgets: [
                    {
                      WidgetInfo: {
                        id: 1
                      }
                    }
                  ]
                }
              }
            ]
          });
        }
      });
      sinon.stub(controller, 'addWidgetToLayout').returns({
        done: Em.clb
      });
      sinon.stub(App.router, 'get').withArgs('mainServiceInfoMetricsController').returns(Em.Object.create({
        userLayoutName: 'l1',
        updateActiveLayout: Em.K
      }));
    });

    afterEach(function() {
      controller.loadLayoutByName.restore();
      controller.addWidgetToLayout.restore();
      App.router.get.restore();
    });

    it("should call 'loadLayoutByName'", function() {
      var requestData = {
        data: {
          WidgetInfo: {
            tag: 1
          }
        }
      };
      controller.set('content.layout', {});
      controller.postWidgetDefinitionSuccessCallback(data, {}, requestData);
      expect(controller.loadLayoutByName.calledTwice).to.be.true;
      expect(controller.addWidgetToLayout.calledTwice).to.be.true;
    });

    it("should call 'addWidgetToLayout' once", function() {
      var requestData = {
        data: {
          WidgetInfo: {
            tag: null
          }
        }
      };
      controller.set('content.layout', {});
      controller.postWidgetDefinitionSuccessCallback(data, {}, requestData);
      expect(controller.loadLayoutByName.called).to.be.false;
      expect(controller.addWidgetToLayout.calledOnce).to.be.true;
    });
  });
});
