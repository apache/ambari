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
    'utils/Globals',
    'utils/Utils',
    'moment',
    'models/VGraphInfo',
   	'hbs!tmpl/tabs/HostInfoTabLayoutView_tmpl' 
],function(require,Backbone,Globals,Utils,moment,VGraphInfo,HostInfoTabLayoutViewTmpl){
    'use strict';

	return Backbone.Marionette.Layout.extend(
		/** @lends HostInfoTabLayoutView */
		{
			_viewName : 'HostInfoTabLayoutView',

			template: HostInfoTabLayoutViewTmpl,

			/** Layout sub regions */
			regions: {
				
			},

			/** ui selector cache */
			ui: {
			},

			/** ui events hash */
			events: function() {
				var events = {};
				return events;
			},

			/**
			 * intialize a new HostInfoTabLayoutView Layout
			 * @constructs
			 */
			initialize: function(options) {
				_.extend(this, _.pick(options,'globalVent'));
				this.graphModel = new VGraphInfo();
				this.bindEvents();
			},
			bindEvents : function(){
			},
			onRender : function(){
				this.fetchGraphData();
			},
			fetchGraphData : function(){
				var that = this;
				this.graphModel.getCriticalLogsInfo({},{
					dataType:"json",
					success : function(data,textStatus,jqXHR){
						that.graphModel.set(data);
						that.renderGraph();
					},
					error : function(){
					},
					complete : function(){
						that.$("#loaderGraph").hide();
						that.$(".loader").hide();
					}
				});
			},
			renderGraph : function(){
				var that = this;
				var data = [];
				var error = {
					"key": "ERROR",
					"color": "#E81D1D",
					"values" : []
				};
				var fatal = {
					"key": "FATAL",
					"color": "#830A0A",
					"values" : []	
				}
				if(this.graphModel.get("errorCount")){
					_.each(this.graphModel.get("errorCount").compName,function(v,i){
						error.values.push({
							label : v,
							value : that.graphModel.get("errorCount").countMsg[i],
							message : that.graphModel.get("errorCount").cricticalMsg[i]
						});
					});
				}
				if(this.graphModel.get("fatalCount")){
					_.each(this.graphModel.get("fatalCount").compName,function(v,i){
						fatal.values.push({
							label : v,
							value : that.graphModel.get("fatalCount").countMsg[i],
							message : that.graphModel.get("fatalCount").cricticalMsg[i]
							
						});
					});
				}
				data.push(error);
				data.push(fatal);
				var parentWidth = (that.$('svg').parent().width()),
                parentHeight = (that.$('svg').parent().height()),
                width = ((parentWidth === 0) ? (293) : (parentWidth)), // -15 because  parent has 15 padding 
                height = ((parentHeight === 0) ? (150) : (parentHeight)) // -15 because  parent has 15 padding
				nv.addGraph(function() {
					var chart = nv.models.multiBarHorizontalChart()
						.width(width)
					  	.height(height)
					  	.x(function(d) { return d.label })
					  	.y(function(d) { return d.value })
					  	.margin({top: 15, right: 10, bottom: 25, left: 80})
					  	.showValues(false)
					  	//.tooltips(false)
					  	.showControls(false)
					  	.showLegend(false);

					chart.yAxis
					      .tickFormat(d3.format(',.2f'));
					chart.tooltip.contentGenerator(
                            function(data) {
                                var tootTipTemplate = '<div>' +
                                    '<table>' +
                                    '<thead>' +
                                    '<tr>' +
                                    '<td colspan="3"><strong class="x-value">' + data.value + '</strong></td>' +
                                    '</tr>' +
                                    '<tr>' +
                                    //'<td colspan="3"><small class="x-value" style="font-size:5px">' + data.data.message + '</small></td>' +
                                    '</tr>' +
                                    '</thead>' +
                                    '<tbody>' +
                                    '<tr>' +
                                    '<td class="legend-color-guide">' +
                                    '<div style="background-color: ' + data.color + '"></div>' +
                                    '</td>' +
                                    '<td class="key">' + data.data.key + '</td>' +
                                    '<td class="value">' + data.data.value + '</td>' +
                                    '</tr>' +
                                    '</tbody>' +
                                    '</table>' +
                                    '</div>'

                                return tootTipTemplate
                            })
					chart.tooltip.enabled();
					chart.dispatch.on('renderEnd', function() {
						d3.selectAll("#hostCriticalGraph g .nv-bar").on("click",function(d){
							Utils.alertPopup({msg : "<pre>"+d.message+"</pre>"});
						})
					});
					d3.select('#hostCriticalGraph svg')
						.datum(data)
					    .transition().duration(500)
					    .call(chart);
					
					return chart;
				});
			}
			
			
		
	});
	
});
	