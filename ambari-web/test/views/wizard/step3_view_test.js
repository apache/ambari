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
require('views/wizard/step3_view');

describe('App.WizardStep3View', function () {
  Em.run.next = function(callback){
    callback()
  };
  var view = App.WizardStep3View.create({
    monitorStatuses: function () {
    },
    content: [
      Em.Object.create({
        name: 'host1',
        bootStatus: 'PENDING',
        isChecked: false
      }),
      Em.Object.create({
        name: 'host2',
        bootStatus: 'PENDING',
        isChecked: true
      }),
      Em.Object.create({
        name: 'host3',
        bootStatus: 'PENDING',
        isChecked: true
      })
    ],
    pageContent: function () {
      return this.get('content');
    }.property('content')
  });

  describe('watchSelection', function () {
    it('2 of 3 hosts selected', function () {
      view.watchSelection();
      expect(view.get('noHostsSelected')).to.equal(false);
      expect(view.get('selectedHostsCount')).to.equal(2);
    });
    it('all hosts selected', function () {
      view.selectAll();
      view.watchSelection();
      expect(view.get('noHostsSelected')).to.equal(false);
      expect(view.get('selectedHostsCount')).to.equal(3);
    });
    it('none hosts selected', function () {
      view.unSelectAll();
      view.watchSelection();
      expect(view.get('noHostsSelected')).to.equal(true);
      expect(view.get('selectedHostsCount')).to.equal(0);
    });
  });


  describe('selectAll', function () {
    it('select all hosts', function () {
      view.selectAll();
      expect(view.get('content').everyProperty('isChecked', true)).to.equal(true);
    });
  });

  describe('unSelectAll', function () {
    it('unselect all hosts', function () {
      view.unSelectAll();
      expect(view.get('content').everyProperty('isChecked', false)).to.equal(true);
    });
  });

  var testCases = [
    {
      title: 'none hosts',
      content: [],
      result: {
        "ALL": 0,
        "RUNNING": 0,
        "REGISTERING": 0,
        "REGISTERED": 0,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts RUNNING',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'RUNNING'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'RUNNING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'RUNNING'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 3,
        "REGISTERING": 0,
        "REGISTERED": 0,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts REGISTERING',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERING'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 3,
        "REGISTERED": 0,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts REGISTERED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'REGISTERED'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'REGISTERED'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 0,
        "REGISTERED": 3,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts FAILED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'FAILED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 0,
        "REGISTERED": 0,
        "FAILED": 3
      }
    },
    {
      title: 'first host is FAILED, second is RUNNING, third is REGISTERED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'RUNNING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 1,
        "REGISTERING": 0,
        "REGISTERED": 1,
        "FAILED": 1
      }
    },
    {
      title: 'two hosts is REGISTERING, one is REGISTERED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 2,
        "REGISTERED": 1,
        "FAILED": 0
      }
    }
  ];

  describe('countCategoryHosts', function () {
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('content', test.content);
        view.countCategoryHosts();
        view.get('categories').forEach(function (category) {
          expect(category.get('hostsCount')).to.equal(test.result[category.get('hostsBootStatus')])
        })
      });
    }, this);
  });

  describe('filter', function () {
    testCases.forEach(function (test) {
      describe(test.title, function () {
        view.get('categories').forEach(function (category) {
          it('. Selected category - ' + category.get('hostsBootStatus'), function () {
            view.set('content', test.content);
            view.selectCategory({context: category});
            view.filter();
            expect(view.get('filteredContent').length).to.equal(test.result[category.get('hostsBootStatus')])
          });
        })
      });
    }, this);
  });

});
