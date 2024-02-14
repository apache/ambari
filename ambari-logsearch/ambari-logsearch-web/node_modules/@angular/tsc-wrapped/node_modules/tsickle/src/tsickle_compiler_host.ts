import * as path from 'path';
import {SourceMapGenerator} from 'source-map';
import * as ts from 'typescript';

import {convertDecorators} from './decorator-annotator';
import {processES5} from './es5processor';
import {ModulesManifest} from './modules_manifest';
import * as sourceMapUtils from './source_map_utils';
import * as tsickle from './tsickle';
import {isDtsFileName} from './tsickle';

/**
 * Tsickle can perform 2 different precompilation transforms - decorator downleveling
 * and closurization.  Both require tsc to have already type checked their
 * input, so they can't both be run in one call to tsc. If you only want one of
 * the transforms, you can specify it in the constructor, if you want both, you'll
 * have to specify it by calling reconfigureForRun() with the appropriate Pass.
 */
export enum Pass {
  NONE,
  DECORATOR_DOWNLEVEL,
  CLOSURIZE
}

export interface Options {
  googmodule?: boolean;
  es5Mode?: boolean;
  prelude?: string;
  /**
   * If true, convert every type to the Closure {?} type, which means
   * "don't check types".
   */
  untyped?: boolean;
  /**
   * If provided a function that logs an internal warning.
   * These warnings are not actionable by an end user and should be hidden
   * by default.
   */
  logWarning?: (warning: ts.Diagnostic) => void;
  /** If provided, a set of paths whose types should always generate as {?}. */
  typeBlackListPaths?: Set<string>;
  /**
   * Convert shorthand "/index" imports to full path (include the "/index").
   * Annotation will be slower because every import must be resolved.
   */
  convertIndexImportShorthand?: boolean;
}

/**
 *  Provides hooks to customize TsickleCompilerHost's behavior for different
 *  compilation environments.
 */
export interface TsickleHost {
  /**
   * If true, tsickle and decorator downlevel processing will be skipped for
   * that file.
   */
  shouldSkipTsickleProcessing(fileName: string): boolean;
  /**
   * Takes a context (the current file) and the path of the file to import
   *  and generates a googmodule module name
   */
  pathToModuleName(context: string, importPath: string): string;
  /**
   * Tsickle treats warnings as errors, if true, ignore warnings.  This might be
   * useful for e.g. third party code.
   */
  shouldIgnoreWarningsForPath(filePath: string): boolean;
  /**
   * If we do googmodule processing, we polyfill module.id, since that's
   * part of ES6 modules.  This function determines what the module.id will be
   * for each file.
   */
  fileNameToModuleId(fileName: string): string;
}

const ANNOTATION_SUPPORT = `
interface DecoratorInvocation {
  type: Function;
  args?: any[];
}
`;


/**
 * TsickleCompilerHost does tsickle processing of input files, including
 * closure type annotation processing, decorator downleveling and
 * require -> googmodule rewriting.
 */
export class TsickleCompilerHost implements ts.CompilerHost {
  // The manifest of JS modules output by the compiler.
  public modulesManifest: ModulesManifest = new ModulesManifest();

  /** Error messages produced by tsickle, if any. */
  public diagnostics: ts.Diagnostic[] = [];

  /** externs.js files produced by tsickle, if any. */
  public externs: {[fileName: string]: string} = {};

  private sourceFileToPreexistingSourceMap = new Map<ts.SourceFile, SourceMapGenerator>();
  private preexistingSourceMaps = new Map<string, SourceMapGenerator>();
  private decoratorDownlevelSourceMaps = new Map<string, SourceMapGenerator>();
  private tsickleSourceMaps = new Map<string, SourceMapGenerator>();

  private runConfiguration: {oldProgram: ts.Program, pass: Pass}|undefined;

