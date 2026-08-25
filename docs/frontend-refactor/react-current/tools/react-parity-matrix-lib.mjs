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
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const scriptDir = path.dirname(fileURLToPath(import.meta.url));
export const reactCurrentRoot = path.resolve(scriptDir, '..');
export const frontendRefactorRoot = path.resolve(scriptDir, '../..');
export const repoRoot = path.resolve(scriptDir, '../../../..');
export const emberBaselineRoot = path.join(frontendRefactorRoot, 'ember-baseline');
export const featureIndexFile = path.join(emberBaselineRoot, 'generated/feature-index.json');

export const matrixFiles = {
  json: path.join(reactCurrentRoot, 'react-feature-parity-matrix.json'),
  csv: path.join(reactCurrentRoot, 'react-feature-parity-matrix.csv'),
  markdown: path.join(reactCurrentRoot, 'react-feature-parity-matrix.md'),
};

export const REVIEWED_PATHS = [
  'ambari-web/classic',
  'ambari-web/latest',
  'docs/frontend-refactor/ember-baseline',
  'docs/frontend-refactor/react-current',
];

const MATRIX_OUTPUT_PATHS = Object.values(matrixFiles).map((file) =>
  toPosix(path.relative(repoRoot, file)),
);

export const MATRIX_COLUMNS = [
  'feature_id',
  'legacy_summary',
  'legacy_doc',
  'legacy_evidence',
  'react_status',
  'source_status',
  'source_location',
  'react_route',
  'react_ui',
  'react_api',
  'condition_evidence',
  'happy_path_test',
  'failure_recovery_test',
  'differences',
  'runtime_scenario',
  'issue',
  'reviewed_commit',
  'reviewed_by',
];

export const REACT_STATUSES = new Set([
  'COVERED',
  'PARTIAL',
  'MISSING',
  'BEHAVIOR_DIFF',
  'NOT_APPLICABLE',
  'NEEDS_RUNTIME_VALIDATION',
  'BLOCKED',
]);

export const LEGACY_EVIDENCE = new Set([
  'CONFIRMED',
  'STATIC_ONLY',
  'CONDITIONAL',
  'PLACEHOLDER',
  'OUT_OF_SCOPE',
]);

export function isLegacyExclusion(legacyEvidence) {
  return legacyEvidence === 'OUT_OF_SCOPE' || legacyEvidence === 'PLACEHOLDER';
}

export const ISSUE_REQUIRED_STATUSES = new Set([
  'PARTIAL',
  'MISSING',
  'BEHAVIOR_DIFF',
  'BLOCKED',
]);

export const METRICS_EXCLUDED_FEATURE_IDS = new Set([
  'DASH-001',
  'DASH-005',
  'HOST-TAB-005',
]);

const LEGACY_EVIDENCE_OVERRIDES = new Map([
  ['DASH-001', 'OUT_OF_SCOPE'],
  ['DASH-005', 'OUT_OF_SCOPE'],
  ['HOST-TAB-005', 'OUT_OF_SCOPE'],
  ['VIEW-SCOPE-004', 'OUT_OF_SCOPE'],
  ['HOST-LIST-006', 'PLACEHOLDER'],
  ['HOST-DETAIL-010', 'PLACEHOLDER'],
  ['UPG-START-005', 'PLACEHOLDER'],
  ['INST-7-006', 'PLACEHOLDER'],
  ['NNHA-ROLLBACK-001', 'PLACEHOLDER'],
  ['NNHA-ROLLBACK-002', 'PLACEHOLDER'],
  ['NNHA-ROLLBACK-003', 'PLACEHOLDER'],
  ['VIEW-X-002', 'PLACEHOLDER'],
  ['VIEW-X-003', 'PLACEHOLDER'],
  ['VIEW-RISK-011', 'PLACEHOLDER'],
  ['ALERT-CREATE-001', 'CONFIRMED'],
  ['NNHA-SCOPE-004', 'CONFIRMED'],
  ['NNHA-SCOPE-005', 'CONFIRMED'],
]);

const MARKDOWN_LICENSE_HEADER = `<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

`;

