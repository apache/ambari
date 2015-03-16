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

moduleFor('controller:sliderApp', 'App.SliderAppController', {

  needs: [
    'component:bs-modal'
  ],

  setup: function () {
    sinon.stub(Bootstrap.ModalManager, 'register', Em.K);
    sinon.stub(Bootstrap.ModalManager, 'open', Em.K);
    sinon.stub(App.ajax, 'send', Em.K);
    sinon.stub(Bootstrap.ModalManager, 'close', Em.K);
  },

  teardown: function () {
    Bootstrap.ModalManager.register.restore();
    Bootstrap.ModalManager.open.restore();
    App.ajax.send.restore();
    Bootstrap.ModalManager.close.restore();
  }

});

test('availableActions', function () {

  var controller = this.subject({model: Em.Object.create({status: ''})});
  controller.set('model.status', App.SliderApp.Status.accepted);
  deepEqual(controller.get('availableActions').mapBy('action'), ['freeze'], 'actions for ACCEPTED');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.failed);
  });
  deepEqual(controller.get('availableActions').findBy('title', 'Advanced').submenu.mapBy('action'), ['destroy'], 'actions for FAILED');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.finished);
  });
  deepEqual(controller.get('availableActions').findBy('title', 'Advanced').submenu.mapBy('action'), ['destroy'], 'actions for FINISHED');
  ok(controller.get('availableActions').mapBy('action').contains('thaw'), 'actions for FINISHED (2)');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.killed);
  });
  deepEqual(controller.get('availableActions').findBy('title', 'Advanced').submenu.mapBy('action'), ['destroy'], 'actions for KILLED');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.new);
  });
  deepEqual(controller.get('availableActions').mapBy('action'), ['freeze'], 'actions for NEW');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.new_saving);
  });
  deepEqual(controller.get('availableActions').mapBy('action'), ['freeze'], 'actions for NEW_SAVING');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.running);
  });
  deepEqual(controller.get('availableActions').mapBy('action'), ['freeze', 'flex'], 'actions for RUNNING');

  Em.run(function () {
    controller.set('model.status', App.SliderApp.Status.frozen);
  });
  deepEqual(controller.get('availableActions').findBy('title', 'Advanced').submenu.mapBy('action'), ['destroy'], 'actions for FROZEN');
  ok(controller.get('availableActions').mapBy('action').contains('thaw'), 'actions for FROZEN (2)');

});

test('sliderAppTabs', function () {

  var cases = [
      {
        length: 1
      },
      {
        configs: {},
        length: 1
      },
      {
        configs: {
          n0: 'v0'
        },
        length: 2
      }
    ],
    title = 'number of tabs should be {0}',
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('model', {
        configs: item.configs
      })
    });

    equal(controller.get('sliderAppTabs.length'), item.length, title.format(item.length));

  });

});

test('weHaveQuicklinks', function () {

  var cases = [
      {
        content: [
          {
            id: '0'
          }
        ],
        value: true
      },
      {
        value: false
      }
    ],
    title = 'should be {0}',
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('model', {
        quickLinks: {
          content: {
            content: item.content
          }
        }
      });
    });

    equal(controller.get('weHaveQuicklinks'), item.value, title.format(item.value));

  });

});

test('destroyButtonEnabled', function () {

  var cases = [
      {
        confirmChecked: true,
        string: 'disabled'
      },
      {
        confirmChecked: false,
        string: 'enabled'
      }
    ],
    title = 'button is {0}',
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('confirmChecked', item.confirmChecked);
    });

    equal(controller.get('destroyButtonEnabled'), !item.confirmChecked, title.format(item.string));

  });

});

test('confirmDestroy', function () {

  var controller = this.subject(),
    assertionsEqual = [
      {
        propertyName: 'name',
        value: 'confirm-modal'
      },
      {
        propertyName: 'title',
        value: Ember.I18n.t('sliderApp.destroy.confirm.title')
      },
      {
        propertyName: 'manual',
        value: true
      }
    ],
    assertionsDeepEqual = [
      {
        propertyName: 'targetObject',
        value: controller,
        valueFormatted: 'App.SliderAppController'
      },
      {
        propertyName: 'controller',
        value: controller,
        valueFormatted: 'App.SliderAppController'
      },
      {
        propertyName: 'body',
        value: App.DestroyAppPopupView
      },
      {
        propertyName: 'footerViews',
        value: [App.DestroyAppPopupFooterView]
      }
    ],
    title = 'modalComponent.{0} should be {1}';

  Em.run(function () {
    controller.confirmDestroy();
  });

  ok(Bootstrap.ModalManager.register.calledOnce, 'Bootstrap.ModalManager.register should be executed');
  assertionsEqual.forEach(function (item) {
    equal(Bootstrap.ModalManager.register.firstCall.args[1][item.propertyName], item.value, title.format(item.propertyName, item.value));
  });
  assertionsDeepEqual.forEach(function (item) {
    deepEqual(Bootstrap.ModalManager.register.firstCall.args[1][item.propertyName], item.value, title.format(item.propertyName, item.valueFormatted || item.value));
  });

});