  constructor(
      private delegate: ts.CompilerHost, private tscOptions: ts.CompilerOptions,
      private options: Options, private environment: TsickleHost) {
    // ts.CompilerHost includes a bunch of optional methods.  If they're
    // present on the delegate host, we want to delegate them.
    if (this.delegate.getCancellationToken) {
      this.getCancellationToken = this.delegate.getCancellationToken!.bind(this.delegate);
    }
    if (this.delegate.getDefaultLibLocation) {
      this.getDefaultLibLocation = this.delegate.getDefaultLibLocation!.bind(this.delegate);
    }
    if (this.delegate.resolveModuleNames) {
      this.resolveModuleNames = this.delegate.resolveModuleNames!.bind(this.delegate);
    }
    if (this.delegate.resolveTypeReferenceDirectives) {
      this.resolveTypeReferenceDirectives =
          this.delegate.resolveTypeReferenceDirectives!.bind(this.delegate);
    }
    if (this.delegate.getEnvironmentVariable) {
      this.getEnvironmentVariable = this.delegate.getEnvironmentVariable!.bind(this.delegate);
    }
    if (this.delegate.trace) {
      this.trace = this.delegate.trace!.bind(this.delegate);
    }
    if (this.delegate.directoryExists) {
      this.directoryExists = this.delegate.directoryExists!.bind(this.delegate);
    }
    if (this.delegate.realpath) {
      this.delegate.realpath = this.delegate.realpath!.bind(this.delegate);
    }
  }

  /**
   * Tsickle can perform 2 kinds of precompilation source transforms - decorator
   * downleveling and closurization.  They can't be run in the same run of the
   * typescript compiler, because they both depend on type information that comes
   * from running the compiler.  We need to use the same compiler host to run both
   * so we have all the source map data when finally write out.  Thus if we want
   * to run both transforms, we call reconfigureForRun() between the calls to
   * ts.createProgram().
   */
  public reconfigureForRun(oldProgram: ts.Program, pass: Pass) {
    this.runConfiguration = {oldProgram, pass};
  }

  getSourceFile(
      fileName: string, languageVersion: ts.ScriptTarget,
      onError?: (message: string) => void): ts.SourceFile {
    if (this.runConfiguration === undefined || this.runConfiguration.pass === Pass.NONE) {
      const sourceFile = this.delegate.getSourceFile(fileName, languageVersion, onError);
      return this.stripAndStoreExistingSourceMap(sourceFile);
    }

    const sourceFile = this.runConfiguration.oldProgram.getSourceFile(fileName);
    switch (this.runConfiguration.pass) {
      case Pass.DECORATOR_DOWNLEVEL:
        return this.downlevelDecorators(
            sourceFile, this.runConfiguration.oldProgram, fileName, languageVersion);
      case Pass.CLOSURIZE:
        return this.closurize(
            sourceFile, this.runConfiguration.oldProgram, fileName, languageVersion);
      default:
        throw new Error('tried to use TsickleCompilerHost with unknown pass enum');
    }
  }

  writeFile(
      fileName: string, content: string, writeByteOrderMark: boolean,
      onError?: (message: string) => void, sourceFiles?: ts.SourceFile[]): void {
    if (path.extname(fileName) !== '.map') {
      if (!isDtsFileName(fileName) && this.tscOptions.inlineSourceMap) {
        content = this.combineInlineSourceMaps(fileName, content);
      }
      if (this.options.googmodule && !isDtsFileName(fileName)) {
        content = this.convertCommonJsToGoogModule(fileName, content);
      }
    } else {
      content = this.combineSourceMaps(fileName, content);
    }

    this.delegate.writeFile(fileName, content, writeByteOrderMark, onError, sourceFiles);
  }

  getSourceMapKeyForPathAndName(outputFilePath: string, sourceFileName: string): string {
    const fileDir = path.dirname(outputFilePath);

    return this.getCanonicalFileName(path.resolve(fileDir, sourceFileName));
  }

  getSourceMapKeyForSourceFile(sourceFile: ts.SourceFile): string {
    return this.getCanonicalFileName(path.resolve(sourceFile.path));
  }

  stripAndStoreExistingSourceMap(sourceFile: ts.SourceFile): ts.SourceFile {
    // Because tsc doesn't have strict null checks, it can pass us an
    // undefined sourceFile, but we can't acknowledge that it does, because
    // we have to comply with their interface, which doesn't allow
    // undefined as far as we're concerned
    if (sourceFile && sourceMapUtils.containsInlineSourceMap(sourceFile.text)) {
      const sourceMapJson = sourceMapUtils.extractInlineSourceMap(sourceFile.text);
      const sourceMap = sourceMapUtils.sourceMapTextToGenerator(sourceMapJson);

      const stripedSourceText = sourceMapUtils.removeInlineSourceMap(sourceFile.text);
      const stripedSourceFile =
          ts.createSourceFile(sourceFile.fileName, stripedSourceText, sourceFile.languageVersion);
      this.sourceFileToPreexistingSourceMap.set(stripedSourceFile, sourceMap);
      return stripedSourceFile;
    }

    return sourceFile;
  }

