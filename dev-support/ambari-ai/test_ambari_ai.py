# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import ambari_ai


class FakeHttpClient:

  def __init__(self, responses=None):
    self.calls = []
    self.responses = list(responses or [])

  def request(self, method, path, payload=None, query=None):
    self.calls.append((method, path, payload, query))
    return self.responses.pop(0) if self.responses else {}


class JiraClientTest(unittest.TestCase):

  def test_create_issue_uses_minimal_ambari_fields(self):
    http = FakeHttpClient([{"key": "AMBARI-26474"}])
    client = ambari_ai.JiraClient("https://jira.example", http_client=http)

    result = client.create_issue("Bug", "Fix test", "Failure details")

    self.assertEqual("AMBARI-26474", result["key"])
    self.assertEqual((
        "POST",
        "/rest/api/2/issue",
        {
            "fields": {
                "project": {"key": "AMBARI"},
                "issuetype": {"name": "Bug"},
                "summary": "Fix test",
                "description": "Failure details",
            }
        },
        None,
    ), http.calls[0])

  def test_update_issue_description_uses_normalized_issue_key(self):
    http = FakeHttpClient()
    client = ambari_ai.JiraClient("https://jira.example", http_client=http)

    client.update_issue_description("ambari-26474", "h2. Failure details")

    self.assertEqual((
        "PUT",
        "/rest/api/2/issue/AMBARI-26474",
        {"fields": {"description": "h2. Failure details"}},
        None,
    ), http.calls[0])


class GitHubClientTest(unittest.TestCase):

  def test_create_pull_request_payload(self):
    http = FakeHttpClient([{"number": 3995}])
    client = ambari_ai.GitHubClient("token", http_client=http)

    client.create_pull_request(
        "AMBARI-26474: Fix test", "body", "JiaLiangC:AMBARI-26474", "trunk")

    self.assertEqual((
        "POST",
        "/repos/apache/ambari/pulls",
        {
            "title": "AMBARI-26474: Fix test",
            "body": "body",
            "head": "JiaLiangC:AMBARI-26474",
            "base": "trunk",
            "draft": False,
        },
        None,
    ), http.calls[0])

  def test_ci_state_combines_legacy_statuses_and_check_runs(self):
    http = FakeHttpClient([
        {"statuses": [{"state": "success", "context": "Jenkins"}]},
        {"check_runs": [{"status": "completed", "conclusion": "success", "name": "RAT"}]},
    ])
    client = ambari_ai.GitHubClient("token", http_client=http)

    self.assertEqual(
        ("success", "All reported CI checks passed"), client.commit_ci_state("abc123"))

  def test_ci_state_reports_failures(self):
    http = FakeHttpClient([
        {"statuses": [{"state": "error", "context": "Jenkins"}]},
        {"check_runs": []},
    ])
    client = ambari_ai.GitHubClient("token", http_client=http)

    self.assertEqual(("failure", "Failed CI: Jenkins"), client.commit_ci_state("abc123"))

  def test_ci_state_waits_until_a_check_is_reported(self):
    http = FakeHttpClient([{"statuses": []}, {"check_runs": []}])
    client = ambari_ai.GitHubClient("token", http_client=http)

    self.assertEqual(
        ("pending", "No CI status has been reported"), client.commit_ci_state("abc123"))


class FormattingTest(unittest.TestCase):

  def test_markdown_to_jira_converts_headings_lists_links_and_code(self):
    markdown = """## Problem

* Existing bullet
- Another bullet
1. First step
2. Second step with `RequestSchedule.id`

[Baseline](https://example.invalid/baseline)

```shell
npm test
```"""

    self.assertEqual("""h2. Problem

* Existing bullet
* Another bullet
# First step
# Second step with {{RequestSchedule.id}}

[Baseline|https://example.invalid/baseline]

{code:shell}
npm test
{code}""", ambari_ai.markdown_to_jira(markdown))

  def test_markdown_to_jira_does_not_interpret_hash_headings_as_lists(self):
    result = ambari_ai.markdown_to_jira("## Scope\n\n### Detail")

    self.assertEqual("h2. Scope\n\nh3. Detail", result)

  def test_pull_request_title_matches_ambari_3995(self):
    self.assertEqual(
        "AMBARI-26474: Fix ambari server py test",
        ambari_ai.pull_request_title("AMBARI-26474", "Fix ambari server py test"),
    )

  def test_pull_request_title_removes_an_existing_issue_prefix(self):
    self.assertEqual(
        "AMBARI-26474: Fix test",
        ambari_ai.pull_request_title("AMBARI-26474", "[AMBARI-26474]: Fix test"),
    )

  def test_pull_request_body_matches_repository_template(self):
    body = ambari_ai.build_pull_request_body(
        "AMBARI-26474",
        "Fix TestAmbariServer initialization.",
        "The affected test passed.",
        "mvn test -Dpython.test.mask=TestAmbariServer.py",
        ["Before: failed", "After: passed"],
    )

    self.assertTrue(
        body.startswith(
            "Issue: [AMBARI-26474]"
            "(https://issues.apache.org/jira/browse/AMBARI-26474)\n\n"
        )
    )
    self.assertIn("## What changes were proposed in this pull request?", body)
    self.assertIn("Fix TestAmbariServer initialization.", body)
    self.assertIn("## How was this patch tested?", body)
    self.assertIn("```shell\nmvn test -Dpython.test.mask=TestAmbariServer.py\n```", body)
    self.assertIn("Before: failed\nAfter: passed", body)
    self.assertIn("Ambari Contributing Guide", body)
    self.assertNotIn("Please explain how this patch was tested", body)


