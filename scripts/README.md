# Bulk test-data generation script

`bulk-generate.sh` drives the test-data extension REST API to populate a Polarion project with a large amount of synthetic data:

- **N documents** × **M workitems** per document (intra-document links + images included)
- **K revision passes** per document — each pass calls `change-wi-descriptions?interval=1`, producing ~M revisions per pass
- **Cross-document workitem links** between every document and the rest
- **Linked-revision references** from the first 10 documents to the captured baseline revisions
- **3 baselines** (`after-initial-creation`, `mid-after-pass-1`, `final`)
- **2 baseline collections** (`collection-initial-q1`, `collection-final-h1`)

## Prerequisites

1. Polarion 2512 with the `ch.sbb.polarion.extension.test-data` jar deployed.
2. An empty target project (e.g. created from a template) where the user has write access.
3. A Polarion personal access token with the project's edit permissions.
4. `bash`, `curl`, optionally `jq` (improves JSON parsing for baseline revisions).

## Usage

Each option can be passed as a CLI flag **or** via the matching environment variable. CLI flags take precedence over env vars when both are present. Run `./scripts/bulk-generate.sh --help` for the full flag list.

With env vars:

```bash
APP_URL=http://localhost \
APP_TOKEN=<your-token> \
PROJECT_ID=test_data_project \
./scripts/bulk-generate.sh
```

Equivalent with CLI flags:

```bash
./scripts/bulk-generate.sh \
    --base-url http://localhost \
    --token <your-token> \
    --project test_data_project
```

> **Note:** `APP_URL` is the Polarion **server root** (no `/polarion` path). The script appends `/polarion/test-data/rest/api` itself.

Default profile: 100 documents × 500 workitems × 4 revision passes ≈ 2000 revisions per document.

## Configuration

| Env var | CLI flag | Default | Notes |
|---|---|---|---|
| `APP_URL` | `-u`, `--base-url` | _required_ | Server root, e.g. `http://localhost` |
| `APP_TOKEN` | `-t`, `--token` | _required_ | Polarion PAT |
| `PROJECT_ID` | `-p`, `--project` | _required_ | Target project id |
| `SPACE_ID` | `-s`, `--space` | `_default` | Space for all generated documents |
| `DOC_COUNT` | `--doc-count` | `100` | Number of documents |
| `WI_PER_DOC` | `--wi-per-doc` | `500` | Workitems per document (intra-doc links generated automatically) |
| `REVISION_PASSES` | `--revision-passes` | `3` | Each pass changes every workitem (≈ +`WI_PER_DOC` revisions per doc) |
| `DOC_PARALLELISM` | `--parallelism` | `4` | Parallel doc creates / revision passes. SVN serializes per resource so this scales with number of distinct docs |
| `LINK_ROLE` | `--link-role` | `relates_to` | Role id used for cross-doc links |
| `LINKS_PER_WI` | `--links-per-wi` | `2` | Cross-document links added to each workitem |
| `DOC_PREFIX` | `--doc-prefix` | `doc_` | Document name prefix; final names are `doc_001`..`doc_NNN` |
| `STATE_DIR` | `--state-dir` | `./.bulk-state` | Checkpoint dir — re-running the script skips completed steps |

## Idempotency / resuming

Each step writes a marker file to `$STATE_DIR`. Re-running the script after an interruption skips finished work, including individual document creates and individual revision-pass calls. To force a full regeneration delete `$STATE_DIR`.

## Scale and timing

100 docs × 500 WI × 4 passes ≈ 200 000 SVN commits. On a single Polarion instance expect hours. The script is built to be safely Ctrl-C-able and resumed.

If you need to push the count further (more documents, more revisions), bump `DOC_COUNT`, `REVISION_PASSES`, `DOC_PARALLELISM`. Note that very high `DOC_PARALLELISM` will saturate the Polarion instance — start with 4 and watch CPU/IO before increasing.
