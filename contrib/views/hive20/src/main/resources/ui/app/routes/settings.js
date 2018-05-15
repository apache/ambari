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

import Ember from 'ember';
import hiveParams from '../configs/hive-parameters';
import UILoggerMixin from '../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {
  model() {
    return this.store.findAll('setting').then(settings => settings.toArray());
  },
  setupController(controller, model) {
    this._super(...arguments);
    const appendedHiveParams = this.prepareExhaustiveParameters(hiveParams, model);
    controller.set('hiveParameters', appendedHiveParams);
  },

  prepareExhaustiveParameters(hiveParams, model) {
    let newHiveParams = [];
    newHiveParams.pushObjects(hiveParams);
    model.forEach(x => {
      let param = hiveParams.findBy('name', x.get('key'));

      if(Ember.isEmpty(param)) {
        newHiveParams.pushObject(
          Ember.Object.create({name: x.get('key'), disabled: true})
        );
      } else {
        param.set('disabled', true);
      }
    });
    return newHiveParams;
  },

  actions: {
    addNewSettings() {
      let model = this.get('controller.model');
      model.forEach(x => x.rollbackAttributes());
      let newItem = this.store.createRecord('setting', {editMode: true});
      model.pushObject(newItem);
    },

    editAction(setting) {
      setting.set('editMode', true);
    },

    deleteAction(setting) {
      return setting.destroyRecord().then(data => {
        let model = this.get('controller.model');
        model.removeObject(data);
        let hiveParameters = this.controller.get('hiveParameters');
        let existingHiveParams = hiveParameters.findBy('name', setting.get('key'));
        if(existingHiveParams) {
          existingHiveParams.set('disabled', false);
        }
      }, err => {
        this.get('logger').danger(`Failed to delete setting with key: '${setting.get('key')}`, this.extractError(err));
      });
    },

    updateAction(newSetting) {
      newSetting.save().then(data => {
        data.set('editMode', false);
      }, error => {
        this.get('logger').danger(`Failed to update setting with key: '${newSetting.get('key')}`, this.extractError(error));
      });
    },

    cancelAction(newSetting) {
      if (newSetting.get('isNew')) {
        let model = this.get('controller.model');
        model.removeObject(newSetting);
      } else {
        newSetting.set('editMode', false);
      }
    },

    willTransition(transition) {
      let unsavedModels = this.get('controller.model').filterBy('isNew', true);
      unsavedModels.forEach(x => this.store.unloadRecord(x));
    }
  }
});
