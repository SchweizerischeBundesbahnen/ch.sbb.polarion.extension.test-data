#!/usr/bin/env bash
# Bulk generator for a large Polarion test project.
# Drives the test-data extension REST API to produce:
#   - DOC_COUNT documents, each with WI_PER_DOC workitems
#   - REVISION_PASSES additional change passes per document (~WI_PER_DOC revisions per pass)
#   - cross-document workitem links
#   - linked-revision references between documents
#   - 3 baselines and 2 baseline collections
#
# Configuration is taken from environment variables; any CLI flag overrides
# the corresponding environment variable.
#
# Usage:
#   APP_URL=http://localhost \
#   APP_TOKEN=... \
#   PROJECT_ID=test_data_project \
#   ./scripts/bulk-generate.sh
#
# APP_URL is the Polarion server root (no /polarion path); the script appends
# /polarion/test-data/rest/api itself.
#
# Equivalent with CLI flags:
#   ./scripts/bulk-generate.sh \
#       --base-url http://localhost \
#       --token <PAT> \
#       --project test_data_project
#
# Run with --help for the full flag list.

set -euo pipefail

# Defaults pulled from env vars (each can be overridden by the matching CLI flag below).
APP_URL="${APP_URL:-}"
APP_TOKEN="${APP_TOKEN:-}"
PROJECT_ID="${PROJECT_ID:-}"
SPACE_ID="${SPACE_ID:-_default}"
DOC_COUNT="${DOC_COUNT:-100}"
WI_PER_DOC="${WI_PER_DOC:-500}"
REVISION_PASSES="${REVISION_PASSES:-3}"
DOC_PARALLELISM="${DOC_PARALLELISM:-4}"
DOC_PREFIX="${DOC_PREFIX:-doc_}"
STATE_DIR="${STATE_DIR:-./.bulk-state}"
LINK_ROLE="${LINK_ROLE:-relates_to}"
LINKS_PER_WI="${LINKS_PER_WI:-2}"

usage() {
  cat <<EOF
Bulk generator for a large Polarion test project.

Each option can be passed as a CLI flag or via the matching environment
variable. CLI flags take precedence over environment variables.

Required:
  -u, --base-url URL          APP_URL    (server root, e.g. http://localhost; /polarion/test-data/rest/api is appended automatically)
  -t, --token TOKEN           APP_TOKEN  (Polarion personal access token)
  -p, --project ID            PROJECT_ID

Optional (defaults shown):
  -s, --space ID              SPACE_ID=_default
      --doc-count N           DOC_COUNT=100
      --wi-per-doc N          WI_PER_DOC=500
      --revision-passes N     REVISION_PASSES=3
      --parallelism N         DOC_PARALLELISM=4
      --doc-prefix STR        DOC_PREFIX=doc_
      --state-dir PATH        STATE_DIR=./.bulk-state
      --link-role ROLE        LINK_ROLE=relates_to
      --links-per-wi N        LINKS_PER_WI=2
  -h, --help                  Show this message and exit
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    -u|--base-url)        APP_URL="$2"; shift 2 ;;
    --base-url=*)         APP_URL="${1#*=}"; shift ;;
    -t|--token)           APP_TOKEN="$2"; shift 2 ;;
    --token=*)            APP_TOKEN="${1#*=}"; shift ;;
    -p|--project)         PROJECT_ID="$2"; shift 2 ;;
    --project=*)          PROJECT_ID="${1#*=}"; shift ;;
    -s|--space)           SPACE_ID="$2"; shift 2 ;;
    --space=*)            SPACE_ID="${1#*=}"; shift ;;
    --doc-count)          DOC_COUNT="$2"; shift 2 ;;
    --doc-count=*)        DOC_COUNT="${1#*=}"; shift ;;
    --wi-per-doc)         WI_PER_DOC="$2"; shift 2 ;;
    --wi-per-doc=*)       WI_PER_DOC="${1#*=}"; shift ;;
    --revision-passes)    REVISION_PASSES="$2"; shift 2 ;;
    --revision-passes=*)  REVISION_PASSES="${1#*=}"; shift ;;
    --parallelism)        DOC_PARALLELISM="$2"; shift 2 ;;
    --parallelism=*)      DOC_PARALLELISM="${1#*=}"; shift ;;
    --doc-prefix)         DOC_PREFIX="$2"; shift 2 ;;
    --doc-prefix=*)       DOC_PREFIX="${1#*=}"; shift ;;
    --state-dir)          STATE_DIR="$2"; shift 2 ;;
    --state-dir=*)        STATE_DIR="${1#*=}"; shift ;;
    --link-role)          LINK_ROLE="$2"; shift 2 ;;
    --link-role=*)        LINK_ROLE="${1#*=}"; shift ;;
    --links-per-wi)       LINKS_PER_WI="$2"; shift 2 ;;
    --links-per-wi=*)     LINKS_PER_WI="${1#*=}"; shift ;;
    -h|--help)            usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

