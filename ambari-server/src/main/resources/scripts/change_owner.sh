#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

usage()
{
cat << EOF
usage: $0 options

This script set ownership for all table, sequence and views for a given database

OPTIONS:
   -h      Show this message
   -d      Database name
   -o      Owner
   -s      Schema (defaults to public)
EOF
}

DB_NAME="";
NEW_OWNER="";
SCHEMA="public";
while getopts "hd:o:s:" OPTION; do
    case $OPTION in
        h)
            usage;
            exit 1;
            ;;
        d)
            DB_NAME=$OPTARG;
            ;;
        o)
            NEW_OWNER=$OPTARG;
            ;;
        s)
            SCHEMA=$OPTARG;
            ;;
    esac
done

if [[ -z $DB_NAME ]] || [[ -z $NEW_OWNER ]]; then
     usage;
     exit 1;
fi

# Using the NULL byte as the separator as its the only character disallowed from PG table names
IFS=\0;
for tbl in `psql -qAt -R\0 -c "SELECT tablename FROM pg_tables WHERE schemaname = '${SCHEMA}';" ${DB_NAME}` \
           `psql -qAt -R\0 -c "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = '${SCHEMA}';" ${DB_NAME}` \
           `psql -qAt -R\0 -c "SELECT table_name FROM information_schema.views WHERE table_schema = '${SCHEMA}';" ${DB_NAME}` ;
do
    psql -c "ALTER TABLE \"${SCHEMA}\".\"$tbl\" OWNER TO ${NEW_OWNER}" ${DB_NAME};
done
unset IFS;