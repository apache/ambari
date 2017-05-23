/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define(['react',
 'react-dom'],
 function(React, ReactDOM) {
	'use strict';
    return React.createClass({
		displayName: 'SearchBar',
		getInitialState: function() {
			return null;
		},
		render: function() {
			return (
				<div className="col-md-3 pull-right searchbar">
                    <div className="input-group">
                        <input type="text" id="searchBox" className="form-control" placeholder="Search in Logs"/>
                        <div className="input-group-btn">
                            <div className="btn-group" role="group">
                                <div className="dropdown dropdown-lg">
                                    <button type="button" className="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
                                        <span className="caret"></span>
                                    </button>
                                    <div className="dropdown-menu dropdown-menu-right" role="menu">
                                        <form>
                                            <div>
                                                <label><input type="checkbox" id="searchArchivedLogs"/> Search archived logs</label>
                                            </div>
                                            <div>
                                                <label><input type="checkbox" id="deepSearch"/> Deep Search</label>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                                <button type="button" className="btn btn-default" onClick={this.handleSearch}>
                                    <i className="fa fa-search"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
			);
    	},
        handleSearch: function(){
            var searchBoxEl = document.getElementById('searchBox');
            var searchArchivedLogsEl = document.getElementById('searchArchivedLogs');
            var deepSearchEl = document.getElementById('deepSearch');
            var topologyId = this.props.id;

            $.get(App.baseURL.replace('proxy?url=', 'storm_details'))
              .success(function(response){
                var url = JSON.parse(response).hostdata+'/';
                if(deepSearchEl.checked == true){
                    url += "deep_search_result.html";
                }else{
                    url += "search_result.html";
                }
                url += '?search='+searchBoxEl.value+'&id='+ topologyId +'&count=1';
                if(searchArchivedLogsEl.checked == true){
                    if(deepSearchEl.checked == true){
                        url += "&search-archived=on";
                    }else{
                        url += "&searchArchived=checked";
                    }
                }
                window.open(url, '_blank');

                searchBoxEl.value = '';
                searchArchivedLogsEl.checked = false;
                deepSearchEl.checked = false;
              });
        },
    });
});
