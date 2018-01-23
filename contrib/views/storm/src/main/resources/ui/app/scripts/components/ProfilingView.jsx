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
import {toastOpt,pageSize} from '../utils/Constants';
import Utils from '../utils/Utils';
import FSReactToastr from '../components/FSReactToastr';
import CommonNotification from '../components/CommonNotification';
import _ from 'lodash';

export default class ProfilingView extends Component{
  constructor(props){
    super(props);
    this.state = {
      currentPage : 1,
      executorArr : this.props.executorStats ? this.fetchData() : [],
      selectedWorker : [],
      selectAll : false,
      warnMsg : false,
      successMsg : false,
      errorMsg : false
    };
  }

  fetchData = () => {
    const {executorStats} = this.props;
    let data = {},executorArr=[];
    _.map(executorStats, (o) => {
      const hostPort = o.host + ":" + o.port;
      if(!data[hostPort]){
        data[hostPort] = {};
      }
      if(!data[hostPort].idArr){
        data[hostPort].idArr = [];
      }
      data[hostPort].idArr.push(o.id);
    });
    let keys = this.hostPortArr = _.keys(data);
    _.map(keys, (k) => {
      executorArr.push({
        hostPort: k,
        executorId: data[k].idArr,
        checked : false
      });
    });
    return executorArr;
  }

  commonBtnAction = (actionType) => {
    const {selectedWorker} = this.state;
    selectedWorker.length ?  this.apiCallback(actionType) : this.setState({warnMsg : true,successMsg : false,errorMsg: false});
  }

  apiCallback = (actionType) => {
    const {topologyId} = this.props;
    const {selectedWorker} = this.state;
    let promiseArr=[];
    _.map(selectedWorker, (w) => {
      promiseArr.push(TopologyREST.getProfiling(topologyId,actionType,w.hostPort));
    });

    Promise.all(promiseArr).then((results) => {
      _.map(results, (r) => {
        let tempErrorMsg= false,tempSuccessMsg=false;
        if(r.errorMessage !== undefined){
          tempErrorMsg = true;
          tempSuccessMsg: false;
        } else {
          tempErrorMsg = false;
          tempSuccessMsg: true;
        }
        this.setState({successMsg : tempSuccessMsg,errorMsg: tempErrorMsg,warnMsg : false});
      });
    });
  }

  handleChange = (hostPort) => {
    let tempSelect = _.cloneDeep(this.state.selectAll);
    let tempExecutor=_.cloneDeep(this.state.executorArr);
    let tempWorker = _.cloneDeep(this.state.selectedWorker);
    if(!!hostPort){
      const ind = _.findIndex(tempExecutor, (e) => {return e.hostPort === hostPort; });
      const index = _.findIndex(tempWorker,(t) => {return t.hostPort === hostPort;});
      if(index === -1 && ind !== -1){
        tempWorker.push(tempExecutor[ind]);
      } else {
        tempWorker.splice(index,1);
      }
      tempExecutor[ind].checked = !tempExecutor[ind].checked;
    } else {
      tempSelect = !this.state.selectAll;
      _.map(tempExecutor,(t) => {
        t.checked = tempSelect;
      });
      tempWorker = tempExecutor;
    }
    this.setState({selectedWorker : tempWorker,selectAll : tempSelect,executorArr :tempExecutor });
  }

  render(){
    const {currentPage,executorArr,selectAll,warnMsg,successMsg,errorMsg} = this.state;
    return(
      <div>
        <div className={`alert alert-warning alert-dismissible warning-msg ${warnMsg ? '' : 'hidden'}`}  role="alert">
          <strong>Warning!</strong> Please select atleast one worker to perform operation.
        </div>
        <div className={`alert alert-success alert-dismissible success-msg ${successMsg ? '' : 'hidden'}`}  role="alert">
          <strong>Success!</strong> Action performed successfully.
        </div>
        <div className={`alert alert-danger alert-dismissible error-msg ${errorMsg ? '' : 'hidden'}`}  role="alert">
          <strong>Error!</strong> Error occured while performing the action.
        </div>
        <div className="clearfix">
          <div className="btn-group btn-group-sm pull-right">
            <button type="button" className="btn btn-primary" onClick={this.commonBtnAction.bind(this,'dumpjstack')}>JStack</button>
            <button type="button" className="btn btn-primary" onClick={this.commonBtnAction.bind(this,'restartworker')}>Restart Worker</button>
            <button type="button" className="btn btn-primary" onClick={this.commonBtnAction.bind(this,'dumpheap')}>Heap</button>
          </div>
        </div>
        <hr />
        <Table className="table table-bordered"  columns={currentPage-1} noDataText="No workers found !">
          <Thead>
            <Th column="checkbox">
              <input type="checkbox" name="single" onChange={this.handleChange.bind(this,null)}/>
            </Th>
            <Th column="hostPort" >Host:Port</Th>
            <Th column="executorId" >Executor Id</Th>
          </Thead>
          {
            _.map(executorArr , (e,i) => {
              return <Tr key={i}>
                  <Td column="checkbox">
                    <input type="checkbox" checked={e.checked} name="single" onChange={this.handleChange.bind(this,e.hostPort)}/>
                  </Td>
                  <Td column="hostPort">{e.hostPort}</Td>
                  <Td column="executorId">{e.executorId.join(',')}</Td>
              </Tr>;
            })
          }
        </Table>
      </div>
    );
  }
}
