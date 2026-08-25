#!/usr/bin/env node

/*
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

import fs from 'node:fs';
import path from 'node:path';
import {
  calculateReviewedDiffSha256,
  calculateStatistics,
  emberBaselineRoot,
  featureIndexFile,
  frontendRefactorRoot,
  inferLegacyEvidence,
  ISSUE_REQUIRED_STATUSES,
  LEGACY_EVIDENCE,
  MATRIX_COLUMNS,
  METRICS_EXCLUDED_FEATURE_IDS,
  matrixFiles,
  parseCsv,
  reactCurrentRoot,
  REACT_STATUSES,
  renderCsv,
  renderMarkdown,
  REVIEWED_PATHS,
  readJson,
} from './react-parity-matrix-lib.mjs';

const errors = [];
const REQUIRED_FEATURE_COUNT = 1154;
const GAP_STATUSES = new Set([...ISSUE_REQUIRED_STATUSES, 'NOT_APPLICABLE']);

function add(message) {
  errors.push(message);
}

function sortObject(value) {
  if (Array.isArray(value)) return value.map(sortObject);
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, child]) => [key, sortObject(child)]),
    );
  }
  return value;
}

function stableJson(value) {
  return JSON.stringify(sortObject(value));
}

function validateRow(row, feature, index) {
  const label = `rows[${index}] (${row?.feature_id ?? 'unknown'})`;
  if (!row || typeof row !== 'object' || Array.isArray(row)) {
    add(`${label} is not an object`);
    return;
  }
  for (const column of MATRIX_COLUMNS) {
    if (!(column in row)) add(`${label} is missing ${column}`);
    else if (typeof row[column] !== 'string') add(`${label}.${column} must be a string`);
  }
  if (!feature) return;
  const expectedLegacyDoc = `ember-baseline/${feature.moduleFile}:${feature.line}`;
  if (row.legacy_summary !== feature.summary) add(`${label}.legacy_summary differs from the canonical feature index`);
  if (row.legacy_doc !== expectedLegacyDoc) add(`${label}.legacy_doc must be ${expectedLegacyDoc}`);
  if (!LEGACY_EVIDENCE.has(row.legacy_evidence)) add(`${label} has illegal legacy_evidence ${row.legacy_evidence}`);
  if (!REACT_STATUSES.has(row.react_status)) add(`${label} has illegal react_status ${row.react_status}`);

  const requiredTextFields = [
    'feature_id',
    'legacy_summary',
    'legacy_doc',
    'legacy_evidence',
    'react_status',
    'source_status',
    'source_location',
    'condition_evidence',
    'happy_path_test',
    'failure_recovery_test',
    'reviewed_commit',
    'reviewed_by',
  ];
  for (const field of requiredTextFields) {
    if (typeof row[field] === 'string' && !row[field].trim()) add(`${label}.${field} must not be empty`);
  }
  if (!/^[0-9a-f]{40}$/.test(row.reviewed_commit)) add(`${label}.reviewed_commit must be a full Git commit`);
  if (GAP_STATUSES.has(row.react_status)) {
    if (!String(row.issue ?? '').trim()) add(`${label}.issue is required for ${row.react_status}`);
    if (!String(row.differences ?? '').trim()) add(`${label}.differences is required for ${row.react_status}`);
  }
  if (row.react_status === 'NEEDS_RUNTIME_VALIDATION' && !String(row.runtime_scenario ?? '').trim()) {
    add(`${label}.runtime_scenario is required for NEEDS_RUNTIME_VALIDATION`);
  }
  if (row.react_status === 'COVERED') {
    const combinedTests = `${row.happy_path_test} ${row.failure_recovery_test}`;
    if (row.source_status !== 'RUNTIME_VALIDATED') {
      add(`${label} cannot be COVERED without explicit RUNTIME_VALIDATED source status`);
    }
    if (!String(row.runtime_scenario ?? '').trim()) {
      add(`${label}.runtime_scenario must record the COVERED acceptance result`);
    }
    if (/not independently established|not implemented/i.test(combinedTests)) {
      add(`${label} cannot be COVERED without independent normal and failure/recovery evidence`);
    }
  }
  if (row.legacy_evidence === 'OUT_OF_SCOPE') {
    if (row.react_status !== 'NOT_APPLICABLE') add(`${label} scope exclusion must be NOT_APPLICABLE`);
    const expectedDescription = METRICS_EXCLUDED_FEATURE_IDS.has(row.feature_id)
      ? /metrics exclusion/i
      : /scope exclusion/i;
    if (!expectedDescription.test(row.differences)) add(`${label} scope exclusion must be explicit in differences`);
  }
  if (METRICS_EXCLUDED_FEATURE_IDS.has(row.feature_id)) {
    if (row.legacy_evidence !== 'OUT_OF_SCOPE') add(`${label} known Metrics exclusion must use OUT_OF_SCOPE evidence`);
    if (row.react_status !== 'NOT_APPLICABLE') add(`${label} known Metrics exclusion must be NOT_APPLICABLE`);
  }
  if (row.legacy_evidence === 'PLACEHOLDER' && row.react_status !== 'NOT_APPLICABLE') {
    add(`${label} legacy PLACEHOLDER must be NOT_APPLICABLE`);
  }
}

function validateSourceLocation(row, index) {
  const match = row.source_location.match(/^(.*):(\d+)$/);
  if (!match) {
    add(`rows[${index}].source_location is invalid: ${row.source_location}`);
    return;
  }
  const file = path.join(frontendRefactorRoot, match[1]);
  if (!fs.existsSync(file)) {
    add(`rows[${index}].source_location points to missing ${match[1]}`);
    return;
  }
  const line = Number(match[2]);
  const sourceLine = fs.readFileSync(file, 'utf8').split(/\r?\n/)[line - 1];
  if (!sourceLine) add(`rows[${index}].source_location points outside ${match[1]}`);
  else if (!sourceLine.includes(row.feature_id)) add(`rows[${index}].source_location line does not contain ${row.feature_id}`);
}

function validateCsv(jsonRows) {
  if (!fs.existsSync(matrixFiles.csv)) {
    add('react-feature-parity-matrix.csv is missing');
    return;
  }
  let rows;
  try {
    rows = parseCsv(fs.readFileSync(matrixFiles.csv, 'utf8'));
  } catch (error) {
    add(`CSV is invalid: ${error.message}`);
    return;
  }
  if (rows.length && JSON.stringify(Object.keys(rows[0])) !== JSON.stringify(MATRIX_COLUMNS)) {
    add('CSV columns do not match the matrix schema or order');
  }
  if (fs.readFileSync(matrixFiles.csv, 'utf8') !== renderCsv(jsonRows)) {
    add('CSV is not the canonical rendering of the JSON rows');
  }
  if (rows.length !== jsonRows.length) add(`CSV has ${rows.length} rows; JSON has ${jsonRows.length}`);
  for (let index = 0; index < Math.min(rows.length, jsonRows.length); index += 1) {
    for (const column of MATRIX_COLUMNS) {
      if (rows[index][column] !== jsonRows[index][column]) {
        add(`CSV row ${index + 2} ${column} differs from JSON for ${jsonRows[index].feature_id}`);
        break;
      }
    }
  }
}

function validateMarkdown(matrix) {
  if (!fs.existsSync(matrixFiles.markdown)) {
    add('react-feature-parity-matrix.md is missing');
    return;
  }
  const source = fs.readFileSync(matrixFiles.markdown, 'utf8');
  if (!source.startsWith('<!---\n   Licensed to the Apache Software Foundation')) {
    add('Markdown matrix is missing the ASF license header');
  }
  if (source !== renderMarkdown(matrix)) {
    add('Markdown is not the canonical rendering of the JSON matrix');
  }
  const jsonRows = matrix.rows;
  const featureIds = new Set(jsonRows.map((row) => row.feature_id));
  const rendered = [];
  let inFeatureRows = false;
  for (const line of source.split(/\r?\n/)) {
    if (line === '## Feature Rows') {
      inFeatureRows = true;
      continue;
    }
    if (!inFeatureRows) continue;
    const match = line.match(/^\|\s*([A-Z][A-Z0-9-]+)\s*\|.*?\|\s*`([A-Z_]+)`\s*\|\s*`([A-Z_]+)`\s*\|/);
    if (match) rendered.push({ id: match[1], legacyEvidence: match[2], reactStatus: match[3] });
  }
  const counts = new Map();
  for (const row of rendered) counts.set(row.id, (counts.get(row.id) ?? 0) + 1);
  for (const row of jsonRows) {
    if (!counts.has(row.feature_id)) add(`Markdown is missing ${row.feature_id}`);
    else if (counts.get(row.feature_id) !== 1) add(`Markdown contains ${row.feature_id} ${counts.get(row.feature_id)} times`);
    const renderedRow = rendered.find((candidate) => candidate.id === row.feature_id);
    if (renderedRow && renderedRow.legacyEvidence !== row.legacy_evidence) {
      add(`Markdown legacy evidence differs from JSON for ${row.feature_id}`);
    }
    if (renderedRow && renderedRow.reactStatus !== row.react_status) {
      add(`Markdown React status differs from JSON for ${row.feature_id}`);
    }
  }
  for (const id of counts.keys()) {
    if (!featureIds.has(id)) add(`Markdown contains unknown feature ID ${id}`);
  }
}

try {
  if (!fs.existsSync(matrixFiles.json)) throw new Error('react-feature-parity-matrix.json is missing');
  const features = readJson(featureIndexFile);
  if (!Array.isArray(features)) throw new Error('feature-index.json must contain an array');
  if (features.length !== REQUIRED_FEATURE_COUNT) add(`Canonical feature index has ${features.length} IDs; expected ${REQUIRED_FEATURE_COUNT}`);
  const featureById = new Map(features.map((feature) => [feature.id, feature]));
  if (featureById.size !== features.length) add('Canonical feature index contains duplicate IDs');

  const matrix = readJson(matrixFiles.json);
  if (!matrix || typeof matrix !== 'object' || Array.isArray(matrix)) throw new Error('Matrix JSON must contain an object');
  if (!String(matrix.license ?? '').includes('Apache License, Version 2.0')) add('Matrix JSON license metadata is missing');
  if (matrix.schema_version !== 1) add('Matrix JSON schema_version must be 1');
  if (matrix.review_basis !== 'base_commit_plus_audited_worktree_diff') {
    add('Matrix JSON review_basis must describe the base commit plus audited working-tree diff');
  }
  if (!/^[0-9a-f]{40}$/.test(matrix.reviewed_commit ?? '')) {
    add('Matrix JSON reviewed_commit must be a full Git commit');
  }
  if (!String(matrix.reviewed_by ?? '').trim()) {
    add('Matrix JSON reviewed_by must not be empty');
  }
  if (JSON.stringify(matrix.reviewed_paths) !== JSON.stringify(REVIEWED_PATHS)) {
    add('Matrix JSON reviewed_paths must match the canonical audited paths');
  }
  if (!/^[0-9a-f]{64}$/.test(matrix.reviewed_diff_sha256 ?? '')) {
    add('Matrix JSON reviewed_diff_sha256 must be a SHA-256 value');
  } else if (/^[0-9a-f]{40}$/.test(matrix.reviewed_commit ?? '')) {
    const currentDiffHash = calculateReviewedDiffSha256(matrix.reviewed_commit);
    if (currentDiffHash !== matrix.reviewed_diff_sha256) {
      add('Matrix reviewed_diff_sha256 is stale; audited frontend or comparison files changed after generation');
    }
  }
  if (!Array.isArray(matrix.rows)) throw new Error('Matrix JSON must contain a rows array');
  const rows = matrix.rows;
  if (rows.length !== features.length) add(`Matrix has ${rows.length} rows; expected ${features.length}`);

  const seen = new Map();
  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index];
    const feature = featureById.get(row?.feature_id);
    if (!feature) add(`rows[${index}] has unknown feature ID ${row?.feature_id}`);
    if (seen.has(row?.feature_id)) add(`Duplicate feature ID ${row.feature_id} at rows ${seen.get(row.feature_id)} and ${index}`);
    else seen.set(row?.feature_id, index);
    if (features[index]?.id !== row?.feature_id) add(`rows[${index}] must be ${features[index]?.id}, found ${row?.feature_id}`);
    validateRow(row, feature, index);
    if (row && typeof row.source_location === 'string') validateSourceLocation(row, index);

    if (feature) {
      const legacyFile = path.join(emberBaselineRoot, feature.moduleFile);
      const legacyLine = fs.readFileSync(legacyFile, 'utf8').split(/\r?\n/)[feature.line - 1];
      let expectedEvidence;
      try {
        expectedEvidence = inferLegacyEvidence(
          feature,
          legacyLine,
          fs.readFileSync(legacyFile, 'utf8').split(/\r?\n/),
        );
      } catch (error) {
        add(error.message);
      }
      if (expectedEvidence && row.legacy_evidence !== expectedEvidence) {
        add(`rows[${index}].legacy_evidence is ${row.legacy_evidence}; expected ${expectedEvidence}`);
      }
    }
  }
  for (const feature of features) {
    if (!seen.has(feature.id)) add(`Missing feature ID ${feature.id}`);
  }
  if (rows.some((row) => row.reviewed_commit !== matrix.reviewed_commit)) add('Row reviewed_commit values differ from matrix metadata');
  if (rows.some((row) => row.reviewed_by !== matrix.reviewed_by)) add('Row reviewed_by values differ from matrix metadata');
  const expectedStatistics = calculateStatistics(rows);
  if (stableJson(matrix.statistics) !== stableJson(expectedStatistics)) add('Matrix statistics are stale or count Metrics exclusions incorrectly');

  validateCsv(rows);
  validateMarkdown(matrix);
} catch (error) {
  add(error.message);
}

if (errors.length) {
  process.stderr.write(`React parity matrix validation failed with ${errors.length} error(s):\n- ${errors.join('\n- ')}\n`);
  process.exitCode = 1;
} else {
  process.stdout.write('React parity matrix validation passed: 1154 canonical feature rows, synchronized JSON/CSV/Markdown, valid statuses, issue references, and Metrics exclusions.\n');
}
