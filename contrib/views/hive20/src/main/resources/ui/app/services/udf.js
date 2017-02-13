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

  getAllUdfs(){
    return $.ajax('/udfs', 'GET');
  },

  getFileResource(fileResourceId){



    return this.get('store').findRecord('file-resource',fileResourceId);
    //return this.get('store').queryRecord('file-resource', { 'fileResource': fileResourceId })

    //let url = '/fileResources/' + fileResourceId;
    //return $.ajax( url , 'GET');


  },

  savefileResource(payload){
    return $.ajax({
      type: "POST",
      //url: this.get('store').adapterFor('udf').fileResourceURL(),
      url: '/fileResources',
      data: JSON.stringify({fileResource: payload}) ,
      contentType:"application/json; charset=utf-8",
      headers: {'X-Requested-By': 'ambari'}
    })
  },

  saveUdf(payload){
    return $.ajax({
      type: "POST",
      //url: this.get('store').adapterFor('udf').udfURL(),
      url: '/udfs',
      data: JSON.stringify({udf: payload}) ,
      contentType:"application/json; charset=utf-8",
      headers: {'X-Requested-By': 'ambari'}
    })
  },

  deleteUdf(udfId){
    let deletURL = '/udfs/' + udfId;

    return $.ajax({
      type: "DELETE",
      url: deletURL,
      contentType:"application/json; charset=utf-8",
      dataType:"json",
      headers: {'X-Requested-By': 'ambari'}
    })
  }

});
