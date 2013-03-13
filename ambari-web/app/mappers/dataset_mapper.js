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

App.dataSetMapper = App.QuickDataMapper.create({
  model : App.DataSet,
  Jobs_model : App.DataSetJob,
  config : {
    name: 'Feeds.name', // from json
    source_cluster_name: 'Feeds.clusters.cluster[0].name', // approach1 : from json
    target_cluster_name: 'target_cluster_name', // approach 2 : to be calculated (TBC1)
    source_dir: 'Feeds.locations.location.path',
    schedule: 'Feeds.frequency',
    dataset_jobs: 'dataset_jobs', // TBC2 ( set of ids will be added )

    // all below are unknown at present and may be blank
    last_failed_date: 'last_failed_date',
    avg_data: 'avg_data',
    data_creation_date: 'data_creation_date',
    target_dir: 'target_dir'
  },
  jobs_config : {
    // $dataset_id: 'none', // will be loaded outside parser
    id: 'Instances.id',
    start_date: 'start_date_str',
    end_date: 'end_date_str',
    duration: 'duration'
    //data: 'Instances.details'
  },

  map:function(json){
    if(!this.get('model')) {return;}
    if(json && json.items && json.items.length > 0){
      var dataset_results = [];
      json.items.forEach(function(item){

        try{
          // TBC1
          item.target_cluster_name = (item.Feeds.clusters.cluster.findProperty("type","target")).name;

          // TBC2
          item.dataset_jobs = [];

          item.instances.forEach(function(job){
            item.dataset_jobs.push(job.Instances.id);
          });

          item.last_failed_date ='';
          item.avg_data ='';
          item.data_creation_date ='';
          item.target_dir ='';

          var newitem = this.parseIt(item, this.config);
          dataset_results.push(newitem);
        }catch(ex){
          console.debug('Exception occured : ' + ex);
        }
      },this);
      console.debug('Before load: App.DataSet.find().content : ' + App.DataSet.find().content );
      App.store.loadMany(this.get('model'), dataset_results);
      console.debug('After load: App.DataSet.find().content : ' + App.DataSet.find().content );

      try{
        // Child records
        var dataset_job_results = [];
        json.items.forEach(function(item){
          item.instances.forEach(function(instance){
            instance.Instances.start = new Date(instance.Instances.start); // neeed to be calulated end -start
            instance.Instances.end = new Date(instance.Instances.end); // neeed to be calulated end -start

            var milliseconds =  instance.Instances.end - instance.Instances.start;
            var date = new Date(milliseconds);
            var h = Math.floor(milliseconds / 3600000);
            var m = Math.floor((milliseconds % 3600000) / 60000);
            var s = Math.floor(((milliseconds % 360000) % 60000) / 1000);
            instance.duration = (h==0?'':h+'hr ') + (m==0?'':m+'mins ') + (s==0?'':s+'secs ');
            instance.start_date_str = instance.Instances.start.toString();
            instance.end_date_str = instance.Instances.end.toString();

            var result = this.parseIt(instance, this.jobs_config);
            dataset_job_results.push(result);
          }, this)
        }, this);

        console.debug('Before load: App.DataSetJob.find().content : ' + App.DataSetJob.find().content );
        App.store.loadMany(this.get('Jobs_model'), dataset_job_results);
        console.debug('After load: App.DataSetJob.find().content : ' + App.DataSetJob.find().content );
      }catch(ex){
        console.debug('Exception occured : ' + ex);
      }
    }
  }

});
