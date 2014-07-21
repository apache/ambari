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

var modelSetup = require('test/init_model_test');
require('models/stack_service_component');

var stackServiceComponent,
  stackServiceComponentData = {
    id: 'ssc'
  },
  components = [
    {
      name: 'NAMENODE',
      isReassignable: true
    },
    {
      name: 'SECONDARY_NAMENODE',
      isReassignable: true
    },
    {
      name: 'JOBTRACKER',
      isReassignable: true
    },
    {
      name: 'RESOURCEMANAGER',
      isReassignable: true
    },
    {
      name: 'SUPERVISOR',
      isDeletable: true,
      isRollinRestartAllowed: true,
      isAddableToHost: true
    },
    {
      name: 'HBASE_MASTER',
      isDeletable: true,
      isAddableToHost: true
    },
    {
      name: 'DATANODE',
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: true,
      isAddableToHost: true
    },
    {
      name: 'TASKTRACKER',
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: true,
      isAddableToHost: true
    },
    {
      name: 'NODEMANAGER',
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: true,
      isAddableToHost: true
    },
    {
      name: 'HBASE_REGIONSERVER',
      isDeletable: true,
      isRollinRestartAllowed: true,
      isDecommissionAllowed: true,
      isAddableToHost: true
    },
    {
      name: 'GANGLIA_MONITOR',
      isDeletable: true,
      isAddableToHost: true
    },
    {
      name: 'FLUME_HANDLER',
      isRefreshConfigsAllowed: true
    },
    {
      name: 'ZOOKEEPER_SERVER',
      isAddableToHost: true
    },
    {
      name: 'MYSQL_SERVER',
      mastersNotShown: true
    },
    {
      name: 'JOURNALNODE',
      mastersNotShown: true
    }
  ],
  reassignable = components.filterProperty('isReassignable').mapProperty('name'),
  deletable = components.filterProperty('isDeletable').mapProperty('name'),
  rollingRestartable = components.filterProperty('isRollinRestartAllowed').mapProperty('name'),
  decommissionable = components.filterProperty('isDecommissionAllowed').mapProperty('name'),
  refreshable = components.filterProperty('isRefreshConfigsAllowed').mapProperty('name'),
  addable = components.filterProperty('isAddableToHost').mapProperty('name'),
  mastersNotShown = components.filterProperty('mastersNotShown').mapProperty('name');

describe('App.StackServiceComponent', function () {

  beforeEach(function () {
    stackServiceComponent = App.StackServiceComponent.createRecord(stackServiceComponentData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(stackServiceComponent);
  });

  describe('#isSlave', function () {
    it('should be true', function () {
      stackServiceComponent.set('componentCategory', 'SLAVE');
      expect(stackServiceComponent.get('isSlave')).to.be.true;
    });
    it('should be false', function () {
      stackServiceComponent.set('componentCategory', 'cc');
      expect(stackServiceComponent.get('isSlave')).to.be.false;
    });
  });

  describe('#isRestartable', function () {
    it('should be true', function () {
      stackServiceComponent.set('isClient', false);
      expect(stackServiceComponent.get('isRestartable')).to.be.true;
    });
    it('should be false', function () {
      stackServiceComponent.set('isClient', true);
      expect(stackServiceComponent.get('isRestartable')).to.be.false;
    });
  });

  describe('#isReassignable', function () {
    reassignable.forEach(function (item) {
      it('should be true', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isReassignable')).to.be.true;
      });
    });
    it('should be false', function () {
      stackServiceComponent.set('componentName', 'name');
      expect(stackServiceComponent.get('isReassignable')).to.be.false;
    });
  });

  describe('#isDeletable', function () {
    deletable.forEach(function (item) {
      it('should be true', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isDeletable')).to.be.true;
      });
    });
    it('should be false', function () {
      stackServiceComponent.set('componentName', 'name');
      expect(stackServiceComponent.get('isDeletable')).to.be.false;
    });
  });

  describe('#isRollinRestartAllowed', function () {
    rollingRestartable.forEach(function (item) {
      it('should be true', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isRollinRestartAllowed')).to.be.true;
      });
    });
    it('should be false', function () {
      stackServiceComponent.set('componentName', 'name');
      expect(stackServiceComponent.get('isRollinRestartAllowed')).to.be.false;
    });
  });

  describe('#isDecommissionAllowed', function () {
    decommissionable.forEach(function (item) {
      it('should be true', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isDecommissionAllowed')).to.be.true;
      });
    });
    it('should be false', function () {
      stackServiceComponent.set('componentName', 'name');
      expect(stackServiceComponent.get('isDecommissionAllowed')).to.be.false;
    });
  });

  describe('#isRefreshConfigsAllowed', function () {
    refreshable.forEach(function (item) {
      it('should be true', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isRefreshConfigsAllowed')).to.be.true;
      });
    });
    it('should be false', function () {
      stackServiceComponent.set('componentName', 'name');
      expect(stackServiceComponent.get('isRefreshConfigsAllowed')).to.be.false;
    });
  });

  describe('#isAddableToHost', function () {
    addable.forEach(function (item) {
      it('should be true', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isAddableToHost')).to.be.true;
      });
    });
    it('should be false', function () {
      stackServiceComponent.set('componentName', 'name');
      expect(stackServiceComponent.get('isAddableToHost')).to.be.false;
    });
  });

  describe('#isShownOnInstallerAssignMasterPage', function () {
    mastersNotShown.forEach(function (item) {
      it('should be false', function () {
        stackServiceComponent.set('componentName', item);
        expect(stackServiceComponent.get('isShownOnInstallerAssignMasterPage')).to.be.false;
      });
    });
    it('should be true', function () {
      stackServiceComponent.set('componentName', 'APP_TIMELINE_SERVER');
      expect(stackServiceComponent.get('isShownOnInstallerAssignMasterPage')).to.be.true;
    });
    it('should be true', function () {
      stackServiceComponent.setProperties({
        componentName: 'name',
        isMaster: true
      });
      expect(stackServiceComponent.get('isShownOnInstallerAssignMasterPage')).to.be.true;
    });
  });

});
