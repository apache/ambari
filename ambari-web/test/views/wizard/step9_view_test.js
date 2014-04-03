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
require('views/wizard/step9_view');


describe('App.WizardStep9View', function () {
  var view = App.WizardStep9View.create({
    onStatus: function () {},
    content: [],
    pageContent: function () {
      return this.get('content');
    }.property('content')
  });
  var testCases = [
    {
      title: 'none hosts',
      content: [],
      result: {
        "all": 0,
        "inProgress": 0,
        "warning": 0,
        "success": 0,
        "failed": 0
      }
    },
    {
      title: 'all hosts inProgress',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'in_progress'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'info'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'pending'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 3,
        "warning": 0,
        "success": 0,
        "failed": 0
      }
    },
    {
      title: 'all hosts warning',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'warning'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'warning'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'warning'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 3,
        "success": 0,
        "failed": 0
      }
    },
    {
      title: 'all hosts success',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'success'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'success'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'success'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 0,
        "success": 3,
        "failed": 0
      }
    },
    {
      title: 'all hosts failed',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'failed'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'failed'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'heartbeat_lost'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 0,
        "success": 0,
        "failed": 3
      }
    },
    {
      title: 'first host is failed, second is warning, third is success',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'failed'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'success'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'warning'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 1,
        "success": 1,
        "failed": 1
      }
    },
    {
      title: 'two hosts is inProgress, one is success',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'pending'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'in_progress'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'success'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 2,
        "warning": 0,
        "success": 1,
        "failed": 0
      }
    }
  ];

  describe('countCategoryHosts', function () {
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('content', test.content);
        view.countCategoryHosts();
        view.get('categories').forEach(function (category) {
          expect(category.get('hostsCount')).to.equal(test.result[category.get('hostStatus')])
        })
      });
    }, this);
  });

  describe('filter', function () {
    testCases.forEach(function (test) {
      describe(test.title, function () {
        view.get('categories').forEach(function (category) {
          it('. Selected category - ' + category.get('hostStatus'), function () {
            view.set('content', test.content);
            view.selectCategory({context: category});
            view.filter();
            expect(view.get('filteredContent').length).to.equal(test.result[category.get('hostStatus')])
          });
        })
      });
    }, this);
  });
});

describe('App.HostStatusView', function () {
  var tests = [
    {
      p: 'isFailed',
      tests: [
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: 99
          },
          e: false
        }
      ]
    },
    {
      p: 'isSuccess',
      tests: [
        {
          obj: {
            status: 'success',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'success',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        }
      ]
    },
    {
      p: 'isWarning',
      tests: [
        {
          obj: {
            status: 'warning',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'warning',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        }
      ]
    }
  ];
  tests.forEach(function(test) {
    describe(test.p, function() {
      test.tests.forEach(function(t) {
        var hostStatusView = App.HostStatusView.create();
        it('obj.progress = ' + t.obj.progress + '; obj.status = ' + t.obj.status, function() {
          hostStatusView.set('obj', t.obj);
          expect(hostStatusView.get(test.p)).to.equal(t.e);
        });
      });
    });
  });
});
