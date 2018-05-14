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

export default Ember.Service.extend({
  tezViewURL: null,
  // replace used to avoid slash duplication by proxy
  tezApiURL: '/api/v1/views/TEZ'.replace(/^\/\//, '/'),
  tezURLPrefix: '/views/TEZ',
  tezDagPath: '?viewPath=/#/dag/',
  getTezViewInfo: function () {
    this.set('error', null);
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
  },

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
      this.set('errorMsg', 'Tez view not deployed');
    } else if (data.instances && !data.instances.length) { // no instance created
      this.set('error', 'tez.errors.no.instance');
      this.set('errorMsg', 'Tez view instance not created');
    } else {
      this.set('error', 'error');
      this.set('errorMsg', 'Error occurred while dispaying TEZ UI');
    }
  },
  getTezViewData(){
    let tezData = {};
    if(this.get('error')){
      tezData.error = this.get('error');
      tezData.errorMsg = this.get('errorMsg');
    } else {
      tezData.tezUrl = this.get('tezViewURL') + this.get("tezDagPath");
    }
    return tezData;
  }
});
