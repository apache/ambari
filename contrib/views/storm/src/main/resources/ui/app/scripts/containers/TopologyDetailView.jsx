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
**/

import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from './BaseContainer';
import SearchLogs from '../components/SearchLogs';
import TopologyREST from '../rest/TopologyREST';
import {Accordion, Panel,OverlayTrigger, Tooltip} from 'react-bootstrap';
import TopologyGraph from '../components/TopologyGraph';
import {
  Table,
  Thead,
  Th,
  Tr,
  Td,
  unsafe
} from 'reactable';
import CommonPagination from '../components/CommonPagination';
import {Link} from 'react-router';
import {toastOpt,pageSize} from '../utils/Constants';
import Utils from '../utils/Utils';
import FSReactToastr from '../components/FSReactToastr';
import CommonNotification from '../components/CommonNotification';
import Breadcrumbs from '../components/Breadcrumbs';
import Modal from '../components/FSModel';
import CommonWindowPanel from '../components/CommonWindowPanel';
import RebalanceTopology from '../components/RebalanceTopology';
import LogLevelComponent  from '../components/LogLevelComponent';
import CommonSwitchComponent from '../components/CommonSwitchComponent';
import BarChart from '../components/BarChart';
import CommonExpanded from '../components/CommonExpanded';

export default class TopologyDetailView extends Component {
  constructor(props){
    super(props);
    this.state = {
      details: {},
      spotActivePage : 1,
      boltsActivePage : 1,
      topologyActivePage : 1,
      spotFilterValue : '',
      blotFilterValue : '',
      topologyFilterValue : '',
      selectedWindowKey : {label : 'All time' , value : ':all-time'},
      windowOptions : [],
      systemFlag : false,
      killWaitTime : 30,
      showLogLevel : false,
      topologyLagFlag : true,
      topologyLagPage : 1,
      toggleGraphAndTable: true,
      expandGraph : true,
      expandSpout : true,
      expandBolt : true,
      expandConfig : false,
      topologyLag : [],
      debugFlag : false
    };
    this.fetchDetails();
  }
  fetchDetails(){
    const {selectedWindowKey,systemFlag} = this.state;
    let promiseArr=[
      TopologyREST.getTopologyDetails(this.props.params.id,selectedWindowKey.value,systemFlag),
      TopologyREST.getTopologyGraphData(this.props.params.id,selectedWindowKey.value),
      TopologyREST.getTopologyLag(this.props.params.id)
    ];

    Promise.all(promiseArr).then((results) => {
      _.map(results, (result) => {
        if(result.errorMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
        }
      });

      let stateObj = {};
      stateObj.details = results[0];
      stateObj.windowOptions = Utils.populateWindowsOptions(stateObj.details.topologyStats);
      if(stateObj.windowOptions.length === 0){
        stateObj.windowOptions = [{label : 'All time', value : ':all-time'}];
      }
      stateObj.debugSimplePCT = stateObj.details.samplingPct;
      stateObj.selectedWindowKey = {label : stateObj.details.windowHint || 'All time', value : stateObj.details.window || ':all-time'};
      stateObj.graphData = results[1];
      stateObj.topologyLag = _.isEmpty(results[2]) ? [] : this.generateTopologyLagData(results[2]);
      stateObj.debugFlag = stateObj.details.debug;
      this.setState(stateObj);
    });
  }

  generateTopologyLagData = (lagObj) => {
    const objKey = _.keys(lagObj);
    let arr = [];
    _.map(objKey, (o) => {
      let data = lagObj[o];
      const topicKeys = _.keys(data.spoutLagResult);
      _.map(topicKeys, (t) => {
        const topicName = t;
        const partitionData = data.spoutLagResult[t];
        const partitionKey = _.keys(partitionData);
        _.map(partitionKey, (pk) => {
          let obj = partitionData[pk];
          obj['spoutId'] = data.spoutId;
          obj['spoutType'] = data.spoutType;
          obj['partition'] = pk;
          obj['topic'] = topicName;
          arr.push(obj);
        });
      });
    });
    return arr;
  }

  componentDidUpdate(){
    if(this.refs.barChart){
      ReactDOM.findDOMNode(document.getElementById('lag-graph')).appendChild(this.refs.barChart.legendsEl);
    }
  }

