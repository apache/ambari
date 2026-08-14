#!/usr/bin/env node

/*
 * Generates traceable inventories for the classic Ambari Web application.
 * This intentionally uses only Node built-ins so it can run before npm install.
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import dynamicAjaxResolutions from './contracts/dynamic-ajax-resolutions.mjs';
import realtimeChannels from './contracts/realtime-channels.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '../../../..');
const classicRoot = path.join(repoRoot, 'ambari-web/classic');
const appRoot = path.join(classicRoot, 'app');
const baselineRoot = path.resolve(scriptDir, '..');
const outputRoot = path.resolve(scriptDir, '..', 'generated');
const ajaxFile = path.join(appRoot, 'utils/ajax/ajax.js');

const METRICS_MARKERS = [
  /metrics?/i,
  /heatmaps?/i,
  /horizon[_-]?chart/i,
  /\/charts?(?:\/|$)/i,
  /timeline/i,
  /ambari[_-]?metrics/i,
  /graph[_-]?(?:data|widget)/i,
  /chart[_-]?(?:data|widget)/i,
  /widgets?\.(?:wizard|layout|hostComponent|hosts)/i,
];

const METRICS_REQUEST_NAMES = [
  /(?:^|\.)metrics?(?:\.|$)/i,
  /(?:^|\.)heatmaps?(?:\.|$)/i,
  /^dashboard\.cluster_metrics\./i,
  /^namenode\.cpu_wio$/i,
  /^widgets?\./i,
  /^widget\.(?!activelayouts)/i,
];

const OPERATIONAL_REQUEST_NAMES = new Set([
  'hosts.ips',
  'hosts.metrics.lazy_load',
  'hiveServerInteractive.getStatus',
]);

const FEATURE_MODULE_FILE_PATTERN = /^(?:0[1-9]|1[0-3])-.*\.md$/;

const OPEN_DYNAMIC_AJAX_DISPATCH_KINDS = new Set([
  'PARAMETER_WRAPPER',
  'MODEL_METADATA_LOOKUP',
  'MIXIN_REQUEST_PROPERTY',
  'FIFO_OPTIONS_QUEUE',
]);

function toPosix(value) {
  return value.split(path.sep).join('/');
}

function relativeToRepo(file) {
  return toPosix(path.relative(repoRoot, file));
}

function isMetricsRelated(...values) {
  return METRICS_MARKERS.some((marker) =>
    values.some((value) => marker.test(String(value ?? ''))),
  );
}

function isMetricsUiFile(file) {
  const relative = toPosix(path.relative(appRoot, file));
  return [
    /^routes\/(?:create_widget|edit_widget)\.js$/,
    /^templates\/main\/service\/widgets(?:\/|$)/,
    /^templates\/main\/dashboard\/(?:widgets(?:\.hbs|\/)|edit_widget_popup(?:_single_threshold)?\.hbs$|plus_button_filter\.hbs$)/,
    /^templates\/common\/widget(?:\/|$)/,
    /^templates\/common\/modal_popups\/widget_browser_(?:footer|popup)\.hbs$/,
  ].some((pattern) => pattern.test(relative));
}

function isMetricsCaller(caller) {
  return isMetricsRelated(caller) || /\/mixins\/common\/widgets\//i.test(caller);
}

function isMetricsDefinition(definition) {
  if (OPERATIONAL_REQUEST_NAMES.has(definition.name)) return false;
  if (METRICS_REQUEST_NAMES.some((pattern) => pattern.test(definition.name))) return true;
  return definition.callers.length > 0 && definition.callers.every(isMetricsCaller);
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
      masked[index] = ' ';
      masked[index + 1] = ' ';
      index += 2;
      while (index < source.length && source[index] !== '\n') {
        masked[index] = source[index] === '\r' ? '\r' : ' ';
        index += 1;
      }
      index -= 1;
      continue;
    }
    if (char === '/' && next === '*') {
      masked[index] = ' ';
      masked[index + 1] = ' ';
      index += 2;
      while (index < source.length) {
        if (source[index] === '*' && source[index + 1] === '/') {
          masked[index] = ' ';
          masked[index + 1] = ' ';
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

function maskDelimitedComments(source, open, close) {
  const masked = source.split('');
  let cursor = 0;
  while (cursor < source.length) {
    const start = source.indexOf(open, cursor);
    if (start === -1) break;
    const closeIndex = source.indexOf(close, start + open.length);
    const end = closeIndex === -1 ? source.length : closeIndex + close.length;
    for (let index = start; index < end; index += 1) {
      if (source[index] !== '\n' && source[index] !== '\r') masked[index] = ' ';
    }
    cursor = end;
  }
  return masked.join('');
}

function maskHandlebarsComments(source) {
  let masked = maskDelimitedComments(source, '{{!--', '--}}');
  masked = maskDelimitedComments(masked, '{{!', '}}');
  return maskDelimitedComments(masked, '<!--', '-->');
}

function listFiles(root, predicate) {
  const files = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      files.push(...listFiles(fullPath, predicate));
    } else if (predicate(fullPath)) {
      files.push(fullPath);
    }
  }
  return files.sort();
}

function lineNumberAt(source, index) {
  let line = 1;
  for (let cursor = 0; cursor < index; cursor += 1) {
    if (source.charCodeAt(cursor) === 10) line += 1;
  }
  return line;
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
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = null;
      }
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
    if (char === closeChar) {
      depth -= 1;
      if (depth === 0) return index;
    }
  }
  throw new Error(`Unbalanced ${openChar}${closeChar} block at offset ${openIndex}`);
}

function extractTopLevelProperty(objectSource, propertyName) {
  const propertyPattern = new RegExp(`(?:^|\\n)\\s{4}(?:['\"]${propertyName}['\"]|${propertyName})\\s*:\\s*`, 'g');
  const match = propertyPattern.exec(objectSource);
  if (!match) return null;

  const start = match.index + match[0].length;
  let quote = null;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  let roundDepth = 0;
  let squareDepth = 0;
  let curlyDepth = 0;

  for (let index = start; index < objectSource.length; index += 1) {
    const char = objectSource[index];
    const next = objectSource[index + 1];
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
    if (char === '(') roundDepth += 1;
    if (char === ')') roundDepth -= 1;
    if (char === '[') squareDepth += 1;
    if (char === ']') squareDepth -= 1;
    if (char === '{') curlyDepth += 1;
    if (char === '}') {
      if (curlyDepth === 0 && roundDepth === 0 && squareDepth === 0) {
        return objectSource.slice(start, index).trim();
      }
      curlyDepth -= 1;
    }
    if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) {
      return objectSource.slice(start, index).trim();
    }
  }
  return objectSource.slice(start).trim();
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
      return end === -1 ? source.length : skipWhitespaceAndComments(source, end + 2);
    }
    break;
  }
  return index;
}

/**
 * Splits an object literal into its top-level properties. This deliberately
 * handles only ordinary identifier/string keys; spread and computed keys are
 * retained by callers as part of the raw expression instead of being guessed.
 */
