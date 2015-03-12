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
import constants from 'hive/utils/constants';

export default Ember.Route.extend({
  setupController: function () {
    var self = this;

    this.controllerFor(constants.namingConventions.databases).set('model', this.store.find(constants.namingConventions.database));

    this.store.find(constants.namingConventions.udf).then(function (udfs) {
      self.controllerFor(constants.namingConventions.udfs).set('udfs', udfs);
    });
  },

  actions: {
    openModal: function (modalTemplate, options) {
      this.controllerFor(modalTemplate).setProperties({
        heading: options.heading,
        text: options.text,
        defer: options.defer
      });

      return this.render(modalTemplate, {
        into: 'application',
        outlet: 'modal'
      });
    },

    closeModal: function () {
      return this.disconnectOutlet({
        outlet: 'modal',
        parentView: 'application'
      });
    },

    addAlert: function (type, message, title) {
      this.controllerFor(constants.namingConventions.alerts).pushObject({
        type: type,
        title: title,
        content: message
      });
    }
  }
});
