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

import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import test from 'node:test';
import {
  calculateReviewedDiffSha256,
  createLegacyExclusionRow,
  inferLegacyEvidence,
  isLegacyExclusion,
  normalizeSourceStatus,
  parseCsv,
  parseGapStatusRows,
  REVIEWED_PATHS,
  renderCsv,
  selectAuthoritativeRows,
  splitMarkdownRow,
} from './react-parity-matrix-lib.mjs';

test('splits escaped pipes and pipes inside code spans', () => {
  assert.deepEqual(
    splitMarkdownRow('| ID | Status | Evidence |'),
    ['ID', 'Status', 'Evidence'],
  );
  assert.deepEqual(
    splitMarkdownRow('| TEST-001 | `MATCH` | `a|b` and a\\|b |'),
    ['TEST-001', '`MATCH`', '`a|b` and a\\|b'],
  );
});

test('normalizes source vocabulary without promoting static evidence to COVERED', () => {
  assert.deepEqual(normalizeSourceStatus('`STATICALLY_ALIGNED`'), {
    sourceStatus: 'STATICALLY_ALIGNED',
    reactStatus: 'NEEDS_RUNTIME_VALIDATION',
  });
  assert.equal(normalizeSourceStatus('COVERED').reactStatus, 'NEEDS_RUNTIME_VALIDATION');
  assert.equal(normalizeSourceStatus('IMPROVED_PARTIAL').reactStatus, 'PARTIAL');
  assert.equal(normalizeSourceStatus('BEHAVIOR_DIFF').reactStatus, 'BEHAVIOR_DIFF');
  assert.equal(normalizeSourceStatus('INCORRECT').reactStatus, 'BEHAVIOR_DIFF');
  assert.equal(normalizeSourceStatus('unknown').reactStatus, null);
});

test('selects post-implementation rows and reports equally authoritative duplicates', () => {
  const selected = selectAuthoritativeRows([
    { featureId: 'TEST-001', sourceFile: 'gap.md', sourceLine: 10, sourceStatus: 'MISSING', priority: 100 },
    { featureId: 'TEST-001', sourceFile: 'gap.md', sourceLine: 30, sourceStatus: 'STATIC_COMPLETE', priority: 600 },
  ]);
  assert.equal(selected.errors.length, 0);
  assert.equal(selected.selected.get('TEST-001').sourceLine, 30);
  assert.equal(selected.superseded.length, 1);

  const conflict = selectAuthoritativeRows([
    { featureId: 'TEST-002', sourceFile: 'gap.md', sourceLine: 10, sourceStatus: 'PARTIAL', priority: 450 },
    { featureId: 'TEST-002', sourceFile: 'gap.md', sourceLine: 11, sourceStatus: 'MISSING', priority: 450 },
  ]);
  assert.equal(conflict.selected.has('TEST-002'), false);
  assert.equal(conflict.errors.length, 1);
});

test('parses only canonical status tables and rejects unsupported statuses', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'ambari-parity-'));
  const file = path.join(root, 'gap.md');
  fs.writeFileSync(file, `# Gap

## Initial Feature Status

| ID | Initial status | Evidence |
| --- | --- | --- |
| TEST-001 | \`MISSING\` | initial |

## Post-Implementation Status

| ID | Final status | Evidence |
| --- | --- | --- |
| TEST-001 | \`STATIC_COMPLETE\` | final |
| TEST-002 | \`NOT_A_STATUS\` | invalid |
| TEST-999 | \`PARTIAL\` | unknown ID |

| ID | Behavior |
| --- | --- |
| TEST-001 | not a status table |
`);
  const parsed = parseGapStatusRows(file, new Set(['TEST-001', 'TEST-002']));
  assert.equal(parsed.rows.length, 2);
  assert.equal(parsed.errors.length, 2);
  assert.match(parsed.errors[0], /NOT_A_STATUS/);
  assert.match(parsed.errors[1], /unknown feature ID TEST-999/);
});

test('round trips quoted CSV fields', () => {
  const row = {
    feature_id: 'TEST-001',
    legacy_summary: 'A value, with "quotes"',
    legacy_doc: 'doc.md:1',
    legacy_evidence: 'CONFIRMED',
    react_status: 'PARTIAL',
    source_status: 'PARTIAL',
    source_location: 'gap.md:1',
    react_route: '',
    react_ui: '',
    react_api: '',
    condition_evidence: 'line one\nline two',
    happy_path_test: 'test',
    failure_recovery_test: 'test',
    differences: 'gap',
    runtime_scenario: '',
    issue: 'AMBARI-1',
    reviewed_commit: 'a'.repeat(40),
    reviewed_by: 'Reviewer',
  };
  assert.deepEqual(parseCsv(renderCsv([row])), [row]);
});

