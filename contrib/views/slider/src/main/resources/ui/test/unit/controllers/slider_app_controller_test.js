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

moduleFor('controller:sliderApp', 'App.SliderAppController');

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
    model: Em.Object.create({
      id: 'someId',
      name: 'SomeName',
      status: 'ACCEPTED'
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
