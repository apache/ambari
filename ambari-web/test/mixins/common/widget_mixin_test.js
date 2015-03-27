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

describe('App.WidgetMixin', function() {
  var mixinClass = Em.Object.extend(App.WidgetMixin, {metrics: [], content: {}});

  describe('#beforeRender()', function () {
    var mixinObject = mixinClass.create();
    beforeEach(function () {
      this.mock = sinon.stub(mixinObject, 'getRequestData');
      sinon.stub(mixinObject, 'getHostComponentMetrics').returns({complete: function(callback){
        callback();
      }});
      sinon.stub(mixinObject, 'getServiceComponentMetrics').returns({complete: function(callback){
        callback();
      }});
    });
    afterEach(function () {
      this.mock.restore();
      mixinObject.getHostComponentMetrics.restore();
      mixinObject.getServiceComponentMetrics.restore();
    });
    it('has host_component_criteria', function () {
      this.mock.returns({'key1': {host_component_criteria: 'criteria'}});
      mixinObject.set('isLoaded', false);
      mixinObject.beforeRender();

      expect(mixinObject.getHostComponentMetrics.calledWith({host_component_criteria: 'criteria'})).to.be.true;
      expect(mixinObject.get('isLoaded')).to.be.true;
    });
    it('host_component_criteria is absent', function () {
      this.mock.returns({'key1': {}});
      mixinObject.set('isLoaded', false);
      mixinObject.beforeRender();

      expect(mixinObject.getServiceComponentMetrics.calledWith({})).to.be.true;
      expect(mixinObject.get('isLoaded')).to.be.true;
    });
  });

  describe("#extractExpressions()", function() {
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

  describe("#getRequestData()", function() {
    var mixinObject = mixinClass.create();
    it("", function() {
      var data = [
        {
          "name": "regionserver.Server.percentFilesLocal",
          "widget_id": "metrics/hbase/regionserver/percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER"
        },
        {
          "name": "regionserver.Server.percentFilesLocal2",
          "widget_id": "w2",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER"
        },
        {
          "name": "regionserver.Server.percentFilesLocal",
          "widget_id": "metrics/hbase/regionserver/percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER",
          "host_component_criteria": 'c1'
        },
        {
          "name": "regionserver.Server.percentFilesLocal",
          "widget_id": "metrics/hbase/regionserver/percentFilesLocal",
          "service_name": "HDFS",
          "component_name": "DATANODE",
          "host_component_criteria": 'c1'
        }
      ];

      expect(mixinObject.getRequestData(data)).to.eql({
        "HBASE_HBASE_REGIONSERVER_undefined": {
          "name": "regionserver.Server.percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER",
          "widget_ids": [
            "metrics/hbase/regionserver/percentFilesLocal",
            "w2"
          ]
        },
        "HBASE_HBASE_REGIONSERVER_c1": {
          "name": "regionserver.Server.percentFilesLocal",
          "service_name": "HBASE",
          "component_name": "HBASE_REGIONSERVER",
          "host_component_criteria": "c1",
          "widget_ids": [
            "metrics/hbase/regionserver/percentFilesLocal"
          ]
        },
        "HDFS_DATANODE_c1": {
          "name": "regionserver.Server.percentFilesLocal",
          "service_name": "HDFS",
          "component_name": "DATANODE",
          "host_component_criteria": "c1",
          "widget_ids": [
            "metrics/hbase/regionserver/percentFilesLocal"
          ]
        }
      });
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
        widget_ids: ['w1', 'w2']
      };
      mixinObject.getServiceComponentMetrics(request);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'widgets.serviceComponent.metrics.get',
        sender: mixinObject,
        data: {
          serviceName: 'S1',
          componentName: 'C1',
          widgetIds: 'w1,w2'
        },
        success: 'getServiceComponentMetricsSuccessCallback'
      })
    });
  });

  describe("#getServiceComponentMetricsSuccessCallback()", function() {
    var mixinObject = mixinClass.create();

    it("", function() {
      var data = {
        metrics: {
          "hbase" : {
            "ipc" : {
              "IPC" : {
                "numOpenConnections" : 11.5
              }
            }
          }
        }
      };
      mixinObject.set('content.metrics', [
        {
          name: 'ipc.IPC.numOpenConnections'
        }
      ]);
      mixinObject.getServiceComponentMetricsSuccessCallback(data, {}, {serviceName: 'hbase'});
      expect(mixinObject.get('metrics').findProperty('name', 'ipc.IPC.numOpenConnections').data).to.equal(11.5);
    });
  });

  describe("#getHostComponentMetrics()", function () {
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
        widget_ids: ['w1', 'w2'],
        host_component_criteria: 'c1'
      };
      mixinObject.getHostComponentMetrics(request);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'widgets.hostComponent.metrics.get',
        sender: mixinObject,
        data: {
          serviceName: 'S1',
          componentName: 'C1',
          widgetIds: 'w1,w2',
          hostComponentCriteria: 'host_components/HostRoles/c1'
        },
        success: 'getHostComponentMetricsSuccessCallback'
      })
    });
  });

});

