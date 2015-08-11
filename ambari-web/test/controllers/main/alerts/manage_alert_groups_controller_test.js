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

var manageAlertGroupsController;
describe('App.ManageAlertGroupsController', function () {

  beforeEach(function () {
    manageAlertGroupsController = App.ManageAlertGroupsController.create({});
  });

  describe('#addAlertGroup', function () {

    beforeEach(function () {
      manageAlertGroupsController.addAlertGroup();
    });

    describe("#validate", function () {
      it("should display no warning if user inputs valid characters into group name", function () {

        manageAlertGroupsController.addGroupPopup.set('alertGroupName', 'test');

        expect(manageAlertGroupsController.addGroupPopup.warningMessage).to.be.empty;
      });

      it("should display warning if user inputs invalid characters into group name", function () {
        manageAlertGroupsController.addGroupPopup.set('alertGroupName', '/{"!@#$%');

        expect(manageAlertGroupsController.addGroupPopup.warningMessage).to.equal('Invalid Alert Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
      });

    });
  });

  describe('#duplicateAlertGroup', function () {
    beforeEach(function () {
      var group = Ember.Object.create({
        name: 'test'
      });

      manageAlertGroupsController.set('selectedAlertGroup', group);
      manageAlertGroupsController.duplicateAlertGroup();
    });

    describe("#validate", function () {
      it("should display no warning if user duplicate an existed group", function () {
        manageAlertGroupsController.addGroupPopup.set('alertGroupName', 'test Copy');

        expect(manageAlertGroupsController.addGroupPopup.warningMessage).to.be.empty;
      });
    });
  });

  describe('#renameAlertGroup', function () {
    beforeEach(function () {
      var group = Ember.Object.create({
        name: 'test'
      });

      manageAlertGroupsController.set('selectedAlertGroup', group);
      manageAlertGroupsController.renameAlertGroup();
    });

    describe("#validate", function () {
      it("should display no warning if user inputs valid characters into group name", function () {
        manageAlertGroupsController.renameGroupPopup.set('alertGroupName', 'hello');

        expect(manageAlertGroupsController.renameGroupPopup.warningMessage).to.be.empty;
      });

      it("should display warning if user inputs invalid characters into group name", function () {
        manageAlertGroupsController.renameGroupPopup.set('alertGroupName', '/{"!@#$%');

        expect(manageAlertGroupsController.renameGroupPopup.warningMessage).to.equal('Invalid Alert Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
      });
    });
  });

  describe('#deleteDefinitions', function () {
    var definitions = [
      Em.Object.create({
        name: 'def1',
        serviceName: 'HDFS',
        label: "Alert Definition 1",
        id: 1
      }),
      Em.Object.create({
        name: 'def2',
        serviceName: 'HDFS',
        label: "Alert Definition 2",
        id: 2
      }),
      Em.Object.create({
        name: 'def3',
        serviceName: 'HDFS',
        label: "Alert Definition 3",
        id: 3
      })
    ];

    beforeEach(function () {
      manageAlertGroupsController = App.ManageAlertGroupsController.create({});
    });

    var createAlertGroupMock = function (groupDefs) {
      return Em.Object.create({
        definitions: groupDefs,
        name: 'group'
      });
    };

    var tests = [
      {
        selectedDefinitions: definitions.slice(0, 1),
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: definitions.slice(1)
      },
      {
        selectedDefinitions: definitions.slice(0, 2),
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: definitions.slice(2)
      },
      {
        selectedDefinitions: definitions,
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: []
      }
    ];

    tests.forEach(function (test) {
      it('delete definitions length {0} definitions'.format(test.selectedDefinitions.slice(0).length), function () {
        manageAlertGroupsController.reopen({
          selectedDefinitions: test.selectedDefinitions,
          selectedAlertGroup: test.selectedAlertGroup
        });
        manageAlertGroupsController.deleteDefinitions();
        expect(manageAlertGroupsController.get('selectedAlertGroup.definitions').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#addDefinitionsCallback', function () {

    var definitions = [
      Em.Object.create({
        name: 'def1',
        serviceName: 'HDFS',
        label: "Alert Definition 1",
        id: 1
      }),
      Em.Object.create({
        name: 'def2',
        serviceName: 'HDFS',
        label: "Alert Definition 2",
        id: 2
      }),
      Em.Object.create({
        name: 'def3',
        serviceName: 'HDFS',
        label: "Alert Definition 3",
        id: 3
      })
    ];

    var definitionsToAdd = [
      Em.Object.create({
        name: 'def4',
        serviceName: 'HDFS',
        label: "Alert Definition 4",
        id: 4
      }),
      Em.Object.create({
        name: 'def5',
        serviceName: 'HDFS',
        label: "Alert Definition 5",
        id: 5
      }),
      Em.Object.create({
        name: 'def6',
        serviceName: 'HDFS',
        label: "Alert Definition 6",
        id: 6
      })
    ];

    beforeEach(function () {
      manageAlertGroupsController = App.ManageAlertGroupsController.create({});
    });

    var createAlertGroupMock = function (groupDefs) {
      return Em.Object.create({
        definitions: groupDefs,
        name: 'group'
      });
    };

    var result = function (originalDefs, addedDefs) {
      return originalDefs.concat(addedDefs);
    };

    var tests = [
      {
        selectedDefinitions: definitionsToAdd.slice(0, 1),
        selectedAlertGroup: createAlertGroupMock(definitions.slice(0, 1)),
        e: result(definitions.slice(0, 1), definitionsToAdd.slice(0, 1))
      },
      {
        selectedDefinitions: definitionsToAdd.slice(0, 2),
        selectedAlertGroup: createAlertGroupMock(definitions.slice(0, 2)),
        e: result(definitions.slice(0, 2), definitionsToAdd.slice(0, 2))
      },
      {
        selectedDefinitions: definitionsToAdd,
        selectedAlertGroup: createAlertGroupMock(definitions),
        e: result(definitions, definitionsToAdd)
      }
    ];

    tests.forEach(function (test) {
      it('add Definitions length {0} definitions'.format(test.selectedDefinitions.slice(0).length), function () {
        manageAlertGroupsController.set('selectedAlertGroup', test.selectedAlertGroup);
        manageAlertGroupsController.addDefinitionsCallback(test.selectedDefinitions);
        expect(manageAlertGroupsController.get('selectedAlertGroup.definitions').toArray()).to.eql(test.e);
      });
    });

  });

});

