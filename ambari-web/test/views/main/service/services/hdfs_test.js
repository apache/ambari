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
var date = require('utils/date/date');
require('/views/main/service/services/hdfs');

function getView(options) {
  return App.MainDashboardServiceHdfsView.create(options || {});
}

describe('App.MainDashboardServiceHdfsView', function () {
  var view;

  beforeEach(function() {
    view = getView({service: Em.Object.create()});
  });

  App.TestAliases.testAsComputedAlias(getView(), 'dataNodesDead', 'service.dataNodesInstalled', 'boolean');

  App.TestAliases.testAsComputedAlias(getView(), 'journalNodesTotal', 'service.journalNodes.length', 'number');

  describe("#metricsNotAvailableObserver()", function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
    });
    afterEach(function() {
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      view.set("service", Em.Object.create({
        metricsNotAvailable: false
      }));
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'tooltip');
      sinon.stub(window, '$').returns(mock);
    });
    afterEach(function() {
      mock.tooltip.restore();
      window.$.restore();
    });

    it("tooltip destroy should be called", function() {
      view.willDestroyElement();
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });
  });

  describe("#journalNodesLive", function() {

    it("should return live journal nodes count", function() {
      view.set('service', Em.Object.create({
        journalNodes: [
          Em.Object.create({workStatus: 'STARTED'}),
          Em.Object.create()
        ]
      }));
      view.propertyDidChange('journalNodesLive');
      expect(view.get('journalNodesLive')).to.be.equal(1);
    });
  });

  describe("#isNfsInStack", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("no NFS_GATEWAY component", function() {
      this.mock.returns([]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.false;
    });

    it("NFS_GATEWAY component present", function() {
      this.mock.returns([{componentName: 'NFS_GATEWAY'}]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.true;
    });
  });

});
