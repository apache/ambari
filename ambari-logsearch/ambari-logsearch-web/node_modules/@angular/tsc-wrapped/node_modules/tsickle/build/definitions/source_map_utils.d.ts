import { SourceMapConsumer, SourceMapGenerator } from 'source-map';
export declare function containsInlineSourceMap(source: string): boolean;
export declare function getInlineSourceMapCount(source: string): number;
export declare function extractInlineSourceMap(source: string): string;
export declare function removeInlineSourceMap(source: string): string;
/**
 * Sets the source map inline in the file.  If there's an existing inline source
 * map, it clobbers it.
 */
export declare function setInlineSourceMap(source: string, sourceMap: string): string;
export declare function sourceMapConsumerToGenerator(sourceMapConsumer: SourceMapConsumer): SourceMapGenerator;
/**
 * Tsc identifies source files by their relative path to the output file.  Since
 * there's no easy way to identify these relative paths when tsickle generates its
 * own source maps, we patch them with the file name from the tsc source maps
 * before composing them.
 */
export declare function sourceMapGeneratorToConsumer(sourceMapGenerator: SourceMapGenerator, fileName?: string, sourceName?: string): SourceMapConsumer;
export declare function sourceMapTextToConsumer(sourceMapText: string): SourceMapConsumer;
export declare function sourceMapTextToGenerator(sourceMapText: string): SourceMapGenerator;