const STATUS_NORMALIZATION = new Map([
  ['COVERED', 'NEEDS_RUNTIME_VALIDATION'],
  ['MATCH', 'NEEDS_RUNTIME_VALIDATION'],
  ['PASS', 'NEEDS_RUNTIME_VALIDATION'],
  ['STATICALLY_ALIGNED', 'NEEDS_RUNTIME_VALIDATION'],
  ['STATIC_COMPLETE', 'NEEDS_RUNTIME_VALIDATION'],
  ['LIVE_REQUIRED', 'NEEDS_RUNTIME_VALIDATION'],
  ['STATIC_ONLY', 'NEEDS_RUNTIME_VALIDATION'],
  ['RESOLVED_STATICALLY', 'NEEDS_RUNTIME_VALIDATION'],
  ['NEEDS_RUNTIME_VALIDATION', 'NEEDS_RUNTIME_VALIDATION'],
  ['PARTIAL', 'PARTIAL'],
  ['IMPROVED_PARTIAL', 'PARTIAL'],
  ['UNCHANGED_PARTIAL', 'PARTIAL'],
  ['CROSS_MODULE_BOUNDARY', 'PARTIAL'],
  ['RUNTIME_OR_CROSS_MODULE', 'PARTIAL'],
  ['MISSING', 'MISSING'],
  ['BEHAVIOR_DIFF', 'BEHAVIOR_DIFF'],
  ['INCORRECT', 'BEHAVIOR_DIFF'],
  ['DIFFERENT', 'BEHAVIOR_DIFF'],
  ['COMPATIBILITY_FIX', 'BEHAVIOR_DIFF'],
  ['IMPROVED_BEYOND_CLASSIC', 'BEHAVIOR_DIFF'],
  ['NOT_APPLICABLE', 'NOT_APPLICABLE'],
  ['NOT_REQUIRED', 'NOT_APPLICABLE'],
  ['OUT_OF_SCOPE', 'NOT_APPLICABLE'],
  ['BROKEN_LEGACY', 'NOT_APPLICABLE'],
  ['BLOCKED', 'BLOCKED'],
  ['BLOCKED_ON_MODULE_03', 'BLOCKED'],
]);

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

export function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

export function toPosix(value) {
  return value.split(path.sep).join('/');
}

export function calculateReviewedDiffSha256(reviewedCommit, options = {}) {
  if (!/^[0-9a-f]{40}$/.test(reviewedCommit)) {
    throw new Error('reviewedCommit must be a full 40-character Git commit');
  }
  const root = options.root ?? repoRoot;
  const reviewedPaths = options.reviewedPaths ?? REVIEWED_PATHS;
  const excludedPaths = options.excludedPaths ?? MATRIX_OUTPUT_PATHS;
  const exclusions = excludedPaths.map((file) => `:(exclude)${file}`);
  const changedOutput = execFileSync(
    'git',
    [
      'diff',
      '--name-only',
      '-z',
      '--no-renames',
      reviewedCommit,
      '--',
      ...reviewedPaths,
      ...exclusions,
    ],
    { cwd: root, maxBuffer: 128 * 1024 * 1024 },
  );
  const untrackedOutput = execFileSync(
    'git',
    [
      'ls-files',
      '--others',
      '--exclude-standard',
      '-z',
      '--',
      ...reviewedPaths,
      ...exclusions,
    ],
    { cwd: root },
  );
  const changedFiles = new Set(changedOutput.toString('utf8').split('\0').filter(Boolean));
  for (const file of untrackedOutput.toString('utf8').split('\0').filter(Boolean)) {
    changedFiles.add(file);
  }
  const hash = crypto.createHash('sha256');
  hash.update('canonical-worktree-diff-v1\0');
  for (const file of [...changedFiles].sort()) {
    const absoluteFile = path.join(root, file);
    let stat;
    try {
      stat = fs.lstatSync(absoluteFile);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
      hash.update('deleted\0');
      hash.update(file);
      hash.update('\0');
      continue;
    }
    const mode = stat.isSymbolicLink()
      ? '120000'
      : stat.isFile()
        ? (stat.mode & 0o111 ? '100755' : '100644')
        : null;
    if (!mode) throw new Error(`Unsupported audited path type: ${file}`);
    const contents = stat.isSymbolicLink()
      ? Buffer.from(fs.readlinkSync(absoluteFile))
      : fs.readFileSync(absoluteFile);
    hash.update('present\0');
    hash.update(file);
    hash.update('\0');
    hash.update(mode);
    hash.update('\0');
    hash.update(contents);
    hash.update('\0');
  }
  return hash.digest('hex');
}

