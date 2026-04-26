#!/usr/bin/env bash

load_env_defaults() {
  local env_file="$1"
  shift || true

  if [[ ! -f "$env_file" ]]; then
    return 0
  fi

  local -a preserved_names=()
  local -a preserved_values=()
  local name

  for name in "$@"; do
    if [[ "${!name+x}" == x ]]; then
      preserved_names+=("$name")
      preserved_values+=("${!name}")
    fi
  done

  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a

  local i
  for i in "${!preserved_names[@]}"; do
    printf -v "${preserved_names[$i]}" '%s' "${preserved_values[$i]}"
    export "${preserved_names[$i]}"
  done
}