test('tryDoAction', function () {

  var controller = this.subject({
    currentAction: 'customMethod',
    customMethod: function () {
      this.set('groupedComponents', [{}]);
    }
  });

  Em.run(function () {
    controller.tryDoAction();
  });

  deepEqual(controller.get('groupedComponents'), [{}], 'currentAction should be executed');

});

test('groupComponents', function () {

  var controller = this.subject({
    appType: {
      components: [
        Em.Object.create({
          name: 'c0'
        }),
        Em.Object.create({
          name: 'c1'
        })
      ]
    },
    components: [
      Em.Object.create({
        componentName: 'c0'
      })
    ]
  });

  Em.run(function () {
    controller.groupComponents();
  });

  equal(controller.get('groupedComponents')[0].count, 1, 'count should be incremented');
  equal(controller.get('groupedComponents')[1].count, 0, 'count shouldn\'t be incremented');

});

test('validateGroupedComponents', function () {

  var cases = [
      {
        count: ' 1',
        hasErrors: true,
        title: 'validation failure'
      },
      {
        count: '-1',
        hasErrors: true,
        title: 'validation failure'
      },
      {
        count: 1,
        hasErrors: false,
        title: 'validation success'
      }
    ],
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('groupedComponents', [{
        count: item.count
      }]);
      controller.validateGroupedComponents();
    });

    equal(controller.get('groupedComponentsHaveErrors'), item.hasErrors, item.title);

  });

});

test('flex', function () {

  var controller = this.subject({
      appType: {
        components: [
          Em.Object.create({
            name: 'c0'
          }),
          Em.Object.create({
            name: 'c1'
          })
        ]
      },
      components: [
        Em.Object.create({
          componentName: 'c0'
        })
      ]
    });

  Em.run(function () {
    controller.flex();
  });

  equal(controller.get('groupedComponents')[0].count, 1, 'count should be incremented');
  equal(controller.get('groupedComponents')[1].count, 0, 'count shouldn\'t be incremented');

});

test('mapComponentsForFlexRequest', function () {

  var controller = this.subject({
    groupedComponents: [
      {
        name: 'c0',
        count: 1
      },
      {
        name: 'c1',
        count: 0
      }
    ]
  });

  deepEqual(controller.mapComponentsForFlexRequest(), {
    c0: {
      instanceCount: 1
    },
    c1: {
      instanceCount: 0
    }
  }, 'should map grouped components');

});

test('destroy', function () {

  var controller = this.subject({
    model: Em.Object.create({
      isActionPerformed: false
    })
  });

  Em.run(function () {
    controller.destroy();
  });

  ok(controller.get('model.isActionPerformed'), 'model.isActionPerformed should be true');

});

test('actionErrorCallback', function () {

  var controller = this.subject({
    model: Em.Object.create({
      isActionPerformed: true
    }),
    defaultErrorHandler: Em.K
  });

  Em.run(function () {
    controller.actionErrorCallback(null, null, null, {
      url: null,
      type: null
    });
  });

  ok(!controller.get('model.isActionPerformed'), 'model.isActionPerformed should be true');

});

test('actions.submitFlex', function () {

  var controller = this.subject({
      model: Em.Object.create({
        id: 0,
        name: 'n'
      }),
      validateGroupedComponents: function () {
        return false;
      },
      groupedComponentsHaveErrors: true
    });

  Em.run(function () {
    controller.send('submitFlex');
  });

  equal(controller.get('groupedComponents.length'), 0, 'should clear grouped components');
  ok(!controller.get('groupedComponentsHaveErrors'), 'should clear components errors');

});

