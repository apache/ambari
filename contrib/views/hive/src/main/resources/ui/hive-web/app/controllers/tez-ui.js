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

export default Ember.Controller.extend({
  needs: [ constants.namingConventions.index ],

  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),

  tezViewURL: null,
  tezApiURL: '/api/v1/views/TEZ',
  tezURLPrefix: '/views/TEZ',
  tezDagPath: '?viewPath=/#/dag/',

  isTezViewAvailable: Ember.computed.bool('tezViewURL'),

  dagId: function () {
    if (this.get('isTezViewAvailable')) {
      return this.get('index.model.dagId');
    }

    return false;
  }.property('index.model.dagId', 'isTezViewAvailable'),

  dagURL: function () {
    if (this.get('dagId')) {
      return "%@%@%@".fmt(this.get('tezViewURL'), this.get('tezDagPath'), this.get('dagId'));
    }

    return false;
  }.property('dagId'),

  getTezView: function () {
    if (this.get('isTezViewAvailable')) {
      return;
    }

    var self = this;
    Ember.$.getJSON(this.get('tezApiURL'))
      .then(function (response) {
        self.getTezViewInstance(response);
      })
      .fail(function (response) {
        self.setTezViewError(response);
      });
  }.on('init'),

  getTezViewInstance: function (data) {
    var self = this;
    var url = this.get('tezApiURL') + '/versions/' + data.versions[0].ViewVersionInfo.version;

    Ember.$.getJSON(url)
      .then(function (response) {
        if (!response.instances.length) {
          self.setTezViewError(response);
          return;
        }

        self.set('isTezViewAvailable', true);

        var instance = response.instances[0].ViewInstanceInfo;
        self.setTezViewURL(instance);
      });
  },

  setTezViewURL: function (instance) {
    var url = "%@/%@/%@/".fmt(
      this.get('tezURLPrefix'),
      instance.version,
      instance.instance_name
    );

    this.set('tezViewURL', url);
  },

  setTezViewError: function (data) {
    // status: 404 => Tev View isn't deployed
    if (data.status && data.status === 404) {
      this.set('error', 'tez.errors.not.deployed');
      return;
    }

    // no instance created
    if (data.instances && !data.instances.length) {
      this.set('error', 'tez.errors.no.instance');
      return;
    }
  }
});
