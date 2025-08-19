--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- Apache Ambari Database Initialization Script for PostgreSQL
-- This script initializes the Ambari database with required schemas and data

\echo 'Initializing Ambari database...'

-- Set client encoding
SET client_encoding = 'UTF8';

-- Create additional databases if needed
-- CREATE DATABASE ambari_metrics OWNER ambari;

-- Connect to ambari database
\c ambari;

-- Set search path
SET search_path = public;

-- Create extensions if available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create basic tables for Ambari (simplified schema for Docker setup)
-- Note: The full schema will be created by Ambari Server setup process

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    user_password VARCHAR(255),
    user_type VARCHAR(255) NOT NULL DEFAULT 'LOCAL',
    ldap_user BOOLEAN NOT NULL DEFAULT FALSE,
    user_active BOOLEAN NOT NULL DEFAULT TRUE,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin user (password: admin)
INSERT INTO users (user_name, user_password, user_type, ldap_user, user_active, admin)
VALUES ('admin', 'c7063d9d94f7ac7d8c6e6d3b429c7c8d', 'LOCAL', FALSE, TRUE, TRUE)
ON CONFLICT (user_name) DO NOTHING;

-- Clusters table
CREATE TABLE IF NOT EXISTS clusters (
    cluster_id BIGSERIAL PRIMARY KEY,
    cluster_name VARCHAR(255) NOT NULL UNIQUE,
    cluster_info VARCHAR(255) NOT NULL DEFAULT 'Ambari',
    desired_cluster_state VARCHAR(255) NOT NULL DEFAULT 'INIT',
    desired_stack_version VARCHAR(255) NOT NULL DEFAULT '{"stackName":"HDP","stackVersion":"2.6"}',
    cluster_state VARCHAR(255) NOT NULL DEFAULT 'INIT',
    provisioning_state VARCHAR(255) NOT NULL DEFAULT 'INIT',
    security_type VARCHAR(255) NOT NULL DEFAULT 'NONE',
    version VARCHAR(255) NOT NULL DEFAULT '1.0.0'
);

-- Hosts table
CREATE TABLE IF NOT EXISTS hosts (
    host_id BIGSERIAL PRIMARY KEY,
    host_name VARCHAR(255) NOT NULL UNIQUE,
    cpu_count INTEGER NOT NULL DEFAULT 1,
    cpu_info VARCHAR(255) NOT NULL DEFAULT 'Unknown',
    discovery_status VARCHAR(2000) NOT NULL DEFAULT 'PENDING',
    host_attributes TEXT,
    ipv4 VARCHAR(255),
    ipv6 VARCHAR(255),
    last_registration_time BIGINT,
    os_arch VARCHAR(255) NOT NULL DEFAULT 'x86_64',
    os_info VARCHAR(1000) NOT NULL DEFAULT 'Unknown',
    os_type VARCHAR(255) NOT NULL DEFAULT 'Unknown',
    ph_cpu_count INTEGER,
    public_host_name VARCHAR(255),
    rack_info VARCHAR(255) NOT NULL DEFAULT '/default-rack',
    total_mem BIGINT NOT NULL DEFAULT 0
);

-- Services table
CREATE TABLE IF NOT EXISTS clusterservices (
    service_name VARCHAR(255) NOT NULL,
    cluster_id BIGINT NOT NULL,
    service_enabled INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (service_name, cluster_id),
    FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id)
);

-- Components table
CREATE TABLE IF NOT EXISTS servicecomponentdesiredstate (
    component_name VARCHAR(255) NOT NULL,
    cluster_id BIGINT NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    desired_state VARCHAR(255) NOT NULL DEFAULT 'INIT',
    desired_stack_version VARCHAR(255) NOT NULL,
    PRIMARY KEY (component_name, cluster_id, service_name),
    FOREIGN KEY (cluster_id, service_name) REFERENCES clusterservices(cluster_id, service_name)
);

-- Host components table
CREATE TABLE IF NOT EXISTS hostcomponentdesiredstate (
    cluster_id BIGINT NOT NULL,
    component_name VARCHAR(255) NOT NULL,
    host_id BIGINT NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    desired_state VARCHAR(255) NOT NULL DEFAULT 'INIT',
    desired_stack_version VARCHAR(255) NOT NULL,
    admin_state VARCHAR(255),
    maintenance_state VARCHAR(255) NOT NULL DEFAULT 'OFF',
    restart_required BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (cluster_id, component_name, host_id, service_name),
    FOREIGN KEY (host_id) REFERENCES hosts(host_id),
    FOREIGN KEY (cluster_id, service_name) REFERENCES clusterservices(cluster_id, service_name)
);

