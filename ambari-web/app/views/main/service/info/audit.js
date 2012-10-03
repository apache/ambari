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

require('views/common/grid');

App.MainServiceInfoAuditView = App.Grid.extend({
//  audits: function() {
//    return App.router.get('mainServiceInfoAuditController.content').get('serviceAudit');
//  }.property('App.router.mainServiceInfoAuditController.content'),

  prepareCollection:function () {
    var audits = App.router.get('mainServiceInfoAuditController.content').get('serviceAudit');
    console.warn(" AUDITS: ", audits);
    this.set('collection', audits);
  },

  addFilters: function(field, values){
    var filters = this.get('appliedFilters');
    filters[field] = values;
    var collection = App.router.get('mainServiceInfoAuditController.content').get('serviceAudit');
    arrayCollection = collection.filter(function(data) {
      var oneFilterFail = false;
      $.each(filters, function(fieldname, values){
        if(values.indexOf(data.get(fieldname)) == -1) {
          return oneFilterFail = true;
        }
      });
      return !oneFilterFail;
    });

    this.set('filteredArray', arrayCollection);
  },

  _collection: {className: App.ServiceAudit},
  prepareColumns:function () {
    this._super();

    this.addColumn({
      name:"date",
      label:Em.I18n.t("admin.audit.grid.date")
    });
    this.addColumn({
      name:"service.label",
      label:Em.I18n.t("admin.audit.grid.service")
    });
    this.addColumn({
      name:"operationName",
      label:Em.I18n.t("admin.audit.grid.operationName")
    });
    this.addColumn({
      name:"user.userName",
      label:Em.I18n.t("admin.audit.grid.performedBy")
    });
  }
});