-- Table SEQUENCE_TABLE is an internal table required by DataNucleus.
-- NOTE: Some versions of SchemaTool do not automatically generate this table.
-- See http://www.datanucleus.org/servlet/jira/browse/NUCRDBMS-416
CREATE TABLE SEQUENCE_TABLE
(
   SEQUENCE_NAME VARCHAR2(255) NOT NULL,
   NEXT_VAL NUMBER NOT NULL
);

ALTER TABLE SEQUENCE_TABLE ADD CONSTRAINT PART_TABLE_PK PRIMARY KEY (SEQUENCE_NAME);

-- Table NUCLEUS_TABLES is an internal table required by DataNucleus.
-- This table is required if datanucleus.autoStartMechanism=SchemaTable
-- NOTE: Some versions of SchemaTool do not automatically generate this table.
-- See http://www.datanucleus.org/servlet/jira/browse/NUCRDBMS-416
CREATE TABLE NUCLEUS_TABLES
(
   CLASS_NAME VARCHAR2(128) NOT NULL,
   TABLE_NAME VARCHAR2(128) NOT NULL,
   TYPE VARCHAR2(4) NOT NULL,
   OWNER VARCHAR2(2) NOT NULL,
   VERSION VARCHAR2(20) NOT NULL,
   INTERFACE_NAME VARCHAR2(255) NULL
);

ALTER TABLE NUCLEUS_TABLES ADD CONSTRAINT NUCLEUS_TABLES_PK PRIMARY KEY (CLASS_NAME);

-- Table TBLS for classes [org.apache.hadoop.hive.metastore.model.MTable]
CREATE TABLE TBLS
(
    TBL_ID NUMBER NOT NULL,
    CREATE_TIME NUMBER (10) NOT NULL,
    DB_ID NUMBER NULL,
    LAST_ACCESS_TIME NUMBER (10) NOT NULL,
    OWNER VARCHAR2(767) NULL,
    RETENTION NUMBER (10) NOT NULL,
    SD_ID NUMBER NULL,
    TBL_NAME VARCHAR2(128) NULL,
    TBL_TYPE VARCHAR2(128) NULL,
    VIEW_EXPANDED_TEXT CLOB NULL,
    VIEW_ORIGINAL_TEXT CLOB NULL
);

ALTER TABLE TBLS ADD CONSTRAINT TBLS_PK PRIMARY KEY (TBL_ID);
