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
require('views/main/service/info/summary');
var batchUtils = require('utils/batch_scheduled_requests');

describe('App.MainServiceInfoSummaryView', function() {

  var view = App.MainServiceInfoSummaryView.create({
    monitorsLiveTextView: Em.View.create(),
    controller: Em.Object.create({
      content: Em.Object.create({
        id: 'HDFS',
        serviceName: 'HDFS',
        hostComponents: []
      }),
      getActiveWidgetLayout: Em.K
    }),
    alertsController: Em.Object.create(),
    service: Em.Object.create()
  });

  describe('#servers', function () {
    it('services shouldn\'t have servers except FLUME and ZOOKEEPER', function () {
      expect(view.get('servers')).to.be.empty;
    });

    it('if one server exists then first server should have isComma and isAnd property false', function () {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        serviceName: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            displayName: '',
            isMaster: true
          })
        ]
      }));
      expect(view.get('servers').objectAt(0).isComma).to.equal(false);
      expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
    });

    it('if more than one servers exist then first server should have isComma - true and isAnd - false', function () {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        serviceName: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            displayName: '',
            isMaster: true
          }),
          Em.Object.create({
            displayName: '',
            isMaster: true
          })
        ]
      }));
      expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      expect(view.get('servers').objectAt(1).isAnd).to.equal(false);
    });

    it('if more than two servers exist then second server should have isComma - false and isAnd - true', function () {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        serviceName: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            displayName: '',
            isMaster: true
          }),
          Em.Object.create({
            displayName: '',
            isMaster: true
          }),
          Em.Object.create({
            displayName: '',
            isMaster: true
          })
        ]
      }));
      expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      expect(view.get('servers').objectAt(1).isAnd).to.equal(true);
      expect(view.get('servers').objectAt(2).isComma).to.equal(false);
      expect(view.get('servers').objectAt(2).isAnd).to.equal(false);
    });

  });

  describe('#hasAlertDefinitions', function () {

    beforeEach(function () {
      sinon.stub(App.AlertDefinition, 'find', function () {
        return [
          {
            serviceName: 'HDFS'
          },
          {
            serviceName: 'YARN'
          }
        ];
      });
    });

    afterEach(function () {
      App.AlertDefinition.find.restore();
    });

    it('should return true if at least one alert definition for this service exists', function () {
      view.set('controller.content', Em.Object.create({
        serviceName: 'HDFS'
      }));
      expect(view.get('hasAlertDefinitions')).to.be.true;

      it('should return false if there is no alert definition for this service', function () {
        view.set('controller.content', Em.Object.create({
          serviceName: 'ZOOKEEPER'
        }));
        expect(view.get('hasAlertDefinitions')).to.be.false;
      });
    })
  });

  describe('#didInsertElement', function () {

    var cases = [
      {
        serviceName: 'STORM',
        isStormMetricsSupported: false,
        isConstructGraphObjectsCalled: false,
        title: 'Storm, metrics not supported'
      },
      {
        serviceName: 'STORM',
        isStormMetricsSupported: true,
        isConstructGraphObjectsCalled: true,
        title: 'Storm, metrics supported'
      },
      {
        serviceName: 'HDFS',
        isConstructGraphObjectsCalled: true,
        title: 'not Storm'
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'constructGraphObjects', Em.K);
      this.mock = sinon.stub(App, 'get');
    });

    afterEach(function () {
      view.constructGraphObjects.restore();
      this.mock.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.set('controller.content.serviceName', item.serviceName);
        this.mock.withArgs('isStormMetricsSupported').returns(item.isStormMetricsSupported);
        view.didInsertElement();
        expect(view.constructGraphObjects.calledOnce).to.equal(item.isConstructGraphObjectsCalled);
      });
    });

  });

  describe.skip('#setTimeRange', function () {

    var cases = [
      {
        currentTimeRangeIndex: 0,
        isServiceMetricLoaded: false,
        graphIds: [],
        title: 'no event passed'
      },
      {
        event: {},
        currentTimeRangeIndex: 0,
        isServiceMetricLoaded: false,
        graphIds: [],
        title: 'no event context passed'
      },
      {
        event: {
          context: {
            index: 1
          }
        },
        currentTimeRangeIndex: 1,
        isServiceMetricLoaded: false,
        graphIds: [],
        title: 'no service name set'
      },
      {
        event: {
          context: {
            index: 2
          }
        },
        serviceName: 'HDFS',
        currentTimeRangeIndex: 2,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-hdfs-space-utilization', 'service-metrics-hdfs-file-operations',
            'service-metrics-hdfs-block-status', 'service-metrics-hdfs-io', 'service-metrics-hdfs-rpc'
          ],
          [
            'service-metrics-hdfs-gc', 'service-metrics-hdfs-jvm-heap', 'service-metrics-hdfs-jvm-threads'
          ]
        ]
      },
      {
        event: {
          context: {
            index: 3
          }
        },
        serviceName: 'YARN',
        currentTimeRangeIndex: 3,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-yarn-queue-allocated', 'service-metrics-yarn-queue-memory-resource',
            'service-metrics-yarn-queue-allocated-container', 'service-metrics-yarn-node-manager-statuses',
            'service-metrics-yarn-apps-current-states'
          ],
          [
            'service-metrics-yarn-apps-finished-states', 'service-metrics-yarn-rpc', 'service-metrics-yarn-gc',
            'service-metrics-yarn-jvm-threads', 'service-metrics-yarn-jvm-heap'
          ]
        ]
      },
      {
        event: {
          context: {
            index: 4
          }
        },
        serviceName: 'HBASE',
        currentTimeRangeIndex: 4,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-hbase-cluster-requests', 'service-metrics-hbase-regionserver-rw-requests',
            'service-metrics-hbase-regionserver-regions', 'service-metrics-hbase-regionserver-queuesize',
            'service-metrics-hbase-hlog-split-time'
          ],
          [
            'service-metrics-hbase-hlog-split-size'
          ]
        ]
      },
      {
        event: {
          context: {
            index: 5
          }
        },
        serviceName: 'AMBARI_METRICS',
        currentTimeRangeIndex: 5,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-ambari-metrics-master-average-load',
            'service-metrics-ambari-metrics-region-server-store-files',
            'service-metrics-ambari-metrics-region-server-regions',
            'service-metrics-ambari-metrics-region-server-requests',
            'service-metrics-ambari-metrics-region-server-block-cache-hit-percent'
          ],
          [
            'service-metrics-ambari-metrics-region-server-compaction-queue-size'
          ]
        ]
      },
      {
        event: {
          context: {
            index: 6
          }
        },
        serviceName: 'FLUME',
        currentTimeRangeIndex: 6,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-flume-channel-size-mma', 'service-metrics-flume-channel-size-sum',
            'service-metrics-flume-incoming_mma', 'service-metrics-flume-incoming_sum',
            'service-metrics-flume-outgoing_mma'
          ],
          [
            'service-metrics-flume-outgoing_sum'
          ]
        ]
      },
      {
        event: {
          context: {
            index: 7
          }
        },
        serviceName: 'STORM',
        currentTimeRangeIndex: 7,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-storm-supervisor-allocated', 'service-metrics-storm-executors',
            'service-metrics-storm-topologies', 'service-metrics-storm-tasks'
          ]
        ]
      },
      {
        event: {
          context: {
            index: 8
          }
        },
        serviceName: 'KAFKA',
        chunkSize: 4,
        currentTimeRangeIndex: 8,
        isServiceMetricLoaded: true,
        graphIds: [
          [
            'service-metrics-kafka-broker-topic-metrics', 'service-metrics-kafka-controller-metrics',
            'service-metrics-kafka-controler-status-metrics', 'service-metrics-kafka-replica-manager-metrics'
          ],
          [
            'service-metrics-kafka-log-metrics', 'service-metrics-kafka-replica-fetcher-metrics'
          ]
        ]
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'postUserPref', Em.K);
      view.setProperties({
        chunkSize: 5,
        currentTimeRangeIndex: 0,
        isServiceMetricLoaded: false,
        serviceMetricGraphs: []
      });
    });

    afterEach(function () {
      view.postUserPref.restore();
    });

    cases.forEach(function (item) {
      it(item.serviceName || item.title, function () {
        view.set('chunkSize', Em.isNone(item.chunkSize) ? 5 : item.chunkSize);
        view.set('service.serviceName', item.serviceName);
        view.setTimeRange(item.event);
        var graphIndices = [],
          graphIds = view.get('serviceMetricGraphs').map(function (graphs) {
          return graphs.map(function (graph) {
            var graphView = graph.create();
            graphIndices.push(graphView.get('currentTimeIndex'));
            return graphView.get('id');
          });
        });
        expect(view.get('currentTimeRangeIndex')).to.equal(item.currentTimeRangeIndex);
        expect(view.get('isServiceMetricLoaded')).to.equal(item.isServiceMetricLoaded);
        if (item.event && item.event.context && item.serviceName) {
          expect(graphIndices.uniq()).to.eql([item.currentTimeRangeIndex]);
        }
        expect(graphIds).to.eql(item.graphIds);
      });
    });

  });

  describe("#restartAllStaleConfigComponents", function () {
    it("trigger restartAllServiceHostComponents", function () {
      var view = App.MainServiceInfoSummaryView.create({
        controller: Em.Object.create({
          content: {
            serviceName: "HDFS"
          },
          getActiveWidgetLayout: Em.K
        }),
        service: Em.Object.create({
          displayName: 'HDFS'
        })
      });
      sinon.stub(batchUtils, "restartAllServiceHostComponents", Em.K);
      view.restartAllStaleConfigComponents().onPrimary();
      expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
      batchUtils.restartAllServiceHostComponents.restore();
    });
    it("trigger check last check point warning before triggering restartAllServiceHostComponents", function () {
      var view = App.MainServiceInfoSummaryView.create({
        controller: Em.Object.create({
          content: {
            serviceName: "HDFS",
            hostComponents: [{
              componentName: 'NAMENODE',
              workStatus: 'STARTED'
            }],
            restartRequiredHostsAndComponents: {
              "host1": ['NameNode'],
              "host2": ['DataNode', 'ZooKeeper']
            }
          },
          getActiveWidgetLayout: Em.K
        }),
        service: Em.Object.create({
          displayName: 'HDFS'
        })
      });
      var mainServiceItemController = App.MainServiceItemController.create({});
      sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
        return true;
      });
      sinon.stub(App.router, 'get', function(k) {
        if ('mainServiceItemController' === k) {
          return mainServiceItemController;
        }
        return Em.get(App.router, k);
      });
      view.restartAllStaleConfigComponents();
      expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      mainServiceItemController.checkNnLastCheckpointTime.restore();
      App.router.get.restore();
    });
  });
});