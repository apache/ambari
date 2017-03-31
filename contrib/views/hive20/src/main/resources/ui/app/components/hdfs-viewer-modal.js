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
import HdfsPickerConfig from '../utils/hdfs-picker-config';

export default Ember.Component.extend({
  store: Ember.inject.service(),
  config: null,
  showSelectedPath: true,

  hdfsLocation: null,

  init() {
    this._super(...arguments);
    this.set('config', HdfsPickerConfig.create({store: this.get('store')}));
  },

  actions: {
    closeDirectoryViewer() {
      this.sendAction('close');
    },

    pathSelected() {
      this.sendAction('selected', this.get('hdfsLocation'));
    },

    viewerSelectedPath(data) {
      this.set('hdfsLocation', data.path);
    },

    viewerError(err) {
      console.log("Error", err);
    }
  }
});
