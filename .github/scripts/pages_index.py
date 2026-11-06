"""Writes the GitHub Pages landing page: the latest main build, then every other branch.
"""
import html
import json
import os
import sys
import urllib.request
from pathlib import Path

REPO = os.environ["REPO"]
TOKEN = os.environ.get("GH_TOKEN")
MAIN = "main"

PAGE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Cortex Camping builds</title>
<style>
  body {{ font: 16px/1.5 system-ui, sans-serif; color: #1f3a2d; background: #f5f7f3; max-width: 760px; margin: 0 auto; padding: 32px 16px; }}
  h1 {{ margin: 0 0 24px; }}
  h2 {{ margin: 32px 0 12px; font-size: 18px; }}
  .card {{ background: #fff; border: 1px solid #d5ddd5; border-radius: 12px; padding: 16px; margin-bottom: 12px; }}
  .branch {{ font-weight: 600; }}
  .meta {{ color: #5a6b60; font-size: 14px; overflow-wrap: anywhere; }}
  .links {{ margin-top: 8px; display: flex; flex-wrap: wrap; gap: 8px 16px; }}
  a {{ color: #275d3f; }}
</style>
</head>
<body>
<h1>Cortex Camping builds</h1>
<h2>Latest main</h2>
{main}
<h2>Other branches</h2>
{others}
</body>
</html>
"""


def api(path):
    request = urllib.request.Request(f"https://api.github.com/repos/{REPO}/{path}")
    request.add_header("Accept", "application/vnd.github+json")
    if TOKEN:
        request.add_header("Authorization", f"Bearer {TOKEN}")
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def published_sha(pages, branch, head):
    """The newest build we have for a branch, the head commit if it's finished publishing."""
    if (pages / branch / head).is_dir():
        return head
    marker = pages / branch / "latest.txt"
    if marker.is_file():
        sha = marker.read_text().strip()
        if (pages / branch / sha).is_dir():
            return sha
    return None


def apk_links():
    """Maps each APK release tag to its download link."""
    links = {}
    for release in api("releases?per_page=100"):
        for asset in release["assets"]:
            if asset["name"].endswith(".apk"):
                links[release["tag_name"]] = asset["browser_download_url"]
    return links


def card(pages, branch, head, apks):
    sha = published_sha(pages, branch, head)
    name = html.escape(branch)
    apk = apks.get("apk-" + branch.replace("/", "-"))
    links = []
    if sha:
        for folder, label in (("docs", "Docs"), ("tests", "Test report")):
            if (pages / branch / sha / folder).is_dir():
                links.append(f'<a href="{html.escape(f"{branch}/{sha}/{folder}/")}">{label}</a>')
    if apk:
        links.append(f'<a href="{html.escape(apk)}">Download APK</a>')

    commit = api(f"commits/{sha or head}")
    title = commit["commit"]["message"].splitlines()[0]
    date = commit["commit"]["committer"]["date"][:10]
    url = commit["html_url"]
    meta = f'<a href="{html.escape(url)}">{(sha or head)[:7]}</a> · {date} · {html.escape(title)}'
    if not sha:
        links.append("<span>No published build yet</span>")
    return (
        f'<div class="card"><div class="branch">{name}</div>'
        f'<div class="meta">{meta}</div>'
        f'<div class="links">{"".join(links)}</div></div>'
    )


def main():
    pages = Path(sys.argv[1])
    apks = apk_links()
    heads = {b["name"]: b["commit"]["sha"] for b in api("branches?per_page=100") if b["name"] != "gh-pages"}
    others = [card(pages, name, sha, apks) for name, sha in sorted(heads.items(), key=lambda b: b[0].lower()) if name != MAIN]
    (pages / "index.html").write_text(PAGE.format(
        main=card(pages, MAIN, heads[MAIN], apks),
        others="\n".join(others) or "<p>No other branches right now.</p>",
    ))


if __name__ == "__main__":
    main()