test('actions.closeFlex', function () {

  var controller = this.subject({
    groupedComponents: [{}],
    groupedComponentsHaveErrors: true
  });

  Em.run(function () {
    controller.send('closeFlex');
  });

  equal(controller.get('groupedComponents.length'), 0, 'should clear grouped components');
  ok(!controller.get('groupedComponentsHaveErrors'), 'should clear components errors');

});

test('modalConfirmed', function () {

  var controller = this.subject({
      confirmChecked: true,
      currentAction: 'customMethod',
      customMethod: function () {
        this.set('groupedComponents', [{}]);
      }
    });

  Em.run(function () {
    controller.send('modalConfirmed');
  });

  deepEqual(controller.get('groupedComponents'), [{}], 'currentAction should be executed');
  ok(!controller.get('confirmChecked'), 'confirmChecked should be false');

});

test('modalCanceled', function () {

  var controller = this.subject({
      confirmChecked: true
    });

  Em.run(function () {
    controller.send('modalCanceled');
  });

  ok(!controller.get('confirmChecked'), 'confirmChecked should be false');

});

test('openModal', function () {

  var cases = [
      {
        options: {
          action: 'customMethod'
        },
        groupedComponents: [{}],
        title: 'should execute currentAction'
      },
      {
        options: {
          action: 'customMethod',
          customConfirm: 'customConfirmMethod'
        },
        groupedComponents: [{}, {}],
        title: 'should execute customConfirm'
      }
    ],
    controller = this.subject({
      customMethod: function () {
        this.set('groupedComponents', [{}]);
      },
      customConfirmMethod: function () {
        this.set('groupedComponents', [{}, {}]);
      }
    });

  Em.run(function () {
    controller.send('openModal', {
      action: 'customMethod'
    });
  });

  equal(controller.get('currentAction'), 'customMethod', 'should set currentAction');

  cases.forEach(function (item) {

    Em.run(function () {
      controller.send('openModal', item.options);
    });

    deepEqual(controller.get('groupedComponents'), item.groupedComponents, item.title);

  });

});

test('quickLinksOrdered', function() {
  expect(2);

  var controller = this.subject({
    model: Em.Object.create({
      'quickLinks': [
        Em.Object.create({ label: 'org.apache.slider.thrift'}),
        Em.Object.create({ label: 'Metrics API'}),
        Em.Object.create({ label: 'org.apache.slider.hbase'}),
        Em.Object.create({ label: 'Metrics UI'}),
        Em.Object.create({ label: 'UI'}),
        Em.Object.create({ label: 'Some Label'})
      ]
    }),
    weHaveQuicklinks: true
  });

  Em.run(function() {
    controller.get('quickLinksOrdered');
  });

  equal(controller.get('quickLinksOrdered').objectAt(4).get('label'), 'Metrics UI', 'Metrics UI link should be before Metrics API');
  equal(controller.get('quickLinksOrdered').objectAt(5).get('label'), 'Metrics API', 'Metrics API link should be last');
});


test('Disable Action Button', function() {
  expect(6);
  var controller = this.subject({
    model: Em.Object.extend({
      isActionFinished: function() {
        return this.get('status') != this.get('statusBeforeAction');
      }.property('statusBeforeAction', 'status')
    }).create({
      id: 'someId',
      name: 'SomeName',
      status: 'ACCEPTED',
      statusBeforeAction: ''
    }),
    defaultErrorHandler: function() { return true; }
  });

  Em.run(function() {
    controller.thaw();
  });

  equal(controller.get('model').get('isActionPerformed'), true, 'Perform start action');

  Em.run(function() {
    controller.set('model.status', 'RUNNING');
    controller.get('availableActions');
  });

  equal(controller.get('model').get('isActionPerformed'), false, 'Start is done.');

  Em.run(function() {
    controller.freeze();
  });

  equal(controller.get('model').get('isActionPerformed'), true, 'Perform freeze action')

  Em.run(function() {
    controller.set('model.status', 'FROZEN');
    controller.get('availableActions');
  });

  equal(controller.get('model').get('isActionPerformed'), false, 'Freeze is done.');

  Em.run(function() {
    controller.thaw();
  });

  equal(controller.get('model').get('isActionPerformed'), true, 'Start action performed expect for error.');

  Em.run(function() {
    controller.actionErrorCallback({requestText: 'some text'}, {url: '/some/url'}, {type: 'PUT'}, true);
  });

  equal(controller.get('model').get('isActionPerformed'), false, 'Error catched button should be enabled');
});
