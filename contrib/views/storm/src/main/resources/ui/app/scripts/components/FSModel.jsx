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
import {Modal, Button} from 'react-bootstrap';

const defaultState = {
  show: false,
  title: '',
  btnOkText: 'Ok',
  btnCancelText: 'Cancel'
};

export default class FSModal extends Component {
  state = defaultState;
  show() {
    var state = state || {};
    state.show = true;
    this.setState(state);
  }
  sure() {
    let resolve = this.props["data-resolve"];
    if (resolve) {
      resolve();
    }
  }
  cancel() {
    let reject = this.props["data-reject"];
    if (reject) {
      reject();
    } else {
      this.hide();
    }
  }
  hide() {
    this.setState({show: false});
  }
  header() {
    return (
      <Modal.Header closeButton>
        <Modal.Title>
          {this.props["data-title"]}
        </Modal.Title>
      </Modal.Header>
    );
  }
  body() {
    return (
      <Modal.Body>
        {this.props.children}
      </Modal.Body>
    );
  }
  footer() {
    return (
      <Modal.Footer>
        {
          this.props.hideCloseBtn
          ? null
          : <Button bsStyle='default' onClick={this.cancel.bind(this)} data-stest="cancelbtn">
              {this.props.closeLabel || this.state.btnCancelText}
            </Button>
        }
        {
          this.props.hideOkBtn
          ? null
          : <Button bsStyle='success' onClick={this.sure.bind(this)}  data-stest="okbtn" disabled={this.props.btnOkDisabled}>
              {this.props.okLabel || this.state.btnOkText}
            </Button>
        }
      </Modal.Footer>
    );
  }
  render() {
    return (
      <Modal aria-labelledby='contained-modal-title' backdrop="static" keyboard={true} onHide={this.cancel.bind(this)} show={this.state.show} {...this.props}>
        {this.props.hideHeader
          ? ''
          : this.header()}
        {this.body()}
        {this.props.hideFooter
          ? ''
          : this.footer()}
      </Modal>
    );
  }
}

var _resolve;
var _reject;

export class Confirm extends FSModal {
  show(state) {
    var state = state || {};
    state.show = true;
    this.setState(state);
    let promise = new Promise(function(resolve, reject) {
      _resolve = resolve;
      _reject = reject;
    });
    return promise;
  }
  sure() {
    _resolve(this);
  }
  cancel() {
    _reject(this);
    this.setState(defaultState);
  }
  header() {
    return (
      <Modal.Header closeButton>
        <Modal.Title>
          {this.state.title}
        </Modal.Title>
      </Modal.Header>
    );
  }
  body() {
    return '';
  }
  footer() {
    return (
      <Modal.Footer>
        <Button bsStyle='danger' onClick={this.cancel.bind(this)} data-stest="confirmBoxCancelBtn">
          {this.state.btnCancelText || 'No'}
        </Button>
        <Button bsStyle='success' onClick={this.sure.bind(this)} data-stest="confirmBoxOkBtn">
          {this.state.btnOkText || 'Yes'}
        </Button>
      </Modal.Footer>
    );
  }
}