  combineSourceMaps(filePath: string, tscSourceMapText: string): string {
    // We stripe inline source maps off source files before they've been parsed
    // which is before they have path properties, so we need to construct the
    // map of sourceMapKey to preexistingSourceMap after the whole program has been
    // loaded.
    if (this.sourceFileToPreexistingSourceMap.size > 0 && this.preexistingSourceMaps.size === 0) {
      this.sourceFileToPreexistingSourceMap.forEach((sourceMap, sourceFile) => {
        const sourceMapKey = this.getSourceMapKeyForSourceFile(sourceFile);
        this.preexistingSourceMaps.set(sourceMapKey, sourceMap);
      });
    }

    const tscSourceMapConsumer = sourceMapUtils.sourceMapTextToConsumer(tscSourceMapText);
    const tscSourceMapGenerator = sourceMapUtils.sourceMapConsumerToGenerator(tscSourceMapConsumer);

    if (this.tsickleSourceMaps.size > 0) {
      // TODO(lucassloan): remove when the .d.ts has the correct types
      for (const sourceFileName of (tscSourceMapConsumer as any).sources) {
        const sourceMapKey = this.getSourceMapKeyForPathAndName(filePath, sourceFileName);
        const tsickleSourceMapGenerator = this.tsickleSourceMaps.get(sourceMapKey)!;
        const tsickleSourceMapConsumer = sourceMapUtils.sourceMapGeneratorToConsumer(
            tsickleSourceMapGenerator, sourceFileName, sourceFileName);
        tscSourceMapGenerator.applySourceMap(tsickleSourceMapConsumer);
      }
    }
    if (this.decoratorDownlevelSourceMaps.size > 0) {
      // TODO(lucassloan): remove when the .d.ts has the correct types
      for (const sourceFileName of (tscSourceMapConsumer as any).sources) {
        const sourceMapKey = this.getSourceMapKeyForPathAndName(filePath, sourceFileName);
        const decoratorDownlevelSourceMapGenerator =
            this.decoratorDownlevelSourceMaps.get(sourceMapKey)!;
        const decoratorDownlevelSourceMapConsumer = sourceMapUtils.sourceMapGeneratorToConsumer(
            decoratorDownlevelSourceMapGenerator, sourceFileName, sourceFileName);
        tscSourceMapGenerator.applySourceMap(decoratorDownlevelSourceMapConsumer);
      }
    }
    if (this.preexistingSourceMaps.size > 0) {
      // TODO(lucassloan): remove when the .d.ts has the correct types
      for (const sourceFileName of (tscSourceMapConsumer as any).sources) {
        const sourceMapKey = this.getSourceMapKeyForPathAndName(filePath, sourceFileName);
        const preexistingSourceMapGenerator = this.preexistingSourceMaps.get(sourceMapKey);
        if (preexistingSourceMapGenerator) {
          const preexistingSourceMapConsumer = sourceMapUtils.sourceMapGeneratorToConsumer(
              preexistingSourceMapGenerator, sourceFileName);
          tscSourceMapGenerator.applySourceMap(preexistingSourceMapConsumer);
        }
      }
    }


    return tscSourceMapGenerator.toString();
  }

  combineInlineSourceMaps(filePath: string, compiledJsWithInlineSourceMap: string): string {
    const sourceMapJson = sourceMapUtils.extractInlineSourceMap(compiledJsWithInlineSourceMap);
    const composedSourceMap = this.combineSourceMaps(filePath, sourceMapJson);
    return sourceMapUtils.setInlineSourceMap(compiledJsWithInlineSourceMap, composedSourceMap);
  }

  convertCommonJsToGoogModule(fileName: string, content: string): string {
    const moduleId = this.environment.fileNameToModuleId(fileName);

    let {output, referencedModules} = processES5(
        fileName, moduleId, content, this.environment.pathToModuleName.bind(this.environment),
        this.options.es5Mode, this.options.prelude);

    const moduleName = this.environment.pathToModuleName('', fileName);
    this.modulesManifest.addModule(fileName, moduleName);
    for (let referenced of referencedModules) {
      this.modulesManifest.addReferencedModule(fileName, referenced);
    }

    return output;
  }