export function stripMarkdown(value) {
  return String(value ?? '')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/\*\*/g, '')
    .replace(/^`+|`+$/g, '')
    .replace(/\\\|/g, '|')
    .replace(/\s+/g, ' ')
    .trim();
}

export function splitMarkdownRow(line) {
  const cells = [];
  let cell = '';
  let escaped = false;
  let inCode = false;
  const start = line.indexOf('|') + 1;
  for (let index = start; index < line.length; index += 1) {
    const char = line[index];
    if (escaped) {
      cell += char;
      escaped = false;
      continue;
    }
    if (char === '\\') {
      cell += char;
      escaped = true;
      continue;
    }
    if (char === '`') {
      inCode = !inCode;
      cell += char;
      continue;
    }
    if (char === '|' && !inCode) {
      cells.push(cell.trim());
      cell = '';
      continue;
    }
    cell += char;
  }
  if (cell.trim()) cells.push(cell.trim());
  if (cells.at(-1) === '') cells.pop();
  return cells;
}

function isDividerCell(value) {
  return /^:?-{3,}:?$/.test(value.trim());
}

function tablePriority(headingPath, statusHeader) {
  const heading = headingPath.toLowerCase();
  let priority = 100;
  if (heading.includes('post-implementation')) priority = 600;
  else if (heading.includes('final')) priority = 550;
  else if (heading.includes('feature id status')) priority = 500;
  else if (heading.includes('test feature ids')) priority = 500;
  else if (heading.includes('feature status')) priority = 450;
  else if (heading.includes('api contract')) priority = 400;
  if (heading.includes('initial') || heading.includes('pre-implementation')) priority -= 500;
  if (/final|current/i.test(statusHeader)) priority += 10;
  if (/initial|source/i.test(statusHeader)) priority -= 10;
  return priority;
}

export function normalizeSourceStatus(value) {
  const normalized = stripMarkdown(value).toUpperCase().replace(/[ -]+/g, '_');
  return {
    sourceStatus: normalized,
    reactStatus: STATUS_NORMALIZATION.get(normalized) ?? null,
  };
}

export function parseGapStatusRows(file, featureIds) {
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
  const headingStack = [];
  const rows = [];
  const errors = [];

  for (let index = 0; index < lines.length; index += 1) {
    const heading = lines[index].match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      const level = heading[1].length;
      headingStack.length = level - 1;
      headingStack[level - 1] = stripMarkdown(heading[2]);
      continue;
    }
    if (!/^\s*\|/.test(lines[index])) continue;

    const tableStart = index;
    const tableLines = [];
    while (index < lines.length && /^\s*\|/.test(lines[index])) {
      tableLines.push({ line: index + 1, cells: splitMarkdownRow(lines[index]) });
      index += 1;
    }
    index -= 1;
    if (tableLines.length < 2 || !tableLines[1].cells.every(isDividerCell)) continue;

    const headers = tableLines[0].cells.map(stripMarkdown);
    const idIndex = headers.findIndex((header) => /^(?:feature\s+)?id$/i.test(header));
    const statusIndex = headers.findIndex((header) => /status/i.test(header));
    if (idIndex === -1 || statusIndex === -1) continue;
    const headingPath = headingStack.filter(Boolean).join(' > ');

    for (const tableRow of tableLines.slice(2)) {
      if (tableRow.cells.length !== headers.length) {
        errors.push(`${toPosix(path.relative(reactCurrentRoot, file))}:${tableRow.line} has ${tableRow.cells.length} cells; expected ${headers.length}`);
        continue;
      }
      const featureId = stripMarkdown(tableRow.cells[idIndex]);
      if (!featureIds.has(featureId)) {
        if (/^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+$/.test(featureId)) {
          errors.push(`${toPosix(path.relative(reactCurrentRoot, file))}:${tableRow.line} has unknown feature ID ${featureId}`);
        }
        continue;
      }
      const normalized = normalizeSourceStatus(tableRow.cells[statusIndex]);
      if (!normalized.reactStatus) {
        errors.push(`${toPosix(path.relative(reactCurrentRoot, file))}:${tableRow.line} has unsupported status ${JSON.stringify(normalized.sourceStatus)} for ${featureId}`);
        continue;
      }
      const evidence = tableRow.cells
        .filter((_, cellIndex) => cellIndex !== idIndex && cellIndex !== statusIndex)
        .map(stripMarkdown)
        .filter(Boolean)
        .join(' | ');
      rows.push({
        featureId,
        ...normalized,
        evidence,
        headingPath,
        sourceFile: toPosix(path.relative(frontendRefactorRoot, file)),
        sourceLine: tableRow.line,
        priority: tablePriority(headingPath, headers[statusIndex]),
        tableStart: tableStart + 1,
      });
    }
  }

  return { rows, errors };
}

