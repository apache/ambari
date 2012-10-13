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
var fixtureAdapter = DS.FixtureAdapter.create();

var adapter = DS.RESTAdapter.extend({
  find: function(store, type, id) {
    /*
     var url = type.url;
     url = url.fmt(id);

     if (type.toString() === 'App.Cluster') {
     store.load(type, { id: id, hosts: [ { hostname: 'host1' }, { hostname: 'host2' } ], services: []});
     }

     jQuery.getJSON(url, function(data) {
     // data is a Hash of key/value pairs. If your server returns a
     // root, simply do something like:
     // store.load(type, id, data.person)
     store.load(type, id, data);
     });
     */
    // return this._super(store, type, id);
    return fixtureAdapter.find(store, type, id);
  },
  findMany: function(store, type, id) {
    // return this._super(store, type, id);
    return fixtureAdapter.findMany(store, type, id);
  },
  findAll: function(store, type, id) {
    // return this._super(store, type, id);
    return fixtureAdapter.findAll(store, type, id);
  },
  findQuery: function(store, type, id) {
    // return this._super(store, type, id);
    return fixtureAdapter.findQuery(store, type, id);
  },
  createRecord: function(store, type, record) {
    //return this._super(store, type, id);
    return fixtureAdapter.createRecord(store, type, record);
  },
  updateRecord: function(store, type, record) {
    //return this._super(store, type, id);
    return fixtureAdapter.updateRecord(store, type, record);
  },
  deleteRecord: function(store, type, record) {
    //return this._super(store, type, id);
    return fixtureAdapter.deleteRecord(store, type, record);
  }

});

module.exports = adapter.create();