  private downlevelDecorators(
      sourceFile: ts.SourceFile, program: ts.Program, fileName: string,
      languageVersion: ts.ScriptTarget): ts.SourceFile {
    this.decoratorDownlevelSourceMaps.set(
        this.getSourceMapKeyForSourceFile(sourceFile), new SourceMapGenerator());
    if (this.environment.shouldSkipTsickleProcessing(fileName)) return sourceFile;
    let fileContent = sourceFile.text;
    const converted = convertDecorators(program.getTypeChecker(), sourceFile);
    if (converted.diagnostics) {
      this.diagnostics.push(...converted.diagnostics);
    }
    if (converted.output === fileContent) {
      // No changes; reuse the existing parse.
      return sourceFile;
    }
    fileContent = converted.output + ANNOTATION_SUPPORT;
    this.decoratorDownlevelSourceMaps.set(
        this.getSourceMapKeyForSourceFile(sourceFile), converted.sourceMap);
    return ts.createSourceFile(fileName, fileContent, languageVersion, true);
  }

  private closurize(
      sourceFile: ts.SourceFile, program: ts.Program, fileName: string,
      languageVersion: ts.ScriptTarget): ts.SourceFile {
    this.tsickleSourceMaps.set(
        this.getSourceMapKeyForSourceFile(sourceFile), new SourceMapGenerator());
    let isDefinitions = isDtsFileName(fileName);
    // Don't tsickle-process any d.ts that isn't a compilation target;
    // this means we don't process e.g. lib.d.ts.
    if (isDefinitions && this.environment.shouldSkipTsickleProcessing(fileName)) return sourceFile;

    let {output, externs, diagnostics, sourceMap} = tsickle.annotate(
        program, sourceFile, this.environment.pathToModuleName.bind(this.environment), this.options,
        this.delegate, this.tscOptions);
    if (externs) {
      this.externs[fileName] = externs;
    }
    if (this.environment.shouldIgnoreWarningsForPath(sourceFile.path)) {
      // All diagnostics (including warnings) are treated as errors.
      // If we've decided to ignore them, just discard them.
      // Warnings include stuff like "don't use @type in your jsdoc"; tsickle
      // warns and then fixes up the code to be Closure-compatible anyway.
      diagnostics = diagnostics.filter(d => d.category === ts.DiagnosticCategory.Error);
    }
    this.diagnostics = diagnostics;
    this.tsickleSourceMaps.set(this.getSourceMapKeyForSourceFile(sourceFile), sourceMap);
    return ts.createSourceFile(fileName, output, languageVersion, true);
  }

  /** Concatenate all generated externs definitions together into a string. */
  getGeneratedExterns(): string {
    let allExterns = tsickle.EXTERNS_HEADER;
    for (let fileName of Object.keys(this.externs)) {
      allExterns += `// externs from ${fileName}:\n`;
      allExterns += this.externs[fileName];
    }
    return allExterns;
  }

  // Delegate everything else to the original compiler host.
  fileExists(fileName: string): boolean {
    return this.delegate.fileExists(fileName);
  }

  getCurrentDirectory(): string {
    return this.delegate.getCurrentDirectory();
  };

  useCaseSensitiveFileNames(): boolean {
    return this.delegate.useCaseSensitiveFileNames();
  }

  getNewLine(): string {
    return this.delegate.getNewLine();
  }

  getDirectories(path: string) {
    return this.delegate.getDirectories(path);
  }

  readFile(fileName: string): string {
    return this.delegate.readFile(fileName);
  }

  getDefaultLibFileName(options: ts.CompilerOptions): string {
    return this.delegate.getDefaultLibFileName(options);
  }

  getCanonicalFileName(fileName: string): string {
    return this.delegate.getCanonicalFileName(fileName);
  }

  // Optional delegated methods, see constructor
  public getCancellationToken: (() => ts.CancellationToken)|undefined;
  public getDefaultLibLocation: (() => string)|undefined;
  public resolveModuleNames:
      ((moduleNames: string[], containingFile: string) => ts.ResolvedModule[])|undefined;
  public resolveTypeReferenceDirectives:
      ((typeReferenceDirectiveNames: string[],
        containingFile: string) => ts.ResolvedTypeReferenceDirective[])|undefined;
  public getEnvironmentVariable: ((name: string) => string)|undefined;
  public trace: ((s: string) => void)|undefined;
  public directoryExists: ((directoryName: string) => boolean)|undefined;
  public realpath: ((path: string) => string)|undefined;
}