export function selectAuthoritativeRows(rows) {
  const errors = [];
  const superseded = [];
  const selected = new Map();
  const grouped = new Map();
  for (const row of rows) {
    if (!grouped.has(row.featureId)) grouped.set(row.featureId, []);
    grouped.get(row.featureId).push(row);
  }

  for (const [featureId, candidates] of grouped) {
    const maxPriority = Math.max(...candidates.map((candidate) => candidate.priority));
    const authoritative = candidates.filter((candidate) => candidate.priority === maxPriority);
    if (authoritative.length !== 1) {
      errors.push(`${featureId} has ${authoritative.length} equally authoritative rows: ${authoritative.map((row) => `${row.sourceFile}:${row.sourceLine}=${row.sourceStatus}`).join(', ')}`);
      continue;
    }
    selected.set(featureId, authoritative[0]);
    for (const candidate of candidates) {
      if (candidate !== authoritative[0]) {
        superseded.push({
          featureId,
          selected: `${authoritative[0].sourceFile}:${authoritative[0].sourceLine}`,
          ignored: `${candidate.sourceFile}:${candidate.sourceLine}`,
          ignoredStatus: candidate.sourceStatus,
        });
      }
    }
  }
  return { selected, errors, superseded };
}

function evidenceMarker(value) {
  if (/\bOUT_OF_SCOPE\b/.test(value)) return 'OUT_OF_SCOPE';
  if (/\bPLACEHOLDER\b/.test(value)) return 'PLACEHOLDER';
  if (/\bSTATIC_ONLY\b/.test(value)) return 'STATIC_ONLY';
  if (/\bCONDITIONAL\b/.test(value)) return 'CONDITIONAL';
  if (/\bCONFIRMED\b/.test(value)) return 'CONFIRMED';
  return null;
}

export function inferLegacyEvidence(feature, line, documentLines = []) {
  const value = String(line ?? '');
  if (!value.includes(feature.id)) throw new Error(`${feature.moduleFile}:${feature.line} does not contain ${feature.id}`);
  const override = LEGACY_EVIDENCE_OVERRIDES.get(feature.id);
  if (override) return override;
  if (documentLines.length) {
    let firstTableLine = feature.line - 1;
    while (firstTableLine > 0 && /^\s*\|/.test(documentLines[firstTableLine - 1])) firstTableLine -= 1;
    const headers = splitMarkdownRow(documentLines[firstTableLine] || '').map(stripMarkdown);
    const rowCells = splitMarkdownRow(value);
    const levelIndex = headers.findIndex((header) => /^(?:evidence\s+)?level$/i.test(header));
    if (levelIndex >= 0) {
      const level = evidenceMarker(rowCells[levelIndex] || '');
      if (level) return level;
    }

    let tableHeader = feature.line - 2;
    while (tableHeader >= 0 && /^\s*\|/.test(documentLines[tableHeader])) tableHeader -= 1;
    const preamble = documentLines
      .slice(Math.max(0, tableHeader - 4), tableHeader + 1)
      .join(' ');
    const tableDefault = preamble.match(/all items below are\s+(.+?)(?:\.|$)/i)?.[1] ?? '';
    const defaultEvidence = evidenceMarker(tableDefault);
    if (defaultEvidence) return defaultEvidence;

    const family = feature.id.split('-')[0];
    const familyDefault = documentLines
      .slice(0, 60)
      .find((documentLine) => new RegExp(`\\b${family} evidence level:`, 'i').test(documentLine));
    if (familyDefault) {
      const familyEvidence = evidenceMarker(familyDefault);
      if (familyEvidence) return familyEvidence;
    }
  }
  const directEvidence = evidenceMarker(value);
  if (directEvidence) return directEvidence;
  return 'CONFIRMED';
}