  handleWindowChange = (obj) => {
    if(!_.isEmpty(obj)){
      this.setState({selectedWindowKey : obj}, () => {
        this.fetchDetails();
      });
    }
  }

  getWorkerData = () => {
    const {details} = this.state;
    let data='';
    _.map(details.workers,(worker,i) => {
      data += worker.host+':'+worker.port;
      if(i !== details.workers.length - 1){
        data += ', \n';
      }
    });
    return data;
  }

  getDateFormat = (d) => {
    let obj = new Date(d * 1000);
    return <span>{obj.toLocaleDateString() + ' ' + obj.toLocaleTimeString()}</span>;
  }

  handleFilter = (section,e) => {
    switch(section){
    case 'spout' : this.setState({spotFilterValue :  e.target.value.trim()});
      break;
    case 'bolt' : this.setState({blotFilterValue :  e.target.value.trim()});
      break;
    case 'topologyConfig' : this.setState({topologyFilterValue :  e.target.value.trim()});
      break;
    default :
      break;
    };
  }

  callBackFunction = (eventKey,tableName) => {
    switch(tableName){
    case 'spout' : this.setState({spotActivePage : eventKey});
      break;
    case 'bolt' : this.setState({boltsActivePage : eventKey});
      break;
    case 'topologyConfig' : this.setState({topologyActivePage : eventKey});
      break;
    case 'topologyLag' : this.setState({topologyLagPage : eventKey});
      break;
    default :
      break;
    };
  }

  getLinks(){
    const {details} = this.state;
    var links = [
      {link: '#/', title: 'Dashboard'},
      {link: '#/topology', title: 'Topology Listing'},
      {link: 'javascript:void(0);', title: details.name? details.name : ""}
    ];
    return links;
  }

  toggleSystem = (toggleStatus) => {
    let stateObj = _.cloneDeep(this.state);
    stateObj[toggleStatus] = !stateObj[toggleStatus];
    this.setState(stateObj,() => {
      if(toggleStatus === 'debugFlag'){
        !stateObj.debugFlag ? this.debugEnableConfirmBox(stateObj.debugFlag,'debugModelRef') : this.refs.debugModelRef.show();
      } else {
        this.fetchDetails();
      }
    });
  }

  handleModelAction = (modalType,action) => {
    if(action === 'save'){
      switch(modalType){
      case 'debugModelRef' : this.handleDebugSave(modalType,'enable');;
        break;
      case 'rebalanceModelRef' : this.handleRebalanceModalSave(modalType);
        break;
      case 'killModelRef' : this.handleTopologyKilled(modalType);
        break;
      default :
        break;
      }
    } else{
      switch(modalType){
      case 'debugModelRef' : Utils.hideFSModal.call(this,modalType,'callBack').then((res) => {
        this.setState({debugFlag : !this.state.debugFlag});
      });
        break;
      default : Utils.hideFSModal.call(this,modalType);
        break;
      }
    }
  }

  inputTextChange = (type,e) => {
    let stateObj = _.cloneDeep(this.state);
    stateObj[type] = e.target.value;
    this.setState(stateObj);
  }

  debugEnableConfirmBox = (confirm,modalType) => {
    if(!confirm){
      this.refs.BaseContainer.refs.Confirm.show({title: 'Do you really want to stop debugging this topology ?"'}).then((confirmBox) => {
        this.setState({debugSimplePCT : 0}, () => {
          this.handleDebugSave(modalType,'disable');
          confirmBox.cancel();
        });
      }, () => {
        this.setState({debugFlag : true});
      });
    }
  }

  handleDebugSave = (modal,toEnableFlag) => {
    const {debugSimplePCT,details} = this.state;
    Utils.hideFSModal.call(this,modal);
    TopologyREST.postDebugTopology(details.id,toEnableFlag,debugSimplePCT).then((result) => {
      if(result.errorMessage !== undefined){
        this.setState({debugSimplePCT : details.samplingPct});
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
      } else {
        FSReactToastr.success(<strong>Debugging enabled successfully.</strong>);
      }
    });
  }

  handleRebalanceModalSave = (modalType) => {
    if(this.refs.rebalanceModal.validateData()){
      Utils.hideFSModal.call(this,modalType);
      this.refs.rebalanceModal.handleSave().then((result) => {
        if(result.errorMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
        } else {
          this.fetchDetails();
          clearTimeout(this.clearTimeOut);
          this.clearTimeOut =  setTimeout(function () {
            FSReactToastr.success(<strong>Topology rebalanced successfully.</strong>);
          },300);
        }
      });
    }
  }

