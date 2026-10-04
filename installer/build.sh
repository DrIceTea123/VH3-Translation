#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
java_cmd=java
if [ -n "${JAVA_HOME:-}" ]; then java_cmd="$JAVA_HOME/bin/java"; fi
if [ "${1:-}" = "--check" ]; then
    exec "$java_cmd" -Dfile.encoding=UTF-8 "$project_dir/tools/Build.java" --project "$project_dir"
fi
exec "$java_cmd" -Dfile.encoding=UTF-8 "$project_dir/tools/Build.java" --project "$project_dir" --release
