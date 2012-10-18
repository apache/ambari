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

/**
 * The data adapter that performs data exchange with Ambari Server's REST interface and other backend servers.
 *
 * To allow for mixture of mock data via Fixture and actual data exchange with backend servers while we iteratively
 * integrate, we are composing an instance of the fixture adapter.
 * The idea is to conditionally use the fixture adapter for model types that have not been integrated yet,
 * and to use a custom, modified implementation of DS.RESTAdapter for model types that have been integrated.
 */

module.exports = DS.Adapter.create({
  fixtureAdapter: DS.FixtureAdapter.create(),
  restAdapter: DS.RESTAdapter.extend({
    buildURL: function (record, suffix) {
      if (/((ftp|http|https):\/\/)?(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/i.test(record)) {
        return record;
      }
      return this._super(record, suffix);
    },
    ajax: function (url, type, hash) {
      hash.url = url;
      hash.type = type;
      hash.dataType = 'jsonp';
      hash.contentType = 'application/javascript; charset=utf-8';
      hash.context = this;
      hash.jsonp = 'jsonp';
      if (hash.data && type !== 'GET') {
        hash.data = JSON.stringify(hash.data);
      }
      jQuery.ajax(hash);
    }
  }).create(),
  isRestType: function (type) {
    return type.url != null;
  },
  find: function (store, type, id) {
    if (this.isRestType(type)) {
      return this.restAdapter.find(store, type, id);
    } else {
      return this.fixtureAdapter.find(store, type, id);
    }
  },
  findMany: function (store, type, ids) {
    if (this.isRestType(type)) {
      return this.restAdapter.findMany(store, type, ids);
    } else {
      return this.fixtureAdapter.findMany(store, type, ids);
    }
  },
  findAll: function (store, type) {
    if (this.isRestType(type)) {
      return this.restAdapter.findAll(store, type);
    } else {
      return this.fixtureAdapter.findAll(store, type);
    }
  },
  findQuery: function (store, type, query, array) {
    if (this.isRestType(type)) {
      return this.restAdapter.findQuery(store, type, query, array);
    } else {
      return this.fixtureAdapter.findQuery(store, type, query, array);
    }
  },
  createRecord: function (store, type, record) {
    if (this.isRestType(type)) {
      return this.restAdapter.createRecord(store, type, record);
    } else {
      return this.fixtureAdapter.createRecord(store, type, record);
    }
  }
});


