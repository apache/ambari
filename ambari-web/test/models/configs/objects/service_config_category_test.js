
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
var configPropertyHelper = require('utils/configs/config_property_helper');

require('models/configs/objects/service_config_category');
require('models/configs/objects/service_config_property');

var serviceConfigCategory,
  nameCases = [
    {
      name: 'DataNode',
      primary: 'DATANODE'
    },
    {
      name: 'TaskTracker',
      primary: 'TASKTRACKER'
    },
    {
      name: 'RegionServer',
      primary: 'HBASE_REGIONSERVER'
    },
    {
      name: 'name',
      primary: null
    }
  ],
  components = [
    {
      name: 'NameNode',
      master: true
    },
    {
      name: 'SNameNode',
      master: true
    },
    {
      name: 'JobTracker',
      master: true
    },
    {
      name: 'HBase Master',
      master: true
    },
    {
      name: 'Oozie Master',
      master: true
    },
    {
      name: 'Hive Metastore',
      master: true
    },
    {
      name: 'WebHCat Server',
      master: true
    },
    {
      name: 'ZooKeeper Server',
      master: true
    },
    {
      name: 'Ganglia',
      master: true
    },
    {
      name: 'DataNode',
      slave: true
    },
    {
      name: 'TaskTracker',
      slave: true
    },
    {
      name: 'RegionServer',
      slave: true
    }
  ],
  masters = components.filterProperty('master'),
  slaves = components.filterProperty('slave'),
  groupsData = {
    groups: [
      Em.Object.create({
        errorCount: 1
      }),
      Em.Object.create({
        errorCount: 2
      })
    ]
  };

describe('App.ServiceConfigCategory', function () {

  beforeEach(function () {
    serviceConfigCategory = App.ServiceConfigCategory.create();
  });

  describe('#primaryName', function () {
    nameCases.forEach(function (item) {
      it('should return ' + item.primary, function () {
        serviceConfigCategory.set('name', item.name);
        expect(serviceConfigCategory.get('primaryName')).to.equal(item.primary);
      })
    });
  });

  describe('#isForMasterComponent', function () {
    masters.forEach(function (item) {
      it('should be true for ' + item.name, function () {
        serviceConfigCategory.set('name', item.name);
        expect(serviceConfigCategory.get('isForMasterComponent')).to.be.true;
      });
    });
    it('should be false', function () {
      serviceConfigCategory.set('name', 'name');
      expect(serviceConfigCategory.get('isForMasterComponent')).to.be.false;
    });
  });

  describe('#isForSlaveComponent', function () {
    slaves.forEach(function (item) {
      it('should be true for ' + item.name, function () {
        serviceConfigCategory.set('name', item.name);
        expect(serviceConfigCategory.get('isForSlaveComponent')).to.be.true;
      });
    });
    it('should be false', function () {
      serviceConfigCategory.set('name', 'name');
      expect(serviceConfigCategory.get('isForSlaveComponent')).to.be.false;
    });
  });

  describe('#slaveErrorCount', function () {
    it('should be 0', function () {
      serviceConfigCategory.set('slaveConfigs', []);
      expect(serviceConfigCategory.get('slaveErrorCount')).to.equal(0);
    });
    it('should sum all errorCount values', function () {
      serviceConfigCategory.set('slaveConfigs', groupsData);
      expect(serviceConfigCategory.get('slaveErrorCount')).to.equal(3);
    });
  });

  describe('#errorCount', function () {
    it('should sum all errors for category', function () {
      serviceConfigCategory.reopen({
        slaveErrorCount: 1
      });
      expect(serviceConfigCategory.get('errorCount')).to.equal(1);
      serviceConfigCategory.set('nonSlaveErrorCount', 2);
      expect(serviceConfigCategory.get('errorCount')).to.equal(3);
      serviceConfigCategory.set('slaveErrorCount', 0);
      expect(serviceConfigCategory.get('errorCount')).to.equal(2);
    });
  });

  describe('#isAdvanced', function () {
    it('should be true', function () {
      serviceConfigCategory.set('name', 'Advanced');
      expect(serviceConfigCategory.get('isAdvanced')).to.be.true;
    });
    it('should be false', function () {
      serviceConfigCategory.set('name', 'name');
      expect(serviceConfigCategory.get('isAdvanced')).to.be.false;
    });
  });

});