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
var c;
var testHelpers = require('test/helpers');
var hostsManagement = require('utils/hosts');

describe('App.ManageConfigGroupsController', function() {

  beforeEach(function() {
    c = App.ManageConfigGroupsController.create({});
  });

  var manageConfigGroupsController = App.ManageConfigGroupsController.create({});

  describe('#addConfigGroup', function() {
    beforeEach(function() {
      manageConfigGroupsController.addConfigGroup();
    });

    describe("#validate", function() {
      it("should display no warning if user inputs valid characters into group name", function() {

        manageConfigGroupsController.addGroupPopup.set('configGroupName', 'hello');

        expect(manageConfigGroupsController.addGroupPopup.warningMessage).to.be.empty;
      });

      it("should display warning if user inputs invalid characters into group name", function() {
        manageConfigGroupsController.addGroupPopup.set('configGroupName', '/{"!@#$%');

        expect(manageConfigGroupsController.addGroupPopup.warningMessage).to.equal('Invalid Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
      });
    });
  });

  describe('#renameConfigGroup', function() {
    beforeEach(function() {
      var configGroup = Ember.Object.create ({
        name: 'name',
        description: 'description'
      });

      manageConfigGroupsController.set('selectedConfigGroup', configGroup);
      manageConfigGroupsController.renameConfigGroup();
    });

    describe("#validate", function() {
      it("should display no warning if user inputs valid characters into group name", function() {
        manageConfigGroupsController.renameGroupPopup.set('configGroupName', 'hello');

        expect(manageConfigGroupsController.renameGroupPopup.warningMessage).to.be.empty;
      });

      it("should display warning if user inputs invalid characters into group name", function() {
        manageConfigGroupsController.renameGroupPopup.set('configGroupName', '/{"!@#$%');

        expect(manageConfigGroupsController.renameGroupPopup.warningMessage).to.equal('Invalid Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
      });
    });
  });

  describe('#addHostsCallback', function() {

    beforeEach(function() {

      c.reopen({
        selectedConfigGroup: Em.Object.create({
          hosts: ['h1'],
          parentConfigGroup: Em.Object.create({
            hosts: ['h2', 'h3']
          })
        })
      });
    });

    it('should set hosts to selectedConfigGroup and remove them form default group', function () {

      c.addHostsCallback(['h2', 'h3']);

      expect(c.get('selectedConfigGroup.hosts')).to.include.members(['h1','h2','h3']);
      expect(c.get('selectedConfigGroup.parentConfigGroup.hosts').toArray()).to.be.empty;
    });

  });

  describe('#isHostsModified', function () {

    Em.A([
      {
        o: {
          toClearHosts: [],
          toDelete: [],
          toSetHosts: [],
          toCreate: []
        },
        e: false
      },
      {
        o: {
          toClearHosts: [{}],
          toDelete: [],
          toSetHosts: [],
          toCreate: []
        },
        e: true
      },
      {
        o: {
          toClearHosts: [],
          toDelete: [{}],
          toSetHosts: [],
          toCreate: []
        },
        e: true
      },
      {
        o: {
          toClearHosts: [],
          toDelete: [],
          toSetHosts: [{}],
          toCreate: []
        },
        e: true
      },
      {
        o: {
          toClearHosts: [],
          toDelete: [],
          toSetHosts: [],
          toCreate: [{}]
        },
        e: true
      }
    ]).forEach(function (test, index) {
      it('test #' + index, function () {
        c.reopen({
          isLoaded: true,
          hostsModifiedConfigGroups: test.o
        });
        expect(c.get('isHostsModified')).to.equal(test.e);
      });
    });

  });

  describe('#deleteConfigGroup', function () {

    beforeEach(function() {

      var defaultGroup = Em.Object.create({
        hosts: ['h2', 'h3'],
        isDefault: true
      });

      var selectedGroup = Em.Object.create({
        hosts: ['h1'],
        parentConfigGroup: defaultGroup
      });

      c.reopen({
        configGroups: [defaultGroup, selectedGroup],
        selectedConfigGroup: selectedGroup
      });

      sinon.stub(App.configGroupsMapper, 'deleteRecord', Em.K);
    });

    afterEach(function(){
      App.configGroupsMapper.deleteRecord.restore();
    });

    it('after deleting some config group, Default should be selected', function () {

      c.deleteConfigGroup();

      expect(c.get('configGroups.length')).to.equal(1);
      expect(c.get('selectedConfigGroup.hosts')).to.include.members(['h1','h2','h3']);
      expect(c.get('selectedConfigGroup.isDefault')).to.be.true;
    });

  });

  describe("#manageConfigurationGroups", function () {
    var service = Em.Object.create({});
    manageConfigGroupsController.set('hostsModifiedConfigGroups', {});
    describe("#controller passed", function () {
      var popup = manageConfigGroupsController.manageConfigurationGroups(Em.Object.create({
        content: Em.Object.create()
      }), service);

      describe("#onPrimary()", function () {
        beforeEach(function () {
          sinon.stub(popup, 'onPrimaryWizard', Em.K);
        });
        afterEach(function () {
          popup.onPrimaryWizard.restore();
        });
        it("onPrimaryWizard is called", function () {
          popup.onPrimary();
          expect(popup.onPrimaryWizard.calledOnce).to.be.true;
        });
      });

      describe("#onPrimaryWizard()", function () {

        var ctrl = Em.Object.create({
          selectedService: Em.Object.create({
            selected: false
          }),
          selectedServiceObserver: Em.K,
          setGroupsToDelete: Em.K
        });

        beforeEach(function () {
          sinon.spy(ctrl, 'selectedServiceObserver');
          sinon.spy(ctrl, 'setGroupsToDelete');
          sinon.stub(manageConfigGroupsController, 'persistConfigGroups', Em.K);
          sinon.stub(popup, 'updateConfigGroupOnServicePage', Em.K);
          sinon.stub(popup, 'hide', Em.K);
        });

        afterEach(function () {
          ctrl.setGroupsToDelete.restore();
          ctrl.selectedServiceObserver.restore();
          manageConfigGroupsController.persistConfigGroups.restore();
          popup.updateConfigGroupOnServicePage.restore();
          popup.hide.restore();
        });

        describe("groups deleted on 7th step", function () {

          beforeEach(function () {
            ctrl.set('name', 'wizardStep7Controller');
            popup.onPrimaryWizard(ctrl, {toDelete: [1]});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });
          it('setGroupsToDelete is called with [1]', function () {
            expect(ctrl.setGroupsToDelete.calledWith([1])).to.be.true;
          });
          it('persistConfigGroups is called once', function () {
            expect(manageConfigGroupsController.persistConfigGroups.calledOnce).to.be.true;
          });
          it('updateConfigGroupOnServicePage is called once', function () {
            expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
          });
          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });
        });

        describe("wizard not on 7th step", function () {

          beforeEach(function () {
            ctrl.set('name', '');
            popup.onPrimaryWizard(ctrl, {});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });

          it('setGroupsToDelete is not called', function () {
            expect(ctrl.setGroupsToDelete.called).to.be.false;
          });

          it('persistConfigGroups is not called', function () {
            expect(manageConfigGroupsController.persistConfigGroups.called).to.be.false;
          });

          it('updateConfigGroupOnServicePage is not called', function () {
            expect(popup.updateConfigGroupOnServicePage.called).to.be.false;
          });

          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });
        });

        describe("wizard on 7th step, service selected", function () {

          beforeEach(function () {
            ctrl.set('name', 'wizardStep7Controller');
            ctrl.set('selectedService.selected', true);
            popup.onPrimaryWizard(ctrl, {toDelete: [1]});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });
          it('setGroupsToDelete is not called', function () {
            expect(ctrl.setGroupsToDelete.called).to.be.false;
          });
          it('persistConfigGroups is called once', function () {
            expect(manageConfigGroupsController.persistConfigGroups.calledOnce).to.be.true;
          });
          it('updateConfigGroupOnServicePage is called once', function () {
            expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
          });
          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });
        });

        describe("wizard on 7th step, no groups to delete", function () {

          beforeEach(function () {
            ctrl.set('name', 'wizardStep7Controller');
            ctrl.set('selectedService.selected', false);
            popup.onPrimaryWizard(ctrl, {toDelete: []});
          });

          it('selectedServiceObserver is called once', function () {
            expect(ctrl.selectedServiceObserver.calledOnce).to.be.true;
          });
          it('setGroupsToDelete is not called', function () {
            expect(ctrl.setGroupsToDelete.called).to.be.false;
          });
          it('persistConfigGroups is called once', function () {
            expect(manageConfigGroupsController.persistConfigGroups.calledOnce).to.be.true;
          });
          it('updateConfigGroupOnServicePage is called once', function () {
            expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
          });
          it('hide is called once', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });

        });

      });
    });

    describe("#controller not passed", function () {
      var popup = manageConfigGroupsController.manageConfigurationGroups(null, service);

      describe("#onPrimary()", function () {
        beforeEach(function () {
          sinon.stub(popup, 'runClearCGQueue').returns({
            done: function (callback) {
              callback();
            }
          });
          sinon.stub(popup, 'runModifyCGQueue').returns({
            done: function (callback) {
              callback();
            }
          });
          sinon.stub(popup, 'runCreateCGQueue').returns({
            done: function (callback) {
              callback();
            }
          });
          sinon.stub(popup, 'updateConfigGroupOnServicePage', Em.K);
          sinon.stub(popup, 'hide', Em.K);
          manageConfigGroupsController.set('hostsModifiedConfigGroups', {toCreate: []});
          popup.onPrimary();
        });
        afterEach(function () {
          popup.runCreateCGQueue.restore();
          popup.runModifyCGQueue.restore();
          popup.runClearCGQueue.restore();
          popup.updateConfigGroupOnServicePage.restore();
          popup.hide.restore();
        });
        it("runClearCGQueue is called", function () {
          expect(popup.runClearCGQueue.calledOnce).to.be.true;
        });
        it("runModifyCGQueue is called", function () {
          expect(popup.runModifyCGQueue.calledOnce).to.be.true;
        });
        it("runCreateCGQueue is called", function () {
          expect(popup.runCreateCGQueue.calledOnce).to.be.true;
        });
        it("updateConfigGroupOnServicePage is called", function () {
          expect(popup.updateConfigGroupOnServicePage.calledOnce).to.be.true;
        });
        it("hide is called", function () {
          expect(popup.hide.calledOnce).to.be.true;
        });
      });
      describe("#runClearCGQueue()", function () {
        beforeEach(function () {
          sinon.stub(manageConfigGroupsController, 'updateConfigurationGroup', Em.K);
          sinon.stub(manageConfigGroupsController, 'deleteConfigurationGroup', Em.K);
          popup.runClearCGQueue(Em.K, {
            initialGroups: [],
            toClearHosts: [Em.Object.create()],
            toDelete: [1]
          });
        });
        afterEach(function () {
          manageConfigGroupsController.updateConfigurationGroup.restore();
          manageConfigGroupsController.deleteConfigurationGroup.restore();
        });
        it("updateConfigurationGroup is called once", function () {
          expect(manageConfigGroupsController.updateConfigurationGroup.calledOnce).to.be.true;
        });
        it("deleteConfigurationGroup is called once", function () {
          expect(manageConfigGroupsController.deleteConfigurationGroup.calledOnce).to.be.true;
        });
      });
      describe("#runModifyCGQueue()", function () {
        beforeEach(function () {
          sinon.stub(manageConfigGroupsController, 'updateConfigurationGroup', Em.K);
        });
        afterEach(function () {
          manageConfigGroupsController.updateConfigurationGroup.restore();
        });
        it("updateConfigurationGroup is called once", function () {
          popup.runModifyCGQueue(Em.K, {toSetHosts: [1]});
          expect(manageConfigGroupsController.updateConfigurationGroup.calledOnce).to.be.true;
        });
      });
      describe("#runCreateCGQueue()", function () {
        beforeEach(function () {
          sinon.stub(manageConfigGroupsController, 'postNewConfigurationGroup', Em.K);
        });
        afterEach(function () {
          manageConfigGroupsController.postNewConfigurationGroup.restore();
        });
        it("postNewConfigurationGroup is called once", function () {
          popup.runCreateCGQueue(Em.K, {toCreate: [1]});
          expect(manageConfigGroupsController.postNewConfigurationGroup.calledOnce).to.be.true;
        });
      });
    });
  });

  describe('#_onLoadPropertiesSuccess', function () {

    var data = {
      items: [
        {
          type: 'type1',
          tag: 'tag1',
          properties: {
            prop1: 'val1',
            prop2: 'val2'
          }
        },
        {
          type: 'type1',
          tag: 'tag2',
          properties: {
            prop3: 'val3'
          }
        },
        {
          type: 'type2',
          tag: 'tag1',
          properties: {
            prop4: 'val4'
          }
        }
      ]
    };
    var params = {
      typeTagToGroupMap: {
        'type1///tag1': 'group1',
        'type1///tag2': 'group2',
        'type2///tag1': 'group3'
      }
    };
    var configGroups = [
      Em.Object.create({
        name: 'group1',
        properties: []
      }),
      Em.Object.create({
        name: 'group2',
        properties: []
      }),
      Em.Object.create({
        name: 'group3',
        properties: []
      }),
      Em.Object.create({
        name: 'group4',
        properties: []
      })
    ];

    beforeEach(function () {
      sinon.stub(c, 'resortConfigGroup', Em.K);
    });

    afterEach(function () {
      c.resortConfigGroup.restore();
    });

    it('should set properties to config groups', function () {
      c.set('configGroups', configGroups);
      c._onLoadPropertiesSuccess(data, null, params);
      expect(JSON.stringify(c.get('configGroups'))).to.equal(JSON.stringify([
        Em.Object.create({
          properties: [
            {
              name: 'prop1',
              value: 'val1',
              type: 'type1'
            },
            {
              name: 'prop2',
              value: 'val2',
              type: 'type1'
            }
          ],
          name: 'group1'
        }),
        Em.Object.create({
          properties: [
            {
              name: 'prop3',
              value: 'val3',
              type: 'type1'
            }
          ],
          name: 'group2'
        }),
        Em.Object.create({
          properties: [
            {
              name: 'prop4',
              value: 'val4',
              type: 'type2'
            }
          ],
          name: 'group3'
        }),
        Em.Object.create({
          properties: [],
          name: 'group4'
        })
      ]));
    });

  });

  describe('#componentsForFilter', function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return [
          Em.Object.create({
            serviceName: 'HDFS'
          }),
          Em.Object.create({
            serviceName: 'noHDFS'
          }),
          Em.Object.create({
            serviceName: 'HDFS'
          })
        ];
      });
      c.set('serviceName', 'HDFS');
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });

    it('should map components for current service', function () {
      expect(c.get('componentsForFilter')).to.have.property('length').equal(2);
    });

    it('no one is selected', function () {
      expect(c.get('componentsForFilter').mapProperty('selected')).to.be.eql([false, false]);
    });

  });

  describe('#getNewlyAddedHostComponentsMap', function() {
    beforeEach(function() {
      this.mockGet = sinon.stub(App.router, 'get');
      this.mockGet.withArgs('addServiceController.content.clients').returns([
        {
          isInstalled: true,
          component_name: 'Client1',
          display_name: 'client1'
        },
        {
          isInstalled: false,
          component_name: 'Client2',
          display_name: 'client2'
        }
      ]);
      this.mockGet.withArgs('addServiceController.content.masterComponentHosts').returns([
        {
          isInstalled: true,
          hostName: 'host1',
          component: 'Master1',
          display_name: 'master1'
        },
        {
          isInstalled: false,
          hostName: 'host2',
          component: 'Master2',
          display_name: 'master2'
        }
      ]);
      this.mockGet.withArgs('addServiceController.content.slaveComponentHosts').returns([
        {
          componentName: 'Slave1',
          displayName: 'slave1',
          hosts: [
            {
              hostName: 'host1',
              isInstalled: false
            }
          ]
        },
        {
          componentName: 'Slave2',
          displayName: 'slave2',
          hosts: [
            {
              hostName: 'host2',
              isInstalled: true
            }
          ]
        },
        {
          componentName: 'CLIENT',
          displayName: 'client',
          hosts: [
            {
              hostName: 'host1',
              isInstalled: false
            }
          ]
        }
      ]);
    });
    afterEach(function() {
      this.mockGet.restore();
    });

    it('should return host-components map', function() {
      expect(JSON.stringify(c.getNewlyAddedHostComponentsMap())).to.be.equal(JSON.stringify({
        "host2": [
          {
            "componentName": "Master2",
            "displayName": "master2"
          }
        ],
        "host1": [
          {
            "componentName": "Slave1",
            "displayName": "slave1"
          },
          {
            "componentName": "Client2",
            "displayName": "client2"
          }
        ]
      }));
    });
  });

  describe('#isDeleteHostsDisabled', function () {

    it('should return true{1}', function () {
      c.propertyDidChange('isDeleteHostsDisabled');
      expect(c.get('isDeleteHostsDisabled')).to.be.true;
    });

    it('should return true{2}', function () {
      c.set('selectedConfigGroup', Em.Object.create({isDefault: true}));
      c.set('selectedHosts', []);
      c.propertyDidChange('isDeleteHostsDisabled');
      expect(c.get('isDeleteHostsDisabled')).to.be.true;
    });

    it('should return false', function () {
      c.set('selectedConfigGroup', Em.Object.create({isDefault: false}));
      c.set('selectedHosts', ['host1']);
      c.propertyDidChange('isDeleteHostsDisabled');
      expect(c.get('isDeleteHostsDisabled')).to.be.false;
    });
  });

  describe('#tooltipText', function () {
    var text1 = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sit amet felis quam. Sed facilisis efficitur urna, a gravida augue. Morbi non justo gravida, congue eros eu, lacinia sem. Nam dolor mauris, pellentesque quis arcu ac, hendrerit efficitur nisl. Nullam feugiat vulputate nibh, eu vulputate est tempus suscipit. Donec at erat sit amet nisl vulputate gravida. Nullam ultricies dignissim arcu. Fusce mattis libero vel nunc ultrices, sit amet cursus libero lacinia. Praesent non imperdiet orci. Proin vitae placerat lorem.';

    var text2 = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sit amet felis quam. Sed facilisis efficitur urna, a gravida augue. Morbi non justo gravida, congue eros eu, lacinia sem. Nam dolor mauris, pellentesque quis arcu ac, hendrerit efficitur nisl. Nullam feugiat vulputate nibh, eu vulputate est tempus suscipit. Donec at erat sit amet nisl vulputate gravida. Nullam ultricies dignissim arcu. Fusce mattis libero vel nunc ultrices, sit amet cursus libero lacinia. Praesent non imperdiet<br/> orci.<br/> Proin vitae placerat lorem.';

    it('should return text', function () {
      c.set('selectedConfigGroup', Em.Object.create({propertiesList: 'some text'}));
      c.propertyDidChange('tooltipText');
      expect(c.get('tooltipText')).to.equal('some text');
    });

    it('should return trimmed text{1}', function () {
      c.set('selectedConfigGroup', Em.Object.create({propertiesList: text1}));
      c.propertyDidChange('tooltipText');
      expect(c.get('tooltipText')).to.equal('Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sit amet felis quam. Sed facilisis efficitur urna, a gravida augue. Morbi non justo gravida, congue eros eu, lacinia sem. Nam dolor mauris, pellentesque quis arcu ac, hendrerit efficitur nisl. Nullam feugiat vulputate nibh, eu vulputate est tempus suscipit. Donec at erat sit amet nisl vulputate gravida. Nullam ultricies dignissim arcu. Fusce mattis libero vel nunc ultrices, sit amet cursus libero lacinia. Praesent non imperdiet orc ...');
    });

    it('should return trimmed text{2}', function () {
      c.set('selectedConfigGroup', Em.Object.create({propertiesList: text2}));
      c.propertyDidChange('tooltipText');
      expect(c.get('tooltipText')).to.equal('Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sit amet felis quam. Sed facilisis efficitur urna, a gravida augue. Morbi non justo gravida, congue eros eu, lacinia sem. Nam dolor mauris, pellentesque quis arcu ac, hendrerit efficitur nisl. Nullam feugiat vulputate nibh, eu vulputate est tempus suscipit. Donec at erat sit amet nisl vulputate gravida. Nullam ultricies dignissim arcu. Fusce mattis libero vel nunc ultrices, sit amet cursus libero lacinia. Praesent non imperdiet<br/> and 1 more property');
    });
  });

  describe('#hostsModifiedConfigGroupsObsOnce', function () {
    var groups = [Em.Object.create({
      id: 123,
      name: 'group1',
      description: 'group description',
      hosts: ['host1'],
      serviceName: 's1',
      desiredConfigs: [],
      properties: [],
      is_default: false
    })];

    beforeEach(function () {
      this.mock = sinon.stub(App.ServiceConfigGroup, 'find');
    });
    afterEach(function () {
      this.mock.restore();
    });

    it('should return false', function () {
      c.set('isLoaded', false);
      expect(c.hostsModifiedConfigGroupsObsOnce()).to.be.false;
    });

    it('should set hostsModifiedConfigGroups{1}', function () {
      c.set('isLoaded', true);
      c.set('configGroups', [Em.Object.create({
        id: 123,
        name: 'group1',
        description: 'group description',
        hosts: ['host1', 'host2'],
        serviceName: 's1',
        desired_configs: [],
        properties: [],
        is_default: false
      })]);
      c.set('originalConfigGroups', groups);
      c.hostsModifiedConfigGroupsObsOnce();
      expect(c.get('hostsModifiedConfigGroups').initialGroups[0]).to.eql(groups[0]);
    });

    it('should set hostsModifiedConfigGroups{2}', function () {
      c.set('isLoaded', true);
      c.set('configGroups', [Em.Object.create({
        id: 123,
        name: 'group2',
        description: 'group2 description',
        hosts: ['host1'],
        serviceName: 's1',
        desired_configs: [],
        properties: [],
        is_default: false
      })]);
      c.set('originalConfigGroups', groups);
      c.hostsModifiedConfigGroupsObsOnce();
      expect(c.get('hostsModifiedConfigGroups').initialGroups[0]).to.eql(groups[0]);
      expect(c.get('hostsModifiedConfigGroups').toSetHosts[0]).to.eql(Em.Object.create({
        id: 123,
        name: 'group2',
        description: 'group2 description',
        hosts: ['host1'],
        serviceName: 's1',
        desired_configs: [],
        properties: [],
        is_default: false
      }));
    });

    it('should set hostsModifiedConfigGroups{3}', function () {
      c.set('isLoaded', true);
      c.set('configGroups', groups);
      c.set('originalConfigGroups', groups);
      c.hostsModifiedConfigGroupsObsOnce();
      expect(c.get('hostsModifiedConfigGroups').initialGroups[0]).to.eql(groups[0]);
    });

    it('should set hostsModifiedConfigGroups{4}', function () {
      c.set('isLoaded', true);
      c.set('configGroups', groups);
      c.hostsModifiedConfigGroupsObsOnce();
      expect(JSON.stringify(c.get('hostsModifiedConfigGroups').toCreate[0])).to.equal(JSON.stringify({
          id: 123,
          name: 'group1',
          description: 'group description',
          hosts: ['host1'],
          service_id: 's1',
          desired_configs: [],
          properties: []
        })
      );
    });
  });

  describe('#resortConfigGroup', function () {

    it('should resort config groups', function () {
      c.set('configGroups', [
        Em.Object.create({
          name: 'group1'
        }),
        Em.Object.create({
          name: 'group2',
          isDefault: true
        })
      ]);
      c.resortConfigGroup();
      expect(JSON.stringify(c.get('configGroups'))).to.equal(JSON.stringify([
        Em.Object.create({
          isDefault: true,
          name: 'group2',
        }),
        Em.Object.create({
          name: 'group1',
        })
      ]));
    });
  });

  describe('#loadHosts', function () {

    beforeEach(function () {
      sinon.stub(c, 'loadInstallerHostsFromServer');
      sinon.stub(c, 'loadHostsFromServer');
      sinon.stub(c, 'loadConfigGroups');
      sinon.stub(App.router, 'get').withArgs('installerController').returns(Em.Object.create({
          allHosts: [
            {
              hostName: 'host1'
            }
          ]
        })
      )
    });
    afterEach(function () {
      c.loadInstallerHostsFromServer.restore();
      c.loadHostsFromServer.restore();
      c.loadConfigGroups.restore();
      App.router.get.restore();
    });

    it('should load config groups{1}', function () {
      c.set('isInstaller', true);
      c.set('isAddService', false);
      c.loadHosts();
      expect(c.loadInstallerHostsFromServer.calledOnce).to.be.true;
      expect(c.loadConfigGroups.calledOnce).to.be.true;
    });

    it('should load config groups{2}', function () {
      c.set('isInstaller', true);
      c.set('isAddService', true);
      c.loadHosts();
      expect(c.loadHostsFromServer.calledOnce).to.be.true;
      expect(c.loadConfigGroups.calledOnce).to.be.true;
    });
  });

  describe('#_loadHostsFromServerSuccessCallback', function () {
    var data = {
      items: [
        {
          id: 'host1',
          ip: '0.0.0.0',
          osType: 'os',
          osArch: 'arch',
          host_name: 'host1',
          public_host_name: 'phost1',
          cpu_count: 4,
          total_mem: 6044,
          host_components: [
            {
              HostRoles: {
                component_name: 'host1'
              },
            }
          ],
          Hosts: {
            host_name: 'host1',
            disk_info: [
              {
                type: 'ext',
                available: 2048,
                size: 1024
              },
              {
                type: 'ext',
                available: 4096,
                size: 2048
              }
            ]
          }
        }
      ]
    };

    beforeEach(function () {
      sinon.stub(c, 'getNewlyAddedHostComponentsMap').returns({host1: {}});
    });
    afterEach(function () {
      c.getNewlyAddedHostComponentsMap.restore();
    });

    it('should set cluster hosts', function () {
      c.set('isAddService', true);
      c._loadHostsFromServerSuccessCallback(data);
      expect(JSON.stringify(c.get('clusterHosts'))).to.equal('[{"id":"host1","hostName":"host1","diskTotal":"3 MB","diskFree":"6 MB","disksMounted":2,"hostComponents":[{"componentName":"host1","displayName":"Host1"},{}]}]');
    });
  });

  describe('#loadConfigGroups', function () {

    beforeEach(function () {
      sinon.stub(c, 'generateOriginalConfigGroups').returns([]);
      sinon.stub(c, 'setProperties');
      sinon.stub(App.router, 'get').returns([]);
    });
    afterEach(function () {
      c.generateOriginalConfigGroups.restore();
      c.setProperties.restore();
      App.router.get.restore();
    });

    it('should set properties', function () {
      c.set('isInstaller', true);
      c.loadConfigGroups('s1');
      expect(c.setProperties.calledOnce).to.be.true;
    });

    it('should send ajax request', function () {
      var args = testHelpers.findAjaxRequest('name', 'service.load_config_groups');
      c.set('isInstaller', false);
      c.loadConfigGroups('s1');
      expect(args).to.exists;
    });
  });

  describe('#_onLoadConfigGroupsSuccess', function () {

    beforeEach(function () {
      sinon.stub(c, 'generateOriginalConfigGroups').returns([
        {
          name: 'group1',
          desired_configs: [{}]
        }
      ]);
      sinon.stub(c, 'loadProperties');
      sinon.stub(App.ServiceConfigGroup, 'find').returns([{serviceName: 's1'}]);
      sinon.stub(App.configGroupsMapper, 'map');
    });
    afterEach(function () {
      c.generateOriginalConfigGroups.restore();
      c.loadProperties.restore();
      App.ServiceConfigGroup.find.restore();
      App.configGroupsMapper.map.restore();
    });

    it('should load properties', function () {
      c.set('isInstaller', true);
      c._onLoadConfigGroupsSuccess({});
      expect(App.configGroupsMapper.map.calledOnce).to.be.true;
      expect(c.loadProperties.calledOnce).to.be.true;
    });
  });

  describe('#generateOriginalConfigGroups', function () {

    var groups = ['group1', 'group2'];

    beforeEach(function () {
      sinon.stub(c, 'createOriginalRecord');
    });
    afterEach(function () {
      c.createOriginalRecord.restore();
    });

    it('should generate original config groups', function () {
      c.generateOriginalConfigGroups(groups);
      expect(c.createOriginalRecord.callCount).to.equal(groups.length);
    });
  });

  describe('#createOriginalRecord', function () {
    var group = Em.Object.create({
      id: 123,
      name: 'group1',
      description: 'group description',
      hosts: ['host1'],
      serviceName: 's1',
      desiredConfigs: [],
      properties: [],
      is_default: false
    });

    it('should create original config group record', function () {
      expect(JSON.stringify(c.createOriginalRecord(group))).to.equal('{"id":123,"name":"group1","service_name":"s1","description":"group description","hosts":["host1"],"service_id":"s1","desired_configs":[],"child_config_groups":[],"properties":[]}');
    });
  });

  describe('#loadProperties', function () {
    var group = {
      group: {
        type: 'type'
      }
    };

    it('should send ajax request', function () {
      c.loadProperties(group);
      var args = testHelpers.findAjaxRequest('name', 'config.host_overrides');
      expect(args).to.exists;
    });
  });

  describe('#showProperties', function () {

    beforeEach(function () {
      sinon.stub(App, 'showAlertPopup');
    });
    afterEach(function () {
      App.showAlertPopup.restore();
    });

    it('should show alert popup', function () {
      c.set('selectedConfigGroup', {propertiesList: '123'});
      c.showProperties();
      expect(App.showAlertPopup.calledOnce).to.be.true;
    });
  });

  describe('#addHosts', function () {

    beforeEach(function () {
      sinon.stub(hostsManagement, 'launchHostsSelectionDialog');
    });
    afterEach(function () {
      hostsManagement.launchHostsSelectionDialog.restore();
    });

    it('should launch host selection dialog', function () {
      c.set('selectedConfigGroup', {availableHosts: ['host1'], displayName: 'group1'});
      c.addHosts();
      expect(hostsManagement.launchHostsSelectionDialog.calledOnce).to.be.true;
    });

    it('should return false', function () {
      c.set('selectedConfigGroup', {isAddHostsDisabled: true});
      expect(c.addHosts()).to.be.false;
    });
  });

  describe('#deleteHosts', function () {

    it('should set new hosts', function () {
      c.set('selectedHosts', ['host1']);
      c.set('selectedConfigGroup', Em.Object.create({parentConfigGroup: Em.Object.create({hosts: ['host1']}), hosts: ['host1', 'host2']}));
      c.deleteHosts();
      expect(c.get('selectedHosts')).to.eql([]);
      expect(c.get('selectedConfigGroup.hosts')[0]).to.equal('host2');
    });

    it('should return false', function () {
      c.set('isDeleteHostsDisabled', true);
      expect(c.deleteHosts()).to.be.undefined;
    });
  });

  describe('#confirmDelete', function () {

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationPopup');
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('should show show confirmation popup', function () {
      c.confirmDelete();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe('#duplicateConfigGroup', function () {

    beforeEach(function () {
      sinon.stub(c, 'addConfigGroup');
    });
    afterEach(function () {
      c.addConfigGroup.restore();
    });

    it('should add config group', function () {
      c.duplicateConfigGroup();
      expect(c.addConfigGroup.calledWith(true)).to.be.true;
    });
  });

  describe('#loadInstallerHostsFromServer', function () {

    it('should send ajax request', function () {
      var hosts = ['host1', 'host2'];
      c.loadInstallerHostsFromServer(hosts);
      var args = testHelpers.findAjaxRequest('name', 'hosts.info.install');
      expect(args[0].data.hostNames).to.eql(hosts);
    });
  });

  describe('#loadInstallerHostsSuccessCallback', function () {
    var data = {
      items: [
        {
          Hosts: {
            id: 'host1',
            ip: '0.0.0.0',
            osType: 'os',
            osArch: 'arch',
            host_name: 'host1',
            public_host_name: 'phost1',
            cpu_count: 4,
            total_mem: 6044,
            disk_info: [
              {
                size: 2048,
                available: 1024
              }
            ]
          }
        }
      ]
    };

    it('should set cluster hosts', function () {
      App.router.installerController.reopen(Em.Object.create({
        allHosts: [{hostName: 'host1'}],
        content: Em.Object.create({
          slaveComponentHosts: [{hosts: [{hostName: 'host1'}], componentName: 'CLIENT'}],
          clients: [{component_name: 'c1', display_name: 'c1'}]
        })
      }))
      c.loadInstallerHostsSuccessCallback(data);
      expect(JSON.stringify(c.get('clusterHosts')[0])).to.equal('{"id":"host1","ip":"0.0.0.0","hostName":"host1","publicHostName":"phost1","cpu":4,"memory":"6044.00","diskInfo":[{"size":2048,"available":1024}],"diskTotal":0.001953125,"diskFree":0.0009765625,"hostComponents":[{"componentName":"c1","displayName":"c1"}]}');
    });
  });
});
