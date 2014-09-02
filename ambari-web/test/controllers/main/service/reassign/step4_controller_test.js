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
    var isHadoop2Stack = false;

    beforeEach(function () {
      sinon.stub(App, 'get', function () {
        return isHadoop2Stack;
      });
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
    it('Component is present', function () {
      controller.set('additionalConfigsMap', [
        {
          componentName: 'COMP1',
          configs: {
            'test-site': {
              'property1': '<replace-value>:1111'
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
          'property1': 'host1:1111'
        }
      });
    });
    it('configs_Hadoop2 is present but isHadoop2Stack = false', function () {
      isHadoop2Stack = false;
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
          'property1': 'host1:1111'
        }
      });
    });
    it('configs_Hadoop2 is present but isHadoop2Stack = true', function () {
      isHadoop2Stack = true;
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
  });

  /*  describe('#loadStep()', function () {
   var isHaEnabled = true;

   beforeEach(function () {
   controller.set('content.reassign.service_id', 'service1');
   sinon.stub(controller, 'onTaskStatusChange', Em.K);
   sinon.stub(App, 'get', function () {
   return isHaEnabled;
   });
   });
   afterEach(function () {
   App.get.restore();
   controller.onTaskStatusChange.restore();
   });

   it('reassign component is NameNode and HA enabled', function () {
   isHaEnabled = true;
   controller.set('content.reassign.component_name', 'NAMENODE');

   controller.loadStep();
   expect(controller.get('hostComponents')).to.eql(['NAMENODE', 'ZKFC']);
   expect(controller.get('restartYarnMRComponents')).to.be.false;
   expect(controller.get('serviceName')).to.eql(['service1']);
   });
   it('reassign component is NameNode and HA disabled', function () {
   isHaEnabled = false;
   controller.set('content.reassign.component_name', 'NAMENODE');

   controller.loadStep();
   expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
   expect(controller.get('restartYarnMRComponents')).to.be.false;
   expect(controller.get('serviceName')).to.eql(['service1']);
   });
   it('reassign component is JOBTRACKER and HA enabled', function () {
   isHaEnabled = true;
   controller.set('content.reassign.component_name', 'JOBTRACKER');

   controller.loadStep();
   expect(controller.get('hostComponents')).to.eql(['JOBTRACKER']);
   expect(controller.get('restartYarnMRComponents')).to.be.true;
   expect(controller.get('serviceName')).to.eql(['service1']);
   });
   it('reassign component is RESOURCEMANAGER and HA enabled', function () {
   isHaEnabled = true;
   controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

   controller.loadStep();
   expect(controller.get('hostComponents')).to.eql(['RESOURCEMANAGER']);
   expect(controller.get('restartYarnMRComponents')).to.be.true;
   expect(controller.get('serviceName')).to.eql(['service1']);
   });
   });*/

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

  describe('#removeUnneededTasks()', function () {
    var isHaEnabled = false;

    beforeEach(function () {
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });
      controller.set('tasks', [
        {id: 1},
        {id: 2},
        {id: 3},
        {id: 4},
        {id: 5},
        {id: 6},
        {id: 7},
        {id: 8},
        {id: 9}
      ]);
    });
    afterEach(function () {
      App.get.restore();
    });

    it('hasManualSteps is false', function () {
      controller.set('content.hasManualSteps', false);

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5, 8, 9]);
    });
    it('reassign component is not NameNode and HA disabled', function () {
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'COMP1');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5]);
    });
    it('reassign component is not NameNode and HA enabled', function () {
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'COMP1');
      isHaEnabled = true;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5]);
    });
    it('reassign component is NameNode and HA disabled', function () {
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'NAMENODE');
      isHaEnabled = false;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5]);
    });
    it('reassign component is NameNode and HA enabled', function () {
      controller.set('content.hasManualSteps', true);
      controller.set('content.reassign.component_name', 'NAMENODE');
      isHaEnabled = true;

      controller.removeUnneededTasks();
      expect(controller.get('tasks').mapProperty('id')).to.eql([1, 2, 3, 4, 5, 6, 7]);
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
      controller.initializeTasks();

      expect(controller.get('tasks')).to.be.empty;
    });
    it('One command', function () {
      controller.set('commands', ['COMMAND1']);
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

    it('No host-components', function () {
      controller.set('multiTaskCounter', 0);
      controller.set('hostComponents', []);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
    it('One host-component', function () {
      controller.set('multiTaskCounter', 0);
      controller.set('hostComponents', [
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
    it('two host-components', function () {
      controller.set('multiTaskCounter', 0);
      controller.set('hostComponents', [
        {},
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.onTaskCompleted.called).to.be.false;
    });
  });

  describe('#getStopServicesData()', function () {
    it('restarting YARN component', function () {
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');
      sinon.stub(App.Service, 'find', function () {
        return [
          {
            serviceName: 'HDFS'
          },
          {
            serviceName: 'SERVICE1'
          }
        ];
      });

      expect(controller.getStopServicesData()).to.eql({
        "ServiceInfo": {
          "state": "INSTALLED"
        },
        "context": "Stop required services",
        "urlParams": "ServiceInfo/service_name.in(SERVICE1)"
      });
      App.Service.find.restore();
    });
    it('restarting non-YARN component', function () {
      controller.set('content.reassign.component_name', 'NAMENODE');
      expect(controller.getStopServicesData()).to.eql({
        "ServiceInfo": {
          "state": "INSTALLED"
        },
        "context": "Stop all services"
      });
    });
  });

  describe('#stopServices()', function () {
    it('', function () {
      sinon.stub(controller, 'getStopServicesData', Em.K);

      controller.stopServices();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.getStopServicesData.calledOnce).to.be.true;

      controller.getStopServicesData.restore();
    });
  });

  describe('#createHostComponents()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'createComponent', Em.K);
    });
    afterEach(function () {
      controller.createComponent.restore();
    });

    it('No host-components', function () {
      controller.set('hostComponents', []);

      controller.createHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(0);
      expect(controller.createComponent.called).to.be.false;
    });
    it('One host-component', function () {
      controller.set('hostComponents', ['COMP1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.set('content.reassign.service_id', 'SERVICE1');

      controller.createHostComponents();

      expect(controller.get('multiTaskCounter')).to.equal(0);
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
      expect(controller.get('multiTaskCounter')).to.equal(0);
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

      expect(controller.get('multiTaskCounter')).to.equal(0);
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
      }
    ];

    var data = {
      Clusters: {
        desired_configs: {
          'hdfs-site': {tag: 1},
          'core-site': {tag: 2},
          'hbase-site': {tag: 3},
          'mapred-site': {tag: 4},
          'yarn-site': {tag: 5}
        }
      }
    };

    var services = [];

    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return services;
      })
    });
    afterEach(function () {
      App.Service.find.restore();
    });

    testCases.forEach(function (test) {
      it('get config of ' + test.componentName, function () {
        expect(controller.getConfigUrlParams(test.componentName, data)).to.eql(test.result);
      })
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
    })
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
      sinon.stub(controller, 'getComponentDir', Em.K);
      sinon.stub(controller, 'saveClusterStatus', Em.K);
      sinon.stub(controller, 'saveConfigsToServer', Em.K);
      controller.set('content.reassignHosts.target', 'host1');
    });
    afterEach(function () {
      controller.setAdditionalConfigs.restore();
      controller.setSecureConfigs.restore();
      controller.setSpecificNamenodeConfigs.restore();
      controller.getComponentDir.restore();
      controller.saveClusterStatus.restore();
      controller.saveConfigsToServer.restore();
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
    });
  });


 /* describe('#setSpecificNamenodeConfigs()', function () {
   it('configs is empty', function () {
   controller.setSpecificNamenodeConfigs();
   });
   });*/
});