class MergeTest(unittest.TestCase):

  @mock.patch("ambari_ai.time.sleep")
  @mock.patch("ambari_ai.time.monotonic", side_effect=[0, 1, 2])
  def test_wait_for_ci_returns_after_success(self, _monotonic, _sleep):
    github = mock.Mock()
    github.commit_ci_state.side_effect = [
        ("pending", "Pending CI: Jenkins"),
        ("success", "All reported CI checks passed"),
    ]

    result = ambari_ai.wait_for_ci(github, "abc123", timeout=10, interval=1)

    self.assertEqual("All reported CI checks passed", result)
    _sleep.assert_called_once_with(1)


class GitRepositoryTest(unittest.TestCase):

  def setUp(self):
    self.temp_dir = tempfile.TemporaryDirectory()
    self.repo_path = Path(self.temp_dir.name)
    subprocess.run(["git", "init", "-b", "trunk"], cwd=str(self.repo_path), check=True,
                   stdout=subprocess.DEVNULL)
    subprocess.run(["git", "config", "user.name", "Ambari AI Test"],
                   cwd=str(self.repo_path), check=True)
    subprocess.run(["git", "config", "user.email", "ambari-ai@example.invalid"],
                   cwd=str(self.repo_path), check=True)
    (self.repo_path / "tracked.txt").write_text("base\n", encoding="utf-8")
    subprocess.run(["git", "add", "tracked.txt"], cwd=str(self.repo_path), check=True)
    subprocess.run(["git", "commit", "-m", "Initial commit"], cwd=str(self.repo_path),
                   check=True, stdout=subprocess.DEVNULL)
    self.repository = ambari_ai.GitRepository(self.repo_path)

  def tearDown(self):
    self.temp_dir.cleanup()

  def test_prepare_branch_and_commit_only_selected_files(self):
    (self.repo_path / "tracked.txt").write_text("changed\n", encoding="utf-8")
    (self.repo_path / "unrelated.txt").write_text("do not commit\n", encoding="utf-8")

    self.repository.prepare_branch("AMBARI-26474", "trunk", "trunk")
    staged = self.repository.commit_files(
        ["tracked.txt"], "AMBARI-26474: Fix ambari server py test")

    self.assertEqual(["tracked.txt"], staged)
    self.assertEqual("AMBARI-26474", self.repository.current_branch())
    self.assertEqual(
        "AMBARI-26474: Fix ambari server py test",
        self.repository.run(["log", "-1", "--pretty=%s"]),
    )
    self.assertEqual("?? unrelated.txt", self.repository.run(["status", "--short"]))

  def test_commit_refuses_an_existing_staged_change(self):
    (self.repo_path / "tracked.txt").write_text("changed\n", encoding="utf-8")
    subprocess.run(["git", "add", "tracked.txt"], cwd=str(self.repo_path), check=True)

    with self.assertRaisesRegex(ambari_ai.CommandError, "already contains staged files"):
      self.repository.commit_files(["tracked.txt"], "AMBARI-26474: Fix test")


class EnvironmentTest(unittest.TestCase):

  @mock.patch.dict(os.environ, {"GITHUB_TOKEN": "author-token"}, clear=True)
  def test_approve_requires_a_separate_review_token(self):
    with self.assertRaisesRegex(ambari_ai.CommandError, "GITHUB_REVIEW_TOKEN"):
      ambari_ai.github_client_from_environment(review=True)


class ApproveCommandTest(unittest.TestCase):

  @mock.patch("ambari_ai.github_client_from_environment")
  def test_rejects_self_approval_before_creating_a_review(self, client_factory):
    github = client_factory.return_value
    github.authenticated_user.return_value = "JiaLiangC"
    github.get_pull_request.return_value = {"user": {"login": "jialiangc"}}
    args = mock.Mock(number=3995, body="+1")

    with self.assertRaisesRegex(ambari_ai.CommandError, "cannot approve their own"):
      ambari_ai.command_pr_approve(args)

    github.approve_pull_request.assert_not_called()


if __name__ == "__main__":
  unittest.main()
