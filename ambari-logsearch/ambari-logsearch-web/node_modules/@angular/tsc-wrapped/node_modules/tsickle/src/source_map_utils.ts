import {SourceMapConsumer, SourceMapGenerator} from 'source-map';

/**
 * Return a new RegExp object every time we want one because the
 * RegExp object has internal state that we don't want to persist
 * between different logical uses.
 */
function getInlineSourceMapRegex(): RegExp {
  return new RegExp('^//# sourceMappingURL=data:application/json;base64,(.*)$', 'mg');
}

export function containsInlineSourceMap(source: string): boolean {
  return getInlineSourceMapCount(source) > 0;
}

export function getInlineSourceMapCount(source: string): number {
  const match = source.match(getInlineSourceMapRegex());
  return match ? match.length : 0;
}

export function extractInlineSourceMap(source: string): string {
  const inlineSourceMapRegex = getInlineSourceMapRegex();
  let previousResult: RegExpExecArray|null = null;
  let result: RegExpExecArray|null = null;
  // We want to extract the last source map in the source file
  // since that's probably the most recent one added.  We keep
  // matching against the source until we don't get a result,
  // then we use the previous result.
  do {
    previousResult = result;
    result = inlineSourceMapRegex.exec(source);
  } while (result !== null);
  const base64EncodedMap = previousResult![1];
  return Buffer.from(base64EncodedMap, 'base64').toString('utf8');
}

export function removeInlineSourceMap(source: string): string {
  return source.replace(getInlineSourceMapRegex(), '');
}

/**
 * Sets the source map inline in the file.  If there's an existing inline source
 * map, it clobbers it.
 */
export function setInlineSourceMap(source: string, sourceMap: string): string {
  const encodedSourceMap = Buffer.from(sourceMap, 'utf8').toString('base64');
  if (containsInlineSourceMap(source)) {
    return source.replace(
        getInlineSourceMapRegex(),
        `//# sourceMappingURL=data:application/json;base64,${encodedSourceMap}`);
  } else {
    return `${source}\n//# sourceMappingURL=data:application/json;base64,${encodedSourceMap}`;
  }
}

export function sourceMapConsumerToGenerator(sourceMapConsumer: SourceMapConsumer):
    SourceMapGenerator {
  return SourceMapGenerator.fromSourceMap(sourceMapConsumer);
}

/**
 * Tsc identifies source files by their relative path to the output file.  Since
 * there's no easy way to identify these relative paths when tsickle generates its
 * own source maps, we patch them with the file name from the tsc source maps
 * before composing them.
 */
export function sourceMapGeneratorToConsumer(
    sourceMapGenerator: SourceMapGenerator, fileName?: string,
    sourceName?: string): SourceMapConsumer {
  const rawSourceMap = sourceMapGenerator.toJSON();
  if (sourceName) {
    rawSourceMap.sources = [sourceName];
  }
  if (fileName) {
    rawSourceMap.file = fileName;
  }
  return new SourceMapConsumer(rawSourceMap);
}

export function sourceMapTextToConsumer(sourceMapText: string): SourceMapConsumer {
  const sourceMapJson: any = sourceMapText;
  return new SourceMapConsumer(sourceMapJson);
}

export function sourceMapTextToGenerator(sourceMapText: string): SourceMapGenerator {
  const sourceMapJson: any = sourceMapText;
  return SourceMapGenerator.fromSourceMap(sourceMapTextToConsumer(sourceMapJson));
}