[ -n "$APP_URL" ]    || { echo "APP_URL / --base-url is required" >&2; exit 2; }
[ -n "$APP_TOKEN" ]  || { echo "APP_TOKEN / --token is required" >&2; exit 2; }
[ -n "$PROJECT_ID" ] || { echo "PROJECT_ID / --project is required" >&2; exit 2; }

API="${APP_URL%/}/polarion/test-data/rest/api"
mkdir -p "$STATE_DIR"

log() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

# Timing: declare an array that grows with each step's "name=secs" entry.
# Bash 3.2 has indexed arrays; we use that.
TIMING_NAMES=()
TIMING_SECS=()
SCRIPT_START_TS=$(date +%s)
now_ts() { date +%s; }
record_timing() { TIMING_NAMES+=("$1"); TIMING_SECS+=("$2"); }
fmt_duration() {
  local s=$1
  if   [ "$s" -lt 60 ];   then printf '%ds' "$s"
  elif [ "$s" -lt 3600 ]; then printf '%dm%02ds' $((s/60)) $((s%60))
  else                         printf '%dh%02dm%02ds' $((s/3600)) $(((s%3600)/60)) $((s%60))
  fi
}

# checkpoint <step-name> -> 0 if already done, otherwise records and returns 1
done_marker() { test -f "$STATE_DIR/$1.done"; }
mark_done() { touch "$STATE_DIR/$1.done"; }

# api <method> <path> [json-body] [extra-curl-args...]
# Retries on transient 5xx/network failures, returns response body on stdout, http status on stderr.
# Tunable per call via env vars:
#   API_MAX_ATTEMPTS  number of attempts including the first (default 5)
#   API_MAX_TIME      curl --max-time in seconds (default 7200 = 2h)
# Set API_MAX_ATTEMPTS=1 for endpoints where the server cannot tell a duplicate
# from a fresh request (e.g. one-shot bulk endpoints that grind for tens of minutes).
api() {
  local method="$1"; shift
  local path="$1"; shift
  local body=""
  if [ "$#" -gt 0 ]; then body="$1"; shift; fi
  local max="${API_MAX_ATTEMPTS:-5}"
  local max_time="${API_MAX_TIME:-7200}"
  local attempt=0 delay=2 status
  local tmp; tmp="$(mktemp)"
  while true; do
    attempt=$((attempt + 1))
    if [ -n "$body" ]; then
      status=$(curl -sS -o "$tmp" -w '%{http_code}' \
        -H "Authorization: Bearer $APP_TOKEN" \
        -H 'Content-Type: application/json' \
        -H 'Accept: application/json' \
        --max-time "$max_time" \
        -X "$method" "$API$path" \
        --data-binary "$body" \
        "$@" || echo 000)
    else
      status=$(curl -sS -o "$tmp" -w '%{http_code}' \
        -H "Authorization: Bearer $APP_TOKEN" \
        -H 'Accept: application/json' \
        --max-time "$max_time" \
        -X "$method" "$API$path" \
        "$@" || echo 000)
    fi
    if [[ "$status" =~ ^2 ]]; then
      cat "$tmp"
      rm -f "$tmp"
      printf '%s' "$status" >&2
      return 0
    fi
    if [[ "$status" =~ ^4 ]]; then
      log "  ERR $method $path -> $status: $(cat "$tmp" | head -c 400)"
      rm -f "$tmp"
      printf '%s' "$status" >&2
      return 1
    fi
    if [ "$attempt" -ge "$max" ]; then
      log "  GIVE UP $method $path after $attempt attempts (last status $status)"
      rm -f "$tmp"
      return 1
    fi
    log "  retry $attempt/$max for $method $path (status $status), sleep ${delay}s"
    sleep "$delay"; delay=$((delay * 2))
  done
}

extract_revision() {
  if command -v jq >/dev/null 2>&1; then
    jq -r '.revision // empty' 2>/dev/null
  else
    sed -n 's/.*"revision"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
  fi
}

# ----- step 1: create documents (parallel) -----
create_one_document() {
  local i="$1"
  local name="${DOC_PREFIX}$(printf '%03d' "$i")"
  local marker="doc-create-$name"
  done_marker "$marker" && return 0
  if api POST "/projects/$PROJECT_ID/spaces/$SPACE_ID/documents/$name?quantity=$WI_PER_DOC" "" >/dev/null 2>/dev/null; then
    mark_done "$marker"
    printf '.'
  else
    printf 'X'
    return 1
  fi
}

