export interface IConfigurationFile {
    extends?: string | string[];
    jsRules?: any;
    linterOptions?: {
        typeCheck?: boolean;
    };
    rulesDirectory?: string | string[];
    rules?: any;
}
export interface IConfigurationLoadResult {
    path?: string;
    results?: IConfigurationFile;
}
export declare const CONFIG_FILENAME = "tslint.json";
export declare const DEFAULT_CONFIG: {
    "extends": string;
};
/**
 * Searches for a TSLint configuration and returns the data from the config.
 * @param configFile A path to a config file, this can be null if the location of a config is not known
 * @param inputFileLocation A path to the current file being linted. This is the starting location
 * of the search for a configuration.
 * @returns Load status for a TSLint configuration object
 */
export declare function findConfiguration(configFile: string | null, inputFilePath: string): IConfigurationLoadResult;
/**
 * Searches for a TSLint configuration and returns the path to it.
 * Could return undefined if not configuration is found.
 * @param suppliedConfigFilePath A path to an known config file supplied by a user. Pass null here if
 * the location of the config file is not known and you want to search for one.
 * @param inputFilePath A path to the current file being linted. This is the starting location
 * of the search for a configuration.
 * @returns An absolute path to a tslint.json file
 * or undefined if neither can be found.
 */
export declare function findConfigurationPath(suppliedConfigFilePath: string | null, inputFilePath: string): string | undefined;
/**
 * Used Node semantics to load a configuration file given configFilePath.
 * For example:
 * '/path/to/config' will be treated as an absolute path
 * './path/to/config' will be treated as a relative path
 * 'path/to/config' will attempt to load a to/config file inside a node module named path
 * @returns a configuration object for TSLint loaded from the file at configFilePath
 */
export declare function loadConfigurationFromPath(configFilePath?: string): IConfigurationFile;
export declare function extendConfigurationFile(targetConfig: IConfigurationFile, nextConfigSource: IConfigurationFile): IConfigurationFile;
export declare function getRelativePath(directory?: string | null, relativeTo?: string): string | undefined;
/**
 * @param directories A path(s) to a directory of custom rules
 * @param relativeTo A path that directories provided are relative to.
 * For example, if the directories come from a tslint.json file, this path
 * should be the path to the tslint.json file.
 * @return An array of absolute paths to directories potentially containing rules
 */
export declare function getRulesDirectories(directories?: string | string[], relativeTo?: string): string[];
