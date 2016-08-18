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

define(['backbone','utils/LangSupport', 'backbone.marionette'],function(Backbone,localization) {
    'use strict';

	//var SessionMgr		= require('mgrs/SessionMgr');
    /*
     * Localization initialization
     */
    localization.setDefaultCulture(); // will take default that is en
    
	var App = new Backbone.Marionette.Application();
	//App.userProfile = SessionMgr.getUserProfile();

	/* Add application regions here */
	App.addRegions({
    	rTopNav			: "#r_topNav",
        rTopProfileBar	: "#r_topProfileBar",
        rBreadcrumbs	: '#r_breadcrumbs',
        rContent		: "#r_content",
        rFooter			: "#r_footer",
        rHeader         : "#r_header"
	});

	/* Add initializers here */
	App.addInitializer( function () {
	//	Communicator.mediator.trigger("Application:Start");
        window._preventNavigation = false;
        Backbone.history.start();
	});
     
    //viewDeployFlag set to false for standalone
    //viewDeployFlag set to true for ambariview
    var viewDeployFlag = false;
    if(viewDeployFlag) {

        var serverUrl = location.pathname+'proxy?url=';
        var urlParts = location.pathname.split('/');
        var tempUrl = "/api/v1/views/"+urlParts[2]+"/versions/"+urlParts[3]+"/instances/"+urlParts[4]+"/resources/status";
        $.ajax({
            type: 'GET',
            dataType: 'json',
            async: false,
            context: this,
            url: tempUrl,
            success: function(response){
                    serverUrl += "http://" + response.parameters['logsearch.server.url']+"/api/v1/";
            },
            error: function(response){
                    console.log("Error while getting log search server url");
            }
        });
        
    
        App.baseUrl = serverUrl//Utils.getLogSearchHostDetails();
    } else {
        App.baseUrl = "api/v1/";
    }

    // Add initialize hooks
    App.on("initialize:before", function() {
    });
	
    App.on("initialize:after", function() {
    });
    
    App.addInitializer(function(options) {
        console.log('Creating new Router instance');
    });
    
    return App;
});

