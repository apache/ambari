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

QUnit.module('App.SliderAppsMapper', {

  setup: function () {
    App.SliderApp.store = App.__container__.lookup('store:main');
  },

  teardown: function () {
    App.reset();
  }

});

test('parseQuickLinks', function () {

  var mapper = App.SliderAppsMapper;

  Em.run(function () {
    App.SliderApp.store.all = function () {
      return [
        Em.Object.create({
          viewConfigName: 'yarn.rm.webapp.url',
          value: 'host'
        })
      ]
    };
    App.SliderApp.store.pushMany = function (model, record) {
      mapper.set('result', record);
    };
    mapper.parseQuickLinks({
      id: '1'
    })
  });

  equal(mapper.get('result')[0].get('url'), 'http://host/cluster/app/application_1', 'valid YARN application URL formed');
  equal(mapper.get('result')[0].get('id'), 'YARN application 1', 'model id set correctly');

});

test('parse | add/remove apps', function () {

  Em.run(function () {

    App.SliderAppsMapper.parse({
      apps: [
        {id: '1', type: 't1'},
        {id: '2', type: 't2'}
      ]
    });

  });

  deepEqual(App.SliderApp.store.all('sliderApp').mapBy('id'), ['1', '2'], 'Mapped all apps');

  Em.run(function () {

    App.SliderAppsMapper.parse({
      apps: [
        {id: '2', type: 't2'},
        {id: '3', type: 't3'}
      ]
    });

  });

  deepEqual(App.SliderApp.store.all('sliderApp').mapBy('id'), ['2', '3'], 'Delete not-existing app and add new');

});
