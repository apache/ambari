<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

## Ambari Infra Solr Client

CLI helper tool(s) for Ambari Infra Solr.

### Post Ambari Server Upgrade (Ambari 2.7.x)

Ambari Infra Solr uses Solr 7 from Ambari 2.7.0, therefore it is required migrate Solr 5 index (Ambari Infra 2.6.x), if you want to keep your old data. (otherwise backup part can be skipped)

#### Contents:
- [I. Upgrade Ambari Infra Solr Clients](#i.-upgrade-ambari-infra-solr-client)
- [II. Backup Solr Collections](#ii.-backup-collections-(ambari-2.6.x-to-ambari-2.7.x))
    - a.) If you have Ranger Ambari service with Solr audits:
        - [1. Backup Ranger collection](#ii/1.-backup-ranger-collection)
        - [2. Backup Ranger configs on Solr ZNode](#ii/2.-backup-ranger-configs-on-solr-znode)
        - [3. Delete Ranger collection](#ii/3.-delete-ranger-collection)
        - [4. Upgrade Ranger Solr schema](#ii/4.-upgrade-ranger-solr-schema)
    - b.) If you have Atlas Ambari service:
        - [5. Backup Atlas collections](#ii/5.-backup-atlas-collections)
        - [6. Delete Atlas collections](#ii/6.-delete-atlas-collections)
    - c.) If you have Log Search Ambari service:
        - [7. Delete Log Search collections](#ii/7.-delete-log-search-collections)
        - [8. Delete Log Search Solr configs](#ii/8.-delete-log-search-solr-configs)
- [III. Upgrade Ambari Infra Solr package](#iii.-upgrade-infra-solr-packages)
- [IV. Re-create Solr Collections](#iv.-re-create-collections)
- [V. Migrate Solr Collections](#v.-migrate-solr-collections)
    - a.) If you have Ranger Ambari service with Solr audits:
        - [1. Migrate Ranger Solr collection](#v/1.-migrate-ranger-collections)
    - b.) If you have Atlas Ambari service:
        - [2. Migrate Atlas Solr collections](#v/2.-migrate-atlas-collections)
- [VI. Restore Solr Collections](#vi.-restore-collections)
    - a.) If you have Ranger Ambari service with Solr audits:
        - [1. Restore old Ranger collection](#vi/1.-restore-old-ranger-collection)
        - [2. Reload restored Ranger collection](#vi/2.-reload-restored-collection)
        - [3. Transport old data to Ranger collection](#vi/3.-transport-old-data-to-ranger-collection)
    - b.) If you have Atlas Ambari service:
        - [4. Restore old Atlas collections](#vi/4.-restore-old-atlas-collections)
        - [5. Reload restored Atlas collections](#vi/5.-reload-restored-atlas-collections)
        - [6. Transport old data to Atlas collections](#vi/6.-transport-old-data-to-atlas-collections)
#### <a id="i.-upgrade-ambari-infra-solr-client">I. Upgrade Ambari Infra Solr Client</a>

First make sure `ambari-infra-solr-client` is the latest. (If its before 2.7.x) It will contain the migrationHelper.py script at `/usr/lib/ambari-infra-solr-client` location. 
Also make sure you won't upgrade `ambari-infra-solr` until the migration has not done. (all of this should happen after `ambari-server` upgrade, also make sure to not restart `INFRA_SOLR` instances)

### <a id="ii.-backup-collections-(ambari-2.6.x-to-ambari-2.7.x)">II. Backup collections (Ambari 2.6.x to Ambari 2.7.x)</a>

Before you start to upgrade process, check how many shards you have for Ranger collection, in order to know later how many shards you need to create for the collection where you will store the migrated index. Also make sure you have stable shards (at least one core is up and running) and will have enough space on the disks to store Solr backup data.

#### <a id="ii/1.-backup-ranger-collection">II/1. Backup Ranger collection</a>

Use [migrationHelper.py](#solr-migration-helper-script) script to backup the ranger collection.

```bash
# collection parameters
BACKUP_COLLECTION=ranger_audits
BACKUP_NAME=ranger
# init ambari parameters
AMBARI_SERVER_HOST=... # e.g.: c7401.ambari.apache.org
AMBARI_SERVER_PORT=... # e.g.: 8080
CLUSTER_NAME=... # e.g.: cl1
AMBARI_USERNAME=... # e.g.: admin
AMBARI_PASSWORD=... # e.g.: admin

BACKUP_PATH=... # set a backup location like /tmp/ranger_backup, the command should create that folder if not exists

# use -s or --ssl option if ssl enabled for ambari-server

/usr/lib/ambari-infra-solr-client/migrationHelper.py -H $AMBARI_SERVER_HOST -P $AMBARI_SERVER_PORT -c $CLUSTER_NAME -u $AMBARI_USERNAME -p $AMBARI_PASSWORD --action backup --index-location $BACKUP_PATH --collection $BACKUP_COLLECTION --backup-name $BACKUP_NAME
```

Optionally, you can stop ranger plugins at this point (or before that).

Also you can do the backup manually on every Solr node, by using [backup API of Solr](https://lucene.apache.org/solr/guide/6_6/making-and-restoring-backups.html). (use against core names, not collection name, it works as expected only if you have 1 shard on every node)

Example:
```bash

su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 
# collection parameters
BACKUP_PATH=... # backup location, e.g.: /tmp/ranger-backup

# RUN THIS FOR EVERY CORE ON SPECIFIC HOSTS !!!
BACKUP_CORE=... # specific core on a host
BACKUP_CORE_NAME=... # core names for backup -> <backup_location>/
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)
mkdir -p $BACKUP_PATH

curl --negotiate -k -u : "$SOLR_URL/$BACKUP_CORE/replication?command=BACKUP&location=$BACKUP_PATH&name=$BACKUP_CORE_NAME"
```

(help: [get core names](#get-core-/-shard-names-with-hosts))

#### <a id="ii/2.-backup-ranger-configs-on-solr-znode">II/2. Backup Ranger configs on Solr ZNode</a>

Next you can copy `ranger_audits` configs to a different znode, in order to keep the old schema.

```bash
export JAVA_HOME=/usr/jdk64/1.8.0_112 # or other jdk8 location
export ZK_CONN_STR=... # without znode, e.g.: myhost1:2181,myhost2:2181,myhost3:2181 
# note 1: --transfer-mode copyToLocal or --transfer-mode copyFromLocal can be used if you want to use the local filesystem
# note 2: use --jaas-file option only if the cluster is kerberized
infra-solr-cloud-cli --transfer-znode -z $ZK_CONN_STR --jaas-file /etc/ambari-infra-solr/conf/infra_solr_jaas.conf --copy-src /infra-solr/configs/ranger_audits --copy-dest /infra-solr/configs/old_ranger_audits
```

#### <a id="ii/3.-delete-ranger-collection">II/3. Delete Ranger collection</a>

At this point you can delete the actual Ranger collection with this command:

```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
COLLECTION_NAME=ranger_audits

# use kinit and --negotiate option for curl only if the cluster is kerberized
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
```

#### <a id="ii/4.-upgrade-ranger-solr-schema">II/4. Upgrade Ranger Solr schema</a>

Before creating the new Ranger collection, it is required to upgrade `managed-schema` configs.

```bash
sudo -u infra-solr -i

# If kerberos enabled
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

## BACKUP OLD CONFIG
export JAVA_HOME=/usr/jdk64/1.8.0_112 # or other jdk8 location
export ZK_CONN_STR=... # without znode, e.g.: myhost1:2181,myhost2:2181,myhost3:2181 
# note: --transfer-mode copyToLocal or --transfer-mode copyFromLocal can be used if you want to use the local filesystem
infra-solr-cloud-cli --transfer-znode -z $ZK_CONN_STR --jaas-file /etc/ambari-infra-solr/conf/infra_solr_jaas.conf --copy-src /infra-solr/configs/ranger_audits --copy-dest /infra-solr/configs/old_ranger_audits
## UPLOAD NEW SCHEMA
# Setup env for zkcli.sh
source /etc/ambari-infra-solr/conf/infra-solr-env.sh
# Run that command only if kerberos is enabled.
export SOLR_ZK_CREDS_AND_ACLS="${SOLR_AUTHENTICATION_OPTS}"
# Download the actual Ranger schema
wget -O managed-schema https://raw.githubusercontent.com/apache/ranger/master/security-admin/contrib/solr_for_audit_setup/conf/managed-schema

# Upload the new schema
/usr/lib/ambari-infra-solr/server/scripts/cloud-scripts/zkcli.sh --zkhost "${ZK_HOST}" -cmd putfile /configs/ranger_audits/managed-schema managed-schema
```

#### <a id="ii/5.-backup-atlas-collections">II/5. Backup Atlas collections</a>

Atlas has 3 collections: fulltext_index, edge_index, vertex_index.
You will need to do similar steps that you did for Ranger, but you it is required to do for all 3 collection. (steps below is for fulltext_index)

```bash
# collection parameters
BACKUP_COLLECTION=fulltext_index
BACKUP_NAME=fulltext_index
# init ambari parameters
AMBARI_SERVER_HOST=... # e.g.: c7401.ambari.apache.org
AMBARI_SERVER_PORT=... # e.g.: 8080
CLUSTER_NAME=... # e.g.: cl1
AMBARI_USERNAME=... # e.g.: admin
AMBARI_PASSWORD=... # e.g.: admin

BACKUP_PATH=... # set a backup location like /tmp/fulltext_index_backup, the command should create that folder if not exists

# use -s or --ssl option if ssl enabled for ambari-server

/usr/lib/ambari-infra-solr-client/migrationHelper.py -H $AMBARI_SERVER_HOST -P $AMBARI_SERVER_PORT -c $CLUSTER_NAME -u $AMBARI_USERNAME -p $AMBARI_PASSWORD --action backup --index-location $BACKUP_PATH --collection $BACKUP_COLLECTION --backup-name $BACKUP_NAME
```

Also you can do the backup manually on every Solr node, by using [backup API of Solr](https://lucene.apache.org/solr/guide/6_6/making-and-restoring-backups.html). (use against core names, not collection name, it works as expected only if you have 1 shard on every node)

Example:
```bash

su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 
# collection parameters
BACKUP_PATH=... # backup location, e.g.: /tmp/fulltext_index_backup

# RUN THIS FOR EVERY CORE ON SPECIFIC HOSTS !!!
BACKUP_CORE=... # specific core on a host
BACKUP_CORE_NAME=... # core names for backup -> <backup_location>/
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)
mkdir -p $BACKUP_PATH

curl --negotiate -k -u : "$SOLR_URL/$BACKUP_CORE/replication?command=BACKUP&location=$BACKUP_PATH&name=$BACKUP_CORE_NAME"
```
(help: [get core names](#get-core-/-shard-names-with-hosts))

#### <a id="ii/6.-delete-atlas-collections">II/6. Delete Atlas collections</a>

Next step for Atlas is to delete all 3 old collections.

```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr

# use kinit and --negotiate option for curl only if the cluster is kerberized
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

COLLECTION_NAME=fulltext_index
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
COLLECTION_NAME=edge_index
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
COLLECTION_NAME=vertex_index
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
```

#### <a id="ii/7.-delete-log-search-collections">II/7. Delete Log Search collections</a>

For Log Search, it is a must to delete the old collections.

```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr

# use kinit and --negotiate option for curl only if the cluster is kerberized
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

COLLECTION_NAME=hadoop_logs
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
COLLECTION_NAME=audit_logs
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
COLLECTION_NAME=history
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
```

#### <a id="ii/8.-delete-log-search-solr-configs">II/8. Delete Log Search Solr configs</a>

Log Search configs are changed a lot between Ambari 2.6.x and Ambari 2.7.x, so it is required to delete those as well. (configs will be regenerated during Log Search startup)

```bash
su infra-solr # infra-solr user - if you have a custom one, use that
# ZOOKEEPER CONNECTION STRING from zookeeper servers
export ZK_CONN_STR=... # without znode,e.g.: myhost1:2181,myhost2:2181,myhost3:2181 

kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

zookeeper-client -server $ZK_CONN_STR rmr /infra-solr/configs/hadoop_logs
zookeeper-client -server $ZK_CONN_STR rmr /infra-solr/configs/audit_logs
zookeeper-client -server $ZK_CONN_STR rmr /infra-solr/configs/history
```

### <a id="iii.-upgrade-infra-solr-packages">III. Upgrade Infra Solr packages</a>

At this step, you will need to upgrade `ambari-infra-solr` packages. (also make sure ambari-logsearch* packages are upgraded as well)

Example (for CentOS):
```bash
yum upgrade -y ambari-infra-solr
```

### <a id="iv.-re-create-collections">IV. Re-create collections</a>

Restart Ranger Admin / Atlas / Log Search Ambari service, as the collections were deleted before, during startup, new collections will be created (as a Solr 7 collection).
At this point you can stop, and do the migration / restore later (until you will have the backup), and go ahead with e.g. HDP upgrade. (migration part can take long - 1GB/min.)

### <a id="v.-migrate-solr-collections">V. Migrate Solr Collections</a>

From this point, you can migrate your old index in the background. On every hosts, where there is a backup located, you can run luce index migration tool (packaged with ambari-infra-solr-client).. For lucene index migration, [migrationHelper.py](#solr-migration-helper-script) can be used, or `/usr/lib/ambari-infra-solr-client/solrIndexHelper.sh` directly. That script uses [IndexMigrationTool](#https://lucene.apache.org/solr/guide/7_3/indexupgrader-tool.html)

#### <a id="v/1.-migrate-ranger-collections">V/1. Migrate Ranger collections</a>

Migration for `ranger_audits` collection (cores):

```bash
# init ambari parameters
AMBARI_SERVER_HOST=...
AMBARI_SERVER_PORT=...
CLUSTER_NAME=...
AMBARI_USERNAME=...
AMBARI_PASSWORD=...

BACKUP_PATH=... # will run migration on every folder which contains *snapshot* in its name
BACKUP_COLLECTION=ranger_audits # collection name - used for only logging

# use -s or --ssl option if ssl enabled for ambari-server
/usr/lib/ambari-infra-solr-client/migrationHelper.py -H $AMBARI_SERVER_HOST -P $AMBARI_SERVER_PORT -c $CLUSTER_NAME -u $AMBARI_USERNAME -p $AMBARI_PASSWORD --action migrate --index-location $BACKUP_PATH --collection $BACKUP_COLLECTION
```

Or you can run commands manually on nodes where your backups are located:
```bash

export JAVA_HOME=/usr/jdk64/1.8.0_112

# if /tmp/ranger-backup is your backup location
infra-lucene-index-tool upgrade-index -d /tmp/ranger-backup -f -b -g

# with 'infra-lucene-index-tool help' command you can checkout the command line options
```

By default, the tool will migrate from lucene version 5 to lucene version 6.6.0. (that's ok for Solr 7) If you want a lucene 7 index, you will need to re-run the migration tool command with `-v 7.3.0` option. 

#### <a id="v/2.-migrate-atlas-collections">V/2. Migrate Atlas collections</a>

As Atlas has 3 collections, you will need similar steps that is required for Ranger, just for all 3 collections.
(fulltext_index, edge_index, vertex_index)

Example with fulltext_index:

```bash
# init ambari parameters
AMBARI_SERVER_HOST=...
AMBARI_SERVER_PORT=...
CLUSTER_NAME=...
AMBARI_USERNAME=...
AMBARI_PASSWORD=...

BACKUP_PATH=... # will run migration on every folder which contains *snapshot* in its name
BACKUP_COLLECTION=fulltext_index # collection name - used for only logging

# use -s or --ssl option if ssl enabled for ambari-server
/usr/lib/ambari-infra-solr-client/migrationHelper.py -H $AMBARI_SERVER_HOST -P $AMBARI_SERVER_PORT -c $CLUSTER_NAME -u $AMBARI_USERNAME -p $AMBARI_PASSWORD --action migrate --index-location $BACKUP_PATH --collection $BACKUP_COLLECTION
```

Or you can run commands manually on nodes where your backups are located:
```bash

export JAVA_HOME=/usr/jdk64/1.8.0_112

# if /tmp/fulltext_index_backup is your backup location
infra-lucene-index-tool upgrade-index -d /tmp/fulltext_index_backup -f -b -g

# with 'infra-lucene-index-tool help' command you can checkout the command line options
```

By default, the tool will migrate from lucene version 5 to lucene version 6.6.0. (that's ok for Solr 7) If you want a lucene 7 index, you will need to re-run the migration tool command with `-v 7.3.0` option. 

### <a id="vi.-restore-collections">VI. Restore Collections</a>

For restoring the old collections, first you will need to create them. As those collections could be not listed in the security.json of Infra Solr, you can get 403 errors if you will try to access those collections later, for that time until you are doing the restoring + transport solr data to another collections, you can [trun off](#turn-off-infra-solr-authorization) the Solr authorization plugin.

#### <a id="vi/1.-restore-old-ranger-collection">VI/1. Restore Old Ranger collection</a>

After lucene data migration is finished, you can restore your replicas on every hosts where you have the backups. But we need to restore the old data to a new collection, so first you will need to create that: (on a host where you have an installed Infra Solr component). For Ranger, use old_ranger_audits config set that you backup up during Solr schema config upgrade step. (set this as CONFIG_NAME), to make that collection to work with Solr 7, you need to copy your solrconfig.xml as well.

Create a collection for restoring the backup (`old_ranger_audits`)
```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
NUM_SHARDS=... # use that number that was used for the old collection - important to use at least that many that you have originally before backup
NUM_REP=1 # can be more, but 1 is recommended for that temp collection
MAX_SHARDS_PER_NODE=... # use that number that was used for the old collection
CONFIG_NAME=old_ranger_audits
OLD_DATA_COLLECTION=old_ranger_audits

# kinit only if kerberos is enabled for tha cluster
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

export JAVA_HOME=/usr/jdk64/1.8.0_112 # or other jdk8 location
export ZK_CONN_STR=... # without znode, e.g.: myhost1:2181,myhost2:2181,myhost3:2181 

# note 1: jaas-file option required only if kerberos is enabled for the cluster
# note 2: copy new solrconfig.xml as the old one won't be compatible with solr 7 
infra-solr-cloud-cli --transfer-znode -z $ZK_CONN_STR --jaas-file /etc/ambari-infra-solr/conf/infra_solr_jaas.conf --copy-src /infra-solr/configs/ranger_audits/solrconfig.xml --copy-dest /infra-solr/configs/old_ranger_audits/solrconfig.xml

curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=CREATE&name=$OLD_DATA_COLLECTION&numShards=$NUM_SHARDS&replicationFactor=$NUM_REP&maxShardsPerNode=$MAX_SHARDS_PER_NODE&collection.configName=$CONFIG_NAME"
```

Restore the collection:
(important note: you will need to add `--solr-hdfs-path` option if your index is on HDFS (value can be like: `/user/infra-solr`), which should be the location where your collections are located.)
```bash
# init ambari parameters
AMBARI_SERVER_HOST=...
AMBARI_SERVER_PORT=...
CLUSTER_NAME=...
AMBARI_USERNAME=...
AMBARI_PASSWORD=...

OLD_BACKUP_COLLECTION=old_ranger_audits
BACKUP_NAME=ranger
BACKUP_PATH=... # backup location, e.g.: /tmp/ranger-backup
NUM_SHARDS=... # important, use a proper number, that will be stored in core.properties files

# use -s or --ssl option if ssl enabled for ambari-server
/usr/lib/ambari-infra-solr-client/migrationHelper.py -H $AMBARI_SERVER_HOST -P $AMBARI_SERVER_PORT -c $CLUSTER_NAME -u $AMBARI_USERNAME -p $AMBARI_PASSWORD --action restore --index-location $BACKUP_PATH --collection $OLD_BACKUP_COLLECTION --backup-name $BACKUP_NAME --shards $NUM_SHARDS
```

Also you can manually run restore commands: ([get core names](#get-core-/-shard-names-with-hosts))

```bash
su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 
BACKUP_PATH=... # backup location, e.g.: /tmp/ranger-backup

OLD_BACKUP_COLLECTION_CORE=... # choose a core to restore
BACKUP_CORE_NAME=... # choose a core from backup cores - you can find these names as : <backup_location>/snapshot.$BACKUP_CORE_NAME

kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)
curl --negotiate -k -u : "$SOLR_URL/$OLD_BACKUP_COLLECTION_CORE/replication?command=RESTORE&location=$BACKUP_PATH&name=$BACKUP_CORE_NAME"
```

Or use simple `cp` or `hdfs dfs -put` commands to copy the migrated cores to the right places.

#### <a id="vi/2.-reload-restored-collection">VI/2. Reload restored collection</a>

After the cores are restored you will need to reload the old_ranger_audits collection:

```bash
su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 
OLD_RANGER_COLLECTION=old_ranger_audits

# use kinit only if kerberos is enabled
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)
curl --negotiate -k -u : "$SOLR_URL/admin/collecions?action=RELOAD&name=$OLD_RANGER_COLLECTION"
```

#### <a id="vi/3.-transport-old-data-to-ranger-collection">VI/3. Transport old data to Ranger collection</a>

In the end, you end up with 2 collections (ranger_audits and old_ranger_audits), in order to drop the restored one, you will need to transfer your old data to the new collection. To achieve this, you can use [solrDataManager.py](#solr-data-manager-script), which is located next to the `migrationHelper.py` script

```bash
# Init values:
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
INFRA_SOLR_KEYTAB=... # example: /etc/security/keytabs/ambari-infra-solr.service.keytab
INFRA_SOLR_PRINCIPAL=... # example: infra-solr/$(hostname -f)@EXAMPLE.COM
END_DATE=... # example: 2018-02-18T12:00:00.000Z , date until you export data

OLD_COLLECTION=old_ranger_audits
ACTIVE_COLLECTION=ranger_audits
EXCLUDE_FIELDS=_version_ # comma separated exclude fields, at least _version_ is required

DATE_FIELD=evtTime
# infra-solr-data-manager is a symlink points to /usr/lib/ambari-infra-solr-client/solrDataManager.py
infra-solr-data-manager -m archive -v -c $OLD_COLLECTION -s $SOLR_URL -z none -r 10000 -w 100000 -f $DATE_FIELD -e $END_DATE --solr-output-collection $ACTIVE_COLLECTION -k $INFRA_SOLR_KEYTAB -n $INFRA_SOLR_PRINCIPAL --exclude-fields $EXCLUDE_FIELDS

# Or if you want to run the command in the background (with log and pid file):
nohup infra-solr-data-manager -m archive -v -c $OLD_COLLECTION -s $SOLR_URL -z none -r 10000 -w 100000 -f $DATE_FIELD -e $END_DATE --solr-output-collection $ACTIVE_COLLECTION -k $INFRA_SOLR_KEYTAB -n $INFRA_SOLR_PRINCIPAL --exclude-fields $EXCLUDE_FIELDS > /tmp/solr-data-mgr.log 2>&1>& echo $! > /tmp/solr-data-mgr.pid
```

#### <a id="vi/4.-restore-old-atlas-collections">VI/4. Restore Old Atlas collections</a>

For Atlas, use `old_` prefix for all 3 collections that you need to create  and use `atlas_configs` config set.

Create a collection for restoring the backup (`old_ranger_audits`)
```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
NUM_SHARDS=... # use that number that was used for the old collection - important to use at least that many that you have originally before backup
NUM_REP=1 # can be more, but 1 is recommended for that temp collection
MAX_SHARDS_PER_NODE=... # use that number that was used for the old collection
CONFIG_NAME=atlas_configs

# kinit only if kerberos is enabled for tha cluster
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

OLD_DATA_COLLECTION=old_fulltext_index
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=CREATE&name=$OLD_DATA_COLLECTION&numShards=$NUM_SHARDS&replicationFactor=$NUM_REP&maxShardsPerNode=$MAX_SHARDS_PER_NODE&collection.configName=$CONFIG_NAME"
OLD_DATA_COLLECTION=old_edge_index
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=CREATE&name=$OLD_DATA_COLLECTION&numShards=$NUM_SHARDS&replicationFactor=$NUM_REP&maxShardsPerNode=$MAX_SHARDS_PER_NODE&collection.configName=$CONFIG_NAME"
OLD_DATA_COLLECTION=old_vertex_index
curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=CREATE&name=$OLD_DATA_COLLECTION&numShards=$NUM_SHARDS&replicationFactor=$NUM_REP&maxShardsPerNode=$MAX_SHARDS_PER_NODE&collection.configName=$CONFIG_NAME"
```

Restore the collection(s): 
(important note: you will need to add `--solr-hdfs-path` option if your index is on HDFS (value can be like: `/user/infra-solr`), which should be the location where your collections are located.)
Example with fulltext_index: (do the same for old_vertex_index and old_edge_index)
```bash
# init ambari parameters
AMBARI_SERVER_HOST=...
AMBARI_SERVER_PORT=...
CLUSTER_NAME=...
AMBARI_USERNAME=...
AMBARI_PASSWORD=...

OLD_BACKUP_COLLECTION=old_fulltext_index
BACKUP_NAME=fulltext_index # or what you set before for backup name during backup step
BACKUP_PATH=... # backup location, e.g.: /tmp/fulltext_index-backup
NUM_SHARDS=... # important, use a proper number, that will be stored in core.properties files

# use -s or --ssl option if ssl enabled for ambari-server
/usr/lib/ambari-infra-solr-client/migrationHelper.py -H $AMBARI_SERVER_HOST -P $AMBARI_SERVER_PORT -c $CLUSTER_NAME -u $AMBARI_USERNAME -p $AMBARI_PASSWORD --action restore --index-location $BACKUP_PATH --collection $OLD_BACKUP_COLLECTION --backup-name $BACKUP_NAME --shards $NUM_SHARDS
```

Also you can manually run restore commands: ([get core names](#get-core-/-shard-names-with-hosts))

```bash
su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 
BACKUP_PATH=... # backup location, e.g.: /tmp/fulltext_index-backup

OLD_BACKUP_COLLECTION_CORE=... # choose a core to restore
BACKUP_CORE_NAME=... # choose a core from backup cores - you can find these names as : <backup_location>/snapshot.$BACKUP_CORE_NAME

kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)
curl --negotiate -k -u : "$SOLR_URL/$OLD_BACKUP_COLLECTION_CORE/replication?command=RESTORE&location=$BACKUP_PATH&name=$BACKUP_CORE_NAME"
```

Or use simple `cp` or `hdfs dfs -put` commands to copy the migrated cores to the right places.

#### <a id="vi/5.-reload-restored-atlas-collections">VI/5. Reload restored Atlas collections</a>

After the cores are restored you will need to reload the all 3 Atlas collections:

```bash
su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 

# use kinit only if kerberos is enabled
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

OLD_BACKUP_COLLECTION=old_fulltext_index
curl --negotiate -k -u : "$SOLR_URL/admin/collecions?action=RELOAD&name=$OLD_BACKUP_COLLECTION"
OLD_BACKUP_COLLECTION=old_edge_index
curl --negotiate -k -u : "$SOLR_URL/admin/collecions?action=RELOAD&name=$OLD_BACKUP_COLLECTION"
OLD_BACKUP_COLLECTION=old_vertex_index
curl --negotiate -k -u : "$SOLR_URL/admin/collecions?action=RELOAD&name=$OLD_BACKUP_COLLECTION"
```

#### <a id="vi/6.-transport-old-data-to-atlas-collections">VI/6. Transport old data to Atlas collections</a>

In the end, you end up with 6 Atlas collections (vertex_index, old_vertex_index, edge_index, old_edge_index, fulltext_index, old_fulltext_index), in order to drop the restored one, you will need to transfer your old data to the new collection. To achieve this, you can use [solrDataManager.py](#solr-data-manager-script), which is located next to the `migrationHelper.py` script

Example: (with fulltext_index, to the same with edge_index and vertex_index)
```bash
# Init values:
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
INFRA_SOLR_KEYTAB=... # example: /etc/security/keytabs/ambari-infra-solr.service.keytab
INFRA_SOLR_PRINCIPAL=... # example: infra-solr/$(hostname -f)@EXAMPLE.COM
END_DATE=... # example: 2018-02-18T12:00:00.000Z , date until you export data

OLD_COLLECTION=old_fulltext_index
ACTIVE_COLLECTION=fulltext_index
EXCLUDE_FIELDS=_version_ # comma separated exclude fields, at least _version_ is required

DATE_FIELD=timestamp
# infra-solr-data-manager is a symlink points to /usr/lib/ambari-infra-solr-client/solrDataManager.py
infra-solr-data-manager -m archive -v -c $OLD_COLLECTION -s $SOLR_URL -z none -r 10000 -w 100000 -f $DATE_FIELD -e $END_DATE --solr-output-collection $ACTIVE_COLLECTION -k $INFRA_SOLR_KEYTAB -n $INFRA_SOLR_PRINCIPAL --exclude-fields $EXCLUDE_FIELDS

# Or if you want to run the command in the background (with log and pid file):
nohup infra-solr-data-manager -m archive -v -c $OLD_COLLECTION -s $SOLR_URL -z none -r 10000 -w 100000 -f $DATE_FIELD -e $END_DATE --solr-output-collection $ACTIVE_COLLECTION -k $INFRA_SOLR_KEYTAB -n $INFRA_SOLR_PRINCIPAL --exclude-fields $EXCLUDE_FIELDS > /tmp/solr-data-mgr.log 2>&1>& echo $! > /tmp/solr-data-mgr.pid
```

### APPENDIX

#### <a id="get-core-/-shard-names-with-hosts">Get core / shard names with hosts</a>

To get which hosts are related for your collections, you can check the Solr UI (using SPNEGO), or checkout get state.json details using a zookeeper-client or Solr zookeeper api to get state.json details of the collection (`/solr/admin/zookeeper?detail=true&path=/collections/<collection_name>/state.json`)

#### <a id="turn-off-infra-solr-authorization">Turn off Infra Solr Authorization</a>

You can turn off Solr authorization plugin with setting `infra-solr-security-json/content` Ambari configuration to `{"authentication": {"class": "org.apache.solr.security.KerberosPlugin"}}` (with that authentication will be still enabled). Then you will need to restart Solr, as that config is uploaded to the `/infra-solr/security.json` znode during startup. Other option is to use zkcli.sh of an Infra Solr to upload the security.json to the right place:
```bash
# Setup env for zkcli.sh
source /etc/ambari-infra-solr/conf/infra-solr-env.sh
# Run that command only if kerberos is enabled.
export SOLR_ZK_CREDS_AND_ACLS="${SOLR_AUTHENTICATION_OPTS}"
ZK_CONN_STRING=... # connection string -> zookeeper server addresses with the znode, e.g.: c7401.ambari.apache.org:2181/infra-solr

/usr/lib/ambari-infra-solr/server/scripts/cloud-scripts/zkcli.sh -zkhost $ZK_CONN_STRING -cmd put /security.json
  '{"authentication": {"class": "org.apache.solr.security.KerberosPlugin"}}'
```

#### <a id="">Solr Migration Helper Script</a>

`/usr/lib/ambari-infra-solr-client/migrationHelper.py --help`

```text
Usage: migrationHelper.py [options]

Options:
  -h, --help            show this help message and exit
  -H HOST, --host=HOST  hostname for ambari server
  -P PORT, --port=PORT  port number for ambari server
  -c CLUSTER, --cluster=CLUSTER
                        name cluster
  -s, --ssl             use if ambari server using https
  -u USERNAME, --username=USERNAME
                        username for accessing ambari server
  -p PASSWORD, --password=PASSWORD
                        password for accessing ambari server
  -a ACTION, --action=ACTION
                        backup | restore | migrate
  -f, --force           force index upgrade even if it's the right version
  --index-location=INDEX_LOCATION
                        location of the index backups
  --backup-name=BACKUP_NAME
                        backup name of the index
  --collection=COLLECTION
                        solr collection
  --version=INDEX_VERSION
                        lucene index version for migration (6.6.2 or 7.3.0)
  --request-tries=REQUEST_TRIES
                        number of tries for BACKUP/RESTORE status api calls in
                        the request
  --request-time-interval=REQUEST_TIME_INTERVAL
                        time interval between BACKUP/RESTORE status api calls
                        in the request
  --request-async       skip BACKUP/RESTORE status api calls from the command
  --shared-fs           shared fs for storing backup (will create index
                        location to <path><hostname>)
  --solr-hosts=SOLR_HOSTS
                        comma separated list of solr hosts
  --disable-solr-host-check
                        Disable to check solr hosts are good for the
                        collection backups
  --core-filter=CORE_FILTER
                        core filter for replica folders
  --skip-cores=SKIP_CORES
                        specific cores to skip (comma separated)
  --shards=SOLR_SHARDS  number of shards (required to set properly for
                        restore)
  --solr-hdfs-path=SOLR_HDFS_PATH
                        Base path of Solr (where collections are located) if
                        HDFS is used (like /user/infra-solr)
  --solr-keep-backup    If it is turned on, Snapshot Solr data will not be
                        deleted from the filesystem during restore.
```

#### <a id="">Solr Data Manager Script</a>

`/usr/lib/ambari-infra-solr-client/solrDataManager.py --help`

```text
Usage: solrDataManager.py [options]

Options:
  --version             show program's version number and exit
  -h, --help            show this help message and exit
  -m MODE, --mode=MODE  archive | delete | save
  -s SOLR_URL, --solr-url=SOLR_URL
                        the url of the solr server including the port
  -c COLLECTION, --collection=COLLECTION
                        the name of the solr collection
  -f FILTER_FIELD, --filter-field=FILTER_FIELD
                        the name of the field to filter on
  -r READ_BLOCK_SIZE, --read-block-size=READ_BLOCK_SIZE
                        block size to use for reading from solr
  -w WRITE_BLOCK_SIZE, --write-block-size=WRITE_BLOCK_SIZE
                        number of records in the output files
  -i ID_FIELD, --id-field=ID_FIELD
                        the name of the id field
  -o DATE_FORMAT, --date-format=DATE_FORMAT
                        the date format to use for --days
  -q ADDITIONAL_FILTER, --additional-filter=ADDITIONAL_FILTER
                        additional solr filter
  -j NAME, --name=NAME  name included in result files
  -g, --ignore-unfinished-uploading
  --json-file           create a json file instead of line delimited json
  -z COMPRESSION, --compression=COMPRESSION
                        none | tar.gz | tar.bz2 | zip | gz
  -k SOLR_KEYTAB, --solr-keytab=SOLR_KEYTAB
                        the keytab for a kerberized solr
  -n SOLR_PRINCIPAL, --solr-principal=SOLR_PRINCIPAL
                        the principal for a kerberized solr
  -a HDFS_KEYTAB, --hdfs-keytab=HDFS_KEYTAB
                        the keytab for a kerberized hdfs
  -l HDFS_PRINCIPAL, --hdfs-principal=HDFS_PRINCIPAL
                        the principal for a kerberized hdfs
  -u HDFS_USER, --hdfs-user=HDFS_USER
                        the user for accessing hdfs
  -p HDFS_PATH, --hdfs-path=HDFS_PATH
                        the hdfs path to upload to
  -t KEY_FILE_PATH, --key-file-path=KEY_FILE_PATH
                        the file that contains S3 <accessKey>,<secretKey>
  -b BUCKET, --bucket=BUCKET
                        the bucket name for S3 upload
  -y KEY_PREFIX, --key-prefix=KEY_PREFIX
                        the key prefix for S3 upload
  -x LOCAL_PATH, --local-path=LOCAL_PATH
                        the local path to save the files to
  -v, --verbose
  --solr-output-collection=SOLR_OUTPUT_COLLECTION
                        target output solr collection for archive
  --exclude-fields=EXCLUDE_FIELDS
                        Comma separated list of excluded fields from json
                        response

  specifying the end of the range:
    -e END, --end=END     end of the range
    -d DAYS, --days=DAYS  number of days to keep
```