function inlineCodeValues(value) {
  return [...String(value).matchAll(/`([^`]+)`/g)].map((match) => match[1]);
}

function selectReferences(value, predicate, limit = 6) {
  return unique(inlineCodeValues(value).filter(predicate)).slice(0, limit).join('; ');
}

function issueReference(gapFile, sourceRow, fallbackIssue) {
  const source = fs.readFileSync(gapFile, 'utf8');
  const jira = source.match(/\bAMBARI-\d+\b/)?.[0];
  if (jira) return jira;
  const modulePrefix = path.basename(gapFile).slice(0, 2);
  const issueDir = path.join(reactCurrentRoot, 'issues');
  const issueDraft = fs.existsSync(issueDir)
    ? fs.readdirSync(issueDir).sort().find((entry) => entry.startsWith(`${modulePrefix}-`) && entry.endsWith('.md'))
    : null;
  if (issueDraft) return `react-current/issues/${issueDraft}`;
  if (fallbackIssue) return fallbackIssue;
  return `${sourceRow.sourceFile}:${sourceRow.sourceLine}`;
}

function notEstablished(label, sourceLocation) {
  return `${label} was not independently established by the static comparison; see ${sourceLocation}.`;
}

export function createMatrixRow(feature, legacyEvidence, sourceRow, review, fallbackIssue) {
  const sourceLocation = `${sourceRow.sourceFile}:${sourceRow.sourceLine}`;
  const isNotApplicable = sourceRow.reactStatus === 'NOT_APPLICABLE';
  const testReferences = selectReferences(
    sourceRow.evidence,
    (value) => /(?:\.test\.[cm]?[jt]sx?|\.spec\.[cm]?[jt]sx?|vitest|node --test|test_[^/\s]+\.py)/i.test(value),
    12,
  );
  const reactRoute = selectReferences(
    sourceRow.evidence,
    (value) => /^\/(?!api\/)(?:main|login|views?|adminView|installer)(?:\/|$)/.test(value),
  );
  const reactUi = selectReferences(
    sourceRow.evidence,
    (value) => /(?:^|\/)src\/.*\.[cm]?[jt]sx?$/.test(value) && !/\.test\./.test(value),
  );
  const reactApi = selectReferences(
    sourceRow.evidence,
    (value) => /(?:^|\/)src\/api\/|^\/api\/v1\/|^(?:GET|POST|PUT|DELETE|PATCH)\s+\//.test(value),
  );
  let happyPathTest = testReferences || notEstablished('Happy-path evidence', sourceLocation);
  let failureRecoveryTest = notEstablished('Failure/recovery evidence', sourceLocation);
  if (testReferences && /fail|error|retry|recover|cancel|refresh|owner|rollback/i.test(sourceRow.evidence)) {
    failureRecoveryTest = `${testReferences}; verify the source row before treating these as complete recovery coverage.`;
  }
  if (isNotApplicable) {
    happyPathTest = 'Not applicable to a deliberately excluded or unreproduced legacy behavior.';
    failureRecoveryTest = 'Not applicable to a deliberately excluded or unreproduced legacy behavior.';
  } else if (sourceRow.reactStatus === 'MISSING') {
    happyPathTest = 'Not implemented; no React happy-path test is available.';
    failureRecoveryTest = 'Not implemented; no React failure/recovery test is available.';
  }

  let differences = sourceRow.evidence;
  if (sourceRow.reactStatus === 'NEEDS_RUNTIME_VALIDATION') {
    differences = `Source status ${sourceRow.sourceStatus} is static evidence only; runtime equivalence is not yet established. ${sourceRow.evidence}`.trim();
  } else if (isNotApplicable && legacyEvidence === 'OUT_OF_SCOPE') {
    differences = `${METRICS_EXCLUDED_FEATURE_IDS.has(feature.id) ? 'Metrics exclusion' : 'Scope exclusion'} recorded by the legacy baseline. ${sourceRow.evidence}`.trim();
  } else if (isNotApplicable && legacyEvidence === 'PLACEHOLDER') {
    differences = `Legacy placeholder with no complete behavior to migrate. ${sourceRow.evidence}`.trim();
  }

  const issue = ISSUE_REQUIRED_STATUSES.has(sourceRow.reactStatus) || isNotApplicable
    ? issueReference(path.join(frontendRefactorRoot, sourceRow.sourceFile), sourceRow, fallbackIssue)
    : '';
  const runtimeScenario = sourceRow.reactStatus === 'NEEDS_RUNTIME_VALIDATION'
      || legacyEvidence === 'STATIC_ONLY'
      || legacyEvidence === 'CONDITIONAL'
    ? `Use the runtime acceptance matrix in ${sourceRow.sourceFile}; validate ${feature.id} against the conditions recorded at ${sourceLocation}.`
    : '';

  return {
    feature_id: feature.id,
    legacy_summary: feature.summary,
    legacy_doc: `ember-baseline/${feature.moduleFile}:${feature.line}`,
    legacy_evidence: legacyEvidence,
    react_status: sourceRow.reactStatus,
    source_status: sourceRow.sourceStatus,
    source_location: sourceLocation,
    react_route: reactRoute,
    react_ui: reactUi,
    react_api: reactApi,
    condition_evidence: `${sourceLocation}: ${sourceRow.evidence}`.trim(),
    happy_path_test: happyPathTest,
    failure_recovery_test: failureRecoveryTest,
    differences,
    runtime_scenario: runtimeScenario,
    issue,
    reviewed_commit: review.commit,
    reviewed_by: review.by,
  };
}

export function createLegacyExclusionRow(feature, legacyEvidence, legacyLine, review) {
  const sourceLocation = `ember-baseline/${feature.moduleFile}:${feature.line}`;
  const metrics = METRICS_EXCLUDED_FEATURE_IDS.has(feature.id);
  const reason = metrics
    ? 'Metrics exclusion recorded by the legacy baseline; this row is represented but excluded from parity completion counts.'
    : legacyEvidence === 'OUT_OF_SCOPE'
      ? 'Scope exclusion recorded by the legacy baseline; this row is represented but excluded from parity completion counts.'
      : 'Legacy placeholder has no complete behavior to migrate and is represented outside parity completion counts.';
  return {
    feature_id: feature.id,
    legacy_summary: feature.summary,
    legacy_doc: sourceLocation,
    legacy_evidence: legacyEvidence,
    react_status: 'NOT_APPLICABLE',
    source_status: `LEGACY_${legacyEvidence}`,
    source_location: sourceLocation,
    react_route: '',
    react_ui: '',
    react_api: '',
    condition_evidence: `${sourceLocation}: ${stripMarkdown(legacyLine)}`,
    happy_path_test: 'Not applicable to a legacy scope exclusion or placeholder.',
    failure_recovery_test: 'Not applicable to a legacy scope exclusion or placeholder.',
    differences: reason,
    runtime_scenario: '',
    issue: sourceLocation,
    reviewed_commit: review.commit,
    reviewed_by: review.by,
  };
}

export function calculateStatistics(rows) {
  const byStatus = Object.fromEntries([...REACT_STATUSES].map((status) => [status, 0]));
  for (const row of rows) byStatus[row.react_status] = (byStatus[row.react_status] ?? 0) + 1;
  const metricsExcluded = rows.filter((row) => METRICS_EXCLUDED_FEATURE_IDS.has(row.feature_id)).length;
  const outOfScope = rows.filter((row) => row.legacy_evidence === 'OUT_OF_SCOPE').length;
  const placeholders = rows.filter((row) => row.legacy_evidence === 'PLACEHOLDER').length;
  const completionRows = rows.filter((row) => row.react_status !== 'NOT_APPLICABLE'
    && row.legacy_evidence !== 'OUT_OF_SCOPE'
    && row.legacy_evidence !== 'PLACEHOLDER');
  const openGapStatuses = new Set(['PARTIAL', 'MISSING', 'BEHAVIOR_DIFF', 'BLOCKED']);
  return {
    total_rows: rows.length,
    metrics_excluded: metricsExcluded,
    out_of_scope: outOfScope,
    legacy_placeholders: placeholders,
    completion_denominator: completionRows.length,
    covered: completionRows.filter((row) => row.react_status === 'COVERED').length,
    needs_runtime_validation: completionRows.filter((row) => row.react_status === 'NEEDS_RUNTIME_VALIDATION').length,
    open_gaps: completionRows.filter((row) => openGapStatuses.has(row.react_status)).length,
    by_status: byStatus,
  };
}

export function csvEscape(value) {
  const text = String(value ?? '');
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

export function renderCsv(rows) {
  return `${MATRIX_COLUMNS.join(',')}\n${rows.map((row) => MATRIX_COLUMNS.map((column) => csvEscape(row[column])).join(',')).join('\n')}\n`;
}

export function parseCsv(source) {
  const records = [];
  let record = [];
  let field = '';
  let quoted = false;
  for (let index = 0; index < source.length; index += 1) {
    const char = source[index];
    if (quoted) {
      if (char === '"' && source[index + 1] === '"') {
        field += '"';
        index += 1;
      } else if (char === '"') quoted = false;
      else field += char;
    } else if (char === '"') quoted = true;
    else if (char === ',') {
      record.push(field);
      field = '';
    } else if (char === '\n') {
      record.push(field.replace(/\r$/, ''));
      records.push(record);
      record = [];
      field = '';
    } else field += char;
  }
  if (quoted) throw new Error('CSV ends inside a quoted field');
  if (field || record.length) {
    record.push(field);
    records.push(record);
  }
  const [headers, ...data] = records.filter((item) => item.some((value) => value !== ''));
  if (!headers) return [];
  if (new Set(headers).size !== headers.length) throw new Error('CSV header contains duplicate columns');
  return data.map((values, rowIndex) => {
    if (values.length !== headers.length) throw new Error(`CSV row ${rowIndex + 2} has ${values.length} fields; expected ${headers.length}`);
    return Object.fromEntries(headers.map((header, index) => [header, values[index]]));
  });
}

function markdownCell(value, limit = 320) {
  const normalized = String(value ?? '').replace(/\r?\n/g, '<br>').replace(/\|/g, '\\|');
  return normalized.length <= limit ? normalized : `${normalized.slice(0, limit - 3)}...`;
}

export function renderMarkdown(matrix) {
  const {
    statistics,
    rows,
    reviewed_commit: reviewedCommit,
    reviewed_diff_sha256: reviewedDiffSha256,
    reviewed_paths: reviewedPaths,
    reviewed_by: reviewedBy,
  } = matrix;
  const summary = [...REACT_STATUSES]
    .map((status) => `| \`${status}\` | ${statistics.by_status[status] ?? 0} |`)
    .join('\n');
  const tableRows = rows.map((row) => `| ${row.feature_id} | ${markdownCell(row.legacy_doc)} | \`${row.legacy_evidence}\` | \`${row.react_status}\` | ${markdownCell(row.condition_evidence)} | ${markdownCell(row.issue)} |`).join('\n');
  return `${MARKDOWN_LICENSE_HEADER}# React Feature Parity Matrix

This file is rendered from the editable JSON/CSV matrix. Do not use a matching route or component name as proof of parity. Static matches remain \`NEEDS_RUNTIME_VALIDATION\` until normal, failure, and recovery behavior is validated in a real environment.

## Review Snapshot

| Item | Value |
| --- | --- |
| Legacy feature IDs | ${statistics.total_rows} |
| Reviewed base commit | \`${reviewedCommit}\` |
| Audited working-tree diff SHA-256 | \`${reviewedDiffSha256}\` |
| Audited paths | ${reviewedPaths.map((value) => `\`${value}\``).join(', ')} |
| Reviewed by | ${markdownCell(reviewedBy)} |
| Completion denominator | ${statistics.completion_denominator} |
| Metrics exclusions | ${statistics.metrics_excluded} |
| All out-of-scope rows | ${statistics.out_of_scope} |
| Legacy placeholders | ${statistics.legacy_placeholders} |
| Runtime validation pending | ${statistics.needs_runtime_validation} |
| Open gaps | ${statistics.open_gaps} |

## Status Counts

| Status | Count |
| --- | ---: |
${summary}

Metrics \`OUT_OF_SCOPE\` rows and legacy \`PLACEHOLDER\` rows are represented for ID completeness but excluded from the completion denominator.

## Feature Rows

| Feature ID | Legacy document | Legacy evidence | React status | Source evidence | Issue/decision |
| --- | --- | --- | --- | --- | --- |
${tableRows}
`;
}

export function matrixDocument(rows, review, reviewedDiffSha256) {
  return {
    license: 'Licensed to the Apache Software Foundation (ASF) under the Apache License, Version 2.0. See the repository LICENSE file.',
    schema_version: 1,
    review_basis: 'base_commit_plus_audited_worktree_diff',
    reviewed_commit: review.commit,
    reviewed_diff_sha256: reviewedDiffSha256,
    reviewed_paths: REVIEWED_PATHS,
    reviewed_by: review.by,
    statistics: calculateStatistics(rows),
    rows,
  };
}

export function normalizeEditableRows(rows) {
  return rows.map((row) => Object.fromEntries(MATRIX_COLUMNS.map((column) => [column, String(row[column] ?? '')])));
}
