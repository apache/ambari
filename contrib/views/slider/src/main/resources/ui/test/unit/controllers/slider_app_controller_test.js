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
