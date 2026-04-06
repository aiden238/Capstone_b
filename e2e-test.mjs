// e2e-test.mjs — 전체 통합 E2E 테스트 (프론트-백 전체 플로우)
const BASE = 'http://localhost:80/api';
let passed = 0, failed = 0;

async function test(name, fn) {
  try {
    await fn();
    passed++;
    console.log(`  ✅ ${name}`);
  } catch (e) {
    failed++;
    console.log(`  ❌ ${name}: ${e.message}`);
  }
}
function assert(cond, msg) { if (!cond) throw new Error(msg); }

async function post(path, body, token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${BASE}${path}`, { method: 'POST', headers: h, body: JSON.stringify(body) });
  return { status: r.status, data: await r.json() };
}
async function get(path, token) {
  const h = {};
  if (token) h['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${BASE}${path}`, { headers: h });
  return { status: r.status, data: await r.json() };
}
async function patch(path, body, token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${BASE}${path}`, { method: 'PATCH', headers: h, body: JSON.stringify(body) });
  return { status: r.status, data: await r.json() };
}

const ts = Date.now();
console.log('=== E2E 통합 테스트 시작 ===\n');

// ────── 1. 회원가입 & 로그인 ──────
console.log('── 1. 인증 플로우 ──');
const profEmail = `e2e_prof_${ts}@test.com`;
const stu1Email = `e2e_s1_${ts}@test.com`;
const stu2Email = `e2e_s2_${ts}@test.com`;

await post('/auth/signup', { email: profEmail, password: 'Pass1234!', name: 'E2E교수', role: 'PROFESSOR' });
await post('/auth/signup', { email: stu1Email, password: 'Pass1234!', name: 'E2E학생1', role: 'STUDENT' });
await post('/auth/signup', { email: stu2Email, password: 'Pass1234!', name: 'E2E학생2', role: 'STUDENT' });

const profToken = (await post('/auth/login', { email: profEmail, password: 'Pass1234!' })).data.data.accessToken;
const s1Token = (await post('/auth/login', { email: stu1Email, password: 'Pass1234!' })).data.data.accessToken;
const s2Token = (await post('/auth/login', { email: stu2Email, password: 'Pass1234!' })).data.data.accessToken;

await test('교수 로그인 성공', async () => {
  const me = await get('/auth/me', profToken);
  assert(me.status === 200, `expected 200, got ${me.status}`);
  assert(me.data.data.role === 'PROFESSOR', 'not professor');
});

// ────── 2. 프로젝트 생성 & 초대 ──────
console.log('\n── 2. 프로젝트 & 멤버 ──');
const proj = await post('/projects', {
  name: 'E2E 프로젝트', description: '통합 테스트용', courseName: '캡스톤', semester: '2026-1'
}, s1Token);
const projectId = proj.data.data.id;
const inviteCode = proj.data.data.inviteCode;

await test('프로젝트 생성 201', () => assert(proj.status === 201));

const joinS2 = await post('/projects/join', { inviteCode }, s2Token);
await test('학생2 초대코드 참여', () => assert(joinS2.status === 201));

const joinProf = await post('/projects/join', { inviteCode }, profToken);
await test('교수 프로젝트 참여', () => assert(joinProf.status === 201));

// ────── 3. 동의 플로우 (새 기능) ──────
console.log('\n── 3. 데이터 수집 동의 ──');
const membersRes = await get(`/projects/${projectId}/members`, s1Token);
const s1Member = membersRes.data.data.find(m => m.name === 'E2E학생1');

await test('동의 전 consentPlatform = false', () => {
  assert(s1Member.consentPlatform === false, 'should be false');
  assert(s1Member.consentedAt === null, 'should be null');
});

const consentRes = await patch(`/projects/${projectId}/members/me/consent`, { consentPlatform: true }, s1Token);
await test('동의 API 200', () => assert(consentRes.status === 200));
await test('동의 후 consentPlatform = true', () => {
  assert(consentRes.data.data.consentPlatform === true, 'should be true');
  assert(consentRes.data.data.consentedAt !== null, 'should have timestamp');
});

// ────── 4. 태스크 관리 (칸반) ──────
console.log('\n── 4. 태스크 (칸반) ──');
const task1 = await post(`/projects/${projectId}/tasks`, {
  title: 'UI 구현', priority: 'HIGH', tag: '기능'
}, s1Token);
await test('태스크1 생성', () => assert(task1.status === 201));

const task2 = await post(`/projects/${projectId}/tasks`, {
  title: 'API 연동', priority: 'MEDIUM', tag: '연동'
}, s1Token);
const task3 = await post(`/projects/${projectId}/tasks`, {
  title: '테스트 작성', priority: 'LOW', tag: '기능'
}, s2Token);

await test('3개 태스크 생성 성공', async () => {
  const all = await get(`/projects/${projectId}/tasks`, s1Token);
  assert(all.data.data.length === 3, `expected 3, got ${all.data.data.length}`);
});

// 상태 변경
const t1Id = task1.data.data.id;
await patch(`/projects/${projectId}/tasks/${t1Id}/status`, { status: 'IN_PROGRESS' }, s1Token);
await patch(`/projects/${projectId}/tasks/${t1Id}/status`, { status: 'DONE' }, s1Token);
await test('태스크 상태 TODO→DONE', async () => {
  const all = await get(`/projects/${projectId}/tasks`, s1Token);
  const t = all.data.data.find(t => t.id === t1Id);
  assert(t.status === 'DONE', `expected DONE, got ${t.status}`);
});

// ────── 5. 회의록 & 체크인 ──────
console.log('\n── 5. 회의록 ──');
const meeting = await post(`/projects/${projectId}/meetings`, {
  title: 'E2E 회의', meetingDate: new Date().toISOString(), purpose: '통합 테스트'
}, s1Token);
await test('회의 생성', () => assert(meeting.status === 201));

const meetingId = meeting.data.data.id;
const checkinCode = meeting.data.data.checkinCode;

const ci1 = await post(`/projects/${projectId}/meetings/${meetingId}/checkin`, { checkinCode }, s1Token);
const ci2 = await post(`/projects/${projectId}/meetings/${meetingId}/checkin`, { checkinCode }, s2Token);
await test('체크인 성공 (2명)', () => {
  assert(ci1.status === 200, 'student1 checkin failed');
  assert(ci2.status === 200, 'student2 checkin failed');
});

// 결정사항 저장
await patch(`/projects/${projectId}/meetings/${meetingId}`, {
  notes: '테스트 성공 확인', decisions: 'API 리팩터링\nUI 개선'
}, s1Token);
await test('회의록 수정 (notes+decisions)', async () => {
  const detail = await get(`/projects/${projectId}/meetings/${meetingId}`, s1Token);
  assert(detail.data.data.decisions === 'API 리팩터링\nUI 개선', 'decisions mismatch');
});

// ────── 6. 파일 업로드 (Hash Vault) ──────
console.log('\n── 6. Hash Vault ──');
const fileContent = `E2E test file ${ts}`;
const blob = new Blob([fileContent], { type: 'text/plain' });
const form = new FormData();
form.append('file', blob, 'e2e-test.txt');

const uploadRes = await fetch(`${BASE}/projects/${projectId}/files`, {
  method: 'POST', headers: { 'Authorization': `Bearer ${s1Token}` }, body: form,
});
const uploadData = await uploadRes.json();
await test('파일 업로드 201 + SHA-256 해시', () => {
  assert(uploadRes.status === 201, `status: ${uploadRes.status}`);
  assert(uploadData.data.fileHash.length === 64, 'hash should be 64 chars');
  assert(uploadData.data.version === 1, 'version should be 1');
});

// ────── 7. Score Engine ──────
console.log('\n── 7. 기여도 점수 ──');
const scores = await get(`/projects/${projectId}/scores`, s1Token);
await test('점수 조회 200', () => assert(scores.status === 200));
await test('학생1 점수 > 0 (활동 있음)', () => {
  const s1 = scores.data.data.find(s => s.userName === 'E2E학생1');
  assert(s1 && s1.totalScore > 0, `score: ${s1?.totalScore}`);
});

// 가중치
const weights = await get(`/projects/${projectId}/weights`, profToken);
await test('기본 가중치 조회', () => assert(weights.status === 200));

const wUpdate = await fetch(`${BASE}/projects/${projectId}/weights`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${profToken}` },
  body: JSON.stringify({ weightTask: 0.40, weightMeeting: 0.30, weightDoc: 0.15, weightGit: 0.15 })
});
await test('교수 가중치 변경', () => assert(wUpdate.status === 200));

