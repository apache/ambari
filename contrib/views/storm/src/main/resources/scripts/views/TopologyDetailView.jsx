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

define([
	'jsx!components/Table',
	'jsx!modules/Table/Pagination',
	'utils/Utils',
	'react',
	'react-dom',
	'collections/BaseCollection',
	'models/VTopology',
	'models/BaseModel',
	'jsx!containers/TopologyConfiguration',
	'jsx!containers/TopologyDetailGraph',
	'jsx!components/Breadcrumbs',
	'jsx!views/RebalanceView',
	'bootbox',
	'x-editable',
	'bootstrap',
	'bootstrap-switch'
	],function(Table, Pagination, Utils, React, ReactDOM, BaseCollection, VTopology, BaseModel, TopologyConfiguration, TopologyDetailGraph, Breadcrumbs, RebalanceView, bootbox, XEditable){
	'use strict';

	return React.createClass({
		displayName: 'TopologyDetailView',
		getInitialState: function(){
			this.model = new VTopology({'id': this.props.id});
			this.spoutCollection = new BaseCollection();
			this.boltCollection = new BaseCollection();
			this.systemFlag = false;
			this.windowSize = ':all-time';
			this.initializeData();
			return {
				model: this.model,
				graphData: {},
				logLevels: {},
				rebalanceModalOpen: false
			};
		},
		componentWillMount: function(){
			$('.loader').show();
		},
		componentDidMount: function(){
			$(".boot-switch.systemSum").bootstrapSwitch({
				size: 'small',
				onSwitchChange: function(event, state){
					this.systemFlag = state;
					this.initializeData();
				}.bind(this)
			});
			$("#slideContent").hide();
			$(".boot-switch.debug").bootstrapSwitch({
				size: 'small',
				onSwitchChange: function(event, state){
					this.debugAction(state);
				}.bind(this)
			});
			$('[data-rel="tooltip"]').tooltip();
			$('.loader').hide();
		},
		componentWillUpdate: function(){
			$('.loader').show();
			$('#collapse-spout').off('hidden.bs.collapse');
			$('#collapse-spout').off('shown.bs.collapse');
			$('#collapse-bolt').off('hidden.bs.collapse');
			$('#collapse-bolt').off('shown.bs.collapse');
			$('#modal-rebalance').off('hidden.bs.modal');
			this.spoutCollection.getFirstPage().fullCollection.reset([]);
			this.spouts = this.renderSpouts();
			this.boltCollection.getFirstPage().fullCollection.reset([]);
			this.bolts = this.renderBolts();
		},
		componentDidUpdate: function(){
			$('#collapse-spout').on('hidden.bs.collapse', function () {
				$("#spout-box").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#spout-box").toggleClass("fa-compress fa-expand");
			});

			$('#collapse-bolt').on('hidden.bs.collapse', function () {
				$("#bolt-box").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#bolt-box").toggleClass("fa-compress fa-expand");
			});
			$('#modal-rebalance').on('hidden.bs.modal', function (e) {
			  this.initializeData();
			  this.setState({"rebalanceModalOpen":false});
			}.bind(this));
			if(this.state.rebalanceModalOpen){
				$('#modal-rebalance').modal("show");
			}
			$('.loader').hide();
		},
		initializeData: function(){
			this.model.getData({
				id: this.model.get('id'),
				window: this.windowSize,
				sys: this.systemFlag,
				success: function(model, response){
					if(response.error){
						Utils.notifyError(response.error);
					} else {
						this.model.set(model);
						this.setState({"model": this.model});
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching topology details.");
				}
			});
			this.initializeGraphData();
			this.initializeLogConfig();
		},
		initializeGraphData: function(){
			this.model.getGraphData({
				id: this.model.get('id'),
				window: this.windowSize,
				success: function(model, response){
					if(response.error){
						Utils.notifyError(response.error);
					} else {
						if(_.isString(model)){
							model = JSON.parse(model);
						}
						this.setState({graphData: model});
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching topology visualization data.");
				}
			});
		},

		initializeLogConfig: function() {
			this.collection = new BaseCollection();
			this.model.getLogConfig({
				id: this.model.get('id'),
				success: function(model, response){
					if(response.error){
						Utils.notifyError(response.error);
					} else {
						this.resetLogCollection(model);
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching log configuration data.");
				}
			});
		},
		resetLogCollection: function(model) {
			this.collection.reset();
			this.setState({logLevels: model.namedLoggerLevels});
			var keys = _.keys(this.state.logLevels);
				keys.map(function(key, index) {
						var obj = this.state.logLevels[key];
						var model = new BaseModel({
							logger: key,
							target_level: obj.target_level,
							timeout: obj.timeout,
							timeout_epoch: obj.timeout_epoch
						});
						this.collection.add(model);
				}.bind(this));

			this.collection.add(new BaseModel({
					logger: 'com.your.organization.LoggerName',
					target_level: 'ALL',
					timeout: 30,
					timeout_epoch: 0,
					isAdd: true
			}));
		},

		renderAccordion: function(type, header, searchField, searchCb, collection, emptyText, columns, toggleCb){
			return ( 
				<div className="box">
					<div className="box-header" data-toggle="collapse" data-target={"#collapse-"+type} aria-expanded="false" aria-controls={"collapse-"+type}>
						<h4>{header}</h4>
						<h4 className="box-control">
							<a href="javascript:void(0);" className="primary">
								<i className="fa fa-compress" id={type+"-box"} onClick={toggleCb}></i>
							</a>
						</h4>
					</div>
					<div className="box-body collapse in" id={"collapse-"+type}>
	                	<div className="input-group col-sm-4">
							<input type="text"  onKeyUp={searchCb} className="form-control" placeholder={"Search by "+searchField} />
							<span className="input-group-btn">
							<button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
							</span>
						</div>
		                <Table className="table table-striped" collection={collection} emptyText={emptyText} columns={columns()} />
						<Pagination collection={collection} />
		            </div>
				</div>
			);
		},
		renderSpouts: function(){
			if(this.state.model.has('spouts')){
				Utils.ArrayToCollection(this.state.model.get('spouts'), this.spoutCollection);
				this.spoutCollection.searchFields = ['spoutId'];
				var searchCb = function(e){
					var value = e.currentTarget.value;
					this.spoutCollection.search(value);
				}.bind(this);
				var toggleCb = function(e){
					$("#collapse-spout").collapse('toggle');
				}
				return this.renderAccordion('spout', 'Spouts', 'id', searchCb, this.spoutCollection, 'No spouts found !', this.getSpoutColumns, toggleCb);
			} else {
				return null;
			}
		},
		getSpoutColumns: function(){
			var self = this;
			return [
				{name: 'spoutId', title: 'Id', tooltip:'The ID assigned to a the Component by the Topology. Click on the name to view the Component\'s page.', component: React.createClass({
					render: function(){
						var topologyId = self.state.model.has('id') ? self.state.model.get('id') : "";
						return ( <a href={"#!/topology/"+topologyId+"/component/"+this.props.model.get('spoutId')}>{this.props.model.get('spoutId')}</a>);
					}
				})},
				{name: 'executors', title: 'Executors', tooltip:'Executors are threads in a Worker process.'},
				{name: 'tasks', title: 'Tasks', tooltip:'A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.'},
				{name: 'emitted', title: 'Emitted', tooltip:'The number of Tuples emitted.'},
				{name: 'transferred', title: 'Transferred', tooltip:'The number of Tuples emitted that sent to one or more bolts.'},
				{name: 'completeLatency', title: 'Complete Latency (ms)', tooltip:'The average time a Tuple "tree" takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.'},
				{name: 'acked', title: 'Acked', tooltip:'The number of Tuple "trees" successfully processed. A value of 0 is expected if no acking is done.'},
				{name: 'failed', title: 'Failed', tooltip:'The number of Tuple "trees" that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.'},
				{name: 'errorHost', title: 'Error Host:Port', component: React.createClass({
					render: function(){
						return (<span>{this.props.model.has('errorHost') && this.props.model.get('errorHost') !== '' ? this.props.model.get('errorHost')+':'+this.props.model.get('errorPort') : null}</span>);
					}
				})},
				{name: 'lastError', title: 'Last Error'},
				{name: 'errorTime', title: 'Error Time', component: React.createClass({
					render: function(){
						if(this.props.model.get('errorTime') != 0) {
							var d = new Date(this.props.model.get('errorTime')),
							date = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
							return (<span>{date}</span>);
						} else return (<span></span>);
					}
				})}
			];
		},
		renderBolts: function(){
			if(this.state.model.has('bolts')){
				Utils.ArrayToCollection(this.state.model.get('bolts'), this.boltCollection);
				this.boltCollection.searchFields = ['boltId'];
				var searchCb = function(e){
					var value = e.currentTarget.value;
					this.boltCollection.search(value);
				}.bind(this);
				var toggleCb = function(e){
					$("#collapse-bolt").collapse('toggle');
				}
				return this.renderAccordion('bolt', 'Bolts', 'id', searchCb, this.boltCollection, 'No bolts found !', this.getBoltColumns, toggleCb);
			} else {
				return null;
			}
		},
		getBoltColumns: function(){
			var self = this;
			return [
				{name: 'boltId', title: 'Id', tooltip:'The ID assigned to a the Component by the Topology. Click on the name to view the Component\'s page.', component: React.createClass({
					render: function(){
						var topologyId = self.state.model.has('id') ? self.state.model.get('id') : "";
						return ( <a href={"#!/topology/"+topologyId+"/component/"+this.props.model.get('boltId')}>{this.props.model.get('boltId')}</a>);
					}
				})},
				{name: 'executors', title: 'Executors', tooltip:'Executors are threads in a Worker process.'},
				{name: 'tasks', title: 'Tasks', tooltip:'A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.'},
				{name: 'emitted', title: 'Emitted', tooltip:'The number of Tuples emitted.'},
				{name: 'transferred', title: 'Transferred', tooltip:'The number of Tuples emitted that sent to one or more bolts.'},
				{name: 'capacity', title: 'Capacity (last 10m)', tooltip:"If this is around 1.0, the corresponding Bolt is running as fast as it can, so you may want to increase the Bolt's parallelism. This is (number executed * average execute latency) / measurement time."},
				{name: 'executeLatency', title: 'Execute Latency (ms)', tooltip:'The average time a Tuple spends in the execute method. The execute method may complete without sending an Ack for the tuple.'},
				{name: 'executed', title: 'Executed', tooltip:'The number of incoming Tuples processed.'},
				{name: 'processLatency', title: 'Process Latency (ms)', tooltip:'The average time it takes to Ack a Tuple after it is first received.  Bolts that join, aggregate or batch may not Ack a tuple until a number of other Tuples have been received.'},
				{name: 'acked', title: 'Acked', tooltip:'The number of Tuples acknowledged by this Bolt.'},
				{name: 'failed', title: 'Failed', tooltip:'The number of tuples Failed by this Bolt.'},
				{name: 'errorHost', title: 'Error Host:Port', component: React.createClass({
					render: function(){
						return (<span>{this.props.model.has('errorHost') && this.props.model.get('errorHost') !== '' ? this.props.model.get('errorHost')+':'+this.props.model.get('errorPort') : null}</span>);
					}
				})},
				{name: 'lastError', title: 'Last Error'},
				{name: 'errorTime', title: 'Error Time', component: React.createClass({
					render: function(){
						if(this.props.model.get('errorTime') != 0) {
							var d = new Date(this.props.model.get('errorTime')),
							date = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
							return (<span>{date}</span>);
						} else return (<span></span>);
					}
				})}
			];
		},
		renderWindowOptions: function(){
			if(this.state.model.has('topologyStats')){
				return this.state.model.get('topologyStats').map(function(object, i){
					return ( <option key={i} value={object.window}>{object.windowPretty}</option> );
				});
			} else {
				return null;
			}
		},
		handleWindowChange: function(e){
			this.windowSize = e.currentTarget.value;
			this.initializeData();
		},
		getLinks: function() {
			var links = [
				{link: '#!/dashboard', title: 'Dashboard'},
				{link: '#!/topology', title: 'Topology Listing'},
				{link: 'javascript:void(0);', title: this.state.model.has('name') ? this.state.model.get('name') : ""}
				];
			return links;
		},

		addLogLevel: function(e) {
			var self = this;
			var id = e.currentTarget.getAttribute('data-name');
			var namedLoggerLevels = {};
			var targetLevel = $(e.currentTarget).parent().siblings().find('.target-level').val(),
				timeout = $(e.currentTarget).parent().siblings().find('.timeout').html(),
				logger = $(e.currentTarget).parent().siblings().find('.logger').html();

			namedLoggerLevels[logger] = {
				target_level: targetLevel,
				reset_level: 'INFO',
				timeout: parseInt(timeout, 10)
			};

            var dataObj = {
				namedLoggerLevels: namedLoggerLevels
			}

			this.model.saveLogConfig({
				id: this.model.get('id'),
				data: JSON.stringify(dataObj),
				contentType: "application/json",
              	success: function(model, response, options){
              		if(response.error){
						Utils.notifyError(response.error);
					} else {
						this.resetLogCollection(model);
              			Utils.notifySuccess("Log configuration added successfully.");
					}
              	}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in saving log configuration data.");
				}

			});
		},
		applyLogLevel: function(e) {
			var self = this;
			var id = e.currentTarget.getAttribute('data-name');
			var namedLoggerLevels = {};
			var targetLevel = $(e.currentTarget).parents('td').siblings().find('.target-level').val(),
				timeout = $(e.currentTarget).parents('td').siblings().find('.timeout').html(),
				logger = $(e.currentTarget).parents('td').siblings().find('.logger').html();

			namedLoggerLevels[logger] = {
				target_level: targetLevel,
				reset_level: 'INFO',
				timeout: parseInt(timeout, 10)
			};

            var dataObj = {
				namedLoggerLevels: namedLoggerLevels
			}

			this.model.saveLogConfig({
				id: this.model.get('id'),
				data: JSON.stringify(dataObj),
				contentType: "application/json",
              	success: function(model, response, options){
              		if(response.error){
						Utils.notifyError(response.error);
					} else {
						this.resetLogCollection(model);
              			Utils.notifySuccess("Log configuration applied successfully.");
					}
              	}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in applying log configuration data.");
				}
			});
		},
		clearLogLevel: function(e) {
			var self = this;
			var id = e.currentTarget.getAttribute('data-name');
			var namedLoggerLevels = {};
			var logger = $(e.currentTarget).parents('td').siblings().find('.logger').html();

			namedLoggerLevels[logger] = {
				target_level: null,
				reset_level: 'INFO',
				timeout: 0
			};

            var dataObj = {
				namedLoggerLevels: namedLoggerLevels
			}

			this.model.saveLogConfig({
				id: this.model.get('id'),
				data: JSON.stringify(dataObj),
				contentType: "application/json",
              	success: function(model, response, options){
              		if(response.error){
						Utils.notifyError(response.error);
					} else {
						this.resetLogCollection(model);
              			Utils.notifySuccess("Log configuration cleared successfully.");
					}
              	}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in clearing log configuration data.");
				}
			});
		},
		getColumns: function(){
			var self = this;
			return [
				{name: 'logger', title: 'Logger', component: React.createClass({
					render: function(){
						if(this.props.model.get('isAdd'))
							return (<a href="javascript:void(0)" className="x-editable logger">{this.props.model.get('logger')}</a>);
						else return (<a href="javascript:void(0)" className="logger">{this.props.model.get('logger')}</a>);
					},
					componentDidMount: function() {
						$(".x-editable").editable({
							mode: 'inline'
						});
					}})
			    },
				{name: 'target_level', title: 'Level', component: React.createClass({
					render: function() {
						return (
							<select className="form-control target-level" defaultValue={this.props.model.get('target_level')}>
								<option value="ALL">ALL</option>
								<option value="TRACE">TRACE</option>
								<option value="DEBUG">DEBUG</option>
								<option value="INFO">INFO</option>
								<option value="WARN">WARN</option>
								<option value="ERROR">ERROR</option>
								<option value="FATAL">FATAL</option>
								<option value="OFF">OFF</option>
							</select>
							);
					}
				})},
				{name: 'timeout', title: 'Timeout', component: React.createClass({
					render: function(){
						return (<a href="javascript:void(0)" className="x-editable timeout">{this.props.model.get('timeout')}</a>);
					},
					componentDidMount: function() {
						$(".x-editable").editable({
							mode: 'inline'
						});
					}})
			    },
				{name: 'timeout_epoch', title: 'Expires At', component: React.createClass({
					render: function(){
						if(this.props.model.get('timeout_epoch') != 0) {
							var d = new Date(this.props.model.get('timeout_epoch')),
							date = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
							return (<span>{date}</span>);
						} else return (<span></span>);

					}
					})
				},
				{name: 'action', title: 'Action', component: React.createClass({
					render: function(){
						if(this.props.model.get('isAdd'))
							return(
							<a href="javascript:void(0)"  data-name={this.props.model.get('logger')} className="btn btn-primary btn-xs" onClick={self.addLogLevel}><i className="fa fa-plus"></i></a>
							)
						else
						return (
							<span>
							<a href="javascript:void(0)" data-name={this.props.model.get('logger')} className="btn btn-success btn-xs" onClick={self.applyLogLevel}><i className="fa fa-check"></i></a>&nbsp;
							<a href="javascript:void(0)" data-name={this.props.model.get('logger')} className="btn btn-danger btn-xs" onClick={self.clearLogLevel}><i className="fa fa-times"></i></a>
							</span>
						);
					}
				})}
			];
		},
		toggleSlide: function() {
			$("#slideContent").slideToggle();
		},
		
		renderStatsRow: function(){
			var statsArr = this.state.model.get('topologyStats');
			if(statsArr){
				return statsArr.map(function(stats, i){
					return (
						<tr key={i}>
							<td>{stats.windowPretty}</td>
							<td>{stats.emitted}</td>
							<td>{stats.transferred}</td>
							<td>{stats.completeLatency}</td>
							<td>{stats.acked}</td>
							<td>{stats.failed}</td>
						</tr>
					);
				});
			}
		},
		render: function() {
			var status = this.state.model.has('status') ? this.state.model.get('status') : null;
			var workersTotal = this.state.model.has('workersTotal') ? this.state.model.get('workersTotal').toString() : '0';
			if(this.state.model.get('debug')){
				$(".boot-switch.debug").bootstrapSwitch('state', true, true);
			} else {
				$(".boot-switch.debug").bootstrapSwitch('state', false, true);
			}
			return (
				<div>
					<Breadcrumbs links={this.getLinks()} />
					<div className="row">
						<div className="col-sm-12">
							<div className="box filter">
								<div className="box-body form-horizontal">
									<div className="form-group no-margin">
										<label className="col-sm-1 control-label">Window</label>
										<div className="col-sm-2">
											<select className="form-control" onChange={this.handleWindowChange} value={this.windowSize}>
												{this.renderWindowOptions()}
											</select>
										</div>
										<label className="col-sm-2 control-label">System Summary</label>
										<div className="col-sm-2">
											<input className="boot-switch systemSum" type="checkbox" />
										</div>
										<label className="col-sm-1 control-label">Debug</label>
										<div className="col-sm-1">
											<input className="boot-switch debug" type="checkbox"/>
										</div>
										<div className="col-sm-3 text-right">
											<div className="btn-group" role="group">
												<button type="button" className="btn btn-primary" onClick={this.handleTopologyActivation} title="Activate" data-rel="tooltip" disabled={status === 'ACTIVE' ? "disabled" : null}>
													<i className="fa fa-play"></i>
												</button>
												<button type="button" className="btn btn-primary" onClick={this.handleTopologyDeactivation} title="Deactivate" data-rel="tooltip" disabled={status === 'INACTIVE' ? "disabled" : null}>
													<i className="fa fa-stop"></i>
												</button>
												<button type="button" className="btn btn-primary" onClick={this.handleTopologyRebalancing} title="Rebalance" data-rel="tooltip" disabled={status === 'REBALANCING' ? "disabled" : null}>
													<i className="fa fa-balance-scale"></i>
												</button>
												<button type="button" className="btn btn-primary" onClick={this.handleTopologyKilling} title="Kill" data-rel="tooltip" disabled={status === 'KILLED' ? "disabled" : null}>
													<i className="fa fa-ban"></i>
												</button>
												<button type="button" className="btn btn-primary" onClick={this.toggleSlide} title="Change Log Level" data-rel="tooltip">
													<i className="fa fa-file-o"></i>
												</button>
											</div>
										</div>
									</div>
									<div className="row" id="slideContent">
										<div className="col-sm-12">
											<hr/>
											<h4 className="col-sm-offset-5">Change Log Level</h4>
											<p>Modify the logger levels for topology. Note that applying a setting restarts the timer in the workers. To configure the root logger, use the name ROOT.</p>
											<Table className="table no-margin" collection={this.collection} columns={this.getColumns()}/>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-4">
							<div className="summary-tile">
								<div className="summary-title">Topology Summary</div>
								<div className="summary-body">
									<p><strong>ID: </strong>{this.state.model.get('id')}</p>
									<p><strong>Owner: </strong>{this.state.model.get('owner')}</p>
									<p><strong>Status: </strong>{this.state.model.get('status')}</p>
									<p><strong>Uptime: </strong>{this.state.model.get('uptime')}</p>
									<p><strong>Workers: </strong>{this.state.model.get('workersTotal')}</p>
									<p><strong>Executors: </strong>{this.state.model.get('executorsTotal')}</p>
									<p><strong>Tasks: </strong>{this.state.model.get('tasksTotal')}</p>
									<p><strong>Memory: </strong>{this.state.model.get('assignedTotalMem')}</p>
								</div>
							</div>
						</div>
						<div className="col-sm-8">
							<div className="stats-tile">
								<div className="stats-title">Topology Stats</div>
								<div className="stats-body">
									<table className="table table-enlarge">
										<thead>
											<tr>
												<th><span data-rel="tooltip" title="The past period of time for which the statistics apply.">Window</span></th>
												<th><span data-rel="tooltip" title="The number of Tuples emitted.">Emitted</span></th>
												<th><span data-rel="tooltip" title="The number of Tuples emitted that sent to one or more bolts.">Transferred</span></th>
												<th><span data-rel="tooltip" title='The average time a Tuple "tree" takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.'>Complete Latency (ms)</span></th>
												<th><span data-rel="tooltip" title='The number of Tuple "trees" successfully processed. A value of 0 is expected if no acking is done.'>Acked</span></th>
												<th><span data-rel="tooltip" title='The number of Tuple "trees" that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.'>Failed</span></th>
											</tr>
										</thead>
										<tbody>
											{this.renderStatsRow()}
										</tbody>
									</table>
								</div>
							</div>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-12">
							<TopologyDetailGraph model={this.state.model} graphData={this.state.graphData}/>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-12">
							{this.spouts}
						</div>
					</div>
					<div className="row">
						<div className="col-sm-12">
							{this.bolts}
						</div>
					</div>
					<div className="row">
						<div className="col-sm-12">
							<TopologyConfiguration configArr={this.state.model.get('configuration')}/>
						</div>
					</div>
					{this.state.rebalanceModalOpen ? <RebalanceView modalId="modal-rebalance" topologyId={this.state.model.get('id')} topologyExecutors={workersTotal} spouts={this.state.model.get('spouts')} bolts={this.state.model.get('bolts')}/> : null}
				</div>
			);
	    },
	    handleTopologyActivation: function(e){
	    	if(this.model.get('status') !== 'ACTIVE'){
		    	var msg = "Do you really want to activate this topology ?";
		    	var successCb = function(){
		    		this.model.activateTopology({
		    			id: this.model.get('id'),
		    			success: function(model, response){
		    				if(response.error){
								Utils.notifyError(response.error);
							} else {
								this.initializeData();
		    					Utils.notifySuccess("Topology activated successfully.")
							}
		    			}.bind(this),
						error: function(model, response, options){
							Utils.notifyError("Error occured in activating topology.");
						}
		    		});
		    	}.bind(this);
		    	Utils.ConfirmDialog(msg, '', successCb);
	    	}
	    },
	    handleTopologyDeactivation: function(e){
	    	if(this.model.get('status') !== 'INACTIVE'){
	    		var msg = "Do you really want to deactivate this topology ?";
		    	var successCb = function(){
		    		this.model.deactivateTopology({
		    			id: this.model.get('id'),
		    			success: function(model, response){
		    				if(response.error){
								Utils.notifyError(response.error);
							} else {
								this.initializeData();
		    					Utils.notifySuccess("Topology deactivated successfully.")
							}
		    			}.bind(this),
						error: function(model, response, options){
							Utils.notifyError("Error occured in deactivating topology.");
						}
		    		});
		    	}.bind(this);
		    	Utils.ConfirmDialog(msg, '', successCb);
	    	}
	    },
	    handleTopologyRebalancing: function(e){
	    	if(this.model.get('status') !== 'REBALANCING'){
	    		this.setState({"rebalanceModalOpen":true});
	    	}
	    },
	    handleTopologyKilling: function(e){
	    	if(this.model.get('status') !== 'KILLED'){
	    		bootbox.prompt({
			        title: 'Are you sure you want to kill this topology ? If yes, please, specify wait time in seconds.',
			        value: "30",
			        buttons: {
			          confirm: {
			            label: 'Yes',
			            className: "btn-success",
			          },
			          cancel: {
			            label: 'No',
			            className: "btn-default",
			          }
			        },
			        callback: function(result) {
			          if(result != null){
			            this.model.killTopology({
			    			id: this.model.get('id'),
			    			waitTime: result,
			    			success: function(model, response){
			    				if(response.error){
									Utils.notifyError(response.error);
								} else {
									this.initializeData();
			    					Utils.notifySuccess("Topology killed successfully.")
								}
			    			}.bind(this),
							error: function(model, response, options){
								Utils.notifyError("Error occured in killing topology.");
							}
			    		});
			          }
			        }.bind(this)
			    });
	    	}
	    },
	    debugAction: function(toEnableFlag){
    		if(toEnableFlag){
    			bootbox.prompt({
			        title: 'Do you really want to debug this topology ? If yes, please, specify sampling percentage.',
			        value: "10",
			        buttons: {
			          confirm: {
			            label: 'Yes',
			            className: "btn-success",
			          },
			          cancel: {
			            label: 'No',
			            className: "btn-default",
			          }
			        },
			        callback: function(result) {
			          if(result != null){
			            this.model.debugTopology({
			    			id: this.model.get('id'),
			    			debugType: 'enable',
			    			percent: result,
			    			success: function(model, response){
			    				if(response.error){
									Utils.notifyError(response.error);
								} else {
									this.initializeData();
			    					Utils.notifySuccess("Debugging enabled successfully.")
								}
			    			}.bind(this),
							error: function(model, response, options){
								Utils.notifyError("Error occured in enabling debugging.");
							}
			    		});
			          } else {
			          	$(".boot-switch.debug").bootstrapSwitch('toggleState', true)
			          }
			        }.bind(this)
			    });
    		} else {
    			var title = "Do you really want to stop debugging this topology ?";
		    	var successCb = function(){
		    		this.model.debugTopology({
		    			id: this.model.get('id'),
		    			debugType: 'disable',
		    			percent: '0',
		    			success: function(model, response){
		    				if(response.error){
								Utils.notifyError(response.error);
							} else {
								this.initializeData();
		    					Utils.notifySuccess("Debugging disabled successfully.")
							}
		    			}.bind(this),
						error: function(model, response, options){
							Utils.notifyError("Error occured in disabling debugging.");
						}
		    		});
		    	}.bind(this);
		    	var cancelCb = function(){
		    		$(".boot-switch.debug").bootstrapSwitch('toggleState', true)
		    	}.bind(this);
		    	Utils.ConfirmDialog('&nbsp;', title, successCb, cancelCb);
    		}
	    },
	});
});