test('infers explicit legacy exclusions and defaults authored rows to CONFIRMED', () => {
  const feature = { id: 'TEST-001', moduleFile: 'test.md', line: 1 };
  assert.equal(inferLegacyEvidence(feature, '| TEST-001 | normal behavior |'), 'CONFIRMED');
  assert.equal(
    inferLegacyEvidence(
      { id: 'ALERT-CREATE-001', moduleFile: 'test.md', line: 1 },
      '| ALERT-CREATE-001 | mentions OUT_OF_SCOPE for only one branch |',
    ),
    'CONFIRMED',
  );
  assert.equal(inferLegacyEvidence(feature, '| TEST-001 | STATIC_ONLY, CONDITIONAL |'), 'STATIC_ONLY');
  assert.equal(inferLegacyEvidence(feature, '| TEST-001 | PLACEHOLDER and STATIC_ONLY |'), 'PLACEHOLDER');
  assert.equal(
    inferLegacyEvidence(
      { id: 'HAWQ-CFG-001', moduleFile: 'test.md', line: 4 },
      '| HAWQ-CFG-001 | property |',
      [
        '> HAWQ evidence level: `CONDITIONAL / STATIC_ONLY`.',
        '',
        '| ID | Behavior |',
        '| HAWQ-CFG-001 | property |',
      ],
    ),
    'STATIC_ONLY',
  );
});

test('treats legacy placeholders and scope exclusions as authoritative exclusions', () => {
  assert.equal(isLegacyExclusion('PLACEHOLDER'), true);
  assert.equal(isLegacyExclusion('OUT_OF_SCOPE'), true);
  assert.equal(isLegacyExclusion('STATIC_ONLY'), false);
  assert.equal(isLegacyExclusion('CONFIRMED'), false);
});

test('keeps the reviewed diff digest stable when untracked files are committed', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'ambari-parity-git-'));
  const runGit = (...arguments_) => execFileSync('git', arguments_, { cwd: root });
  fs.mkdirSync(path.join(root, 'src'));
  fs.writeFileSync(path.join(root, 'src/changed.txt'), 'before\n');
  fs.writeFileSync(path.join(root, 'src/deleted.txt'), 'delete me\n');
  runGit('init', '--quiet');
  runGit('config', 'user.name', 'Parity Test');
  runGit('config', 'user.email', 'parity@example.invalid');
  runGit('add', 'src');
  runGit('commit', '--quiet', '-m', 'base');
  const base = runGit('rev-parse', 'HEAD').toString('utf8').trim();

  fs.writeFileSync(path.join(root, 'src/changed.txt'), 'after\n');
  fs.rmSync(path.join(root, 'src/deleted.txt'));
  fs.writeFileSync(path.join(root, 'src/added.txt'), 'new\n');
  fs.writeFileSync(path.join(root, 'src/output.json'), 'excluded before commit\n');
  const options = {
    root,
    reviewedPaths: ['src'],
    excludedPaths: ['src/output.json'],
  };
  const beforeCommit = calculateReviewedDiffSha256(base, options);

  runGit('add', 'src');
  runGit('commit', '--quiet', '-m', 'reviewed changes');
  const afterCommit = calculateReviewedDiffSha256(base, options);
  assert.equal(afterCommit, beforeCommit);

  fs.writeFileSync(path.join(root, 'src/added.txt'), 'changed again\n');
  assert.notEqual(calculateReviewedDiffSha256(base, options), afterCommit);
});

test('audits Classic, Ember baseline, React source, and parity evidence', () => {
  assert.deepEqual(REVIEWED_PATHS, [
    'ambari-web/classic',
    'ambari-web/latest',
    'docs/frontend-refactor/ember-baseline',
    'docs/frontend-refactor/react-current',
  ]);
});

test('labels non-Metrics out-of-scope rows as scope exclusions', () => {
  const row = createLegacyExclusionRow(
    { id: 'VIEW-SCOPE-004', moduleFile: '12-views.md', line: 1, summary: 'scope' },
    'OUT_OF_SCOPE',
    '| VIEW-SCOPE-004 | scope |',
    { commit: 'a'.repeat(40), by: 'Reviewer' },
  );
  assert.match(row.differences, /Scope exclusion/);
  assert.doesNotMatch(row.differences, /placeholder/i);
});
