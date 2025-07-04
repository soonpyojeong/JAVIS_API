#!/bin/bash

LOG_FILE="javis.log"
OLD_JAR_FILE="old_jar.file"
DATE_TAG=$(date "+%Y-%m-%d %H:%M:%S")
echo "[$DATE_TAG] ▶ 스크립트 실행 시작" >> "$LOG_FILE"

# 1. oracle 유저의 실행 중인 JAVIS 프로세스 종료
echo "[INFO] oracle 유저의 JAVIS 프로세스 종료 중..." | tee -a "$LOG_FILE"
pids=$(ps -fu oracle | grep "java -jar JAVIS" | grep -v grep | awk '{print $2}')
old_jar=$(ps -fu oracle | grep "java -jar JAVIS" | grep -v grep | awk '{print $10}')

# old_jar 기록
if [ -n "$old_jar" ]; then
  echo "$old_jar" > "$OLD_JAR_FILE"
  echo "[INFO] 현재 실행 중인 JAR: $old_jar → 기록 완료 ($OLD_JAR_FILE)" | tee -a "$LOG_FILE"
fi

if [ -n "$pids" ]; then
  echo "[INFO] 종료 대상 PID: $pids" | tee -a "$LOG_FILE"
  for pid in $pids; do
    /usr/bin/expect <<EOF
spawn kill -9 $pid
expect "Allow command Input?(y/n)"
send "y\r"
expect eof
EOF
    echo "[INFO] PID $pid 종료 완료" | tee -a "$LOG_FILE"
  done
else
  echo "[INFO] 종료할 프로세스 없음" | tee -a "$LOG_FILE"
fi

# 2. dongkukDBmon-1.0.1.jar 존재 여부
if [ -f "dongkukDBmon-1.0.1.jar" ]; then
  echo "[INFO] 새로운 JAR 버전 생성 시작..." | tee -a "$LOG_FILE"

  # 최신 JAR 버전 파악
  latest_jar=$(ls JAVIS-*.jar 2>/dev/null | sort -V | tail -n 1)
  if [ -z "$latest_jar" ]; then
    version="1.0.0"
  else
    version=$(echo "$latest_jar" | sed -n 's/^JAVIS-\(.*\)\.jar$/\1/p')
  fi

  IFS='.' read -r major minor patch <<< "$version"
  new_patch=$((patch + 1))
  new_version="${major}.${minor}.${new_patch}"
  new_jar="JAVIS-${new_version}.jar"

  # JAR 파일 이동
  echo "[INFO] 새 JAR 파일명: $new_jar" | tee -a "$LOG_FILE"
  mv dongkukDBmon-1.0.1.jar "$new_jar"
  echo "$new_jar" > "$OLD_JAR_FILE"

  # JAVA 실행
  export JAVA_HOME=/u01/app/oracle/JAVIS/jdk-17
  export PATH=$JAVA_HOME/bin:$PATH
  echo "[INFO] $new_jar 실행 중..." | tee -a "$LOG_FILE"
  nohup java -jar "$new_jar" --server.port=8813 >> "$LOG_FILE" 2>&1 &
  echo "[INFO] 새로운 JAR($new_jar) 백그라운드 실행 완료" | tee -a "$LOG_FILE"

else
  echo "[INFO] 빌드 파일(dongkukDBmon-1.0.1.jar)이 존재하지 않습니다." | tee -a "$LOG_FILE"

  # old_jar.file 에서 실행
  if [ -f "$OLD_JAR_FILE" ]; then
    jar_from_file=$(cat "$OLD_JAR_FILE")
    if [ -f "$jar_from_file" ]; then
      echo "[INFO] 이전 JAR($jar_from_file) 재실행..." | tee -a "$LOG_FILE"
      export JAVA_HOME=/u01/app/oracle/JAVIS/jdk-17
      export PATH=$JAVA_HOME/bin:$PATH
      nohup java -jar "$jar_from_file" --server.port=8813 >> "$LOG_FILE" 2>&1 &
      echo "[INFO] 백그라운드 실행 완료" | tee -a "$LOG_FILE"
    else
      echo "[ERROR] 기록된 JAR 파일($jar_from_file)이 존재하지 않습니다. 수동 확인 필요." | tee -a "$LOG_FILE"
    fi
  else
    echo "[ERROR] old_jar.file이 존재하지 않습니다. 수동 확인 필요." | tee -a "$LOG_FILE"
  fi
fi

DATE_END=$(date "+%Y-%m-%d %H:%M:%S")
JAVIS_PROCESS=$(ps -fu oracle | grep "java -jar JAVIS" | grep -v grep)
echo $JAVIS_PROCESS  >> "$LOG_FILE"
echo "[$DATE_END] ▶ 스크립트 종료" >> "$LOG_FILE"