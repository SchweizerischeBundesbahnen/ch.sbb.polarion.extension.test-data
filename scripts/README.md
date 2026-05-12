# Bulk test-data generation script

`bulk-generate.sh` drives the test-data extension REST API to populate a Polarion project with a large amount of synthetic data.

## What it produces

For a default profile (`DOC_COUNT=100`, `WI_PER_DOC=500`, `REVISION_PASSES=3`):

- 100 documents, each with 500 workitems (intra-document links and SVG images included)
- ~2000–2500 SVN revisions per document
- ~100 000 cross-document workitem links
- 3 project baselines: `after-initial-creation`, `mid-after-pass-1`, `final`
- 2 baseline collections: `collection-initial-q1` (25 docs at initial baseline), `collection-final-h1` (50 docs at final baseline)
- Linked-revision references on the first 10 documents pointing to the 3 baseline revisions

## Algorithm

The script executes the steps below sequentially. Every step writes a checkpoint marker to `$STATE_DIR`; re-running after an interruption resumes from the first incomplete step. Per-document work inside steps 1 and 4 is parallelized across `DOC_PARALLELISM` workers.

1. **Create documents** — `POST /projects/{p}/spaces/{s}/documents/{name}?quantity=WI_PER_DOC` for each `doc_001..doc_NNN`. For every workitem the server commits twice (once for `workItem.save()`, once for `document.save()`), producing **~2× WI_PER_DOC initial revisions** per document. Intra-document workitem links and SVG images are generated on the server.

2. **Cross-document workitem links** — `POST /projects/{p}/cross-document-links` with the full document list. For every workitem in every listed document the server adds `LINKS_PER_WI` links to random workitems in **other** documents (role: `LINK_ROLE`, default `relates_to`). One SVN commit per source workitem-batch.

3. **Baseline `after-initial-creation`** — `POST /projects/{p}/baselines/after-initial-creation`. The server resolves the current repository HEAD, calls `IBaselinesManager.createBaseline(name, description, revision, user)`, explicitly saves the baseline, and returns `{name, revision}`. The script captures the revision into `$STATE_DIR/baseline-*.rev` for later steps.

4. **Revision passes** (×`REVISION_PASSES`) — for each pass `p ∈ 1..REVISION_PASSES` and each document, call `PATCH /projects/{p}/spaces/{s}/documents/{name}/change-wi-descriptions?interval=1`. The server iterates all workitems of the document and rewrites their title/description; **each workitem save = one SVN commit = one revision**. One pass therefore produces ~`WI_PER_DOC` revisions per document. Between pass 1 and the rest, baseline **`mid-after-pass-1`** is created.

5. **Baseline `final`** — same flow as step 3, taken after all revision passes complete.

6. **Linked revisions** — for the first `min(10, DOC_COUNT)` documents, `POST .../linked-revisions` with the three captured baseline revisions. Per request, the server picks `workItemsPerRevision=3` random workitems and attaches each revision via `IWorkItem.addLinkedRevision(repositoryName=null, revision)`. This is how individual workitems get "Linked Revisions" pointing at fixed baseline states.

7. **Collections** — `POST /projects/{p}/collections/{name}` twice:
   - `collection-initial-q1` — first `min(25, DOC_COUNT)` documents pinned at `after-initial-creation`
   - `collection-final-h1`  — first `min(50, DOC_COUNT)` documents pinned at `final`

   Each call resolves each document at its baseline revision via `IDataService.getVersionedInstance(...)`, then calls `IBaselineCollection.addElement(versionedModule)` and saves.

### Revisions math (default profile)

```
per document = 2 × WI_PER_DOC          (initial create, both saves)
             + REVISION_PASSES × WI_PER_DOC
             ≈ 500 × (2 + 3) = 2500 SVN revisions

total commits = DOC_COUNT × per-document
              ≈ 100 × 2500 = 250 000
```

Cross-document links, baselines, linked-revisions and collections add a handful of additional commits — negligible against the per-document totals.

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

Default profile: 100 documents × 500 workitems × 3 revision passes ≈ 2000–2500 SVN revisions per document. See [Algorithm](#algorithm) for what happens at each step.

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

Default profile ≈ 250 000 SVN commits (see [Revisions math](#revisions-math-default-profile)). On a single Polarion instance expect 1–2 hours wall-clock with `--parallelism 8`; effective parallel scaling is ~3× rather than the nominal worker count because Polarion serializes commits internally. The script is built to be safely Ctrl-C-able and resumed.

If you need to push the count further (more documents, more revisions), bump `DOC_COUNT`, `REVISION_PASSES`, `DOC_PARALLELISM`. Note that very high `DOC_PARALLELISM` will saturate the Polarion instance — start with 4–8 and watch CPU/IO before increasing.
