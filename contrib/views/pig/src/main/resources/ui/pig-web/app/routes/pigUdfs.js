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

var App = require('app');

App.PigUdfsRoute = Em.Route.extend({
  actions:{
    createUdfModal:function () {
      this.controllerFor('pigModal').set('content', this.store.createRecord('udf'));
      return this.send('openModal','createUdf');
    },
    createUdf:function (udf) {
      var router = this;
      var onSuccess = function(model){
          router.send('showAlert', {'message': Em.I18n.t('udfs.alert.udf_created',{name : model.get('name')}), status:'success'});
        };
      var onFail = function(error){
          var trace = null;
          if (error && error.responseJSON.trace)
            trace = error.responseJSON.trace;
          router.send('showAlert', {'message':Em.I18n.t('udfs.alert.create_failed'),status:'error',trace:trace});
        };
      return udf.save().then(onSuccess,onFail);
    },
    deleteUdf:function(udf){
      var router = this;
      var onSuccess = function(model){
            router.send('showAlert', {'message': Em.I18n.t('udfs.alert.udf_deleted',{name : model.get('name')}),status:'success'});
          };
      var onFail = function(error){
            var trace = null;
            if (error && error.responseJSON.trace)
              trace = error.responseJSON.trace;
            router.send('showAlert', {'message': Em.I18n.t('udfs.alert.delete_failed'),status:'error',trace:trace});
          };
      udf.deleteRecord();
      return udf.save().then(onSuccess,onFail);
    }
  },
  enter: function() {
    this.controllerFor('pig').set('category',"udfs");
  },
  model: function() {
    return this.store.find('udf');
  }
});
