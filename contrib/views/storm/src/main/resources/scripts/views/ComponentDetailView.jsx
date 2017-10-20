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
	'react',
	'react-dom',
	'collections/BaseCollection',
	'models/VTopology',
	'jsx!components/Breadcrumbs',
	'jsx!components/SearchLogs',
	'jsx!views/ProfilingView',
	'utils/Utils',
	'bootbox',
	'bootstrap',
	'bootstrap-switch'
	],function(Table, Pagination, React, ReactDOM, BaseCollection, VTopology, Breadcrumbs, SearchLogs, ProfilingView, Utils, bootbox){
	'use strict';

	return React.createClass({
		displayName: 'ComponentDetailView',
		propTypes: {
			id: React.PropTypes.string.isRequired,
			name: React.PropTypes.string.isRequired
		},
		getInitialState: function(){
			this.model = new VTopology({'id': this.props.id});
			this.systemFlag = (this.props.name.startsWith('__')) ? true : false;
			this.windowSize = ':all-time';
			this.initializeData();
			return {
				componentObj: {},
				profilingModalOpen: false
			};
		},
		componentWillMount: function(){
			$('.loader').show();
		},
		componentWillUpdate: function(){
			$('.loader').show();
			$('#collapse-input').off('hidden.bs.collapse').off('shown.bs.collapse');
			$('#collapse-output').off('hidden.bs.collapse').off('shown.bs.collapse');
			$('#collapse-executor').off('hidden.bs.collapse').off('shown.bs.collapse');
			$('#collapse-error').off('hidden.bs.collapse').off('shown.bs.collapse');
		},
		componentDidMount: function(){
			$(".boot-switch.systemSum").bootstrapSwitch({
				size: 'small',
				onSwitchChange: function(event, state){
					this.systemFlag = state;
					this.initializeData();
				}.bind(this)
			});

			$(".boot-switch.debug").bootstrapSwitch({
				size: 'small',
				onSwitchChange: function(event, state){
					this.debugAction(state);
				}.bind(this)
			});
			$('.loader').hide();
		},
		componentDidUpdate: function(){
			$('#collapse-input').on('hidden.bs.collapse', function () {
				$("#input-box").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#input-box").toggleClass("fa-compress fa-expand");
			});

			$('#collapse-output').on('hidden.bs.collapse', function () {
				$("#output-box").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#output-box").toggleClass("fa-compress fa-expand");
			});

			$('#collapse-executor').on('hidden.bs.collapse', function () {
				$("#executor-box").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#executor-box").toggleClass("fa-compress fa-expand");
			});

			$('#collapse-error').on('hidden.bs.collapse', function () {
				$("#error-box").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#error-box").toggleClass("fa-compress fa-expand");
			});
			$('#modal-profiling').on('hidden.bs.modal', function (e) {
			  this.initializeData();
			  this.setState({"profilingModalOpen":false});
			}.bind(this));
			if(this.state.profilingModalOpen){
				$('#modal-profiling').modal("show");
			}
			$('.loader').hide();
		},
		initializeData: function(){
			this.model.getComponent({
				id: this.props.id,
				name: this.props.name,
				window: this.windowSize,
				sys: this.systemFlag,
				success: function(model, response){
					if(response.error || model.error){
						Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
					} else {
						this.setState({"componentObj": model});
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching topology component data.");
				}
			});
		},
		renderWindowOptions: function(){
			var arr = this.state.componentObj.spoutSummary || this.state.componentObj.boltStats;
			if(arr){
				return arr.map(function(object, i){
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
				{link: '#!/topology/'+this.state.componentObj.topologyId, title: this.state.componentObj.name || ""},
				{link: 'javascript:void(0);', title: this.state.componentObj.id || ""}
				];
			return links;
		},
		renderStatsRow: function(){
			var spoutFlag = (this.state.componentObj.componentType === 'spout' ? true: false);
			var statsArr = this.state.componentObj.spoutSummary || this.state.componentObj.boltStats;
			if(statsArr){
				return statsArr.map(function(stats, i){
					return (
						<tr key={i}>
							<td>{stats.windowPretty}</td>
							<td>{stats.emitted}</td>
							<td>{stats.transferred}</td>
							{spoutFlag ? <td>{stats.completeLatency}</td> : null}
							{!spoutFlag ? <td>{stats.executeLatency}</td> : null}
							{!spoutFlag ? <td>{stats.executed}</td> : null}
							{!spoutFlag ? <td>{stats.processLatency}</td> : null}
							<td>{stats.acked}</td>
							<td>{stats.failed}</td>
						</tr>
					);
				});
			}
		},
		renderAccordion: function(type, header, searchField, searchCb, collection, emptyText, columns, toggleCb){
			return ( 
				<div className="box">
					<div className="box-header" data-toggle="collapse" data-target={"#collapse-"+type} aria-expanded="false" aria-controls={"collapse-"+type}>
						<h4>{header} ( {this.state.componentObj.windowHint} )</h4>
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
						{type === 'error' ? <Pagination collection={collection} /> : null}
		            </div>
				</div>
			);
		},
		renderInputStats: function(){
			var inputCollection = Utils.ArrayToCollection(this.state.componentObj.inputStats, new BaseCollection());
			inputCollection.searchFields = ['component'];
			var searchCb = function(e){
				var value = e.currentTarget.value;
				inputCollection.search(value);
			};
			var toggleCb = function(e){
				$("#collapse-input").collapse('toggle');
			}
			return this.renderAccordion('input', 'Input Stats', 'component', searchCb, inputCollection, 'No input stats found !', this.getInputColumns, toggleCb);
		},
		getInputColumns: function(){
			return [
				{name: 'component', title: 'Component', tooltip: 'The ID assigned to a the Component by the Topology.'},
				{name: 'stream', title: 'Stream', tooltip: 'The name of the Tuple stream given in the Topolgy, or "default" if none was given.'},
				{name: 'executeLatency', title: 'Execute Latency (ms)', tooltip: 'The average time a Tuple spends in the execute method. The execute method may complete without sending an Ack for the tuple.'},
				{name: 'executed', title: 'Executed', tooltip: 'The number of incoming Tuples processed.'},
				{name: 'processLatency', title: 'Process Latency (ms)', tooltip: 'The average time it takes to Ack a Tuple after it is first received.  Bolts that join, aggregate or batch may not Ack a tuple until a number of other Tuples have been received.'},
				{name: 'acked', title: 'Acked', tooltip: 'The number of Tuples acknowledged by this Bolt.'},
				{name: 'failed', title: 'Failed', tooltip: 'The number of tuples Failed by this Bolt.'}
			];
		},
		renderOutputStats: function(){
			var outputCollection = Utils.ArrayToCollection(this.state.componentObj.outputStats, new BaseCollection());
			outputCollection.searchFields = ['stream'];
			var searchCb = function(e){
				var value = e.currentTarget.value;
				outputCollection.search(value);
			};
			var toggleCb = function(e){
				$("#collapse-output").collapse('toggle');
			}
			return this.renderAccordion('output', 'Output Stats', 'stream', searchCb, outputCollection, 'No output stats found !', this.getOutputColumns, toggleCb);
		},
		getOutputColumns: function(){
			if(this.state.componentObj.componentType === 'spout'){
				return [
					{name: 'stream', title: 'Stream', tooltip: 'The name of the Tuple stream given in the Topolgy, or "default" if none was given.'},
					{name: 'emitted', title: 'Emitted', tooltip: 'The number of Tuples emitted.'},
					{name: 'transferred', title: 'Transferred', tooltip: 'The number of Tuples emitted that sent to one or more bolts.'},
					{name: 'completeLatency', title: 'Complete Latency (ms)', tooltip: 'The average time a Tuple "tree" takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.'},
					{name: 'acked', title: 'Acked', tooltip: 'The number of Tuple "trees" successfully processed. A value of 0 is expected if no acking is done.'},
					{name: 'failed', title: 'Failed', tooltip: 'The number of Tuple "trees" that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.'}
				];
			} else {
				return [
					{name: 'stream', title: 'Stream', tooltip: 'The name of the Tuple stream given in the Topolgy, or "default" if none was given.'},
					{name: 'emitted', title: 'Emitted', tooltip: 'The number of Tuples emitted.'},
					{name: 'transferred', title: 'Transferred', tooltip: 'The number of Tuples emitted that sent to one or more bolts.'}
				];
			}
		},
		renderExecutorStats: function(){
			var executorCollection = Utils.ArrayToCollection(this.state.componentObj.executorStats, new BaseCollection());
			executorCollection.searchFields = ['id'];
			var searchCb = function(e){
				var value = e.currentTarget.value;
				executorCollection.search(value);
			};
			var toggleCb = function(e){
				$("#collapse-executor").collapse('toggle');
			}
			return this.renderAccordion('executor', 'Executor Stats', 'id', searchCb, executorCollection, 'No executor stats found !', this.getExecutorColumns, toggleCb);
		},
		getExecutorColumns: function(){
			var self = this;
			if(this.state.componentObj.componentType === 'spout'){
				return [
					{name: 'id', title: 'Id', tooltip: 'The unique executor ID.'},
					{name: 'uptime', title: 'Uptime', tooltip: 'The length of time an Executor (thread) has been alive.'},
					{name: 'port', title: 'Host:Port', tooltip: 'The hostname reported by the remote host. (Note that this hostname is not the result of a reverse lookup at the Nimbus node.) Click on it to open the logviewer page for this Worker.', component: React.createClass({
						propTypes: {
							model: React.PropTypes.object.isRequired
						},
						render: function(){
							return ( <a href={this.props.model.get('workerLogLink')} target="_blank"> {this.props.model.get('host')}:{this.props.model.get('port')} </a>);
						}
					})},
					{name: 'emitted', title: 'Emitted', tooltip: 'The number of Tuples emitted.'},
					{name: 'transferred', title: 'Transferred', tooltip: 'The number of Tuples emitted that sent to one or more bolts.'},
					{name: 'completeLatency', title: 'Complete Latency (ms)', tooltip: 'The average time a Tuple "tree" takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.'},
					{name: 'acked', title: 'Acked', tooltip: 'The number of Tuple "trees" successfully processed. A value of 0 is expected if no acking is done.'},
					{name: 'failed', title: 'Failed', tooltip: 'The number of Tuple "trees" that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.'},
					{name: 'workerLogLink', title: 'Dumps', component: React.createClass({
						propTypes: {
							model: React.PropTypes.object.isRequired
						},
						render: function(){
							var link = this.props.model.get('workerLogLink');
							link = ""+link.split('/log')[0]+"/dumps/"+self.props.id+"/"+this.props.model.get('host')+":"+this.props.model.get('port');
							return (<a href={link} className="btn btn-primary btn-xs" target="_blank"><i className="fa fa-file-text"></i></a>);
						}
					})}
				];
			} else {
				return [
					{name: 'id', title: 'Id', tooltip: 'The unique executor ID.'},
					{name: 'uptime', title: 'Uptime', tooltip: 'The length of time an Executor (thread) has been alive.'},
					{name: 'port', title: 'Host:Port', tooltip: 'The hostname reported by the remote host. (Note that this hostname is not the result of a reverse lookup at the Nimbus node.) Click on it to open the logviewer page for this Worker.', component: React.createClass({
						propTypes: {
							model: React.PropTypes.object.isRequired
					    },
						render: function(){
							return ( <a href={this.props.model.get('workerLogLink')} target="_blank"> {this.props.model.get('host')}:{this.props.model.get('port')} </a>);
						}
					})},
					{name: 'emitted', title: 'Emitted', tooltip: 'The number of Tuples emitted.'},
					{name: 'transferred', title: 'Transferred', tooltip: 'The number of Tuples emitted that sent to one or more bolts.'},
					{name: 'capacity', title: 'Capacity (last 10m)', tooltip: "If this is around 1.0, the corresponding Bolt is running as fast as it can, so you may want to increase the Bolt's parallelism. This is (number executed * average execute latency) / measurement time."},
					{name: 'executeLatency', title: 'Execute Latency (ms)', tooltip: 'The average time a Tuple spends in the execute method. The execute method may complete without sending an Ack for the tuple.'},
					{name: 'executed', title: 'Executed', tooltip: 'The number of incoming Tuples processed.'},
					{name: 'processLatency', title: 'Process Latency (ms)', tooltip: 'The average time it takes to Ack a Tuple after it is first received.  Bolts that join, aggregate or batch may not Ack a tuple until a number of other Tuples have been received.'},
					{name: 'acked', title: 'Acked', tooltip: 'The number of Tuples acknowledged by this Bolt.'},
					{name: 'failed', title: 'Failed', tooltip: 'The number of tuples Failed by this Bolt.'},
					{name: 'workerLogLink', title: 'Dumps', component: React.createClass({
						propTypes: {
							model: React.PropTypes.object.isRequired
						},
						render: function(){
							var link = this.props.model.get('workerLogLink');
							link = ""+link.split('/log')[0]+"/dumps/"+self.props.id+"/"+this.props.model.get('host')+":"+this.props.model.get('port');
							return (<a href={link} className="btn btn-primary btn-xs" target="_blank"><i className="fa fa-file-text"></i></a>);
						}
					})}
				];
			}
		},
		renderErrorStats: function(){
			var errorCollection = Utils.ArrayToCollection(this.state.componentObj.componentErrors, new BaseCollection());
			errorCollection.searchFields = ['error'];
			var searchCb = function(e){
				var value = e.currentTarget.value;
				errorCollection.search(value);
			};
			var toggleCb = function(e){
				$("#collapse-error").collapse('toggle');
			}
			return this.renderAccordion('error', 'Error Stats', 'error', searchCb, errorCollection, 'No errors found !', this.getErrorColumns, toggleCb);
		},
		getErrorColumns: function(){
			return [
				{name: 'errorTime', title: 'Time', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						if(this.props.model.get('errorTime') && this.props.model.get('errorTime') != 0) {
							var d = new Date(this.props.model.get('errorTime') * 1000),
							date = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
							return (<span>{date}</span>);
						} else return (<span></span>);
					}
				})},
				{name: 'errorPort', title: 'Host:Port', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						return ( <a href={this.props.model.get('errorWorkerLogLink')} target="_blank"> {this.props.model.get('errorHost')}:{this.props.model.get('errorPort')} </a>);
					}
				})},
				{name: 'error', title: 'Error'}
			];
		},
		render: function() {
			if(this.state.componentObj.debug){
				$(".boot-switch.debug").bootstrapSwitch('state', true, true);
			} else {
				$(".boot-switch.debug").bootstrapSwitch('state', false, true);
			}
			if(this.systemFlag){
				$(".boot-switch.systemSum").bootstrapSwitch('state', true, true);
			} else {
				$(".boot-switch.systemSum").bootstrapSwitch('state', false, true);
			}
			var spoutFlag = (this.state.componentObj.componentType === 'spout' ? true: false);
			return (
				<div>					
					<Breadcrumbs links={this.getLinks()} />
					<SearchLogs id={this.state.componentObj.topologyId}/>
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
												<button type="button" className="btn btn-primary" onClick={this.handleProfiling} title="Profiling & Debugging" data-rel="tooltip">
													<i className="fa fa-cogs"></i>
												</button>
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-4">
							<div className="summary-tile">
								<div className="summary-title">Component Summary</div>
								<div className="summary-body">
									<p><strong>ID: </strong>{this.state.componentObj.id}</p>
									<p><strong>Topology: </strong>{this.state.componentObj.name}</p>
									<p><strong>Executors: </strong>{this.state.componentObj.executors}</p>
									<p><strong>Tasks: </strong>{this.state.componentObj.tasks}</p>
									<p><strong>Debug: </strong><a href={this.state.componentObj.eventLogLink} target="_blank">events</a></p>
								</div>
							</div>
						</div>
						<div className="col-sm-8">
							<div className="stats-tile">
								<div className="stats-title">{spoutFlag ? "Spout Stats" : "Bolt Stats"}</div>
								<div className="stats-body">
									<table className="table table-condensed no-margin">
										<thead>
											<tr>
												<th><span data-rel="tooltip" title="The past period of time for which the statistics apply.">Window</span></th>
												<th><span data-rel="tooltip" title="The number of Tuples emitted.">Emitted</span></th>
												<th><span data-rel="tooltip" title="The number of Tuples emitted that sent to one or more bolts.">Transferred</span></th>
												{spoutFlag ? <th><span data-rel="tooltip" title='The average time a Tuple "tree" takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.'>Complete Latency (ms)</span></th> : null}
												{!spoutFlag ? <th><span data-rel="tooltip" title="The average time a Tuple spends in the execute method. The execute method may complete without sending an Ack for the tuple.">Execute Latency (ms)</span></th> : null}
												{!spoutFlag ? <th><span data-rel="tooltip" title="The number of incoming Tuples processed.">Executed</span></th> : null}
												{!spoutFlag ? <th><span data-rel="tooltip" title="The average time it takes to Ack a Tuple after it is first received.  Bolts that join, aggregate or batch may not Ack a tuple until a number of other Tuples have been received.">Process Latency (ms)</span></th> : null}
												<th><span data-rel="tooltip" title={spoutFlag ? 'The number of Tuple "trees" successfully processed. A value of 0 is expected if no acking is done.' : "The number of Tuples acknowledged by this Bolt."}>Acked</span></th>
												<th><span data-rel="tooltip" title={spoutFlag ? 'The number of Tuple "trees" that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.' : "The number of tuples Failed by this Bolt."}>Failed</span></th>
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
							{this.state.componentObj.inputStats ? this.renderInputStats() : null}
							{this.state.componentObj.outputStats ? this.renderOutputStats() : null}
							{this.state.componentObj.executorStats ? this.renderExecutorStats() : null}
							{this.state.componentObj.componentErrors ? this.renderErrorStats() : null}
						</div>
					</div>
					{this.state.profilingModalOpen ? <ProfilingView modalId="modal-profiling" topologyId={this.props.id} executorStats={this.state.componentObj.executorStats} /> : null}
				</div>
			);
	    },
	    handleProfiling: function(){
	    	this.setState({"profilingModalOpen":true});
	    },
	    debugAction: function(toEnableFlag){
    		if(toEnableFlag){
    			bootbox.prompt({
			        title: 'Do you really want to debug this component ? If yes, please, specify sampling percentage.',
			        value: this.state.componentObj.samplingPct ? this.state.componentObj.samplingPct : "10",
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
					  if(result == null) {
						$(".boot-switch.debug").bootstrapSwitch('toggleState', true);
			          } else if(result == "" || isNaN(result) || result < 0) {
						Utils.notifyError("Enter valid sampling percentage");
						$(".boot-switch.debug").bootstrapSwitch('toggleState', true)
			          } else {
			            this.model.debugComponent({
			    			id: this.state.componentObj.topologyId,
			    			name: this.state.componentObj.id,
			    			debugType: 'enable',
			    			percent: result,
			    			success: function(model, response){
			    				if(response.error || model.error){
									Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
								} else {
									this.initializeData();
			    					Utils.notifySuccess("Debugging enabled successfully.");
								}
			    			}.bind(this),
							error: function(model, response, options){
								Utils.notifyError("Error occured in enabling debugging.");
							}
			    		});
			          }
			        }.bind(this)
			    });
    		} else {
    			var title = "Do you really want to stop debugging this component ?";
		    	var successCb = function(){
		    		this.model.debugComponent({
		    			id: this.state.componentObj.topologyId,
		    			name: this.state.componentObj.id,
		    			debugType: 'disable',
		    			percent: '0',
		    			success: function(model, response){
		    				if(response.error || model.error){
								Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
							} else {
								this.initializeData();
		    					Utils.notifySuccess("Debugging disabled successfully.");
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