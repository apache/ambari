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
import { execFileSync } from 'node:child_process';
import {
  calculateReviewedDiffSha256,
  createLegacyExclusionRow,
  createMatrixRow,
  emberBaselineRoot,
  featureIndexFile,
  inferLegacyEvidence,
  isLegacyExclusion,
  matrixDocument,
  matrixFiles,
  normalizeEditableRows,
  parseCsv,
  parseGapStatusRows,
  reactCurrentRoot,
  readJson,
  renderCsv,
  renderMarkdown,
  selectAuthoritativeRows,
} from './react-parity-matrix-lib.mjs';

function parseArguments(argv) {
  const result = { source: 'gaps', check: false, issue: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === '--check') result.check = true;
    else if (argument === '--source') result.source = argv[++index];
    else if (argument === '--reviewed-commit') result.reviewedCommit = argv[++index];
    else if (argument === '--reviewed-by') result.reviewedBy = argv[++index];
    else if (argument === '--issue') result.issue = argv[++index];
    else throw new Error(`Unknown argument: ${argument}`);
  }
  if (!['gaps', 'json', 'csv'].includes(result.source)) {
    throw new Error('--source must be gaps, json, or csv');
  }
  if (result.source === 'gaps' && !/^AMBARI-\d+$/.test(result.issue)) {
    throw new Error('--issue must be the unified AMBARI-nnnnn JIRA key when --source gaps is used');
  }
  return result;
}

function currentCommit() {
  return execFileSync('git', ['rev-parse', 'HEAD'], { cwd: reactCurrentRoot, encoding: 'utf8' }).trim();
}

function reviewFromArguments(arguments_, existingMatrix) {
  const commit = arguments_.reviewedCommit ?? existingMatrix?.reviewed_commit ?? currentCommit();
  const by = arguments_.reviewedBy ?? existingMatrix?.reviewed_by ?? `Codex static audit, ${new Date().toISOString().slice(0, 10)}`;
  if (!/^[0-9a-f]{40}$/.test(commit)) throw new Error('--reviewed-commit must be a full 40-character Git commit');
  if (!by.trim()) throw new Error('--reviewed-by must not be empty');
  return { commit, by };
}

function loadFeatures() {
  const features = readJson(featureIndexFile);
  if (!Array.isArray(features)) throw new Error(`${featureIndexFile} must contain an array`);
  const ids = new Set(features.map((feature) => feature.id));
  if (ids.size !== features.length) throw new Error('Legacy feature index contains duplicate IDs');
  return { features, ids };
}

function sourceRowsFromGaps(arguments_, review) {
  const { features, ids } = loadFeatures();
  const moduleSources = new Map();
  const missingModules = new Set();
  const parsedRows = [];
  const errors = [];
  for (const moduleFile of [...new Set(features.map((feature) => feature.moduleFile))]) {
    const gapFile = path.join(reactCurrentRoot, moduleFile.replace(/\.md$/, '-gap.md'));
    moduleSources.set(moduleFile, gapFile);
    if (!fs.existsSync(gapFile)) {
      errors.push(`Missing React comparison document: ${path.relative(reactCurrentRoot, gapFile)}`);
      missingModules.add(moduleFile);
      continue;
    }
    const parsed = parseGapStatusRows(gapFile, ids);
    parsedRows.push(...parsed.rows);
    errors.push(...parsed.errors);
  }

  const selection = selectAuthoritativeRows(parsedRows);
  errors.push(...selection.errors);
  const rows = [];
  for (const feature of features) {
    const legacyFile = path.join(emberBaselineRoot, feature.moduleFile);
    const legacyLines = fs.readFileSync(legacyFile, 'utf8').split(/\r?\n/);
    const legacyLine = legacyLines[feature.line - 1];
    let legacyEvidence;
    try {
      legacyEvidence = inferLegacyEvidence(feature, legacyLine, legacyLines);
    } catch (error) {
      errors.push(error.message);
      continue;
    }
    if (isLegacyExclusion(legacyEvidence)) {
      rows.push(createLegacyExclusionRow(feature, legacyEvidence, legacyLine, review));
      continue;
    }
    const sourceRow = selection.selected.get(feature.id);
    if (!sourceRow) {
      if (missingModules.has(feature.moduleFile)) continue;
      errors.push(`No authoritative React status row for ${feature.id} (${feature.moduleFile}:${feature.line})`);
      continue;
    }
    rows.push(createMatrixRow(feature, legacyEvidence, sourceRow, review, arguments_.issue));
  }

  if (errors.length) {
    throw new Error(`Cannot generate the matrix:\n- ${errors.join('\n- ')}`);
  }
  if (selection.superseded.length) {
    process.stderr.write(`Resolved ${selection.superseded.length} lower-priority status rows using final/post-implementation tables.\n`);
  }
  return rows;
}

function loadExistingJson() {
  if (!fs.existsSync(matrixFiles.json)) throw new Error(`Missing ${matrixFiles.json}`);
  const matrix = readJson(matrixFiles.json);
  if (!Array.isArray(matrix.rows)) throw new Error('Matrix JSON must contain a rows array');
  return matrix;
}

function loadRows(arguments_) {
  const existing = fs.existsSync(matrixFiles.json) ? loadExistingJson() : null;
  const review = reviewFromArguments(arguments_, existing);
  if (arguments_.source === 'gaps') return { rows: sourceRowsFromGaps(arguments_, review), review };
  if (arguments_.source === 'json') {
    const rows = normalizeEditableRows(existing.rows).map((row) => ({
      ...row,
      reviewed_commit: review.commit,
      reviewed_by: review.by,
    }));
    return { rows, review };
  }
  if (!fs.existsSync(matrixFiles.csv)) throw new Error(`Missing ${matrixFiles.csv}`);
  const rows = normalizeEditableRows(parseCsv(fs.readFileSync(matrixFiles.csv, 'utf8'))).map((row) => ({
    ...row,
    reviewed_commit: review.commit,
    reviewed_by: review.by,
  }));
  return { rows, review };
}

function writeOrCheck(file, contents, check, errors) {
  if (check) {
    if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== contents) {
      errors.push(`${path.relative(reactCurrentRoot, file)} is stale`);
    }
    return;
  }
  fs.writeFileSync(file, contents);
}

try {
  const arguments_ = parseArguments(process.argv.slice(2));
  const { rows, review } = loadRows(arguments_);
  const reviewedDiffSha256 = calculateReviewedDiffSha256(review.commit);
  const matrix = matrixDocument(rows, review, reviewedDiffSha256);
  const outputs = {
    [matrixFiles.json]: `${JSON.stringify(matrix, null, 2)}\n`,
    [matrixFiles.csv]: renderCsv(rows),
    [matrixFiles.markdown]: renderMarkdown(matrix),
  };
  const errors = [];
  for (const [file, contents] of Object.entries(outputs)) writeOrCheck(file, contents, arguments_.check, errors);
  if (errors.length) throw new Error(errors.join('\n'));
  process.stdout.write(`${arguments_.check ? 'Checked' : 'Generated'} ${rows.length} React parity rows.\n`);
} catch (error) {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
}
