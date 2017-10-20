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
	'jsx!components/Table',
	'utils/Utils',
	'collections/VTopologyList',
	'models/VTopology',
	'jsx!components/RadialChart',
	'jsx!modules/Table/Pagination',
	'jsx!containers/SupervisorSummary'
	], function(React, ReactDOM, Table, Utils, VTopologyList, VTopology, RadialChart, Pagination, SupervisorSummary){
	'use strict';

	return React.createClass({
		displayName: 'TopologyListing',
		propTypes: {
			fromDashboard: React.PropTypes.bool
		},
		getInitialState: function(){
			this.initializeCollection();
			return null;
		},
		initializeCollection: function(){
			this.collection = new VTopologyList();
			this.collection.fetch({
				success: function(model, response){
					if(response.error || model.error){
						Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
					} else {
						var result = [];
						if(!_.isArray(response.topologies)){
							response.topologies = new Array(response.topologies);
						}
						response.topologies.map(function(t){
							result.push(new VTopology(t));
						});
						this.collection.getFirstPage().fullCollection.reset(result);
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching topology listing data.");
				}
			});
		},
		getColumns: function(){
			var columns = [
				{name: 'name', title: 'Topology Name', tooltip:'The name given to the topology by when it was submitted. Click the name to view the Topology\'s information.', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return ( <a href={"#!/topology/"+this.props.model.get('id')}> {this.props.model.get('name')} </a>);
					}
				})},
				{name: 'status', title: 'Status', tooltip:'The status can be one of ACTIVE, INACTIVE, KILLED, or REBALANCING.', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						var classname="label ";
						switch(this.props.model.get("status")){
							case 'ACTIVE':
								classname += "label-success";
							break;
							case 'INACTIVE':
								classname += "label-default";
							break;
							case 'REBALANCING':
								classname += "label-warning";
							break;
							case 'KILLED':
								classname += "label-danger";
							break;
							default:
								classname += "label-primary";
							break;
						}
						return ( <span className={classname}> {this.props.model.get("status")} </span> );
					}
				})}
			];
			if(!this.props.fromDashboard){
				var additionalColumns = [
					{name: 'assignedTotalMem', title: 'Memory Assigned (MB)', tooltip:'Assigned Total Memory by Scheduler.'},
					{name: 'workersTotal', title: 'Workers', tooltip:'The number of Workers (processes).'},
					{name: 'executorsTotal', title: 'Executors', tooltip:'Executors are threads in a Worker process.'},
					{name: 'tasksTotal', title: 'Tasks', tooltip:'A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.'},
					{name: 'owner', title: 'Owner', tooltip:'The user that submitted the Topology, if authentication is enabled.'}
				];
				Array.prototype.push.apply(columns, additionalColumns);
			}
			columns.push({name: 'uptime', title: 'Uptime', tooltip:'The time since the Topology was submitted.', component: React.createClass({
				propTypes: {
					model: React.PropTypes.object.isRequired
				},
				render: function(){
					return (<small>{this.props.model.get('uptime')}</small>);
				}
			})})
			return columns;
		},
		handleFilter: function(e){
			var value = e.currentTarget.value;
			this.collection.search(value);
		},
		render: function(){
			var completeElem = null,
				className = null;

			if(this.props.fromDashboard){
				var topologyListingElem = (
					<div className="row">
						<div className="col-sm-12">
							<div className="box">
					            <div className="box-header">
					                <h4>Topology Listing</h4>
					                <div className="box-control">
					                    <a className="primary" href="#!/topology"><i className="fa fa-external-link"></i></a>
					                </div>
					            </div>
					            <div className="box-body paddless">
					            	<Table className="table no-margin" collection={this.collection} emptyText="No topology found !" columns={this.getColumns()} limitRows={this.props.fromDashboard ? "5" : undefined}/>
					            </div>
					        </div>
						</div>
					</div>
				);
				var supervisorSummaryELem = (
					<div className="row">
						<div className="col-sm-12">
							<SupervisorSummary fromDashboard={true} />
						</div>
					</div>
				);
				completeElem = (
					<div>
						{topologyListingElem}{supervisorSummaryELem}
					</div>
				);
				className = "col-sm-7";
			} else {
				var headerELem = (
					<div className="box-header">
		                <h4>Topology Listing</h4>		                
		            </div>);
		        var bodyElem = (
		        	<div className="box-body">
		        		<div className="input-group col-sm-4">
								<input type="text"  onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Topology Name" />
								<span className="input-group-btn">
								<button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
								</span>
						</div>
		            	<Table className="table no-margin" collection={this.collection} emptyText="No topology found !" columns={this.getColumns()} limitRows={this.props.fromDashboard ? "5" : undefined}/>
		            	<Pagination collection={this.collection} />
		            </div>);
				completeElem = (
					<div>
		            	{headerELem}{bodyElem}
		            </div>
				);
				className = "box";
			}
			return (
				<div className={className}>
					{completeElem}
				</div>
			);
		}
	});
});