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
	'react-dom',
	'utils/Utils',
	'jsx!components/RadialChart',
	'models/VCluster',
	'jsx!containers/NimbusSummary',
	], function(React, ReactDOM, Utils, RadialChart, VCluster, NimbusSummary){
	'use strict';

	return React.createClass({
		displayName: 'ClusterSummary',
		getInitialState: function(){
			this.initializeData();
			return {
				executorsTotal: 0,
				tasksTotal: 0,
				supervisors: 0,
				slotsUsed: 0,
				slotsTotal:0
			};
		},
		initializeData: function(){
			this.model = new VCluster();
			this.model.fetch({
				success: function(model, response){
					if(response.error || model.error){
						Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
					} else {
						this.setState(model.attributes);
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching cluster summary data.");
				}
			});
		},
		componentDidMount: function(){
			$('[data-rel="tooltip1"]').tooltip({
				placement: 'bottom'
			});
		},
		render: function(){
			return (
				<div className="col-sm-5">
					<div className="row">
				        <div className="col-sm-6">
				            <div className="tile primary" title="Executors are threads in a Worker process." data-rel="tooltip1">
				                <div className="tile-header">Executor</div>
				                <div className="tile-body">
				                    <i className="fa fa-play-circle-o"></i>
				                    <span className="count">{this.state.executorsTotal}</span>
				                </div>
				            </div>
				        </div>
				        <div className="col-sm-6">
				            <div className="tile warning" title="A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors." data-rel="tooltip1">
				                <div className="tile-header">Tasks</div>
				                <div className="tile-body">
				                    <i className="fa fa-tasks"></i>
				                    <span className="count">{this.state.tasksTotal}</span>
				                </div>
				            </div>
				        </div>
				    </div>
				    <div className="row">
			            <div className="col-sm-6">
			                <div className="tile success" title="The number of nodes in the cluster currently." data-rel="tooltip1">
			                    <div className="tile-header" style={{textAlign:"center"}}>Supervisor</div>
			                    <div className="tile-body" style={{textAlign:"center"}}>
			                        <div id="supervisorCount">
			                            <RadialChart width="100" height="100" innerRadius="46" outerRadius="50" 
			                            	color={["rgba(255,255,255,0.6)", "rgba(255,255,255,1)"]} 
			                            	data={[this.state.supervisors, this.state.supervisors]}
			                            	labels={['Used','Total']}
			                            />
			                        </div>
			                    </div>
			                </div>
			            </div>
			            <div className="col-sm-6">
			                <div className="tile danger" title="Slots are Workers (processes)." data-rel="tooltip1">
			                    <div className="tile-header" style={{textAlign:"center"}}>Slots</div>
			                    <div className="tile-body" style={{textAlign:"center"}}>
			                        <div id="slotsCount">
			                            <RadialChart width="100" height="100" innerRadius="46" outerRadius="50" 
			                            	color={["rgba(255,255,255,0.6)", "rgba(255,255,255,1)"]} 
			                            	data={[this.state.slotsUsed, this.state.slotsTotal]}
			                            	labels={['Used','Total']}
			                            />
			                        </div>
			                    </div>
			                </div>
			            </div>
			        </div>
			        <div className="row">
			        	<div className="col-sm-12">
			        		<NimbusSummary fromDashboard={true} />
			        	</div>
			        </div>
			    </div>
			);
		}
	});
});