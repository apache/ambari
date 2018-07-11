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
var testHelpers = require('test/helpers');

describe('App.WidgetMixin', function () {
  var mixinClass = Em.Object.extend(App.WidgetMixin, {metrics: [], content: {}});
  var mixinObject;

  beforeEach(function () {
    mixinObject = mixinClass.create();
  });

  afterEach(function () {
    clearTimeout(mixinObject.get('timeoutId'));
    mixinObject.destroy();
  });


  describe('#VALUE_NAME_REGEX', function() {
    var testCases = [
      {
        data: 'a-a - b',
        expected: ['a-a', 'b']
      },
      {
        data: 'a.a / b',
        expected: ['a.a', 'b']
      },
      {
        data: 'a=a * b',
        expected: ['a=a', 'b']
      },
      {
        data: 'a,a + b',
        expected: ['a,a', 'b']
      },
      {
        data: 'a:a + b',
        expected: ['a:a', 'b']
      },
      {
        data: 'a[a] + b',
        expected: ['a[a]', 'b']
      },
      {
        data: 'Total tasks - Free tasks',
        expected: ['Total tasks', 'Free tasks']
      },
      {
        data: 'Hadoop:service=azure-file-system,name=AzureFileSystemMetrics4 - 1',
        expected: ['Hadoop:service=azure-file-system,name=AzureFileSystemMetrics4', '1']
      }
    ];
    var regex = mixinClass.create().get('VALUE_NAME_REGEX');

    testCases.forEach(function(test) {
      it(test.data + ' should match ' + test.expected, function() {
        expect(test.data.match(regex)).to.be.eql(test.expected);
      });
    });
  });

  describe('#loadMetrics()', function () {
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
    it('input is null', function () {
      var input = null;
      expect(mixinObject.extractExpressions(input)).to.be.empty;
    });
  });

  describe("#getRequestData()", function () {
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

    beforeEach(function () {
      this.requestData = mixinClass.create().getRequestData(data);
    });

    it('HBASE_HBASE_REGIONSERVER', function () {
      var hbaseRegionServer = {
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
        };
      expect(JSON.stringify(this.requestData.HBASE_HBASE_REGIONSERVER)).to.equal(JSON.stringify(hbaseRegionServer));
    });

    it('HBASE_HBASE_REGIONSERVER_c1', function () {
      var hbaseRegionServerC1 = {
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
      };
      expect(JSON.stringify(this.requestData.HBASE_HBASE_REGIONSERVER_c1)).to.equal(JSON.stringify(hbaseRegionServerC1));
    });

    it('HDFS_DATANODE_c1', function () {
      var hdfsDataNodeC1 = {
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
      };
      expect(JSON.stringify(this.requestData.HDFS_DATANODE_c1)).to.equal(JSON.stringify(hdfsDataNodeC1));
    });

  });

  describe("#getServiceComponentMetrics()", function () {

    it("valid request is sent", function () {
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
      var args = testHelpers.findAjaxRequest('name', 'widgets.serviceComponent.metrics.get');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(mixinObject);
      expect(args[0].data).to.be.eql({
        serviceName: 'S1',
        componentName: 'C1',
        metricPaths: 'w1,w2'
      });
    });
  });

  describe("#getMetricsSuccessCallback()", function () {
    it("metric is mapped from provided path", function () {
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

  describe("#disableGraph", function () {
    var graph = Em.Object.create({
        hasData: true,
        _showMessage: Em.K
      }),
      cases = [
        {
          graphView: null,
          childViews: [],
          hasData: true,
          isExportButtonHidden: false,
          showMessageCallCount: 0,
          title: 'no graph'
        },
        {
          graphView: {},
          childViews: [{}],
          hasData: true,
          isExportButtonHidden: false,
          showMessageCallCount: 0,
          title: 'no graph view rendered'
        },
        {
          graphView: {},
          childViews: [
            {},
            graph
          ],
          hasData: false,
          isExportButtonHidden: true,
          showMessageCallCount: 1,
          title: 'graph view rendered'
        }
      ];

    cases.forEach(function (item) {
      describe(item.title, function () {
        beforeEach(function() {
          sinon.stub(mixinObject, 'loadMetrics');
          mixinObject.setProperties({
            isExportButtonHidden: false,
            childViews: item.childViews,
            graphView: item.graphView,
            metrics: [
              {
                name: 'm1'
              },
              {
                name: 'm2'
              }
            ],
            content: {
              metrics: [
                {
                  name: 'm2'
                }
              ]
            }
          });
          sinon.stub(graph, '_showMessage');
          mixinObject.disableGraph();
        });

        afterEach(function() {
          mixinObject.loadMetrics.restore();
          graph._showMessage.restore();
        });

        it('hasData', function() {
          expect(graph.get('hasData')).to.equal(item.hasData);
        });

        it('isExportButtonHidden', function() {
          expect(mixinObject.get('isExportButtonHidden')).to.equal(item.isExportButtonHidden);
        });

        it('_showMessage call count', function() {
          expect(graph._showMessage.callCount).to.equal(item.showMessageCallCount);
        });

        it('metrics should be filtered', function() {
          expect(mixinObject.get('metrics').mapProperty('name')).to.eql(['m1']);
        });
      });
    });
  });

  describe("#getHostComponentMetrics()", function () {
    beforeEach(function () {
      sinon.stub(mixinObject, 'computeHostComponentCriteria').returns('criteria')
    });
    afterEach(function () {
      mixinObject.computeHostComponentCriteria.restore();
    });
    it("valid request is sent", function () {
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
      var args = testHelpers.findAjaxRequest('name', 'widgets.hostComponent.metrics.get');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(mixinObject);
      expect(args[0].data).to.be.eql({
        componentName: 'C1',
        metricPaths: 'w1,w2',
        hostComponentCriteria: 'criteria'
      });
    });
  });

  describe("#calculateValues()", function () {

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
    var popup;
    beforeEach(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(mixinObject, 'postWidgetDefinition', Em.K);
      popup = mixinObject.cloneWidget();
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      mixinObject.postWidgetDefinition.restore();
    });
    it("popup is shown", function () {
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
    it('postWidgetDefinition is called', function () {
      popup.onPrimary();
      expect(mixinObject.postWidgetDefinition.calledOnce).to.be.true;
    });
  });

  describe("#postWidgetDefinition()", function () {
    beforeEach(function () {
      sinon.stub(mixinObject, 'collectWidgetData').returns({});
    });
    afterEach(function () {
      mixinObject.collectWidgetData.restore();
    });
    it("valid request is sent", function () {
      mixinObject.postWidgetDefinition();
      var args = testHelpers.findAjaxRequest('name', 'widgets.wizard.add');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(mixinObject);
      expect(args[0].data).to.be.eql({
        data: {}
      });
    });
  });

  describe('#getMetricsErrorCallback()', function () {

    var obj,
      view = Em.Object.create({
        _showMessage: Em.K
      }),
      metrics = [
        {
          name: 'n0'
        },
        {
          name: 'n1'
        }
      ],
      cases = [
        {
          graphView: null,
          isForcedAbort: false,
          metrics: metrics,
          showMessageCallCount: 0,
          isExportButtonHidden: false,
          title: 'no graph view'
        },
        {
          graphView: {},
          isForcedAbort: false,
          metrics: metrics,
          showMessageCallCount: 0,
          isExportButtonHidden: false,
          title: 'no childViews property'
        },
        {
          graphView: {},
          childViews: [],
          isForcedAbort: false,
          metrics: metrics,
          showMessageCallCount: 0,
          isExportButtonHidden: false,
          title: 'no child views'
        },
        {
          graphView: {},
          childViews: [Em.Object.create({})],
          isForcedAbort: false,
          metrics: metrics,
          showMessageCallCount: 0,
          isExportButtonHidden: false,
          title: 'no view with _showMessage method'
        },
        {
          graphView: {},
          childViews: [Em.Object.create({}), view],
          isForcedAbort: false,
          metrics: [
            {
              name: 'n1'
            }
          ],
          showMessageCallCount: 1,
          isExportButtonHidden: true,
          title: 'graph view is available'
        },
        {
          graphView: {},
          childViews: [Em.Object.create({}), view],
          isForcedAbort: true,
          metrics: metrics,
          showMessageCallCount: 0,
          isExportButtonHidden: false,
          title: 'request is aborted'
        }
      ],
      messageCases = [
        {
          readyState: 2,
          status: 0,
          textStatus: 'error',
          title: 'incomplete request'
        },
        {
          readyState: 4,
          status: 0,
          textStatus: 'error',
          title: 'no status code'
        },
        {
          readyState: 4,
          status: 404,
          textStatus: '404 error',
          title: 'status code available'
        }
      ];

    beforeEach(function () {
      sinon.spy(view, '_showMessage');
    });

    afterEach(function () {
      view._showMessage.restore();
    });

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          obj = Em.Object.create(App.WidgetMixin, {
            metrics: metrics,
            content: {
              metrics: [
                {
                  name: 'n0'
                }
              ]
            },
            isExportButtonHidden: false,
            graphView: item.graphView,
            childViews: item.childViews
          });
          obj.getMetricsErrorCallback({
            isForcedAbort: item.isForcedAbort
          });
        });

        it('metrics array', function () {
          expect(obj.get('metrics')).to.eql(item.metrics);
        });

        it('error message', function () {
          expect(view._showMessage.callCount).to.equal(item.showMessageCallCount);
        });

        it('export button display', function () {
          expect(obj.get('isExportButtonHidden')).to.equal(item.isExportButtonHidden);
        });

      });

    });

    messageCases.forEach(function (item) {

      it(item.title, function () {
        obj = Em.Object.create(App.WidgetMixin, {
          graphView: Em.Object.create({}),
          childViews: [view]
        });
        obj.getMetricsErrorCallback({
          readyState: item.readyState,
          status: item.status
        }, 'error', 'Not Found');
        expect(view._showMessage.firstCall.args).to.eql(['warn', Em.I18n.t('graphs.error.title'), Em.I18n.t('graphs.error.message').format(item.textStatus, 'Not Found')]);
      });

    });

  });
});


