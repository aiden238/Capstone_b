// Alert System API 테스트
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

  // --- 1. Setup ---
  console.log('\n=== Setup ===');
  const s1 = await api('POST', '/auth/signup', { name: `Alert학생1_${ts}`, email: `a1_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const s2 = await api('POST', '/auth/signup', { name: `Alert학생2_${ts}`, email: `a2_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const s3 = await api('POST', '/auth/signup', { name: `Alert학생3_${ts}`, email: `a3_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });

  const l1 = await api('POST', '/auth/login', { email: `a1_${ts}@test.com`, password: 'pass1234' });
  const l2 = await api('POST', '/auth/login', { email: `a2_${ts}@test.com`, password: 'pass1234' });
  const l3 = await api('POST', '/auth/login', { email: `a3_${ts}@test.com`, password: 'pass1234' });

  const t1 = l1.data.data.accessToken;
  const t2 = l2.data.data.accessToken;
  const t3 = l3.data.data.accessToken;

  // 프로젝트 생성
  const proj = await api('POST', '/projects', { name: `AlertTest_${ts}`, description: 'alert test', courseName: 'CS101', semester: '2025-1' }, t1);
  const projectId = proj.data.data.id;
  const inviteCode = proj.data.data.inviteCode;

  await api('POST', '/projects/join', { inviteCode }, t2);
  await api('POST', '/projects/join', { inviteCode }, t3);

  console.log(`  프로젝트: ${projectId}`);

  // 학생1만 활동 (불균형 유발)
  for (let i = 0; i < 8; i++) {
    await api('POST', `/projects/${projectId}/tasks`, { title: `task${i}`, priority: 'HIGH', status: 'TODO' }, t1);
  }
  // 학생2: 약간의 활동
  await api('POST', `/projects/${projectId}/tasks`, { title: 'task_s2', priority: 'LOW', status: 'TODO' }, t2);
  // 학생3: 활동 없음

  // 점수 계산 먼저
  await api('POST', `/projects/${projectId}/scores/recalculate`, null, t1);
  console.log('  활동 및 점수 계산 완료');

  // --- 2. 경보 감지 실행 ---
  console.log('\n=== Test 1: 경보 감지 실행 ===');
  const detect = await api('POST', `/projects/${projectId}/alerts/detect`, null, t1);
  check('경보 감지 200', detect.status === 200);
  check('경보 배열 반환', Array.isArray(detect.data.data));
  console.log(`  감지된 경보 수: ${detect.data.data.length}`);
  for (const a of detect.data.data) {
    console.log(`    [${a.alertType}/${a.severity}] ${a.message}`);
  }

  // OVERLOAD 경보가 있어야 함 (학생1이 8/9 = 88.9% 활동)
  const hasOverload = detect.data.data.some(a => a.alertType === 'OVERLOAD');
  check('OVERLOAD 경보 감지됨', hasOverload);

  // FREE_RIDE 경보 (학생3 기여도 0)
  const hasFreeRide = detect.data.data.some(a => a.alertType === 'FREE_RIDE');
  check('FREE_RIDE 경보 감지됨', hasFreeRide);

  // --- 3. 전체 경보 조회 ---
  console.log('\n=== Test 2: 전체 경보 조회 ===');
  const allAlerts = await api('GET', `/projects/${projectId}/alerts`, null, t1);
  check('전체 경보 조회 200', allAlerts.status === 200);
  check('경보 목록 존재', allAlerts.data.data.length > 0);

  // --- 4. 읽지 않은 경보 조회 ---
  console.log('\n=== Test 3: 읽지 않은 경보 조회 ===');
  const unread = await api('GET', `/projects/${projectId}/alerts/unread`, null, t1);
  check('읽지 않은 경보 조회 200', unread.status === 200);
  check('읽지 않은 경보 존재', unread.data.data.length > 0);

  // --- 5. 읽지 않은 경보 카운트 ---
  console.log('\n=== Test 4: 읽지 않은 경보 카운트 ===');
  const count = await api('GET', `/projects/${projectId}/alerts/unread/count`, null, t1);
  check('카운트 조회 200', count.status === 200);
  check('카운트 > 0', count.data.data.count > 0);
  console.log(`  읽지 않은 경보: ${count.data.data.count}개`);

  // --- 6. 경보 읽음 처리 ---
  console.log('\n=== Test 5: 경보 읽음 처리 ===');
  const firstAlertId = allAlerts.data.data[0].id;
  const markRead = await api('PATCH', `/projects/${projectId}/alerts/${firstAlertId}/read`, null, t1);
  check('읽음 처리 200', markRead.status === 200);

  // 카운트 감소 확인
  const newCount = await api('GET', `/projects/${projectId}/alerts/unread/count`, null, t1);
  check('읽음 후 카운트 감소', newCount.data.data.count < count.data.data.count);

  // --- 7. 전체 읽음 처리 ---
  console.log('\n=== Test 6: 전체 읽음 처리 ===');
  const readAll = await api('PATCH', `/projects/${projectId}/alerts/read-all`, null, t1);
  check('전체 읽음 200', readAll.status === 200);

  const zeroCount = await api('GET', `/projects/${projectId}/alerts/unread/count`, null, t1);
  check('전체 읽음 후 카운트 = 0', zeroCount.data.data.count === 0);

  // --- 8. 비멤버 접근 거부 ---
  console.log('\n=== Test 7: 비멤버 접근 거부 ===');
  const out = await api('POST', '/auth/signup', { name: 'outsider', email: `out2_${ts}@test.com`, password: 'pass1234', role: 'STUDENT' });
  const lout = await api('POST', '/auth/login', { email: `out2_${ts}@test.com`, password: 'pass1234' });
  const tout = lout.data.data.accessToken;
  const nonMember = await api('GET', `/projects/${projectId}/alerts`, null, tout);
  check('비멤버 경보 조회 거부 403', nonMember.status === 403);

  // --- 결과 ---
  console.log(`\n=============================`);
  console.log(`  Alert System: ${pass}/${pass+fail} tests passed`);
  console.log(`=============================\n`);
}

run().catch(e => console.error('Error:', e));