// 재계산
const recalc = await post(`/projects/${projectId}/scores/recalculate`, {}, s1Token);
await test('점수 재계산', () => assert(recalc.status === 200));

// ────── 8. 경보 시스템 ──────
console.log('\n── 8. 경보 ──');
const detect = await post(`/projects/${projectId}/alerts/detect`, {}, s1Token);
await test('경보 감지 실행', () => assert(detect.status === 200));

const alerts = await get(`/projects/${projectId}/alerts`, s1Token);
await test('경보 목록 조회', () => {
  assert(alerts.status === 200);
  assert(Array.isArray(alerts.data.data));
});

if (alerts.data.data.length > 0) {
  const firstAlert = alerts.data.data[0];
  const markRes = await patch(`/projects/${projectId}/alerts/${firstAlert.id}/read`, {}, s1Token);
  await test('경보 읽음 처리', () => assert(markRes.status === 200));
}

// ────── 9. 교수 대시보드 ──────
console.log('\n── 9. 교수 대시보드 ──');
const overview = await get('/dashboard/overview', profToken);
await test('교수 대시보드 overview 200', () => assert(overview.status === 200));
await test('프로젝트 요약 존재', () => {
  const proj = overview.data.data.find(p => p.projectName === 'E2E 프로젝트');
  assert(proj, 'project not found in overview');
  assert(proj.memberCount >= 3, `members: ${proj.memberCount}`);
  assert(proj.taskTotal === 3, `tasks: ${proj.taskTotal}`);
  assert(['HEALTHY','WARNING','DANGER'].includes(proj.healthStatus), `health: ${proj.healthStatus}`);
});

// ────── 10. 프론트엔드 접근성 ──────
console.log('\n── 10. 프론트엔드 ──');
const frontRes = await fetch('http://localhost/');
await test('프론트엔드 메인 접근 가능', () => {
  assert([200, 307, 308].includes(frontRes.status), `status: ${frontRes.status}`);
});

const loginPage = await fetch('http://localhost/login');
await test('로그인 페이지 접근 가능', () => {
  assert(loginPage.status === 200, `status: ${loginPage.status}`);
});

// ────── Summary ──────
console.log(`\n${'='.repeat(40)}`);
console.log(`  E2E 통합 테스트 결과: ${passed}/${passed + failed} passed`);
if (failed > 0) console.log(`  ❌ ${failed} failed`);
else console.log('  🎉 모든 테스트 통과!');
console.log('='.repeat(40));
