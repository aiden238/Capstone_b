// Score Engine + Weight Config API 테스트
const BASE = 'http://localhost:80/api';

async function api(method, path, body, token) {
  const opts = { method, headers: {} };
  if (token) opts.headers['Authorization'] = `Bearer ${token}`;
  if (body) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(`${BASE}${path}`, opts);
  const text = await res.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }
  return { status: res.status, data };
}

let pass = 0, fail = 0;
function check(name, ok) {
  if (ok) { pass++; console.log(`  ✅ ${name}`); }
  else    { fail++; console.log(`  ❌ ${name}`); }
}

async function run() {
  const ts = Date.now();

  // --- 1. 회원 3명 가입 + 프로젝트 생성 ---
  console.log('\n=== Setup: 학생3명 + 교수1명 ===');

  const s1 = await api('POST', '/auth/signup', { name: `Score학생1_${ts}`, email: `s1_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const s2 = await api('POST', '/auth/signup', { name: `Score학생2_${ts}`, email: `s2_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const s3 = await api('POST', '/auth/signup', { name: `Score학생3_${ts}`, email: `s3_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const prof = await api('POST', '/auth/signup', { name: `Score교수_${ts}`, email: `prof_${ts}@test.com`, password: 'pass1234', role: 'PROFESSOR' });

  const login1 = await api('POST', '/auth/login', { email: `s1_${ts}@test.com`, password: 'pass1234' });
  const login2 = await api('POST', '/auth/login', { email: `s2_${ts}@test.com`, password: 'pass1234' });
  const login3 = await api('POST', '/auth/login', { email: `s3_${ts}@test.com`, password: 'pass1234' });
  const loginProf = await api('POST', '/auth/login', { email: `prof_${ts}@test.com`, password: 'pass1234' });

  const t1 = login1.data.data.accessToken;
  const t2 = login2.data.data.accessToken;
  const t3 = login3.data.data.accessToken;
  const tp = loginProf.data.data.accessToken;

  // 프로젝트 생성 (학생1 = LEADER)
  const proj = await api('POST', '/projects', { name: `ScoreTest_${ts}`, description: 'score test', courseName: 'CS101', semester: '2025-1' }, t1);
  const projectId = proj.data.data.id;
  const inviteCode = proj.data.data.inviteCode;

  // 학생2, 학생3, 교수 참여
  await api('POST', `/projects/join`, { inviteCode }, t2);
  await api('POST', `/projects/join`, { inviteCode }, t3);
  await api('POST', `/projects/join`, { inviteCode }, tp);

  console.log(`  프로젝트: ${projectId}`);

  // --- 2. 활동 기록 생성 (태스크 CRUD로) ---
  console.log('\n=== 활동 기록 생성 (Task 작업) ===');

  // 학생1: 태스크 3개 생성 + 1개 완료
  const task1 = await api('POST', `/projects/${projectId}/tasks`, { title: '분석', priority: 'HIGH', status: 'TODO' }, t1);
  const task2 = await api('POST', `/projects/${projectId}/tasks`, { title: '설계', priority: 'MEDIUM', status: 'TODO' }, t1);
  const task3 = await api('POST', `/projects/${projectId}/tasks`, { title: '구현', priority: 'LOW', status: 'TODO' }, t1);
  await api('PATCH', `/projects/${projectId}/tasks/${task1.data.data.id}/status`, { status: 'DONE' }, t1);

  // 학생2: 태스크 1개 생성
  const task4 = await api('POST', `/projects/${projectId}/tasks`, { title: '테스트', priority: 'MEDIUM', status: 'TODO' }, t2);

  // 학생3: 아무것도 안 함
  console.log('  학생1: task 3개 생성 + 1개 완료');
  console.log('  학생2: task 1개 생성');
  console.log('  학생3: 활동 없음');

  // 회의 생성 + 체크인 (학생1,2만)
  const meeting = await api('POST', `/projects/${projectId}/meetings`, {
    title: '스코어테스트회의',
    meetingDate: new Date().toISOString(),
    purpose: 'score test'
  }, t1);
  console.log('  Meeting response:', JSON.stringify(meeting.data).substring(0, 300));
  const meetingId = meeting.data.data.id;
  const checkinCode = meeting.data.data.checkinCode;

  // 학생1, 학생2 체크인
  await api('POST', `/projects/${projectId}/meetings/${meetingId}/checkin`, { checkinCode }, t1);
  await api('POST', `/projects/${projectId}/meetings/${meetingId}/checkin`, { checkinCode }, t2);

  console.log('  학생1: 회의 생성 + 체크인');
  console.log('  학생2: 체크인');

  // --- 3. 점수 조회 (자동 계산) ---
  console.log('\n=== Test 1: 전체 점수 조회 (자동 계산 트리거) ===');
  const scores = await api('GET', `/projects/${projectId}/scores`, null, t1);
  check('전체 점수 조회 200', scores.status === 200);
  check('점수 배열 반환', Array.isArray(scores.data.data));

  const scoreList = scores.data.data;
  console.log(`  팀원 수: ${scoreList.length}`);
  for (const s of scoreList) {
    console.log(`  ${s.userName}: task=${s.taskScore} meeting=${s.meetingScore} doc=${s.docScore} git=${s.gitScore} total=${s.totalScore}`);
  }

  // 학생1이 가장 높아야 함
  const s1Score = scoreList.find(s => s.email === `s1_${ts}@test.com`);
  const s3Score = scoreList.find(s => s.email === `s3_${ts}@test.com`);
  check('학생1 점수 > 0', s1Score && parseFloat(s1Score.totalScore) > 0);
  check('학생3 점수 = 0 (활동 없음)', s3Score && parseFloat(s3Score.totalScore) === 0);

  // --- 4. 내 점수 조회 ---
  console.log('\n=== Test 2: 내 점수 조회 ===');
  const myScore = await api('GET', `/projects/${projectId}/scores/me`, null, t2);
  check('내 점수 조회 200', myScore.status === 200);
  check('내 점수 데이터 존재', myScore.data.data && myScore.data.data.userId);
  console.log(`  학생2 점수: total=${myScore.data.data.totalScore}`);

  // --- 5. 가중치 조회 (기본값) ---
  console.log('\n=== Test 3: 가중치 조회 (기본값) ===');
  const weights = await api('GET', `/projects/${projectId}/weights`, null, t1);
  check('가중치 조회 200', weights.status === 200);
  check('기본 가중치 git=0.30', parseFloat(weights.data.data.weightGit) === 0.30);
  check('기본 가중치 task=0.25', parseFloat(weights.data.data.weightTask) === 0.25);

  // --- 6. 가중치 변경 (교수만) ---
  console.log('\n=== Test 4: 가중치 변경 (교수) ===');
  const newWeights = await api('PUT', `/projects/${projectId}/weights`, {
    weightGit: 0.10,
    weightDoc: 0.20,
    weightMeeting: 0.30,
    weightTask: 0.40
  }, tp);
  check('가중치 변경 200', newWeights.status === 200);
  check('변경된 가중치 task=0.40', parseFloat(newWeights.data.data.weightTask) === 0.40);

  // --- 7. 학생이 가중치 변경 시도 → 403 ---
  console.log('\n=== Test 5: 학생 가중치 변경 → 403 ===');
  const studentWeight = await api('PUT', `/projects/${projectId}/weights`, {
    weightGit: 0.25, weightDoc: 0.25, weightMeeting: 0.25, weightTask: 0.25
  }, t1);
  check('학생 가중치 변경 거부 403', studentWeight.status === 403);

  // --- 8. 잘못된 가중치 합 → 400 ---
  console.log('\n=== Test 6: 잘못된 가중치 합 → 400 ===');
  const badWeight = await api('PUT', `/projects/${projectId}/weights`, {
    weightGit: 0.50, weightDoc: 0.50, weightMeeting: 0.50, weightTask: 0.50
  }, tp);
  check('가중치 합 != 1.00 거부 400', badWeight.status === 400);

  // --- 9. 재계산 (가중치 변경 후) ---
  console.log('\n=== Test 7: 재계산 (변경된 가중치 반영) ===');
  const recalc = await api('POST', `/projects/${projectId}/scores/recalculate`, null, t1);
  check('재계산 200', recalc.status === 200);
  const recalcList = recalc.data.data;
  for (const s of recalcList) {
    console.log(`  ${s.userName}: task=${s.taskScore} meeting=${s.meetingScore} total=${s.totalScore}`);
  }
  // 가중치가 task 0.40으로 높아졌으므로 학생1의 total은 이전과 다를 수 있음
  const s1Recalc = recalcList.find(s => s.email === `s1_${ts}@test.com`);
  check('재계산된 점수 존재', s1Recalc && parseFloat(s1Recalc.totalScore) > 0);

  // --- 10. 비멤버 접근 거부 ---
  console.log('\n=== Test 8: 비멤버 접근 거부 ===');
  const outsider = await api('POST', '/auth/signup', { name: 'outsider', email: `out_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const loginOut = await api('POST', '/auth/login', { email: `out_${ts}@test.com`, password: 'pass1234' });
  const tout = loginOut.data.data.accessToken;
  const nonMember = await api('GET', `/projects/${projectId}/scores`, null, tout);
  check('비멤버 점수 조회 거부 403', nonMember.status === 403);

  // --- 결과 ---
  console.log(`\n=============================`);
  console.log(`  Score Engine: ${pass}/${pass+fail} tests passed`);
  console.log(`=============================\n`);
}

run().catch(e => console.error('Error:', e));
