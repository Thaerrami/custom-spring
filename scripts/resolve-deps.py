#!/usr/bin/env python3
"""
Resolve dependencies declared in pom.json and download JARs from Maven Central.

This is the JSON equivalent of what Maven does when you run `mvn dependency:resolve`.
Each dependency becomes a URL:

  {repo}/{groupId as path}/{artifactId}/{version}/{artifactId}-{version}.jar

Example:
  com.h2database:h2:2.2.224
  → https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
"""

import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
POM = ROOT / "pom.json"
LIB = ROOT / "lib"
LOCK = LIB / ".lock.json"


def group_to_path(group_id: str) -> str:
    return group_id.replace(".", "/")


def jar_url(repo: str, group_id: str, artifact_id: str, version: str) -> str:
    path = group_to_path(group_id)
    filename = f"{artifact_id}-{version}.jar"
    return f"{repo.rstrip('/')}/{path}/{artifact_id}/{version}/{filename}"


def download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    print(f"  ↓ {dest.name}")
    print(f"    {url}")
    request = urllib.request.Request(url, headers={"User-Agent": "custom-spring/1.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        dest.write_bytes(response.read())


def main() -> int:
    if not POM.exists():
        print(f"ERROR: {POM} not found", file=sys.stderr)
        return 1

    with POM.open() as f:
        project = json.load(f)

    repos = project.get("repositories", [{"url": "https://repo1.maven.org/maven2"}])
    repo_url = repos[0]["url"]

    dependencies = [
        d for d in project.get("dependencies", [])
        if d.get("scope", "compile") in ("compile", "runtime")
    ]

    print(f"📦 Resolving {len(dependencies)} dependencies from pom.json")
    print(f"   Repository: {repo_url}")
    print(f"   Output:     {LIB}/")
    print()

    lock = {"resolved": []}

    for dep in dependencies:
        group_id = dep["groupId"]
        artifact_id = dep["artifactId"]
        version = dep["version"]
        url = jar_url(repo_url, group_id, artifact_id, version)
        dest = LIB / f"{artifact_id}-{version}.jar"

        if dest.exists():
            print(f"  ✓ {dest.name} (cached)")
        else:
            try:
                download(url, dest)
            except urllib.error.HTTPError as e:
                print(f"ERROR: HTTP {e.code} for {url}", file=sys.stderr)
                return 1
            except urllib.error.URLError as e:
                print(f"ERROR: {e.reason}", file=sys.stderr)
                return 1

        lock["resolved"].append({
            "groupId": group_id,
            "artifactId": artifact_id,
            "version": version,
            "file": dest.name,
            "url": url,
        })

    LOCK.write_text(json.dumps(lock, indent=2))
    print()
    print(f"✅ {len(lock['resolved'])} JAR(s) ready in lib/")
    print(f"   Lock file: {LOCK.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
