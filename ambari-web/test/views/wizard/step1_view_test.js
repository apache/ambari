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
require('views/wizard/step1_view');

var view;

var controller = Em.Object.create({
  content: {
    stacks: []
  }
});

describe('App.WizardStep1View', function () {

  beforeEach(function() {
    view = App.WizardStep1View.create({'controller': controller});
  });

  describe('#osTypeToGroup', function() {

    var tests = Em.A([
      {os:'redhat5', e: 0},
      {os: 'centos5', e: 0},
      {os: 'oraclelinux5', e: 0},
      {os: 'redhat6', e: 1},
      {os: 'centos6', e: 1},
      {os: 'oraclelinux6', e: 1},
      {os: 'sles11', e: 2},
      {os: 'suse11', e: 2},
      {os: 'ubuntu12', e: 3},
      {os: 'bulgen', e: -1}
    ]);

    tests.forEach(function(test) {
      it(test.os, function() {
        expect(view.osTypeToGroup(test.os)).to.equal(test.e);
      });
    });

  });

  describe('#groupToOsType', function () {

    var tests = Em.A([
      {type: 0, e: ['redhat5', 'centos5', 'oraclelinux5']},
      {type: 1, e: ['redhat6', 'centos6', 'oraclelinux6']},
      {type: 2, e: ['sles11', 'suse11']},
      {type: 3, e: ['ubuntu12']},
      {type: -1, e: []}
    ]);

    tests.forEach(function(test) {
      it(test.type, function() {
        expect(view.groupToOsType(test.type)).to.eql(test.e);
      });
    });

  });

  describe('#emptyRepoExist', function() {

    var tests = Em.A([
      {
        allRepositoriesGroup: [{'empty-error': false},{'empty-error': false},{'empty-error': false}],
        e: false
      },
      {
        allRepositoriesGroup: [{'empty-error': true},{'empty-error': false},{'empty-error': false}],
        e: true
      },
      {
        allRepositoriesGroup: [{'empty-error': true},{'empty-error': true},{'empty-error': true}],
        e: true
      }
    ]);

    tests.forEach(function(test) {
      it(test.allRepositoriesGroup.mapProperty('empty-error'), function() {
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('emptyRepoExist')).to.equal(test.e);
      });
    });

  });

  describe('#allRepoUnchecked', function() {

    var tests = Em.A([
      {
        allRepositoriesGroup: [{'checked': false},{'checked': false},{'checked': false}],
        e: true
      },
      {
        allRepositoriesGroup: [{'checked': true},{'checked': false},{'checked': false}],
        e: false
      },
      {
        allRepositoriesGroup: [{'checked': true},{'checked': true},{'checked': true}],
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.allRepositoriesGroup.mapProperty('empty-error'), function() {
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('allRepoUnchecked')).to.equal(test.e);
      });
    });

  });

  describe('#stacks', function() {

    var tests = Em.A([
      {
        m: 'Stack with 2 HDP',
        stacks: [
          Em.Object.create({isSelected: true, name: 'HDP-2.0.1'}),
          Em.Object.create({isSelected: false, name: 'HDP-1.3.3'})
        ],
        e: {
          names: ['HDP 2.0.1', 'HDP 1.3.3'],
          selected: [true, false]
        }
      },
      {
        m: 'No HDP',
        stacks: [],
        e: {
          names: [],
          selected: []
        }
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller.content.stacks', test.stacks);
        var stacks = view.get('stacks');
        expect(stacks.mapProperty('name')).to.eql(test.e.names);
        expect(stacks.mapProperty('isSelected')).to.eql(test.e.selected);
      });
    });

  });

  describe('#isSubmitDisabled', function() {

    var tests = Em.A([
      {
        emptyRepoExist: false,
        allRepoUnchecked: false,
        invalidUrlExist: false,
        e: false
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: false,
        invalidUrlExist: false,
        e: true
      },
      {
        emptyRepoExist: false,
        allRepoUnchecked: true,
        invalidUrlExist: false,
        e: true
      },
      {
        emptyRepoExist: false,
        allRepoUnchecked: false,
        invalidUrlExist: true,
        e: true
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: false,
        invalidUrlExist: true,
        e: true
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: true,
        invalidUrlExist: false,
        e: true
      },
      {
        emptyRepoExist: false,
        allRepoUnchecked: true,
        invalidUrlExist: true,
        e: true
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: true,
        invalidUrlExist: true,
        e: true
      }
    ]);

    tests.forEach(function(test) {
      it(test.emptyRepoExist.toString() + ' ' + test.allRepoUnchecked.toString() + ' ' + test.invalidUrlExist.toString(), function() {
        view = App.WizardStep1View.create();
        view.reopen({
          emptyRepoExist: test.emptyRepoExist,
          allRepoUnchecked: test.allRepoUnchecked,
          invalidUrlExist: test.invalidUrlExist
        });
        expect(view.get('isSubmitDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#invalidUrlExist', function() {
    var tests = Em.A([
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 1})],
        allRepositoriesGroup: [Em.Object.create({validation: 'icon-exclamation-sign'})],
        m: 'invalidCnt: 1, validation: icon-exclamation-sign',
        e: true
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 1})],
        allRepositoriesGroup: [Em.Object.create({validation: ''})],
        m: 'invalidCnt: 1, validation: ""',
        e: false
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 0})],
        allRepositoriesGroup: [Em.Object.create({validation: ''})],
        m: 'invalidCnt: 0, validation: ""',
        e: false
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 0})],
        allRepositoriesGroup: [Em.Object.create({validation: 'icon-exclamation-sign'})],
        m: 'invalidCnt: 0, validation: icon-exclamation-sign',
        e: false
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller.content.stacks', test.stacks);
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('invalidUrlExist')).to.equal(test.e);
      });
    });
  });

  describe('#totalErrorCnt', function() {
    var tests = Em.A([
      {
        allRepositoriesGroup: [{checked:false}],
        m: 'allRepoUnchecked',
        e: 1
      },
      {
        allRepositoriesGroup: [{checked:true, 'empty-error': true}, {checked:false, 'empty-error': true}],
        m: 'two with empty-error',
        e: 2
      },
      {
        allRepositoriesGroup: [{checked:true, 'validation': 'icon-exclamation-sign'}, {checked:false, 'validation': 'icon-exclamation-sign'}],
        m: 'two with validation="icon-exclamation-sign"',
        e: 2
      },
      {
        allRepositoriesGroup: [{checked:true, 'empty-error': true, 'validation': 'icon-exclamation-sign'}, {checked:false, 'empty-error': true, 'validation': 'icon-exclamation-sign'}],
        m: 'two with empty-error, two with validation="icon-exclamation-sign"',
        e: 4
      },
      {
        allRepositoriesGroup: [{checked:true}],
        m: 'no errors/warnings etc',
        e: 0
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('totalErrorCnt')).to.equal(test.e);
      });
    });
  });

});
