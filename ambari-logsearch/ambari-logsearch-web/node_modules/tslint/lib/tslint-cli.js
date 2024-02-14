/**
 * @license
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var optimist = require("optimist");
var runner_1 = require("./runner");
var processed = optimist
    .usage("Usage: $0 [options] file ...")
    .check(function (argv) {
    // at least one of file, help, version, project or unqualified argument must be present
    if (!(argv.h || argv.i || argv.test || argv.v || argv.project || argv._.length > 0)) {
        // throw a string, otherwise a call stack is printed for this message
        // tslint:disable-next-line:no-string-throw
        throw "Missing files";
    }
    if (argv.f) {
        // throw a string, otherwise a call stack is printed for this message
        // tslint:disable-next-line:no-string-throw
        throw "-f option is no longer available. Supply files directly to the tslint command instead.";
    }
})
    .options({
    "c": {
        alias: "config",
        describe: "configuration file",
        type: "string",
    },
    "e": {
        alias: "exclude",
        describe: "exclude globs from path expansion",
        type: "string",
    },
    "fix": {
        describe: "fixes linting errors for select rules (this may overwrite linted files)",
        type: "boolean",
    },
    "force": {
        describe: "return status code 0 even if there are lint errors",
        type: "boolean",
    },
    "h": {
        alias: "help",
        describe: "display detailed help",
        type: "boolean",
    },
    "i": {
        alias: "init",
        describe: "generate a tslint.json config file in the current working directory",
        type: "boolean",
    },
    "o": {
        alias: "out",
        describe: "output file",
        type: "string",
    },
    "project": {
        describe: "tsconfig.json file",
        type: "string",
    },
    "r": {
        alias: "rules-dir",
        describe: "rules directory",
        type: "string",
    },
    "s": {
        alias: "formatters-dir",
        describe: "formatters directory",
        type: "string",
    },
    "t": {
        alias: "format",
        default: "prose",
        describe: "output format (prose, json, stylish, verbose, pmd, msbuild, checkstyle, vso, fileslist, codeFrame)",
        type: "string",
    },
    "test": {
        describe: "test that tslint produces the correct output for the specified directory",
        type: "string",
    },
    "type-check": {
        describe: "enable type checking when linting a project",
        type: "boolean",
    },
    "v": {
        alias: "version",
        describe: "current version",
        type: "boolean",
    },
});
var argv = processed.argv;
var outputStream;
if (argv.o != null) {
    outputStream = fs.createWriteStream(argv.o, {
        flags: "w+",
        mode: 420,
    });
}
else {
    outputStream = process.stdout;
}
if (argv.help) {
    outputStream.write(processed.help());
    var outputString = "\ntslint accepts the following commandline options:\n\n    -c, --config:\n        The location of the configuration file that tslint will use to\n        determine which rules are activated and what options to provide\n        to the rules. If no option is specified, the config file named\n        tslint.json is used, so long as it exists in the path.\n        The format of the file is { rules: { /* rules list */ } },\n        where /* rules list */ is a key: value comma-seperated list of\n        rulename: rule-options pairs. Rule-options can be either a\n        boolean true/false value denoting whether the rule is used or not,\n        or a list [boolean, ...] where the boolean provides the same role\n        as in the non-list case, and the rest of the list are options passed\n        to the rule that will determine what it checks for (such as number\n        of characters for the max-line-length rule, or what functions to ban\n        for the ban rule).\n\n    -e, --exclude:\n        A filename or glob which indicates files to exclude from linting.\n        This option can be supplied multiple times if you need multiple\n        globs to indicate which files to exclude.\n\n    --fix:\n        Fixes linting errors for select rules. This may overwrite linted files.\n\n    --force:\n        Return status code 0 even if there are any lint errors.\n        Useful while running as npm script.\n\n    -i, --init:\n        Generates a tslint.json config file in the current working directory.\n\n    -o, --out:\n        A filename to output the results to. By default, tslint outputs to\n        stdout, which is usually the console where you're running it from.\n\n    -r, --rules-dir:\n        An additional rules directory, for user-created rules.\n        tslint will always check its default rules directory, in\n        node_modules/tslint/lib/rules, before checking the user-provided\n        rules directory, so rules in the user-provided rules directory\n        with the same name as the base rules will not be loaded.\n\n    -s, --formatters-dir:\n        An additional formatters directory, for user-created formatters.\n        Formatters are files that will format the tslint output, before\n        writing it to stdout or the file passed in --out. The default\n        directory, node_modules/tslint/build/formatters, will always be\n        checked first, so user-created formatters with the same names\n        as the base formatters will not be loaded.\n\n    -t, --format:\n        The formatter to use to format the results of the linter before\n        outputting it to stdout or the file passed in --out. The core\n        formatters are prose (human readable), json (machine readable)\n        and verbose. prose is the default if this option is not used.\n        Other built-in options include pmd, msbuild, checkstyle, and vso.\n        Additional formatters can be added and used if the --formatters-dir\n        option is set.\n\n    --test:\n        Runs tslint on matched directories and checks if tslint outputs\n        match the expected output in .lint files. Automatically loads the\n        tslint.json files in the directories as the configuration file for\n        the tests. See the full tslint documentation for more details on how\n        this can be used to test custom rules.\n\n    --project:\n        The location of a tsconfig.json file that will be used to determine which\n        files will be linted.\n\n    --type-check\n        Enables the type checker when running linting rules. --project must be\n        specified in order to enable type checking.\n\n    -v, --version:\n        The current version of tslint.\n\n    -h, --help:\n        Prints this help message.\n";
    outputStream.write(outputString);
    process.exit(0);
}
var options = {
    config: argv.c,
    exclude: argv.exclude,
    files: argv._,
    fix: argv.fix,
    force: argv.force,
    format: argv.t,
    formattersDirectory: argv.s,
    init: argv.init,
    out: argv.out,
    project: argv.project,
    rulesDirectory: argv.r,
    test: argv.test,
    typeCheck: argv["type-check"],
    version: argv.v,
};
new runner_1.Runner(options, outputStream)
    .run(function (status) { return process.exit(status); });
