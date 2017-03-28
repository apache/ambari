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

define(['require',
    'backbone',
    'utils/Utils',
    'hbs!tmpl/dialog/ApplySearchFilterView_tmpl'
], function(require, Backbone, Utils, ApplySearchFilterViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends SaveSearchFilterView */
        {
            _viewName: 'SaveSearchFilterView',

            template: ApplySearchFilterViewTmpl,


            /** ui selector cache */
            ui: {
                applyFilter: "[data-id = 'applyFilter']",
                deleteFilter: "[data-id = 'deleteFilter']"
            },

            regions: {
                'rTable': 'div[data-id="r_table"]'
            },


            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.applyFilter] = 'onApplyClick';
                events['click ' + this.ui.deleteFilter] = 'onDeleteClick';
                events["click [data-id='searchFilter']"] = 'onSearchFilterClick';
                events["keypress [data-id='filterName']"] = 'onSearchFilterKeypress';
                return events;
            },

            /**
             * intialize a new SaveSearchFilterView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'collection'));
                this.dateUtil = Utils.dateUtil;
                this.bindEvents();
                this.fetchFilters();

            },
            bindEvents: function() {
                this.listenTo(this.collection, 'reset', function() {

                }, this);
            },
            onRender: function() {
                this.renderTable();
            },
            fetchFilters: function() {

                var that = this;
                $.extend(this.collection.queryParams, {
                    rowType:"history"
                });
                this.collection.getFirstPage({
                    error: function(jqXHR, textStatus, errorThrown) {
                        Utils.notifyError({
                            content: "There is some problem in Event History, Please try again later."
                        });
                        that.initializeData();
                    },
                    reset: true
                });
            },
            renderTable: function() {
                var that = this;
                var cols = new Backgrid.Columns(this.getColumns());
                require(['views/common/TableLayout'],function(TableLayout){
                    that.rTable.show(new TableLayout({
                        columns: cols,
                        collection: that.collection,
                        includeFilter: false,
                        includePagination: true,
                        includePageSize: true,
                        includeFooterRecords: true,
                        gridOpts: {
                            emptyText: 'No records found!'
                        },
                        filterOpts: {},
                        paginatorOpts: {}
                    })); 
                });
            },
            getColumns: function() {
                var that = this,
                    cols = {
                        filtername: {
                            label: "Name",
                            cell: "String",
                            editable: false,
                            sortType: 'toggle',
                            sortable: true,
                            direction: 'ascending',
                            className: "filterName",
                            width: 20
                        },
                        values: {
                            label: "Message",
                            cell: "html",
                            editable: false,
                            sortType: 'toggle',
                            sortable: false,
                            className: "logMessage",
                            formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                fromRaw: function(rawValue, model) {
                                    return that.showParams(JSON.parse(rawValue), model.get('id'))

                                }
                            })

                        }
                    }


                return this.collection.constructor.getTableCols(cols, this.collection);
            },
            showParams: function(params, id) {
               
                return '<pre class="applyFilter">' +
                       '<button class="btn btn-primary btn-app-sm pull-right" data-nameId="' + id + '" data-id="applyFilter"><i class="fa fa-check"></i></button>' +
                       '<button class="btn btn-primary btn-app-sm pull-right" data-nameId="' + id + '" data-id="deleteFilter"><i class="fa fa-times"></i></button>' +
                       '<strong>Range:</strong>' + (this.createInnerSpan(params, "from")) + '<strong>&nbsp:To:&nbsp:</strong>' + (this.createInnerSpan(params, "to")) + '<br>' +
                       '<strong>Level:</strong>' + (this.createInnerSpan(params, "level")) + '<br>' +
                       '<strong>Include Components:</strong>' + (this.createInnerSpan(params, "mustBe")) + '<br>' +
                       '<strong>Exclude Components:</strong>' + (this.createInnerSpan(params, "mustNot")) + '<br>' +
                       '<strong>Include Columns:</strong>' + (this.createInnerSpan(params, "includeQuery")) + '<br>' +
                       '<strong>Exclude Columns:</strong>' + (this.createInnerSpan(params, "excludeQuery")) +
                       '</pre>';

            },
            createInnerSpan: function(params, type) {
                var typeString = "",
                    that = this;
                if (params[type]) {
                    if(type == "includeQuery" || type == "excludeQuery"){
                        typeString += "<span>"+params[type].replace("},{",",").replace("[{","").replace("}]","")+"</span>";
                    }else{
                        Utils.encodeIncludeExcludeStr(params[type], false, ((type == "iMessage" || type == "eMessage") ? ("|i::e|") : (","))).map(function(typeName) {
                            typeString += '<span class="' + ((type != "level") ? (type) : (typeName)) + '">' +
                                ((type == "from" || type == "to") ? (that.dateUtil.getTimeZone(params[type])) : (Utils.escapeHtmlChar(typeName))) + '</span>' +
                                ((type == "level") ? (",") : (""));
                        });
                    }

                }
                return ((typeString.length == 0) ? ("-") : ((type == "level") ? ((typeString).slice(0, -1)) : (typeString)))
            },
            onApplyClick: function(e) {
                this.selectedModel = this.collection.findWhere({
                    id: ""+parseInt($(arguments[0].currentTarget).attr('data-nameId'))
                });
                this.trigger("apply:filter",this.selectedModel);
            },
            onDeleteClick: function(e) {
                var that = this;
                var postObject = {
                    id: parseInt($(arguments[0].currentTarget).attr('data-nameId'))
                }
                this.collection.deleteEventHistory(postObject, {
                    success: function(data, textStatus, jqXHR) {
                        Utils.notifySuccess({
                            content: "Event History Deleted successfully."
                        });
                        that.fetchFilters();
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        Utils.notifyError({
                            content: "There is some problem in Event History, Please try again later."
                        });
                    }
                });

            },
            onSearchFilterClick: function() {
                var filterName = this.$("[data-id='filterName']").val();
                $.extend(this.collection.queryParams, {
                    filterName: filterName
                });
                this.fetchFilters();
            },
            onSearchFilterKeypress: function(e) {
                if (e.which == 13) {
                    this.onSearchFilterClick();
                }
            }
        });


});
