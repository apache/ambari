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
import TopologyREST from '../rest/TopologyREST';
import {
  Table,
  Thead,
  Th,
  Tr,
  Td,
  unsafe
} from 'reactable';
import CommonPagination from './CommonPagination';
import {toastOpt,pageSize} from '../utils/Constants';
import Select from 'react-select';
import FSReactToastr from './FSReactToastr';
import CommonNotification from './CommonNotification';
import Editable  from './Editable';

export default class LogLevelComponent extends Component{
  constructor(props){
    super(props);
    this.state = {
      logLevelObj : {},
      traceOption : this.populateTraceOptions(),
      selectedKeyName : 'com.your.organization.LoggerName',
      selectedTrace : 'ALL',
      selectedTimeOut : 30
    };
    this.fetchData();
    this.keyName = '';
    this.timeChange='';
  }

  fetchData = () => {
    const {topologyId} = this.props;
    TopologyREST.getLogConfig(topologyId).then((result) => {
      if(result.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
      } else {
        let stateObj={};
        stateObj.selectedKeyName = 'com.your.organization.LoggerName';
        stateObj.selectedTrace = 'ALL';
        stateObj.selectedTimeOut = 30;
        stateObj.logLevelObj = result.namedLoggerLevels;
        this.setState(stateObj);
      }
    });
  }

  populateTraceOptions = () => {
    let temp=[];
    const arr = ['ALL','TRACE','DEBUG','INFO','WARN','ERROR','FATAL','OFF'];
    _.map(arr, (a) => {
      temp.push({label : a, value : a});
    });
    return temp;
  }

  handleNameChange = (e) => {
    this.keyName =  e.target.value.trim();
  }

  handleTimeChange = (e) => {
    this.timeChange =  e.target.value.trim();
  }

  traceLavelChange = (type,key,addRow,obj) => {
    let tempObj = _.cloneDeep(this.state.logLevelObj);
    let tempKeyName = 'ALL';
    if(!!addRow){
      tempKeyName = obj.value;
    } else{
      tempObj[type][key] = obj.value;
    }
    this.setState({logLevelObj : tempObj,selectedTrace : tempKeyName});
  }

  modifyCommonObjValue = (refType,type,key,action,addRow) => {
    let logObj = _.cloneDeep(this.state.logLevelObj);
    let tempTimeOut = _.cloneDeep(this.state.selectedTimeOut);
    const timeValue =  (this.timeChange === '' || this.timeChange === undefined) ? parseInt(this.refs[refType].defaultValue || 0,10) : parseInt(this.timeChange,10);
    if(action === 'save' && addRow === null){
      logObj[type][key] = timeValue;
    } else if(action === 'save' && !!addRow){
      tempTimeOut = timeValue;
      this.timeChange = '';
    }else if(action === 'reject'){
      this.timeChange = parseInt(this.refs[refType].defaultValue || 0,10);
    }
    this.refs[refType].hideEditor();
    this.setState({logLevelObj : logObj ,selectedTimeOut :tempTimeOut });
  }

  getDateFormat = (str) => {
    const d = new Date(str);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
  }

  saveAndClearLogConfig = (type,action) => {
    let tempObj = _.cloneDeep(this.state.logLevelObj);
    let obj={},namedLoggerLevels={};
    obj.namedLoggerLevels={};
    if(action === 'clear'){
      obj.namedLoggerLevels[type] = {};
      obj.namedLoggerLevels[type].timeout = 0;
      obj.namedLoggerLevels[type].target_level = null;
    } else {
      obj.namedLoggerLevels[type] = tempObj[type];
    }
    obj.namedLoggerLevels[type].reset_level = 'INFO';
    delete obj.namedLoggerLevels[type].timeout_epoch;

    this.callLogConfigAPI(obj,null,action);
  }

  callLogConfigAPI = (obj,addRow,action) => {
    const {topologyId,logConfig} =  this.props;
    const {logLevelObj} = this.state;
    TopologyREST.postLogConfig(topologyId, {body : JSON.stringify(obj)}).then((result) => {
      if(result.errorMessage !== undefined){
        this.setState({logLevelObj : logConfig});
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
      } else {
        let msg = !!addRow ? "Log configuration added successfully" : (action === 'save' ? "Log configuration applied successfully." : "Log configuration cleared successfully.");
        FSReactToastr.success(<strong>{msg}</strong>);
        this.fetchData();
      }
    });
  }

  addLoggerName = (refType,action) => {
    let tempName = _.cloneDeep(this.state.selectedKeyName);
    if(action === 'save'){
      tempName = !!this.keyName ? this.keyName :  tempName;
    }else if(action === 'reject'){
      this.keyName = this.refs[refType].defaultValue || tempName;
    }
    this.refs[refType].hideEditor();
    this.setState({selectedKeyName : tempName});
  }