step_create_documents() {
  if done_marker "step-create-documents"; then
    log "step-create-documents: already done"
    return
  fi
  log "Creating $DOC_COUNT documents (parallel=$DOC_PARALLELISM, $WI_PER_DOC WI each)..."
  export -f api log create_one_document done_marker mark_done
  export STATE_DIR API APP_TOKEN PROJECT_ID SPACE_ID WI_PER_DOC DOC_PREFIX
  # xargs returns non-zero if any child failed; we want to keep going through the batch
  # and let the operator inspect markers/STATE_DIR. Individual checkpoints prevent re-creating successes.
  seq 1 "$DOC_COUNT" | xargs -n1 -P"$DOC_PARALLELISM" -I{} bash -c 'create_one_document "$@"' _ {} || true
  echo
  local missing=0
  for i in $(seq 1 "$DOC_COUNT"); do
    local name="${DOC_PREFIX}$(printf '%03d' "$i")"
    done_marker "doc-create-$name" || missing=$((missing + 1))
  done
  if [ "$missing" -gt 0 ]; then
    log "step-create-documents: $missing of $DOC_COUNT documents failed to create — fix and re-run (idempotent)"
    return 1
  fi
  mark_done "step-create-documents"
  log "step-create-documents: complete"
}

# ----- step 2: cross-document WI links -----
build_documents_json() {
  local arr=""
  for i in $(seq 1 "$DOC_COUNT"); do
    local name="${DOC_PREFIX}$(printf '%03d' "$i")"
    arr+="{\"spaceId\":\"$SPACE_ID\",\"documentName\":\"$name\"},"
  done
  printf '[%s]' "${arr%,}"
}

step_cross_doc_links() {
  if done_marker "step-cross-doc-links"; then
    log "step-cross-doc-links: already done"
    return
  fi
  log "Creating cross-document workitem links (linksPerWorkItem=$LINKS_PER_WI, role=$LINK_ROLE)..."
  local docs body created
  docs="$(build_documents_json)"
  body="{\"documents\":$docs,\"linksPerWorkItem\":$LINKS_PER_WI,\"linkRole\":\"$LINK_ROLE\"}"
  # No retry: a long-running bulk endpoint whose server-side work is not
  # cancellable by client retry. A retried POST kicks off a parallel duplicate
  # transaction on the server instead of replacing the first one.
  created="$(API_MAX_ATTEMPTS=1 api POST "/projects/$PROJECT_ID/cross-document-links" "$body")"
  log "  created links: $created"
  mark_done "step-cross-doc-links"
}

# ----- baselines -----
create_baseline() {
  local name="$1"
  local desc="$2"
  local marker="baseline-$name"
  if done_marker "$marker"; then
    cat "$STATE_DIR/$marker.rev"
    return
  fi
  local body rev
  body="$(api POST "/projects/$PROJECT_ID/baselines/$name?description=$(printf %s "$desc" | sed 's/ /%20/g')" "")"
  rev="$(printf '%s' "$body" | extract_revision)"
  printf '%s' "$rev" >"$STATE_DIR/$marker.rev"
  mark_done "$marker"
  log "  baseline '$name' created at revision: ${rev:-HEAD}"
  printf '%s' "$rev"
}

# ----- step 3: revision passes (sequential per doc, parallel across docs) -----
revision_pass_one_document() {
  local pass="$1" i="$2"
  local name="${DOC_PREFIX}$(printf '%03d' "$i")"
  local marker="rev-pass-$pass-$name"
  done_marker "$marker" && return 0
  if api PATCH "/projects/$PROJECT_ID/spaces/$SPACE_ID/documents/$name/change-wi-descriptions?interval=1" "" >/dev/null 2>/dev/null; then
    mark_done "$marker"
    printf '.'
  else
    printf 'X'
    return 1
  fi
}

step_revision_pass() {
  local pass="$1"
  if done_marker "step-rev-pass-$pass"; then
    log "step-rev-pass-$pass: already done"
    return
  fi
  log "Revision pass $pass/$REVISION_PASSES across $DOC_COUNT documents (parallel=$DOC_PARALLELISM)..."
  export -f api log revision_pass_one_document done_marker mark_done
  export STATE_DIR API APP_TOKEN PROJECT_ID SPACE_ID DOC_PREFIX
  seq 1 "$DOC_COUNT" | xargs -n1 -P"$DOC_PARALLELISM" -I{} bash -c 'revision_pass_one_document "$@"' _ "$pass" {} || true
  echo
  local missing=0
  for i in $(seq 1 "$DOC_COUNT"); do
    local name="${DOC_PREFIX}$(printf '%03d' "$i")"
    done_marker "rev-pass-$pass-$name" || missing=$((missing + 1))
  done
  if [ "$missing" -gt 0 ]; then
    log "step-rev-pass-$pass: $missing of $DOC_COUNT documents failed — fix and re-run"
    return 1
  fi
  mark_done "step-rev-pass-$pass"
  log "step-rev-pass-$pass: complete"
}

