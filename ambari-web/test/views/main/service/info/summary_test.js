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

describe('App.MainServiceInfoSummaryView', function() {

  var view = App.MainServiceInfoSummaryView.create({
    monitorsLiveTextView: Em.View.create(),
    controller: Em.Object.create({
      content: Em.Object.create({
        id: 'HDFS',
        serviceName: 'HDFS',
        hostComponents: []
      })
    }),
    alertsController: Em.Object.create()
  });

  describe('#servers', function () {
    it('services shuldn\'t have servers except FLUME and ZOOKEEPER', function () {
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

  describe("#constructGraphObjects", function() {
    it("should return a single array with the items in the fom of '<name>.extend()' when the number of items is less than 4", function() {
      var graphs = ['HDFS_SpaceUtilization'];

      expect(view.constructGraphObjects(graphs).length).to.equal(1);
      expect(view.constructGraphObjects(graphs)[0].length).to.equal(1);
    });

    it("should return an array with arrays that are grouped into sizes of 4 or less when number of items is greater than 4", function() {
      var graphs = ['HDFS_SpaceUtilization', 'YARN_AllocatedMemory', 'MapReduce_JobsStatus', 
      'HBASE_ClusterRequests', 'Flume_ChannelSizeMMA'];

      expect(view.constructGraphObjects(graphs).length).to.equal(2);
      expect(view.constructGraphObjects(graphs)[0].length).to.equal(4);
      expect(view.constructGraphObjects(graphs)[1].length).to.equal(1);
    });

    it("should return an empty array if the graphs array provided is empty", function() {
      expect(view.constructGraphObjects([])).to.be.empty;
    });
  });

  describe("#observeAlerts()", function() {
    var mock = {state: 'inDOM'};
    it("No alerts loaded", function() {
      var alerts = [];
      view.set('alertsController.alerts', []);
      view.set('alerts', alerts);
      view.observeAlerts(mock);

      expect(alerts).to.be.empty;
      expect(view.get('alertsMap')).to.be.empty;
    });
    it("One alert loaded", function() {
      var alerts = [];
      view.set('alertsController.alerts', [{
        id: 1
      }]);
      view.set('alerts', alerts);
      view.observeAlerts(mock);

      expect(alerts[0]).to.be.eql({
        "id": 1
      });
      expect(alerts.length).to.be.equal(1);
      expect(view.get('alertsMap')).to.be.eql({"1": {
        "id": 1
      }});
    });
    it("No new alerts", function() {
      var alerts = [{id: 1}];
      view.set('alertsController.alerts', []);
      view.set('alerts', alerts);
      view.set('alertsMap', {'1': {id: '1'}});
      view.observeAlerts(mock);

      expect(alerts).to.be.empty;
      expect(view.get('alertsMap')).to.be.empty;
    });
    before(function () {
      sinon.stub(view, 'updateAlerts', Em.K);
    });
    after(function () {
      view.updateAlerts.restore();
    });
    it("Alerts already exist", function() {
      var alerts = [{id: 1}];
      view.set('alertsController.alerts', [{
        id: 1
      }]);
      view.set('alerts', alerts);
      view.set('alertsMap', {'1': {id: '1'}});
      view.observeAlerts(mock);

      expect(view.updateAlerts.calledWith(
        [{
          id: 1
        }],
        [{id: 1}]
      )).to.be.true;
    });
  });

  describe("#updateAlerts()", function() {
    var currentAlerts = [];
    var alertsMap = {};
    var newAlerts = [];
    it("Add new alert", function() {
      newAlerts.clear();
      currentAlerts.clear();
      alertsMap = {};

      newAlerts.pushObjects([
        Em.Object.create({
          id: '1',
          status: '1',
          isLoaded: true
        }),
        Em.Object.create({
          id: '2',
          status: '2',
          isLoaded: true
        })
      ]);
      var currentAlert = Em.Object.create({
          id: '1',
          status: '1',
          isLoaded: true
        });
      alertsMap['1'] = currentAlert;
      view.set('alertsMap', alertsMap);
      currentAlerts.pushObject(currentAlert);

      view.updateAlerts(newAlerts, currentAlerts);
      expect(currentAlerts.length).to.be.equal(2);
      expect(currentAlerts[1]).to.be.eql(Em.Object.create({
        id: '2',
        status: '2',
        isLoaded: true
      }));
      expect(alertsMap).to.be.eql({
        "1": Em.Object.create({
          id: '1',
          status: '1',
          isLoaded: true
        }),
        "2": Em.Object.create({
          id: '2',
          status: '2',
          isLoaded: true
        })
      });
    });
    it("Update properties of existing alert", function() {
      newAlerts.clear();
      currentAlerts.clear();
      alertsMap = {};

      newAlerts.pushObjects([
        Em.Object.create({
          id: '1',
          status: '2',
          isLoaded: true
        })
      ]);
      var currentAlert = Em.Object.create({
        id: '1',
        status: '1',
        isLoaded: true
      });
      alertsMap['1'] = currentAlert;
      view.set('alertsMap', alertsMap);
      currentAlerts.pushObject(currentAlert);

      view.updateAlerts(newAlerts, currentAlerts);
      expect(currentAlerts.length).to.be.equal(1);
      expect(currentAlerts[0]).to.be.eql(Em.Object.create({
        id: '1',
        status: '2',
        isLoaded: true
      }));
      expect(alertsMap).to.be.eql({
        "1": Em.Object.create({
          id: '1',
          status: '2',
          isLoaded: true
        })
      });
    });
    it("delete old alert", function() {
      newAlerts.clear();
      currentAlerts.clear();
      alertsMap = {};

      newAlerts.pushObjects([
        Em.Object.create({
          id: '1',
          status: '1',
          isLoaded: true
        })
      ]);
      var currentAlert1 = Em.Object.create({
        id: '1',
        status: '1',
        isLoaded: true
      });
      var currentAlert2 = Em.Object.create({
        id: '2',
        status: '2',
        isLoaded: true
      });
      alertsMap["1"] = currentAlert1;
      alertsMap["2"] = currentAlert2;
      view.set('alertsMap', alertsMap);
      currentAlerts.pushObjects([currentAlert1, currentAlert2]);

      view.updateAlerts(newAlerts, currentAlerts);

      expect(currentAlerts.length).to.be.equal(1);
      expect(currentAlerts[0]).to.be.eql(Em.Object.create({
        id: '1',
        status: '1',
        isLoaded: true
      }));
      expect(alertsMap).to.be.eql({
        "1": Em.Object.create({
          id: '1',
          status: '1',
          isLoaded: true
        })
      });
    });
  });
});