-- Configuration tables
CREATE TABLE IF NOT EXISTS clusterconfig (
    config_id BIGSERIAL PRIMARY KEY,
    version_tag VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    type_name VARCHAR(255) NOT NULL,
    cluster_id BIGINT NOT NULL,
    config_data TEXT NOT NULL,
    config_attributes TEXT,
    create_timestamp BIGINT NOT NULL,
    selected SMALLINT NOT NULL DEFAULT 0,
    selected_timestamp BIGINT NOT NULL DEFAULT 0,
    unmapped SMALLINT NOT NULL DEFAULT 0,
    FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id),
    UNIQUE (cluster_id, type_name, version_tag)
);

-- Permissions and privileges tables
CREATE TABLE IF NOT EXISTS adminprincipal (
    principal_id BIGSERIAL PRIMARY KEY,
    principal_type_id INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS adminprincipaltype (
    principal_type_id SERIAL PRIMARY KEY,
    principal_type_name VARCHAR(255) NOT NULL UNIQUE
);

-- Insert default principal types
INSERT INTO adminprincipaltype (principal_type_name) VALUES ('USER') ON CONFLICT DO NOTHING;
INSERT INTO adminprincipaltype (principal_type_name) VALUES ('GROUP') ON CONFLICT DO NOTHING;
INSERT INTO adminprincipaltype (principal_type_name) VALUES ('ROLE') ON CONFLICT DO NOTHING;

-- Create principal for admin user
INSERT INTO adminprincipal (principal_type_id)
SELECT principal_type_id FROM adminprincipaltype WHERE principal_type_name = 'USER'
ON CONFLICT DO NOTHING;

-- Repositories table
CREATE TABLE IF NOT EXISTS repo_version (
    repo_version_id BIGSERIAL PRIMARY KEY,
    stack_id BIGINT NOT NULL,
    version VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    repo_type VARCHAR(255) NOT NULL DEFAULT 'STANDARD',
    version_url VARCHAR(1024),
    version_xml TEXT,
    version_xsd VARCHAR(512),
    parent_id BIGINT,
    hidden SMALLINT NOT NULL DEFAULT 0,
    resolved SMALLINT NOT NULL DEFAULT 0,
    legacy SMALLINT NOT NULL DEFAULT 1,
    UNIQUE (stack_id, version)
);

-- Stacks table
CREATE TABLE IF NOT EXISTS stack (
    stack_id BIGSERIAL PRIMARY KEY,
    stack_name VARCHAR(255) NOT NULL,
    stack_version VARCHAR(255) NOT NULL,
    UNIQUE (stack_name, stack_version)
);

-- Insert default stack
INSERT INTO stack (stack_name, stack_version) VALUES ('HDP', '2.6') ON CONFLICT DO NOTHING;

-- Metrics tables (simplified)
CREATE TABLE IF NOT EXISTS metricaggregate (
    metric_name VARCHAR(255) NOT NULL,
    app_id VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255),
    start_time BIGINT NOT NULL,
    units VARCHAR(255),
    metric_sum DOUBLE PRECISION,
    metric_count INTEGER,
    metric_max DOUBLE PRECISION,
    metric_min DOUBLE PRECISION,
    PRIMARY KEY (metric_name, app_id, instance_id, start_time)
);

-- Views tables
CREATE TABLE IF NOT EXISTS viewmain (
    view_name VARCHAR(255) NOT NULL,
    label VARCHAR(255),
    description VARCHAR(2048),
    version VARCHAR(255),
    build VARCHAR(128),
    resource_type_id INTEGER,
    icon VARCHAR(255),
    icon64 VARCHAR(255),
    archive VARCHAR(255),
    mask VARCHAR(255),
    system_view SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (view_name)
);

-- Alerts tables (simplified)
CREATE TABLE IF NOT EXISTS alert_definition (
    definition_id BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT NOT NULL,
    definition_name VARCHAR(255) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    component_name VARCHAR(255),
    scope VARCHAR(255) DEFAULT 'ANY',
    label VARCHAR(255),
    description TEXT,
    enabled SMALLINT NOT NULL DEFAULT 1,
    schedule_interval INTEGER NOT NULL,
    source_type VARCHAR(255) NOT NULL,
    alert_source TEXT NOT NULL,
    hash VARCHAR(64) NOT NULL,
    ignore_host SMALLINT NOT NULL DEFAULT 0,
    help_url VARCHAR(512),
    repeat_tolerance INTEGER NOT NULL DEFAULT 1,
    repeat_tolerance_enabled SMALLINT NOT NULL DEFAULT 0,
    FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id),
    UNIQUE (cluster_id, definition_name)
);

-- Topology tables
CREATE TABLE IF NOT EXISTS topology_request (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    cluster_id BIGINT NOT NULL,
    bp_name VARCHAR(100) NOT NULL,
    cluster_properties TEXT,
    cluster_attributes TEXT,
    description VARCHAR(1024),
    provision_action VARCHAR(255),
    FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id)
);

