#!/usr/bin/env python3

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

"""Create Ambari JIRAs and submit, approve, or merge GitHub pull requests."""

import argparse
import base64
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple
from urllib import error, parse, request


ISSUE_KEY_PATTERN = re.compile(r"^AMBARI-\d+$", re.IGNORECASE)
DEFAULT_JIRA_URL = "https://issues.apache.org/jira"
DEFAULT_GITHUB_API_URL = "https://api.github.com"
DEFAULT_GITHUB_REPO = "apache/ambari"
DEFAULT_BASE_BRANCH = "trunk"
CONTRIBUTING_GUIDE = (
    "https://cwiki.apache.org/confluence/display/AMBARI/How+to+Contribute"
)


class CommandError(RuntimeError):
  """Raised for a user-facing command failure."""


class ApiError(CommandError):
  """Raised when a remote API returns an unsuccessful response."""

  def __init__(self, method: str, url: str, status: int, response_body: str):
    detail = response_body.strip()
    try:
      parsed_body = json.loads(response_body)
      detail = parsed_body.get("message") or parsed_body.get("errorMessages") or detail
    except (ValueError, AttributeError):
      pass
    super().__init__("{} {} failed with HTTP {}: {}".format(
        method, url, status, detail or "empty response"))
    self.status = status
    self.response_body = response_body


class HttpClient:
  """Small JSON HTTP client based on the Python standard library."""

  def __init__(self, base_url: str, default_headers: Optional[Dict[str, str]] = None):
    self.base_url = base_url.rstrip("/")
    self.default_headers = default_headers or {}

  def request(
      self,
      method: str,
      path: str,
      payload: Optional[Dict[str, Any]] = None,
      query: Optional[Dict[str, Any]] = None,
  ) -> Any:
    url = "{}{}".format(self.base_url, path)
    if query:
      url = "{}?{}".format(url, parse.urlencode(query))

    headers = dict(self.default_headers)
    data = None
    if payload is not None:
      data = json.dumps(payload).encode("utf-8")
      headers["Content-Type"] = "application/json"

    http_request = request.Request(url, data=data, headers=headers, method=method)
    try:
      with request.urlopen(http_request, timeout=30) as response:
        body = response.read().decode("utf-8")
    except error.HTTPError as exc:
      body = exc.read().decode("utf-8", errors="replace")
      raise ApiError(method, url, exc.code, body) from exc
    except error.URLError as exc:
      raise ApiError(method, url, 0, str(exc.reason)) from exc

    if not body:
      return None
    try:
      return json.loads(body)
    except ValueError as exc:
      raise ApiError(method, url, 200, "Expected JSON response: {}".format(body)) from exc


class JiraClient:
  """Minimal ASF JIRA REST API client."""

  def __init__(
      self,
      base_url: str,
      username: Optional[str] = None,
      password: Optional[str] = None,
      bearer_token: Optional[str] = None,
      http_client: Optional[HttpClient] = None,
  ):
    headers = {"Accept": "application/json", "User-Agent": "ambari-ai/1.0"}
    if bearer_token:
      headers["Authorization"] = "Bearer {}".format(bearer_token)
    elif username and password:
      credential = base64.b64encode(
          "{}:{}".format(username, password).encode("utf-8")).decode("ascii")
      headers["Authorization"] = "Basic {}".format(credential)
    self.http = http_client or HttpClient(base_url, headers)

  def create_issue(
      self,
      issue_type: str,
      summary: str,
      description: str,
      project: str = "AMBARI",
  ) -> Dict[str, Any]:
    payload = {
        "fields": {
            "project": {"key": project},
            "issuetype": {"name": issue_type},
            "summary": summary,
            "description": description,
        }
    }
    return self.http.request("POST", "/rest/api/2/issue", payload=payload)

  def update_issue_description(self, issue_key: str, description: str) -> None:
    self.http.request(
        "PUT",
        "/rest/api/2/issue/{}".format(parse.quote(normalize_issue_key(issue_key))),
        payload={"fields": {"description": description}},
    )

  def get_issue(self, issue_key: str) -> Dict[str, Any]:
    return self.http.request(
        "GET",
        "/rest/api/2/issue/{}".format(parse.quote(issue_key)),
        query={"fields": "summary,status"},
    )


