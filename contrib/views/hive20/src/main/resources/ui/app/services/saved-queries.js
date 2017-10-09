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

  store: Ember.inject.service(),

  getAllQueries(){
    let url = this.get('store').adapterFor('saved-query').buildURL();
    return Ember.$.ajax(url, 'GET');
  },

  saveQuery(payload){
    return Ember.$.ajax({
      type: "POST",
      url: this.get('store').adapterFor('saved-query').buildURL() + '/savedQueries/',
      data: JSON.stringify({savedQuery: payload}) ,
      contentType:"application/json; charset=utf-8",
      dataType:"json",
      headers: {'X-Requested-By': 'ambari'}
    });
  },

  deleteSaveQuery(id){
    let deletURL = this.get('store').adapterFor('saved-query').buildURL()+ '/savedQueries/' + id;

    return Ember.$.ajax({
      type: "DELETE",
      url: deletURL,
      contentType:"application/json; charset=utf-8",
      dataType:"json",
      headers: {'X-Requested-By': 'ambari'}
    });
  },

  updateSavedQuery(savedQueryId, shortQuery, selectedDb, owner){
    return new Ember.RSVP.Promise((resolve, reject) => {

      this.get('store').findRecord('saved-query', savedQueryId, {async: true} ).then(savedQuery => {
          savedQuery.set('shortQuery', shortQuery);
          savedQuery.set('dataBase', selectedDb );
          savedQuery.set('owner', owner );
          savedQuery.save().then(() => {
            return resolve("");
          })




       }).catch(function (response) {
          console.log('error', response);
          return reject(response);
       });
    });
  },

  fetchSavedQuery(path) {
    let url = this.get('store').adapterFor('application').buildURL()+ '/files/' + encodeURIComponent(path);

    return Ember.$.ajax({
      type: "GET",
      url: url,
      headers: {'X-Requested-By': 'ambari'}
    });
  }

});
