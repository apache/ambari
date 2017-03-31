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
    'moment',
    'd3.tip',
    'utils/Globals',
    'utils/Utils',
    'collections/VLogList',
    'hbs!tmpl/graphs/GridGraphLayoutView_tmpl',
    'bootstrap-daterangepicker',
    'nv'
], function(require, Backbone, moment, tip, Globals, Utils, VLogList, GridGraphLayoutViewTmpl, daterangepicker, nv) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends GridGraphLayoutView */
        {
            _viewName: 'GridGraphLayoutView',

            template: GridGraphLayoutViewTmpl,


            /** ui selector cache */
            ui: {
                histoGraph: "div[data-id='rHistogramGraph']",
                dateRange: "#dateRange",
                selectDateRange: ".selectDateRange",
                dateRangeTitle: "span[data-id='dateRangeTitle']",
                gridSettingPopup: "[data-id='gridSettingPopup']",
                gridHeader: ".gridHeader"

            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.viewType] = 'onViewTypeChange';
                events['click [data-id="refresh-tab-graph"]'] = 'onTabRefresh';
                events['click ' + this.ui.gridSettingPopup] = 'onGridSettingPopupClick';
                return events;
            },

            /**
             * intialize a new GridGraphLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params', 'viewType', 'dashboard', 'model', 'gridHelp', 'collection', 'showHeader'));
                this.vent = new Backbone.Wreqr.EventAggregator();
                if(! this.collection){
                	this.collection = new VLogList([], {
                        state: {
                            firstPage: 0,
                            pageSize: 999999999,

                        }
                    });
                	this.collection.url = Globals.baseURL + "audit/logs/anygraph";
                    this.collection.modelAttrName = "graphData";
                }
                this.dateUtil = Utils.dateUtil;
                this.dateRangeLabel = new String();

                this.bindEvents();
                this.graphParams = {};
                this.unit = "+1HOUR";
                var modalParams = this.model.get('params');
                if (modalParams && modalParams.unit) {
                    this.unit = modalParams.unit
                }

                this.firstRender = true;

                if (!this.viewType || this.viewType == Globals.graphType.HISTOGRAM.value) {
                    this.histogramView = true;
                } else if (this.viewType == Globals.graphType.MULTILINE.value) {
                    this.lineView = true;
                } else if (this.viewType == Globals.graphType.PIE.value) {
                    this.pieView = true;
                }

            },
            bindEvents: function() {
                this.listenTo(this.collection, "reset", function(collection) {
                    this.$(".loader").hide();
                    this.$("#loaderGraph").hide();
                }, this);
                this.listenTo(this.collection, 'request', function() {
                    this.$(".loader").show();
                    this.$("#loaderGraph").show();
                }, this);
                this.listenTo(this.collection, 'sync error', function() {
                    this.$(".loader").hide();
                    this.$("#loaderGraph").hide();
                }, this);
                this.listenTo(this.vent, "graph:update", function(options) {

                    options['params'] = this.params
                    if (this.model) {
                        options['id'] = this.model.get('id')
                        this.model.clear().set(options)
                    }
                    this.configureChart(options);
                }, this);
                this.listenTo(this.vent, "graph:data:update", function(params, options) {
                    this.params = params
                    options['params'] = this.params
                    if (this.model) {
                        options['id'] = this.model.get('id')
                        this.model.clear().set(options)
                    }
                    this.fetchGraphData(params, options);
                }, this);
                this.listenTo(this.vent, "graph:grid:update", function(options) {
                    options['params'] = this.params
                    if (this.model) {
                        options['id'] = this.model.get('id')
                        this.model.clear().set(options)
                    }
                    this.updateGrid(options)
                }, this);

            },
            onRender: function() {
                var that = this;
                if (this.model) {
                    var mObject = this.model.toJSON();
                    if (mObject.params) {
                        this.params = mObject.params;
                        this.fetchGraphData(mObject.params, mObject);
                    }
                    this.updateGrid(mObject);
                }
                if (this.gridHelp) {
                    setTimeout(function() {
                        this.$('.gridSettinghand').hide();
                    }, 3000);
                } else {
                    this.$('.gridSettinghand').hide();
                }
                if(! _.isUndefined(this.showHeader)){
                	if(! this.showHeader)
                		this.$(".gridHeader").hide();
                }	
            },

            fetchGraphData: function(params, options) {
                var that = this;
                that.$("#loaderGraph").show();
                that.$(".loader").show();
                _.extend(this.collection.queryParams, params);
                this.collection.fetch({
                    reset: true,
                    success: function() {
                        that.createDataForGraph(options)
                    }
                });
            },
            updateGrid: function(options) {
                if (options.title) {
                    this.ui.gridHeader.find('.gridTitle').text(options.title);
                }
                /*if (options.gridSizeX && options.gridSizeY) {
                    this.dashboard.resize_widget(this.$el.parents('li'), parseInt(options.gridSizeX), parseInt(options.gridSizeY))
                }*/
            },
            createDataForGraph: function(options) {
                var that = this,
                    color = d3.scale.category20().range();
                this.data = [];
                this.dataL = [];
                if (this.lineView || this.histogramView) {
                    _.each(this.collection.models, function(model, i) {
                        var Obj = {
                            key: model.get('name'),
                            values: model.get('dataCount').map(function(object) {
                                return {
                                    x: (options && options.xAxis == "evtTime") ? (that.dateUtil.getMomentObject(object.name)) : (object.name), //(new Date(object.name)).toUTCString(),
                                    y: parseFloat(object.value)
                                }
                            })
                        };
                        if (that.histogramView) {
                               Obj['color'] = (((""+model.get('name')).toUpperCase() === 'ERROR') ? ("#E81D1D") :
                                   ( (""+model.get('name')).toUpperCase() === 'INFO') ? ("#2577B5") :
                                   ( (""+model.get('name')).toUpperCase() === 'WARN') ? ("#FF8916") :
                                   ( (""+model.get('name')).toUpperCase() === 'FATAL') ? ("#830A0A") :
                                   ( (""+model.get('name')).toUpperCase() === 'DEBUG') ? ("#65E8FF") :
                                   ( (""+model.get('name')).toUpperCase() === 'TRACE') ? ("#888888") :
                                   ( (""+model.get('name')).toUpperCase() === 'UNKNOWN') ? ("#bdbdbd") : color[i]);
                           } else {
                               Obj['color'] = color[i];
                           }
                        that.data.push(Obj);
                    });
                }
                if (this.pieView) {
                    _.each(this.collection.models, function(model, i) {
                        that.data = model.get('dataCount').map(function(object) {
                            return {
                                x: (object.name),
                                y: parseFloat(object.value)
                            }
                        });
                    });
                }
                if (that.histogramView) {
                    for (var i = this.data.length - 1; i >= 0; i--) {
                        this.dataL.push(this.data[i])
                    }
                }
                this.configureChart(options)
                    //(!this.firstRender) ? this.updateGraph(this.data, this.dataL): this.renderGraph(this.data, this.dataL);

            },
            //Using NVD3
            configureChart: function(options) {
                var that = this,
                    formatValue = d3.format(".2s");

                if (this.firstRender) {
                    if (that.histogramView) {
                        that.chart = nv.models.multiBarChart()
                            .showControls(false)
                            .showXAxis(true)
                            .showLegend(true);
                        that.chart.groupSpacing(0.6) // for bar width and aspace 
                        this.showLegend = true
                    } else if (this.lineView) {
                        that.chart = nv.models.lineChart().options({
                            transitionDuration: 300,
                            useInteractiveGuideline: true
                        });
                        //We want nice looking tooltips and a guideline!

                        that.chart.showLegend(true); //Show the legend, allowing users to turn on/off line series.
                        that.chart.showYAxis(true); //Show the y-axis
                        that.chart.showXAxis(true);

                    } else if (this.pieView) {

                        var height = this.$el.find('.gridGraph').height() || 260;
                        var width = this.$el.find('.gridGraph').width() - 20 || 420;
                        that.chart = nv.models.pieChart()
                            .x(function(d) {
                                return d.x
                            })
                            .y(function(d) {
                                return d.y
                            })
                            .width(width)
                            .height(height);
                    }
                    that.chart.legend.margin({
                        top: 5,
                        right: 50,
                        left: 90,
                        bottom: 10
                    });
                    that.chart.margin({
                        right: 30,
                        left: 30
                    });

                    that.chart.dispatch.on('renderEnd', function() {
                        if (that.firstRender) {
                            that.firstRender = false
                            var svgElem = that.$('svg');
                            if (that.histogramView) {
                                var elem = svgElem.find('g.nv-barsWrap.nvd3-svg .nv-groups');
                            } else {
                                var elem = svgElem.find('g.nv-linesWrap.nvd3-svg .nv-groups');
                            }
                            if (elem.get(0)) {
                                that.svgHeight = elem.get(0).getBBox().height;
                                that.svgWidth = elem.get(0).getBBox().width;
                            }
                        }
                        that.graphXAxisBreak();
                    });
                    /*          that.chart.xAxis
                                  .tickFormat(function(d) {
                                      var date = that.dateUtil.getTimeZoneFromMomentObject(((that.histogramView) ? (d) : (that.dateUtil.getMomentObject(d))));
                                      return (d3.time.format('%H:%M:%S - %m/%d/%y')(date))
                                  });
                              that.chart.yAxis
                                  .tickFormat(function(d) {
                                      return formatValue(d).replace('G', 'B');
                                  });*/
                    /*             that.chart.tooltip.contentGenerator(
                                     function(data) {
                                         var tootTipTemplate = '<div>' +
                                             '<table>' +
                                             '<thead>' +
                                             '<tr>' +
                                             '<td colspan="3"><strong class="x-value">' + data[((that.histogramView) ? ('data') : ('point'))].x.format('MM/DD/YYYY') + '</strong></td>' +
                                             '</tr>' +
                                             '<tr>' +
                                             '<td colspan="3"><strong class="x-value">' + data[((that.histogramView) ? ('data') : ('point'))].x.format('H:mm:ss') + '</strong></td>' +
                                             '</tr>' +
                                             '</thead>' +
                                             '<tbody>' +
                                             '<tr>' +
                                             '<td class="legend-color-guide">' +
                                             '<div style="background-color: ' + ((that.histogramView) ? (data.color) : (data.series[0].color)) + '"></div>' +
                                             '</td>' +
                                             '<td class="key">' + ((that.histogramView) ? (data.data.key) : (data.series[0].key)) + '</td>' +
                                             '<td class="value">' + ((that.histogramView) ? (data.data.size) : (data.series[0].value)) + '</td>' +
                                             '</tr>' +
                                             '</tbody>' +
                                             '</table>' +
                                             '</div>'

                                         return tootTipTemplate
                                     });*/
                    that.chart.legend.dispatch.legendClick = function(d, i) {
                        that.$('svg').find('g.nv-x  g.tick').hide();
                    };
                    that.$el.parents('.gs-w').resize(function() {
                        if (that.svg) {
                            if (that.pieView) {
                                that.chart.height(that.$el.find('.gridGraph').height());
                                that.chart.width(that.$el.find('.gridGraph').width() - 20);
                            }
                            that.svg.transition().duration(0).call(that.chart);
                        } else {
                            var elem = that.$('[data-id=rHistogramGraph] svg');
                            d3.select(elem[0])
                                .attr('width', that.$el.parents('.brick').width())
                                .attr('height', that.$el.parents('.brick').height())
                                .transition().duration(0)
                                .call(that.chart);
                        }
                    });
                }

                
                // For legend and control margin
                if (options) {
                	if(options.rotateXaxis){
                		this.chart.xAxis.rotateLabels(options.rotateXaxis);
                	}
                    if (this.histogramView) {
                        if (options.stackOrGroup && options.stackOrGroup == "Group") {
                            this.chart.stacked(false);
                        } else {
                            this.chart.stacked(true);
                        }
                    }

                    if (this.histogramView || this.lineView) {
                        if (options.showX) {
                            that.chart.showXAxis(true);
                            if (options.xAxis) {

                                if (options.xAxis == "evtTime") {
                                    var xTimeFormat = ""
                                    if (options.xTimeFormat) {
                                        xTimeFormat = options.xTimeFormat;
                                    } else {
                                        xTimeFormat = "%H:%M:%S.%L - %m/%d/%y"
                                    }
                                    this.chart.x(function(d) {
                                        return d.x
                                    });
                                    this.chart.xAxis
                                        .tickFormat(function(d) {
                                            var xAxisFormat = xTimeFormat;
                                            var date = that.dateUtil.getTimeZoneFromMomentObject(((that.histogramView) ? (d) : (that.dateUtil.getMomentObject(d))));
                                            return (d3.time.format(xAxisFormat)(date))
                                        });
                                    if (options.startDate && options.endDate) {}
                                    if (options.unit) {}
                                } else {

                                    if (!this.histogramView) {
                                        this.chart.xAxis
                                            .tickFormat(function(d) {
                                                return that.data[0].values[d].x // x will be same for stack and group only y is different so we take name of x from first value array
                                            });
                                        this.chart.x(function(d, i) {
                                            return i
                                        });
                                         this.chart.interpolate('basis')
                                    } else {
                                        this.chart.xAxis
                                            .tickFormat(function(d) {
                                                return d
                                            });
                                        this.chart.x(function(d) {
                                            return d.x
                                        });
                                    }
                                }
                            }
                        } else {
                            this.chart.showXAxis(false);
                        }

                        // Y axis Setting
                        if (options.showY) {
                            that.chart.showYAxis(true);
                            var yFormat = ""
                            if (options.yAxisFormat && options.yAxisFormat.length) {
                                yFormat = options.yAxisFormat;
                            } else {
                                yFormat = ".2s"
                            }
                            var yformatValue = d3.format(yFormat);
                            this.chart.y(function(d) {
                                return parseInt(d.y)
                            })
                            this.chart.yAxis
                                .tickFormat(function(d) {
                                    return yformatValue(d) //.replace('G', 'B');
                                });

                        } else {
                            this.chart.showYAxis(false);
                        }
                    }
                    if (this.pieView) {
                        if (options.pieOrDonut == "donut") {
                            that.chart.donut(true);
                            that.chart.donutRatio(0.35);
                        } else {
                            that.chart.donut(false);
                        }
                    }



                    // Legend  Setting
                    if (options.showLegend) {
                        this.chart.showLegend(true);
                        this.showLegend = true;
                    } else {
                        this.chart.showLegend(false);
                        this.showLegend = false;
                    }
                    // refreshInterval 

                    if (options.refreshInterval && options.refreshInterval.length) {
                        this.setRefereshInterval(options);
                    } else {
                        if (this.refereshInterval) {
                            this.clearRefereshInterval();
                        }
                    }
                    this.updateGraph(options)

                } else {
                    this.updateGraph()
                }

            },
            setRefereshInterval: function(options) {
                var that = this;
                clearInterval(this.refereshInterval);
                this.refereshInterval = setInterval(function() {
                    that.fetchGraphData({}, options);
                }, options.refreshInterval);
            },
            clearRefereshInterval: function() {
                clearInterval(this.refereshInterval);
            },
            updateGraph: function(options) {
                if (options) {
                    var elem = this.$('svg').empty();
                } else {
                    var elem = this.$('svg');
                }
                // Update the SVG with the new this.data and call chart
                if (this.data) {
                    this.svg = d3.select(elem[0]).datum(this.data);
                    if (this.pieView) {
                        this.chart.height(this.$el.find('.gridGraph').height());
                        this.chart.width(this.$el.find('.gridGraph').width() - 20);
                    }
                    this.svg.transition().duration(0).call(this.chart);
                    //this.svg.datum(this.data).transition().duration(0).call(this.chart);
                    if (this.dataL.length > 0 && this.showLegend) {
                        this.svg.datum(this.dataL).transition().duration(0).call(this.chart.legend);
                        if (d3.select(this.$('svg').children().first()[0]).classed('nvd3 nv-legend')) {
                            d3.select(this.$('svg').children().first()[0]).remove();
                        }
                        var legendObject = (this.$('svg').children()[0]);
                        if (this.$(legendObject).is('g[class="nvd3 nv-legend"]')) {
                            this.$(legendObject).remove();
                        } else if (this.$(legendObject).is('text[class="nvd3 nv-nodata"]')) {
                            this.$(this.$('svg').children()[1]).remove();
                        }

                    }
                }


            },
            graphXAxisBreak: function() {
                var insertLinebreaks = function() {
                    var el = d3.select(this);
                    var words = $(this).text().split('-');
                    $(this).text('');
                    for (var i = 0; i < words.length; i++) {
                        var tspan = el.append('tspan').text(words[i]);
                        if (i > 0)
                            tspan.attr('x', 0).attr('dy', '19');
                    }
                };
                if (this.$('svg').find('g.nv-x  g.tick text').first().text().split('-').length > 1) {
                    this.$('svg').find('g.nv-x  g.tick text').each(insertLinebreaks);
                    this.$('svg').find('g.nv-x  g.nv-axisMaxMin text').each(insertLinebreaks);
                    this.$('svg').find('g.nv-x  g.tick').show();
                } else {
                    this.$('svg').find('g.nv-x  g.tick').show();
                }
            },
            setupDialog: function(options) {
                var that = this;
                require(['views/common/JBDialog'], function(JBDialog) {
                    var opts = _.extend({
                        appendTo: that.$el,
                        modal: true,
                        resizable: false,
                        beforeClose: function(event, ui) {
                            that.onDialogClosed();
                        }
                    }, options);
                    var dialog = that.dialog = new JBDialog(opts).render().open();
                })

            },
            onGridSettingPopupClick: function() {
                this.$('.gridSettinghand').hide();
                this.dashboard.disable();
                var that = this;
                var overlay = document.createElement('div');
                overlay.setAttribute('class', 'overlayDashboard');
                this.$el.append(overlay);
                require(['views/dialog/GridGraphSettingView'], function(GridGraphSettingView) {
                    var view = new GridGraphSettingView({
                        vent: that.vent,
                        params: (that.model) ? (that.params) : ({}),
                        model: that.model,
                        viewType: that.viewType
                    });

                    that.setupDialog({
                        title: "Setting",
                        content: view,
                        viewType: 'Save',
                        width: 560,
                        height: 500,
                        buttons: [{
                            id: "cancelBtn",
                            text: "Close",
                            "class": "btn btn-default",
                            click: function() {
                                that.onDialogClosed();
                            }
                        }]
                    });
                });
            },
            onDialogClosed: function() {
                this.$el.find('.overlayDashboard').remove();
                this.dashboard.enable();
                if (this.dialog) {
                    this.dialog.close && this.dialog.close();
                    this.dialog.remove && this.dialog.remove();
                    this.dialog = null;
                }
            }
        });


});
