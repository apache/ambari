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

module.exports = Em.Application.create({
  name: 'Ambari Web',

  rootElement: '#wrapper',

  store: DS.Store.create({
    revision: 4,
    adapter: DS.FixtureAdapter.create()
    //adapter: DS.RESTAdapter.create({
    // bulkCommit: false
    // namespace: '/api/v1'
    // })
    /*
     adapter: DS.Adapter.create({
     find: function(store, type, id) {
     //var url = type.url;
     //url = url.fmt(id);

     if (type.toString() === 'App.Cluster') {
     store.load(type, { id: id, hosts: [ { hostname: 'host1' }, { hostname: 'host2' } ], services: []});
     }
     debugger;

     //jQuery.getJSON(url, function(data) {
     // data is a Hash of key/value pairs. If your server returns a
     // root, simply do something like:
     // store.load(type, id, data.person)
     //    store.load(type, id, data);
     //});
     },
     findAll: function(store, type, id) {
     debugger;
     },
     findMany: function(store, type, id) {
     debugger;
     }
     })
     */
  })

});
