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


/*
 * Validates the hand-written Ember baseline against every generated inventory.
 * This intentionally uses only Node built-ins so it can run before npm install.
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import dynamicAjaxResolutions from './contracts/dynamic-ajax-resolutions.mjs';
import realtimeChannelsContract from './contracts/realtime-channels.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const baselineRoot = path.resolve(scriptDir, '..');
const generatedRoot = path.join(baselineRoot, 'generated');
const repoRoot = path.resolve(scriptDir, '../../../..');
const errors = [];
const warnings = [];
const ALLOWED_NEW_FEATURE_IDS = new Set(['INST-MODE-011', 'INST-8-009']);
const LEGACY_FEATURE_ID_COUNT = 1000;
const LEGACY_FEATURE_ID_SEQUENCE_SHA256 = '21699bfe0be07648e5124cfd640d8593a83d840ca19de455c40712b74f1f1a23';
const SERVICE_THEME_MODULE = '14-service-theme-layout.md';
const SERVICE_THEME_FEATURE_ID_COUNT = 152;
const SERVICE_THEME_FEATURE_ID_SEQUENCE_SHA256 = '7dc625b3b77624012c4f541ff456f23c606f81f3c491e49a82dfcc467574a1c1';
const HTTP_METHODS = new Set(['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'DYNAMIC']);

const OPEN_DYNAMIC_AJAX_DISPATCH_KINDS = new Set([
  'PARAMETER_WRAPPER',
  'MODEL_METADATA_LOOKUP',
  'MIXIN_REQUEST_PROPERTY',
  'FIFO_OPTIONS_QUEUE',
]);

function dynamicAjaxResolutionKey({ source, line, requestExpression }) {
  return `${source}\u0000${line}\u0000${requestExpression}`;
}

function listFiles(root, predicate) {
  if (!fs.existsSync(root)) return [];
  const files = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) files.push(...listFiles(fullPath, predicate));
    else if (predicate(fullPath)) files.push(fullPath);
  }
  return files.sort();
}

function relative(file) {
  return path.relative(baselineRoot, file).split(path.sep).join('/');
}

function read(file) {
  return fs.readFileSync(file, 'utf8');
}

function readJson(file) {
  try {
    return JSON.parse(read(file));
  } catch (error) {
    errors.push(`${relative(file)} is not valid JSON: ${error.message}`);
    return [];
  }
}

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function sha256Json(value) {
  return sha256(JSON.stringify(value));
}

function scanBalanced(source, openIndex, openChar = '{', closeChar = '}') {
  let depth = 0;
  let quote = null;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];
    if (lineComment) {
      if (char === '\n') lineComment = false;
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        index += 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
      continue;
    }
    if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === openChar) depth += 1;
    if (char === closeChar && --depth === 0) return index;
  }
  return -1;
}

function ajaxDefinitionObjectAtLine(source, line) {
  const lineStart = source.split(/\r?\n/, line - 1).reduce((offset, value) => offset + value.length + 1, 0);
  const entryStart = source.indexOf('{', lineStart);
  const entryEnd = entryStart === -1 ? -1 : scanBalanced(source, entryStart);
  return entryEnd === -1 ? null : source.slice(entryStart, entryEnd + 1);
}

function skipWhitespaceAndComments(source, start) {
  let index = start;
  while (index < source.length) {
    if (/\s/.test(source[index])) {
      index += 1;
      continue;
    }
    if (source[index] === '/' && source[index + 1] === '/') {
      index += 2;
      while (index < source.length && source[index] !== '\n') index += 1;
      continue;
    }
    if (source[index] === '/' && source[index + 1] === '*') {
      const end = source.indexOf('*/', index + 2);
      index = end === -1 ? source.length : end + 2;
      continue;
    }
    break;
  }
  return index;
}

function extractObjectProperties(objectSource) {
  const open = objectSource.indexOf('{');
  const close = open === -1 ? -1 : scanBalanced(objectSource, open);
  if (close === -1) return new Map();
  const properties = new Map();
  let index = open + 1;
  while (index < close) {
    index = skipWhitespaceAndComments(objectSource, index);
    while (objectSource[index] === ',') index = skipWhitespaceAndComments(objectSource, index + 1);
    if (index >= close) break;
    let key = null;
    if (objectSource[index] === "'" || objectSource[index] === '"') {
      const quote = objectSource[index];
      let cursor = index + 1;
      let escaped = false;
      for (; cursor < close; cursor += 1) {
        if (escaped) escaped = false;
        else if (objectSource[cursor] === '\\') escaped = true;
        else if (objectSource[cursor] === quote) break;
      }
      key = objectSource.slice(index + 1, cursor);
      index = cursor + 1;
    } else {
      const keyMatch = objectSource.slice(index).match(/^[$A-Z_a-z][$\w]*/);
      if (keyMatch) {
        key = keyMatch[0];
        index += key.length;
      }
    }
    index = skipWhitespaceAndComments(objectSource, index);
    if (!key || objectSource[index] !== ':') {
      const comma = objectSource.indexOf(',', index);
      index = comma === -1 || comma >= close ? close : comma + 1;
      continue;
    }
    const valueStart = skipWhitespaceAndComments(objectSource, index + 1);
    let cursor = valueStart;
    let roundDepth = 0;
    let squareDepth = 0;
    let curlyDepth = 0;
    let quote = null;
    let escaped = false;
    let lineComment = false;
    let blockComment = false;
    for (; cursor < close; cursor += 1) {
      const char = objectSource[cursor];
      const next = objectSource[cursor + 1];
      if (lineComment) {
        if (char === '\n') lineComment = false;
        continue;
      }
      if (blockComment) {
        if (char === '*' && next === '/') {
          blockComment = false;
          cursor += 1;
        }
        continue;
      }
      if (quote) {
        if (escaped) escaped = false;
        else if (char === '\\') escaped = true;
        else if (char === quote) quote = null;
        continue;
      }
      if (char === '/' && next === '/') {
        lineComment = true;
        cursor += 1;
      } else if (char === '/' && next === '*') {
        blockComment = true;
        cursor += 1;
      } else if (char === "'" || char === '"' || char === '`') quote = char;
      else if (char === '(') roundDepth += 1;
      else if (char === ')') roundDepth -= 1;
      else if (char === '[') squareDepth += 1;
      else if (char === ']') squareDepth -= 1;
      else if (char === '{') curlyDepth += 1;
      else if (char === '}') curlyDepth -= 1;
      else if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
    }
    properties.set(key, objectSource.slice(valueStart, cursor).trim());
    index = cursor + 1;
  }
  return properties;
}

function maskJavaScriptComments(source) {
  const masked = source.split('');
  let quote = null;
  let escaped = false;
  for (let index = 0; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '/' && next === '/') {
      masked[index] = masked[index + 1] = ' ';
      index += 2;
      while (index < source.length && source[index] !== '\n') {
        if (source[index] !== '\r') masked[index] = ' ';
        index += 1;
      }
      index -= 1;
    } else if (char === '/' && next === '*') {
      masked[index] = masked[index + 1] = ' ';
      index += 2;
      while (index < source.length) {
        if (source[index] === '*' && source[index + 1] === '/') {
          masked[index] = masked[index + 1] = ' ';
          index += 1;
          break;
        }
        if (source[index] !== '\n' && source[index] !== '\r') masked[index] = ' ';
        index += 1;
      }
    }
  }
  return masked.join('');
}

function extractReturnedObjectProperties(functionExpression) {
  if (!functionExpression) return [];
  const masked = maskJavaScriptComments(functionExpression);
  const returnedObjects = [];
  for (const match of masked.matchAll(/\breturn\b/g)) {
    const start = skipWhitespaceAndComments(masked, match.index + match[0].length);
    if (masked[start] !== '{') continue;
    const end = scanBalanced(functionExpression, start);
    if (end !== -1) returnedObjects.push(extractObjectProperties(functionExpression.slice(start, end + 1)));
  }
  return returnedObjects;
}

