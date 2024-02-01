/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 Test modules and groups.

 Use command "mvn -fae -Dexec.executable='echo' -Dexec.args='${project.artifactId}' exec:exec -q" to revisit

 Ambari WebUI ->  ambari-web, ambari-admin
 Ambari Agent ->  ambari-agent, ambari-utility
 Ambari Server -> ambari-server, ambari-views
 Ambari Metrics ->  (cd) -> ambari-metrics  (only build, no checks)
 ServiceAdvisor -> ambari-serviceadvisor
 Ambari LogSearch ->  (cd) -> ambari-logsearch
 Ambari Infra -> (cd) -> ambari-infra


 Ambari Server JTest Profiles: FastTests, SlowTests, NonFastTests, AlertTests, AmbariUpgradeTests, BlueprintTests, KerberosTests, MetricsTests,  StackUpgradeTests

 Jenkins Plugins required:
 - JIRA Steps, https://plugins.jenkins.io/jira-steps/
 - Jenkins Multibranch pipeline
 **/
pipeline {
    agent {
        node {
            label 'Hadoop'
        }
    }

    tools {
        maven 'maven_3_latest'
        jdk 'jdk_1.8_latest'
    }

    environment {
     TEMP='/tmp'
     TEMPDIR='/tmp'
     TMP='/tmp'
     TMPDIR='/tmp'
    }

    stages {
        stage('Pre-Build Deps') {
            parallel{
                stage('JIRA Integration') {
                    steps {
                       publishPRLink env.CHANGE_ID, env.CHANGE_URL, env.CHANGE_TITLE
                    }
                }
                stage('Ambari Metrics Build (deps)') {
                    steps {
                        script{
                           def dirExists = fileExists 'ambari-metrics'
                           if (dirExists) {
                               echo "Ambari-Metrics is here!"
                                dir('ambari-metrics') {
                                    sh 'mvn -T 3C install -DskipSurefireTests -DskipPythonTests -Dmaven.test.failure.ignore -DskipTests -Dfindbugs.skip -Drat.skip -Dmaven.artifact.threads=10 -X'
                                }
                            } else {
                                echo "Ignoring ambari-metrics, as no such directory found"
                            }
                        }
                    }
                }
                stage('Ambari Service Advisor') {
                    steps {
                        sh 'mvn -T 3C -am install -pl ambari-serviceadvisor -DskipSurefireTests -DskipPythonTests -Dmaven.test.failure.ignore -DskipTests -Dfindbugs.skip -Drat.skip -Dmaven.artifact.threads=10'
                    }
                }
            }
        }

        // Rat & Checkstyle is not really like multi-threading due to high possiblity of false possitives.
        stage('RAT') {
            steps{
                sh 'mvn org.apache.rat:apache-rat-plugin:check -Dmaven.artifact.threads=10'
            }
        }

        stage('Parallel Unit Tests') {
            parallel {
                 stage('Ambari WebUI Tests') {
                    steps {
                        withEnv(['CHROME_BIN=/usr/bin/chromium-browser']) {
                            sh 'mvn -T 2C -am test -pl ambari-web,ambari-admin -Dmaven.artifact.threads=10 -Drat.skip'
                        }
                    }
                }
                stage('Ambari Agent Tests') {
                    steps {
                        sh 'pip3 install distro'
                        sh 'mvn -Dmaven.test.failure.ignore=true -am test -pl ambari-agent -Dmaven.artifact.threads=10 -Drat.skip'
                    }
                }

                stage('Ambari Server PyTests') {
                    steps {
                        sh 'mvn -am test -pl ambari-server -DskipSurefireTests -Dmaven.test.failure.ignore -Dmaven.artifact.threads=10 -Drat.skip -Dcheckstyle.skip'
                    }
                }
            }
        }
        stage('Ambari Server JTests') {
            steps {
                sh 'mvn -am test -pl ambari-server -DskipPythonTests -Dmaven.test.failure.ignore -Dmaven.artifact.threads=10 -Drat.skip'
            }
        }
    }
}

@NonCPS
def publishPRLink(change_id, change_url, change_title) {
    println "Change id: $change_id, title: $change_title, url: $change_url"
    def jira_num = change_title =~  /(?i)^\[*(?<JIRA>AMBARI\-\d+)/

    jira_num = jira_num.find() ? jira_num.group("JIRA") : ""
    println "JIRA ID: $jira_num"

    if (jira_num) {
        // tip: https://developer.atlassian.com/server/jira/platform/jira-rest-api-for-remote-issue-links/
        // globalId is required for JIRA to distinguish 2 same links and do not stack new objects
        def remoteLink = [
            globalId    : "system=$change_url",
            object      : [url  :  change_url,
                            title: "GitHub PR#$change_id",
                            icon : [
                                    url16x16: "https://github.com/favicon.ico",
                                    title   : "GitHub PR#$change_id"
                            ]
            ]
        ]
        jiraNewIssueRemoteLink idOrKey: jira_num, remoteLink: remoteLink, site: "JIRA"
    } else {
        println """
        ======failed to add link to jira
        Ooops, no jira id found :(
        ============================="""
        pullRequest.comment("Pull request summary '${change_title}' doesn't contain JIRA Number in format 'AMBARI-xxxxx' at the beginning, please, create the JIRA issue and update pull request summary")
    }
}