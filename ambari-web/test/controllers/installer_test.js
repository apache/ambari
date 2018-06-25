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
require('models/cluster');
require('controllers/wizard');
require('controllers/installer');

describe('App.InstallerController', function () {

  var installerController = App.InstallerController.create();

  after(function () {
    installerController.destroy();
  });

  describe('#init', function () {
    var c;
    beforeEach(function () {
      c = App.InstallerController.create({});
    });
    it('all steps are disabled by default', function () {
      expect(c.get('isStepDisabled.length')).to.eq(c.get('totalSteps'));
      for (var i = 0, length = c.get('isStepDisabled.length'); i < length; i++) {
        expect(c.get('isStepDisabled').findProperty('step', i).get('value')).to.eq(true);
      }
    });
  });

  describe('#getCluster', function() {
    it ('Should return merged clusterStatusTemplate', function() {
      installerController.set('clusterStatusTemplate', {
        name: 'template'
      });
      expect(installerController.getCluster()).to.eql({
        name: 'template'
      });
    });
  });

  describe('#checkRepoURL', function() {
    var stacks = Em.A([
      Em.Object.create({
        isSelected: false
      }),
      Em.Object.create({
        isSelected: true,
        reload: false,
        id: 'nn-cc',
        stackNameVersion: 'nn-cc',
        repositories: Em.A([
          Em.Object.create({
            isSelected: true,
            isEmpty: false
          })
        ]),
        operatingSystems: Em.A([
          Em.Object.create({
            isSelected: true,
            isEmpty: false,
            repositories: Em.A([
              Em.Object.create({
                isEmpty: false,
                errorTitle: '1',
                errorContent: '1',
                validation: '',
                showRepo: true
              })
            ])
          })
        ])
      })
    ]);
    var wizard = Em.Object.create({
      skipValidationChecked: true
    });
    it ('Should reload installed stacks', function() {

      installerController.set('content.stacks', stacks);
      installerController.checkRepoURL(wizard);

      var expected = [
        {
          "isSelected": false
        },
        {
          "isSelected": true,
          "reload": true,
          "id": "nn-cc",
          "stackNameVersion": 'nn-cc',
          "repositories": [
            {
              "isSelected": true,
              "isEmpty": false
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "isEmpty": false,
              "repositories": [
                {
                  "isEmpty": false,
                  "errorTitle": "",
                  "errorContent": "",
                  "validation": "INPROGRESS",
                  "showRepo": true
                }
              ]
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('content.stacks')));

      expect(res).to.be.eql(expected);
    });
  });

  describe('#checkRepoURLSuccessCallback', function() {
    var stacks = Em.A([
      Em.Object.create({
        isSelected: false
      }),
      Em.Object.create({
        isSelected: true,
        reload: false,
        id: 'nn-cc',
        repositories: Em.A([
          Em.Object.create({
            repoId: 11,
            isSelected: true,
            isEmpty: false
          })
        ]),
        operatingSystems: Em.A([
          Em.Object.create({
            isSelected: true,
            isEmpty: false,
            id: 1,
            repositories: Em.A([
              Em.Object.create({
                repoId: 11,
                isEmpty: false,
                errorTitle: '1',
                errorContent: '1',
                validation: '',
                showRepo: true
              })
            ])
          })
        ])
      })
    ]);
    var resolve = false;
    var data = {
      osId: 1,
      repoId: 11,
      dfd: {
        resolve: function() {
          resolve = true;
        }
      }
    };
    it ('Should check stacks for success', function() {

      installerController.set('content.stacks', stacks);
      installerController.checkRepoURLSuccessCallback(null,null,data);

      var expected = [
        {
          "isSelected": false
        },
        {
          "isSelected": true,
          "reload": false,
          "id": "nn-cc",
          "repositories": [
            {
              "repoId": 11,
              "isSelected": true,
              "isEmpty": false
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "isEmpty": false,
              "id": 1,
              "repositories": [
                {
                  "repoId": 11,
                  "isEmpty": false,
                  "errorTitle": "1",
                  "errorContent": "1",
                  "validation": "OK",
                  "showRepo": true
                }
              ]
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('content.stacks')));
      expect(resolve).to.be.true;
      expect(res).to.be.eql(expected);
    });
  });

  describe('#checkRepoURLErrorCallback', function() {
    var stacks = Em.A([
      Em.Object.create({
        isSelected: false
      }),
      Em.Object.create({
        isSelected: true,
        reload: false,
        id: 'nn-cc',
        repositories: Em.A([
          Em.Object.create({
            repoId: 11,
            isSelected: true
          })
        ]),
        operatingSystems: Em.A([
          Em.Object.create({
            isSelected: true,
            id: 1,
            repositories: Em.A([
              Em.Object.create({
                repoId: 11,
                errorTitle: '1',
                errorContent: '1',
                validation: ''
              })
            ])
          })
        ])
      })
    ]);
    var resolve = false;
    var data = {
      osId: 1,
      repoId: 11,
      dfd: {
        reject: function() {
          resolve = true;
        }
      }
    };
    it ('Should check stacks for error', function() {

      var req = {
        status: 500,
        statusText: 'error'
      };
      installerController.set('content.stacks', stacks);
      installerController.checkRepoURLErrorCallback(req,{},{},{},data);

      var expected = [
        {
          "isSelected": false
        },
        {
          "isSelected": true,
          "reload": false,
          "id": "nn-cc",
          "repositories": [
            {
              "repoId": 11,
              "isSelected": true
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "id": 1,
              "repositories": [
                {
                  "repoId": 11,
                  "errorTitle": "500:error",
                  "errorContent": "",
                  "validation": "INVALID"
                }
              ]
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('content.stacks')));
      expect(resolve).to.be.true;
      expect(res).to.be.eql(expected);
    });
  });

  describe('#setLowerStepsDisable', function() {

    beforeEach(function () {
      var steps = Em.A([
        Em.Object.create({
          step: 0,
          value: false
        }),
        Em.Object.create({
          step: 1,
          value: false
        }),
        Em.Object.create({
          step: 2,
          value: false
        }),
        Em.Object.create({
          step: 3,
          value: false
        }),
        Em.Object.create({
          step: 4,
          value: false
        })
      ]);
      installerController.set('isStepDisabled', steps);
      installerController.setLowerStepsDisable(3);
    });

    it('Should disable lower steps', function() {
      var expected = [
        {
          "step": 0,
          "value": true
        },
        {
          "step": 1,
          "value": true
        },
        {
          "step": 2,
          "value": true
        },
        {
          "step": 3,
          "value": false
        },
        {
          "step": 4,
          "value": false
        }
      ];
      var res = JSON.parse(JSON.stringify(installerController.get('isStepDisabled')));
      expect(res).to.eql(expected);
    });
  });

  describe('#totalSteps', function() {
    beforeEach(function() {
      installerController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);
    });

    it('Should return the number of steps', function() {
      var totalSteps = installerController.get('totalSteps');
      expect(totalSteps).to.eq(installerController.get("steps").length);
    });
  })

  describe('#setStepsEnable', function() {
    beforeEach(function () {
      var steps = Em.A([
        Em.Object.create({
          step: 0,
          value: true
        }),
        Em.Object.create({
          step: 1,
          value: true
        }),
        Em.Object.create({
          step: 2,
          value: true
        }),
        Em.Object.create({
          step: 3,
          value: true
        }),
        Em.Object.create({
          step: 4,
          value: true
        })
      ]);
      
      installerController.set('isStepDisabled', steps);
      
      installerController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);
      
      installerController.set('currentStep', 2);
    });

    it('Should enable next steps', function() {
      var stepController = Em.Object.create({
        isStepDisabled: function () {
          return false;
        }
      });

      sinon.stub(installerController, 'getStepController').returns(stepController);

      var expected = [
        {
          "step": 0,
          "value": false
        },
        {
          "step": 1,
          "value": false
        },
        {
          "step": 2,
          "value": false
        },
        {
          "step": 3,
          "value": false
        },
        {
          "step": 4,
          "value": false
        }
      ];

      installerController.setStepsEnable();

      var res = JSON.parse(JSON.stringify(installerController.get('isStepDisabled')));
      expect(res).to.eql(expected);

      installerController.getStepController.restore();
    });
  });

  describe('#loadMap', function() {

    describe('Should load cluster', function() {
      var loadCluster = false;
      var checker = {
        load: function() {
          loadCluster = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['step0'][0].callback.call(checker);
      });

      it('cluster info is loaded', function () {
        expect(loadCluster).to.be.true;
      });
    });

    describe('Should load installOptions', function() {
      var installOptions = false;
      var checker = {
        load: function() {
          installOptions = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['step2'][0].callback.call(checker);
      });

      it('install option are loaded', function () {
        expect(installOptions).to.be.true;
      });
    });

    describe('Should load loadConfirmedHosts', function() {
      var loadConfirmedHosts = false;
      var checker = {
        loadConfirmedHosts: function() {
          loadConfirmedHosts = true;
        },
        loadRegisteredMpacks: sinon.stub()
      };

      beforeEach(function () {
        installerController.loadMap['step3'][0].callback.call(checker);
      });

      it('confirmed hosts are loaded', function () {
        expect(loadConfirmedHosts).to.be.true;
      });
    });

    describe('Should load registered mpacks', function () {
      it('maps registered mpacks', function () {
        sinon.stub(installerController, 'getDBProperty').returns([
          {
            MpackInfo: { mpack_name: "mpack1" }
          },
          {
            MpackInfo: { mpack_name: "mpack2" }
          },
          {
            MpackInfo: { mpack_name: "mpack3" }
          }
        ]);
        sinon.stub(App.stackMapper, 'map');
        installerController.loadRegisteredMpacks();
        expect(App.stackMapper.map.calledThrice).to.be.true;
        const serviceGroups = installerController.get('content.serviceGroups');
        expect(serviceGroups).to.deep.equal(["mpack1", "mpack2", "mpack3"]);
        installerController.getDBProperty.restore();
        App.stackMapper.map.restore();
      });  
    });

    describe('Should load loadServices (2)', function() {
      var setSkipSlavesStep = false;
      var loadMasterComponentHosts = false;
      var loadConfirmedHosts = false;
      var loadComponentsFromConfigs = false;
      var loadRecommendations = false;

      var checker = {
        setSkipSlavesStep: function() {
          setSkipSlavesStep = true;
        },
        loadMasterComponentHosts: function() {
          loadMasterComponentHosts = true;
        },
        loadConfirmedHosts: function() {
          loadConfirmedHosts = true;
        },
        loadComponentsFromConfigs: function() {
          loadComponentsFromConfigs = true;
        },
        loadRecommendations: function() {
          loadRecommendations = true;
        },
        getStepIndex: function () {
          return 0;
        },
        loadRegisteredMpacks: sinon.stub()
      };

      beforeEach(function () {
        installerController.loadMap['step5'][0].callback.call(checker);
      });

      it('confirmed hosts are loaded', function() {
        expect(loadConfirmedHosts).to.be.true;
      });

      it('`skipSlavesStep` is loaded', function() {
        expect(setSkipSlavesStep).to.be.true;
      });

      it('master components hosts are loaded', function() {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('components added via configs are loaded', function () {
        expect(loadComponentsFromConfigs).to.be.true;
      });

      it('recommendations are loaded', function() {
        expect(loadRecommendations).to.be.true;
      });

    });

    describe ('Should load serviceConfigGroups', function() {
      var loadServiceConfigGroups = false;
      var loadServiceConfigProperties = false;
      var loadCurrentHostGroups = false;
      var loadRecommendationsConfigs = false;
      var loadComponentsFromConfigs = false;
      var loadConfigThemes = false;

      var checker = {
        loadServiceConfigGroups: function() {
          loadServiceConfigGroups = true;
        },
        loadServiceConfigProperties: function() {
          loadServiceConfigProperties = true;
          return $.Deferred().resolve().promise();
        },
        loadCurrentHostGroups: function() {
          loadCurrentHostGroups = true;
        },
        loadRecommendationsConfigs: function() {
          loadRecommendationsConfigs = true;
        },
        loadComponentsFromConfigs: function() {
          loadComponentsFromConfigs = true;
        },
        loadConfigThemes: function() {
          loadConfigThemes = true;
          return $.Deferred().resolve().promise();
        }
      };

      beforeEach(function () {
        installerController.loadMap['step7'][0].callback.call(checker);
      });

      it('config groups are loaded', function () {
        expect(loadServiceConfigGroups).to.be.true;
      });

      it('config properties are loaded', function () {
        expect(loadServiceConfigProperties).to.be.true;
      });

      it('current host groups are loaded', function () {
        expect(loadCurrentHostGroups).to.be.true;
      });

      it('recommendations are loaded', function () {
        expect(loadRecommendationsConfigs).to.be.true;
      });

      it('components added via configs are loaded', function () {
        expect(loadComponentsFromConfigs).to.be.true;
      });

      it('config themes are loaded', function () {
        expect(loadConfigThemes).to.be.true;
      });

    });

    describe('Should load clients', function() {
      var loadSlaveComponentHosts = false;
      var loadClients = false;
      var loadRecommendations = false;
      var loadComponentsFromConfigs = false;

      var checker = {
        loadSlaveComponentHosts: function() {
          loadSlaveComponentHosts = true;
        },
        loadClients: function() {
          loadClients = true;
        },
        loadComponentsFromConfigs: function() {
          loadComponentsFromConfigs = true;
        },
        loadRecommendations: function() {
          loadRecommendations = true;
        }
      };

      beforeEach(function () {
        installerController.loadMap['step6'][0].callback.call(checker);
      });

      it('slave components hosts are loaded', function () {
        expect(loadSlaveComponentHosts).to.be.true;
      });

      it('clients are loaded', function () {
        expect(loadClients).to.be.true;
      });

      it('components added via configs are loaded', function () {
        expect(loadComponentsFromConfigs).to.be.true;
      });

      it('recommendations are loaded', function () {
        expect(loadRecommendations).to.be.true;
      });

    });

  });

  describe('#removeHosts', function() {
    var hostsDb = {
      'h1': {},
      'h2': {},
      'h3': {},
      'h4': {}
    };
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns(hostsDb);
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should remove hosts from the list', function() {
      var hosts = Em.A([
        {
          name: 'h1'
        },
        {
          name: 'h2'
        },
        {
          name: 'h3'
        }
      ]);
      installerController.removeHosts(hosts);
      expect(hostsDb).to.eql({
        'h4': {}
      });
    });
  });

  describe('#allHosts', function() {
    it ('Should return hosts', function() {
      var hosts = {
        'h1': {
          hostComponents: Em.A([])
        }
      };
      var masterComponentHosts = Em.A([
        {
          hostName: 'h1',
          component: 'component',
          display_name: 'n1'
        }
      ]);
      var slaveComponentHosts = Em.A([
        {
          hosts: Em.A([
          {
            hostName: 'h1'
          }
          ])
        }
      ]);
      installerController.set('content.hosts', hosts);
      installerController.set('content.masterComponentHosts', masterComponentHosts);
      installerController.set('content.slaveComponentHosts', slaveComponentHosts);
      var res = JSON.parse(JSON.stringify(installerController.get('allHosts')));
      expect(res).to.eql([
        {
          "hostComponents": [
            {
              "componentName": "component",
              "displayName": "n1"
            },
            {}
          ]
        }
      ]);
    });
  });

  describe('#saveClients', function() {
    var stepController;

    beforeEach(function () {
      stepController = Em.Object.create({
        content: Em.A([
          Em.Object.create({
            isInstalled: true,
            isSelected: true,
            serviceName: 'i1',
            serviceComponents: Em.A([
              Em.Object.create({
                isClient: true,
                componentName: 'name',
                displayName: 'dname'
              })
            ])
          }),
          Em.Object.create({
            isInstalled: false,
            isSelected: true,
            serviceName: 'i2',
            serviceComponents: Em.A([
              Em.Object.create({
                isClient: false
              })
            ])
          }),
          Em.Object.create({
            isInstalled: true,
            isSelected: false,
            serviceName: 'i3',
            serviceComponents: Em.A([
              Em.Object.create({
                isClient: false
              })
            ])
          })
        ])
      });
    });
    it ('Should return correct clients names', function() {
      installerController.saveClients(stepController);
      var res = JSON.parse(JSON.stringify(installerController.get('content.clients')));
      expect(res).to.eql([
        {
          "component_name": "name",
          "display_name": "dname",
          "isInstalled": false,
          "service_name": "i1"
        }
      ]);
    });
  });

  describe('#saveMasterComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should return hosts', function() {
      var stepController = Em.Object.create({
        selectedServicesMasters: Em.A([
          Em.Object.create({
            display_name: 'n1',
            component_name: 'c1',
            serviceId: 1,
            selectedHost: 'h1',
            mpackInstance: 'm1'
          })
        ])
      });
      installerController.saveMasterComponentHosts(stepController);
      expect(installerController.get('content.masterComponentHosts')).to.eql([
        {
          "display_name": "n1",
          "component": "c1",
          "serviceId": 1,
          "isInstalled": false,
          "host_id": 11,
          "serviceGroupName": "m1"
        }
      ]);
    });
  });

  describe('#loadConfirmedHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should load hosts from db', function() {
      installerController.loadConfirmedHosts();
      expect(installerController.get('content.hosts')).to.eql({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
  });

  describe('#loadMasterComponentHosts', function() {
    beforeEach(function () {
      installerController.set('content.masterComponentHosts', null);
      sinon.stub(installerController, 'getDBProperties', function() {
        return {
          masterComponentHosts: Em.A([
            {
              hostName: '',
              host_id: 11
            }
          ]),
          hosts: {
            'h1': {
              id: 11
            },
            'h3': {
              id: 13
            },
            'h2': {
              id: 12
            }
          }
        }
      });
    });
    afterEach(function () {
      installerController.getDBProperties.restore();
    });
    it('Should load hosts', function() {
      installerController.loadMasterComponentHosts();
      expect(installerController.get('content.masterComponentHosts')).to.eql([
        {
          "hostName": "h1",
          "host_id": 11
        }
      ]);
    });
  });

  describe('#loadSlaveComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperties', function() {
        return {
          hosts: {
            'h1': {
              id: 11
            },
            'h3': {
              id: 13
            },
            'h2': {
              id: 12
            }
          },
          slaveComponentHosts: Em.A([
            {
              hosts: Em.A([
                {
                  hostName: '',
                  host_id: 11
                }
              ])
            }
          ])
        };
      });
    });
    afterEach(function () {
      installerController.getDBProperties.restore();
    });
    it ('Should load slave hosts', function() {
      installerController.loadSlaveComponentHosts();
      expect(installerController.get('content.slaveComponentHosts')).to.eql([
        {
          "hosts": [
            {
              "hostName": "h1",
              "host_id": 11
            }
          ]
        }
      ]);
    });
  });

  describe('#getServerVersionSuccessCallback', function () {

    var cases = [
        {
          osFamily: 'redhat5',
          expected: false
        },
        {
          osFamily: 'redhat6',
          expected: true
        },
        {
          osFamily: 'suse11',
          expected: false
        }
      ],
      title = 'App.isManagedMySQLForHiveEnabled should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osFamily), function () {
        installerController.getServerVersionSuccessCallback({
          'RootServiceComponents': {
            'component_version': '',
            'properties': {
              'server.os_family': item.osFamily
            }
          }
        });
        expect(App.get('isManagedMySQLForHiveEnabled')).to.equal(item.expected);
      });
    });

  });

  describe('#postVersionDefinitionFileErrorCallback', function () {

    beforeEach(function () {
      sinon.stub(App, 'showAlertPopup', Em.K);
    });

    afterEach(function () {
      App.showAlertPopup.restore();
    });

    it('should delete VDF-data', function () {
      App.db.setLocalRepoVDFData({});
      expect(App.db.getLocalRepoVDFData()).to.not.be.an.object;
      installerController.postVersionDefinitionFileErrorCallback({}, {}, {}, {}, {dfd: $.Deferred()});
      expect(App.db.getLocalRepoVDFData()).to.be.undefined;
    });

  });

  describe('#setStepSaved', function() {
    beforeEach(function() {
      installerController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);

      installerController.set('content.stepsSavedState', null);
    });

    it('Should save step and unsave all subsequent steps when step is not saved yet', function() {
      var expected = Em.Object.create({
        "1": true,
        "2": false,
        "3": false,
        "4": false
      });

      installerController.setStepSaved('step1');
      var actual = installerController.get('content.stepsSavedState');
      expect(actual).to.deep.equal(expected);
      expect(installerController.getStepSavedState('step1')).to.be.true;
    });

    it('Should do nothing when step is already saved', function() {
      var expected = Em.Object.create({
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      installerController.set('content.stepsSavedState', expected);

      installerController.setStepSaved('step1');
      var actual = installerController.get('content.stepsSavedState');
      expect(actual).to.deep.equal(expected);
      expect(installerController.getStepSavedState('step1')).to.be.true;
    });
  });

  describe('#setStepUnsaved', function() {
    beforeEach(function() {
      installerController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);

      var initial = Em.Object.create({
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      installerController.set('content.stepsSavedState', initial);
    });

    it('Should set step to unsaved', function() {
      var expected = Em.Object.create({
        "1": false,
        "2": true,
        "3": true,
        "4": true
      })

      installerController.setStepUnsaved('step1');
      var actual = installerController.get('content.stepsSavedState');
      expect(actual).to.deep.equal(expected);
      expect(installerController.getStepSavedState('step1')).to.be.false;
    });
  });

  describe('#getStepSavedState', function() {
    beforeEach(function() {
      installerController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);

      var initial = Em.Object.create({
        "1": true,
        "2": false
      })
      installerController.set('content.stepsSavedState', initial);
    });

    it('Should return false for bad step name', function() {
      expect(installerController.getStepSavedState('step5')).to.be.false;
    });

    it('Should return false for step that was never saved', function() {
      expect(installerController.getStepSavedState('step0')).to.be.false;
    });
  });

  describe('#hasErrors', function () {
    before(function () {
      installerController.addError("There is an error.");
    });

    it('Should return true if there are errors.', function () {
      var hasErrors = installerController.get('hasErrors');

      expect(hasErrors).to.be.true;
    });

    it('Should return false if there are no errors.', function () {
      installerController.clearErrors();
      var hasErrors = installerController.get('hasErrors');

      expect(hasErrors).to.be.false;
    });
  });

  describe('#getStepController', function () {
    var wizardStep0Controller = {};
    var wizardStep2Controller = {};

    before(function () {
      installerController.set('steps', [
        "step0",
        "step1",
        "step2"
      ]);

      App.router.set('wizardStep0Controller', wizardStep0Controller);
      App.router.set('wizardStep2Controller', wizardStep2Controller);
    });

    it('Should return controller for the step number provided.', function () {
      var stepController = installerController.getStepController(2);
      expect(stepController).to.equal(wizardStep2Controller);
    });

    it('Should return controller for the step name provided.', function () {
      var stepController = installerController.getStepController("step0");
      expect(stepController).to.equal(wizardStep0Controller);
    });
  });
});
