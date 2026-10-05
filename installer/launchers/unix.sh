#!/bin/sh
# All paths are relative to this bundle; no JAR association is required.
bundle_dir=$(CDPATH= cd -P "$(dirname "$0")" && pwd) || exit 1

finish() {
    result=$1
    if [ "$result" -ne 0 ] && [ -t 0 ]; then
        printf '\n按回车键关闭此窗口……'
        IFS= read -r ignored
    fi
    exit "$result"
}
fail() { printf '%s\n' "$1" >&2; finish 1; }

installer_mode=${1-}
case "$installer_mode" in ''|--check|--console) ;; *) fail '仅支持 --check 或 --console 参数。' ;; esac
# 按数字段比较汉化包版本；版本相等时才比较导出序列号。
compare_release() {
    awk -v left="$1" -v right="$2" -v left_serial="$3" -v right_serial="$4" '
        function number(a, b) {
            sub(/^0+/, "", a); sub(/^0+/, "", b)
            if (a == "") a = "0"; if (b == "") b = "0"
            if (length(a) != length(b)) return length(a) > length(b) ? 1 : -1
            if (("x" a) == ("x" b)) return 0
            return ("x" a) > ("x" b) ? 1 : -1
        }
        BEGIN {
            nl = split(left, l, "[.]"); nr = split(right, r, "[.]")
            for (i = 1; i <= nl || i <= nr; i++) {
                c = number(i <= nl ? l[i] : "0", i <= nr ? r[i] : "0")
                if (c != 0) { print c; exit }
            }
            print number(left_serial, right_serial); exit
        }'
}
jar_path=''; latest_version='0'; latest_serial='0'; duplicate_latest=0; jar_count=0; single_jar=''
for candidate in "$bundle_dir"/*.jar; do
    [ -f "$candidate" ] || continue
    jar_count=$((jar_count + 1)); single_jar=$candidate
    stem=${candidate##*/}; stem=${stem%.jar}
    case "$stem" in *-V*-*) ;; *) continue ;; esac
    serial=${stem##*-}; version=${stem%-*}; version=${version##*-V}
    case "$serial" in ''|0*|*[!0-9]*) continue ;; esac
    case "$version" in ''|.*|*.|*..*|*[!0-9.]*) continue ;; esac
    comparison=$(compare_release "$version" "$latest_version" "$serial" "$latest_serial") || fail '无法比较安装器版本，请确认系统可用 awk。'
    if [ -z "$jar_path" ] || [ "$comparison" -gt 0 ]; then
        jar_path=$candidate; latest_version=$version; latest_serial=$serial; duplicate_latest=0
    elif [ "$comparison" -eq 0 ]; then duplicate_latest=1
    fi
done
[ "$jar_count" -gt 0 ] || fail '同目录没有找到安装器 JAR。请将启动文件与安装器 JAR 放在一起。'
[ "$duplicate_latest" -eq 0 ] || fail '存在汉化包版本和导出序列号均相同的最新 JAR，请只保留需要启动的那个文件。'
if [ -z "$jar_path" ] && [ "$jar_count" -eq 1 ]; then jar_path=$single_jar; fi
[ -n "$jar_path" ] || fail '无法确定最新安装器。请保留文件名末尾的 -V汉化包版本-导出序列号.jar，或只保留一个 JAR。'
printf '自动启动：%s\n' "${jar_path##*/}"
set --
[ -z "$installer_mode" ] || set -- "$installer_mode"

java_cmd=''
try_java() {
    [ -n "$1" ] && [ -x "$1" ] || return 1
    version=$("$1" -version 2>&1) || return 1
    major=$(printf '%s\n' "$version" | sed -n '1{s/^[^0-9]*//;s/^1\.//;s/[^0-9].*$//;p;}')
    case "$major" in ''|*[!0-9]*) return 1 ;; esac
    [ "$major" -ge 17 ] || return 1
    java_cmd=$1
}
try_java "$bundle_dir/runtime/bin/java" ||
    try_java "${JAVA_HOME:+$JAVA_HOME/bin/java}" ||
    try_java "$(command -v java 2>/dev/null)" || :
if [ -z "$java_cmd" ] && [ -x /usr/libexec/java_home ]; then
    mac_java_home=$(/usr/libexec/java_home 2>/dev/null) || mac_java_home=''
    try_java "${mac_java_home:+$mac_java_home/bin/java}" || :
fi
[ -n "$java_cmd" ] || fail '未找到 Java 17 或更新版本。请安装适合本系统的 Java 17+，或将 JAVA_HOME 设置为已有 Java 的目录，然后重新运行。'
printf '%s\n' '正在启动汉化安装器，请稍候……'
"$java_cmd" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar "$jar_path" "$@"
result=$?
if [ "$result" -ne 0 ]; then
    printf '\n安装器启动或运行失败（退出码 %s）。请保留上方错误信息；无图形桌面时可以使用 --console 参数。\n' "$result" >&2
fi
finish "$result"
