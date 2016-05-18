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
	'collections/VNimbusList',
	'models/VNimbus'
	], function(React, ReactDOM, Table, Utils, Pagination, VNimbusList, VNimbus){
	'use strict';

	return React.createClass({
		displayName: 'NimbusSummary',
		getInitialState: function(){
			this.initializeCollection();
			return null;
		},
		initializeCollection: function(){
			this.collection = new VNimbusList();
			this.collection.fetch({
				success: function(model, response){
					if(response.error){
						Utils.notifyError(response.error);
					} else {
						var result = [];
						if(!_.isArray(response.nimbuses)){
							response.nimbuses = new Array(response.nimbuses);
						}
						response.nimbuses.map(function(n){
							n['host:port'] = n.host+':'+n.port;
							result.push(new VNimbus(n));
						});
						this.collection.getFirstPage().fullCollection.reset(result);
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching nimbus summary data.");
				}
			});
		},
		getColumns: function(){
			return [
				{name: 'host', title: 'Host:Port', tooltip: 'Nimbus hostname and port number', component: React.createClass({
					render: function(){
						return ( <a href={this.props.model.get('nimbusLogLink')} target="_blank"> {this.props.model.get('host:port')} </a> );
					}
				})},
				{name: 'status', title: 'Status', tooltip: 'Leader if this host is leader, Not a Leader for all other live hosts, note that these hosts may or may not be in leader lock queue, and Dead for hosts that are part of nimbus.seeds list but are not alive.', component: React.createClass({
					render: function(){
						var classname="label ";
						switch(this.props.model.get("status")){
							case 'Leader':
								classname += "label-success";
							break;
							// case 'Follower':
							// 	classname += "label-warning";
							// break;
							default:
								classname += "label-warning";
							break;
						}
						return (<span className={classname}>{this.props.model.get('status')}</span>);
					}
				})},
				{name: 'nimbusUpTime', title: 'Uptime', tooltip: 'Time since this nimbus host has been running.', component: React.createClass({
					render: function(){
						return (<small>{this.props.model.get('nimbusUpTime')}</small>);
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
		                    <a className="primary" href="#!/nimbus"><i className="fa fa-external-link"></i></a>
		                </div> )
			} else {				
		        pagination = ( <Pagination collection={this.collection} /> );
		        elemBox = (
		        		<div className="input-group col-sm-4">
								<input type="text"  onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Key" />
								<span className="input-group-btn">
								<button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
								</span>
						</div>
		        	);
			}
			return (
				<div className="box">
		            <div className="box-header">
		                <h4>Nimbus Summary</h4>
		                {elemI}
		            </div>
		            <div className={this.props.fromDashboard ? "box-body paddless" : "box-body"}>
		                {elemBox}
		            	<Table className="table no-margin" collection={this.collection} emptyText="No nimbus found !" columns={this.getColumns()} limitRows={this.props.fromDashboard ? "6" : undefined}/>
		            	{pagination}
		            </div>
		        </div>
			);
		}
	});
});