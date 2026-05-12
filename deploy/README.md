# deploy/

RankingServer를 OCI(또는 다른 Ubuntu 서버)에 **systemd 서비스로 등록**하기 위한 자료.

## 왜 systemd로 바꿨나
이전 운영 방식은 SSH 세션에서 `nohup java -jar ...`로 띄우고 세션을 끊은 후 `session-NNNN.scope`(abandoned)에 매달려 살아가는 구조였습니다. 이 방식의 약점:

- **인스턴스 재부팅 시 자동 시작 안 됨** (OCI 호스트 패치·Ubuntu 보안 업데이트로 재부팅이 가끔 발생).
- **Java 프로세스가 OOM 등으로 죽으면 다운된 채로 방치**.
- **DB 비밀번호가 `~/.bashrc`에 평문**으로 박혀 있어 백업·스크린 공유 시 노출 위험.
- **로그가 `~/nohup.out`, `~/server_log.txt`로 분산**되어 표준 위치 없음.

systemd로 옮기면 위 네 가지가 한 번에 정리됩니다.

## 구성 파일
- [`ranking-server.service`](./ranking-server.service) — systemd unit 파일
- [`ranking-server.env.example`](./ranking-server.env.example) — `/etc/ranking-server.env` 템플릿. 실제 비밀값은 placeholder

## 설치 (서버에 SSH 접속 후 1회)

> 사전 가정: `~/RankingServer-0.0.1-SNAPSHOT.jar` 가 이미 최신본으로 올라가 있고, MySQL이 같은 호스트에서 동작 중.

```bash
# 1) 운영 비밀 파일 생성 (root:root, 권한 600)
sudo install -m 600 -o root -g root /dev/null /etc/ranking-server.env
sudo tee /etc/ranking-server.env >/dev/null <<'EOF'
DB_URL=jdbc:mysql://localhost:3306/ranking_game?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=ranking_app
DB_PASSWORD=__YOUR_DB_PASSWORD__
RANKING_SECRET=__YOUR_RANKING_SECRET__
EOF
sudo ls -l /etc/ranking-server.env   # → -rw------- 1 root root ...

# 2) Java 17 절대 경로 확인 (unit 파일의 ExecStart와 다르면 수정)
which java
# 보통 /usr/bin/java. 다르면 sudo nano ranking-server.service 로 ExecStart 수정.

# 3) unit 파일을 시스템 위치로 복사 + 로드
sudo cp ~/RankingServer/deploy/ranking-server.service /etc/systemd/system/ranking-server.service
# (또는 PR을 clone 안 했다면 raw url 로:
#   sudo curl -fsSL https://raw.githubusercontent.com/romantic02/RankingServer/main/deploy/ranking-server.service \
#     -o /etc/systemd/system/ranking-server.service )
sudo systemctl daemon-reload

# 4) 기존 nohup 프로세스 정리
OLD_PID=$(pgrep -f 'RankingServer-0.0.1-SNAPSHOT.jar' || true)
if [ -n "$OLD_PID" ]; then
  echo "killing nohup PID $OLD_PID"
  kill "$OLD_PID"
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    kill -0 "$OLD_PID" 2>/dev/null && sleep 1 || break
  done
  kill -0 "$OLD_PID" 2>/dev/null && kill -9 "$OLD_PID" || true
fi

# 5) 서비스 활성화 + 시작
sudo systemctl enable --now ranking-server
sleep 10
sudo systemctl status ranking-server --no-pager | head -20

# 6) ~/.bashrc 에 박혀 있던 평문 비번 정리
#    (이번에 systemd EnvironmentFile 로 옮겼으니 더 이상 .bashrc 에 둘 이유 없음)
sed -i '/# --- RankingServer environment ---/,/^export RANKING_SECRET=/d' ~/.bashrc
grep -c 'RankingServer environment' ~/.bashrc   # → 0 이면 정리 완료
```

## 검증
```bash
# (a) 서비스 상태 - active (running)
sudo systemctl status ranking-server --no-pager | head -15

# (b) 헬스체크 - 200/"ok" 및 detailed JSON
curl -sS -i http://localhost:8080/api/health
echo
curl -sS http://localhost:8080/api/health/detailed
echo

# (c) 외부 HTTPS (역방향 프록시 통과)
curl -sS https://pd-ranking.duckdns.org/api/health/detailed

# (d) 실시간 로그 모니터링
sudo journalctl -u ranking-server -f --since "5 minutes ago"

# (e) 재부팅 시뮬레이션 (선택, 운영에 영향 있음)
sudo systemctl restart ranking-server
sleep 30
sudo systemctl status ranking-server --no-pager | head -10
```

## 운영 명령
```bash
# 재기동
sudo systemctl restart ranking-server

# 중지/시작
sudo systemctl stop ranking-server
sudo systemctl start ranking-server

# 부팅 시 자동 시작 비활성화
sudo systemctl disable ranking-server

# 로그 보기 (최근)
sudo journalctl -u ranking-server -n 200 --no-pager

# 로그 실시간
sudo journalctl -u ranking-server -f

# 비밀번호 변경 시 (EnvironmentFile만 수정 후 재기동)
sudo nano /etc/ranking-server.env
sudo systemctl restart ranking-server
```

## 새 jar 배포 절차 (앞으로 이렇게)
```bash
# 로컬에서 빌드 후 SCP 업로드 (현재 방식 그대로)
scp -i <KEY> build/libs/RankingServer-0.0.1-SNAPSHOT.jar \
    ubuntu@<HOST>:~/RankingServer-0.0.1-SNAPSHOT.jar.new

# 서버에서:
mv ~/RankingServer-0.0.1-SNAPSHOT.jar ~/RankingServer-0.0.1-SNAPSHOT.jar.bak.$(date +%Y%m%d_%H%M%S)
mv ~/RankingServer-0.0.1-SNAPSHOT.jar.new ~/RankingServer-0.0.1-SNAPSHOT.jar
sudo systemctl restart ranking-server
sudo journalctl -u ranking-server -f
```

## 롤백 (문제가 생겼을 때)
```bash
# 1) systemd 비활성화 후 옛 jar 로 되돌리기
sudo systemctl disable --now ranking-server

# 2) 백업 jar 로 복귀
ls -lt ~/RankingServer-0.0.1-SNAPSHOT.jar.bak.* | head -3
cp ~/RankingServer-0.0.1-SNAPSHOT.jar.bak.<YYYYMMDD_HHMMSS> ~/RankingServer-0.0.1-SNAPSHOT.jar

# 3) 임시로 nohup 으로 띄우려면 환경변수 다시 export 필요
#    (.bashrc 에서 지웠으니 EnvironmentFile 내용 참고)
set -a; source /etc/ranking-server.env; set +a
nohup java -jar ~/RankingServer-0.0.1-SNAPSHOT.jar > ~/nohup.out 2>&1 & disown
```

## 보안 주의
- `/etc/ranking-server.env` 는 **반드시 권한 600 / root:root** 로 유지.
- 비밀번호·secret을 git에 절대 커밋하지 말 것. 본 디렉토리의 `*.env.example` 은 placeholder만 포함.
- `RANKING_SECRET` 이나 DB 비번을 **변경하면 application 단순 재기동만으로 즉시 적용** 되지만, 클라이언트(Project-Drop)의 `ServerConfig.asset.secretKey` 와 짝이 맞아야 점수 등록이 통과한다는 점에 유의.