  handleTopologyAction = (action) => {
    if(action === 'activate' || action === 'deactivate'){
      this.handleTopologyActiveAndDeactive(action);
    } else if(action === 'rebalance'){
      this.refs.rebalanceModelRef.show();
    } else if (action === "kill"){
      this.refs.killModelRef.show();
    }
  }

  handleTopologyKilled = (modalType) => {
    const {killWaitTime,details} = this.state;
    Utils.hideFSModal.call(this,modalType);
    TopologyREST.postActionOnTopology(details.id,'kill',killWaitTime).then((result) => {
      if(result.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
      } else {
        clearTimeout(this.clearTimeOutKill);
        this.clearTimeOutKill =  setTimeout(function () {
          FSReactToastr.success(<strong>"Topology killed successfully."</strong>);
        },300);
      }
    });
  }

  handleTopologyActiveAndDeactive = (action) => {
    this.refs.BaseContainer.refs.Confirm.show({title: "Do you really want to "+action+" this topology ?"}).then((confirmBox) => {
      const {details} = this.state;
      TopologyREST.postActionOnTopology(details.id,action).then((result) => {
        if(result.errorMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(<strong>{"Topology "+action+"d successfully."}</strong>);
        }
      });
      confirmBox.cancel();
    }, () => {});
  }

  handleLogLevel = () => {
    this.setState({showLogLevel : !this.state.showLogLevel});
  }

  toggleKafkaLag = (action,event) => {
    event.stopPropagation();
    this.setState({toggleGraphAndTable : !this.state.toggleGraphAndTable});
  }

  lagAccodianClick = () => {
    this.setState({topologyLagFlag : !this.state.topologyLagFlag});
  }

  commonOnSelectFunction = (type) => {
    let tempState = _.cloneDeep(this.state);
    tempState[type] = !tempState[type];
    this.setState(tempState);
  }

  populateLagGraphData = (data) => {
    let graphArr=[];
    _.map(data, (t) => {
      graphArr.push({
        'Latest Offset': t.logHeadOffset,
        'Spout Committed Offset': t.consumerCommittedOffset,
        'spoutId-partition': t.spoutId+'-'+t.partition
      });
    });
    return graphArr;
  }

