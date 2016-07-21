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
	'jsx!modules/Table/Pagination',
	'jsx!components/RadialChart',
	'collections/VSupervisorList',
	'models/VSupervisor'
	], function(React, ReactDOM, Table, Utils, Pagination, RadialChart, VSupervisorList, VSupervisor){
	'use strict';

	return React.createClass({
		displayName: 'SupervisorSummary',
		propTypes: {
			fromDashboard: React.PropTypes.bool
		},
		getInitialState: function(){
			this.initializeCollection();
			return null;
		},
		initializeCollection: function(){
			this.collection = new VSupervisorList();
			this.collection.fetch({
				success: function(model, response){
					if(response.error || model.error){
						Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
					} else {
						var result = [];
						if(!_.isArray(response.supervisors)){
							response.supervisors = new Array(response.supervisors);
						}
						response.supervisors.map(function(s){
							result.push(new VSupervisor(s));
						});
						this.collection.getFirstPage().fullCollection.reset(result);
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching supervisor summary data.");
				}
			});
		},
		getColumns: function(){
			return [
				{name: 'host', title: 'Host', tooltip:'The hostname reported by the remote host. (Note that this hostname is not the result of a reverse lookup at the Nimbus node.)', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return ( <a href={this.props.model.get('logLink')} target="_blank"> {this.props.model.get('host')} </a> );
					}
				})},
				{name: 'slotsTotal', title: 'Slots', tooltip:'Slots are Workers (processes).', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return (<RadialChart innerRadius="19" outerRadius="21" 
							color={["#bcbcbc", "#235693"]} 
							data={[this.props.model.get('slotsUsed'), this.props.model.get('slotsTotal')]}
							labels={['Used','Total']}/>
						);
					}
				})},
				{name: 'totalCpu', title: 'CPU', tooltip:'CPU that has been allocated.', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return (<RadialChart innerRadius="19" outerRadius="21" 
							color={["#bcbcbc", "#235693"]} 
							data={[this.props.model.get('usedCpu'), this.props.model.get('totalCpu')]}
							labels={['Used','Total']}/>
						);
					}
				})},
				{name: 'totalMem', title: 'Memory', tooltip:'Memory that has been allocated.', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return (<RadialChart innerRadius="19" outerRadius="21" 
							color={["#bcbcbc", "#235693"]} 
							data={[this.props.model.get('usedMem'), this.props.model.get('totalMem')]}
							labels={['Used','Total']}/>
						);
					}
				})},
				{name: 'uptime', title: 'Uptime', tooltip:'The length of time a Supervisor has been registered to the cluster.', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return (<small>{this.props.model.get('uptime')}</small>);
					}
				})}
			];
		},
		handleFilter: function(e){
			var value = e.currentTarget.value;
			this.collection.search(value);
		},
		render: function(){
			var elemI = null,
				pagination = null,
				elemBox = null;
			if(this.props.fromDashboard){
				elemI = ( <div className="box-control">
		                    <a className="primary" href="#!/supervisor"><i className="fa fa-external-link"></i></a>
		                </div> )
			} else {				
		        pagination = ( <Pagination collection={this.collection} /> );
		        elemBox = (
		        		<div className="input-group col-sm-4">
								<input type="text"  onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Host" />
								<span className="input-group-btn">
								<button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
								</span>
						</div>
		        	);
			}
			return (
				<div className="box">
		            <div className="box-header">
		                <h4>Supervisor Summary</h4>
		                {elemI}
		            </div>
		            <div className={this.props.fromDashboard ? "box-body paddless" : "box-body"}>
		            	{elemBox}
		            	<Table className="table no-margin supervisor-table" collection={this.collection} emptyText="No supervisor found !" columns={this.getColumns()} limitRows={this.props.fromDashboard ? "3" : undefined}/>
		            	{pagination}
		            </div>
		        </div>
			);
		}
	});
});