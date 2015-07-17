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

describe('App.WidgetMixin', function () {
  var mixinClass = Em.Object.extend(App.WidgetMixin, {metrics: [], content: {}});

  describe('#loadMetrics()', function () {
    var mixinObject = mixinClass.create();
    beforeEach(function () {
      this.mock = sinon.stub(mixinObject, 'getRequestData');
      sinon.stub(App.WidgetLoadAggregator, 'add');
    });
    afterEach(function () {
      this.mock.restore();
      App.WidgetLoadAggregator.add.restore();
    });
    it('has host_component_criteria', function () {
      this.mock.returns({'key1': {host_component_criteria: 'criteria'}});
      mixinObject.set('isLoaded', false);
      mixinObject.loadMetrics();

      expect(App.WidgetLoadAggregator.add.calledOnce).to.be.true;
    });
    it('host_component_criteria is absent', function () {
      this.mock.returns({'key1': {}});
      mixinObject.set('isLoaded', false);
      mixinObject.loadMetrics();

      expect(App.WidgetLoadAggregator.add.calledOnce).to.be.true;
    });
  });

  describe("#extractExpressions()", function () {
    var mixinObject = mixinClass.create();
    var testCases = [
      {
        data: '',
        result: []
      },
      {
        data: 'text',
        result: []
      },
      {
        data: 'text${a}',
        result: ['a']
      },
      {
        data: 'text${a} - ${a.b}',
        result: ['a', 'a.b']
      },
      {
        data: '${o.a-(b+4)/cc*tt}',
        result: ['o.a-(b+4)/cc*tt']
      }
    ];
    testCases.forEach(function (test) {
      it('input: ' + test.data, function () {
        var input = {value: test.data};
        expect(mixinObject.extractExpressions(input)).to.eql(test.result);
      });
    });
  });

  describe("#getRequestData()", function () {
    var mixinObject = mixinClass.create();
    it("", function () {
      var data = [
        {
          "name": "regionserver.Server.percentFilesLocal",
          "metric_path": "metrics/hbase/regionserver/percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER"
        },
        {
          "name": "regionserver.Server.percentFilesLocal2",
          "metric_path": "w2",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER"
        },
        {
          "name": "regionserver.Server.percentFilesLocal",
          "metric_path": "metrics/hbase/regionserver/percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER",
          "host_component_criteria": 'c1'
        },
        {
          "name": "regionserver.Server.percentFilesLocal",
          "metric_path": "metrics/hbase/regionserver/percentFilesLocal",
          "service_name": "HDFS",
          "component_name": "DATANODE",
          "host_component_criteria": 'c1'
        }
      ];

      expect(JSON.stringify(mixinObject.getRequestData(data))).to.eql(JSON.stringify({
        "HBASE_HBASE_REGIONSERVER": {
          "name": "regionserver.Server.percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER",
          "metric_paths": [
            {
              "metric_path": "metrics/hbase/regionserver/percentFilesLocal",
              "metric_type": "POINT_IN_TIME",
              "id": "metrics/hbase/regionserver/percentFilesLocal_POINT_IN_TIME",
              "context": {}
            },
            {
              "metric_path": "w2",
              "metric_type": "POINT_IN_TIME",
              "id": "w2_POINT_IN_TIME",
              "context": {}
            }
          ]
        },
        "HBASE_HBASE_REGIONSERVER_c1": {
          "name": "regionserver.Server.percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER",
          "host_component_criteria": "c1",
          "metric_paths": [
            {
              "metric_path": "metrics/hbase/regionserver/percentFilesLocal",
              "metric_type": "POINT_IN_TIME",
              "id": "metrics/hbase/regionserver/percentFilesLocal_POINT_IN_TIME",
              "context": {}
            }
          ]
        },
        "HDFS_DATANODE_c1": {
          "name": "regionserver.Server.percentFilesLocal",
          "service_name": "HDFS",
          "component_name": "DATANODE",
          "host_component_criteria": "c1",
          "metric_paths": [
            {
              "metric_path": "metrics/hbase/regionserver/percentFilesLocal",
              "metric_type": "POINT_IN_TIME",
              "id": "metrics/hbase/regionserver/percentFilesLocal_POINT_IN_TIME",
              "context": {}
            }
          ]
        }
      }));
    });
  });

  describe("#getServiceComponentMetrics()", function () {
    var mixinObject = mixinClass.create();
    before(function () {
      sinon.stub(App.ajax, 'send');
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("", function () {
      var request = {
        service_name: 'S1',
        component_name: 'C1',
        metric_paths: [
          {
            "metric_path": "w1",
            "metric_type": "POINT_IN_TIME",
            "id": "w1_POINT_IN_TIME",
            "context": {}
          },
          {
            "metric_path": "w2",
            "metric_type": "POINT_IN_TIME",
            "id": "w2_POINT_IN_TIME",
            "context": {}
          }
        ]
      };
      mixinObject.getServiceComponentMetrics(request);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'widgets.serviceComponent.metrics.get',
        sender: mixinObject,
        data: {
          serviceName: 'S1',
          componentName: 'C1',
          metricPaths: 'w1,w2'
        }
      })
    });
  });

  describe("#getMetricsSuccessCallback()", function () {
    var mixinObject = mixinClass.create();
    it("", function () {
      var data = {
        metrics: {
          "hbase": {
            "ipc": {
              "IPC": {
                "numOpenConnections": 11.5
              }
            }
          }
        }
      };
      mixinObject.set('content.metrics', [
        {
          metric_path: 'metrics/hbase/ipc/IPC/numOpenConnections'
        }
      ]);
      mixinObject.getMetricsSuccessCallback(data);
      expect(mixinObject.get('metrics').findProperty('metric_path', 'metrics/hbase/ipc/IPC/numOpenConnections').data).to.equal(11.5);
    });
  });

  describe("#getHostComponentMetrics()", function () {
    var mixinObject = mixinClass.create();
    before(function () {
      sinon.stub(App.ajax, 'send');
      sinon.stub(mixinObject, 'computeHostComponentCriteria').returns('criteria')
    });
    after(function () {
      App.ajax.send.restore();
      mixinObject.computeHostComponentCriteria.restore();
    });
    it("", function () {
      var request = {
        component_name: 'C1',
        metric_paths: [
          {
            "metric_path": "w1",
            "metric_type": "POINT_IN_TIME",
            "id": "w1_POINT_IN_TIME",
            "context": {}
          },
          {
            "metric_path": "w2",
            "metric_type": "POINT_IN_TIME",
            "id": "w2_POINT_IN_TIME",
            "context": {}
          }
        ],
        host_component_criteria: 'c1'
      };
      mixinObject.getHostComponentMetrics(request);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'widgets.hostComponent.metrics.get',
        sender: mixinObject,
        data: {
          componentName: 'C1',
          metricPaths: 'w1,w2',
          hostComponentCriteria: 'criteria'
        }
      })
    });
  });

  describe("#calculateValues()", function () {
    var mixinObject = mixinClass.create();

    beforeEach(function () {
      sinon.stub(mixinObject, 'extractExpressions');
      this.mock = sinon.stub(mixinObject, 'computeExpression');
    });
    afterEach(function () {
      mixinObject.extractExpressions.restore();
      this.mock.restore();
    });
    it("value compute correctly", function () {
      this.mock.returns({'${a}': 1});
      mixinObject.set('content.values', [{
        value: '${a}'
      }]);
      mixinObject.calculateValues();
      expect(mixinObject.get('content.values')[0].computedValue).to.equal('1');
    });
    it("value not available", function () {
      this.mock.returns({});
      mixinObject.set('content.values', [{
        value: '${a}'
      }]);
      mixinObject.calculateValues();
      expect(mixinObject.get('content.values')[0].computedValue).to.equal('<span class="grey">n/a</span>');
    });
    it("value is null", function () {
      this.mock.returns({'${a}': null});
      mixinObject.set('content.values', [{
        value: '${a}'
      }]);
      mixinObject.calculateValues();
      expect(mixinObject.get('content.values')[0].computedValue).to.equal('<span class="grey">n/a</span>');
    });
  });

  describe("#computeExpression()", function () {
    var mixinObject = mixinClass.create();

    it("expression missing metrics", function () {
      var expressions = ['e.m1'];
      var metrics = [];
      expect(mixinObject.computeExpression(expressions, metrics)).to.eql({
        "${e.m1}": ""
      });
    });
    it("Value is not correct mathematical expression", function () {
      var expressions = ['e.m1'];
      var metrics = [{
        name: 'e.m1',
        data: 'a+1'
      }];
      expect(mixinObject.computeExpression(expressions, metrics)).to.eql({
        "${e.m1}": ""
      });
    });
    it("correct expression", function () {
      var expressions = ['e.m1+e.m1'];
      var metrics = [{
        name: 'e.m1',
        data: 1
      }];
      expect(mixinObject.computeExpression(expressions, metrics)).to.eql({
        "${e.m1+e.m1}": "2"
      });
    });
  });

  describe("#cloneWidget()", function () {
    var mixinObject = mixinClass.create();

    before(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(mixinObject, 'postWidgetDefinition', Em.K);
    });
    after(function () {
      App.showConfirmationPopup.restore();
      mixinObject.postWidgetDefinition.restore();
    });
    it("", function () {
      var popup = mixinObject.cloneWidget();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(mixinObject.postWidgetDefinition.calledOnce).to.be.true;
    });
  });

  describe("#postWidgetDefinition()", function () {
    var mixinObject = mixinClass.create();

    before(function () {
      sinon.spy(App.ajax, 'send');
      sinon.stub(mixinObject, 'collectWidgetData').returns({});
    });
    after(function () {
      App.ajax.send.restore();
      mixinObject.collectWidgetData.restore();
    });
    it("", function () {
      mixinObject.postWidgetDefinition();
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'widgets.wizard.add',
        sender: mixinObject,
        data: {
          data: {}
        },
        success: 'postWidgetDefinitionSuccessCallback'
      });
    });
  });
});