describe('App.WidgetLoadAggregator', function () {
  var aggregator = App.WidgetLoadAggregator;

  describe("#add()", function () {
    it("timeout started", function () {
      aggregator.set('timeoutId', 'timeId');
      aggregator.get('requests').clear();
      aggregator.add({});
      expect(aggregator.get('requests')).to.not.be.empty;
    });
    it("timeout started (2)", function () {
      aggregator.set('timeoutId', null);
      aggregator.get('requests').clear();
      aggregator.add({});
      expect(aggregator.get('requests')).to.not.be.empty;
    });
  });

  describe("#groupRequests()", function () {
    var result;

    beforeEach(function () {
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
      result = aggregator.groupRequests(requests);
    });

    it("result.n1_C1.subRequests.length", function () {
      expect(result.n1_C1.subRequests.length).to.equal(1);
    });
    it("result.n1_C1.data.metric_paths.length", function () {
      expect(result.n1_C1.data.metric_paths.length).to.equal(1);
    });
    it("result.n1_C1_graph.subRequests.length", function () {
      expect(result.n1_C1_graph.subRequests.length).to.equal(1);
    });
    it("result.n1_C1_graph.data.metric_paths.length", function () {
      expect(result.n1_C1_graph.data.metric_paths.length).to.equal(1);
    });
    it("result.n2_C1.subRequests.length", function () {
      expect(result.n2_C1.subRequests.length).to.equal(1);
    });
    it("result.n2_C1.data.metric_paths.length", function () {
      expect(result.n2_C1.data.metric_paths.length).to.equal(1);
    });
    it("result.n1_C2.subRequests.length", function () {
      expect(result.n1_C2.subRequests.length).to.equal(1);
    });
    it("result.n1_C2.data.metric_paths.length", function () {
      expect(result.n1_C2.data.metric_paths.length).to.equal(1);
    });
  });

  describe("#runRequests()", function () {
    var mock = Em.Object.create({
      f1: function () {
        return {
          done: Em.K,
          fail: Em.K,
          always: Em.K
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
      expect(mock.f1.calledWith(requests.r1.data)).to.be.true;
    });
    it.skip("view destroyed", function () {
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
