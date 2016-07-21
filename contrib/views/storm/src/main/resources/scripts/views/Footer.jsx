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

define(['react', 'react-dom', 'models/VCluster', 'utils/Utils'], function(React, ReactDOM, VCluster, Utils) {
	'use strict';
	return React.createClass({
		displayName: 'Footer',
		getInitialState: function(){
			this.initializeData();
			return {
				version: 0
			};
		},
		initializeData: function(){
			this.model = new VCluster();
			this.model.fetch({
				success: function(model, response){
					if(response.error || model.error){
						Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
					} else {
						this.setState({version: model.get('stormVersion')});
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching cluster summary data.");
				}
			});
		},
		render: function() {
			return (<p className="text-center">Apache Storm - v{this.state.version}</p>);
		}
	});
});