# ----- linked revisions: attach baseline revs as references on a few docs -----
step_linked_revisions() {
  local rev_initial="$1" rev_mid="$2" rev_final="$3"
  if done_marker "step-linked-revisions"; then
    log "step-linked-revisions: already done"
    return
  fi
  local n=$DOC_COUNT
  [ "$n" -gt 10 ] && n=10
  log "Adding linked-revisions to first $n document(s)..."
  local revs="\"$rev_initial\",\"$rev_mid\",\"$rev_final\""
  local body="{\"revisions\":[$revs],\"workItemsPerRevision\":3,\"comment\":\"auto-ref to baseline state\"}"
  for i in $(seq 1 "$n"); do
    local name="${DOC_PREFIX}$(printf '%03d' "$i")"
    api POST "/projects/$PROJECT_ID/spaces/$SPACE_ID/documents/$name/linked-revisions" "$body" >/dev/null
    printf '.'
  done
  echo
  mark_done "step-linked-revisions"
}

# ----- collections -----
build_elements_json() {
  local rev="$1" from="$2" to="$3"
  local arr=""
  for i in $(seq "$from" "$to"); do
    local name="${DOC_PREFIX}$(printf '%03d' "$i")"
    arr+="{\"spaceId\":\"$SPACE_ID\",\"documentName\":\"$name\",\"revision\":\"$rev\"},"
  done
  printf '[%s]' "${arr%,}"
}

step_collections() {
  local rev_initial="$1" rev_final="$2"
  if done_marker "step-collections"; then
    log "step-collections: already done"
    return
  fi
  local n1=$DOC_COUNT n2=$DOC_COUNT
  [ "$n1" -gt 25 ] && n1=25
  [ "$n2" -gt 50 ] && n2=50
  log "Creating 2 collections ($n1 docs at initial baseline, $n2 docs at final baseline)..."
  local elem body
  elem="$(build_elements_json "$rev_initial" 1 "$n1")"
  body="{\"description\":\"first quarter at initial baseline\",\"elements\":$elem}"
  api POST "/projects/$PROJECT_ID/collections/collection-initial-q1" "$body" >/dev/null
  log "  collection-initial-q1 created ($n1 docs at initial baseline)"
  elem="$(build_elements_json "$rev_final" 1 "$n2")"
  body="{\"description\":\"first half at final baseline\",\"elements\":$elem}"
  api POST "/projects/$PROJECT_ID/collections/collection-final-h1" "$body" >/dev/null
  log "  collection-final-h1 created ($n2 docs at final baseline)"
  mark_done "step-collections"
}

# ----- main -----
log "API base: $API"
log "Project: $PROJECT_ID, space: $SPACE_ID, docs: $DOC_COUNT x $WI_PER_DOC WI, revision passes: $REVISION_PASSES"
log "State dir: $STATE_DIR"

timed() {
  # timed <label> <command...>
  local label="$1"; shift
  local start; start=$(now_ts)
  "$@"
  local rc=$?
  local elapsed=$(( $(now_ts) - start ))
  record_timing "$label" "$elapsed"
  log "  ⏱  $label took $(fmt_duration "$elapsed")"
  return $rc
}

timed "create_documents" step_create_documents
timed "cross_doc_links"  step_cross_doc_links
B1_START=$(now_ts); rev_initial="$(create_baseline "after-initial-creation" "Snapshot right after WI creation and cross-doc links")"; record_timing "baseline_initial" $(( $(now_ts) - B1_START ))

if [ "$REVISION_PASSES" -ge 1 ]; then timed "rev_pass_1" step_revision_pass 1; fi
B2_START=$(now_ts); rev_mid="$(create_baseline "mid-after-pass-1" "After first revision pass")"; record_timing "baseline_mid" $(( $(now_ts) - B2_START ))

for (( p=2; p<=REVISION_PASSES; p++ )); do
  timed "rev_pass_$p" step_revision_pass "$p"
done
B3_START=$(now_ts); rev_final="$(create_baseline "final" "After all revision passes")"; record_timing "baseline_final" $(( $(now_ts) - B3_START ))

timed "linked_revisions" step_linked_revisions "$rev_initial" "$rev_mid" "$rev_final"
timed "collections"      step_collections "$rev_initial" "$rev_final"

TOTAL=$(( $(now_ts) - SCRIPT_START_TS ))
log "DONE."
log "Summary: $DOC_COUNT docs, ~$((WI_PER_DOC * (1 + REVISION_PASSES))) revisions/doc, 3 baselines, 2 collections."
log "──── Timing breakdown ────"
for i in "${!TIMING_NAMES[@]}"; do
  log "  $(printf '%-22s' "${TIMING_NAMES[$i]}") $(fmt_duration "${TIMING_SECS[$i]}")"
done
log "  $(printf '%-22s' "TOTAL")                      $(fmt_duration "$TOTAL")"
