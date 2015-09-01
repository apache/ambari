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

  describe('#init', function () {
    var c;
    beforeEach(function () {
      c = App.InstallerController.create({});
    });
    it('all steps are disabled by default', function () {
      expect(c.get('isStepDisabled.length') > 0 ).to.be.ok;
      expect(c.get('isStepDisabled').everyProperty('value', true)).to.be.ok;
    });
  });

  describe('#loadStacksVersionsSuccessCallback', function() {
    beforeEach(function () {
      sinon.stub(App.store, 'commit', Em.K);
    });
    afterEach(function () {
      App.store.commit.restore();
    });
    it ('Correct data', function() {
      installerController.set('loadStacksRequestsCounter', 1);
      installerController.loadStacksVersionsSuccessCallback(require('test/stack'));
      expect(installerController.get('content.stacks.length')).to.equal(2);
      expect(installerController.get('content.stacks').everyProperty('isSelected')).to.be.false;
      expect(installerController.get('content.stacks').mapProperty('id')).to.eql(['HDP-2.1','HDP-1.3']);
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

  describe('#getHosts', function() {
    it ('Should return empty array', function() {
      expect(installerController.getHosts()).to.eql([]);
    });
  });

  describe('#loadServices', function() {
    it ('Should resolve nothing', function() {
      var res = installerController.loadServices();
      res.then(function(data){
        expect(data).to.be.undefined;
      });
    });
  });

  describe('#checkRepoURL', function() {
     beforeEach(function () {
      sinon.stub(App.ajax, 'send', function(data){
        return null;
      });
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it ('Should reload installed stacks', function() {
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
              isSelected: true
            })
          ]),
          operatingSystems: Em.A([
            Em.Object.create({
              isSelected: true,
              repositories: Em.A([
                Em.Object.create({
                  errorTitle: '1',
                  errorContent: '1',
                  validation: ''
                })
              ])
            })
          ]) 
        })
      ]);
      var wizard = Em.Object.create({
        skipValidationChecked: true
      });
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
          "repositories": [
            {
              "isSelected": true
            }
          ],
          "operatingSystems": [
            {
              "isSelected": true,
              "repositories": [
                {
                  "errorTitle": "",
                  "errorContent": "",
                  "validation": "icon-repeat"
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
    it ('Should check stacks for sucess', function() {
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
          resolve: function() {
            resolve = true;
          }
        }
      };
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
                  "errorTitle": "1",
                  "errorContent": "1",
                  "validation": "icon-ok"
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
    it ('Should check stacks for error', function() {
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
                  "validation": "icon-exclamation-sign"
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

  describe('#loadStacks', function() {
    it ('Should resolve promise with true', function() {
      installerController.set('content.stacks', Em.Object.create({
        length: 2
      }));
      var res = installerController.loadStacks();
      res.then(function(data){
        expect(data).to.be.true;
      });
    });
    it ('Should resolve promise with false', function() {
      installerController.set('content.stacks', null);
      var res = installerController.loadStacks();
      res.then(function(data){
        expect(data).to.be.false;
      });
    });
  });

  describe('#setLowerStepsDisable', function() {
    it ('Should disable lower steps', function() {
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

  describe('#setStepsEnable', function() {
    it ('Should enable next steps', function() {
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
      installerController.totalSteps = steps.length - 1;
      installerController.set('currentStep',2);
      var expected = [
        {
          "step": 0,
          "value": false
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
          "value": true
        },
        {
          "step": 4,
          "value": true
        }
      ];

      var res = JSON.parse(JSON.stringify(installerController.get('isStepDisabled')));

      expect(res).to.eql(expected);
    });
  });

  describe('#loadMap', function() {
    it ('Should load cluster', function() {
      var loadCluster = false;
      var checker = {
        load: function() {
          loadCluster = true;
        }
      };
      installerController.loadMap['0'][0].callback.call(checker);
      expect(loadCluster).to.be.true;
    });
    it ('Should load stacks', function() {
      var loadStacks = false;
      var checker = {
        loadStacks: function() {
          return {
            always: function() {
              loadStacks = true;
            }
          };
        }
      };
      installerController.loadMap['1'][0].callback.call(checker);
      expect(loadStacks).to.be.true;
    });
    it ('Should load stacks async', function() {
      var loadStacksVersions = false;
      var checker = {
        loadStacksVersions: function() {
          loadStacksVersions = true;
        }
      };
      installerController.loadMap['1'][1].callback.call(checker, true).then(function(data){
        expect(data).to.be.true;
      });
      expect(loadStacksVersions).to.be.false;
    });
    it ('Should load installOptions', function() {
      var loadStacks = false;
      var checker = {
        load: function() {
          loadStacks = true;
        }
      };
      installerController.loadMap['2'][0].callback.call(checker);
      expect(loadStacks).to.be.true;
    });
    it ('Should load loadConfirmedHosts', function() {
      var loadConfirmedHosts = false;
      var checker = {
        loadConfirmedHosts: function() {
          loadConfirmedHosts = true;
        }
      };
      installerController.loadMap['3'][0].callback.call(checker);
      expect(loadConfirmedHosts).to.be.true;
    });
    it ('Should load loadServices', function() {
      var loadServices = false;
      var checker = {
        loadServices: function() {
          loadServices = true;
        }
      };
      installerController.loadMap['4'][0].callback.call(checker);
      expect(loadServices).to.be.true;
    });
    it ('Should load loadServices', function() {
      var setSkipSlavesStep = false;
      var loadMasterComponentHosts = false;
      var loadConfirmedHosts = false;
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
        loadRecommendations: function() {
          loadRecommendations = true;
        }
      };
      installerController.loadMap['5'][0].callback.call(checker);
      expect(loadConfirmedHosts).to.be.true;
      expect(setSkipSlavesStep).to.be.true;
      expect(loadMasterComponentHosts).to.be.true;
      expect(loadRecommendations).to.be.true;

    });
    it ('Should load serviceConfigGroups', function() {
      var loadServiceConfigGroups = false;
      var loadServiceConfigProperties = false;
      var loadCurrentHostGroups = false;
      var loadRecommendationsConfigs = false;
      var loadConfigThemes = false;

      var checker = {
        loadServiceConfigGroups: function() {
          loadServiceConfigGroups = true;
        },
        loadServiceConfigProperties: function() {
          loadServiceConfigProperties = true;
        },
        loadCurrentHostGroups: function() {
          loadCurrentHostGroups = true;
        },
        loadRecommendationsConfigs: function() {
          loadRecommendationsConfigs = true;
        },
        loadConfigThemes: function() {
          loadConfigThemes = true;
        }
      };
      installerController.loadMap['7'][0].callback.call(checker);
      expect(loadServiceConfigGroups).to.be.true;
      expect(loadServiceConfigProperties).to.be.true;
      expect(loadCurrentHostGroups).to.be.true;
      expect(loadRecommendationsConfigs).to.be.true;
      expect(loadConfigThemes).to.be.true;
    });
    it ('Should load clients', function() {
      var loadSlaveComponentHosts = false;
      var loadClients = false;
      var loadRecommendations = false;

      var checker = {
        loadSlaveComponentHosts: function() {
          loadSlaveComponentHosts = true;
        },
        loadClients: function() {
          loadClients = true;
        },
        loadRecommendations: function() {
          loadRecommendations = true;
        }
      };
      installerController.loadMap['6'][0].callback.call(checker);
      expect(loadSlaveComponentHosts).to.be.true;
      expect(loadClients).to.be.true;
      expect(loadRecommendations).to.be.true;
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
          disk_info: Em.A([{
            available: 1,
            size: 10
          }]),
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
          "diskInfo": [
            {
              "available": 1,
              "size": 10
            }
          ],
          "diskTotal": 0.0000095367431640625,
          "diskFree": 9.5367431640625e-7,
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

  describe('#loadServiceConfigProperties', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getDBProperty').returns({
        value: 2
      });
    });
    afterEach(function () {
      installerController.getDBProperty.restore();
    });
    it ('Should load service config property', function() {
      installerController.loadServiceConfigProperties();
      expect(installerController.get('content.serviceConfigProperties')).to.eql({
        "value": 2
      });
    });
  });

  describe('#saveServices', function() {
    it ('Should return correct names', function() {
      var stepController = Em.A([
        Em.Object.create({
          isInstalled: true,
          isSelected: true,
          serviceName: 'i1'
        }),
        Em.Object.create({
          isInstalled: false,
          isSelected: true,
          serviceName: 'i2'
        }),
        Em.Object.create({
          isInstalled: true,
          isSelected: false,
          serviceName: 'i3'
        })
      ]);
      installerController.saveServices(stepController);
      expect(installerController.get('content.selectedServiceNames')).to.eql(['i1','i2']);
      expect(installerController.get('content.installedServiceNames')).to.eql(['i1','i3']);
    });
  });

  describe('#saveClients', function() {
    it ('Should return correct clients names', function() {
      var stepController = Em.Object.create({
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
      installerController.saveClients(stepController);
      var res = JSON.parse(JSON.stringify(installerController.get('content.clients')));
      expect(res).to.eql([
        {
          "component_name": "name",
          "display_name": "dname",
          "isInstalled": false
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
            selectedHost: 'h1'
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
          "host_id": 11
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
      sinon.stub(installerController, 'getDBProperties', function(key) {
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
    it ('Should load hosts', function() {
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

  describe('#validateJDKVersion', function() {
    var tests = [
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.7',
          isSelected: true
        })],
        m: 'JDK 1.8, stack supports 1.6-1.7 popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.8, stack supports 1.7-1.8 procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports 1.6-1.8, popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: null,
          maxJdkVersion: null,
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max and min are null, procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: '1.5',
          maxJdkVersion: null,
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max is missed and min is 1.5, procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.6'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: '1.5',
          maxJdkVersion: null,
          isSelected: true
        })],
        m: 'JDK 1.6, stack supports max is missed and min is 1.5, popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: null,
          maxJdkVersion: '1.5',
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max 1.5 and min is missed, procceed installation without warning'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.5'
        },
        successCallbackCalled: false,
        popupCalled: true,
        stacks: [Em.Object.create({
          minJdkVersion: null,
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.5, stack supports max 1.8 and min is missed, popup should be displayed'
      },
      {
        isCustomJDK: false,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          isSelected: true
        })],
        m: 'JDK 1.8, min, max jdk missed in stack definition, procceed installation without warning'
      },
      {
        isCustomJDK: true,
        ambariProperties: {
          'java.version': '1.8'
        },
        successCallbackCalled: true,
        popupCalled: false,
        stacks: [Em.Object.create({
          minJdkVersion: '1.6',
          maxJdkVersion: '1.8',
          isSelected: true
        })],
        m: 'JDK 1.8, custom jdk location used, procceed installation without warning'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        sinon.stub(App.Stack, 'find').returns(test.stacks);
        sinon.stub(App.router, 'get').withArgs('clusterController.isCustomJDK').returns(test.isCustomJDK)
          .withArgs('clusterController.ambariProperties').returns(test.ambariProperties);
        sinon.stub(App, 'showConfirmationPopup', Em.K);
        var successCallback = sinon.spy();
        installerController.validateJDKVersion(successCallback);
        expect(successCallback.called).to.be.eql(test.successCallbackCalled);
        expect(App.showConfirmationPopup.called).to.be.eql(test.popupCalled);
        App.router.get.restore();
        App.Stack.find.restore();
        App.showConfirmationPopup.restore();
      });
    });
  });

});