  addLogRow = () => {
    const {selectedKeyName,selectedTrace,selectedTimeOut} = this.state;
    let obj={};
    obj.namedLoggerLevels = {};
    obj.namedLoggerLevels[selectedKeyName] = {};
    obj.namedLoggerLevels[selectedKeyName].target_level = selectedTrace;
    obj.namedLoggerLevels[selectedKeyName].reset_level = 'INFO';
    obj.namedLoggerLevels[selectedKeyName].timeout = selectedTimeOut;
    this.callLogConfigAPI(obj,'addRow');
  }

  render(){
    const {logLevelObj,traceOption,selectedKeyName,selectedTrace,selectedTimeOut} = this.state;
    return(
      <div className={`boxAnimated`}>
        <hr/>
                                <h4 className="col-sm-offset-5">Change Log Level</h4>
                                <p>Modify the logger levels for topology. Note that applying a setting restarts the timer in the workers. To configure the root logger, use the name ROOT.</p>
        <Table className="table no-margin">
          <Thead>
            <Th column="logger" title="Logger">Logger</Th>
            <Th column="target_level" title="Level">Level</Th>
            <Th column="timeout" title="Timeout">Timeout</Th>
            <Th column="timeout_epoch" title="Expires At">Expires At</Th>
            <Th column="action" title="Action">Action</Th>
          </Thead>
          {
            _.map(_.keys(logLevelObj), (logKey, i) => {
              return <Tr key={i}>
                      <Td column="logger">
                        <a href="javascript:void(0)">{logKey}</a>
                      </Td>
                      <Td column="target_level">{}
                        <Select value={logLevelObj[logKey].target_level} options={traceOption} onChange={this.traceLavelChange.bind(this,logKey,'target_level',null)} required={true} clearable={false} />
                      </Td>
                      <Td column="timeout">
                        <Editable ref={`logKey${i}`} inline={true} resolve={this.modifyCommonObjValue.bind(this,`logKey${i}`,logKey,'timeout','save',null)} reject={this.modifyCommonObjValue.bind(this,`logKey${i}`,logKey,'timeout','reject',null)}>
                          <input className="form-control input-sm editInput"  ref={this.focusInput} defaultValue={logLevelObj[logKey].timeout} onChange={this.handleTimeChange.bind(this)}/>
                        </Editable>
                      </Td>
                      <Td column="timeout_epoch">{this.getDateFormat(logLevelObj[logKey].timeout_epoch)}</Td>
                      <Td column="action">
                        <span>
								<a href="javascript:void(0)" className="btn btn-success btn-xs" onClick={this.saveAndClearLogConfig.bind(this,logKey,'save')}><i className="fa fa-check"></i></a>&nbsp;
								<a href="javascript:void(0)"  className="btn btn-danger btn-xs" onClick={this.saveAndClearLogConfig.bind(this,logKey,'clear')}><i className="fa fa-times"></i></a>
								</span>
                      </Td>
                    </Tr>;
            })
          }
          <Tr key={Math.random()}>
            <Td  column="logger">
              <Editable ref="addRowRef" inline={true} resolve={this.addLoggerName.bind(this,'addRowRef','save')} reject={this.addLoggerName.bind(this,"addRowRef",'reject')}>
                <input className="form-control input-sm editInput"  ref={this.focusInput} defaultValue={selectedKeyName} onChange={this.handleNameChange.bind(this)}/>
              </Editable>
            </Td>
            <Td  column="target_level">
              <Select value={selectedTrace} options={traceOption} onChange={this.traceLavelChange.bind(this,null,'target_level','ADD')} required={true} clearable={false} />
            </Td>
            <Td  column="timeout">
              <Editable ref={"timeoutRef"} inline={true} resolve={this.modifyCommonObjValue.bind(this,"timeoutRef",null,'timeout','save','ADD')} reject={this.modifyCommonObjValue.bind(this,"timeoutRef",null,'timeout','reject','ADD')}>
                <input className="form-control input-sm editInput"  ref={this.focusInput} defaultValue={selectedTimeOut} onChange={this.handleTimeChange.bind(this)}/>
              </Editable>
            </Td>
            <Td  column="timeout_epoch">&nbsp;</Td>
              <Td column="action">
                <span>
                  <a href="javascript:void(0)" className="btn btn-primary btn-xs" onClick={this.addLogRow.bind(this,'save')}><i className="fa fa-check"></i></a>&nbsp;
                  </span>
              </Td>
          </Tr>
        </Table>
      </div>
    );
  }
}
