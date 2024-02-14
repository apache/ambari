"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const fs = require("fs");
const glob = require("glob");
const path = require("path");
const require_project_module_1 = require("../utilities/require-project-module");
const SilentError = require('silent-error');
const Task = require('../ember-cli/lib/models/task');
class LintTaskOptions {
    constructor() {
        this.format = 'prose';
        this.silent = false;
        this.typeCheck = false;
    }
}
exports.LintTaskOptions = LintTaskOptions;
exports.default = Task.extend({
    run: function (options) {
        options = Object.assign({}, new LintTaskOptions(), options);
        const ui = this.ui;
        const projectRoot = this.project.root;
        const lintConfigs = options.configs || [];
        if (lintConfigs.length === 0) {
            if (!options.silent) {
                ui.writeLine(chalk.yellow('No lint configuration(s) found.'));
            }
            return Promise.resolve(0);
        }
        const tslint = require_project_module_1.requireProjectModule(projectRoot, 'tslint');
        const Linter = tslint.Linter;
        const Configuration = tslint.Configuration;
        const result = lintConfigs
            .map((config) => {
            let program;
            if (config.project) {
                program = Linter.createProgram(config.project);
            }
            else if (options.typeCheck) {
                if (!options.silent) {
                    ui.writeLine(chalk.yellow('A "project" must be specified to enable type checking.'));
                }
            }
            const files = getFilesToLint(program, config, Linter);
            const lintOptions = {
                fix: options.fix,
                formatter: options.format
            };
            const lintProgram = options.typeCheck ? program : undefined;
            const linter = new Linter(lintOptions, lintProgram);
            let lastDirectory;
            let configLoad;
            files.forEach((file) => {
                const fileContents = getFileContents(file, program);
                if (!fileContents) {
                    return;
                }
                // Only check for a new tslint config if path changes
                const currentDirectory = path.dirname(file);
                if (currentDirectory !== lastDirectory) {
                    configLoad = Configuration.findConfiguration(config.tslintConfig, file);
                    lastDirectory = currentDirectory;
                }
                linter.lint(file, fileContents, configLoad.results);
            });
            return linter.getResult();
        })
            .reduce((total, current) => {
            const failures = current.failures
                .filter((cf) => !total.failures.some((ef) => ef.equals(cf)));
            total.failures = total.failures.concat(...failures);
            if (current.fixes) {
                total.fixes = (total.fixes || []).concat(...current.fixes);
            }
            return total;
        }, {
            failures: [],
            fixes: undefined
        });
        if (!options.silent) {
            const Formatter = tslint.findFormatter(options.format);
            if (!Formatter) {
                throw new SilentError(chalk.red(`Invalid lint format "${options.format}".`));
            }
            const formatter = new Formatter();
            const output = formatter.format(result.failures, result.fixes);
            if (output) {
                ui.writeLine(output);
            }
        }
        // print formatter output directly for non human-readable formats
        if (['prose', 'verbose', 'stylish'].indexOf(options.format) == -1) {
            return (result.failures.length == 0 || options.force)
                ? Promise.resolve(0) : Promise.resolve(2);
        }
        if (result.failures.length > 0) {
            if (!options.silent) {
                ui.writeLine(chalk.red('Lint errors found in the listed files.'));
            }
            return options.force ? Promise.resolve(0) : Promise.resolve(2);
        }
        if (!options.silent) {
            ui.writeLine(chalk.green('All files pass linting.'));
        }
        return Promise.resolve(0);
    }
});
function getFilesToLint(program, lintConfig, Linter) {
    let files = [];
    if (lintConfig.files) {
        files = Array.isArray(lintConfig.files) ? lintConfig.files : [lintConfig.files];
    }
    else if (program) {
        files = Linter.getFileNames(program);
    }
    let globOptions = {};
    if (lintConfig.exclude) {
        const excludePatterns = Array.isArray(lintConfig.exclude)
            ? lintConfig.exclude
            : [lintConfig.exclude];
        globOptions = { ignore: excludePatterns, nodir: true };
    }
    files = files
        .map((file) => glob.sync(file, globOptions))
        .reduce((a, b) => a.concat(b), []);
    return files;
}
function getFileContents(file, program) {
    let contents;
    if (program) {
        const sourceFile = program.getSourceFile(file);
        if (sourceFile) {
            contents = sourceFile.getFullText();
        }
    }
    else {
        // NOTE: The tslint CLI checks for and excludes MPEG transport streams; this does not.
        try {
            contents = fs.readFileSync(file, 'utf8');
        }
        catch (e) {
            throw new SilentError(`Could not read file "${file}".`);
        }
    }
    return contents;
}
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/lint.js.map