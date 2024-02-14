import * as ts from "typescript";
import { findConfiguration, findConfigurationPath, getRulesDirectories, IConfigurationFile, loadConfigurationFromPath } from "./configuration";
import { ILinterOptions, LintResult } from "./index";
/**
 * Linter that can lint multiple files in consecutive runs.
 */
declare class Linter {
    private options;
    private program;
    static VERSION: string;
    static findConfiguration: typeof findConfiguration;
    static findConfigurationPath: typeof findConfigurationPath;
    static getRulesDirectories: typeof getRulesDirectories;
    static loadConfigurationFromPath: typeof loadConfigurationFromPath;
    private failures;
    private fixes;
    private languageService;
    /**
     * Creates a TypeScript program object from a tsconfig.json file path and optional project directory.
     */
    static createProgram(configFile: string, projectDirectory?: string): ts.Program;
    /**
     * Returns a list of source file names from a TypeScript program. This includes all referenced
     * files and excludes declaration (".d.ts") files.
     */
    static getFileNames(program: ts.Program): string[];
    constructor(options: ILinterOptions, program?: ts.Program);
    lint(fileName: string, source: string, configuration?: IConfigurationFile): void;
    getResult(): LintResult;
    private applyRule(rule, sourceFile);
    private getEnabledRules(sourceFile, configuration, isJs);
    private getSourceFile(fileName, source);
    private containsRule(rules, rule);
}
declare namespace Linter {
}
export = Linter;
