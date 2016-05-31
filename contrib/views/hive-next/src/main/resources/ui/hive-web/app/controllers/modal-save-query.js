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
import ModalSave from '../controllers/modal-save';
import constants from '../utils/constants';

export default ModalSave.extend({
  showMessage: function () {
    var content = this.get('content');

    return !content.get('isNew') &&
            content.get('title') === this.get('text') &&
            content.get('constructor.typeKey') !== constants.namingConventions.job;
  }.property('content.isNew', 'text'),

  actions: {
    save: function () {
      this.send('closeModal');

      this.defer.resolve(Ember.Object.create({
        text: this.get('text'),
        overwrite: this.get('showMessage')
      }));
    }
  }
});
