## Ambari Infra Solr Client

CLI helper tool(s) for Ambari Infra Solr.

### Solr Migration Helper (Solr 5.x to 7.x)

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

#### I. Backup/Migrate/Restore Ranger collection (Ambari 2.6.x to Ambari 2.7.x)

Before you start to upgrade process, check how many shards you have for Ranger collection, in order to know later how many shards you need to create for the collection where you will store the migrated index. Also make sure you have stable shards (at least one core is up and running)

##### 1. Upgrade Ambari Infra Solr Client

First make sure `ambari-infra-solr-client` is the latest. (If its before 2.7.x) It will contain the migrationHelper.py script at `/usr/lib/ambari-infra-solr-client` location. 
Also make sure you won't upgrade `ambari-infra-solr` until the migration has not done. (all of this should happen after `ambari-server` upgrade, also make sure to not restart `INFRA_SOLR` instances)

##### 2. Backup Ranger collection

Use `/usr/lib/ambari-infra-solr-client/migrationHelper.py` script to backup the ranger collection.

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

##### 3. Backuo Ranger configs on Solr ZNode

Next you can copy `ranger_audits` configs to a different znode, in order to keep the old schema.

```bash
export JAVA_HOME=/usr/jdk64/1.8.0_112 # or other jdk8 location
export ZK_CONN_STR=... # without znode, e.g.: myhost1:2181,myhost2:2181,myhost3:2181 
# note 1: --transfer-mode copyToLocal or --transfer-mode copyFromLocal can be used if you want to use the local filesystem
# note 2: use --jaas-file option only if the cluster is kerberized
infra-solr-cloud-cli --transfer-znode -z $ZK_CONN_STR --jaas-file /etc/ambari-infra-solr/conf/infra_solr_jaas.conf --copy-src /infra-solr/configs/ranger_audits --copy-dest /infra-solr/configs/old_ranger_audits
```

##### 4. Delete Ranger Collection

At this point you can delete the actual Ranger collection with this command:

```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
COLLECTION_NAME=ranger_audits

# use kinit and --negotiate option for curl only if the cluster is kerberized
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)

curl --negotiate -k -u : "$SOLR_URL/admin/collections?action=DELETE&name=$COLLECTION_NAME" 
```

##### 5. Upgrade Ranger Solr schema

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
##### 6. Upgrade Infra Solr Packages 

At this step, you will need to upgrade ambari-infra-solr packages as well, but just after that you finished the backup and config upgrades for other collections as well (not just RANGER, do it for ATLAS and LOGSEARCH as well).
So you will need to stop here, and only continue if you are ready with the backup + delete collection part with all of the collections.

Example (for CentOS)
```bash
yum upgrade -y ambari-infra-solr
```

##### 7. Re-create Ranger collections

Just restart Ranger Admin service, as the collection was deleted before, during startup, the new Ranger Solr collection will be created (as a Solr 7 collection)


##### 8. Migrate Ranger index

From this point, you can migrate your old index in the background. On every hosts, where there is a backup located, you can run luce index migration tool (packaged with ambari-infra-solr-client).

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

##### 9. Restore Old Ranger Collection

After you finished your lucene data migration, you can restore your replicas on every hosts where you have the backups. But we need to restore the old data to a new collection, so first you will need to create that: (on a host where you have an installed Infra Solr component). For Ranger, use old_ranger_audits config set that you backup up during Solr schema config upgrade step. (set this as CONFIG_NAME), to make that collection to work with Solr 7, you need to copy your solrconfig.xml as well.

Create a collection for restoring the backup (`old_ranger_audits`)
```bash
su infra-solr # infra-solr user - if you have a custom one, use that
SOLR_URL=... # example: http://c6401.ambari.apache.org:8886/solr
NUM_SHARDS=... # use that number that was used for the old collection - important to use at least that many that you have originally before backup
NUM_REP=1 # can be more, but 1 is recommended for that temp collection
MAX_SHARDS_PER_NODE=... # use that number that was used for the old collection
CONFIG_NAME=old_ranger_audits
OLD_DATA_COLLECTION=old_ranger_audit

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

You will need to add `--solr-hdfs-path` option if your index is on HDFS (value can be like: `/user/infra-solr`), which should be the location where your collections are located.

Also you can manually run restore commands:

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

##### 10. Reload restored collection

After the cores are restored you will need to reload the old_ranger_audits collection:

```bash
su infra-solr
SOLR_URL=... # actual solr host url, example: http://c6401.ambari.apache.org:8886/solr 
OLD_RANGER_COLLECTION=old_ranger_audits

# use kinit only if kerberos is enabled
kinit -kt /etc/security/keytabs/ambari-infra-solr.service.keytab $(whoami)/$(hostname -f)
curl --negotiate -k -u : "$SOLR_URL/admin/collecions?action=RELOAD&name=$OLD_RANGER_COLLECTION"
```

##### 11. Transport old data to ranger_audits collection

In the end, you end up with 2 collections (ranger_audits and old_ranger_audits), in order to drop the restored one, you will need to transfer your old data to the new collection. To achieve this, you can use `solrDataManager.py`, which is located next to the `migrationHelper.py` script

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

### Solr Data Manager

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