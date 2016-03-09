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
require('models/host_component');

describe('App.HostComponent', function() {

  App.store.load(App.HostComponent, {
    id: 'COMP_host',
    component_name: 'COMP1'
  });
  var hc = App.HostComponent.find('COMP_host');


  describe('#getStatusesList', function() {
    it('allowed statuses', function() {
      var statuses = ["STARTED","STARTING","INSTALLED","STOPPING","INSTALL_FAILED","INSTALLING","UPGRADE_FAILED","UNKNOWN","DISABLED","INIT"];
      expect(App.HostComponentStatus.getStatusesList()).to.include.members(statuses);
      expect(statuses).to.include.members(App.HostComponentStatus.getStatusesList());
    });
  });

  describe('#getStatusesList', function() {
    it('allowed statuses', function() {
      var statuses = ["STARTED","STARTING","INSTALLED","STOPPING","INSTALL_FAILED","INSTALLING","UPGRADE_FAILED","UNKNOWN","DISABLED","INIT"];
      expect(App.HostComponentStatus.getStatusesList()).to.include.members(statuses);
      expect(statuses).to.include.members(App.HostComponentStatus.getStatusesList());
    });
  });

  describe('#isClient', function() {
    it('', function() {
      sinon.stub(App.get('components.clients'), 'contains', Em.K);
      hc.propertyDidChange('isClient');
      hc.get('isClient');
      expect(App.get('components.clients').contains.calledWith('COMP1')).to.be.true;
      App.get('components.clients').contains.restore();
    });
  });

  describe('#isMaster', function() {
    it('', function() {
      sinon.stub(App.get('components.masters'), 'contains', Em.K);
      hc.propertyDidChange('isMaster');
      hc.get('isMaster');
      expect(App.get('components.masters').contains.calledWith('COMP1')).to.be.true;
      App.get('components.masters').contains.restore();
    });
  });

  describe('#isSlave', function() {
    it('', function() {
      sinon.stub(App.get('components.slaves'), 'contains', Em.K);
      hc.propertyDidChange('isSlave');
      hc.get('isSlave');
      expect(App.get('components.slaves').contains.calledWith('COMP1')).to.be.true;
      App.get('components.slaves').contains.restore();
    });
  });

  describe('#isDeletable', function() {
    it('', function() {
      sinon.stub(App.get('components.deletable'), 'contains', Em.K);
      hc.propertyDidChange('isDeletable');
      hc.get('isDeletable');
      expect(App.get('components.deletable').contains.calledWith('COMP1')).to.be.true;
      App.get('components.deletable').contains.restore();
    });
  });

  describe('#isRunning', function() {
    var testCases = [
      {
        workStatus: 'INSTALLED',
        result: false
      },
      {
        workStatus: 'STARTING',
        result: true
      },
      {
        workStatus: 'STARTED',
        result: true
      }
    ];
    testCases.forEach(function(test){
      it('workStatus - ' + test.workStatus, function() {
        hc.set('workStatus', test.workStatus);
        hc.propertyDidChange('isRunning');
        expect(hc.get('isRunning')).to.equal(test.result);
      });
    });
  });

  describe('#isDecommissioning', function() {
    var mock = [];
    beforeEach(function () {
      sinon.stub(App.HDFSService, 'find', function () {
        return mock;
      })
    });
    afterEach(function () {
      App.HDFSService.find.restore();
    });
    it('component name is not DATANODE', function() {
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.false;
    });
    it('component name is DATANODE but no HDFS service', function() {
      hc.set('componentName', 'DATANODE');
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.false;
    });
    it('HDFS has no decommission DataNodes', function() {
      hc.set('componentName', 'DATANODE');
      mock.push(Em.Object.create({
        decommissionDataNodes: []
      }));
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.false;
    });
    it('HDFS has decommission DataNodes', function() {
      hc.set('componentName', 'DATANODE');
      hc.set('hostName', 'host1');
      mock.clear();
      mock.push(Em.Object.create({
        decommissionDataNodes: [{hostName: 'host1'}]
      }));
      hc.propertyDidChange('isDecommissioning');
      expect(hc.get('isDecommissioning')).to.be.true;
    });
  });

  describe('#isActive', function() {
    it('passiveState is ON', function() {
      hc.set('passiveState', "ON");
      hc.propertyDidChange('isActive');
      expect(hc.get('isActive')).to.be.false;
    });
    it('passiveState is OFF', function() {
      hc.set('passiveState', "OFF");
      hc.propertyDidChange('isActive');
      expect(hc.get('isActive')).to.be.true;
    });
  });

  describe('#statusClass', function() {
    it('isActive is false', function() {
      hc.reopen({
        isActive: false
      });
      hc.propertyDidChange('statusClass');
      expect(hc.get('statusClass')).to.equal('icon-medkit');
    });
    it('isActive is true', function() {
      var status = 'INSTALLED';
      hc.set('isActive', true);
      hc.set('workStatus', status);
      hc.propertyDidChange('statusClass');
      expect(hc.get('statusClass')).to.equal(status);
    });
  });

  describe('#statusIconClass', function () {
    var testCases = [
      {
        statusClass: 'STARTED',
        result: 'icon-ok-sign'
      },
      {
        statusClass: 'STARTING',
        result: 'icon-ok-sign'
      },
      {
        statusClass: 'INSTALLED',
        result: 'icon-warning-sign'
      },
      {
        statusClass: 'STOPPING',
        result: 'icon-warning-sign'
      },
      {
        statusClass: 'UNKNOWN',
        result: 'icon-question-sign'
      },
      {
        statusClass: '',
        result: ''
      }
    ];

    it('reset statusClass to plain property', function () {
      hc.reopen({
        statusClass: ''
      })
    });
    testCases.forEach(function (test) {
      it('statusClass - ' + test.statusClass, function () {
        hc.set('statusClass', test.statusClass);
        hc.propertyDidChange('statusIconClass');
        expect(hc.get('statusIconClass')).to.equal(test.result);
      });
    });
  });

  describe('#componentTextStatus', function () {
    before(function () {
      sinon.stub(App.HostComponentStatus, 'getTextStatus', Em.K);
    });
    after(function () {
      App.HostComponentStatus.getTextStatus.restore();
    });
    it('componentTextStatus should be changed', function () {
      var status = 'INSTALLED';
      hc.set('workStatus', status);
      hc.propertyDidChange('componentTextStatus');
      hc.get('componentTextStatus');
      expect(App.HostComponentStatus.getTextStatus.calledWith(status)).to.be.true;
    });
  });

  describe("#getCount", function () {
    var testCases = [
      {
        t: 'unknown component',
        data: {
          componentName: 'CC',
          type: 'totalCount',
          stackComponent: Em.Object.create()
        },
        result: 0
      },
      {
        t: 'master component',
        data: {
          componentName: 'C1',
          type: 'totalCount',
          stackComponent: Em.Object.create({componentCategory: 'MASTER'})
        },
        result: 3
      },
      {
        t: 'slave component',
        data: {
          componentName: 'C1',
          type: 'installedCount',
          stackComponent: Em.Object.create({componentCategory: 'SLAVE'})
        },
        result: 4
      },
      {
        t: 'client component',
        data: {
          componentName: 'C1',
          type: 'startedCount',
          stackComponent: Em.Object.create({componentCategory: 'CLIENT'})
        },
        result: 5
      },
      {
        t: 'client component, unknown type',
        data: {
          componentName: 'C1',
          type: 'unknownCount',
          stackComponent: Em.Object.create({componentCategory: 'CLIENT'})
        },
        result: 0
      }
    ];

    beforeEach(function () {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create({totalCount: 3}));
      sinon.stub(App.SlaveComponent, 'find').returns(Em.Object.create({installedCount: 4}));
      sinon.stub(App.ClientComponent, 'find').returns(Em.Object.create({startedCount: 5, unknownCount: null}));
    });
    afterEach(function () {
      this.mock.restore();
      App.MasterComponent.find.restore();
      App.SlaveComponent.find.restore();
      App.ClientComponent.find.restore();
    });

    testCases.forEach(function (test) {
      it(test.t, function () {
        this.mock.returns(test.data.stackComponent);
        expect(App.HostComponent.getCount(test.data.componentName, test.data.type)).to.equal(test.result);
      });
    });
  });
});