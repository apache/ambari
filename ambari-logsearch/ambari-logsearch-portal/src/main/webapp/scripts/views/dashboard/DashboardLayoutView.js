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
    'dashboard',
    'utils/Globals',
    'collections/VEventHistoryList',
    'hbs!tmpl/dashboard/DashboardLayoutView_tmpl'
], function(require, Backbone, Utils, dashboard, Globals, VEventHistory, DashboardLayoutView_Tmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends DashboardLayoutView */
        {
            _viewName: 'DashboardLayoutView',

            template: DashboardLayoutView_Tmpl,


            /** ui selector cache */
            ui: {
                gridClose: "[data-id='gridClose']",
                saveData: "[data-id='save']",
                loadData: "[data-id='load']",
                enableHelp: "[data-id='enableHelp']"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.gridClose] = 'onGridCloseClick';
                events['click ' + this.ui.saveData] = 'onSaveDataClick';
                events['click ' + this.ui.loadData] = 'onLoadDataClick';
                events['change ' + this.ui.enableHelp] = 'onEnableHelpClick';
                return events;
            },
            regions: {},

            /**
             * intialize a new DashboardLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params', 'componetList'));
                this.bindEvents();
                this.tabOpend = false;
                this.graphIdCounter = 0;
                this.dateUtil = Utils.dateUtil;
                this.vent = new Backbone.Wreqr.EventAggregator();
                this.storedValueCollection = new Backbone.Collection();
                this.saveDahboardCollection = new VEventHistory();
                this.gridHelp = true;
            },
            bindEvents: function() {
                this.listenTo(this.globalVent, "tab:click", function($this) {
                    if (!this.tabOpend) {
                        this.tabOpend = true
                        this.initializePlugin();
                        //this.dashboard.bindEvent();
                    }
                }, this);
            },
            onRender: function() {
                this.setSettingCheckbox();
            },
            addElement: function(el, data) {
                var that = this;
                //this.dashboard.addWidget($('<li class="new" data-type="' + type + '"></li>'), 3, 2);
                var regionid = that.createRegion();
                if (data) {
                    el.attr('data-type', data.myData.type)
                }

                el.append('<div class="regionBox" data-regionid= "' + regionid + '"  id = "' + regionid + '"> </div>')
                el.attr('data-dataId', regionid)
                var dataType = el.data();
                if (dataType && dataType.type) {
                    that.addRegion(regionid, dataType.type, data);
                }

            },
            initializePlugin: function() {
                var that = this;
                this.dashboard = $(".gridster").dashboard({
                    widget_margins: [10, 10],
                    widget_base_dimensions: [140, 140],
                    resize: {
                        enabled: true,
                        stop: function(e, ui, $widget) {
                            $widget.resize();
                        },
                        axes: ['both']
                    },
                    max_cols: 7,
                    avoid_overlapped_widgets: true,
                    dock: {
                        dockIcon: [{
                                iClass: "fa fa-bar-chart",
                                parentAttr: {
                                    "data-type": Globals.graphType.HISTOGRAM.value,
                                    "class": "icon"
                                }
                            }, {
                                iClass: "fa fa-line-chart",
                                parentAttr: {
                                    "data-type": Globals.graphType.MULTILINE.value,
                                    "class": "iconG"
                                }
                            }, {
                                iClass: "fa fa-pie-chart",
                                parentAttr: {
                                    "data-type": Globals.graphType.PIE.value,
                                    "class": "iconG"
                                }
                            },
                            /* {
                                    iClass: "fa fa-table",
                                    parentAttr: {
                                        "data-type": Globals.graphType.TABLE.value,
                                        "class": "iconG"
                                    }
                            }*/
                        ],
                        position: 'right',
                        dockClick: true,
                        droppable: {
                            /*  el: that.$(".dashboard .grid-container"),*/
                            hoverClass: "over",
                            accept: ".slideMenu li.iconG"
                        },
                        draggable: {
                            /*el: that.$('.dashboard .slideMenu li.iconG'),*/
                            cursor: "move",
                            start: function(event, ui) {},
                            appendTo: '.grid-container',
                            scroll: false,
                            helper: "clone"

                        }
                    },
                    serialize_params: function($w, wgd) {
                        return {
                            myData: {
                                type: $w.data('type'),
                                dataId: $w.data('dataid')
                            },
                            col: wgd.col,
                            row: wgd.row,
                            size_x: wgd.size_x,
                            size_y: wgd.size_y
                        };
                    },
                    onLoaded: function() {
                        //console.log('loaded')
                        that.onLoadDataClick();
                    },
                    onAdded: function(el, data) {
                        that.addElement(el, data);
                    },
                    onDeleted: function() {
                        console.log("delete");
                    },
                    onAllDeleted: function() {
                        console.log("onAllDeleted");
                    }
                }).data('dashboard');

                /*   this.gridster = this.$(".gridster ul").gridster({
                       widget_margins: [10, 10],
                       widget_base_dimensions: [140, 140],
                       resize: {
                           enabled: true
                       },
                       draggable: {
                           handle: '.Header'
                       },
                       resize: {
                           enabled: true,
                           stop: function(e, ui, $widget) {
                               $widget.resize();
                           }
                       },
                       avoid_overlapped_widgets: true,
                       serialize_params: function($w, wgd) {
                           return {
                               myData: $w.data('type'),
                               col: wgd.col,
                               row: wgd.row,
                               size_x: wgd.size_x,
                               size_y: wgd.size_y
                           };
                       },

                   }).data('gridster');*/

                this.$('.dashboard').on('mouseenter', 'li.iconG', function() {
                    that.$('.dashboard').find('.grid-container').addClass('droupAreaHoverEffect');
                });
                this.$('.dashboard').on('mouseleave', 'li.iconG', function() {
                    that.$('.dashboard').find('.grid-container').removeClass('droupAreaHoverEffect');
                });
            },
            addRegion: function(region_id, type, data) {
                var that = this;
                require(['views/graphs/GridGraphLayoutView', 'views/dashboard/GridTableLayoutView'], function(GridGraphLayoutView, GridTableLayoutView) {
                    var region = that.getRegion(region_id);
                    var ViewtypeObj = type;
                    var modalObj = that.storedValueCollection.add(new Backbone.Model({
                        id: region_id
                    }))
                    if (data) {
                        modalObj.set(data);
                    }
                    var options = {
                        globalVent: that.globalVent,
                        vent: that.vent,
                        viewType: ViewtypeObj,
                        model: modalObj,
                        dashboard: that.dashboard,
                        gridHelp: that.gridHelp

                    }
                    if (type == Globals.graphType.TABLE.value) {
                        that.showRegion(region, new GridTableLayoutView(options));
                    } else {
                        that.showRegion(region, new GridGraphLayoutView(options));
                    }

                })

            },
            createRegion: function(rId) {
                var id = (rId) ? (rId) : ("grid_histo" + (this.graphIdCounter++));
                var region = {};
                region[id] = '#' + id;
                this.addRegions(region);
                return id;

            },
            showRegion: function(region, layout) {
                region.show(layout);
            },
            onSaveDataClick: function() {
                var that = this;
                var exportData = this.dashboard.exportData();

                _.each(exportData, function(ref, i) {
                    var obj = JSON.parse(JSON.stringify(ref));
                    if (that.storedValueCollection.get(ref.myData.dataId)) {
                        var id = ref.myData.dataId;
                        exportData[i] = _.extend(that.storedValueCollection.get(id).attributes, obj)
                    }
                });

                var postObject = {
                    userId: 0,
                    name: "Temp",
                    rowType: "dashboard",
                    isOverwrite: true,
                    values: JSON.stringify(exportData)
                }

                this.saveDahboardCollection.saveDashboard(postObject, {
                    success: function(data, textStatus, jqXHR) {
                        Utils.notifySuccess({
                            content: "Dashboard saved successfully."
                        });
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        Utils.notifyError({
                            content: JSON.parse(jqXHR.responseText).msgDesc || "There is some problem in Dashboard, Please try again later."
                        });
                    },
                    complete: function() {
                        that.$("#loaderEvent").hide();
                        that.$(".loader").hide();
                    }
                });

            },
            onLoadDataClick: function() {
                var that = this;
                $.extend(this.saveDahboardCollection.queryParams, {
                    userId: 0,
                    rowType: "dashboard"
                });
                that.$(".loader").show();
                this.saveDahboardCollection.fetch({
                    error: function(jqXHR, textStatus, errorThrown) {
                        Utils.notifyError({
                            content: "There is some problem in Event History, Please try again later."
                        });

                    },
                    success: function(data, textStatus, jqXHR) {
                    	if(that.saveDahboardCollection.length){
                    		var dataObject = JSON.parse(that.saveDahboardCollection.models[0].get('values'));
                            //that.saveDahboardCollection.reset(dataObject);
                            //that.storedValueCollection.reset(dataObject);
                            //var importData = that.storedValueCollection.toJSON();
                            that.dashboard.importData(dataObject);
                    	}
                    },
                    complete: function() {
                        //that.$("#loaderEvent").hide();
                        that.$(".loader").hide();
                    }
                });
            },
            onGridCloseClick: function(e) {
                this.dashboard.deleteWidget($(e.currentTarget).parents('li'))
            },
            setSettingCheckbox: function() {
                var gridHelp = Utils.localStorage.checkLocalStorage('gridHelp');
                if (gridHelp && gridHelp.found && gridHelp.value) {
                    if (gridHelp.value == "false") {
                        this.gridHelp = false;
                        this.ui.enableHelp.prop("checked", false);
                    } else {
                        this.gridHelp = true;
                        this.ui.enableHelp.prop("checked", true);
                    }
                }
            },
            onEnableHelpClick: function(e) {
                if (e.currentTarget.checked) {
                    this.gridHelp = true;
                    Utils.localStorage.setLocalStorage('gridHelp', true);
                } else {
                    this.gridHelp = false;
                    Utils.localStorage.setLocalStorage('gridHelp', false);
                }
            },
        });


});
