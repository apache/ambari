/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var App = require('app');

require('config');
require('utils/updater');
require('utils/ajax');

require('controllers/global/background_operations_controller');
require('views/common/modal_popup');

/*window.console.log = function(text){
  console.log(text);
}*/

describe('App.BackgroundOperationsController', function () {

  /**
   * Predefined data
   *
   */
  App.set('clusterName', 'testName');
  App.set('testMode', 'true');
  App.bgOperationsUpdateInterval = 100;

  /**
   * Test object
   */
  var controller = App.BackgroundOperationsController.create();

  describe('#getOperationsForRequestId', function(){

    var obj = new window.Array({"exit_code":0,"stdout":"Output","status":"COMPLETED","stderr":"none","host_name":"dev.hortonworks.com","id":10,"cluster_name":"mycluster","attempt_cnt":1,"request_id":2,"command":"EXECUTE","role":"HDFS_SERVICE_CHECK","start_time":1352119106480,"stage_id":2,"display_exit_code":true},{"exit_code":0,"stdout":"Output","status":"COMPLETED","stderr":"none","host_name":"dev.hortonworks.com","id":14,"cluster_name":"mycluster","attempt_cnt":1,"request_id":2,"command":"EXECUTE","role":"MAPREDUCE_SERVICE_CHECK","start_time":1352119157294,"stage_id":3,"display_exit_code":true},{"exit_code":0,"stdout":"Output","status":"QUEUED","stderr":"none","host_name":"dev.hortonworks.com","id":16,"cluster_name":"mycluster","attempt_cnt":1,"request_id":3,"command":"STOP","role":"NAMENODE","start_time":1352125378300,"stage_id":1,"display_exit_code":true});
    var controller = App.BackgroundOperationsController.create();


    it('test 1 with 3 items  ', function(){

      controller.set('allOperations', obj);
      expect(controller.getOperationsForRequestId(1).length).to.be.equal(0);
      expect(controller.getOperationsForRequestId(2).length).to.be.equal(2);
      expect(controller.getOperationsForRequestId(3).length).to.be.equal(1);

    });



    it('test 2 with 0 items  ', function(){

      controller.set('allOperations', new window.Array());
      expect(controller.getOperationsForRequestId(1).length).to.be.equal(0);
      expect(controller.getOperationsForRequestId(2).length).to.be.equal(0);
      expect(controller.getOperationsForRequestId(3).length).to.be.equal(0);

    });

    it('test 3 with 9 items  ', function(){

      controller.set('allOperations', obj.concat(obj, obj));
      expect(controller.getOperationsForRequestId(1).length).to.be.equal(0);
      expect(controller.getOperationsForRequestId(2).length).to.be.equal(6);
      expect(controller.getOperationsForRequestId(3).length).to.be.equal(3);

    });

  });

  describe('when set isWorking to true  ', function () {

    it('startPolling executes App.updater.run  ', function(done){
      sinon.stub(App.updater, 'run', function(){
        controller.set('isWorking', false);
        App.updater.run.restore();
        done();
      });

      controller.set('isWorking', true);
    });

    it('loadOperations should be called  ', function(done){
      this.timeout(App.bgOperationsUpdateInterval + 500);

      sinon.stub(controller, 'loadOperations', function(){
        controller.set('isWorking', false);
        controller.loadOperations.restore();
        done();
      });

      controller.set('isWorking', true);
    });

    it('updateBackgroundOperations should be called  ', function(done){
      this.timeout(App.bgOperationsUpdateInterval + 500);

      sinon.stub(controller, 'updateBackgroundOperations', function(){
        controller.set('isWorking', false);
        controller.updateBackgroundOperations.restore();
        done();
      });

      controller.set('isWorking', true);
    });

    it('allOperations should be set  ', function(done){
      this.timeout(App.bgOperationsUpdateInterval + 500);

      sinon.stub(controller, 'updateBackgroundOperations', function(data){
        controller.set('isWorking', false);
        controller.updateBackgroundOperations.restore();
        controller.updateBackgroundOperations(data);
        expect(controller.get('executeTasks').length).to.be.equal(2);
        expect(controller.get('allOperationsCount')).to.be.equal(1);

        var bgOperation = controller.get('allOperations')[0];
        expect(bgOperation).to.have.property('id');
        expect(bgOperation).to.have.property('request_id');
        expect(bgOperation).to.have.property('role');
        expect(bgOperation).to.have.property('command');
        expect(bgOperation).to.have.property('status');

        done();
      });

      controller.set('isWorking', true);
    });

  })

  describe('#showPopup', function () {
    it('works without exceptions  ', function(){
      var popup = controller.showPopup();
      popup.onPrimary();
    });
  });

  function generateTask(time, role, status, id, command){
    command = command || 'STOP';
    return {
      "Tasks" : {
        "exit_code" : 0,
        "stdout" : "Output",
        "status" : status,
        "stderr" : "none",
        "host_name" : "dev.hortonworks.com",
        "id" : id,
        "cluster_name" : "mycluster",
        "attempt_cnt" : 1,
        "request_id" : 1,
        "command" : command,
        "role" : role,
        "start_time" : time,
        "stage_id" : 1
      }
    };
  }

  function generate_test_json(){

    var time = new Date().getTime();

    return {
      items:[
        {
          "Requests" : {
            "id" : 3,
            "cluster_name" : "mycluster"
          },
          "tasks":[
            generateTask(time, 'NAMENODE', 'QUEUED', 16),
            generateTask(time, 'DATANODE', 'QUEUED', 15),
            generateTask(time, 'SECONDARY_NAMENODE', 'QUEUED', 14),
            generateTask(time, 'MAPREDUCE_SERVICE_CHECK', 'QUEUED', 13, 'EXECUTE')
          ]
        }
      ]
    };
  }

  function update_test_json(json, index, state){
    var item = json.items[0].tasks[index].Tasks;
    item.status = state;
    item.finishedTime = new Date().getTime();
  }

  function add_to_test_json(json){
    var tasks = json.items[0].tasks;
    var item = tasks[0].Tasks;
    tasks.push(generateTask(item.start_time, 'HBASE_MASTER', 'QUEUED', 12));
    tasks.push(generateTask(item.start_time, 'ZOOKEEPER_SERVER', 'QUEUED', 11));
  }

  describe('#updateBackgroundOperations', function () {
    describe('finished items are removed with delay  ', function(){
      controller.set('allOperations', new window.Array());
      controller.set('executeTasks', new window.Array());
      controller.set('allOperationsCount', 0);

      var json = generate_test_json();

      it('on start we have 4 tasks  ', function(){
        controller.updateBackgroundOperations(json);
        expect(controller.get('allOperationsCount')).to.be.equal(4);
        expect(controller.get('allOperations').length).to.be.equal(4);
        expect(controller.get('executeTasks').length).to.be.equal(1);
      })

      it('first task is in progress  ', function(){
        update_test_json(json, 2, 'IN_PROGRESS');
        controller.updateBackgroundOperations(json);
        expect(controller.get('allOperationsCount')).to.be.equal(4);
        expect(controller.get('allOperations').length).to.be.equal(4);
        expect(controller.get('executeTasks').length).to.be.equal(1);
      });

      it('first task finished  ', function(){
        update_test_json(json, 2, 'COMPLETED');
        controller.updateBackgroundOperations(json);
        expect(controller.get('allOperationsCount')).to.be.equal(3);
        expect(controller.get('allOperations').length).to.be.equal(3);
        expect(controller.get('executeTasks').length).to.be.equal(1);
      });

      it('second task is in progress  ', function(){
        update_test_json(json, 3, 'IN_PROGRESS');
        controller.updateBackgroundOperations(json);
        expect(controller.get('allOperationsCount')).to.be.equal(3);
        expect(controller.get('allOperations').length).to.be.equal(3);
        expect(controller.get('executeTasks').length).to.be.equal(1);
      });

      it('second task finished  ', function(){
        update_test_json(json, 3, 'COMPLETED');
        controller.updateBackgroundOperations(json);
        expect(controller.get('allOperationsCount')).to.be.equal(2);
        expect(controller.get('allOperations').length).to.be.equal(3);
        expect(controller.get('executeTasks').length).to.be.equal(1);
      });

      var oldLifeTime = controller.get('taskLifeTime');
      controller.set('taskLifeTime', 10);

      it('second task removed from list  ', function(done){
        setTimeout(function(){
          controller.updateBackgroundOperations(json);
          expect(controller.get('allOperationsCount')).to.be.equal(2);
          expect(controller.get('allOperations').length).to.be.equal(2);
          expect(controller.get('executeTasks').length).to.be.equal(0);

          done();
        }, controller.get('taskLifeTime'));
      });

      it('add new items  ', function(){
        add_to_test_json(json);
        controller.updateBackgroundOperations(json);
        expect(controller.get('allOperationsCount')).to.be.equal(4);
        expect(controller.get('allOperations').length).to.be.equal(4);
        expect(controller.get('executeTasks').length).to.be.equal(0);
        controller.set('taskLifeTime', oldLifeTime);
      });
    });
  });

  describe('#updateFinishedTask  ', function(){

    var json = null;

    beforeEach(function(){
      controller.set('allOperations', new window.Array());
      controller.set('executeTasks', new window.Array());
      controller.set('allOperationsCount', 0);

      json = generate_test_json();
      controller.updateBackgroundOperations(json);
    });

    it('update task  ', function(){
      update_test_json(json, 3, 'COMPLETED');
      controller.updateFinishedTask(json.items[0].tasks[3]);

      var item = controller.get('allOperations').findProperty('id', 13);
      expect(item).to.be.an('object');
      expect(item).to.have.property('status', 'COMPLETED');
      expect(item.finishedTime).to.be.above(0);
    })

    it("don't update task  ", function(){
      controller.updateFinishedTask({ Tasks: { id : 1 } });

      var item = controller.get('allOperations').findProperty('id', 13);
      expect(item).to.be.an('object');
      expect(item).to.have.property('status', 'QUEUED');
      expect(item.finishedTime).to.be.equal(undefined);
    })

  });

  describe('#eventsArray  ', function(){

    var json = null;

    beforeEach(function(){
      controller.set('allOperations', new window.Array());
      controller.set('executeTasks', new window.Array());
      controller.set('allOperationsCount', 0);

      json = generate_test_json();
      controller.updateBackgroundOperations(json);
    });

    it("it's working  ", function(done){
      controller.get('eventsArray').push({
        "when" : function(controller){
          return controller.get('allOperationsCount') == 3;
        },
        "do" : done
      });

      controller.updateBackgroundOperations(json);
      controller.updateBackgroundOperations(json);
      controller.updateBackgroundOperations(json);
      controller.updateBackgroundOperations(json);

      update_test_json(json, 1, 'COMPLETED');
      controller.updateBackgroundOperations(json);
    })
  });


})