class GitHubClient:
  """Minimal GitHub REST API client for Ambari pull requests."""

  def __init__(
      self,
      token: str,
      repo: str = DEFAULT_GITHUB_REPO,
      api_url: str = DEFAULT_GITHUB_API_URL,
      http_client: Optional[HttpClient] = None,
  ):
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": "Bearer {}".format(token),
        "User-Agent": "ambari-ai/1.0",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    self.http = http_client or HttpClient(api_url, headers)
    self.repo = repo

  def authenticated_user(self) -> str:
    return self.http.request("GET", "/user")["login"]

  def find_open_pull_request(self, head: str, base: str) -> Optional[Dict[str, Any]]:
    pulls = self.http.request(
        "GET",
        "/repos/{}/pulls".format(self.repo),
        query={"state": "open", "head": head, "base": base},
    )
    return pulls[0] if pulls else None

  def create_pull_request(
      self,
      title: str,
      body: str,
      head: str,
      base: str,
      draft: bool = False,
  ) -> Dict[str, Any]:
    return self.http.request(
        "POST",
        "/repos/{}/pulls".format(self.repo),
        payload={
            "title": title,
            "body": body,
            "head": head,
            "base": base,
            "draft": draft,
        },
    )

  def get_pull_request(self, number: int) -> Dict[str, Any]:
    return self.http.request("GET", "/repos/{}/pulls/{}".format(self.repo, number))

  def approve_pull_request(self, number: int, body: str) -> Dict[str, Any]:
    return self.http.request(
        "POST",
        "/repos/{}/pulls/{}/reviews".format(self.repo, number),
        payload={"event": "APPROVE", "body": body},
    )

  def commit_ci_state(self, sha: str) -> Tuple[str, str]:
    status = self.http.request(
        "GET", "/repos/{}/commits/{}/status".format(self.repo, sha))
    checks = self.http.request(
        "GET", "/repos/{}/commits/{}/check-runs".format(self.repo, sha))

    statuses = status.get("statuses", [])
    check_runs = checks.get("check_runs", [])
    failures = []
    pending = []

    for item in statuses:
      state = item.get("state")
      name = item.get("context", "commit status")
      if state in ("failure", "error"):
        failures.append(name)
      elif state != "success":
        pending.append(name)

    passing_conclusions = {"success", "neutral", "skipped"}
    for item in check_runs:
      name = item.get("name", "check run")
      if item.get("status") != "completed":
        pending.append(name)
      elif item.get("conclusion") not in passing_conclusions:
        failures.append(name)

    if failures:
      return "failure", "Failed CI: {}".format(", ".join(sorted(set(failures))))
    if not statuses and not check_runs:
      return "pending", "No CI status has been reported"
    if pending:
      return "pending", "Pending CI: {}".format(", ".join(sorted(set(pending))))
    return "success", "All reported CI checks passed"

  def merge_pull_request(
      self,
      number: int,
      sha: str,
      merge_method: str = "merge",
  ) -> Dict[str, Any]:
    return self.http.request(
        "PUT",
        "/repos/{}/pulls/{}/merge".format(self.repo, number),
        payload={"sha": sha, "merge_method": merge_method},
    )