function extractObjectProperties(objectSource) {
  const open = objectSource.indexOf('{');
  if (open === -1) return new Map();
  let close;
  try {
    close = scanBalanced(objectSource, open);
  } catch {
    return new Map();
  }

  const properties = new Map();
  let index = open + 1;
  while (index < close) {
    index = skipWhitespaceAndComments(objectSource, index);
    while (objectSource[index] === ',') {
      index = skipWhitespaceAndComments(objectSource, index + 1);
    }
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
      const match = objectSource.slice(index).match(/^[$A-Z_a-z][$\w]*/);
      if (match) {
        key = match[0];
        index += match[0].length;
      }
    }

    index = skipWhitespaceAndComments(objectSource, index);
    if (!key || objectSource[index] !== ':') {
      // Skip methods, spreads, and computed keys without pretending to parse them.
      let cursor = index;
      let roundDepth = 0;
      let squareDepth = 0;
      let curlyDepth = 0;
      let quote = null;
      let escaped = false;
      for (; cursor < close; cursor += 1) {
        const char = objectSource[cursor];
        if (quote) {
          if (escaped) escaped = false;
          else if (char === '\\') escaped = true;
          else if (char === quote) quote = null;
          continue;
        }
        if (char === "'" || char === '"' || char === '`') quote = char;
        else if (char === '(') roundDepth += 1;
        else if (char === ')') roundDepth -= 1;
        else if (char === '[') squareDepth += 1;
        else if (char === ']') squareDepth -= 1;
        else if (char === '{') curlyDepth += 1;
        else if (char === '}') curlyDepth -= 1;
        else if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
      }
      index = cursor + 1;
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

function extractReturnedObjectProperties(functionExpression) {
  if (!functionExpression) return [];
  const masked = maskJavaScriptComments(functionExpression);
  const returnedObjects = [];
  for (const match of masked.matchAll(/\breturn\b/g)) {
    const objectStart = skipWhitespaceAndComments(masked, match.index + match[0].length);
    if (masked[objectStart] !== '{') continue;
    let objectEnd;
    try {
      objectEnd = scanBalanced(functionExpression, objectStart);
    } catch {
      continue;
    }
    returnedObjects.push(extractObjectProperties(functionExpression.slice(objectStart, objectEnd + 1)));
  }
  return returnedObjects;
}

function staticHttpMethod(expression) {
  return expression?.match(/^(['"])([A-Z]+)\1$/)?.[2] ?? null;
}

function isStaticEmptyString(expression) {
  return /^(['"])\1$/.test(expression?.trim() ?? '');
}

function isStaticStringExpression(expression) {
  if (!expression) return false;
  const stringPattern = /(['"])((?:\\.|(?!\1).)*)\1/g;
  return expression.replace(stringPattern, '').replace(/[+\s()]/g, '') === '';
}

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function sha256Json(value) {
  return sha256(JSON.stringify(value));
}

function normalizeExpression(expression, maxLength = 180) {
  const value = String(expression ?? '').replace(/\s+/g, ' ').trim();
  return value.length <= maxLength ? value : `${value.slice(0, maxLength - 3)}...`;
}

function extractReferencedDataKeys(source) {
  const keys = new Set();
  for (const match of source.matchAll(/\bdata(?:\?\.)?\.([$A-Z_a-z][$\w]*)/g)) keys.add(match[1]);
  for (const match of source.matchAll(/\bdata\s*\[\s*(['"])([^'"\n]+)\1\s*\]/g)) keys.add(match[2]);
  return [...keys].sort();
}

function collapseStringExpression(expression) {
  if (!expression) return '';
  const stringParts = [];
  const stringPattern = /(['"])((?:\\.|(?!\1).)*)\1/g;
  let match;
  while ((match = stringPattern.exec(expression))) {
    stringParts.push(match[2].replace(/\\(['"\\])/g, '$1'));
  }
  const collapsed = stringParts.join('');
  if (!collapsed) return expression.replace(/\s+/g, ' ').trim();
  const isOnlyStringsAndConcatenation = expression
    .replace(stringPattern, '')
    .replace(/[+\s()]/g, '') === '';
  return isOnlyStringsAndConcatenation
    ? collapsed
    : `${collapsed} [dynamic: ${expression.replace(/\s+/g, ' ').trim()}]`;
}

function escapeMarkdown(value) {
  return String(value ?? '')
    .replace(/\|/g, '\\|')
    .replace(/\r?\n/g, ' ')
    .replace(/`/g, '\\`');
}

function extractAjaxDefinitions() {
  const source = fs.readFileSync(ajaxFile, 'utf8');
  const declaration = source.indexOf('var urls = {');
  if (declaration === -1) throw new Error('Could not find AJAX URL registry');
  const registryOpen = source.indexOf('{', declaration);
  const registryClose = scanBalanced(source, registryOpen);
  const registry = source.slice(registryOpen + 1, registryClose);
  const entryPattern = /^\s{2}(['"])([^'"\n]+)\1\s*:\s*\{/gm;
  const definitions = [];
  let match;

  while ((match = entryPattern.exec(registry))) {
    const entryOpen = registry.indexOf('{', match.index);
    const entryClose = scanBalanced(registry, entryOpen);
    const objectSource = registry.slice(entryOpen, entryClose + 1);
    const realExpression = extractTopLevelProperty(objectSource, 'real');
    const apiPrefixExpression = extractTopLevelProperty(objectSource, 'apiPrefix');
    const topLevelType = extractTopLevelProperty(objectSource, 'type');
    const formatExpression = extractTopLevelProperty(objectSource, 'format');
    const formatProperties = extractReturnedObjectProperties(formatExpression);
    const formatTypeExpressions = formatProperties
      .map((properties) => properties.get('type'))
      .filter((expression) => expression !== undefined);
    const formatUrlExpressions = formatProperties
      .map((properties) => properties.get('url'))
      .filter((expression) => expression !== undefined);
    const methods = new Set();
    const methodExpressions = [topLevelType, ...formatTypeExpressions]
      .filter((expression) => expression !== null && expression !== undefined);
    for (const expression of methodExpressions) {
      const method = staticHttpMethod(expression);
      if (method) methods.add(method);
    }
    if (methods.size === 0) methods.add('GET');
    const hasDynamicMethod = methodExpressions.some((expression) => !staticHttpMethod(expression));
    if (hasDynamicMethod) methods.add('DYNAMIC');

    const realIsEmpty = realExpression === null || isStaticEmptyString(realExpression);
    const usesFormatUrl = realIsEmpty && formatUrlExpressions.length > 0;
    const endpointExpressions = usesFormatUrl ? formatUrlExpressions : [realExpression].filter(Boolean);
    let endpoint = endpointExpressions.map(collapseStringExpression).join(' OR ');
    let endpointSource = 'real';
    if (usesFormatUrl) {
      endpointSource = 'format';
    }
    const endpointExpression = endpointExpressions.map((expression) => normalizeExpression(expression)).join(' OR ') || null;
    const hasDynamicUrl = endpointExpressions.some((expression) => !isStaticStringExpression(expression));
    const registryOffset = registryOpen + 1 + match.index;
    definitions.push({
      name: match[2],
      methods: [...methods].sort(),
      endpoint: endpoint || '(empty)',
      endpointSource,
      endpointExpression,
      hasDynamicUrl,
      apiPrefix: apiPrefixExpression === null ? '/api/v1 (default)' : collapseStringExpression(apiPrefixExpression) || '(empty)',
      hasFormat: formatExpression !== null,
      formatExpression,
      topLevelTypeExpression: topLevelType,
      formatTypeExpressions,
      formatUrlExpressions,
      inputKeys: extractReferencedDataKeys(objectSource),
      line: lineNumberAt(source, registryOffset),
      source: relativeToRepo(ajaxFile),
      sourceContractSha256: sha256(objectSource),
      objectSource,
    });
    entryPattern.lastIndex = entryClose + 1;
  }
  return definitions;
}

function extractAjaxCalls(jsFiles, includedRequestNames, allRequestNames) {
  const calls = [];
  const callPattern = /App\.ajax\.send\s*\(/g;
  for (const file of jsFiles) {
    if (file === ajaxFile || isMetricsUiFile(file)) continue;
    const isMetricsFile = isMetricsRelated(file);
    const originalSource = fs.readFileSync(file, 'utf8');
    const source = maskJavaScriptComments(originalSource);
    let match;
    while ((match = callPattern.exec(source))) {
      const argumentStart = skipWhitespaceAndComments(source, match.index + match[0].length);
      let argumentExpression;
      let properties = new Map();
      if (source[argumentStart] === '{') {
        let argumentEnd;
        try {
          argumentEnd = scanBalanced(source, argumentStart);
        } catch {
          argumentEnd = source.indexOf('\n', argumentStart);
          if (argumentEnd === -1) argumentEnd = source.length;
        }
        argumentExpression = originalSource.slice(argumentStart, argumentEnd + 1);
        properties = extractObjectProperties(argumentExpression);
        callPattern.lastIndex = Math.max(callPattern.lastIndex, argumentEnd + 1);
      } else {
        let cursor = argumentStart;
        let roundDepth = 0;
        let squareDepth = 0;
        let curlyDepth = 0;
        let quote = null;
        let escaped = false;
        for (; cursor < source.length; cursor += 1) {
          const char = source[cursor];
          if (quote) {
            if (escaped) escaped = false;
            else if (char === '\\') escaped = true;
            else if (char === quote) quote = null;
            continue;
          }
          if (char === "'" || char === '"' || char === '`') quote = char;
          else if (char === '(') roundDepth += 1;
          else if (char === ')') {
            if (roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
            roundDepth -= 1;
          } else if (char === '[') squareDepth += 1;
          else if (char === ']') squareDepth -= 1;
          else if (char === '{') curlyDepth += 1;
          else if (char === '}') curlyDepth -= 1;
          else if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
        }
        argumentExpression = originalSource.slice(argumentStart, cursor);
      }

      const nameExpression = properties.get('name');
      const staticNameMatch = nameExpression?.match(/^(['"])([^'"\n]+)\1$/);
      if (isMetricsFile && !OPERATIONAL_REQUEST_NAMES.has(staticNameMatch?.[2])) continue;
      if (staticNameMatch && allRequestNames.has(staticNameMatch[2])
          && !includedRequestNames.has(staticNameMatch[2])) continue;
      const dataExpression = properties.get('data');
      const dataProperties = dataExpression?.trim().startsWith('{')
        ? [...extractObjectProperties(dataExpression).keys()]
        : [];
      const callbacks = ['success', 'error', 'callback', 'complete', 'statusCode']
        .filter((key) => properties.has(key));
      calls.push({
        requestName: staticNameMatch?.[2] ?? null,
        requestExpression: normalizeExpression(nameExpression || argumentExpression),
        registrationStatus: staticNameMatch
          ? (includedRequestNames.has(staticNameMatch[2]) ? 'REGISTERED' : 'UNREGISTERED')
          : 'DYNAMIC',
        dataKeys: dataProperties.sort(),
        dataExpression: dataExpression && dataProperties.length === 0 ? normalizeExpression(dataExpression) : null,
        callbacks,
        source: relativeToRepo(file),
        line: lineNumberAt(source, match.index),
      });
    }
  }
  return calls;
}

function dynamicAjaxResolutionKey({ source, line, requestExpression }) {
  return `${source}\u0000${line}\u0000${requestExpression}`;
}

function attachDynamicAjaxResolutions(calls) {
  const resolutionsByKey = new Map();
  for (const resolution of dynamicAjaxResolutions) {
    const key = dynamicAjaxResolutionKey(resolution);
    if (resolutionsByKey.has(key)) {
      throw new Error(`Duplicate dynamic AJAX resolution contract for ${resolution.source}:${resolution.line}`);
    }
    resolutionsByKey.set(key, resolution);
  }

  const consumedKeys = new Set();
  const resolvedCalls = calls.map((call) => {
    if (call.registrationStatus !== 'DYNAMIC') return call;
    const key = dynamicAjaxResolutionKey(call);
    const resolution = resolutionsByKey.get(key);
    if (!resolution) {
      throw new Error(`Dynamic AJAX call ${call.source}:${call.line} (${call.requestExpression}) has no exact resolution contract`);
    }
    if (consumedKeys.has(key)) {
      throw new Error(`Dynamic AJAX resolution contract was consumed more than once at ${call.source}:${call.line}`);
    }
    consumedKeys.add(key);
    return {
      ...call,
      resolutionStatus: OPEN_DYNAMIC_AJAX_DISPATCH_KINDS.has(resolution.dispatchKind)
        ? 'RESOLVED_OPEN_BOUNDARY'
        : 'RESOLVED_CLOSED',
      dispatchKind: resolution.dispatchKind,
      candidateRequestNames: resolution.candidateRequestNames,
      dispatchCondition: resolution.dispatchCondition,
      boundaryNotes: resolution.boundaryNotes,
      evidence: resolution.evidence,
    };
  });

  const unused = dynamicAjaxResolutions.filter((resolution) =>
    !consumedKeys.has(dynamicAjaxResolutionKey(resolution)));
  if (unused.length) {
    throw new Error(`Unused dynamic AJAX resolution contracts: ${unused.map((item) => `${item.source}:${item.line}`).join(', ')}`);
  }
  return resolvedCalls;
}

function extractPermissionUses(jsFiles, templateFiles) {
  const uses = new Map();
  const addUse = (permissions, gate, file, source, index) => {
    for (const permission of permissions.split(',').map((item) => item.trim()).filter(Boolean)) {
      if (isMetricsRelated(permission)) continue;
      if (!uses.has(permission)) uses.set(permission, []);
      uses.get(permission).push({
        kind: gate,
        source: relativeToRepo(file),
        line: lineNumberAt(source, index),
      });
    }
  };
  const permissionPattern = /\b(?:App\.)?(isAuthorized|havePermissions)\s*\(\s*(['"])([^'"\n]+)\2\s*\)/g;
  for (const file of jsFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    let match;
    while ((match = permissionPattern.exec(source))) {
      addUse(match[3], match[1], file, source, match.index);
    }
  }
  const templatePattern = /{{#?(isAuthorized|havePermissions)\s+(['"])([^'"\n]+)\2/g;
  for (const file of templateFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    let match;
    while ((match = templatePattern.exec(source))) {
      addUse(match[3], match[1], file, source, match.index);
    }
  }
  return [...uses.entries()]
    .map(([name, rawUses]) => {
      const uniqueUses = [...new Map(rawUses.map((use) => [
        `${use.kind}:${use.source}:${use.line}`,
        use,
      ])).values()];
      return {
        name,
        kinds: [...new Set(uniqueUses.map((use) => use.kind))].sort(),
        callers: uniqueUses.map((use) => `${use.source}:${use.line}`),
        uses: uniqueUses,
      };
    })
    .sort((left, right) => left.name.localeCompare(right.name));
}

function extractFeatureFlagUses(jsFiles, templateFiles) {
  const uses = new Map();
  const patterns = [
    /App\.supports\.([$A-Z_a-z][$\w]*)/g,
    /App\.get\(\s*(['"])supports\.([$A-Z_a-z][$\w]*)\1\s*\)/g,
    /(?:this\.)?get\(\s*(['"])App\.supports\.([$A-Z_a-z][$\w]*)\1\s*\)/g,
  ];
  for (const file of jsFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    for (const pattern of patterns) {
      let match;
      while ((match = pattern.exec(source))) {
        const name = match[2] ?? match[1];
        if (!name || isMetricsRelated(name)) continue;
        if (!uses.has(name)) uses.set(name, []);
        uses.get(name).push({
          kind: 'JavaScript',
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
        });
      }
    }
  }
  const templatePattern = /\bApp\.supports\.([$A-Z_a-z][$\w]*)/g;
  for (const file of templateFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    let match;
    while ((match = templatePattern.exec(source))) {
      if (isMetricsRelated(match[1])) continue;
      if (!uses.has(match[1])) uses.set(match[1], []);
      uses.get(match[1]).push({
        kind: 'Handlebars',
        source: relativeToRepo(file),
        line: lineNumberAt(source, match.index),
      });
    }
  }
  return [...uses.entries()]
    .map(([name, rawUses]) => {
      const uniqueUses = [...new Map(rawUses.map((use) => [
        `${use.kind}:${use.source}:${use.line}`,
        use,
      ])).values()];
      return {
        name,
        kinds: [...new Set(uniqueUses.map((use) => use.kind))].sort(),
        callers: uniqueUses.map((use) => `${use.source}:${use.line}`),
        uses: uniqueUses,
      };
    })
    .sort((left, right) => left.name.localeCompare(right.name));
}

function findDefinitionCallers(definitions, jsFiles) {
  const searchableFiles = jsFiles.filter((file) => file !== ajaxFile);
  const fileContents = searchableFiles.map((file) => ({
    file,
    source: maskJavaScriptComments(fs.readFileSync(file, 'utf8')),
  }));

  for (const definition of definitions) {
    definition.callers = [];
    const quotedName = new RegExp(`(['"])${definition.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\1`, 'g');
    for (const { file, source } of fileContents) {
      let match;
      while ((match = quotedName.exec(source))) {
        definition.callers.push(`${relativeToRepo(file)}:${lineNumberAt(source, match.index)}`);
      }
    }
  }
}

function classifyModules(definition) {
  const evidence = `${definition.name} ${definition.callers.join(' ')}`;
  const modules = [];
  const rules = [
    ['Authentication and Application Shell', /router|login|application|user_settings|inactiv|keepalive/i],
    ['Installation Wizards', /installer|wizard\/step|controllers\/wizard|bootstrap|recommendation|blueprint/i],
    ['Hosts', /main\/host|hosts?|host_component/i],
    ['Services and Configs', /main\/service|service_config|config_group|configs?\//i],
    ['Alerts', /alerts?|alert_/i],
    ['Stack and Upgrades', /stack_and_upgrade|stack_upgrade|repo|version_definition|upgrades?/i],
    ['Security, HA, and Federation', /kerberos|security|highAvailability|federation|journal|namenode|resourceManager|rangerAdmin|hawq/i],
    ['Views', /main\/views|routes\/view|views?\./i],
    ['Background Operations and Common Capabilities', /background|request|cluster_controller|update_controller|utils\//i],
  ];
  for (const [name, pattern] of rules) {
    if (pattern.test(evidence)) modules.push(name);
  }
  return modules.length ? modules : ['Cross-Module and Manual Classification'];
}

function classifyModule(definition) {
  return classifyModules(definition).join(', ');
}

const DIRECT_HTTP_CONTRACTS = {
  'ambari-web/classic/app/controllers/global/cluster_controller.js:194': {
    semanticKind: 'CLUSTER_MODEL_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}?fields=Clusters',
    operationalFields: ['Clusters/*'],
    notes: 'Initial load of core cluster identity/security/credential-store models; complete refreshes isCredentialStorePersistent after the mapper runs.',
  },
  'ambari-web/classic/app/controllers/global/cluster_controller.js:384': {
    semanticKind: 'DYNAMIC_HOST_MODEL_LOAD_HELPER',
    urlContract: 'GET {callerSuppliedRealUrl}; test mode uses /data/hosts/HDP2/hosts.json',
    operationalFields: ['caller supplied'],
    notes: '`requestHosts()` has no production caller in the classic app tree and is used only by a controller unit test; retained as a STATIC_ONLY legacy helper.',
  },
  'ambari-web/classic/app/controllers/global/cluster_controller.js:449': {
    semanticKind: 'CLUSTER_MODEL_REFRESH',
    urlContract: 'GET /api/v1/clusters/{clusterName}?fields=Clusters',
    operationalFields: ['Clusters/*'],
    notes: 'Refreshes the cluster mapper at runtime; the complete callback is empty.',
  },
  'ambari-web/classic/app/controllers/global/cluster_controller.js:485': {
    semanticKind: 'ORIGINAL_REQUEST_REPLAY',
    urlContract: '{ajaxOpt.type} {ajaxOpt.url}; payload/headers/callbacks are the original failed jQuery request',
    operationalFields: ['original request dependent'],
    notes: 'Saves the KDC credential first, then replays `ajaxOpt` unchanged; this is not a new endpoint and must preserve the original success/error/statusCode handling.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:323': {
    semanticKind: 'HOST_MODEL_LOAD',
    urlContract: 'GET-over-POST /api/v1/clusters/{clusterName}/hosts?fields={runtimeFields}&minimal_response=true&{pagination}&{sort}; RequestInfo.query={hostPredicate}',
    operationalFields: [
      'Hosts rack/name/maintenance/public_host/cpu_count/ph_cpu_count/status/state/heartbeat/ip/os/agent-env',
      'host component state/maintenance/stale-config/service/display/desired-admin-state',
      'stack/repository version identity',
      'logging metadata when supports.logSearch',
      'NameNode ClusterId and HAState when HDFS is loaded',
    ],
    notes: 'The response may also conditionally include disk/load/cpu/memory metric fields, which are excluded; filtering, sorting, pagination, and oversized-GET override behavior must be preserved.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:568': {
    semanticKind: 'COMPONENT_TOPOLOGY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/components/?{runtimeComponentPredicates}&fields={runtimeFields}&minimal_response=true',
    operationalFields: [
      'service/component identity and master/client topology',
      'host/display/public-host/state/maintenance/stale-config/ha-state/desired-admin-state',
      'HDFS ClusterId and HBase IsActiveMaster operational selectors',
    ],
    notes: 'Shares serviceMetricsMapper and subsequent requests include many metrics; this baseline retains only operational semantics such as topology/state/HA/Active-Standby.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:615': {
    semanticKind: 'SERVICE_STATE_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state,ServiceInfo/desired_repository_version_id,components/ServiceComponentInfo/component_name&minimal_response=true',
    operationalFields: ['service state/maintenance/desired repository version', 'component names'],
    notes: 'Loads the service model during cluster initialization.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:626': {
    semanticKind: 'COMPONENT_STATE_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/components/?fields=ServiceComponentInfo/{service_name,category,installed_count,started_count,init_count,install_failed_count,unknown_count,total_count,display_name},host_components/HostRoles/host_name&minimal_response=true',
    operationalFields: ['component category and aggregate state counts', 'host component host names'],
    notes: 'Used to map service-component state and topology.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:639': {
    semanticKind: 'ALERT_DEFINITION_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alert_definitions?fields=AlertDefinition/{component_name,description,enabled,repeat_tolerance,repeat_tolerance_enabled,id,ignore_host,interval,label,name,scope,service_name,source,help_url}',
    operationalFields: ['alert definition configuration and source'],
    notes: 'Initial load of all alert definitions.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:654': {
    semanticKind: 'UNHEALTHY_ALERT_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alerts?fields={Alert operational fields}&Alert/state.in(CRITICAL,WARNING)&Alert/maintenance_state.in(OFF)&from={from}&page_size={pageSize}',
    operationalFields: ['critical/warning non-maintenance alert instances', 'repeat-tolerance remaining', 'timestamps/text/host/service/component'],
    notes: 'Loads only paginated non-healthy instances with maintenance OFF.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:665': {
    semanticKind: 'ALERT_GROUPED_SUMMARY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alerts?format=groupedSummary',
    operationalFields: ['server grouped alert summary'],
    notes: 'Loads server-side grouped statistics for the alert-definition summary mapper.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:676': {
    semanticKind: 'ALERT_GROUP_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alert_groups?fields=AlertGroup/{default,definitions,id,name,targets}',
    operationalFields: ['alert group membership and targets'],
    notes: 'Initial load of alert groups.',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:682': {
    semanticKind: 'ALERT_TARGET_LOAD',
    urlContract: 'GET /api/v1/alert_targets?fields=*',
    operationalFields: ['all alert target/notification fields'],
    notes: 'Root-level alert target resource without a cluster path.',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:2025': {
    semanticKind: 'STACK_VERSION_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/stack_versions?fields={full: *,repository_versions/*,... | update: ClusterStackVersions/*}',
    operationalFields: ['cluster stack versions', 'repository/OS/repository details on full load'],
    notes: '`fullLoad` selects the initial full URL or the lightweight runtime URL.',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:2041': {
    semanticKind: 'REPOSITORY_VERSION_LOAD',
    urlContract: 'GET /api/v1/stacks?fields=versions/repository_versions/RepositoryVersions,versions/repository_versions/operating_systems/*,versions/repository_versions/operating_systems/repositories/*',
    operationalFields: ['stack repository versions', 'OS and repository definitions'],
    notes: 'Full repository-version model load.',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_upgrade_history_controller.js:69': {
    semanticKind: 'UPGRADE_HISTORY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/upgrades?fields=Upgrade',
    operationalFields: ['Upgrade/*'],
    notes: 'The historical list resolves when complete is called; HttpClient errors do not call complete and may leave the promise unresolved.',
  },
  'ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js:131': {
    semanticKind: 'CONFIG_HISTORY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/configurations/service_config_versions?{filterAndSortPredicates}fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true',
    operationalFields: ['service config version history and compatibility metadata'],
    notes: 'Filter and sort conditions are generated dynamically by the table mixin.',
  },
  'ambari-web/classic/app/utils/polling.js:67': {
    semanticKind: 'GENERIC_MUTATION_POLL_HELPER',
    urlContract: 'PUT {App.Poll.url}; body={App.Poll.data}; response is text or JSON with Requests.id',
    operationalFields: ['Requests.id when asynchronous', 'caller-defined task/request polling data'],
    notes: 'No production call to `App.Poll.create()` was found in the classic app tree, only a unit test; empty/non-JSON success is marked successful directly, and polling begins only when a request ID exists.',
  },
  'ambari-web/classic/app/views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_ckecks_host_hearbeat_view.js:52': {
    semanticKind: 'HOST_DELETE_PREFLIGHT_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/hosts/?Hosts/host_name.in({hostName})&fields={host,host-component,stack-version,logging and metric fields}&minimal_response=true&page_size=10&from=0&sortBy=Hosts/host_name.asc',
    operationalFields: ['host and host-component state/maintenance/stale-config/desired-admin-state', 'stack versions and logging', 'NameNode ClusterId/HAState safety selectors'],
    notes: 'Safe deletion flow in the pre-upgrade heartbeat check; the response includes disk/load/cpu/memory metrics, but their display is excluded.',
  },
};

const pageReloadContract = (urlContract, notes) => ({
  semanticKind: 'PAGE_RELOAD',
  networkEffect: 'NAVIGATION_REQUEST',
  urlContract,
  notes,
});

const BROWSER_NETWORK_CONTRACTS = {
  'ambari-web/classic/app/controllers/global/user_settings_controller.js:327': pageReloadContract(
    'reload current Ambari document after show_bg/timezone preference requests settle',
    'Both preference mutations are chained with `.always()`; either success or failure can trigger a reload to apply the timezone.',
  ),
  'ambari-web/classic/app/controllers/main.js:122': pageReloadContract(
    'delayed reload of current Ambari document after route/status change',
    'When the cluster is installed, a route or install-status observer reloads after the `App.pageReloadTime` delay.',
  ),
  'ambari-web/classic/app/controllers/main/host/bulk_operations_controller.js:453': pageReloadContract(
    'reload current Ambari document after bulk host-delete result closes',
    'Both successful and partially failed results reload the entire page after clearing the deleted host selection.',
  ),
  'ambari-web/classic/app/controllers/main/host/bulk_operations_controller.js:792': pageReloadContract(
    'reload current Ambari document after bulk host-component-delete result primary action',
    'The result popup has separate reload calls for Primary and Close.',
  ),
  'ambari-web/classic/app/controllers/main/host/bulk_operations_controller.js:797': pageReloadContract(
    'reload current Ambari document after bulk host-component-delete result closes',
    'The result popup Close branch rebuilds the host/component models on the entire page.',
  ),
  'ambari-web/classic/app/controllers/main/alerts/alert_definitions_actions_controller.js:251': {
    semanticKind: 'PAGE_RELOAD',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'reload current Ambari document after starting repeat-tolerance config save',
    notes: 'The config PUT is not awaited; the page reloads immediately while the save request is in flight, which may interrupt or hide failure results.',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:2329': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes generated HTML configuration diff',
    notes: 'The new window receives only generated in-memory HTML and does not request the backend.',
  },
  'ambari-web/classic/app/controllers/main/views_controller.js:112': {
    semanticKind: 'INTERNAL_AMBARI_ROUTE',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '#/main/view/{viewName}/{shortUrl} or #/main/views/{viewName}/{version}/{instanceName}',
    notes: 'The new browsing context opens an Ember hash route, after which the View details iframe requests its context.',
  },
  'ambari-web/classic/app/controllers/main/service/item.js:2042': {
    semanticKind: 'PAGE_RELOAD',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'reload current Ambari document after service deletion confirmation',
    notes: 'After the user closes the delete-success confirmation, the page reloads and rebuilds cluster/service models through the REST startup chain.',
  },
  'ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js:39': {
    semanticKind: 'AMBARI_REST_DOWNLOAD',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: 'GET one of the five generated client-config contracts (`?format=client_config_tar`)',
    notes: 'See the client-config-downloads catalog for the exact resource-scope URL.',
  },
  'ambari-web/classic/app/utils/configs/database.js:223': {
    semanticKind: 'URL_PARSER_NO_NETWORK',
    networkEffect: 'NO_NETWORK',
    urlContract: 'none; assigns caller URL to a detached anchor to read `.hostname`',
    notes: 'Uses only the browser URL parser to read the hostname.',
  },
  'ambari-web/classic/app/utils/file_utils.js:76': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes caller-provided text/HTML',
    notes: 'Local content preview.',
  },
  'ambari-web/classic/app/utils/file_utils.js:91': {
    semanticKind: 'LOCAL_DOWNLOAD',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'data:attachment/{fileType};charset=utf-8,{encodedData}',
    notes: 'Local data-URL download for the Safari fallback.',
  },
  'ambari-web/classic/app/utils/helper.js:1115': {
    semanticKind: 'ADMIN_VIEW_REDIRECT_HELPER',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'window.location.replace({callerSuppliedLocation})',
    notes: 'Testable helper implementation; actual business call sites are listed separately.',
  },
  'ambari-web/classic/app/views/common/host_progress_popup_body_view.js:1044': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes selected task/component log text',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/views/common/modal_popups/log_tail_popup.js:57': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes already-loaded log tail',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/views/common/modal_popups/logs_popup.js:43': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes task log HTML already in the DOM',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/views/main/admin/stack_upgrade/failed_hosts_modal_view.js:77': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes failed-host JSON',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/views/main/admin/stack_upgrade/upgrade_task_view.js:174': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes already-loaded upgrade task log',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/views/wizard/step3/hostWarningPopupBody_view.js:480': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes bootstrap warning details',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/views/wizard/step9/hostLogPopupBody_view.js:197': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes already-loaded install task logs',
    notes: 'Sends no new backend request.',
  },
  'ambari-web/classic/app/router.js:669': {
    semanticKind: 'ADMIN_VIEW_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{appURLRoot}views/ADMIN_VIEW/{lexicallyLatestServerComponentVersion}/INSTANCE/#/',
    notes: 'Admin View branch for no cluster; location.replace leaves Ember with a full-page navigation.',
  },
  'ambari-web/classic/app/router.js:349': {
    semanticKind: 'JWT_PROVIDER_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{401/403 response jwtProviderUrl}{encoded current window URL}{redirected query flag}',
    notes: 'Authentication-failure response drives a full-page redirect to the external IdP; the redirect counter limits only loops and does not constrain the provider origin.',
  },
  'ambari-web/classic/app/router.js:773': {
    semanticKind: 'PAGE_RELOAD',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'reload current Ambari document after logout when cluster model was loaded',
    notes: 'Transitions to login first, then reloads the page on the next run loop.',
  },
  'ambari-web/classic/app/router.js:806': {
    semanticKind: 'PREFERRED_PATH_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{saved preferredPath beginning with / or #, except a path containing #/login}',
    notes: '`startsWith("/")` also accepts `//host/path`, so the legacy implementation may perform protocol-relative cross-origin navigation; React should treat this as a known security defect.',
  },
  'ambari-web/classic/app/router.js:889': {
    semanticKind: 'WINDOW_LOCATION_SETTER',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'window.location={callerSuppliedUrl}',
    notes: 'Final testable setter for the JWT redirect helper; the actual provider data entry point is listed separately.',
  },
  'ambari-web/classic/app/routes/activate_hawq_standby_routes.js:56': pageReloadContract(
    'reload current Ambari document after closing an unfinished Activate HAWQ Standby wizard',
    'Cluster status persistence ends with `alwaysCallback`; failure also triggers a transition followed by reload.',
  ),
  'ambari-web/classic/app/routes/activate_hawq_standby_routes.js:178': pageReloadContract(
    'reload current Ambari document after completing Activate HAWQ Standby',
    'Cleanup persistence ends with `alwaysCallback`.',
  ),
  'ambari-web/classic/app/routes/add_alert_definition_routes.js:141': pageReloadContract(
    'reload current Ambari document after Add Alert Definition completes',
    'Cleans up wizard state and transitions to Alerts; status-persistence failure also reloads.',
  ),
  'ambari-web/classic/app/routes/add_hawq_standby_routes.js:57': pageReloadContract(
    'reload current Ambari document after closing an unfinished Add HAWQ Standby wizard',
    'Cluster status persistence ends with `alwaysCallback`; failure also reloads.',
  ),
  'ambari-web/classic/app/routes/add_hawq_standby_routes.js:202': pageReloadContract(
    'reload current Ambari document after completing Add HAWQ Standby',
    'Cleanup persistence ends with `alwaysCallback`.',
  ),
  'ambari-web/classic/app/routes/main.js:549': pageReloadContract(
    'reload current Ambari document after Disable Kerberos closes',
    'Disable cleanup and cluster status persistence end with `alwaysCallback`, then return to the Kerberos page and reload.',
  ),
  'ambari-web/classic/app/routes/ra_high_availability_routes.js:179': pageReloadContract(
    'reload current Ambari document after Ranger Admin HA completes',
    'Cleanup persistence ends with `alwaysCallback`.',
  ),
  'ambari-web/classic/app/routes/reassign_master_routes.js:314': pageReloadContract(
    'reload current Ambari document after Reassign Master completes',
    'Cluster status persistence ends with `alwaysCallback`; failure also triggers a transition followed by reload.',
  ),
  'ambari-web/classic/app/routes/remove_hawq_standby_routes.js:63': pageReloadContract(
    'reload current Ambari document after confirmed close of an active Remove HAWQ Standby wizard',
    'Cleans up state after confirmed close; both persistence success and failure reload.',
  ),
  'ambari-web/classic/app/routes/remove_hawq_standby_routes.js:81': pageReloadContract(
    'reload current Ambari document after closing an inactive Remove HAWQ Standby wizard',
    'No confirmation is required during the inactive phase, but reload still occurs after persistence settles.',
  ),
  'ambari-web/classic/app/routes/remove_hawq_standby_routes.js:189': pageReloadContract(
    'reload current Ambari document after completing Remove HAWQ Standby',
    'Cleanup persistence ends with `alwaysCallback`.',
  ),
  'ambari-web/classic/app/routes/rollbackHA_routes.js:154': pageReloadContract(
    'reload current Ambari document after NameNode HA rollback completes',
    'This branch does not wait for cluster status persistence; it transitions and reloads directly on the next run loop.',
  ),
  'ambari-web/classic/app/routes/stack_upgrade_routes.js:83': pageReloadContract(
    'conditionally reload current Ambari document when closing an upgrade wizard',
    'Reloads and resets the wizard owner only when global upgrade state is NOT_REQUIRED or COMPLETED.',
  ),
  'ambari-web/classic/app/controllers/wizard.js:1528': pageReloadContract(
    'reload current Ambari document after generic long-running wizard exit cleanup',
    'Shared `exitWizard()` flows such as HA/Federation transition and reload after status persistence settles.',
  ),
  'ambari-web/classic/app/mixins/common/reload_popup.js:63': pageReloadContract(
    'reload current Ambari document from the stale-page popup link',
    'The reload call is in a runtime-compiled inline onclick and executes when the user clicks.',
  ),
  'ambari-web/classic/app/views/main/admin/stack_upgrade/versions_view.js:233': {
    semanticKind: 'ADMIN_VIEW_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{appURLRoot}views/ADMIN_VIEW/{lexicallyLatestServerComponentVersion}/INSTANCE/#/',
    notes: 'After Manage Versions confirmation, performs a full-page navigation to Admin View.',
  },
  'ambari-web/classic/app/views/main/views/details.js:85': {
    semanticKind: 'VIEW_WEB_CONTEXT',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: '{window.location.protocol}//{window.location.host}{ViewInstanceInfo.context_path}/{viewPath}',
    notes: 'Same-origin View application iframe; not `/api/v1` REST.',
  },
  'ambari-web/classic/app/templates/common/about.hbs:30': {
    semanticKind: 'STATIC_EXTERNAL_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'http://ambari.apache.org/',
    notes: 'Static project link in the About modal.',
  },
  'ambari-web/classic/app/templates/common/about.hbs:32': {
    semanticKind: 'STATIC_EXTERNAL_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'http://www.apache.org/licenses/LICENSE-2.0',
    notes: 'Static license link in the About modal.',
  },
  'ambari-web/classic/app/assets/index.html:48': {
    semanticKind: 'STATIC_EXTERNAL_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'http://www.apache.org/licenses/LICENSE-2.0',
    notes: 'External Apache License link in the legacy application footer.',
  },
  'ambari-web/classic/app/assets/index.html:49': {
    semanticKind: 'STATIC_NOTICE_DOCUMENT',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: 'GET /licenses/NOTICE.txt',
    notes: 'The legacy application footer opens a same-origin third-party NOTICE document.',
  },
  'ambari-web/classic/app/templates/application.hbs:70': {
    semanticKind: 'NEW_UI_NAVIGATION',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'GET /latest/#',
    notes: 'The user menu navigates to the new frontend entry point with a full-page navigation through Switch Experience.',
  },
  'ambari-web/classic/app/templates/common/host_progress_popup.hbs:345': {
    semanticKind: 'EXTERNAL_LOG_SEARCH',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{Log Search UI quick-link URL}{hostLog.linkTail}',
    notes: 'Depends on the Log Search service quick-link descriptor and loaded log context.',
  },
  'ambari-web/classic/app/templates/common/modal_popups/log_tail_popup.hbs:32': {
    semanticKind: 'EXTERNAL_LOG_SEARCH',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{Log Search quick-link template}/#/logs/serviceLogs;{component/host/file query}',
    notes: 'Shown when the Log Search service and quick link are available.',
  },
  'ambari-web/classic/app/templates/main/alerts/definition_details.hbs:196': {
    semanticKind: 'EXTERNAL_ALERT_HELP',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{AlertDefinition.help_url}',
    notes: 'Shown only when the definition has a help URL supplied by backend data.',
  },
  'ambari-web/classic/app/templates/main/host/logs.hbs:46': {
    semanticKind: 'EXTERNAL_LOG_SEARCH',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{Log Search UI quick-link URL}{file.linkTail}',
    notes: 'The host logs page constructs a Log Search deep link for the selected file.',
  },
  'ambari-web/classic/app/templates/main/service/info/summary.hbs:103': {
    semanticKind: 'EXTERNAL_QUICK_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{stack quicklinks descriptor + current configs + selected public host/protocol/port}',
    notes: 'Service Quick Links for multiple masters/groups.',
  },
  'ambari-web/classic/app/templates/main/service/info/summary.hbs:111': {
    semanticKind: 'EXTERNAL_QUICK_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{stack quicklinks descriptor + current configs + selected public host/protocol/port}',
    notes: 'Service Quick Links for a single host or flat layout.',
  },
  'ambari-web/classic/app/templates/main/service/services/hive.hbs:54': {
    semanticKind: 'INTERNAL_AMBARI_ROUTE',
    networkEffect: 'CONDITIONAL',
    urlContract: '{ViewInstance.internalAmbariUrl}',
    notes: 'Legacy Hive-to-View hook; `viewsToShow` is currently empty, so no link is visible by default and routing occurs only with a runtime extension.',
  },
  'ambari-web/classic/app/templates/main/views.hbs:28': {
    semanticKind: 'VIEW_ICON_RESOURCE',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: 'GET {ViewInstanceInfo.icon_path}',
    notes: 'Dynamic image request driven by backend View-instance metadata; not a fixed built image.',
  },
};

function attachRequiredContract(record, contracts, catalogName) {
  const key = `${record.source}:${record.line}`;
  const contract = contracts[key];
  if (!contract) throw new Error(`${catalogName} record ${key} has no audited semantic contract`);
  return { ...record, ...contract };
}

function extractDirectCalls(jsFiles) {
  const calls = [];
  const patterns = [
    { kind: 'HttpClient', regex: /App\.HttpClient\.(get|post|request)\s*\(([^\n]*)/g },
    { kind: 'jQuery AJAX', regex: /(?:\$|jQuery)\.ajax\s*\(([^\n]*)/g },
    { kind: 'XMLHttpRequest', regex: /new\s+XMLHttpRequest\s*\(([^\n]*)/g },
  ];
  for (const file of jsFiles) {
    if (file === ajaxFile || file.endsWith('/utils/http_client.js')) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    for (const { kind, regex } of patterns) {
      let match;
      while ((match = regex.exec(source))) {
        const excerpt = match[0].replace(/\s+/g, ' ').trim();
        const isMixedOperationalCall = file.endsWith('/controllers/global/update_controller.js')
          && excerpt.includes('App.serviceMetricsMapper');
        if (!isMixedOperationalCall && isMetricsRelated(file, excerpt)) continue;
        const record = {
          kind,
          method: kind === 'HttpClient' ? match[1].toUpperCase() : 'DYNAMIC',
          expression: excerpt,
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
          scope: isMixedOperationalCall ? 'MIXED' : 'NON_METRICS',
        };
        if (record.source.endsWith('/controllers/global/update_controller.js') && record.line === 323) {
          record.scope = 'MIXED';
        }
        if (record.source.endsWith('/custom_cluster_ckecks_host_hearbeat_view.js')) {
          record.scope = 'MIXED';
        }
        calls.push(attachRequiredContract(record, DIRECT_HTTP_CONTRACTS, 'direct HTTP'));
      }
    }
  }
  return calls;
}

function extractBrowserNetworkEntrypoints(jsFiles, markupFiles) {
  const entries = [];
  const patterns = [
    { kind: 'window.open', regex: /\bwindow\.open\s*\(([^\n]*)/g },
    { kind: 'location.replace', regex: /\bwindow\.location\.replace\s*\(([^\n]*)/g },
    { kind: 'replaceWindowLocation call', regex: /\bApp\.replaceWindowLocation\s*\(([^\n]*)/g },
    { kind: 'redirectByURL call', regex: /\bthis\.redirectByURL\s*\(([^\n]*)/g },
    { kind: 'window.location assignment', regex: /\bwindow\.location\s*=\s*([^;\n]+)/g },
    { kind: 'window.location.reload', regex: /\bwindow\.location\.reload\s*\(([^\n]*)/g },
    { kind: 'location.reload', regex: /(?<!window\.)\blocation\.reload\s*\(([^\n]*)/g },
    { kind: 'anchor href', regex: /\.href\s*=\s*([^;\n]+)/g },
    { kind: 'iframe src', regex: /\.attr\s*\(\s*['"]src['"]\s*,\s*([^\n]*)/g },
    { kind: 'iframe computed src', regex: /\bsrc\s*:\s*function\s*\([^)]*\)\s*\{/g },
  ];
  for (const file of jsFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    for (const { kind, regex } of patterns) {
      let match;
      while ((match = regex.exec(source))) {
        const record = {
          kind,
          expression: normalizeExpression(match[0]),
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
        };
        entries.push(attachRequiredContract(record, BROWSER_NETWORK_CONTRACTS, 'browser network'));
      }
    }
  }
  const markupPatterns = [
    { kind: 'target=_blank anchor', regex: /<a\b[^>]*\btarget\s*=\s*['"]_blank['"][^>]*>/g },
    { kind: 'new UI anchor', regex: /<a\b[^>]*\bhref\s*=\s*['"]\/latest\/#['"][^>]*>/g },
    { kind: 'dynamic image src', regex: /<img\b[^>]*\{\{bindAttr\s+src=[^}]+\}\}[^>]*>/g },
  ];
  for (const file of markupFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    for (const { kind, regex } of markupPatterns) {
      let match;
      while ((match = regex.exec(source))) {
        const record = {
          kind,
          expression: normalizeExpression(match[0]),
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
        };
        entries.push(attachRequiredContract(record, BROWSER_NETWORK_CONTRACTS, 'browser network'));
      }
    }
  }
  entries.sort((left, right) => left.source.localeCompare(right.source) || left.line - right.line || left.kind.localeCompare(right.kind));
  const consumedContracts = new Set(entries.map((entry) => `${entry.source}:${entry.line}`));
  const unusedContracts = Object.keys(BROWSER_NETWORK_CONTRACTS).filter((key) => !consumedContracts.has(key));
  if (unusedContracts.length) {
    throw new Error(`Unused browser network contracts: ${unusedContracts.join(', ')}`);
  }
  return entries;
}

function extractClientConfigDownloads() {
  const file = path.join(appRoot, 'mixins/main/host/details/support_client_configs_download.js');
  const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
  const suffix = '?format=client_config_tar';
  const contracts = [
    {
      resourceType: 'CLUSTER',
      endpoint: '/clusters/{clusterName}/components' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.CLUSTER:',
    },
    {
      resourceType: 'HOST',
      endpoint: '/clusters/{clusterName}/hosts/{hostName}/host_components' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.HOST:',
    },
    {
      resourceType: 'SERVICE',
      endpoint: '/clusters/{clusterName}/services/{serviceName}/components' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.SERVICE:',
    },
    {
      resourceType: 'SERVICE_COMPONENT',
      endpoint: '/clusters/{clusterName}/services/{serviceName}/components/{componentName}' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.SERVICE_COMPONENT:',
    },
    {
      resourceType: 'HOST_COMPONENT',
      endpoint: '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.HOST_COMPONENT:',
    },
  ];
  if (!source.includes("result += '?format=client_config_tar'")) {
    throw new Error('Could not find client config download suffix');
  }
  return contracts.map(({ sourceMarker, ...contract }) => {
    const markerIndex = source.indexOf(sourceMarker);
    if (markerIndex === -1) {
      throw new Error(`Could not find client config download branch ${contract.resourceType}`);
    }
    return {
      ...contract,
      method: 'GET (browser download)',
      source: relativeToRepo(file),
      line: lineNumberAt(source, markerIndex),
    };
  });
}

function extractRoutes(jsFiles) {
  const routeFiles = jsFiles.filter((file) =>
    file === path.join(appRoot, 'router.js') || file.includes(`${path.sep}routes${path.sep}`),
  );
  const routes = [];
  for (const file of routeFiles) {
    if (isMetricsUiFile(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    const routePattern = /\broute\s*:\s*(['"])([^'"\n]+)\1/g;
    let match;
    while ((match = routePattern.exec(source))) {
      if (isMetricsRelated(file, match[2])) continue;
      routes.push({
        fragment: match[2],
        source: relativeToRepo(file),
        line: lineNumberAt(source, match.index),
      });
    }
  }
  return routes;
}

function extractTemplateActions(templateFiles) {
  const actions = new Map();
  for (const file of templateFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    const actionPattern = /{{action\s+['"]?([^\s'"}]+)/g;
    let match;
    while ((match = actionPattern.exec(source))) {
      if (isMetricsRelated(match[1])) continue;
      const key = match[1];
      if (!actions.has(key)) actions.set(key, []);
      actions.get(key).push(`${relativeToRepo(file)}:${lineNumberAt(source, match.index)}`);
    }
  }
  return [...actions.entries()]
    .map(([name, callers]) => ({ name, callers }))
    .sort((left, right) => left.name.localeCompare(right.name));
}

function extractFeatureIndex() {
  const moduleFiles = fs.readdirSync(baselineRoot, { withFileTypes: true })
    .filter((entry) => entry.isFile() && FEATURE_MODULE_FILE_PATTERN.test(entry.name))
    .map((entry) => path.join(baselineRoot, entry.name))
    .sort();
  const features = [];
  const idPattern = /^\|\s*([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)\s*\|/;
  const extractFirstCell = (row, start) => {
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
  };
  for (const file of moduleFiles) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    let section = '';
    for (let index = 0; index < lines.length; index += 1) {
      const heading = lines[index].match(/^#{2,3}\s+(.+?)\s*$/);
      if (heading) section = heading[1];
      const feature = lines[index].match(idPattern);
      if (!feature) continue;
      features.push({
        id: feature[1],
        summary: extractFirstCell(lines[index], feature[0].length),
        section,
        moduleFile: path.basename(file),
        line: index + 1,
      });
    }
  }
  return features;
}

function renderAjaxCatalog(definitions, excludedDefinitions) {
  const lines = [
    '# Ember Non-Metrics Named AJAX API Catalog',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`; do not edit manually. URLs use `/api/v1` by default; `DYNAMIC` in the Method column means only that the HTTP method is determined by a runtime expression, while dynamic URLs are marked separately as `DYNAMIC_URL`. Caller counts are string-reference counts in the classic frontend `app/` directory.',
    '',
    `- Included definitions: ${definitions.length}`,
    `- Excluded Metrics definitions: ${excludedDefinitions.length}`,
    `- With legacy frontend call evidence: ${definitions.filter((item) => item.callers.length > 0).length}`,
    `- Without legacy frontend call evidence: ${definitions.filter((item) => item.callers.length === 0).length}`,
    '',
    '| Request name | Method | URL (without default prefix) | format input keys | Prefix | Module | Callers | Definition |',
    '| --- | --- | --- | --- | --- | --- | ---: | --- |',
  ];
  for (const definition of definitions) {
    const inputKeys = definition.hasFormat
      ? (definition.inputKeys.length ? definition.inputKeys.map((key) => `\`${escapeMarkdown(key)}\``).join(', ') : 'No static `data.*` references')
      : 'No `format()`';
    const endpoint = `${definition.endpoint}${definition.hasDynamicUrl ? ' [DYNAMIC_URL]' : ''}`;
    lines.push(`| \`${escapeMarkdown(definition.name)}\` | \`${definition.methods.join('/')}\` | \`${escapeMarkdown(endpoint)}\` | ${inputKeys} | \`${escapeMarkdown(definition.apiPrefix)}\` | ${classifyModule(definition)} | ${definition.callers.length} | [source](../../../../${definition.source}#L${definition.line}) |`);
    if (definition.callers.length) {
      lines.push(`|  |  | Call sites: ${definition.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')} |  |  |  |  |  |`);
    }
  }
  lines.push('', '## Excluded Metrics Requests', '', excludedDefinitions.map((item) => `- \`${item.name}\` ([source](../../../../${item.source}#L${item.line}))`).join('\n'), '');
  return lines.join('\n');
}

function renderAjaxCalls(calls, includedRequestNames) {
  const dynamicCalls = calls.filter((call) => call.registrationStatus === 'DYNAMIC');
  const dynamicCandidateNames = new Set(dynamicCalls.flatMap((call) => call.candidateRequestNames));
  const dynamicMissingCandidates = new Set(dynamicCalls.flatMap((call) =>
    call.candidateRequestNames.filter((name) => !includedRequestNames.has(name))));
  const lines = [
    '# Ember Non-Metrics AJAX Call Sites',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. Each row is an `App.ajax.send(...)` call site; dynamic requests are resolved using the manual-audit contracts in `tools/contracts/dynamic-ajax-resolutions.mjs`. Parameter keys list only top-level keys in `data: {...}` on the call object.',
    '',
    `${calls.length} call sites: ${calls.filter((call) => call.registrationStatus === 'REGISTERED').length} match in-scope registered requests, ${calls.filter((call) => call.registrationStatus === 'UNREGISTERED').length} use static request names absent from the registry, and ${calls.filter((call) => call.registrationStatus === 'DYNAMIC').length} use dynamic expressions.`,
    '',
    '| Request name/expression | Registration status | Call parameters | Callbacks | Location |',
    '| --- | --- | --- | --- | --- |',
  ];
  for (const call of calls) {
    const request = call.requestName
      ? `\`${escapeMarkdown(call.requestName)}\``
      : `DYNAMIC: \`${escapeMarkdown(call.requestExpression)}\``;
    const data = call.dataKeys.length
      ? call.dataKeys.map((key) => `\`${escapeMarkdown(key)}\``).join(', ')
      : call.dataExpression
        ? `Expression: \`${escapeMarkdown(call.dataExpression)}\``
        : 'No inline `data` found';
    const callbacks = call.callbacks.length
      ? call.callbacks.map((key) => `\`${key}\``).join(', ')
      : 'Default handling';
    lines.push(`| ${request} | \`${call.registrationStatus}\` | ${data} | ${callbacks} | [source](../../../../${call.source}#L${call.line}) |`);
  }
  lines.push(
    '',
    '## Dynamic Call Resolution',
    '',
    `Dynamic resolution: ${dynamicCalls.length}/${dynamicCalls.length} call sites have contracts, ${dynamicCandidateNames.size} unique candidate request names, and ${dynamicMissingCandidates.size} candidates absent from the in-scope registry.`,
    '',
    '| Location/expression | Resolution status | Dispatch kind | Candidate request names | Dispatch condition | Runtime boundary |',
    '| --- | --- | --- | --- | --- | --- |',
  );
  for (const call of dynamicCalls) {
    const candidates = call.candidateRequestNames
      .map((name) => `\`${escapeMarkdown(name)}\``)
      .join('<br>');
    lines.push(`| [source](../../../../${call.source}#L${call.line})<br>\`${escapeMarkdown(call.requestExpression)}\` | \`${call.resolutionStatus}\` | \`${call.dispatchKind}\` | ${candidates} | ${escapeMarkdown(call.dispatchCondition)} | ${escapeMarkdown(call.boundaryNotes)}<br>Evidence: ${call.evidence.map((item) => `\`${escapeMarkdown(item)}\``).join('<br>')} |`);
  }
  lines.push(
    '',
    '`RESOLVED_CLOSED` means the current production branches are enumerable; `RESOLVED_OPEN_BOUNDARY` means the current candidates are enumerable but a generic wrapper, model metadata, mixin property, or FIFO queue may be extended by future code.',
    '',
    '`UNREGISTERED` is legacy behavior where the classic code calls a name with no matching definition in `app/utils/ajax/ajax.js`. It is not a valid backend contract; migration must first confirm the intended endpoint before fixing or preserving compatibility behavior.',
    '',
  );
  return lines.join('\n');
}

function renderDirectCalls(calls) {
  const lines = [
    '# Ember Non-Metrics Direct HTTP Calls',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. These calls bypass the `App.ajax` named registry; each has manually reconstructed business semantics, URL contract, and operational fields retained by this baseline. The generator fails if it encounters a new call without a semantic contract.',
    '',
    `${calls.length} static call sites.`,
    '',
    '| Semantics | Method / Scope | URL Contract | Retained Fields | Behavior/Boundary | Location |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  for (const call of calls) {
    const fields = call.operationalFields.map((field) => `\`${escapeMarkdown(field)}\``).join('<br>');
    lines.push(`| \`${call.semanticKind}\`<br>${call.kind} | \`${call.method}\` / \`${call.scope}\` | \`${escapeMarkdown(call.urlContract)}\` | ${fields} | ${escapeMarkdown(call.notes)}<br>Call: \`${escapeMarkdown(call.expression)}\` | [source](../../../../${call.source}#L${call.line}) |`);
  }
  lines.push('', '`MIXED` calls return both metric fields and non-Metrics operational fields. This document includes only the topology/state/maintenance/stale config/HA/Active-Standby/safety-decision fields listed in the table; metric values remain excluded. `ORIGINAL_REQUEST_REPLAY` replays the original request after saving the KDC credential and is not a fixed endpoint.', '');
  return lines.join('\n');
}

function renderBrowserNetworkEntrypoints(entries) {
  const lines = [
    '# Ember Non-Metrics Browser Network and Download Entry Points',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. Each command entry point, full-page reload/redirect, View iframe/icon, and non-Metrics declarative navigation link is manually classified. The generator fails if it encounters a new entry point without a semantic contract. Ordinary startup scripts, stylesheets, favicons, and fixed UI images are build/static resources outside this catalog.',
    '',
    `${entries.length} candidate call sites.`,
    '',
    '| Semantics | Network Effect | URL / Content Contract | Expression | Boundary | Location |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  for (const entry of entries) {
    lines.push(`| \`${entry.semanticKind}\`<br>${entry.kind} | \`${entry.networkEffect}\` | \`${escapeMarkdown(entry.urlContract)}\` | \`${escapeMarkdown(entry.expression)}\` | ${escapeMarkdown(entry.notes)} | [source](../../../../${entry.source}#L${entry.line}) |`);
  }
  const effects = [...new Set(entries.map((entry) => entry.networkEffect))].sort();
  lines.push('', `Network effect categories: ${effects.map((effect) => `\`${effect}\``).join(', ')}. \`LOCAL_ONLY\` and \`NO_NETWORK\` are not backend APIs, but remain user behavior that React must preserve or explicitly change.`, '');
  return lines.join('\n');
}

function renderClientConfigDownloads(contracts) {
  const lines = [
    '# Ember Client Config Browser Download Contracts',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. Five resource scopes share one mixin and request the archive directly through `window.open()`; they are not included in `App.ajax` or direct HTTP call counts.',
    '',
    `${contracts.length} resource scopes.`,
    '',
    '| Resource Type | Method | URL (path after `/api/v1`) | Branch Location |',
    '| --- | --- | --- | --- |',
  ];
  for (const contract of contracts) {
    lines.push(`| \`${contract.resourceType}\` | ${contract.method} | \`${escapeMarkdown(contract.endpoint)}\` | [source](../../../../${contract.source}#L${contract.line}) |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderRealtimeLocations(locations) {
  if (!locations.length) return 'None';
  return locations
    .map(({ source, line }) => `[source](../../../../${source}#L${line})`)
    .join('<br>');
}

function renderRealtimeChannels({ transports, subscriptions, lifecycle }) {
  const countSites = (property) => subscriptions.reduce(
    (count, subscription) => count + subscription[property].length,
    0,
  );
  const staticDestinations = subscriptions.filter(
    (subscription) => !subscription.destinationTemplate.includes('{'),
  ).length;
  const dynamicDestinations = subscriptions.length - staticDestinations;
  const lines = [
    '# Ember Non-Metrics Realtime Channel Contract',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs` from the audited static contract. This document freezes classic UI STOMP/WebSocket/SockJS behavior and excludes Metrics time-series channels.',
    '',
    '## Verifiable Summary',
    '',
    `- transports: ${transports.length}`,
    `- destinations: ${subscriptions.length} (${staticDestinations} static + ${dynamicDestinations} dynamic)`,
    `- subscribe sites: ${countSites('subscribeSites')}`,
    `- addHandler sites: ${countSites('addHandlerSites')}`,
    `- removeHandler sites: ${countSites('removeHandlerSites')}`,
    `- business unsubscribe sites: ${countSites('unsubscribeSites')}`,
    `- lifecycle contracts: ${lifecycle.length}`,
    '',
    '## Transport',
    '',
    '| ID / Type | URL | Protocol and Heartbeat | Fallback / Reconnection | Failure Boundaries | Source / Test |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  for (const transport of transports) {
    const protocolDetail = [
      `STOMP ${transport.protocols.stompVersions.join('/')}`,
      `heartbeat ${transport.heartbeat.clientOutgoingMs}/${transport.heartbeat.clientIncomingMs} ms`,
      transport.protocols.sockJsTransports?.length
        ? `SockJS ${transport.protocols.sockJsTransports.join(', ')}`
        : `schemes ${transport.protocols.socketSchemes.join('/')}`,
    ].join('<br>');
    const recovery = [
      `fallback: ${transport.fallback.trigger}`,
      `reconnect: ${transport.reconnect.policy}`,
    ].map(escapeMarkdown).join('<br>');
    const locations = [
      `Implementation: ${renderRealtimeLocations(transport.sourceLocations)}`,
      `Tests: ${renderRealtimeLocations(transport.testLocations)}`,
    ].join('<br>');
    lines.push(`| \`${transport.id}\`<br>\`${transport.kind}\` | \`${escapeMarkdown(transport.urlTemplate)}\` | ${protocolDetail} | ${recovery} | ${transport.failureBoundaries.map(escapeMarkdown).join('<br>')} | ${locations} |`);
  }

  lines.push(
    '',
    '## Destination Contract',
    '',
    '| ID / Destination | Event | Ember Consumed Fields | Handler Chain | Lifecycle | REST Reconciliation | Failure Boundaries | Source |',
    '| --- | --- | --- | --- | --- | --- | --- | --- |',
  );
  for (const subscription of subscriptions) {
    const consumedFields = subscription.consumedFields
      .map((field) => `\`${escapeMarkdown(field)}\``)
      .join('<br>');
    const handlers = subscription.handlerChain
      .map((handler) => `\`${escapeMarkdown(handler)}\``)
      .join('<br>');
    const sources = [
      ['subscribe', subscription.subscribeSites],
      ['addHandler', subscription.addHandlerSites],
      ['removeHandler', subscription.removeHandlerSites],
      ['business unsubscribe', subscription.unsubscribeSites],
      ['event', subscription.eventSourceSites],
      ['test', subscription.testLocations],
    ]
      .filter(([, locations]) => locations.length)
      .map(([name, locations]) => `${name}: ${renderRealtimeLocations(locations)}`)
      .join('<br>');
    lines.push(`| \`${subscription.id}\`<br>\`${escapeMarkdown(subscription.destinationTemplate)}\` | \`${escapeMarkdown(subscription.eventClass)}\` | ${consumedFields} | ${handlers} | ${escapeMarkdown(subscription.lifecycle)} | ${escapeMarkdown(subscription.restReconciliation)} | ${subscription.failureBoundaries.map(escapeMarkdown).join('<br>')} | ${sources} |`);
  }

  lines.push('', '## Payload Schema', '');
  for (const subscription of subscriptions) {
    lines.push(
      '<details>',
      `<summary><code>${subscription.id}</code> <code>${escapeMarkdown(subscription.destinationTemplate)}</code></summary>`,
      '',
      '```json',
      JSON.stringify(subscription.payloadSchema, null, 2),
      '```',
      '',
      '</details>',
      '',
    );
  }

  lines.push(
    '## Lifecycle',
    '',
    '| ID / Name | Behavior | Failure Boundaries | Source / Test |',
    '| --- | --- | --- | --- |',
  );
  for (const entry of lifecycle) {
    const locations = [
      `Implementation: ${renderRealtimeLocations(entry.sourceLocations)}`,
      `Tests: ${renderRealtimeLocations(entry.testLocations)}`,
    ].join('<br>');
    lines.push(`| \`${entry.id}\`<br>\`${entry.name}\` | ${escapeMarkdown(entry.behavior)} | ${entry.failureBoundaries.map(escapeMarkdown).join('<br>')} | ${locations} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderNamedUsageCatalog(title, description, items) {
  const lines = [
    `# ${title}`,
    '',
    `> ${description}`,
    '',
    `${items.length} distinct names.`,
    '',
    '| Name | Usage Form | Call Sites | Locations |',
    '| --- | --- | ---: | --- |',
  ];
  for (const item of items) {
    const kinds = item.kinds?.length ? item.kinds.map((kind) => `\`${kind}\``).join(', ') : 'Static reference';
    lines.push(`| \`${escapeMarkdown(item.name)}\` | ${kinds} | ${item.callers.length} | ${item.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderFeatureIndex(features) {
  const lines = [
    '# Ember Stable Feature Index',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs` from module documents `01` through `13`; do not edit manually. `00` methodology, `14` React matrix, and `15` audit report are not recognized as feature sources.',
    '',
    `${features.length} stable feature IDs.`,
    '',
    '| Feature ID | Module | Section | Summary | Definition |',
    '| --- | --- | --- | --- | --- |',
  ];
  for (const feature of features) {
    lines.push(`| \`${feature.id}\` | \`${feature.moduleFile}\` | ${escapeMarkdown(feature.section)} | ${escapeMarkdown(feature.summary)} | [source](../${feature.moduleFile}#L${feature.line}) |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderRoutes(routes) {
  const lines = [
    '# Ember Non-Metrics Route Fragments',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. The legacy frontend uses nested `Em.Route`; the table contains route fragments from source, not concatenated final URLs.',
    '',
    `${routes.length} non-Metrics route fragments.`,
    '',
    '| Route fragment | Location |',
    '| --- | --- |',
  ];
  for (const route of routes) {
    lines.push(`| \`${escapeMarkdown(route.fragment)}\` | [source](../../../../${route.source}#L${route.line}) |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderActions(actions) {
  const lines = [
    '# Ember Non-Metrics Template Actions',
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. This table identifies user-triggerable behavior; dynamic actions and behavior triggered only by JavaScript still require review of the module documents and source.',
    '',
    `${actions.length} distinct action names.`,
    '',
    '| Action | Occurrences | Template Locations |',
    '| --- | ---: | --- |',
  ];
  for (const action of actions) {
    lines.push(`| \`${escapeMarkdown(action.name)}\` | ${action.callers.length} | ${action.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderModuleCatalog(moduleName, definitions) {
  const contentContract = definitions.map((definition) => ({
    name: definition.name,
    methods: definition.methods,
    endpoint: definition.endpoint,
    inputKeys: definition.inputKeys,
    callers: definition.callers,
  }));
  const lines = [
    `# ${moduleName}: Ember Non-Metrics Named AJAX Candidate Index`,
    '',
    '> Generated by `tools/extract-ember-baseline.mjs`. This page uses broad regular-expression heuristics over request names and caller paths: shared requests may be mixed across modules or duplicated, and module requests may be omitted or assigned elsewhere. It is not a complete module API catalog; authoritative review must combine `../ajax-endpoints.json`, `../ajax-calls.json`, `../direct-http-calls.json`, `../browser-network-entrypoints.json`, and `../realtime-channels.json`.',
    '',
    `${definitions.length} named request candidates.`,
    `Candidate content SHA-256: \`${sha256Json(contentContract)}\`.`,
    '',
    '| Request Name | Method | URL (without default prefix) | format Input Keys | Call Sites |',
    '| --- | --- | --- | --- | --- |',
  ];
  for (const definition of definitions) {
    const callers = definition.callers.length
      ? definition.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')
      : 'No legacy frontend string-call evidence found';
    const inputKeys = definition.hasFormat
      ? (definition.inputKeys.length ? definition.inputKeys.map((key) => `\`${escapeMarkdown(key)}\``).join(', ') : 'No static `data.*` references')
      : 'No `format()`';
    const endpoint = `${definition.endpoint}${definition.hasDynamicUrl ? ' [DYNAMIC_URL]' : ''}`;
    lines.push(`| \`${escapeMarkdown(definition.name)}\` | \`${definition.methods.join('/')}\` | \`${escapeMarkdown(endpoint)}\` | ${inputKeys} | ${callers} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function writeGenerated(fileName, content) {
  fs.mkdirSync(outputRoot, { recursive: true });
  fs.writeFileSync(path.join(outputRoot, fileName), content);
}

function writeJson(fileName, value) {
  fs.mkdirSync(outputRoot, { recursive: true });
  fs.writeFileSync(path.join(outputRoot, fileName), `${JSON.stringify(value, null, 2)}\n`);
}

const jsFiles = listFiles(appRoot, (file) => file.endsWith('.js'));
const templateFiles = listFiles(path.join(appRoot, 'templates'), (file) => file.endsWith('.hbs'));
const browserMarkupFiles = [...templateFiles, path.join(appRoot, 'assets/index.html')];
const allDefinitions = extractAjaxDefinitions();
findDefinitionCallers(allDefinitions, jsFiles);
const excludedDefinitions = allDefinitions.filter(isMetricsDefinition);
const definitions = allDefinitions.filter((definition) => !excludedDefinitions.includes(definition));
const includedRequestNames = new Set(definitions.map((definition) => definition.name));
const ajaxCalls = attachDynamicAjaxResolutions(extractAjaxCalls(
  jsFiles,
  includedRequestNames,
  new Set(allDefinitions.map((definition) => definition.name)),
));
const directCalls = extractDirectCalls(jsFiles);
const browserNetworkEntrypoints = extractBrowserNetworkEntrypoints(jsFiles, browserMarkupFiles);
const clientConfigDownloads = extractClientConfigDownloads();
const permissionUses = extractPermissionUses(jsFiles, templateFiles);
const featureFlagUses = extractFeatureFlagUses(jsFiles, templateFiles);
const routes = extractRoutes(jsFiles);
const actions = extractTemplateActions(templateFiles);
const features = extractFeatureIndex();

writeGenerated('ajax-endpoints.md', renderAjaxCatalog(definitions, excludedDefinitions));
writeGenerated('ajax-calls.md', renderAjaxCalls(ajaxCalls, includedRequestNames));
writeGenerated('direct-http-calls.md', renderDirectCalls(directCalls));
writeGenerated('browser-network-entrypoints.md', renderBrowserNetworkEntrypoints(browserNetworkEntrypoints));
writeGenerated('client-config-downloads.md', renderClientConfigDownloads(clientConfigDownloads));
writeGenerated('permissions.md', renderNamedUsageCatalog(
  'Ember Non-Metrics Permission Usage',
  'Generated by `tools/extract-ember-baseline.mjs`. Identifies static-string `isAuthorized`/`havePermissions` forms in JavaScript and Handlebars; helper dynamic arguments and server privileges still require manual audit.',
  permissionUses,
));
writeGenerated('feature-flags.md', renderNamedUsageCatalog(
  'Ember Non-Metrics Feature Flag Usage',
  'Generated by `tools/extract-ember-baseline.mjs`. Identifies `App.supports.flag` and `App.get(\'supports.flag\')` in JavaScript and Handlebars; see the authored permissions document for default values and server override semantics.',
  featureFlagUses,
));
writeGenerated('routes.md', renderRoutes(routes));
writeGenerated('template-actions.md', renderActions(actions));
writeGenerated('feature-index.md', renderFeatureIndex(features));
writeGenerated('realtime-channels.md', renderRealtimeChannels(realtimeChannels));
writeJson('ajax-endpoints.json', definitions.map(({ objectSource, ...definition }) => ({
  ...definition,
  modules: classifyModules(definition),
})));
writeJson('excluded-metrics-ajax-endpoints.json', excludedDefinitions.map(({ objectSource, ...definition }) => definition));
writeJson('ajax-calls.json', ajaxCalls);
writeJson('direct-http-calls.json', directCalls);
writeJson('browser-network-entrypoints.json', browserNetworkEntrypoints);
writeJson('client-config-downloads.json', clientConfigDownloads);
writeJson('permissions.json', permissionUses);
writeJson('feature-flags.json', featureFlagUses);
writeJson('routes.json', routes);
writeJson('template-actions.json', actions);
writeJson('feature-index.json', features);
writeJson('realtime-channels.json', realtimeChannels);

const definitionsByModule = new Map();
for (const definition of definitions) {
  for (const moduleName of classifyModules(definition)) {
    if (!definitionsByModule.has(moduleName)) definitionsByModule.set(moduleName, []);
    definitionsByModule.get(moduleName).push(definition);
  }
}
const moduleFileNames = new Map([
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
const moduleDir = path.join(outputRoot, 'api-by-module');
fs.mkdirSync(moduleDir, { recursive: true });
for (const [moduleName, moduleDefinitions] of definitionsByModule) {
  fs.writeFileSync(
    path.join(moduleDir, moduleFileNames.get(moduleName)),
    renderModuleCatalog(moduleName, moduleDefinitions),
  );
}

console.log(JSON.stringify({
  ajaxDefinitions: definitions.length,
  excludedMetricsDefinitions: excludedDefinitions.length,
  ajaxCalls: ajaxCalls.length,
  dynamicAjaxCalls: ajaxCalls.filter((call) => !call.requestName).length,
  unregisteredAjaxCalls: ajaxCalls.filter((call) => call.registrationStatus === 'UNREGISTERED').length,
  directHttpCalls: directCalls.length,
  browserNetworkEntrypoints: browserNetworkEntrypoints.length,
  clientConfigDownloadScopes: clientConfigDownloads.length,
  permissions: permissionUses.length,
  featureFlags: featureFlagUses.length,
  routeFragments: routes.length,
  templateActions: actions.length,
  featureIds: features.length,
  realtimeTransports: realtimeChannels.transports.length,
  realtimeDestinations: realtimeChannels.subscriptions.length,
  realtimeLifecycleContracts: realtimeChannels.lifecycle.length,
}, null, 2));
