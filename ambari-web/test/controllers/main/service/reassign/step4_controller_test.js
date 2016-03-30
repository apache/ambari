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

App = require('app');

require('controllers/main/service/reassign/step4_controller');

describe('App.ReassignMasterWizardStep4Controller', function () {

  var controller = App.ReassignMasterWizardStep4Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create(),
      reassignHosts: Em.Object.create()
    })
  });

  beforeEach(function () {
    sinon.stub(App.ajax, 'send', Em.K);
  });
  afterEach(function () {
    App.ajax.send.restore();
  });

  describe('#setAdditionalConfigs()', function () {

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isHaEnabled').returns(true);
    });

    afterEach(function () {
      App.get.restore();
    });

    it('Component is absent', function () {
      controller.set('additionalConfigsMap', []);
      var configs = {};

      expect(controller.setAdditionalConfigs(configs, 'COMP1', '')).to.be.false;
      expect(configs).to.eql({});
    });

    it('configs for Hadoop 2 is present', function () {
      controller.set('additionalConfigsMap', [
        {
          componentName: 'COMP1',
          configs: {
            'test-site': {
              'property1': '<replace-value>:1111'
            }
          },
          configs_Hadoop2: {
            'test-site': {
              'property2': '<replace-value>:2222'
            }
          }
        }
      ]);
      var configs = {
        'test-site': {}
      };

      expect(controller.setAdditionalConfigs(configs, 'COMP1', 'host1')).to.be.true;
      expect(configs).to.eql({
        'test-site': {
          'property2': 'host1:2222'
        }
      });
    });

    it('ignore some configs for NameNode after HA', function () {
      controller.set('additionalConfigsMap', [
        {
          componentName: 'NAMENODE',
          configs: {
            'test-site': {
              'fs.defaultFS': '<replace-value>:1111',
              'dfs.namenode.rpc-address': '<replace-value>:1111'
            }
          }
        }
      ]);
      var configs = {'test-site': {}};

      expect(controller.setAdditionalConfigs(configs, 'NAMENODE', 'host1')).to.be.true;
      expect(configs).to.eql({'test-site': {}});
    });
  });

  describe('#getHostComponentsNames()', function () {
    it('No host-components', function () {
      controller.set('hostComponents', []);
      expect(controller.getHostComponentsNames()).to.be.empty;
    });
    it('one host-components', function () {
      controller.set('hostComponents', ['COMP1']);
      expect(controller.getHostComponentsNames()).to.equal('Comp1');
    });
    it('ZKFC host-components', function () {
      controller.set('hostComponents', ['COMP1', 'ZKFC']);
      expect(controller.getHostComponentsNames()).to.equal('Comp1+ZKFC');
    });
  });

  describe('#testDBConnection', function() {
    beforeEach(function() {
      controller.set('requiredProperties', Em.A([]));
      controller.set('content.serviceProperties', Em.Object.create({'javax.jdo.option.ConnectionDriverName': 'mysql'}));
      controller.set('content.reassign.component_name', 'HIVE_SERVER');
      sinon.stub(controller, 'getConnectionProperty', Em.K);
      sinon.stub(App.router, 'get', Em.K);
    });

    afterEach(function() {
      controller.getConnectionProperty.restore();
      App.router.get.restore();
    });

    it('tests database connection', function() {
      sinon.stub(controller, 'prepareDBCheckAction', Em.K);

      controller.testDBConnection();
      expect(controller.prepareDBCheckAction.calledOnce).to.be.true;

      controller.prepareDBCheckAction.restore();
    });

    it('tests prepareDBCheckAction', function() {
      controller.prepareDBCheckAction();

      expect(App.ajax.send.calledOnce).to.be.true;
    });

  });

  describe('#removeUnneededTasks()', function () {
    var isHaEnabled = false;
    var commands;
    var commandsForDB;

    beforeEach(function () {
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });

      commands = [
        { id: 1, command: 'stopRequiredServices' },
        { id: 2, command: 'cleanMySqlServer' },
        { id: 3, command: 'createHostComponents' },
        { id: 4, command: 'putHostComponentsInMaintenanceMode' },
        { id: 5, command: 'reconfigure' },
        { id: 6, command: 'installHostComponents' },
        { id: 7, command: 'startZooKeeperServers' },
        { id: 8, command: 'startNameNode' },
        { id: 9, command: 'deleteHostComponents' },
        { id: 10, command: 'configureMySqlServer' },
        { id: 11, command: 'startMySqlServer' },
        { id: 12, command: 'startNewMySqlServer' },
        { id: 13, command: 'startRequiredServices' }
      ];

      commandsForDB = [
        { id: 1, command: 'createHostComponents' },
        { id: 2, command: 'installHostComponents' },
        { id: 3, command: 'configureMySqlServer' },
        { id: 4, command: 'restartMySqlServer' },
        { id: 5, command: 'testDBConnection' },
        { id: 6, command: 'stopRequiredServices' },
        { id: 7, command: 'cleanMySqlServer' },
        { id: 8, command: 'putHostComponentsInMaintenanceMode' },
        { id: 9, command: 'reconfigure' },
        { id: 10, command: 'deleteHostComponents' },
        { id: 11, command: 'configureMySqlServer' },
        { id: 12, command: 'startRequiredServices' }
      ];
    });

    afterEach(function () {
      App.get.restore();
    });

    it('hasManualSteps is false', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', false);

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1,3,4,5,6,9,12,13]);
    });

    it('reassign component is not NameNode and HA disabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'COMP1');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 5, 6]);
    });

    it('reassign component is not NameNode and HA enabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'COMP1');
      isHaEnabled = true;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 5, 6]);
    });

    it('reassign component is NameNode and HA disabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'NAMENODE');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 5, 6]);
    });

    it('reassign component is NameNode and HA enabled', function () {
      controller.set('tasks', commands);
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'NAMENODE');
      isHaEnabled = true;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 3, 4, 5, 6, 7, 8]);
    });

    it('reassign component is HiveServer and db type is mysql', function () {
      controller.set('tasks', commandsForDB);
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'mysql');
      controller.set('content.reassign.component_name', 'HIVE_SERVER');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
    });

    it('reassign component is HiveServer and db type is not mysql', function () {
      controller.set('tasks', commandsForDB);
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'derby');
      controller.set('content.reassign.component_name', 'HIVE_SERVER');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 6, 8, 9, 10, 12]);
    });

    it('reassign component is Oozie Server and db type is derby', function () {
      controller.set('tasks', commandsForDB);
      controller.set('content.hasManualSteps', true);
      controller.set('content.databaseType', 'derby');
      controller.set('content.reassign.component_name', 'OOZIE_SERVER');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1,2,6,8,9]);
    });

    it('reassign component is Oozie Server and db type is mysql', function () {
      controller.set('content.hasManualSteps', false);
      controller.set('content.databaseType', 'mysql');
      controller.set('content.reassign.component_name', 'OOZIE_SERVER');
      isHaEnabled = false;

      controller.set('tasks', commandsForDB);
      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1,2,3,4,5,6,7,8,9,10,11,12]);
    });
  });

  describe('#initializeTasks()', function () {
    beforeEach(function () {
      controller.set('tasks', []);
      sinon.stub(controller, 'getHostComponentsNames', Em.K);
      sinon.stub(controller, 'removeUnneededTasks', Em.K);
    });
    afterEach(function () {
      controller.removeUnneededTasks.restore();
      controller.getHostComponentsNames.restore();
    });
    it('No commands', function () {
      controller.set('commands', []);
      controller.set('commandsForDB', []);
      controller.initializeTasks();

      expect(controller.get('tasks')).to.be.empty;
    });
    it('One command', function () {
      controller.set('commands', ['COMMAND1']);
      controller.set('commandsForDB', ['COMMAND1']);
      controller.initializeTasks();

      expect(controller.get('tasks')[0].get('id')).to.equal(0);
      expect(controller.get('tasks')[0].get('command')).to.equal('COMMAND1');
    });
  });

  describe('#hideRollbackButton()', function () {

    it('No showRollback command', function () {
      controller.set('tasks', [Em.Object.create({
        showRollback: false
      })]);
      controller.hideRollbackButton();
      expect(controller.get('tasks')[0].get('showRollback')).to.be.false;
    });
    it('showRollback command is present', function () {
      controller.set('tasks', [Em.Object.create({
        showRollback: true
      })]);
      controller.hideRollbackButton();
      expect(controller.get('tasks')[0].get('showRollback')).to.be.false;
    });
  });

  describe('#onComponentsTasksSuccess()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted', Em.K);
    });
    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('One host-component', function () {
      controller.set('multiTaskCounter', 1);
      controller.set('hostComponents', [
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(0);
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
    it('two host-components', function () {
      controller.set('multiTaskCounter', 2);
      controller.set('hostComponents', [
        {},
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.onTaskCompleted.called).to.be.false;
    });
  });

  describe('#stopServices()', function () {
    it('', function () {
      controller.stopServices();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#createHostComponents()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'createComponent', Em.K);
    });
    afterEach(function () {
      controller.createComponent.restore();
    });

    it('One host-component', function () {
      controller.set('hostComponents', ['COMP1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.set('content.reassign.service_id', 'SERVICE1');

      controller.createHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.createComponent.calledWith('COMP1', 'host1', 'SERVICE1')).to.be.true;
    });
  });

  describe('#onCreateComponent()', function () {
    it('', function () {
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      controller.onCreateComponent();
      expect(controller.onComponentsTasksSuccess.calledOnce).to.be.true;
      controller.onComponentsTasksSuccess.restore();
    });
  });

  describe('#putHostComponentsInMaintenanceMode()', function () {
    beforeEach(function(){
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      controller.set('content.reassignHosts.source', 'source');
    });
    afterEach(function(){
      controller.onComponentsTasksSuccess.restore();
    });
    it('No host-components', function () {
      controller.set('hostComponents', []);
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.called).to.be.false;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it('One host-components', function () {
      controller.set('hostComponents', [{}]);
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('multiTaskCounter')).to.equal(1);
    });
  });

  describe('#installHostComponents()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'updateComponent', Em.K);
    });
    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('No host-components', function () {
      controller.set('hostComponents', []);

      controller.installHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(0);
      expect(controller.updateComponent.called).to.be.false;
    });
    it('One host-component', function () {
      controller.set('hostComponents', ['COMP1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.set('content.reassign.service_id', 'SERVICE1');

      controller.installHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.updateComponent.calledWith('COMP1', 'host1', 'SERVICE1', 'Install', 1)).to.be.true;
    });
  });

  describe('#reconfigure()', function () {
    it('', function () {
      sinon.stub(controller, 'loadConfigsTags', Em.K);
      controller.reconfigure();
      expect(controller.loadConfigsTags.calledOnce).to.be.true;
      controller.loadConfigsTags.restore();
    });
  });

  describe('#loadConfigsTags()', function () {
    it('', function () {
      controller.loadConfigsTags();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#getConfigUrlParams()', function () {
    var testCases  = [
      {
        componentName: 'NAMENODE',
        result: [
          "(type=hdfs-site&tag=1)",
          "(type=core-site&tag=2)"
        ]
      },
      {
        componentName: 'SECONDARY_NAMENODE',
        result: [
          "(type=hdfs-site&tag=1)",
          "(type=core-site&tag=2)"
        ]
      },
      {
        componentName: 'JOBTRACKER',
        result: [
          "(type=mapred-site&tag=4)"
        ]
      },
      {
        componentName: 'RESOURCEMANAGER',
        result: [
          "(type=yarn-site&tag=5)"
        ]
      },
      {
        componentName: 'APP_TIMELINE_SERVER',
        result: [
          "(type=yarn-site&tag=5)",
          "(type=yarn-env&tag=8)",
        ]
      },
      {
        componentName: 'OOZIE_SERVER',
        result: [
          "(type=oozie-site&tag=6)",
          "(type=core-site&tag=2)",
          "(type=oozie-env&tag=2)"
        ]
      },
      {
        componentName: 'WEBHCAT_SERVER',
        result: [
          "(type=webhcat-site&tag=7)"
        ]
      }
    ];

    var data = {
      Clusters: {
        desired_configs: {
          'hdfs-site': {tag: 1},
          'core-site': {tag: 2},
          'hbase-site': {tag: 3},
          'mapred-site': {tag: 4},
          'yarn-site': {tag: 5},
          'oozie-site': {tag: 6},
          'oozie-env': {tag: 2},
          'webhcat-site': {tag: 7},
          'yarn-env': {tag: 8},
          'accumulo-site': {tag: 9}
        }
      }
    };

    var services = [];

    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return services;
      });
    });
    afterEach(function () {
      App.Service.find.restore();
    });

    testCases.forEach(function (test) {
      it('get config of ' + test.componentName, function () {
        expect(controller.getConfigUrlParams(test.componentName, data)).to.eql(test.result);
      });
    });
    it('get config of NAMENODE when HBASE installed', function () {
      services = [
        {
          serviceName: 'HBASE'
        }
      ];
      expect(controller.getConfigUrlParams('NAMENODE', data)).to.eql([
        "(type=hdfs-site&tag=1)",
        "(type=core-site&tag=2)",
        "(type=hbase-site&tag=3)"
      ]);
    });

    it('get config of NAMENODE when ACCUMULO installed', function () {
      services = [
        {
          serviceName: 'ACCUMULO'
        }
      ];
      expect(controller.getConfigUrlParams('NAMENODE', data)).to.eql([
        "(type=hdfs-site&tag=1)",
        "(type=core-site&tag=2)",
        "(type=accumulo-site&tag=9)"
      ]);
    });

  });

  describe('#onLoadConfigsTags()', function () {
    it('', function () {
      sinon.stub(controller, 'getConfigUrlParams', function () {
        return [];
      });
      controller.set('content.reassign.component_name', 'COMP1');

      controller.onLoadConfigsTags({});
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.getConfigUrlParams.calledWith('COMP1', {})).to.be.true;

      controller.getConfigUrlParams.restore();
    });
  });

  describe('#onLoadConfigs()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'setAdditionalConfigs', Em.K);
      sinon.stub(controller, 'setSecureConfigs', Em.K);
      sinon.stub(controller, 'setSpecificNamenodeConfigs', Em.K);
      sinon.stub(controller, 'setSpecificResourceMangerConfigs', Em.K);
      sinon.stub(controller, 'getWebAddressPort', Em.K);
      sinon.stub(controller, 'getComponentDir', Em.K);
      sinon.stub(controller, 'saveClusterStatus', Em.K);
      sinon.stub(controller, 'saveConfigsToServer', Em.K);
      sinon.stub(controller, 'saveServiceProperties', Em.K);
      controller.set('content.reassignHosts.target', 'host1');
    });
    afterEach(function () {
      controller.setAdditionalConfigs.restore();
      controller.setSecureConfigs.restore();
      controller.setSpecificNamenodeConfigs.restore();
      controller.setSpecificResourceMangerConfigs.restore();
      controller.getWebAddressPort.restore();
      controller.getComponentDir.restore();
      controller.saveClusterStatus.restore();
      controller.saveConfigsToServer.restore();
      controller.saveServiceProperties.restore();
    });

    it('component is not NAMENODE', function () {
      controller.set('content.reassign.component_name', 'COMP1');

      controller.onLoadConfigs({items: []});
      expect(controller.setAdditionalConfigs.calledWith({}, 'COMP1', 'host1')).to.be.true;
      expect(controller.setSecureConfigs.calledWith([], {}, 'COMP1')).to.be.true;
      expect(controller.setSpecificNamenodeConfigs.called).to.be.false;
      expect(controller.getComponentDir.calledWith({}, 'COMP1')).to.be.true;
      expect(controller.saveClusterStatus.calledWith([])).to.be.true;
      expect(controller.saveConfigsToServer.calledWith({})).to.be.true;
      expect(controller.saveServiceProperties.calledWith({})).to.be.true;
    });
    it('component is NAMENODE, has configs', function () {
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.onLoadConfigs({items: [
        {
          type: 'hdfs-site',
          properties: {}
        }
      ]});
      expect(controller.setAdditionalConfigs.calledWith({'hdfs-site': {}}, 'NAMENODE', 'host1')).to.be.true;
      expect(controller.setSecureConfigs.calledWith([], {'hdfs-site': {}}, 'NAMENODE')).to.be.true;
      expect(controller.setSpecificNamenodeConfigs.calledWith({'hdfs-site': {}}, 'host1')).to.be.true;
      expect(controller.getComponentDir.calledWith({'hdfs-site': {}}, 'NAMENODE')).to.be.true;
      expect(controller.saveClusterStatus.calledWith([])).to.be.true;
      expect(controller.saveConfigsToServer.calledWith({'hdfs-site': {}})).to.be.true;
      expect(controller.saveServiceProperties.calledWith({'hdfs-site': {}})).to.be.true;
    });
    it('component is RESOURCEMANAGER, has configs', function () {
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

      controller.onLoadConfigs({items: [
        {
          type: 'hdfs-site',
          properties: {}
        }
      ]});
      expect(controller.setAdditionalConfigs.calledWith({'hdfs-site': {}}, 'RESOURCEMANAGER', 'host1')).to.be.true;
      expect(controller.setSecureConfigs.calledWith([], {'hdfs-site': {}}, 'RESOURCEMANAGER')).to.be.true;
      expect(controller.setSpecificResourceMangerConfigs.calledWith({'hdfs-site': {}}, 'host1')).to.be.true;
      expect(controller.getComponentDir.calledWith({'hdfs-site': {}}, 'RESOURCEMANAGER')).to.be.true;
      expect(controller.saveClusterStatus.calledWith([])).to.be.true;
      expect(controller.saveConfigsToServer.calledWith({'hdfs-site': {}})).to.be.true;
      expect(controller.saveServiceProperties.calledWith({'hdfs-site': {}})).to.be.true;
    });
  });

  describe('#loadStep()', function () {
    var isHaEnabled = true;

    beforeEach(function () {
      controller.set('content.reassign.service_id', 'service1');
      sinon.stub(controller, 'onTaskStatusChange', Em.K);
      sinon.stub(controller, 'initializeTasks', Em.K);
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });
    });
    afterEach(function () {
      controller.onTaskStatusChange.restore();
      controller.initializeTasks.restore();
      App.get.restore();
    });

    it('reassign component is NameNode and HA enabled', function () {
      isHaEnabled = true;
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE', 'ZKFC']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('reassign component is NameNode and HA disabled', function () {
      isHaEnabled = false;
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('reassign component is JOBTRACKER and HA enabled', function () {
      isHaEnabled = true;
      controller.set('content.reassign.component_name', 'JOBTRACKER');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['JOBTRACKER']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
    it('reassign component is RESOURCEMANAGER and HA enabled', function () {
      isHaEnabled = true;
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['RESOURCEMANAGER']);
      expect(controller.get('serviceName')).to.eql(['service1']);
    });
  });


  describe('#saveConfigsToServer()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'getServiceConfigData', Em.K);
    });
    afterEach(function () {
      controller.getServiceConfigData.restore();
    });
    it('', function () {
      controller.saveConfigsToServer([1]);
      expect(controller.getServiceConfigData.calledWith([1])).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#setSpecificNamenodeConfigs()', function () {
    var isHaEnabled = false;
    var service = Em.Object.create();
    beforeEach(function () {
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });
      sinon.stub(App.Service, 'find', function () {
        return service;
      });
      controller.set('content.reassignHosts.source', 'host1');
    });
    afterEach(function () {
      App.get.restore();
      App.Service.find.restore();
    });
    it('HA isn\'t enabled and no HBASE or ACCUMULO service', function () {
      isHaEnabled = false;
      var configs = {};
      controller.setSpecificNamenodeConfigs(configs, 'host1');
      expect(configs).to.eql({});
    });
    it('HA isn\'t enabled and HBASE and ACCUMULO service', function () {
      isHaEnabled = false;
      service = Em.Object.create({
        isLoaded: true
      });
      var configs = {
        'hbase-site': {
          'hbase.rootdir': 'hdfs://localhost:8020/apps/hbase/data'
        },
        'accumulo-site': {
          'instance.volumes': 'hdfs://localhost:8020/apps/accumulo/data'
        }
      };
      controller.setSpecificNamenodeConfigs(configs, 'host1');
      expect(configs['hbase-site']['hbase.rootdir']).to.equal('hdfs://host1:8020/apps/hbase/data');
      expect(configs['accumulo-site']['instance.volumes']).to.equal('hdfs://host1:8020/apps/accumulo/data');
    });
    it('HA enabled and namenode 1', function () {
      isHaEnabled = true;
      var configs = {
        'hdfs-site': {
          'dfs.nameservices': 's',
          'dfs.namenode.http-address.s.nn1': 'host1:50070',
          'dfs.namenode.https-address.s.nn1': '',
          'dfs.namenode.rpc-address.s.nn1': ''
        }
      };
      controller.setSpecificNamenodeConfigs(configs, 'host2');
      expect(configs['hdfs-site']).to.eql({
        "dfs.nameservices": "s",
        "dfs.namenode.http-address.s.nn1": "host2:50070",
        "dfs.namenode.https-address.s.nn1": "host2:50470",
        "dfs.namenode.rpc-address.s.nn1": "host2:8020"
      });
    });
    it('HA enabled and namenode 2', function () {
      isHaEnabled = true;
      var configs = {
        'hdfs-site': {
          'dfs.nameservices': 's',
          'dfs.namenode.http-address.s.nn2': 'host2:50070',
          'dfs.namenode.https-address.s.nn2': '',
          'dfs.namenode.rpc-address.s.nn2': ''
        }
      };
      controller.setSpecificNamenodeConfigs(configs, 'host1');
      expect(configs['hdfs-site']).to.eql({
        "dfs.nameservices": "s",
        "dfs.namenode.http-address.s.nn2": "host1:50070",
        "dfs.namenode.https-address.s.nn2": "host1:50470",
        "dfs.namenode.rpc-address.s.nn2": "host1:8020"
      });
    });
  });

  describe('#setSpecificResourceMangerConfigs()', function () {
    var isRMHaEnabled = false;
    var service = Em.Object.create();
    beforeEach(function () {
      sinon.stub(App, 'get', function () {
        return isRMHaEnabled;
      });
      controller.set('content.reassignHosts.source', 'host1');
    });
    afterEach(function () {
      App.get.restore();
    });

    it('HA isn\'t enabled', function () {
      isRMHaEnabled = false;
      var configs = {};
      controller.setSpecificResourceMangerConfigs(configs, 'host1');
      expect(configs).to.eql({});
    });
    it('HA enabled and resource manager 1', function () {
      isRMHaEnabled = true;
      var configs = {
        'yarn-site': {
          'yarn.resourcemanager.hostname.rm1': 'host1',
          'yarn.resourcemanager.webapp.address.rm1': 'host1:8088',
          'yarn.resourcemanager.webapp.https.address.rm1': 'host1:8443'
        }
      };
      controller.setSpecificResourceMangerConfigs(configs, 'host2');
      expect(configs['yarn-site']).to.eql({
        'yarn.resourcemanager.hostname.rm1': 'host2',
        'yarn.resourcemanager.webapp.address.rm1': 'host2:8088',
        'yarn.resourcemanager.webapp.https.address.rm1': 'host2:8443'
      });
    });
    it('HA enabled and resource manager 2', function () {
      isRMHaEnabled = true;
      var configs = {
        'yarn-site': {
          'yarn.resourcemanager.hostname.rm2': 'host2',
          'yarn.resourcemanager.webapp.address.rm2': 'host2:8088',
          'yarn.resourcemanager.webapp.https.address.rm2': 'host2:8443'
        }
      };
      controller.setSpecificResourceMangerConfigs(configs, 'host1');
      expect(configs['yarn-site']).to.eql({
        'yarn.resourcemanager.hostname.rm2': 'host1',
        'yarn.resourcemanager.webapp.address.rm2': 'host1:8088',
        'yarn.resourcemanager.webapp.https.address.rm2': 'host1:8443'
      });
    });
  });

  describe('#getWebAddressPort', function(){
    var configs = {
        'yarn-site': {
          'yarn.resourcemanager.hostname.rm2': 'host2',
          'yarn.resourcemanager.webapp.address.rm2': 'host2:8088',
          'yarn.resourcemanager.webapp.https.address.rm2': 'host2:8443'
        }
    };
    
    var httpPort = controller.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.address.rm2');
    expect(httpPort).to.eql('8088');
    
    var httpsPort = controller.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.https.address.rm2');
    expect(httpsPort).to.eql('8443');

    configs = {
        'yarn-site': {
          'yarn.resourcemanager.hostname.rm2': 'host2',
          'yarn.resourcemanager.webapp.address.rm2': 'host2:',
          'yarn.resourcemanager.webapp.https.address.rm2': 'host2:  '
        }
    };
    
    //check for falsy conditions
    httpPort = controller.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.address.rm2');
    var flag = "falsy"
    if (httpPort)
      flag = "truthy"
    expect(flag).to.eql('falsy')

    httpsPort = controller.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.https.address.rm2');
    flag = "falsy"
    if (httpsPort)
      flag = "truthy"
    expect(flag).to.eql("falsy")

    configs = {
        'yarn-site': {
          'yarn.resourcemanager.hostname.rm2': 'host2'
        }
    };

   httpPort = controller.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.address.rm2');
   var flag = "falsy"
   if (httpPort != null) //check for null, still part of the falsy condition checks.
      flag = "truthy"
    expect(flag).to.eql('falsy')
  });
  
  describe('#setSecureConfigs()', function () {
    it('undefined component and security disabled', function () {
      var secureConfigs = [];
      sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(false);
      controller.set('secureConfigsMap', []);
      expect(controller.setSecureConfigs(secureConfigs, {}, 'COMP1')).to.be.false;
      expect(secureConfigs).to.eql([]);
      App.get.restore();
    });
    it('component exist and security disabled', function () {
      var secureConfigs = [];
      sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(false);
      controller.set('secureConfigsMap', [{
        componentName: 'COMP1'
      }]);
      expect(controller.setSecureConfigs(secureConfigs, {}, 'COMP1')).to.be.false;
      expect(secureConfigs).to.eql([]);
      App.get.restore();
    });
    it('undefined component and security enabled', function () {
      var secureConfigs = [];
      sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(true);
      controller.set('secureConfigsMap', []);
      expect(controller.setSecureConfigs(secureConfigs, {}, 'COMP1')).to.be.false;
      expect(secureConfigs).to.eql([]);
      App.get.restore();
    });
    it('component exist and security enabled', function () {
      var secureConfigs = [];
      sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(true);
      var configs = {'s1': {
        'k1': 'kValue',
        'p1': 'pValue'
      }};
      controller.set('secureConfigsMap', [{
        componentName: 'COMP1',
        configs: [{
          site: 's1',
          keytab: 'k1',
          principal: 'p1'
        }]
      }]);
      expect(controller.setSecureConfigs(secureConfigs, configs, 'COMP1')).to.be.true;
      expect(secureConfigs).to.eql([
        {
          "keytab": "kValue",
          "principal": "pValue"
        }
      ]);
      App.get.restore();
    });
  });

  describe('#getComponentDir()', function () {
    var configs = {
      'hdfs-site': {
        'dfs.name.dir': 'case1',
        'dfs.namenode.name.dir': 'case2',
        'dfs.namenode.checkpoint.dir': 'case3'
      },
      'core-site': {
        'fs.checkpoint.dir': 'case4'
      }
    };

    it('unknown component name', function () {
      expect(controller.getComponentDir(configs, 'COMP1')).to.be.empty;
    });
    it('NAMENODE component', function () {
      expect(controller.getComponentDir(configs, 'NAMENODE')).to.equal('case2');
    });
    it('SECONDARY_NAMENODE component', function () {
      expect(controller.getComponentDir(configs, 'SECONDARY_NAMENODE')).to.equal('case3');
    });
  });

  describe('#saveClusterStatus()', function () {
    var mock = {
      saveComponentDir: Em.K,
      saveSecureConfigs: Em.K
    };
    beforeEach(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.router, 'get', function() {
        return mock;
      });
      sinon.spy(mock, 'saveComponentDir');
      sinon.spy(mock, 'saveSecureConfigs');
    });
    afterEach(function () {
      App.clusterStatus.setClusterStatus.restore();
      App.router.get.restore();
      mock.saveSecureConfigs.restore();
      mock.saveComponentDir.restore();
    });

    it('componentDir undefined and secureConfigs is empty', function () {
      expect(controller.saveClusterStatus([], null)).to.be.false;
    });
    it('componentDir defined and secureConfigs is empty', function () {
      expect(controller.saveClusterStatus([], 'dir1')).to.be.true;
      expect(mock.saveComponentDir.calledWith('dir1')).to.be.true;
      expect(mock.saveSecureConfigs.calledWith([])).to.be.true;
    });
    it('componentDir undefined and secureConfigs has data', function () {
      expect(controller.saveClusterStatus([1], null)).to.be.true;
      expect(mock.saveComponentDir.calledWith(null)).to.be.true;
      expect(mock.saveSecureConfigs.calledWith([1])).to.be.true;
    });
    it('componentDir defined and secureConfigs has data', function () {
      expect(controller.saveClusterStatus([1], 'dir1')).to.be.true;
      expect(mock.saveComponentDir.calledWith('dir1')).to.be.true;
      expect(mock.saveSecureConfigs.calledWith([1])).to.be.true;
    });
  });

  describe('#onSaveConfigs()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted', Em.K);
    });
    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('', function () {
      controller.onSaveConfigs();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe('#startZooKeeperServers()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'updateComponent', Em.K);
    });
    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('', function () {
      controller.set('content.masterComponentHosts', [{
        component: 'ZOOKEEPER_SERVER',
        hostName: 'host1'
      }]);
      controller.startZooKeeperServers();
      expect(controller.updateComponent.calledWith('ZOOKEEPER_SERVER', ['host1'], 'ZOOKEEPER', 'Start')).to.be.true;
    });
  });

  describe('#startNameNode()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'updateComponent', Em.K);
    });
    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('reassign host does not match current', function () {
      controller.set('content.masterComponentHosts', [{
        component: 'NAMENODE',
        hostName: 'host1'
      }]);
      controller.set('content.reassignHosts.source', 'host2');
      controller.startNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', ['host1'], 'HDFS', 'Start')).to.be.true;
    });
    it('reassign host matches current', function () {
      controller.set('content.masterComponentHosts', [{
        component: 'NAMENODE',
        hostName: 'host1'
      }]);
      controller.set('content.reassignHosts.source', 'host1');
      controller.startNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', [], 'HDFS', 'Start')).to.be.true;
    });
  });

  describe('#startServices()', function () {
    before(function () {
      sinon.stub(App.router, 'get').returns({"skip.service.checks": "false"});
    });
    after(function () {
      App.router.get.restore();
    });
    it('', function () {
      controller.startServices();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#deleteHostComponents()', function () {

    it('No host components', function () {
      controller.set('hostComponents', []);
      controller.set('content.reassignHosts.source', 'host1');
      controller.deleteHostComponents();
      expect(App.ajax.send.called).to.be.false;
    });
    it('delete two components', function () {
      controller.set('hostComponents', [1, 2]);
      controller.set('content.reassignHosts.source', 'host1');
      controller.deleteHostComponents();
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({
        "hostName": "host1",
        "componentName": 1
      });
      expect(App.ajax.send.getCall(1).args[0].data).to.eql({
        "hostName": "host1",
        "componentName": 2
      });
    });
  });

  describe('#onDeleteHostComponentsError()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      sinon.stub(controller, 'onTaskError', Em.K);
    });
    afterEach(function () {
      controller.onComponentsTasksSuccess.restore();
      controller.onTaskError.restore();
    });

    it('task success', function () {
      var error = {
        responseText: 'org.apache.ambari.server.controller.spi.NoSuchResourceException'
      }
      controller.onDeleteHostComponentsError(error);
      expect(controller.onComponentsTasksSuccess.calledOnce).to.be.true;
    });
    it('unknown error', function () {
      var error = {
        responseText: ''
      }
      controller.onDeleteHostComponentsError(error);
      expect(controller.onTaskError.calledOnce).to.be.true;
    });
  });

  describe('#done()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'removeObserver', Em.K);
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      controller.removeObserver.restore();
      App.router.send.restore();
    });

    it('submit disabled', function () {
      controller.set('isSubmitDisabled', true);
      controller.done();
      expect(App.router.send.called).to.be.false;
    });
    it('submit enabled and does not have manual steps', function () {
      controller.set('isSubmitDisabled', false);
      controller.set('content.hasManualSteps', false);
      controller.done();
      expect(controller.removeObserver.calledWith('tasks.@each.status', controller, 'onTaskStatusChange')).to.be.true;
      expect(App.router.send.calledWith('complete')).to.be.true;
    });
    it('submit enabled and has manual steps', function () {
      controller.set('isSubmitDisabled', false);
      controller.set('content.hasManualSteps', true);
      controller.done();
      expect(controller.removeObserver.calledWith('tasks.@each.status', controller, 'onTaskStatusChange')).to.be.true;
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });

  describe('#getServiceConfigData()', function () {
    var services = [];
    var stackServices = [];
    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return services;
      });
      sinon.stub(App.StackService, 'find', function () {
        return stackServices;
      });
    });
    afterEach(function () {
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    it('No services', function () {
      services = [];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData([])).to.eql([]);
    });
    it('No services in stackServices', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData([])).to.eql([]);
    });
    it('Services in stackServicesm but configTypesRendered is empty', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [Em.Object.create({
        serviceName: 'S1',
        configTypesRendered: {}
      })];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData([])[0]).to.equal("{\"Clusters\":{\"desired_config\":[]}}");
    });
    it('Services in stackServicesm and configTypesRendered has data, but configs is empty', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [
        Em.Object.create({
          serviceName: 'S1',
          configTypesRendered: {'type1': {}}
        })
      ];
      controller.set('content.reassign.component_name', 'COMP1');
      expect(controller.getServiceConfigData([])[0]).to.equal("{\"Clusters\":{\"desired_config\":[]}}");
    });
    it('Services in stackServicesm and configTypesRendered has data, and configs present', function () {
      services = [Em.Object.create({serviceName: 'S1'})];
      stackServices = [
        Em.Object.create({
          serviceName: 'S1',
          configTypesRendered: {'type1': {}}
        })
      ];
      var configs = {
        'type1': {
          'prop1': 'value1'
        }
      };
      controller.set('content.reassign.component_name', 'COMP1');
      expect(JSON.parse(controller.getServiceConfigData(configs)[0]).Clusters.desired_config.length).to.equal(1);
    });
  });

  describe('#testsMySqlServer()', function () {
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find', function() {
        return Em.A([
          Em.Object.create({
            'componentName': 'MYSQL_SERVER',
            'hostName': 'c6401.ambari.apache.org'
          })
        ]);
      });
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('Cleans MySql Server', function () {
      controller.cleanMySqlServer();
      expect(App.ajax.send.calledOnce).to.be.true;
    });

    it('Configures MySql Server', function () {
      controller.configureMySqlServer();
      expect(App.ajax.send.calledOnce).to.be.true;
    });

  });

  describe("#prepareDBCheckAction()", function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({
        'jdk_location': 'jdk_location',
        'jdk.name': 'jdk.name',
        'java.home': 'java.home'
      });
      sinon.stub(controller, 'getConnectionProperty').returns('prop1');
    });
    afterEach(function () {
      App.router.get.restore();
      controller.getConnectionProperty.restore();
    });
    it("", function() {
      controller.set('content.reassignHosts', Em.Object.create({target: 'host1'}));
      controller.reopen({
        dbType: 'type1',
        requiredProperties: [],
        preparedDBProperties: {}
      });
      controller.prepareDBCheckAction();
      expect(App.ajax.send.getCall(0).args[0].name).to.equal('cluster.custom_action.create');
      expect(App.ajax.send.getCall(0).args[0].success).to.equal('onCreateActionSuccess');
      expect(App.ajax.send.getCall(0).args[0].error).to.equal('onTaskError');
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({
        requestInfo: {
          "context": "Check host",
          "action": "check_host",
          "parameters": {
            "db_name": "type1",
            "jdk_location": "jdk_location",
            "jdk_name": "jdk.name",
            "java_home": "java.home",
            "threshold": 60,
            "ambari_server_host": "",
            "check_execute_list": "db_connection_check"
          }
        },
        filteredHosts: ['host1']
      });
    });
  });
});