class GitRepository:
  """Run the narrowly scoped Git operations needed to submit a pull request."""

  def __init__(self, path: Path):
    self.path = path

  def run(self, args: Sequence[str], capture: bool = True) -> str:
    command = ["git"] + list(args)
    try:
      result = subprocess.run(
          command,
          cwd=str(self.path),
          check=True,
          text=True,
          stdout=subprocess.PIPE if capture else None,
          stderr=subprocess.PIPE if capture else None,
      )
    except subprocess.CalledProcessError as exc:
      detail = (exc.stderr or exc.stdout or "").strip()
      raise CommandError("{} failed: {}".format(" ".join(command), detail)) from exc
    return (result.stdout or "").strip()

  def ensure_repository(self) -> None:
    if self.run(["rev-parse", "--is-inside-work-tree"]) != "true":
      raise CommandError("{} is not a Git work tree".format(self.path))

  def current_branch(self) -> str:
    return self.run(["branch", "--show-current"])

  def has_local_branch(self, branch: str) -> bool:
    result = subprocess.run(
        ["git", "show-ref", "--verify", "--quiet", "refs/heads/{}".format(branch)],
        cwd=str(self.path),
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    return result.returncode == 0

  def resolve_base_ref(self, base: str, base_ref: Optional[str]) -> str:
    candidates = [base_ref] if base_ref else ["origin/{}".format(base), base]
    for candidate in candidates:
      if not candidate:
        continue
      result = subprocess.run(
          ["git", "rev-parse", "--verify", "{}^{{commit}}".format(candidate)],
          cwd=str(self.path),
          stdout=subprocess.DEVNULL,
          stderr=subprocess.DEVNULL,
      )
      if result.returncode == 0:
        return candidate
    raise CommandError("Cannot resolve a local base ref for '{}'".format(base))

  def prepare_branch(self, branch: str, base: str, base_ref: Optional[str]) -> None:
    current = self.current_branch()
    if current == branch:
      return
    if self.has_local_branch(branch):
      raise CommandError(
          "Local branch '{}' already exists; switch to it or choose --branch".format(branch))
    resolved_base = self.resolve_base_ref(base, base_ref)
    self.run(["switch", "-c", branch, resolved_base], capture=False)

  def commit_files(self, files: Sequence[str], message: str) -> List[str]:
    already_staged = self.run(["diff", "--cached", "--name-only"])
    if already_staged:
      raise CommandError(
          "The index already contains staged files; commit or unstage them first: {}".format(
              ", ".join(already_staged.splitlines())))

    self.run(["add", "--"] + list(files), capture=False)
    staged = self.run(["diff", "--cached", "--name-only"]).splitlines()
    if not staged:
      raise CommandError("The selected --files contain no changes")
    self.run(["commit", "-m", message], capture=False)
    return staged

  def push(self, destination: str, branch: str) -> None:
    self.run(["push", "-u", destination, branch], capture=False)


def normalize_issue_key(issue_key: str) -> str:
  normalized = issue_key.upper()
  if not ISSUE_KEY_PATTERN.fullmatch(normalized):
    raise CommandError("Invalid Ambari issue key: {}".format(issue_key))
  return normalized


def normalize_summary(issue_key: str, summary: str) -> str:
  value = summary.strip()
  value = re.sub(
      r"^\[?{}\]?\s*:\s*".format(re.escape(issue_key)), "", value, flags=re.IGNORECASE)
  if not value:
    raise CommandError("The issue summary cannot be empty")
  return value


def pull_request_title(issue_key: str, summary: str) -> str:
  return "{}: {}".format(issue_key, normalize_summary(issue_key, summary))


def markdown_to_jira(value: str) -> str:
  """Convert the Markdown accepted by the CLI to Jira wiki markup."""
  lines = []
  in_code_block = False
  code_language = ""

  for line in value.splitlines():
    fence = re.fullmatch(r"\s*```([^`]*)\s*", line)
    if fence:
      if in_code_block:
        lines.append("{code}")
        in_code_block = False
        code_language = ""
      else:
        code_language = fence.group(1).strip()
        lines.append("{code%s}" % (":" + code_language if code_language else ""))
        in_code_block = True
      continue

    if in_code_block:
      lines.append(line)
      continue

    heading = re.match(r"^(#{1,6})\s+(.+)$", line)
    if heading:
      line = "h{}. {}".format(len(heading.group(1)), heading.group(2))
    else:
      unordered = re.match(r"^(\s*)[-+]\s+(.+)$", line)
      ordered = re.match(r"^(\s*)\d+[.)]\s+(.+)$", line)
      if unordered:
        depth = max(1, len(unordered.group(1)) // 2 + 1)
        line = "{} {}".format("*" * depth, unordered.group(2))
      elif ordered:
        depth = max(1, len(ordered.group(1)) // 2 + 1)
        line = "{} {}".format("#" * depth, ordered.group(2))

    protected = []

    def protect(replacement: str) -> str:
      protected.append(replacement)
      return "\x00{}\x00".format(len(protected) - 1)

    line = re.sub(
        r"`([^`\n]+)`",
        lambda match: protect("{{" + match.group(1) + "}}"),
        line,
    )
    line = re.sub(
        r"\[([^\]\n]+)\]\(([^)\s]+)\)",
        lambda match: protect("[{}|{}]".format(match.group(1), match.group(2))),
        line,
    )
    line = re.sub(r"\*\*([^*\n]+)\*\*", r"*\1*", line)
    line = re.sub(r"__([^_\n]+)__", r"*\1*", line)
    line = re.sub(r"(?<!\*)\*([^*\n]+)\*(?!\*)", r"_\1_", line)
    for index, replacement in enumerate(protected):
      line = line.replace("\x00{}\x00".format(index), replacement)
    lines.append(line)

  if in_code_block:
    lines.append("{code}")
  return "\n".join(lines)


def build_pull_request_body(
    issue_key: str,
    changes: str,
    test_result: str,
    test_command: Optional[str] = None,
    evidence: Optional[Sequence[str]] = None,
) -> str:
  test_parts = [test_result.strip()]
  if test_command:
    test_parts.extend(["", "```shell", test_command.strip(), "```"])
  if evidence:
    test_parts.extend(["", "\n".join(item.strip() for item in evidence)])

  return "\n".join([
      "Issue: [{}]({}/browse/{})".format(
          issue_key, DEFAULT_JIRA_URL, issue_key),
      "",
      "## What changes were proposed in this pull request?",
      "",
      changes.strip(),
      "",
      "## How was this patch tested?",
      "",
      "\n".join(test_parts),
      "",
      "Please review [Ambari Contributing Guide]({}) before opening a pull request.".format(
          CONTRIBUTING_GUIDE),
  ])


def read_argument(value: Optional[str], file_name: Optional[str], label: str) -> str:
  if value and file_name:
    raise CommandError("Use either --{} or --{}-file, not both".format(label, label))
  if file_name:
    try:
      return Path(file_name).read_text(encoding="utf-8").strip()
    except OSError as exc:
      raise CommandError("Cannot read {}: {}".format(file_name, exc)) from exc
  if value:
    return value.strip()
  raise CommandError("Provide --{} or --{}-file".format(label, label))


def jira_client_from_environment(require_auth: bool = True) -> JiraClient:
  username = os.environ.get("ASF_JIRA_USER")
  password = os.environ.get("ASF_JIRA_TOKEN") or os.environ.get("ASF_JIRA_PASSWORD")
  bearer_token = os.environ.get("ASF_JIRA_BEARER_TOKEN")
  if require_auth and not bearer_token and not (username and password):
    raise CommandError(
        "Set ASF_JIRA_USER and ASF_JIRA_TOKEN (or ASF_JIRA_PASSWORD), "
        "or set ASF_JIRA_BEARER_TOKEN")
  return JiraClient(
      os.environ.get("ASF_JIRA_URL", DEFAULT_JIRA_URL),
      username=username,
      password=password,
      bearer_token=bearer_token,
  )


def github_client_from_environment(review: bool = False) -> GitHubClient:
  token_name = "GITHUB_REVIEW_TOKEN" if review else "GITHUB_TOKEN"
  token = os.environ.get(token_name)
  if not token:
    raise CommandError("Set the {} environment variable".format(token_name))
  return GitHubClient(
      token,
      repo=os.environ.get("GITHUB_REPO", DEFAULT_GITHUB_REPO),
      api_url=os.environ.get("GITHUB_API_URL", DEFAULT_GITHUB_API_URL),
  )


def command_issue_create(args: argparse.Namespace) -> Dict[str, Any]:
  description = markdown_to_jira(
      read_argument(args.description, args.description_file, "description"))
  client = jira_client_from_environment()
  issue = client.create_issue(args.type, args.summary.strip(), description)
  key = issue["key"]
  base_url = os.environ.get("ASF_JIRA_URL", DEFAULT_JIRA_URL).rstrip("/")
  return {"key": key, "url": "{}/browse/{}".format(base_url, key)}


def command_issue_update(args: argparse.Namespace) -> Dict[str, Any]:
  issue_key = normalize_issue_key(args.issue)
  description = markdown_to_jira(
      read_argument(args.description, args.description_file, "description"))
  client = jira_client_from_environment()
  client.update_issue_description(issue_key, description)
  base_url = os.environ.get("ASF_JIRA_URL", DEFAULT_JIRA_URL).rstrip("/")
  return {"key": issue_key, "url": "{}/browse/{}".format(base_url, issue_key)}


def command_pr_create(args: argparse.Namespace) -> Dict[str, Any]:
  issue_key = normalize_issue_key(args.issue)
  changes = read_argument(args.changes, args.changes_file, "changes")
  test_result = read_argument(args.test_result, args.test_result_file, "test-result")

  if args.summary:
    summary = args.summary
  else:
    summary = jira_client_from_environment(require_auth=False).get_issue(issue_key)["fields"][
        "summary"]
  summary = normalize_summary(issue_key, summary)
  title = pull_request_title(issue_key, summary)
  branch = args.branch or issue_key

  github = github_client_from_environment()
  fork_repo = args.fork_repo or os.environ.get("GITHUB_FORK_REPO")
  if not fork_repo:
    fork_repo = "{}/{}".format(github.authenticated_user(), github.repo.rsplit("/", 1)[-1])
  if "/" not in fork_repo:
    raise CommandError("--fork-repo must use the OWNER/REPO format")
  fork_owner = fork_repo.split("/", 1)[0]

  repository = GitRepository(Path(args.repo).resolve())
  repository.ensure_repository()
  repository.prepare_branch(branch, args.base, args.base_ref)
  staged_files = repository.commit_files(args.files, title)

  push_destination = args.push_remote or os.environ.get("GITHUB_PUSH_REMOTE", "fork")
  remotes = repository.run(["remote"]).splitlines()
  if push_destination == "fork" and "fork" not in remotes:
    push_destination = "git@github.com:{}.git".format(fork_repo)
  repository.push(push_destination, branch)

  head = "{}:{}".format(fork_owner, branch)
  existing = github.find_open_pull_request(head, args.base)
  if existing:
    pull = existing
    created = False
  else:
    body = build_pull_request_body(
        issue_key, changes, test_result, args.test_command, args.evidence)
    pull = github.create_pull_request(title, body, head, args.base, args.draft)
    created = True

  return {
      "number": pull["number"],
      "url": pull["html_url"],
      "title": title,
      "branch": branch,
      "files": staged_files,
      "created": created,
  }


def command_pr_approve(args: argparse.Namespace) -> Dict[str, Any]:
  github = github_client_from_environment(review=True)
  reviewer = github.authenticated_user()
  pull = github.get_pull_request(args.number)
  author = pull["user"]["login"]
  if reviewer.lower() == author.lower():
    raise CommandError(
        "GitHub user '{}' cannot approve their own pull request; "
        "set GITHUB_REVIEW_TOKEN for a different reviewer".format(reviewer))
  review = github.approve_pull_request(args.number, args.body)
  return {
      "number": args.number,
      "review_id": review["id"],
      "state": review["state"],
      "url": review.get("html_url"),
      "reviewer": reviewer,
  }


def wait_for_ci(
    github: GitHubClient,
    sha: str,
    timeout: int,
    interval: int,
) -> str:
  deadline = time.monotonic() + timeout
  while True:
    state, detail = github.commit_ci_state(sha)
    if state == "success":
      return detail
    if state == "failure":
      raise CommandError(detail)
    if time.monotonic() >= deadline:
      raise CommandError("Timed out waiting for CI: {}".format(detail))
    print(detail, file=sys.stderr)
    time.sleep(interval)


def command_pr_merge(args: argparse.Namespace) -> Dict[str, Any]:
  github = github_client_from_environment()
  pull = github.get_pull_request(args.number)
  if pull.get("merged"):
    return {
        "number": args.number,
        "merged": True,
        "sha": pull.get("merge_commit_sha"),
        "message": "Pull request was already merged",
    }
  if pull.get("state") != "open":
    raise CommandError("Pull request #{} is not open".format(args.number))

  head_sha = pull["head"]["sha"]
  ci_result = "CI wait skipped"
  if not args.skip_ci:
    ci_result = wait_for_ci(github, head_sha, args.timeout, args.interval)

  result = github.merge_pull_request(args.number, head_sha, args.method)
  if not result.get("merged"):
    raise CommandError(result.get("message", "GitHub did not merge the pull request"))
  return {
      "number": args.number,
      "merged": True,
      "sha": result.get("sha"),
      "message": result.get("message"),
      "ci": ci_result,
  }


def build_parser() -> argparse.ArgumentParser:
  parser = argparse.ArgumentParser(
      prog="ambari-ai",
      description="Create Ambari JIRAs and submit, approve, or merge pull requests.",
  )
  subparsers = parser.add_subparsers(dest="resource", required=True)

  issue_parser = subparsers.add_parser("issue", help="Manage ASF JIRA issues")
  issue_subparsers = issue_parser.add_subparsers(dest="action", required=True)
  issue_create = issue_subparsers.add_parser("create", help="Create an AMBARI issue")
  issue_create.add_argument("--type", required=True, help="JIRA issue type, such as Bug")
  issue_create.add_argument("--summary", required=True)
  issue_create.add_argument("--description")
  issue_create.add_argument("--description-file")
  issue_create.set_defaults(handler=command_issue_create)

  issue_update = issue_subparsers.add_parser(
      "update", help="Update an AMBARI issue description")
  issue_update.add_argument("--issue", required=True)
  issue_update.add_argument("--description")
  issue_update.add_argument("--description-file")
  issue_update.set_defaults(handler=command_issue_update)

  pr_parser = subparsers.add_parser("pr", help="Manage GitHub pull requests")
  pr_subparsers = pr_parser.add_subparsers(dest="action", required=True)

  pr_create = pr_subparsers.add_parser(
      "create", help="Commit selected files, push them, and create a pull request")
  pr_create.add_argument("--issue", required=True)
  pr_create.add_argument("--summary", help="Defaults to the JIRA summary")
  pr_create.add_argument("--changes")
  pr_create.add_argument("--changes-file")
  pr_create.add_argument("--test-result")
  pr_create.add_argument("--test-result-file")
  pr_create.add_argument("--test-command")
  pr_create.add_argument("--evidence", action="append", default=[])
  pr_create.add_argument("--files", nargs="+", required=True)
  pr_create.add_argument("--base", default=DEFAULT_BASE_BRANCH)
  pr_create.add_argument("--base-ref", help="Local base ref; defaults to origin/<base>")
  pr_create.add_argument("--branch", help="Defaults to the JIRA issue key")
  pr_create.add_argument("--fork-repo", help="GitHub fork in OWNER/REPO format")
  pr_create.add_argument("--push-remote", help="Git remote name or URL")
  pr_create.add_argument("--repo", default=".", help="Local Git work tree")
  pr_create.add_argument("--draft", action="store_true")
  pr_create.set_defaults(handler=command_pr_create)

  pr_approve = pr_subparsers.add_parser("approve", help="Approve a pull request")
  pr_approve.add_argument("--number", type=int, required=True)
  pr_approve.add_argument("--body", default="+1")
  pr_approve.set_defaults(handler=command_pr_approve)

  pr_merge = pr_subparsers.add_parser("merge", help="Wait for CI and merge a pull request")
  pr_merge.add_argument("--number", type=int, required=True)
  pr_merge.add_argument("--method", choices=("merge", "squash", "rebase"), default="merge")
  pr_merge.add_argument("--timeout", type=int, default=3600)
  pr_merge.add_argument("--interval", type=int, default=20)
  pr_merge.add_argument(
      "--skip-ci", action="store_true", help="Merge without waiting for reported CI checks")
  pr_merge.set_defaults(handler=command_pr_merge)
  return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
  parser = build_parser()
  args = parser.parse_args(argv)
  try:
    result = args.handler(args)
  except (CommandError, KeyError) as exc:
    print(json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=True), file=sys.stderr)
    return 1
  print(json.dumps({"ok": True, **result}, ensure_ascii=True, indent=2, sort_keys=True))
  return 0


if __name__ == "__main__":
  sys.exit(main())
