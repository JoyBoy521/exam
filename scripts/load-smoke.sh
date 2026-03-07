#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-123456}"
STUDENT_USERNAME="${STUDENT_USERNAME:-20260001}"
STUDENT_PASSWORD="${STUDENT_PASSWORD:-123456}"
ROUNDS="${ROUNDS:-20}"
CONCURRENCY="${CONCURRENCY:-1}"

login_resp="$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"
token="$(echo "${login_resp}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"

if [[ -z "${token}" ]]; then
  echo "登录失败，无法获取 token"
  echo "${login_resp}"
  exit 1
fi

hit_api() {
  local name="$1"
  local path="$2"
  local auth_header="$3"
  local i=0
  local total=0
  local ok=0
  while [[ "${i}" -lt "${ROUNDS}" ]]; do
    local start end cost code
    start="$(date +%s%3N)"
    code="$(curl -s -o /tmp/load_smoke.out -w "%{http_code}" \
      -H "${auth_header}" \
      "${BASE_URL}${path}")"
    end="$(date +%s%3N)"
    cost="$((end - start))"
    total="$((total + cost))"
    if [[ "${code}" == "200" ]]; then
      ok="$((ok + 1))"
    fi
    i="$((i + 1))"
  done
  local avg=0
  if [[ "${ROUNDS}" -gt 0 ]]; then
    avg="$((total / ROUNDS))"
  fi
  echo "${name}: rounds=${ROUNDS}, ok=${ok}, avg=${avg}ms"
}

run_group() {
  local name="$1"
  local path="$2"
  local auth_header="$3"
  local pids=()
  local idx=0
  while [[ "${idx}" -lt "${CONCURRENCY}" ]]; do
    hit_api "${name}#${idx}" "${path}" "${auth_header}" &
    pids+=($!)
    idx=$((idx + 1))
  done
  for pid in "${pids[@]}"; do
    wait "${pid}"
  done
}

student_login_resp="$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${STUDENT_USERNAME}\",\"password\":\"${STUDENT_PASSWORD}\"}")"
student_token="$(echo "${student_login_resp}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
if [[ -z "${student_token}" ]]; then
  echo "学生登录失败，无法获取 token"
  echo "${student_login_resp}"
  exit 1
fi

echo "BASE_URL=${BASE_URL}, ROUNDS=${ROUNDS}, CONCURRENCY=${CONCURRENCY}"
echo "=== Teacher APIs ==="
run_group "teacher/exams" "/api/teacher/exams" "Authorization: Bearer ${token}"
run_group "teacher/statistics" "/api/teacher/statistics" "Authorization: Bearer ${token}"
run_group "teacher/todo" "/api/teacher/todo" "Authorization: Bearer ${token}"
run_group "teacher/makeup-requests" "/api/teacher/makeup-requests?page=1&size=10" "Authorization: Bearer ${token}"

echo "=== Student APIs ==="
run_group "student/exams/list" "/api/student/exams/list" "Authorization: Bearer ${student_token}"
run_group "student/my-records" "/api/student/exams/my-records?page=1&size=10" "Authorization: Bearer ${student_token}"
run_group "student/wrong-book" "/api/student/wrong-book/list?page=1&size=10" "Authorization: Bearer ${student_token}"
