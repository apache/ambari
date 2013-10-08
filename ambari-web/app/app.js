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

// Application bootstrapper

var stringUtils = require('utils/string_utils');

module.exports = Em.Application.create({
  name: 'Ambari Web',
  rootElement: '#wrapper',

  store: DS.Store.create({
    revision: 4,
    adapter: DS.FixtureAdapter.create({
      simulateRemoteResponse: false
    })
  }),
  isAdmin: false,
  /**
   * return url prefix with number value of version of HDP stack
   */
  stackVersionURL:function(){
    var stackVersion = this.get('currentStackVersion') || this.get('defaultStackVersion');
    if(stackVersion.indexOf('HDPLocal') !== -1){
      return '/stacks/HDPLocal/version/' + stackVersion.replace(/HDPLocal-/g, '');
    }
    return '/stacks/HDP/version/' + stackVersion.replace(/HDP-/g, '');
  }.property('currentStackVersion'),
  
  /**
   * return url prefix with number value of version of HDP stack
   */
  stack2VersionURL:function(){
    var stackVersion = this.get('currentStackVersion') || this.get('defaultStackVersion');
    if(stackVersion.indexOf('HDPLocal') !== -1){
      return '/stacks2/HDPLocal/versions/' + stackVersion.replace(/HDPLocal-/g, '');
    }
    return '/stacks2/HDP/versions/' + stackVersion.replace(/HDP-/g, '');
  }.property('currentStackVersion'),
  clusterName: null,
  currentStackVersion: '',
  currentStackVersionNumber: function(){
    return this.get('currentStackVersion').replace(/HDP(Local)?-/, '');
  }.property('currentStackVersion'),
  isHadoop2Stack: function(){
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.0") === 1 ||
      stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.0") === 0)
  }.property('currentStackVersionNumber')
});

/**
 * overwritten set method of Ember.View to avoid uncaught errors
 * when trying to set property of destroyed view
 */
Em.View.reopen({
  set: function(attr, value){
    if(!this.get('isDestroyed') && !this.get('isDestroying')){
      this._super(attr, value);
    } else {
      console.debug('Calling set on destroyed view');
    }
  }
});

/**
 * Ambari overrides the default date transformer.
 * This is done because of the non-standard data
 * sent. For example Nagios sends date as "12345678".
 * The problem is that it is a String and is represented
 * only in seconds whereas Javascript's Date needs
 * milliseconds representation.
 */
DS.attr.transforms.date = {
  from: function (serialized) {
    var type = typeof serialized;
    if (type === "string") {
      serialized = parseInt(serialized);
      type = typeof serialized;
    }
    if (type === "number") {
      // The number could be seconds or milliseconds.
      // If seconds, then multiplying with 1000 should still
      // keep it below the current time.
      if (serialized * 1000 < new Date().getTime()) {
        serialized = serialized * 1000;
      }
      return new Date(serialized);
    } else if (serialized === null || serialized === undefined) {
      // if the value is not present in the data,
      // return undefined, not null.
      return serialized;
    } else {
      return null;
    }
  },
  to: function (deserialized) {
    if (deserialized instanceof Date) {
      return deserialized.getTime();
    } else if (deserialized === undefined) {
      return undefined;
    } else {
      return null;
    }
  }
}

DS.attr.transforms.object = {
  from: function(serialized) {
    return Ember.none(serialized) ? null : Object(serialized);
  },

  to: function(deserialized) {
    return Ember.none(deserialized) ? null : Object(deserialized);
  }
};