function staticHttpMethod(expression) {
  return expression?.match(/^(['"])([A-Z]+)\1$/)?.[2] ?? null;
}

function collapseStringExpression(expression) {
  if (!expression) return '';
  const stringPattern = /(['"])((?:\\.|(?!\1).)*)\1/g;
  const parts = [...expression.matchAll(stringPattern)].map((match) => match[2].replace(/\\(['"\\])/g, '$1'));
  if (!parts.length || !parts.join('')) return expression.replace(/\s+/g, ' ').trim();
  const staticExpression = expression.replace(stringPattern, '').replace(/[+\s()]/g, '') === '';
  return staticExpression ? parts.join('') : `${parts.join('')} [dynamic: ${expression.replace(/\s+/g, ' ').trim()}]`;
}

function normalizeExpression(expression, maxLength = 180) {
  const value = String(expression ?? '').replace(/\s+/g, ' ').trim();
  return value.length <= maxLength ? value : `${value.slice(0, maxLength - 3)}...`;
}

function expectedAjaxDefinitionContract(objectSource) {
  const properties = extractObjectProperties(objectSource);
  const realExpression = properties.get('real') ?? null;
  const apiPrefixExpression = properties.get('apiPrefix') ?? null;
  const topLevelTypeExpression = properties.get('type') ?? null;
  const formatExpression = properties.get('format') ?? null;
  const returnedProperties = extractReturnedObjectProperties(formatExpression);
  const formatTypeExpressions = returnedProperties.map((item) => item.get('type')).filter((item) => item !== undefined);
  const formatUrlExpressions = returnedProperties.map((item) => item.get('url')).filter((item) => item !== undefined);
  const methodExpressions = [topLevelTypeExpression, ...formatTypeExpressions].filter((item) => item !== null);
  const methods = new Set(methodExpressions.map(staticHttpMethod).filter(Boolean));
  if (methods.size === 0) methods.add('GET');
  if (methodExpressions.some((item) => !staticHttpMethod(item))) methods.add('DYNAMIC');
  const realIsEmpty = realExpression === null || /^(['"])\1$/.test(realExpression.trim());
  const usesFormatUrl = realIsEmpty && formatUrlExpressions.length > 0;
  const endpointExpressions = usesFormatUrl ? formatUrlExpressions : [realExpression].filter(Boolean);
  const stringPattern = /(['"])((?:\\.|(?!\1).)*)\1/g;
  const hasDynamicUrl = endpointExpressions.some((expression) =>
    expression.replace(stringPattern, '').replace(/[+\s()]/g, '') !== '');
  const inputKeys = new Set();
  for (const match of objectSource.matchAll(/\bdata(?:\?\.)?\.([$A-Z_a-z][$\w]*)/g)) inputKeys.add(match[1]);
  for (const match of objectSource.matchAll(/\bdata\s*\[\s*(['"])([^'"\n]+)\1\s*\]/g)) inputKeys.add(match[2]);
  return {
    methods: [...methods].sort(),
    endpoint: endpointExpressions.map(collapseStringExpression).join(' OR ') || '(empty)',
    endpointSource: usesFormatUrl ? 'format' : 'real',
    endpointExpression: endpointExpressions.map((item) => normalizeExpression(item)).join(' OR ') || null,
    hasDynamicUrl,
    apiPrefix: apiPrefixExpression === null ? '/api/v1 (default)' : collapseStringExpression(apiPrefixExpression) || '(empty)',
    hasFormat: formatExpression !== null,
    formatExpression,
    topLevelTypeExpression,
    formatTypeExpressions,
    formatUrlExpressions,
    inputKeys: [...inputKeys].sort(),
  };
}

function countLinesBefore(source, index) {
  return source.slice(0, index).split('\n').length;
}

function parseCount(file, pattern, label) {
  const match = read(file).match(pattern);
  const value = Number(match?.[1]);
  if (!Number.isInteger(value)) errors.push(`Could not parse ${label} from ${relative(file)}`);
  return value;
}

function extractFirstCell(row, start) {
  let inCode = false;
  let escaped = false;
  for (let index = start; index < row.length; index += 1) {
    const char = row[index];
    if (escaped) {
      escaped = false;
      continue;
    }
    if (char === '\\') {
      escaped = true;
      continue;
    }
    if (char === '`') {
      inCode = !inCode;
      continue;
    }
    if (char === '|' && !inCode) return row.slice(start, index).trim();
  }
  return row.slice(start).trim();
}

function unescapedPipePositions(row) {
  const positions = [];
  let escaped = false;
  for (let index = 0; index < row.length; index += 1) {
    const char = row[index];
    if (escaped) {
      escaped = false;
      continue;
    }
    if (char === '\\') {
      escaped = true;
      continue;
    }
    if (char === '|') positions.push(index);
  }
  return positions;
}

function validateMarkdownTableColumns(file) {
  const lines = read(file).split(/\r?\n/);
  let inFence = false;
  let fenceMarker = null;
  let expectedColumns = null;
  let tableStartLine = null;
  let tables = 0;
  let rows = 0;

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const fence = line.match(/^\s*(`{3,}|~{3,})/);
    if (fence) {
      const marker = fence[1][0];
      if (!inFence) {
        inFence = true;
        fenceMarker = marker;
      } else if (marker === fenceMarker) {
        inFence = false;
        fenceMarker = null;
      }
      expectedColumns = null;
      tableStartLine = null;
      continue;
    }

    const isTableRow = !inFence && /^\s*\|/.test(line);
    if (!isTableRow) {
      expectedColumns = null;
      tableStartLine = null;
      continue;
    }

    const pipePositions = unescapedPipePositions(line);
    const lastNonWhitespace = line.search(/\s*$/) - 1;
    const hasLeadingDelimiter = pipePositions[0] === line.search(/\S/);
    const hasTrailingDelimiter = pipePositions.at(-1) === lastNonWhitespace;
    if (!hasLeadingDelimiter || !hasTrailingDelimiter || pipePositions.length < 2) {
      errors.push(`${relative(file)}:${index + 1} is not a closed Markdown table row`);
      expectedColumns = null;
      tableStartLine = null;
      continue;
    }

    const columns = pipePositions.length - 1;
    rows += 1;
    if (expectedColumns === null) {
      expectedColumns = columns;
      tableStartLine = index + 1;
      tables += 1;
    } else if (columns !== expectedColumns) {
      errors.push(`${relative(file)}:${index + 1} has ${columns} Markdown table columns; table at line ${tableStartLine} has ${expectedColumns}`);
    }
  }

  return { tables, rows };
}

function parseSourceLocation(location, label) {
  const match = String(location).match(/^(.*):(\d+)$/);
  if (!match) {
    errors.push(`${label} has invalid source location ${location}`);
    return;
  }
  validateSourceLine(match[1], Number(match[2]), label);
}

const lineCountCache = new Map();
function validateSourceLine(source, line, label) {
  const file = path.resolve(repoRoot, source);
  if (!fs.existsSync(file)) {
    errors.push(`${label} points to missing source ${source}`);
    return;
  }
  if (!lineCountCache.has(file)) lineCountCache.set(file, read(file).split(/\r?\n/).length);
  if (!Number.isInteger(line) || line < 1 || line > lineCountCache.get(file)) {
    errors.push(`${label} points outside ${source}: ${line}`);
  }
}

function validateExactFilePath(source, label) {
  let current = repoRoot;
  for (const segment of source.split('/')) {
    if (!fs.existsSync(current) || !fs.statSync(current).isDirectory()) {
      errors.push(`${label} traverses non-directory ${path.relative(repoRoot, current)}`);
      return;
    }
    const entries = fs.readdirSync(current);
    if (!entries.includes(segment)) {
      const caseInsensitiveMatch = entries.find((entry) => entry.toLowerCase() === segment.toLowerCase());
      const detail = caseInsensitiveMatch ? `; actual casing is ${caseInsensitiveMatch}` : '';
      errors.push(`${label} points to missing or case-mismatched path ${source}${detail}`);
      return;
    }
    current = path.join(current, segment);
  }
  if (!fs.statSync(current).isFile()) errors.push(`${label} does not point to a file: ${source}`);
}

function isRecord(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function requireFields(value, fields, label) {
  if (!isRecord(value)) {
    errors.push(`${label} is not an object`);
    return false;
  }
  for (const field of fields) {
    if (!Object.hasOwn(value, field)) errors.push(`${label} has no required field ${field}`);
  }
  return true;
}

function requireNonEmptyString(value, label) {
  if (typeof value !== 'string' || !value.trim()) errors.push(`${label} is not a non-empty string`);
}

function requireStringArray(value, label, allowEmpty = false) {
  if (!Array.isArray(value)) {
    errors.push(`${label} is not an array`);
    return;
  }
  if (!allowEmpty && value.length === 0) errors.push(`${label} is empty`);
  for (const [index, item] of value.entries()) {
    requireNonEmptyString(item, `${label}[${index}]`);
  }
}

function validateLocationArray(value, label, allowEmpty = false) {
  if (!Array.isArray(value)) {
    errors.push(`${label} is not an array`);
    return;
  }
  if (!allowEmpty && value.length === 0) errors.push(`${label} is empty`);
  for (const [index, location] of value.entries()) {
    const locationLabel = `${label}[${index}]`;
    if (!requireFields(location, ['source', 'line'], locationLabel)) continue;
    if (typeof location.source !== 'string' || !location.source.trim()) {
      errors.push(`${locationLabel}.source is not a non-empty string`);
      continue;
    }
    validateSourceLine(location.source, location.line, locationLabel);
  }
}

const requiredDocuments = [
  '00-methodology.md',
  '01-auth-shell.md',
  '02-background-dashboard.md',
  '03-hosts.md',
  '04-services-configs.md',
  '05-alerts.md',
  '06-stack-upgrades-admin.md',
  '07-cluster-installation.md',
  '08-kerberos.md',
  '09-namenode-journalnode-ha.md',
  '10-rm-ranger-ha.md',
  '11-federation-hawq.md',
  '12-views.md',
  '13-permissions-flags.md',
  SERVICE_THEME_MODULE,
  '15-react-gap-matrix.md',
  '16-five-pass-audit.md',
  'README.md',
  'api/README.md',
];
for (const document of requiredDocuments) {
  if (!fs.existsSync(path.join(baselineRoot, document))) errors.push(`Missing required document ${document}`);
}

const markdownFiles = listFiles(baselineRoot, (file) => file.endsWith('.md'));
const handWrittenMarkdownFiles = markdownFiles.filter((file) => !relative(file).startsWith('generated/'));
const markdownTableStats = handWrittenMarkdownFiles.reduce((stats, file) => {
  const fileStats = validateMarkdownTableColumns(file);
  stats.tables += fileStats.tables;
  stats.rows += fileStats.rows;
  return stats;
}, { tables: 0, rows: 0 });
const featureModuleFiles = markdownFiles.filter((file) =>
  path.dirname(file) === baselineRoot && /^(?:0[1-9]|1[0-4])-.*\.md$/.test(path.basename(file)),
);
const expectedModuleFileNames = Array.from({ length: 14 }, (_, index) =>
  String(index + 1).padStart(2, '0'),
);
for (const prefix of expectedModuleFileNames) {
  const matches = featureModuleFiles.filter((file) => path.basename(file).startsWith(`${prefix}-`));
  if (matches.length !== 1) errors.push(`Expected exactly one ${prefix}-*.md feature module, found ${matches.length}`);
}

const handWrittenFeatures = [];
const featureIdLocations = new Map();
const featureIdPattern = /^\|\s*([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)\s*\|/;
for (const file of featureModuleFiles) {
  const lines = read(file).split(/\r?\n/);
  let section = '';
  for (let index = 0; index < lines.length; index += 1) {
    const heading = lines[index].match(/^#{2,3}\s+(.+?)\s*$/);
    if (heading) section = heading[1];
    const feature = lines[index].match(featureIdPattern);
    if (!feature) continue;
    const item = {
      id: feature[1],
      summary: extractFirstCell(lines[index], feature[0].length),
      section,
      moduleFile: path.basename(file),
      line: index + 1,
    };
    handWrittenFeatures.push(item);
    if (!featureIdLocations.has(item.id)) featureIdLocations.set(item.id, []);
    featureIdLocations.get(item.id).push(`${relative(file)}:${item.line}`);
  }
}
for (const [featureId, locations] of featureIdLocations) {
  if (locations.length > 1) errors.push(`Duplicate feature ID ${featureId}: ${locations.join(', ')}`);
}
const currentFeatureIds = handWrittenFeatures.map((feature) => feature.id);
const currentNewFeatureIds = currentFeatureIds.filter((id) => ALLOWED_NEW_FEATURE_IDS.has(id));
const serviceThemeFeatureIds = handWrittenFeatures
  .filter((feature) => feature.moduleFile === SERVICE_THEME_MODULE)
  .map((feature) => feature.id);
const legacyFeatureIds = handWrittenFeatures
  .filter((feature) => feature.moduleFile !== SERVICE_THEME_MODULE && !ALLOWED_NEW_FEATURE_IDS.has(feature.id))
  .map((feature) => feature.id);
for (const id of ALLOWED_NEW_FEATURE_IDS) {
  if (!currentFeatureIds.includes(id)) errors.push(`Required post-freeze feature ID is missing: ${id}`);
}
if (currentNewFeatureIds.length !== ALLOWED_NEW_FEATURE_IDS.size) {
  errors.push(`Expected exactly ${ALLOWED_NEW_FEATURE_IDS.size} post-freeze feature IDs, found ${currentNewFeatureIds.length}`);
}
if (legacyFeatureIds.length !== LEGACY_FEATURE_ID_COUNT) {
  errors.push(`Expected ${LEGACY_FEATURE_ID_COUNT} legacy feature IDs after filtering allowed additions, found ${legacyFeatureIds.length}`);
}
const legacyFeatureIdHash = sha256Json(legacyFeatureIds);
if (legacyFeatureIdHash !== LEGACY_FEATURE_ID_SEQUENCE_SHA256) {
  errors.push(`Legacy feature ID sequence changed: ${legacyFeatureIdHash}`);
}
if (serviceThemeFeatureIds.length !== SERVICE_THEME_FEATURE_ID_COUNT) {
  errors.push(`Expected ${SERVICE_THEME_FEATURE_ID_COUNT} Service Theme feature IDs, found ${serviceThemeFeatureIds.length}`);
}
const serviceThemeFeatureIdHash = sha256Json(serviceThemeFeatureIds);
if (serviceThemeFeatureIdHash !== SERVICE_THEME_FEATURE_ID_SEQUENCE_SHA256) {
  errors.push(`Service Theme feature ID sequence changed: ${serviceThemeFeatureIdHash}`);
}

// Audit complete classic source paths written as code spans in module table rows.
// Directory shorthand remains useful prose but is not a machine-checkable file reference.
const inlineCodePattern = /`([^`\r\n]+)`/g;
const moduleSourceReferencePattern = /^((?:ambari-web\/classic\/)?(?:app|test|vendor)\/[A-Za-z0-9_./-]+\.(?:js|hbs))(?:(?:#[A-Za-z0-9_./#-]+)|(?::\d+(?:-\d+)?))?$/;
const handWrittenSourceReferences = [];
for (const file of featureModuleFiles) {
  for (const [index, line] of read(file).split(/\r?\n/).entries()) {
    if (!/^\s*\|/.test(line)) continue;
    for (const match of line.matchAll(inlineCodePattern)) {
      const sourceMatch = match[1].match(moduleSourceReferencePattern);
      if (!sourceMatch) continue;
      const source = `ambari-web/classic/${sourceMatch[1].replace(/^ambari-web\/classic\//, '')}`;
      handWrittenSourceReferences.push(source);
      validateExactFilePath(source, `${relative(file)}:${index + 1} source reference`);
    }
  }
}
const uniqueHandWrittenSourceReferences = new Set(handWrittenSourceReferences);

const classicTestRoot = path.join(repoRoot, 'ambari-web/classic/test');
const classicTestJsFiles = listFiles(classicTestRoot, (file) => file.endsWith('.js'));
const classicDiskTestModules = classicTestJsFiles
  .filter((file) => file.endsWith('_test.js'))
  .map((file) => path.relative(path.join(repoRoot, 'ambari-web/classic'), file).split(path.sep).join('/').replace(/\.js$/, ''));
const testManifestFile = path.join(repoRoot, 'ambari-web/classic/app/assets/test/tests.js');
const testManifestSource = read(testManifestFile);
const testManifestBlock = testManifestSource.match(/var files\s*=\s*\[([\s\S]*?)\n\];/);
const testManifestEntries = testManifestBlock
  ? [...testManifestBlock[1].matchAll(/^\s*'([^']+)'\s*,?\s*(?:\/\/.*)?$/gm)].map((match) => match[1])
  : [];
if (!testManifestBlock) errors.push('Could not parse test manifest files array');
const testManifestReferences = testManifestEntries.filter((name) => /_test$/.test(name));
const uniqueTestManifestReferences = new Set(testManifestReferences);
const testManifestReferenceCounts = new Map();
for (const name of testManifestReferences) {
  testManifestReferenceCounts.set(name, (testManifestReferenceCounts.get(name) ?? 0) + 1);
}
const duplicateTestManifestReferences = [...testManifestReferenceCounts]
  .filter(([, count]) => count > 1)
  .sort(([left], [right]) => left.localeCompare(right));
const diskTestsNotLoaded = classicDiskTestModules
  .filter((name) => !uniqueTestManifestReferences.has(name))
  .sort();
const manifestTestsMissingOnDisk = [...uniqueTestManifestReferences]
  .filter((name) => !classicDiskTestModules.includes(name))
  .sort();
const testSkipStats = classicTestJsFiles.reduce((stats, file) => {
  const source = read(file);
  const describeSkip = source.match(/\bdescribe\.skip\s*\(/g)?.length ?? 0;
  const itSkip = source.match(/\bit\.skip\s*\(/g)?.length ?? 0;
  stats.describeSkip += describeSkip;
  stats.itSkip += itSkip;
  if (describeSkip + itSkip > 0) stats.files += 1;
  return stats;
}, { files: 0, describeSkip: 0, itSkip: 0 });

const expectedClassicTestAudit = {
  jsFiles: 546,
  diskTestModules: 500,
  manifestEntries: 499,
  manifestTestReferences: 498,
  uniqueManifestTestReferences: 497,
  initializationModules: ['test/init_computed_aliases'],
  duplicateReferences: [['test/utils/config_test', 2]],
  diskTestsNotLoaded: [
    'test/data/configs/wizards/secure_mapping_test',
    'test/mappers/configs/stack_config_properties_mapper_test',
    'test/views/main/charts/heatmap/heatmap_rack_test',
  ],
  skipFiles: 52,
  describeSkip: 59,
  itSkip: 22,
};
const actualClassicTestAudit = {
  jsFiles: classicTestJsFiles.length,
  diskTestModules: classicDiskTestModules.length,
  manifestEntries: testManifestEntries.length,
  manifestTestReferences: testManifestReferences.length,
  uniqueManifestTestReferences: uniqueTestManifestReferences.size,
  initializationModules: testManifestEntries.filter((name) => !/_test$/.test(name)),
  duplicateReferences: duplicateTestManifestReferences,
  diskTestsNotLoaded,
  skipFiles: testSkipStats.files,
  describeSkip: testSkipStats.describeSkip,
  itSkip: testSkipStats.itSkip,
};
if (JSON.stringify(actualClassicTestAudit) !== JSON.stringify(expectedClassicTestAudit)) {
  errors.push(`Classic test audit differs from frozen baseline: ${JSON.stringify(actualClassicTestAudit)}`);
}
if (manifestTestsMissingOnDisk.length > 0) {
  errors.push(`Test manifest references missing modules: ${manifestTestsMissingOnDisk.join(', ')}`);
}

const markdownLinkPattern = /\[[^\]]*\]\(([^)]+)\)/g;
let checkedLinks = 0;
for (const file of markdownFiles) {
  const source = read(file);
  let match;
  while ((match = markdownLinkPattern.exec(source))) {
    const rawTarget = match[1].trim();
    if (!rawTarget || rawTarget.startsWith('#') || /^[a-z]+:/i.test(rawTarget)) continue;
    const targetWithoutAnchor = decodeURIComponent(rawTarget.split('#')[0]);
    if (!targetWithoutAnchor) continue;
    checkedLinks += 1;
    const target = path.resolve(path.dirname(file), targetWithoutAnchor);
    if (!fs.existsSync(target)) {
      errors.push(`${relative(file)}:${countLinesBefore(source, match.index)} links to missing ${rawTarget}`);
    }
  }
}

const catalogs = {
  ajaxEndpoints: {
    json: 'ajax-endpoints.json',
    markdown: 'ajax-endpoints.md',
    pattern: /- Included definitions: (\d+)/,
    label: 'included AJAX definitions',
  },
  excludedMetricsDefinitions: {
    json: 'excluded-metrics-ajax-endpoints.json',
    markdown: 'ajax-endpoints.md',
    pattern: /- Excluded Metrics definitions: (\d+)/,
    label: 'excluded Metrics AJAX definitions',
  },
  ajaxCalls: {
    json: 'ajax-calls.json',
    markdown: 'ajax-calls.md',
    pattern: /(\d+) call sites:/,
    label: 'AJAX calls',
  },
  directHttpCalls: {
    json: 'direct-http-calls.json',
    markdown: 'direct-http-calls.md',
    pattern: /(\d+) static call sites\./,
    label: 'direct HTTP calls',
  },
  browserNetworkEntrypoints: {
    json: 'browser-network-entrypoints.json',
    markdown: 'browser-network-entrypoints.md',
    pattern: /(\d+) candidate call sites\./,
    label: 'browser network entrypoints',
  },
  clientConfigDownloads: {
    json: 'client-config-downloads.json',
    markdown: 'client-config-downloads.md',
    pattern: /(\d+) resource scopes\./,
    label: 'client config download scopes',
  },
  permissions: {
    json: 'permissions.json',
    markdown: 'permissions.md',
    pattern: /(\d+) distinct names\./,
    label: 'permissions',
  },
  featureFlags: {
    json: 'feature-flags.json',
    markdown: 'feature-flags.md',
    pattern: /(\d+) distinct names\./,
    label: 'feature flags',
  },
  routes: {
    json: 'routes.json',
    markdown: 'routes.md',
    pattern: /(\d+) non-Metrics route fragments\./,
    label: 'route fragments',
  },
  templateActions: {
    json: 'template-actions.json',
    markdown: 'template-actions.md',
    pattern: /(\d+) distinct action names\./,
    label: 'template actions',
  },
  featureIndex: {
    json: 'feature-index.json',
    markdown: 'feature-index.md',
    pattern: /(\d+) stable feature IDs\./,
    label: 'feature IDs',
  },
};

for (const catalog of Object.values(catalogs)) {
  catalog.jsonFile = path.join(generatedRoot, catalog.json);
  catalog.markdownFile = path.join(generatedRoot, catalog.markdown);
  if (!fs.existsSync(catalog.jsonFile)) errors.push(`Missing generated/${catalog.json}`);
  if (!fs.existsSync(catalog.markdownFile)) errors.push(`Missing generated/${catalog.markdown}`);
  catalog.items = fs.existsSync(catalog.jsonFile) ? readJson(catalog.jsonFile) : [];
  catalog.markdownCount = fs.existsSync(catalog.markdownFile)
    ? parseCount(catalog.markdownFile, catalog.pattern, catalog.label)
    : Number.NaN;
  if (catalog.items.length !== catalog.markdownCount) {
    errors.push(`${catalog.json} has ${catalog.items.length} records but Markdown reports ${catalog.markdownCount}`);
  }
}

const realtimeCatalog = {
  jsonFile: path.join(generatedRoot, 'realtime-channels.json'),
  markdownFile: path.join(generatedRoot, 'realtime-channels.md'),
};
if (!fs.existsSync(realtimeCatalog.jsonFile)) errors.push('Missing generated/realtime-channels.json');
if (!fs.existsSync(realtimeCatalog.markdownFile)) errors.push('Missing generated/realtime-channels.md');
realtimeCatalog.value = fs.existsSync(realtimeCatalog.jsonFile)
  ? readJson(realtimeCatalog.jsonFile)
  : {};

const expectedRealtimeTopLevelKeys = ['lifecycle', 'subscriptions', 'transports'];
const actualRealtimeTopLevelKeys = isRecord(realtimeCatalog.value)
  ? Object.keys(realtimeCatalog.value).sort()
  : [];
if (JSON.stringify(actualRealtimeTopLevelKeys) !== JSON.stringify(expectedRealtimeTopLevelKeys)) {
  errors.push(`realtime-channels.json top-level keys are ${actualRealtimeTopLevelKeys.join(', ') || 'invalid'}, expected ${expectedRealtimeTopLevelKeys.join(', ')}`);
}
if (JSON.stringify(realtimeCatalog.value) !== JSON.stringify(realtimeChannelsContract)) {
  errors.push('realtime-channels.json differs from contracts/realtime-channels.mjs');
}

const realtimeTransports = Array.isArray(realtimeCatalog.value.transports)
  ? realtimeCatalog.value.transports
  : [];
const realtimeSubscriptions = Array.isArray(realtimeCatalog.value.subscriptions)
  ? realtimeCatalog.value.subscriptions
  : [];
const realtimeLifecycle = Array.isArray(realtimeCatalog.value.lifecycle)
  ? realtimeCatalog.value.lifecycle
  : [];
if (!Array.isArray(realtimeCatalog.value.transports)) errors.push('realtime-channels.json transports is not an array');
if (!Array.isArray(realtimeCatalog.value.subscriptions)) errors.push('realtime-channels.json subscriptions is not an array');
if (!Array.isArray(realtimeCatalog.value.lifecycle)) errors.push('realtime-channels.json lifecycle is not an array');
if (realtimeTransports.length !== 2) errors.push(`Expected 2 realtime transports, found ${realtimeTransports.length}`);
if (realtimeSubscriptions.length !== 11) errors.push(`Expected 11 realtime destinations, found ${realtimeSubscriptions.length}`);
if (realtimeLifecycle.length !== 4) errors.push(`Expected 4 realtime lifecycle contracts, found ${realtimeLifecycle.length}`);

const realtimeIds = [];
for (const [index, transport] of realtimeTransports.entries()) {
  const label = `realtime transport ${index + 1}`;
  if (!requireFields(transport, [
    'id', 'kind', 'urlTemplate', 'protocols', 'connectHeaders', 'heartbeat',
    'fallback', 'reconnect', 'sourceLocations', 'testLocations', 'failureBoundaries',
  ], label)) continue;
  realtimeIds.push(transport.id);
  for (const property of ['id', 'kind', 'urlTemplate']) requireNonEmptyString(transport[property], `${label}.${property}`);
  if (!/^RT-TRANSPORT-\d{3}$/.test(transport.id)) errors.push(`${label} has invalid ID ${transport.id}`);
  if (!requireFields(transport.protocols, ['socketSchemes', 'stompVersions'], `${label}.protocols`)) continue;
  requireStringArray(transport.protocols.socketSchemes, `${label}.protocols.socketSchemes`);
  requireStringArray(transport.protocols.stompVersions, `${label}.protocols.stompVersions`);
  if (transport.protocols.sockJsTransports !== undefined) {
    requireStringArray(transport.protocols.sockJsTransports, `${label}.protocols.sockJsTransports`);
  }
  if (!isRecord(transport.connectHeaders)) errors.push(`${label}.connectHeaders is not an object`);
  if (requireFields(transport.heartbeat, [
    'clientOutgoingMs', 'clientIncomingMs', 'serverDefaultMs', 'serverProperty',
  ], `${label}.heartbeat`)) {
    for (const property of ['clientOutgoingMs', 'clientIncomingMs', 'serverDefaultMs']) {
      if (!Number.isInteger(transport.heartbeat[property]) || transport.heartbeat[property] <= 0) {
        errors.push(`${label}.heartbeat.${property} is not a positive integer`);
      }
    }
    requireNonEmptyString(transport.heartbeat.serverProperty, `${label}.heartbeat.serverProperty`);
  }
  if (requireFields(transport.fallback, [
    'trigger', 'transportId', 'automaticAfterEstablishedConnection',
  ], `${label}.fallback`)) {
    requireNonEmptyString(transport.fallback.trigger, `${label}.fallback.trigger`);
    if (transport.fallback.transportId !== null) {
      requireNonEmptyString(transport.fallback.transportId, `${label}.fallback.transportId`);
    }
    if (typeof transport.fallback.automaticAfterEstablishedConnection !== 'boolean') {
      errors.push(`${label}.fallback.automaticAfterEstablishedConnection is not boolean`);
    }
  }
  if (requireFields(transport.reconnect, ['delayMs', 'policy', 'eventReplay'], `${label}.reconnect`)) {
    if (!Number.isInteger(transport.reconnect.delayMs) || transport.reconnect.delayMs < 0) {
      errors.push(`${label}.reconnect.delayMs is not a non-negative integer`);
    }
    requireNonEmptyString(transport.reconnect.policy, `${label}.reconnect.policy`);
    if (typeof transport.reconnect.eventReplay !== 'boolean') errors.push(`${label}.reconnect.eventReplay is not boolean`);
  }
  validateLocationArray(transport.sourceLocations, `${label}.sourceLocations`);
  validateLocationArray(transport.testLocations, `${label}.testLocations`);
  requireStringArray(transport.failureBoundaries, `${label}.failureBoundaries`);
}

const realtimeDestinations = [];
const realtimeCallSiteProperties = [
  'subscribeSites', 'addHandlerSites', 'removeHandlerSites', 'unsubscribeSites',
];
for (const [index, subscription] of realtimeSubscriptions.entries()) {
  const label = `realtime subscription ${index + 1}`;
  if (!requireFields(subscription, [
    'id', 'destinationTemplate', 'eventClass', 'eventSourceSites', 'payloadSchema',
    'consumedFields', ...realtimeCallSiteProperties, 'handlerChain', 'lifecycle',
    'restReconciliation', 'clusterFiltering', 'failureBoundaries', 'testLocations',
    'metricsDisposition',
  ], label)) continue;
  realtimeIds.push(subscription.id);
  realtimeDestinations.push(subscription.destinationTemplate);
  for (const property of [
    'id', 'destinationTemplate', 'eventClass', 'lifecycle', 'restReconciliation',
    'clusterFiltering', 'metricsDisposition',
  ]) requireNonEmptyString(subscription[property], `${label}.${property}`);
  if (!/^RT-SUB-\d{3}$/.test(subscription.id)) errors.push(`${label} has invalid ID ${subscription.id}`);
  if (!requireFields(subscription.payloadSchema, ['type', 'fields'], `${label}.payloadSchema`)) {
    // Shape errors have already been recorded.
  } else {
    requireNonEmptyString(subscription.payloadSchema.type, `${label}.payloadSchema.type`);
    if (!isRecord(subscription.payloadSchema.fields) || Object.keys(subscription.payloadSchema.fields).length === 0) {
      errors.push(`${label}.payloadSchema.fields is not a non-empty object`);
    }
  }
  requireStringArray(subscription.consumedFields, `${label}.consumedFields`);
  requireStringArray(subscription.handlerChain, `${label}.handlerChain`);
  requireStringArray(subscription.failureBoundaries, `${label}.failureBoundaries`);
  validateLocationArray(subscription.eventSourceSites, `${label}.eventSourceSites`);
  validateLocationArray(subscription.testLocations, `${label}.testLocations`);
  validateLocationArray(subscription.subscribeSites, `${label}.subscribeSites`);
  for (const property of ['addHandlerSites', 'removeHandlerSites', 'unsubscribeSites']) {
    validateLocationArray(subscription[property], `${label}.${property}`, true);
  }
}

for (const [index, entry] of realtimeLifecycle.entries()) {
  const label = `realtime lifecycle ${index + 1}`;
  if (!requireFields(entry, [
    'id', 'name', 'behavior', 'sourceLocations', 'testLocations', 'failureBoundaries',
  ], label)) continue;
  realtimeIds.push(entry.id);
  for (const property of ['id', 'name', 'behavior']) requireNonEmptyString(entry[property], `${label}.${property}`);
  if (!/^RT-LIFE-\d{3}$/.test(entry.id)) errors.push(`${label} has invalid ID ${entry.id}`);
  validateLocationArray(entry.sourceLocations, `${label}.sourceLocations`);
  validateLocationArray(entry.testLocations, `${label}.testLocations`);
  requireStringArray(entry.failureBoundaries, `${label}.failureBoundaries`);
}

if (new Set(realtimeIds).size !== realtimeIds.length) errors.push('realtime-channels.json contains duplicate IDs');
if (new Set(realtimeDestinations).size !== realtimeDestinations.length) {
  errors.push('realtime-channels.json contains duplicate destinationTemplate values');
}
const realtimeDynamicDestinations = realtimeDestinations.filter((destination) => destination.includes('{')).length;
const realtimeStaticDestinations = realtimeDestinations.length - realtimeDynamicDestinations;
if (realtimeStaticDestinations !== 10 || realtimeDynamicDestinations !== 1) {
  errors.push(`Expected 10 static and 1 dynamic realtime destinations, found ${realtimeStaticDestinations} static and ${realtimeDynamicDestinations} dynamic`);
}
const realtimeCallSiteCounts = Object.fromEntries(realtimeCallSiteProperties.map((property) => [
  property,
  realtimeSubscriptions.reduce(
    (count, subscription) => count + (Array.isArray(subscription[property]) ? subscription[property].length : 0),
    0,
  ),
]));
const expectedRealtimeCallSiteCounts = {
  subscribeSites: 11,
  addHandlerSites: 1,
  removeHandlerSites: 1,
  unsubscribeSites: 1,
};
for (const [property, expected] of Object.entries(expectedRealtimeCallSiteCounts)) {
  if (realtimeCallSiteCounts[property] !== expected) {
    errors.push(`Expected ${expected} realtime ${property}, found ${realtimeCallSiteCounts[property]}`);
  }
}

if (fs.existsSync(realtimeCatalog.markdownFile)) {
  const markdown = read(realtimeCatalog.markdownFile);
  const summaryPatterns = {
    transports: /- transports: (\d+)/,
    destinations: /- destinations: (\d+) \((\d+) static \+ (\d+) dynamic\)/,
    subscribeSites: /- subscribe sites: (\d+)/,
    addHandlerSites: /- addHandler sites: (\d+)/,
    removeHandlerSites: /- removeHandler sites: (\d+)/,
    unsubscribeSites: /- business unsubscribe sites: (\d+)/,
    lifecycle: /- lifecycle contracts: (\d+)/,
  };
  const summaryMatches = Object.fromEntries(Object.entries(summaryPatterns).map(([name, pattern]) => {
    const match = markdown.match(pattern);
    if (!match) errors.push(`Could not parse realtime Markdown ${name} count`);
    return [name, match];
  }));
  const expectedMarkdownCounts = {
    transports: realtimeTransports.length,
    destinations: realtimeSubscriptions.length,
    subscribeSites: realtimeCallSiteCounts.subscribeSites,
    addHandlerSites: realtimeCallSiteCounts.addHandlerSites,
    removeHandlerSites: realtimeCallSiteCounts.removeHandlerSites,
    unsubscribeSites: realtimeCallSiteCounts.unsubscribeSites,
    lifecycle: realtimeLifecycle.length,
  };
  for (const [name, expected] of Object.entries(expectedMarkdownCounts)) {
    const stated = Number(summaryMatches[name]?.[1]);
    if (Number.isInteger(stated) && stated !== expected) {
      errors.push(`realtime-channels.md reports ${stated} ${name}, expected ${expected}`);
    }
  }
  const statedStatic = Number(summaryMatches.destinations?.[2]);
  const statedDynamic = Number(summaryMatches.destinations?.[3]);
  if (Number.isInteger(statedStatic) && statedStatic !== realtimeStaticDestinations) {
    errors.push(`realtime-channels.md reports ${statedStatic} static destinations, expected ${realtimeStaticDestinations}`);
  }
  if (Number.isInteger(statedDynamic) && statedDynamic !== realtimeDynamicDestinations) {
    errors.push(`realtime-channels.md reports ${statedDynamic} dynamic destinations, expected ${realtimeDynamicDestinations}`);
  }
  for (const entry of [...realtimeTransports, ...realtimeSubscriptions, ...realtimeLifecycle]) {
    if (!markdown.includes(`\`${entry.id}\``)) errors.push(`realtime-channels.md does not contain ${entry.id}`);
  }
  for (const destination of realtimeDestinations) {
    if (!markdown.includes(`\`${destination}\``)) errors.push(`realtime-channels.md does not contain ${destination}`);
  }
}

const endpointNames = new Set(catalogs.ajaxEndpoints.items.map((item) => item.name));
const excludedNames = new Set(catalogs.excludedMetricsDefinitions.items.map((item) => item.name));
if (endpointNames.size !== catalogs.ajaxEndpoints.items.length) errors.push('ajax-endpoints.json contains duplicate request names');
if (excludedNames.size !== catalogs.excludedMetricsDefinitions.items.length) errors.push('excluded-metrics-ajax-endpoints.json contains duplicate request names');
for (const name of endpointNames) {
  if (excludedNames.has(name)) errors.push(`AJAX request ${name} is both included and Metrics-excluded`);
}
const metricLookingIncluded = [...endpointNames].filter((name) => /metrics?|heatmaps?|timeline|widgets?/i.test(name));
const allowedOperationalMetricRequests = new Set(['hosts.metrics.lazy_load']);
for (const name of metricLookingIncluded) {
  if (!allowedOperationalMetricRequests.has(name)) errors.push(`Metrics-looking request unexpectedly included: ${name}`);
}
for (const requiredName of allowedOperationalMetricRequests) {
  if (!endpointNames.has(requiredName)) errors.push(`Required operational metrics-field request is missing: ${requiredName}`);
}

const ajaxRegistrySource = read(path.join(repoRoot, 'ambari-web/classic/app/utils/ajax/ajax.js'));
let dynamicAjaxMethodDefinitions = 0;
for (const endpoint of [...catalogs.ajaxEndpoints.items, ...catalogs.excludedMetricsDefinitions.items]) {
  validateSourceLine(endpoint.source, endpoint.line, `AJAX definition ${endpoint.name}`);
  for (const caller of endpoint.callers ?? []) parseSourceLocation(caller, `AJAX definition caller ${endpoint.name}`);
  const label = `AJAX definition ${endpoint.name}`;
  requireStringArray(endpoint.methods, `${label}.methods`);
  for (const method of endpoint.methods ?? []) {
    if (!HTTP_METHODS.has(method)) errors.push(`${label} has invalid method ${method}`);
  }
  if (endpoint.methods?.includes('DYNAMIC')) dynamicAjaxMethodDefinitions += 1;
  for (const property of ['endpoint', 'endpointSource', 'apiPrefix', 'sourceContractSha256']) {
    requireNonEmptyString(endpoint[property], `${label}.${property}`);
  }
  if (!['real', 'format'].includes(endpoint.endpointSource)) {
    errors.push(`${label} has invalid endpointSource ${endpoint.endpointSource}`);
  }
  if (typeof endpoint.hasDynamicUrl !== 'boolean') errors.push(`${label}.hasDynamicUrl is not boolean`);
  if (typeof endpoint.hasFormat !== 'boolean') errors.push(`${label}.hasFormat is not boolean`);
  for (const property of ['formatTypeExpressions', 'formatUrlExpressions', 'inputKeys']) {
    requireStringArray(endpoint[property], `${label}.${property}`, true);
  }
  const objectSource = ajaxDefinitionObjectAtLine(ajaxRegistrySource, endpoint.line);
  const definitionLine = ajaxRegistrySource.split(/\r?\n/)[endpoint.line - 1] ?? '';
  if (!definitionLine.includes(`'${endpoint.name}'`) && !definitionLine.includes(`"${endpoint.name}"`)) {
    errors.push(`${label} name does not match ajax.js source line ${endpoint.line}`);
  }
  if (!objectSource) {
    errors.push(`${label} source object could not be reconstructed`);
  } else {
    if (sha256(objectSource) !== endpoint.sourceContractSha256) {
      errors.push(`${label} source contract SHA-256 differs from ajax.js`);
    }
    const expectedContract = expectedAjaxDefinitionContract(objectSource);
    for (const property of Object.keys(expectedContract)) {
      if (JSON.stringify(endpoint[property]) !== JSON.stringify(expectedContract[property])) {
        errors.push(`${label}.${property} differs from independent source extraction`);
      }
    }
  }
}
if (dynamicAjaxMethodDefinitions !== 0) {
  errors.push(`Expected no dynamic AJAX method definitions in the frozen registry, found ${dynamicAjaxMethodDefinitions}`);
}
const smokeEndpoint = catalogs.ajaxEndpoints.items.find((endpoint) => endpoint.name === 'service.item.smoke');
if (JSON.stringify(smokeEndpoint?.methods) !== JSON.stringify(['POST'])) {
  errors.push(`service.item.smoke method must be POST, found ${JSON.stringify(smokeEndpoint?.methods)}`);
}
for (const name of ['hosts.host_components.pre_load', 'hosts.metrics.lazy_load', 'hiveServerInteractive.getStatus']) {
  const endpoint = catalogs.ajaxEndpoints.items.find((item) => item.name === name);
  if (!endpoint?.hasDynamicUrl || endpoint.endpointSource !== 'format' || endpoint.endpoint === '(empty)') {
    errors.push(`${name} must expose its dynamic format() URL contract`);
  }
}

const ajaxCallsByStatus = { REGISTERED: 0, UNREGISTERED: 0, DYNAMIC: 0 };
const dynamicResolutionContractsByKey = new Map();
if (dynamicAjaxResolutions.length !== 27) {
  errors.push(`Expected 27 dynamic AJAX resolution contracts, found ${dynamicAjaxResolutions.length}`);
}
for (const [index, resolution] of dynamicAjaxResolutions.entries()) {
  const label = `dynamic AJAX resolution contract ${index + 1}`;
  const key = dynamicAjaxResolutionKey(resolution);
  validateSourceLine(resolution.source, resolution.line, label);
  if (dynamicResolutionContractsByKey.has(key)) {
    errors.push(`${label} duplicates ${resolution.source}:${resolution.line} (${resolution.requestExpression})`);
  } else {
    dynamicResolutionContractsByKey.set(key, resolution);
  }
  for (const property of ['source', 'requestExpression', 'dispatchKind', 'dispatchCondition', 'boundaryNotes']) {
    if (typeof resolution[property] !== 'string' || !resolution[property].trim()) {
      errors.push(`${label} has no ${property}`);
    }
  }
  for (const property of ['candidateRequestNames', 'evidence']) {
    if (!Array.isArray(resolution[property]) || resolution[property].length === 0) {
      errors.push(`${label} has no ${property}`);
    }
  }
  for (const name of resolution.candidateRequestNames ?? []) {
    if (!endpointNames.has(name)) errors.push(`${label} candidate ${name} is not an included AJAX endpoint`);
  }
}

const consumedDynamicResolutionKeys = new Set();
const dynamicCandidateNames = new Set();
const dynamicMissingCandidateNames = new Set();
const dynamicCallsByResolutionStatus = { RESOLVED_CLOSED: 0, RESOLVED_OPEN_BOUNDARY: 0 };
for (const [index, call] of catalogs.ajaxCalls.items.entries()) {
  const label = `AJAX call ${index + 1}`;
  validateSourceLine(call.source, call.line, label);
  if (!(call.registrationStatus in ajaxCallsByStatus)) {
    errors.push(`${label} has invalid registrationStatus ${call.registrationStatus}`);
    continue;
  }
  ajaxCallsByStatus[call.registrationStatus] += 1;
  if (call.registrationStatus === 'REGISTERED' && (!call.requestName || !endpointNames.has(call.requestName))) {
    errors.push(`${label} claims REGISTERED but request ${call.requestName} is not included`);
  }
  if (call.registrationStatus === 'UNREGISTERED' && (!call.requestName || endpointNames.has(call.requestName) || excludedNames.has(call.requestName))) {
    errors.push(`${label} has inconsistent UNREGISTERED request ${call.requestName}`);
  }
  if (call.registrationStatus === 'DYNAMIC' && call.requestName !== null) {
    errors.push(`${label} claims DYNAMIC but has static requestName ${call.requestName}`);
  }
  if (call.registrationStatus === 'DYNAMIC') {
    const key = dynamicAjaxResolutionKey(call);
    const resolution = dynamicResolutionContractsByKey.get(key);
    if (!resolution) {
      errors.push(`${label} has no unique contract for ${call.source}:${call.line} (${call.requestExpression})`);
      continue;
    }
    if (consumedDynamicResolutionKeys.has(key)) {
      errors.push(`${label} consumes dynamic contract ${call.source}:${call.line} more than once`);
    }
    consumedDynamicResolutionKeys.add(key);
    const expectedStatus = OPEN_DYNAMIC_AJAX_DISPATCH_KINDS.has(resolution.dispatchKind)
      ? 'RESOLVED_OPEN_BOUNDARY'
      : 'RESOLVED_CLOSED';
    if (!(call.resolutionStatus in dynamicCallsByResolutionStatus)) {
      errors.push(`${label} has invalid resolutionStatus ${call.resolutionStatus}`);
    } else {
      dynamicCallsByResolutionStatus[call.resolutionStatus] += 1;
    }
    if (call.resolutionStatus !== expectedStatus) {
      errors.push(`${label} resolutionStatus ${call.resolutionStatus} does not match ${resolution.dispatchKind}`);
    }
    for (const property of ['dispatchKind', 'candidateRequestNames', 'dispatchCondition', 'boundaryNotes', 'evidence']) {
      if (JSON.stringify(call[property]) !== JSON.stringify(resolution[property])) {
        errors.push(`${label} ${property} differs from its dynamic resolution contract`);
      }
    }
    for (const name of call.candidateRequestNames ?? []) {
      dynamicCandidateNames.add(name);
      if (!endpointNames.has(name)) dynamicMissingCandidateNames.add(name);
    }
  } else {
    for (const property of ['resolutionStatus', 'dispatchKind', 'candidateRequestNames', 'dispatchCondition', 'boundaryNotes', 'evidence']) {
      if (call[property] !== undefined && call[property] !== null) {
        errors.push(`${label} is not DYNAMIC but has ${property}`);
      }
    }
  }
}
for (const resolution of dynamicAjaxResolutions) {
  const key = dynamicAjaxResolutionKey(resolution);
  if (!consumedDynamicResolutionKeys.has(key)) {
    errors.push(`Dynamic AJAX resolution contract was not consumed: ${resolution.source}:${resolution.line} (${resolution.requestExpression})`);
  }
}
if (consumedDynamicResolutionKeys.size !== 27) {
  errors.push(`Expected 27 consumed dynamic AJAX resolution contracts, found ${consumedDynamicResolutionKeys.size}`);
}
if (dynamicCandidateNames.size !== 45) {
  errors.push(`Expected 45 unique dynamic AJAX candidates, found ${dynamicCandidateNames.size}`);
}
if (dynamicMissingCandidateNames.size !== 0) {
  errors.push(`Expected no unregistered dynamic AJAX candidates, found ${dynamicMissingCandidateNames.size}`);
}
const ajaxCallsMarkdown = read(catalogs.ajaxCalls.markdownFile);
const ajaxStatusMatch = ajaxCallsMarkdown.match(/(\d+) call sites: (\d+) match in-scope registered requests, (\d+) use static request names absent from the registry, and (\d+) use dynamic expressions\./);
if (!ajaxStatusMatch) {
  errors.push('Could not parse AJAX registration status counts from ajax-calls.md');
} else {
  const stated = ajaxStatusMatch.slice(2).map(Number);
  const actual = [ajaxCallsByStatus.REGISTERED, ajaxCallsByStatus.UNREGISTERED, ajaxCallsByStatus.DYNAMIC];
  if (stated.some((value, index) => value !== actual[index])) {
    errors.push(`AJAX status counts ${stated.join('/')} do not match JSON ${actual.join('/')}`);
  }
}
const dynamicResolutionSummaryMatch = ajaxCallsMarkdown.match(/Dynamic resolution: (\d+)\/(\d+) call sites have contracts, (\d+) unique candidate request names, and (\d+) candidates absent from the in-scope registry\./);
if (!dynamicResolutionSummaryMatch) {
  errors.push('Could not parse dynamic AJAX resolution summary from ajax-calls.md');
} else {
  const stated = dynamicResolutionSummaryMatch.slice(1).map(Number);
  const actual = [
    consumedDynamicResolutionKeys.size,
    ajaxCallsByStatus.DYNAMIC,
    dynamicCandidateNames.size,
    dynamicMissingCandidateNames.size,
  ];
  if (stated.some((value, index) => value !== actual[index])) {
    errors.push(`Dynamic AJAX resolution summary ${stated.join('/')} does not match JSON/contracts ${actual.join('/')}`);
  }
}

for (const key of ['directHttpCalls', 'browserNetworkEntrypoints', 'clientConfigDownloads', 'routes']) {
  for (const [index, item] of catalogs[key].items.entries()) {
    validateSourceLine(item.source, item.line, `${catalogs[key].label} record ${index + 1}`);
  }
}
const directSemanticKinds = new Set();
for (const [index, item] of catalogs.directHttpCalls.items.entries()) {
  const label = `direct HTTP record ${index + 1}`;
  for (const property of ['semanticKind', 'urlContract', 'notes']) {
    if (typeof item[property] !== 'string' || !item[property].trim()) errors.push(`${label} has no ${property}`);
  }
  if (!['NON_METRICS', 'MIXED'].includes(item.scope)) errors.push(`${label} has invalid scope ${item.scope}`);
  if (!Array.isArray(item.operationalFields) || item.operationalFields.length === 0) {
    errors.push(`${label} has no operationalFields`);
  }
  if (item.semanticKind) directSemanticKinds.add(item.semanticKind);
}
for (const requiredKind of ['ORIGINAL_REQUEST_REPLAY', 'HOST_MODEL_LOAD', 'COMPONENT_TOPOLOGY_LOAD', 'GENERIC_MUTATION_POLL_HELPER', 'HOST_DELETE_PREFLIGHT_LOAD']) {
  if (!directSemanticKinds.has(requiredKind)) errors.push(`Missing required direct HTTP semantic kind ${requiredKind}`);
}
const browserSemanticKinds = new Set();
const browserNetworkEffects = new Set(['REMOTE_REQUEST', 'NAVIGATION_REQUEST', 'LOCAL_ONLY', 'NO_NETWORK', 'CONDITIONAL']);
const browserRecordKeys = new Set();
for (const [index, item] of catalogs.browserNetworkEntrypoints.items.entries()) {
  const label = `browser network record ${index + 1}`;
  for (const property of ['semanticKind', 'networkEffect', 'urlContract', 'notes']) {
    if (typeof item[property] !== 'string' || !item[property].trim()) errors.push(`${label} has no ${property}`);
  }
  if (!browserNetworkEffects.has(item.networkEffect)) errors.push(`${label} has invalid networkEffect ${item.networkEffect}`);
  if (item.semanticKind) browserSemanticKinds.add(item.semanticKind);
  const recordKey = `${item.source}:${item.line}:${item.kind}`;
  if (browserRecordKeys.has(recordKey)) errors.push(`${label} duplicates ${recordKey}`);
  browserRecordKeys.add(recordKey);
}
for (const requiredKind of [
  'AMBARI_REST_DOWNLOAD', 'VIEW_WEB_CONTEXT', 'EXTERNAL_QUICK_LINK', 'ADMIN_VIEW_REDIRECT',
  'LOCAL_DOCUMENT', 'URL_PARSER_NO_NETWORK', 'PAGE_RELOAD', 'JWT_PROVIDER_REDIRECT',
  'PREFERRED_PATH_REDIRECT', 'NEW_UI_NAVIGATION', 'VIEW_ICON_RESOURCE', 'STATIC_NOTICE_DOCUMENT',
]) {
  if (!browserSemanticKinds.has(requiredKind)) errors.push(`Missing required browser semantic kind ${requiredKind}`);
}
for (const key of ['permissions', 'featureFlags']) {
  const names = catalogs[key].items.map((item) => item.name);
  if (new Set(names).size !== names.length) errors.push(`${catalogs[key].json} contains duplicate names`);
  for (const item of catalogs[key].items) {
    if ((item.callers?.length ?? 0) !== (item.uses?.length ?? 0)) {
      errors.push(`${key} ${item.name} has different caller/use counts`);
    }
    for (const use of item.uses ?? []) validateSourceLine(use.source, use.line, `${key} ${item.name}`);
  }
}
for (const action of catalogs.templateActions.items) {
  for (const caller of action.callers ?? []) parseSourceLocation(caller, `template action ${action.name}`);
}

const featureIndex = catalogs.featureIndex.items;
if (featureIndex.length !== handWrittenFeatures.length) {
  errors.push(`feature-index.json has ${featureIndex.length} records but modules contain ${handWrittenFeatures.length}`);
}
for (let index = 0; index < Math.max(featureIndex.length, handWrittenFeatures.length); index += 1) {
  const generated = featureIndex[index];
  const expected = handWrittenFeatures[index];
  if (JSON.stringify(generated) !== JSON.stringify(expected)) {
    errors.push(`Feature index differs from hand-written modules at record ${index + 1}: generated ${generated?.id ?? 'missing'}, expected ${expected?.id ?? 'missing'}`);
    break;
  }
}

const gateDocumentFile = path.join(baselineRoot, '13-permissions-flags.md');
if (fs.existsSync(gateDocumentFile)) {
  const gateDocument = read(gateDocumentFile);
  for (const key of ['permissions', 'featureFlags']) {
    for (const item of catalogs[key].items) {
      if (!gateDocument.includes(item.name)) errors.push(`13-permissions-flags.md does not index ${key} name ${item.name}`);
    }
  }
}

const clientScopes = new Set(catalogs.clientConfigDownloads.items.map((item) => item.resourceType));
for (const scope of ['CLUSTER', 'HOST', 'SERVICE', 'SERVICE_COMPONENT', 'HOST_COMPONENT']) {
  if (!clientScopes.has(scope)) errors.push(`Missing client config download scope ${scope}`);
}
if (clientScopes.size !== 5) errors.push(`Expected exactly 5 client config download scopes, found ${clientScopes.size}`);

const apiByModuleFiles = new Map([
  ['Authentication and Application Shell', 'auth-shell.md'],
  ['Installation Wizards', 'installation-wizards.md'],
  ['Hosts', 'hosts.md'],
  ['Services and Configs', 'services-configs.md'],
  ['Alerts', 'alerts.md'],
  ['Stack and Upgrades', 'stack-upgrades.md'],
  ['Security, HA, and Federation', 'security-ha-federation.md'],
  ['Views', 'views.md'],
  ['Background Operations and Common Capabilities', 'background-common.md'],
  ['Cross-Module and Manual Classification', 'cross-cutting.md'],
]);
for (const [moduleName, fileName] of apiByModuleFiles) {
  const file = path.join(generatedRoot, 'api-by-module', fileName);
  if (!fs.existsSync(file)) {
    errors.push(`Missing generated/api-by-module/${fileName}`);
    continue;
  }
  const expectedCount = catalogs.ajaxEndpoints.items.filter((item) => item.modules?.includes(moduleName)).length;
  const statedCount = parseCount(file, /(\d+) named request candidates\./, `${moduleName} API module candidate count`);
  if (expectedCount !== statedCount) {
    errors.push(`api-by-module/${fileName} reports ${statedCount}, expected ${expectedCount}`);
  }
  const moduleDefinitions = catalogs.ajaxEndpoints.items.filter((item) => item.modules?.includes(moduleName));
  const expectedContract = moduleDefinitions.map((definition) => ({
    name: definition.name,
    methods: definition.methods,
    endpoint: definition.endpoint,
    inputKeys: definition.inputKeys,
    callers: definition.callers,
  }));
  const statedHash = read(file).match(/Candidate content SHA-256: `([a-f0-9]{64})`/)?.[1];
  const expectedHash = sha256Json(expectedContract);
  if (statedHash !== expectedHash) {
    errors.push(`api-by-module/${fileName} content SHA-256 ${statedHash ?? 'missing'} does not match ${expectedHash}`);
  }
}

const readmeFile = path.join(baselineRoot, 'README.md');
if (fs.existsSync(readmeFile)) {
  const readme = read(readmeFile);
  const readmeCounts = readme.match(/(\d+) non-Metrics named AJAX definitions, (\d+) included call sites \((\d+) dynamic, (\d+) unregistered\), (\d+) direct HTTP call sites, (\d+) browser network candidates, (\d+) client-config downloads, (\d+) route fragments, (\d+) template actions, and (\d+) stable feature IDs/);
  if (!readmeCounts) {
    errors.push('README baseline count sentence is missing or has an unexpected format');
  } else {
    const actual = [
      catalogs.ajaxEndpoints.items.length,
      catalogs.ajaxCalls.items.length,
      ajaxCallsByStatus.DYNAMIC,
      ajaxCallsByStatus.UNREGISTERED,
      catalogs.directHttpCalls.items.length,
      catalogs.browserNetworkEntrypoints.items.length,
      catalogs.clientConfigDownloads.items.length,
      catalogs.routes.items.length,
      catalogs.templateActions.items.length,
      catalogs.featureIndex.items.length,
    ];
    const stated = readmeCounts.slice(1).map(Number);
    if (actual.some((value, index) => value !== stated[index])) {
      errors.push(`README counts ${stated.join('/')} do not match generated ${actual.join('/')}`);
    }
  }
}

const routeSourceFiles = new Set(catalogs.routes.items.map((item) => item.source)).size;
const distinctRouteFragments = new Set(catalogs.routes.items.map((item) => item.fragment)).size;
const templateActionOccurrences = catalogs.templateActions.items.reduce(
  (count, item) => count + (item.callers?.length ?? 0),
  0,
);
const permissionUses = catalogs.permissions.items.flatMap((item) => item.uses ?? []);
const permissionUseKindCounts = permissionUses.reduce((counts, use) => {
  counts[use.kind] = (counts[use.kind] ?? 0) + 1;
  return counts;
}, {});
const featureFlagUses = catalogs.featureFlags.items.flatMap((item) => item.uses ?? []);
const runtimeGates = handWrittenFeatures.filter((feature) => feature.id.startsWith('GATE-RUNTIME-')).length;
const realtimeLocationProperties = [
  'sourceLocations', 'testLocations', 'eventSourceSites',
  ...realtimeCallSiteProperties,
];
const realtimeLocations = [...realtimeTransports, ...realtimeSubscriptions, ...realtimeLifecycle]
  .flatMap((entry) => realtimeLocationProperties.flatMap((property) => entry[property] ?? []));
const realtimeUniqueLocations = new Set(
  realtimeLocations.map((location) => `${location.source}:${location.line}`),
);
const actualFivePassAuditCounts = {
  featureIds: handWrittenFeatures.length,
  routeRecords: catalogs.routes.items.length,
  routeSourceFiles,
  distinctRouteFragments,
  templateActionNames: catalogs.templateActions.items.length,
  templateActionOccurrences,
  ajaxDefinitions: catalogs.ajaxEndpoints.items.length,
  excludedMetricsDefinitions: catalogs.excludedMetricsDefinitions.items.length,
  ajaxCalls: catalogs.ajaxCalls.items.length,
  registeredAjaxCalls: ajaxCallsByStatus.REGISTERED,
  dynamicAjaxCalls: ajaxCallsByStatus.DYNAMIC,
  unregisteredAjaxCalls: ajaxCallsByStatus.UNREGISTERED,
  resolvedClosedDynamicCalls: dynamicCallsByResolutionStatus.RESOLVED_CLOSED,
  resolvedOpenDynamicCalls: dynamicCallsByResolutionStatus.RESOLVED_OPEN_BOUNDARY,
  uniqueDynamicCandidates: dynamicCandidateNames.size,
  directHttpCalls: catalogs.directHttpCalls.items.length,
  browserNetworkEntrypoints: catalogs.browserNetworkEntrypoints.items.length,
  clientConfigDownloadScopes: catalogs.clientConfigDownloads.items.length,
  permissions: catalogs.permissions.items.length,
  permissionUses: permissionUses.length,
  isAuthorizedUses: permissionUseKindCounts.isAuthorized ?? 0,
  havePermissionsUses: permissionUseKindCounts.havePermissions ?? 0,
  featureFlags: catalogs.featureFlags.items.length,
  featureFlagUses: featureFlagUses.length,
  runtimeGates,
  realtimeTransports: realtimeTransports.length,
  realtimeDestinations: realtimeSubscriptions.length,
  realtimeStaticDestinations,
  realtimeDynamicDestinations,
  realtimeLifecycleContracts: realtimeLifecycle.length,
  realtimeSubscribeSites: realtimeCallSiteCounts.subscribeSites,
  realtimeAddHandlerSites: realtimeCallSiteCounts.addHandlerSites,
  realtimeRemoveHandlerSites: realtimeCallSiteCounts.removeHandlerSites,
  realtimeUnsubscribeSites: realtimeCallSiteCounts.unsubscribeSites,
  realtimeLocationOccurrences: realtimeLocations.length,
  realtimeUniqueLocations: realtimeUniqueLocations.size,
  sourceReferenceOccurrences: handWrittenSourceReferences.length,
  sourceReferenceUniquePaths: uniqueHandWrittenSourceReferences.size,
  testJsFiles: actualClassicTestAudit.jsFiles,
  diskTestModules: actualClassicTestAudit.diskTestModules,
  manifestEntries: actualClassicTestAudit.manifestEntries,
  manifestTestReferences: actualClassicTestAudit.manifestTestReferences,
  uniqueManifestTestReferences: actualClassicTestAudit.uniqueManifestTestReferences,
  manifestInitializationModules: actualClassicTestAudit.initializationModules.length,
  duplicateManifestReferences: actualClassicTestAudit.duplicateReferences.length,
  diskTestsNotLoaded: actualClassicTestAudit.diskTestsNotLoaded.length,
  manifestTestsMissingOnDisk: manifestTestsMissingOnDisk.length,
  skipFiles: actualClassicTestAudit.skipFiles,
  describeSkipMarkers: actualClassicTestAudit.describeSkip,
  itSkipMarkers: actualClassicTestAudit.itSkip,
  skipMarkers: actualClassicTestAudit.describeSkip + actualClassicTestAudit.itSkip,
};
const fivePassAuditFile = path.join(baselineRoot, '16-five-pass-audit.md');
if (fs.existsSync(fivePassAuditFile)) {
  const auditSource = read(fivePassAuditFile);
  const auditCountBlock = auditSource.match(/## Machine-Frozen Counts[\s\S]*?```json\s*\n([\s\S]*?)\n```/);
  if (!auditCountBlock) {
    errors.push('16-five-pass-audit.md has no machine count JSON block');
  } else {
    try {
      const statedCounts = JSON.parse(auditCountBlock[1]);
      if (JSON.stringify(statedCounts) !== JSON.stringify(actualFivePassAuditCounts)) {
        errors.push(`16-five-pass-audit.md counts differ from current audit: stated ${JSON.stringify(statedCounts)}, actual ${JSON.stringify(actualFivePassAuditCounts)}`);
      }
    } catch (error) {
      errors.push(`16-five-pass-audit.md machine count JSON is invalid: ${error.message}`);
    }
  }
}

const evidenceMarkers = ['STATIC_ONLY', 'CONDITIONAL', 'PLACEHOLDER', 'OUT_OF_SCOPE', 'NEEDS_RUNTIME_VALIDATION'];
const evidenceCounts = Object.fromEntries(evidenceMarkers.map((marker) => [
  marker,
  featureModuleFiles.reduce((count, file) => count + (read(file).match(new RegExp(`\\b${marker}\\b`, 'g'))?.length ?? 0), 0),
]));
const metricsBoundaryPattern = /\b(?:metrics?|heatmaps?|horizon|timeline|widgets?|AMS|AMBARI_METRICS|Ganglia)\b/gi;
let metricsBoundaryMentions = 0;
for (const file of featureModuleFiles) metricsBoundaryMentions += read(file).match(metricsBoundaryPattern)?.length ?? 0;
if (metricsBoundaryMentions === 0) warnings.push('No Metrics boundary mentions found; explicit exclusions may have been removed');

const result = {
  moduleDocuments: featureModuleFiles.length,
  featureIds: handWrittenFeatures.length,
  markdownFiles: markdownFiles.length,
  handWrittenMarkdownTables: markdownTableStats.tables,
  handWrittenMarkdownTableRows: markdownTableStats.rows,
  handWrittenSourceReferences: {
    occurrences: handWrittenSourceReferences.length,
    uniquePaths: uniqueHandWrittenSourceReferences.size,
  },
  classicTestAudit: {
    ...actualClassicTestAudit,
    manifestTestsMissingOnDisk,
  },
  checkedRelativeLinks: checkedLinks,
  ajaxDefinitions: catalogs.ajaxEndpoints.items.length,
  excludedMetricsDefinitions: catalogs.excludedMetricsDefinitions.items.length,
  ajaxCalls: catalogs.ajaxCalls.items.length,
  ajaxCallStatuses: ajaxCallsByStatus,
  dynamicAjaxResolutions: {
    consumed: consumedDynamicResolutionKeys.size,
    uniqueCandidates: dynamicCandidateNames.size,
    missingCandidates: dynamicMissingCandidateNames.size,
    statuses: dynamicCallsByResolutionStatus,
  },
  directHttpCalls: catalogs.directHttpCalls.items.length,
  browserNetworkEntrypoints: catalogs.browserNetworkEntrypoints.items.length,
  clientConfigDownloadScopes: catalogs.clientConfigDownloads.items.length,
  permissions: catalogs.permissions.items.length,
  featureFlags: catalogs.featureFlags.items.length,
  routeFragments: catalogs.routes.items.length,
  templateActions: catalogs.templateActions.items.length,
  realtimeChannels: {
    transports: realtimeTransports.length,
    destinations: realtimeSubscriptions.length,
    staticDestinations: realtimeStaticDestinations,
    dynamicDestinations: realtimeDynamicDestinations,
    callSites: realtimeCallSiteCounts,
    lifecycle: realtimeLifecycle.length,
  },
  evidenceMarkers: evidenceCounts,
  metricsBoundaryMentionsForManualReview: metricsBoundaryMentions,
  warnings,
  errors,
};

console.log(JSON.stringify(result, null, 2));
if (errors.length) process.exitCode = 1;
