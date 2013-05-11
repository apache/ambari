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

var Ember = require('ember');
var App = require('app');
require('controllers/wizard/step6_controller');

/*
describe('App.InstallerStep6Controller', function () {

  var HOSTS = [ 'host1', 'host2', 'host3', 'host4' ];
  //App.InstallerStep6Controller.set.rawHosts = HOSTS;
  var controller = App.InstallerStep6Controller.create();
  controller.set('showHbase', false);
  HOSTS.forEach(function (_hostName) {
    controller.get('hosts').pushObject(Ember.Object.create({
      hostname: _hostName,
      isDataNode: true,
      isTaskTracker: true,
      isRegionServer: true
    }));
  });



describe('#selectAllDataNodes()', function () {
  controller.get('hosts').setEach('isDataNode', false);

  it('should set isDataNode to true on all hosts', function () {
    controller.selectAllDataNodes();
    expect(controller.get('hosts').everyProperty('isDataNode', true)).to.equal(true);
  })
})

describe('#selectAllTaskTrackers()', function () {
  it('should set isTaskTracker to true on all hosts', function () {
    controller.selectAllTaskTrackers();
    expect(controller.get('hosts').everyProperty('isTaskTracker', true)).to.equal(true);
  })
})

describe('#selectAllRegionServers()', function () {
  it('should set isRegionServer to true on all hosts', function () {
    controller.selectAllRegionServers();
    expect(controller.get('hosts').everyProperty('isRegionServer', true)).to.equal(true);
  })
})

describe('#isAllDataNodes()', function () {

  beforeEach(function () {
    controller.get('hosts').setEach('isDataNode', true);
  })

  it('should return true if isDataNode is true for all services', function () {
    expect(controller.get('isAllDataNodes')).to.equal(true);
  })

  it('should return false if isDataNode is false for one host', function () {
    controller.get('hosts')[0].set('isDataNode', false);
    expect(controller.get('isAllDataNodes')).to.equal(false);
  })
})

describe('#isAllTaskTrackers()', function () {

  beforeEach(function () {
    controller.get('hosts').setEach('isTaskTracker', true);
  })

  it('should return true if isTaskTracker is true for all hosts', function () {
    expect(controller.get('isAllTaskTrackers')).to.equal(true);
  })

  it('should return false if isTaskTracker is false for one host', function () {
    controller.get('hosts')[0].set('isTaskTracker', false);
    expect(controller.get('isAllTaskTrackers')).to.equal(false);
  })

})

describe('#isAllRegionServers()', function () {

  beforeEach(function () {
    controller.get('hosts').setEach('isRegionServer', true);
  });

  it('should return true if isRegionServer is true for all hosts', function () {
    expect(controller.get('isAllRegionServers')).to.equal(true);
  })

  it('should return false if isRegionServer is false for one host', function () {
    controller.get('hosts')[0].set('isRegionServer', false);
    expect(controller.get('isAllRegionServers')).to.equal(false);
  })

})

describe('#validate()', function () {

  beforeEach(function () {
    controller.get('hosts').setEach('isDataNode', true);
    controller.get('hosts').setEach('isTaskTracker', true);
    controller.get('hosts').setEach('isRegionServer', true);
  });

  it('should return false if isDataNode is false for all hosts', function () {
    controller.get('hosts').setEach('isDataNode', false);
    expect(controller.validate()).to.equal(false);
  })

  it('should return false if isTaskTracker is false for all hosts', function () {
    controller.get('hosts').setEach('isTaskTracker', false);
    expect(controller.validate()).to.equal(false);
  })

  it('should return false if isRegionServer is false for all hosts', function () {
    controller.get('hosts').setEach('isRegionServer', false);
    expect(controller.validate()).to.equal(false);
  })

  it('should return true if isDataNode, isTaskTracker, and isRegionServer is true for all hosts', function () {
    expect(controller.validate()).to.equal(true);
  })

  it('should return true if isDataNode, isTaskTracker, and isRegionServer is true for only one host', function () {
    controller.get('hosts').setEach('isDataNode', false);
    controller.get('hosts').setEach('isTaskTracker', false);
    controller.get('hosts').setEach('isRegionServer', false);
    var host = controller.get('hosts')[0];
    host.set('isDataNode', true);
    host.set('isTaskTracker', true);
    host.set('isRegionServer', true);
    expect(controller.validate()).to.equal(true);
  })

})

})*/