-- Upgrade tables
CREATE TABLE IF NOT EXISTS upgrade (
    upgrade_id BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    from_version VARCHAR(255) NOT NULL DEFAULT '',
    to_version VARCHAR(255) NOT NULL DEFAULT '',
    direction VARCHAR(255) NOT NULL DEFAULT 'UPGRADE',
    upgrade_package VARCHAR(255) NOT NULL,
    upgrade_type VARCHAR(32) NOT NULL,
    repo_version_id BIGINT NOT NULL,
    skip_failures SMALLINT NOT NULL DEFAULT 0,
    skip_service_check_failures SMALLINT NOT NULL DEFAULT 0,
    downgrade_allowed SMALLINT NOT NULL DEFAULT 1,
    suspended SMALLINT NOT NULL DEFAULT 0,
    FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id),
    FOREIGN KEY (repo_version_id) REFERENCES repo_version(repo_version_id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_user_name ON users(user_name);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_clusters_cluster_name ON clusters(cluster_name);
CREATE INDEX IF NOT EXISTS idx_hosts_host_name ON hosts(host_name);
CREATE INDEX IF NOT EXISTS idx_clusterservices_cluster_id ON clusterservices(cluster_id);
CREATE INDEX IF NOT EXISTS idx_clusterconfig_cluster_id ON clusterconfig(cluster_id);
CREATE INDEX IF NOT EXISTS idx_clusterconfig_type_name ON clusterconfig(type_name);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ambari;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ambari;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO ambari;

-- Create a function to update modified timestamps
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified_date = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for timestamp updates
DROP TRIGGER IF EXISTS update_users_modtime ON users;
CREATE TRIGGER update_users_modtime 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- Insert some sample data for development
DO $$
BEGIN
    -- Insert sample cluster if not exists
    IF NOT EXISTS (SELECT 1 FROM clusters WHERE cluster_name = 'development') THEN
        INSERT INTO clusters (cluster_name, cluster_info, desired_stack_version)
        VALUES ('development', 'Development Cluster', '{"stackName":"HDP","stackVersion":"2.6"}');
    END IF;
    
    -- Insert localhost as a host if not exists
    IF NOT EXISTS (SELECT 1 FROM hosts WHERE host_name = 'localhost') THEN
        INSERT INTO hosts (host_name, cpu_count, cpu_info, os_arch, os_type, total_mem, rack_info)
        VALUES ('localhost', 4, 'Intel Core i7', 'x86_64', 'centos7', 8589934592, '/default-rack');
    END IF;
END $$;

-- Create a view for easy cluster monitoring
CREATE OR REPLACE VIEW cluster_summary AS
SELECT 
    c.cluster_id,
    c.cluster_name,
    c.cluster_state,
    c.desired_cluster_state,
    c.security_type,
    COUNT(DISTINCT h.host_id) as host_count,
    COUNT(DISTINCT cs.service_name) as service_count
FROM clusters c
LEFT JOIN hosts h ON TRUE  -- All hosts for now, should join through cluster_host_mapping
LEFT JOIN clusterservices cs ON c.cluster_id = cs.cluster_id
GROUP BY c.cluster_id, c.cluster_name, c.cluster_state, c.desired_cluster_state, c.security_type;

-- Create a function to get cluster health
CREATE OR REPLACE FUNCTION get_cluster_health(cluster_name_param VARCHAR)
RETURNS TABLE (
    cluster_name VARCHAR,
    total_hosts BIGINT,
    total_services BIGINT,
    cluster_state VARCHAR,
    last_updated TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.cluster_name,
        COUNT(DISTINCT h.host_id) as total_hosts,
        COUNT(DISTINCT cs.service_name) as total_services,
        c.cluster_state,
        CURRENT_TIMESTAMP as last_updated
    FROM clusters c
    LEFT JOIN hosts h ON TRUE
    LEFT JOIN clusterservices cs ON c.cluster_id = cs.cluster_id
    WHERE c.cluster_name = cluster_name_param
    GROUP BY c.cluster_name, c.cluster_state;
END;
$$ LANGUAGE plpgsql;

-- Set up database maintenance
-- Create a function to clean old metrics data
CREATE OR REPLACE FUNCTION cleanup_old_metrics(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_time BIGINT;
BEGIN
    -- Calculate cutoff time (current time - days_to_keep in milliseconds)
    cutoff_time := EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - INTERVAL '1 day' * days_to_keep) * 1000;
    
    -- Delete old metrics
    DELETE FROM metricaggregate WHERE start_time < cutoff_time;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create database statistics view
CREATE OR REPLACE VIEW database_stats AS
SELECT 
    schemaname,
    tablename,
    attname as column_name,
    n_distinct,
    correlation
FROM pg_stats 
WHERE schemaname = 'public'
ORDER BY schemaname, tablename, attname;

\echo 'Ambari database initialization completed successfully!'
\echo 'Default admin user created: admin/admin'
\echo 'Sample development cluster created'
\echo 'Database is ready for Ambari Server setup'
