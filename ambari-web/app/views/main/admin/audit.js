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
var validator = require('utils/validator');

App.GridHeader = Em.View.extend({
  templateName:require('templates/common/grid/header'),
  tagName:'th',
  filterable:true,
  getGrid:function () {
    return this.get('grid');
  },
  doFilter:function () {
    console.log(this.get('grid'));
  },
  click:function (event) {
    // will be called when when an instance's
    // rendered element is clicked
  }
});

App.GridRow = Em.View.extend({
  tagName:'tr',
  init:function (options) {
    console.warn("gridrow init");
    var object = this.get('object');
    var grid = this.get('grid');
    var fieldNames = grid.get('fieldNames');
    var template = '';
//    console.warn("FIELD NAMES:", +fieldNames);

    if (fieldNames) {
      $.each(grid.get('fieldNames'), function (i, field) {
        template += "<td>"+ object.get(field) +"</td>";
      });

      this.set('template', Em.Handlebars.compile(template));
    }
    return this._super();
  }
});

App.MainAdminAuditView = Em.View.extend({
  _columns:{},
  columns:[],
  rows:[],
  initComleted:false,
  collection:[],
  templateName:require('templates/main/admin/audit'),
  fieldNames:[],
  init:function () {
    this._super();
    if (!this.columns.length) { // init completed on this
      this.prepareColumns();
      this.prepareFilters();
      this.prepareRows();
      this.prepareCollection();
    }
  },
  prepareCollection:function () {
    this.set('collection', App.ServiceAudit.find());
  },

  addColumn:function (options) {
    options.grid = this;
    if (validator.empty(options.name)) {
      throw "define column name";
    }

    if (this.get('_columns.' + options.name)) {
      throw "column with this '" + options.name + "' already exists";
    }

    var field = App.GridHeader.extend(options);
    this.columns.push(field);

    if (field.filterable || 1) { // .filterable - field not working :(
      this.fieldNames.push(options.name);
    }
  },
  prepareColumns:function () {
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
  },
  prepareFilters:function () {
    var collection = this.get('collection');
    var fieldNames = this.get('fieldNames');
    var options = {};

    if (collection && collection.content && collection.content.length) {
      collection.forEach(function (i, object) {
//        console.warn("INTO");
//        console.warn(object, object.content);
        $.each(fieldNames, function (j, field) {
          if (!options[field]) {
            options[field] = [];
          }
//          var value = object.get(field); repair this
//          options[field].push({value:value});
        });
      })

//      console.warn("SORT OPTIONS:", options);
//      this.set('fieldNames', false);
//      indexOf

//      console.warn("LENGTH:" + collection.content.length);
//      console.warn("fieldNames:", this.get('fieldNames'));
    }
//    var fieldNames = this.get('fieldNames');

//    $.each(this.get('collection'), function(){})

  }.observes('collection'),
  prepareRows:function () {
    var collection = this.get('collection');
    var thisGrid = this;
    console.warn("PREPARE ROWS");
    if (collection && collection.content && collection.content.length) {
      collection.forEach(function (object) {
        console.warn("FOREACH COLLECTION");
        var row = App.GridRow.extend({grid:thisGrid, object:object});
        thisGrid.rows.push(row);
      });
    }

  }.observes('collection')
});