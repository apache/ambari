/// <reference types="node" />
export interface IRunnerOptions {
    /**
     * Path to a configuration file.
     */
    config?: string;
    /**
     * Exclude globs from path expansion.
     */
    exclude?: string | string[];
    /**
     * File paths to lint.
     */
    files?: string[];
    /**
     * Whether to return status code 0 even if there are lint errors.
     */
    force?: boolean;
    /**
     * Whether to fixes linting errors for select rules. This may overwrite linted files.
     */
    fix?: boolean;
    /**
     * Output format.
     */
    format?: string;
    /**
     * Formatters directory path.
     */
    formattersDirectory?: string;
    /**
     * Whether to generate a tslint.json config file in the current working directory.
     */
    init?: boolean;
    /**
     * Output file path.
     */
    out?: string;
    /**
     * tsconfig.json file.
     */
    project?: string;
    /**
     * Rules directory paths.
     */
    rulesDirectory?: string | string[];
    /**
     * That TSLint produces the correct output for the specified directory.
     */
    test?: string;
    /**
     * Whether to enable type checking when linting a project.
     */
    typeCheck?: boolean;
    /**
     * Whether to show the current TSLint version.
     */
    version?: boolean;
}
export declare class Runner {
    private options;
    private outputStream;
    private static trimSingleQuotes(str);
    constructor(options: IRunnerOptions, outputStream: NodeJS.WritableStream);
    run(onComplete: (status: number) => void): void;
    private processFiles(onComplete, files, program?);
}