describe('App.WidgetLoadAggregator', function () {
  var aggregator = App.WidgetLoadAggregator;

  describe("#add()", function () {
    beforeEach(function () {
      sinon.stub(window, 'setTimeout').returns('timeId');
    });
    afterEach(function () {
      window.setTimeout.restore();
    });
    it("timeout started", function () {
      aggregator.set('timeoutId', 'timeId');
      aggregator.get('requests').clear();
      aggregator.add({});
      expect(aggregator.get('requests')).to.not.be.empty;
      expect(window.setTimeout.called).to.be.false;
    });
    it("timeout started", function () {
      aggregator.set('timeoutId', null);
      aggregator.get('requests').clear();
      aggregator.add({});
      expect(aggregator.get('requests')).to.not.be.empty;
      expect(window.setTimeout.calledOnce).to.be.true;
      expect(aggregator.get('timeoutId')).to.equal('timeId');
    });
  });

  describe("#groupRequests()", function () {
    it("", function () {
      var requests = [
        {
          startCallName: 'n1',
          data: {
            component_name: 'C1',
            metric_paths: ['m1']
          },
          context: Em.Object.create({
            content: {
              widgetType: 'GRAPH'
            }
          })
        },
        {
          startCallName: 'n1',
          data: {
            component_name: 'C1',
            metric_paths: ['m2']
          },
          context: Em.Object.create({
            content: {
              widgetType: 'NUMBER'
            }
          })
        },
        {
          startCallName: 'n2',
          data: {
            component_name: 'C1',
            metric_paths: ['m3']
          },
          context: Em.Object.create({
            content: {
              widgetType: 'TEMPLATE'
            }
          })
        },
        {
          startCallName: 'n1',
          data: {
            component_name: 'C2',
            metric_paths: ['m4']
          },
          context: Em.Object.create({
            content: {
              widgetType: 'GAUGE'
            }
          })
        }
      ];
      var result = aggregator.groupRequests(requests);

      expect(result['n1_C1'].subRequests.length).to.equal(1);
      expect(result['n1_C1'].data.metric_paths.length).to.equal(1);
      expect(result['n1_C1_graph'].subRequests.length).to.equal(1);
      expect(result['n1_C1_graph'].data.metric_paths.length).to.equal(1);
      expect(result['n2_C1'].subRequests.length).to.equal(1);
      expect(result['n2_C1'].data.metric_paths.length).to.equal(1);
      expect(result['n1_C2'].subRequests.length).to.equal(1);
      expect(result['n1_C2'].data.metric_paths.length).to.equal(1);
    });
  });

  describe("#runRequests()", function () {
    var mock = Em.Object.create({
      f1: function () {
        return {
          done: Em.K,
          complete: Em.K
        }
      },
      state: 'inDOM'
    });
    beforeEach(function () {
      sinon.stub(aggregator, 'groupRequests', function (requests) {
        return requests;
      });
      sinon.spy(mock, 'f1');
    });
    afterEach(function () {
      aggregator.groupRequests.restore();
      mock.f1.restore();
    });
    it("view in DOM", function () {
      var requests = {
        'r1': {
          data: {
            metric_paths: ['m1', 'm1', 'm2']
          },
          context: mock,
          startCallName: 'f1'
        }
      };
      aggregator.runRequests(requests);
      expect(mock.f1.calledWith(requests['r1'].data)).to.be.true;
    });
    it("view destroyed", function () {
      var requests = {
        'r1': {
          data: {
            metric_paths: ['m1', 'm1', 'm2']
          },
          context: mock,
          startCallName: 'f1'
        }
      };
      mock.set('state', 'destroyed');
      aggregator.runRequests(requests);
      expect(mock.f1.called).to.be.false;
    });
  });
});
