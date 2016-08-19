/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */



define(['require',
    'utils/Utils',
    'moment',
    'collections/VNameValueList',
    'utils/Globals'
], function(require, Utils, moment, VNameValueList, Globals) {
    'use strict';

    var ViewUtil = {};

    ViewUtil.setdefaultParams = function() {
        var fromSolr, toSolr,that=this;
        var params = Utils.getQueryParams(window.location.search);
        this.defaultParams = {
            q: "*:*",
            from: moment().hours("00").minutes("00").seconds("01").milliseconds("000").toJSON(),
            to: moment().hours("23").minutes("59").seconds("59").milliseconds("999").toJSON(),
            unit: "+1HOUR",
            level: "FATAL,ERROR,WARN"
        };
        var applyParamsDate = function(date) {
            if (date) {
                var dateString = date.split(',');
                if (dateString.length) {
                    var checkDate = Utils.dateUtil.getMomentUTC(dateString[0]);
                    if (checkDate.isValid()) {
                        if (dateString[1]) {
                            checkDate.millisecond(dateString[1])
                        } else {
                            checkDate.millisecond('000')
                        }
                        return checkDate.toJSON();
                    }
                }
            }
        }
        if (params.bundle_id && !params.start_time && !params.end_time) {
            var collection = new VNameValueList();

            collection.url = Globals.baseURL + "service/logs/solr/boundarydates";
            collection.modelAttrName = "vNameValues";
            _.extend(collection.queryParams, {
                "bundle_id": params.bundle_id
            });
            collection.fetch({
                reset: true,
                async: false,
                success: function(data) {
                    collection.each(function(model) {
                        if (model.get('name') == "From") {
                            fromSolr = moment(parseInt(model.get('value'))).toJSON();
                        }
                        if (model.get('name') == "To") {
                            toSolr = moment(parseInt(model.get('value'))).toJSON();
                        }
                        if(fromSolr && toSolr){
                            that.defaultParams['dateRangeLabel'] = "Custom Range";
                        }
                    })

                }
            });
        }
        if (params.bundle_id) {
            this.defaultParams['bundle_id'] = params.bundle_id;
        }
        if (params.start_time) {
            var startDateString = applyParamsDate(params.start_time);
        }
        if (params.end_time) {
            var endDateString = applyParamsDate(params.end_time);
        }
        if (params.host_name) {
            this.defaultParams['host_name'] = params.host_name;
        }
        if (params.component_name) {
            this.defaultParams['component_name'] = params.component_name;
        }
        if (params.file_name) {
            this.defaultParams['file_name'] = params.file_name;
        }
        if (startDateString && endDateString) {
            if (params.timezone) {
                var timeZoneObject = worldMapTime.getTimeZoneObject(params.timezone)
                if (timeZoneObject.length) {
                    var timeZoneName = timeZoneObject[0].timezone
                }
                if (timeZoneName) {
                    var startEncodeDate = params.start_time.replace(",", ".")
                    var endEncodeDate = params.end_time.replace(",", ".")
                    startDateString = moment.tz(startEncodeDate, timeZoneName).toJSON();
                    endDateString = moment.tz(endEncodeDate, timeZoneName).toJSON();
                    var timeZoneString = timeZoneName + "," + timeZoneObject[0].zoneName + "," + timeZoneObject.length;
                    Utils.localStorage.setLocalStorage('timezone', timeZoneString);
                    moment.tz.setDefault(timeZoneName);
                }
            }
            this.defaultParams['end_time'] = endDateString
            this.defaultParams['start_time'] = startDateString;
            this.defaultParams['from'] = startDateString;
            this.defaultParams['to'] = endDateString;
            this.defaultParams['dateRangeLabel'] = "Custom Range";
        }
        if (fromSolr && toSolr) {
            this.defaultParams['from'] = fromSolr;
            this.defaultParams['to'] = toSolr;
            this.defaultParams['dateRangeLabel'] = "Custom Range";
        }
    }
    ViewUtil.getDefaultParams = function() {
        return $.extend(true, {}, this.defaultParams);
    }
    ViewUtil.getCountDistributionHTML = function(node) {
        if (!node.logLevelCount)
            return "";
        return '<div data-node = "' + node.name + '" class="nodebar">' + ViewUtil.getLevelDistributionHTML(node) + '</div>';
    };
    ViewUtil.getLevelDistributionHTML = function(node) {
        var html = "";
        if (!_.isUndefined(node.logLevelCount) && !_.isArray(node.logLevelCount))
            node.logLevelCount = [node.logLevelCount];
        var toPct = ViewUtil.calculatePercentge(node.logLevelCount);
        _.each(node.logLevelCount, function(data) {
            //html += '<div class="node '+data.name+'" style="width:'+toPct(data)+'%;" data-toggle="tooltip" title="'+data.value+'" data-original-title="'+data.value+'"></div>';
            html += '<div class="node ' + data.name + '" style="width:' + toPct(data) + '%;"></div>';
        });
        return html;
    };
    ViewUtil.calculatePercentge = function(values) {
        var sum = 0;
        for (var i = 0; i != values.length; ++i) {
            sum = sum + parseInt(values[i].value, 10);
        }
        var scale = 100 / sum;
        return function(x) {
            return (parseInt(x.value, 10) * scale) /*.toFixed(5)*/ ;
        };
    };

    ViewUtil.getDefaultParamsForHierarchy = function() {
        return ViewUtil.getDefaultParams();
    };

    ViewUtil.setLatestTimeParams = function(params) {
        if (params && params.dateRangeLabel) {
            var arr = Utils.dateUtil.getRelativeDateFromString(params.dateRangeLabel);
            if (_.isArray(arr)) {
                params.from = arr[0].toJSON();
                params.to = arr[1].toJSON();
            };
        }

    };
    
    ViewUtil.foramtLogMessageAsLogFile = function(model, logMessageHtmlClass){
    	var attrs = model.attributes;
		var str="";
		if(attrs.logtime)
			str += moment(attrs.logtime).format("YYYY-MM-DD HH:mm:ss,SSS")+" "; 
		if(attrs.level)
			str += "<span class='"+(""+attrs.level).toUpperCase()+"'>"+(""+attrs.level).toUpperCase()+"</span> ";
		if(attrs.thread_name)
			str += $.trim(attrs.thread_name)+" ";
		if(attrs.logger_name)
			str += $.trim(attrs.logger_name)+" ";
		if(attrs.file && attrs.line_number)
			str += attrs.file+":"+attrs.line_number+" ";
//		if(attrs.cluster)
//			str += attrs.cluster+" ";
		if(attrs.log_message)
			str += "<span class='"+(logMessageHtmlClass ? logMessageHtmlClass : "logMessage")+"'>- "+Utils.escapeHtmlChar(attrs.log_message)+" </span>";
		return str;
    }
    
    ViewUtil.scrollToElement = function(top, speed){
    	if(!top)
    		return;
    	$("html, body").animate({ scrollTop : (top -200) }, (speed) ? speed : 300);
    };
    
    ViewUtil.formatAuditGraphData = function(collection){
    	var mainObj = [],len=0,that=this;
		collection.each(function(m,index){
			var userName = m.get("name");
			if(len < userName.length )
				len = userName.length;
			var compo = m.get("dataCount");
			for(var i=0;i < compo.length; i++){
				var b = {label:userName,value:parseInt(compo[i].value,10)}
				var ret = _.findWhere(mainObj,{key:compo[i].name});
				if(ret){
					ret.values.push(b);
				}else{
					mainObj.push({
						key : compo[i].name,
						values : [b],
						color : Utils.getRandomColor(compo[i].name) 
					});
				}
			}
		});
		return {
			max : len,
			arr : mainObj
		}
    }

    return ViewUtil;

});