  render() {
    const {details,spotActivePage,boltsActivePage,topologyActivePage,spotFilterValue,blotFilterValue,topologyFilterValue,
      graphData,selectedWindowKey,windowOptions,systemFlag,debugFlag,debugSimplePCT,killWaitTime,showLogLevel,
      topologyLagFlag,topologyLagPage,topologyLag,toggleGraphAndTable,expandGraph,expandSpout,expandBolt,expandConfig,onEntervalue} = this.state;
    const spoutfilteredEntities = Utils.filterByKey(details.spouts || [], spotFilterValue,'spoutId');
    const blotfilteredEntities = Utils.filterByKey(details.bolts || [], blotFilterValue,'boltId');
    const topologyfilteredEntities = Utils.filterByKey(_.keys(details.configuration) || [], topologyFilterValue);
    const spotPaginationObj = {
      activePage :spotActivePage,
      pageSize,
      filteredEntities : spoutfilteredEntities
    };
    const boltPaginationObj = {
      activePage :boltsActivePage,
      pageSize,
      filteredEntities : blotfilteredEntities
    };
    const topologyPaginationObj = {
      activePage :topologyActivePage,
      pageSize,
      filteredEntities : topologyfilteredEntities
    };
    const graphDataObj = _.isEmpty(graphData) && graphData === undefined ? {} : graphData;
    const topologyStatus = details !== undefined ? details.status : '';
    const lagPanelHeader = <h4>
      Kafka Spout Lag
      <CommonSwitchComponent KYC="kafka" checked={toggleGraphAndTable} textON="Table" textOFF="Graph" switchCallBack={this.toggleKafkaLag.bind(this,'kafkaSpoutLag')} />
    </h4>;

    const graphPanelHead = <h4> {details.name}
                              <CommonExpanded  expandFlag={expandGraph}/></h4>;

    const spoutPanelHead = <h4> Spouts
                            <CommonExpanded  expandFlag={expandSpout}/></h4>;

    const boltPanelHead = <h4> Bolts
                            <CommonExpanded  expandFlag={expandBolt}/></h4>;

    const configPanelHead = <h4> Topology Configuration
                              <CommonExpanded  expandFlag={expandConfig}/></h4>;

    return (
    <BaseContainer ref="BaseContainer">
      <Breadcrumbs links={this.getLinks()} />
      <SearchLogs
        id={this.props.params.id}
      />
      <div className="row">
        <div className="col-sm-12">
          <div className="box filter">
            <div className="box-body form-horizontal">
              <CommonWindowPanel KYC="detailView" selectedWindowKey={selectedWindowKey} windowOptions={windowOptions} status={topologyStatus} systemFlag={systemFlag} debugFlag={debugFlag} handleWindowChange={this.handleWindowChange.bind(this)} toggleSystem={this.toggleSystem.bind(this)} handleTopologyAction={this.handleTopologyAction.bind(this)} handleLogLevel={this.handleLogLevel.bind(this)} topologyStatus={topologyStatus}/>
              {
                showLogLevel
                ? <LogLevelComponent topologyId={details.id}/>
                : ''
              }
            </div>
          </div>
        </div>
      </div>
      <div className="row">
        <div className="col-sm-5">
          <div className="summary-tile">
            <div className="summary-title">Topology Summary</div>
            <div className="summary-body form-horizontal">
              <div className="form-group">
                <label className="col-sm-4 control-label">ID:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.id}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Owner:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.owner}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Status:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.status}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Uptime:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.uptime}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Workers:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.workersTotal}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Executors:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.executorsTotal}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Tasks:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.tasksTotal}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Memory:</label>
                <div className="col-sm-8">
                  <p className="form-control-static">{details.assignedTotalMem}</p>
                </div>
              </div>
              <div className="form-group">
                <label className="col-sm-4 control-label">Worker-Host:Port:</label>
                <div className="col-sm-8">
                  <p className="form-control-static preformatted">{this.getWorkerData()}</p>
                </div>
              </div>

            </div>
          </div>
        </div>
        <div className="col-sm-7">
          <div className="stats-tile">
            <div className="stats-title">Topology Stats</div>
            <div className="stats-body">
              <Table className="table table-enlarge" noDataText="No records found." currentPage={0} >
                <Thead>
                  <Th column="windowPretty">
                    <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">The past period of time for which the statistics apply.</Tooltip>}>
                      <span>Window</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="emitted">
                    <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">The number of Tuples emitted.</Tooltip>}>
                       <span>Emitted</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="transferred">
                    <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">The number of Tuples emitted that sent to one or more bolts.</Tooltip>}>
                       <span>Transferred</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="completeLatency">
                    <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">The average time a Tuple tree takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.</Tooltip>}>
                       <span>Complete Latency (ms)</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="acked">
                    <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">The number of Tuple trees successfully processed. A value of 0 is expected if no acking is done.</Tooltip>}>
                       <span>Acked</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="failed">
                    <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">The number of Tuple trees that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.</Tooltip>}>
                       <span>Failed</span>
                    </OverlayTrigger>
                  </Th>
                </Thead>
                {
                  _.map(details.topologyStats,(s,i) => {
                    return(
                      <Tr key={i}>
                        <Td column="windowPretty">{s.windowPretty}</Td>
                        <Td column="emitted">{s.emitted}</Td>
                        <Td column="transferred">{s.transferred}</Td>
                        <Td column="completeLatency">{s.completeLatency}</Td>
                        <Td column="acked">{s.acked}</Td>
                        <Td column="failed">{s.failed}</Td>
                      </Tr>
                    );
                  })
                }
              </Table>
            </div>
          </div>
        </div>
      </div>
      <Panel expanded={expandGraph} collapsible header={graphPanelHead} eventKey="1"  onSelect={this.commonOnSelectFunction.bind(this,'expandGraph')}>
        <div className="graph-bg">
          <TopologyGraph
            data={graphDataObj}
          />
        </div>
      </Panel>
      {
        topologyLag.length
        ? <Panel expanded={true} collapsible header={lagPanelHeader} eventKey="2" onSelect={this.lagAccodianClick.bind(this)}>
          {
            toggleGraphAndTable
            ? <Table className="table table-striped table-bordered"  noDataText="No data found !"  currentPage={topologyLagPage-1} itemsPerPage={pageSize}>
                <Thead>
                  <Th column="spoutId">
                    <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Id</Tooltip>}>
                       <span>Id</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="topic">
                    <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Topic</Tooltip>}>
                       <span>Topic</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="partition">
                    <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Partition</Tooltip>}>
                       <span>Partition</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="logHeadOffset">
                    <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Latest Offset</Tooltip>}>
                       <span>Latest Offset</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="consumerCommittedOffset">
                    <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Spout Committed Offset</Tooltip>}>
                       <span>Spout Committed Offset</span>
                    </OverlayTrigger>
                  </Th>
                  <Th column="lag">
                    <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Lag</Tooltip>}>
                       <span>Lag</span>
                    </OverlayTrigger>
                  </Th>
                </Thead>
                {
                  _.map(topologyLag , (l, i) => {
                    return <Tr key={i}>
                      <Td column="spoutId">{l.spoutId}</Td>
                      <Td column="topic">{l.topic}</Td>
                      <Td column="partition">{l.partition}</Td>
                      <Td column="logHeadOffset">{l.logHeadOffset}</Td>
                      <Td column="consumerCommittedOffset">{l.consumerCommittedOffset}</Td>
                      <Td column="lag">{l.lag}</Td>
                    </Tr>;
                  })
                }
              </Table>
            : <div id="lag-graph">
                <BarChart
                  ref="barChart"
                  width={window != window.parent ? 1100 : 1300}
                  height={400}
                  xAttr="spoutId-partition"
                  yAttr="count"
                  data={this.populateLagGraphData(topologyLag)}
                />
            </div>
          }


          </Panel>
        : null
      }
      <Panel expanded={expandSpout} collapsible header={spoutPanelHead} eventKey="3" onSelect={this.commonOnSelectFunction.bind(this,'expandSpout')}>
        <div className="input-group col-sm-4">
          <input type="text"  onKeyUp={this.handleFilter.bind(this,'spout')} className="form-control" placeholder="Search By Id" />
          <span className="input-group-btn">
          <button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
          </span>
        </div>
        <div className="table-responsive">
        <Table className="table no-margin"  noDataText="No spouts found !"  currentPage={spotActivePage-1} itemsPerPage={pageSize}>
          <Thead>
            <Th column="spoutId">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The ID assigned to a the Component by the Topology. Click on the name to view the Component's page.</Tooltip>}>
                 <span>Id</span>
              </OverlayTrigger>
            </Th>
            <Th column="executors">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Executors are threads in a Worker process.</Tooltip>}>
                 <span>Executors</span>
              </OverlayTrigger>
            </Th>
            <Th column="tasks">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.</Tooltip>}>
                 <span>Tasks</span>
              </OverlayTrigger>
            </Th>
            <Th column="emitted">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuples emitted.</Tooltip>}>
                 <span>Emitted</span>
              </OverlayTrigger>
            </Th>
            <Th column="transferred">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuples emitted that sent to one or more bolts.</Tooltip>}>
                 <span>Transferred</span>
              </OverlayTrigger>
            </Th>
            <Th column="completeLatency">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The average time a Tuple tree takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.</Tooltip>}>
                 <span>Complete Latency (ms)</span>
              </OverlayTrigger>
            </Th>
            <Th column="acked" title="">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuple trees successfully processed. A value of 0 is expected if no acking is done.</Tooltip>}>
                 <span>Acked</span>
              </OverlayTrigger>
            </Th>
            <Th column="failed">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuple trees that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.</Tooltip>}>
                 <span>Failed</span>
              </OverlayTrigger>
            </Th>
            <Th column="errorHost">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Error Host:Port</Tooltip>}>
                 <span>Error Host:Port</span>
              </OverlayTrigger>
            </Th>
            <Th column="lastError">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Last Error</Tooltip>}>
                 <span>Last Error</span>
              </OverlayTrigger>
            </Th>
            <Th column="errorTime">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Error Time</Tooltip>}>
                 <span>Error Time</span>
              </OverlayTrigger>
            </Th>
          </Thead>
          {
            _.map(spoutfilteredEntities, (s,i) => {
              return(
                <Tr key={i}>
                  <Td column="spoutId"><Link to={`/topology/${details.id}/component/${s.spoutId}`}>{s.spoutId}</Link></Td>
                  <Td column="executors">{s.executors}</Td>
                  <Td column="tasks">{s.tasks}</Td>
                  <Td column="emitted">{s.emitted}</Td>
                  <Td column="transferred">{s.transferred}</Td>
                  <Td column="completeLatency">{s.completeLatency}</Td>
                  <Td column="acked">{s.acked}</Td>
                  <Td column="failed">{s.failed}</Td>
                  <Td column="errorHost">{s.errorHost !== '' ? s.errorHost+s.errorPort : '' }</Td>
                  <Td column="lastError">{s.lastError}</Td>
                  <Td column="errorTime">{s.errorTime !== null && s.errorTime !== 0 ? this.getDateFormat(s.errorTime) : '' }</Td>
                </Tr>
              );
            })
          }
        </Table>
        </div>
        {
          spoutfilteredEntities.length !== 0
          ? <CommonPagination  {...spotPaginationObj} callBackFunction={this.callBackFunction.bind(this)} tableName="spout"/>
          : ''
        }
      </Panel>
      <Panel expanded={expandBolt} collapsible header={boltPanelHead} eventKey="4" onSelect={this.commonOnSelectFunction.bind(this,'expandBolt')}>
        <div className="input-group col-sm-4">
          <input type="text"  onKeyUp={this.handleFilter.bind(this,'bolt')} className="form-control" placeholder="Search By Id" />
          <span className="input-group-btn">
          <button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
          </span>
        </div>
        <div className="table-responsive">
        <Table className="table no-margin"  noDataText="No bolts found !"  currentPage={boltsActivePage-1} itemsPerPage={pageSize}>
          <Thead>
            <Th column="boltId">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The ID assigned to a the Component by the Topology. Click on the name to view the Component's page.</Tooltip>}>
                 <span>Id</span>
              </OverlayTrigger>
            </Th>
            <Th column="executors">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Executors are threads in a Worker process.</Tooltip>}>
                 <span>Executors</span>
              </OverlayTrigger>
            </Th>
            <Th column="tasks">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.</Tooltip>}>
                 <span>Tasks</span>
              </OverlayTrigger>
            </Th>
            <Th column="emitted">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuples emitted.</Tooltip>}>
                 <span>Emitted</span>
              </OverlayTrigger>
            </Th>
            <Th column="transferred">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuples emitted that sent to one or more bolts.</Tooltip>}>
                 <span>Transferred</span>
              </OverlayTrigger>
            </Th>
            <Th column="capacity">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">If this is around 1.0, the corresponding Bolt is running as fast as it can, so you may want to increase the Bolt's parallelism. This is (number executed * average execute latency) / measurement time.</Tooltip>}>
                 <span>Capacity (last 10m)</span>
              </OverlayTrigger>
            </Th>
            <Th column="executeLatency">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The average time a Tuple spends in the execute method. The execute method may complete without sending an Ack for the tuple.</Tooltip>}>
                 <span>Execute Latency (ms)</span>
              </OverlayTrigger>
            </Th>
            <Th column="executed">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of incoming Tuples processed.</Tooltip>}>
                 <span>Executed</span>
              </OverlayTrigger>
            </Th>
            <Th column="processLatency">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The average time it takes to Ack a Tuple after it is first received.  Bolts that join, aggregate or batch may not Ack a tuple until a number of other Tuples have been received.</Tooltip>}>
                 <span>Process Latency (ms)</span>
              </OverlayTrigger>
            </Th>
            <Th column="acked">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Tuples acknowledged by this Bolt.</Tooltip>}>
                 <span>Acked</span>
              </OverlayTrigger>
            </Th>
            <Th column="failed">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of tuples Failed by this Bolt.</Tooltip>}>
                 <span>Failed</span>
              </OverlayTrigger>
            </Th>
            <Th column="errorHost">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Error Host:Port</Tooltip>}>
                 <span>Error Host:Port</span>
              </OverlayTrigger>
            </Th>
            <Th column="lastError">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Last Error</Tooltip>}>
                 <span>Last Error</span>
              </OverlayTrigger>
            </Th>
            <Th column="errorTime">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Error Time</Tooltip>}>
                 <span>Error Time</span>
              </OverlayTrigger>
            </Th>
          </Thead>
          {
            _.map(blotfilteredEntities, (b,k) => {
              return(
                <Tr key={k}>
                  <Td column="boltId"><Link to={`/topology/${details.id}/component/${b.boltId}`}>{b.boltId}</Link></Td>
                  <Td column="executors">{b.executors}</Td>
                  <Td column="tasks">{b.tasks}</Td>
                  <Td column="emitted">{b.emitted}</Td>
                  <Td column="transferred">{b.transferred}</Td>
                  <Td column="capacity">{b.capacity}</Td>
                  <Td column="executeLatency">{b.executeLatency}</Td>
                  <Td column="executed">{b.executed}</Td>
                  <Td column="processLatency">{b.processLatency}</Td>
                  <Td column="acked">{b.acked}</Td>
                  <Td column="failed">{b.failed}</Td>
                  <Td column="errorHost">{b.errorHost !== '' ? b.errorHost+b.errorPort : '' }</Td>
                  <Td column="lastError">{b.lastError}</Td>
                  <Td column="errorTime">{b.errorTime !== null && b.errorTime !== 0 ? this.getDateFormat(b.errorTime) : '' }</Td>
                </Tr>
              );
            })
          }
        </Table>
        </div>
        {
          blotfilteredEntities.length !== 0
          ? <CommonPagination  {...boltPaginationObj} callBackFunction={this.callBackFunction.bind(this)} tableName="bolt"/>
          : ''
        }
      </Panel>
      <Panel expanded={expandConfig} collapsible header={configPanelHead} eventKey="5" onSelect={this.commonOnSelectFunction.bind(this,'expandConfig')}>
        <div className="input-group col-sm-4">
          <input type="text"  onKeyUp={this.handleFilter.bind(this,'topologyConfig')} className="form-control" placeholder="Search By Key" />
          <span className="input-group-btn">
          <button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
          </span>
        </div>
        <Table className="table no-margin"  noDataText="No topology configuration found !"  currentPage={topologyActivePage-1} itemsPerPage={pageSize}>
          <Thead>
            <Th column="Key">Key</Th>
            <Th column="value">Value</Th>
          </Thead>
          {
            _.map(topologyfilteredEntities, (k,t) => {
              return(
                <Tr key={t}>
                  <Td column="Key">{k}</Td>
                  <Td column="value">{details.configuration[k]}</Td>
                </Tr>
              );
            })
          }
        </Table>
        {
          topologyfilteredEntities.length !== 0
          ? <CommonPagination  {...topologyPaginationObj} callBackFunction={this.callBackFunction.bind(this)} tableName="topologyConfig"/>
          : ''
        }
      </Panel>

      {/*Model start here*/}
      <Modal ref={"debugModelRef"} data-title="Do you really want to debug this topology ? If yes, please, specify sampling percentage."  data-resolve={this.handleModelAction.bind(this,'debugModelRef','save')} data-reject={this.handleModelAction.bind(this,'debugModelRef','hide')}>
        <input className="form-control" type="number" min={0} max={Number.MAX_SAFE_INTEGER} value={debugSimplePCT} onChange={this.inputTextChange.bind(this,'debugSimplePCT')}/>
      </Modal>

      <Modal ref={"killModelRef"} data-title="Are you sure you want to kill this topology ? If yes, please, specify wait time in seconds."  data-resolve={this.handleModelAction.bind(this,'killModelRef','save')} data-reject={this.handleModelAction.bind(this,'killModelRef','hide')}>
        <input className="form-control" type="number" min={0} max={Number.MAX_SAFE_INTEGER} value={killWaitTime} onChange={this.inputTextChange.bind(this,'killWaitTime')}/>
      </Modal>

      <Modal ref={"rebalanceModelRef"} data-title="Rebalance Topology"  data-resolve={this.handleModelAction.bind(this,'rebalanceModelRef','save')} data-reject={this.handleModelAction.bind(this,'rebalanceModelRef','hide')}>
        <RebalanceTopology ref={"rebalanceModal"} topologyId={details.id} spoutArr={details.spouts} boltArr={details.bolts} topologyExecutors={details.workersTotal}/>
      </Modal>

    </BaseContainer>);
  }
}
