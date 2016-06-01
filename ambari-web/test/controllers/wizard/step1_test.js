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

var wizardStep1Controller;

var stacks = [
  App.Stack.createRecord({
    "id": "HDP-2.4",
    "stackName": "HDP",
    "stackVersion": "2.4"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.5-2.5.0.0",
    "stackName": "HDP",
    "stackVersion": "2.5"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.5",
    "stackName": "HDP",
    "stackVersion": "2.5"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.3.ECS",
    "stackName": "HDP",
    "stackVersion": "2.3.ECS"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.3",
    "stackName": "HDP",
    "stackVersion": "2.3"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.2",
    "stackName": "HDP",
    "stackVersion": "2.2"
  }),
  App.Stack.createRecord({
    "id": "HDP-2.4-2.4.1.1-12345",
    "stackName": "HDP",
    "stackVersion": "2.4"
  })
];

function getController() {
  return App.WizardStep1Controller.create({content: Em.Object.create({stacks: stacks}), onNetworkIssuesExist: Em.K});
}

describe('App.WizardStep1Controller', function () {

  beforeEach(function() {
    wizardStep1Controller = getController();
  });

  App.TestAliases.testAsComputedFindBy(getController(), 'selectedStack', 'content.stacks', 'isSelected', true);

  App.TestAliases.testAsComputedFindBy(getController(), 'selectedStackType', 'availableStackTypes', 'isSelected', true);

  App.TestAliases.testAsComputedFilterBy(getController(), 'servicesForSelectedStack', 'selectedStack.stackServices', 'isHidden', false);

  App.TestAliases.testAsComputedEveryBy(getController(), 'networkIssuesExist', 'content.stacks', 'stackDefault', true);

  describe('#usePublicRepo', function () {

    beforeEach(function () {
      wizardStep1Controller.get('content.stacks').findProperty('id', 'HDP-2.5-2.5.0.0').setProperties({
        isSelected: true,
        useRedhatSatellite: true,
        usePublicRepo: false,
        useLocalRepo: true,
      });
      wizardStep1Controller.usePublicRepo();
    });

    it('correct stack is selected', function () {
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0');
    });

    it('`useRedhatSatellite` is set `false`', function () {
      expect(wizardStep1Controller.get('selectedStack.useRedhatSatellite')).to.be.false;
    });

    it('`usePublicRepo` is set `true`', function () {
      expect(wizardStep1Controller.get('selectedStack.usePublicRepo')).to.be.true;
    });

    it('`useLocalRepo` is set `false`', function () {
      expect(wizardStep1Controller.get('selectedStack.useLocalRepo')).to.be.false;
    });

  });

  describe('#useLocalRepo', function () {

    beforeEach(function () {
      wizardStep1Controller.get('content.stacks').findProperty('id', 'HDP-2.5-2.5.0.0').setProperties({
        isSelected: true,
        usePublicRepo: true,
        useLocalRepo: false,
      });
      wizardStep1Controller.useLocalRepo();
    });

    it('correct stack is selected', function () {
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0');
    });

    it('`usePublicRepo` is set `false`', function () {
      expect(wizardStep1Controller.get('selectedStack.usePublicRepo')).to.be.false;
    });

    it('`useLocalRepo` is set `true`', function () {
      expect(wizardStep1Controller.get('selectedStack.useLocalRepo')).to.be.true;
    });

  });

  describe('#selectStackBy', function () {

    it('select by `id`', function () {
      wizardStep1Controller.selectStackBy('id', 'HDP-2.5-2.5.0.0');
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0');
      expect(wizardStep1Controller.get('content.stacks').filterProperty('isSelected')).to.have.property('length').equal(1);
    });

    it('select by `stackNameVersion`', function () {
      wizardStep1Controller.selectStackBy('stackNameVersion', 'HDP-2.5');
      expect(wizardStep1Controller.get('selectedStack.id')).to.be.equal('HDP-2.5-2.5.0.0'); // `HDP-2.5-2.5.0.0`-id is before `HDP-2.5`-id
      expect(wizardStep1Controller.get('content.stacks').filterProperty('isSelected')).to.have.property('length').equal(1);
    });

  });

  describe('#availableStackTypes', function () {

    it('stack types are sorted desc', function () {
      expect(wizardStep1Controller.get('availableStackTypes').mapProperty('stackName')).to.be.eql(['HDP-2.5', 'HDP-2.4', 'HDP-2.3.ECS', 'HDP-2.3', 'HDP-2.2']);
    });

  });

  describe('#readInfoIsNotProvided', function () {

    Em.A([
      {
        options: {
          uploadFile: {isSelected: false},
          enterUrl: {isSelected: false}
        },
        m: 'url and file are not selected',
        e: false
      },
      {
        options: {
          uploadFile: {isSelected: false},
          enterUrl: {isSelected: true, url: ''}
        },
        m: 'url is selected but not provided',
        e: true
      },
      {
        options: {
          uploadFile: {isSelected: false},
          enterUrl: {isSelected: true, url: ' url'}
        },
        m: 'url is selected and provided',
        e: false
      },
      {
        options: {
          uploadFile: {isSelected: true, file: ''},
          enterUrl: {isSelected: false}
        },
        m: 'file is selected but not provided',
        e: true
      },
      {
        options: {
          uploadFile: {isSelected: true, file: 'path'},
          enterUrl: {isSelected: false}
        },
        m: 'file is selected and provided',
        e: false
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        wizardStep1Controller.set('optionsToSelect.useLocalRepo', test.options);
        expect(wizardStep1Controller.get('readInfoIsNotProvided')).to.be.equal(test.e);
      });

    });

  });

});
