// overview-test.mjs — 교수 대시보드 Overview API 테스트
const BASE = 'http://localhost:80/api';
let passed = 0, failed = 0;

async function test(name, fn) {
  try {
    await fn();
    passed++;
    console.log(`✅ ${name}`);
  } catch (e) {
    failed++;
    console.log(`❌ ${name}: ${e.message}`);
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

const ts = Date.now();

// === Setup ===
// 교수 계정 생성
const profEmail = `prof_ov_${ts}@test.com`;
const profSignup = await post('/auth/signup', { email: profEmail, password: 'Pass1234!', name: '김교수', role: 'PROFESSOR' });
const profLogin = await post('/auth/login', { email: profEmail, password: 'Pass1234!' });
const profToken = profLogin.data.data.accessToken;

// 학생 3명 생성
const students = [];
for (let i = 1; i <= 3; i++) {
  const email = `stu_ov_${ts}_${i}@test.com`;
  await post('/auth/signup', { email, password: 'Pass1234!', name: `학생${i}`, role: 'STUDENT' });
  const login = await post('/auth/login', { email, password: 'Pass1234!' });
  students.push({ email, token: login.data.data.accessToken });
}

// 프로젝트 2개 생성 (학생1이 생성, 교수/학생들 참여)
const proj1 = await post('/projects', { name: '프로젝트A', description: '테스트A', courseName: 'CS101', semester: '2026-1' }, students[0].token);
const proj1Id = proj1.data.data.id;
const invite1 = proj1.data.data.inviteCode;

const proj2 = await post('/projects', { name: '프로젝트B', description: '테스트B', courseName: 'CS202', semester: '2026-1' }, students[0].token);
const proj2Id = proj2.data.data.id;
const invite2 = proj2.data.data.inviteCode;

// 교수가 두 프로젝트에 참여
await post('/projects/join', { inviteCode: invite1 }, profToken);
await post('/projects/join', { inviteCode: invite2 }, profToken);

// 학생2, 학생3도 프로젝트1에 참여
await post('/projects/join', { inviteCode: invite1 }, students[1].token);
await post('/projects/join', { inviteCode: invite1 }, students[2].token);

// 프로젝트1에 태스크 생성
for (let i = 0; i < 5; i++) {
  await post(`/projects/${proj1Id}/tasks`, { title: `태스크${i+1}`, priority: 'MEDIUM' }, students[0].token);
}

// === Tests ===

await test('GET /dashboard/overview → 200', async () => {
  const r = await get('/dashboard/overview', profToken);
  assert(r.status === 200, `status: ${r.status}`);
  assert(r.data.success === true, 'not success');
});

await test('overview returns array', async () => {
  const r = await get('/dashboard/overview', profToken);
  assert(Array.isArray(r.data.data), 'data is not array');
});

await test('overview has 2 projects', async () => {
  const r = await get('/dashboard/overview', profToken);
  assert(r.data.data.length === 2, `expected 2, got ${r.data.data.length}`);
});

await test('project A has correct info', async () => {
  const r = await get('/dashboard/overview', profToken);
  const projA = r.data.data.find(p => p.projectId === proj1Id);
  assert(projA, 'projA not found');
  assert(projA.projectName === '프로젝트A', `name: ${projA.projectName}`);
  assert(projA.courseName === 'CS101', `course: ${projA.courseName}`);
  assert(projA.memberCount === 4, `members: ${projA.memberCount}`); // 3 students + 1 professor
});

await test('project A has 5 tasks', async () => {
  const r = await get('/dashboard/overview', profToken);
  const projA = r.data.data.find(p => p.projectId === proj1Id);
  assert(projA.taskTotal === 5, `tasks: ${projA.taskTotal}`);
  assert(projA.taskTodo === 5, `todo: ${projA.taskTodo}`);
});

await test('project B has fewer members', async () => {
  const r = await get('/dashboard/overview', profToken);
  const projB = r.data.data.find(p => p.projectId === proj2Id);
  assert(projB, 'projB not found');
  assert(projB.memberCount === 2, `members: ${projB.memberCount}`); // student1 + professor
});

await test('project B has 0 tasks', async () => {
  const r = await get('/dashboard/overview', profToken);
  const projB = r.data.data.find(p => p.projectId === proj2Id);
  assert(projB.taskTotal === 0, `tasks: ${projB.taskTotal}`);
});

await test('healthStatus field present', async () => {
  const r = await get('/dashboard/overview', profToken);
  r.data.data.forEach(p => {
    assert(['HEALTHY', 'WARNING', 'DANGER'].includes(p.healthStatus), `bad health: ${p.healthStatus}`);
  });
});

await test('student can also access overview', async () => {
  const r = await get('/dashboard/overview', students[0].token);
  assert(r.status === 200, `status: ${r.status}`);
  assert(r.data.data.length === 2, `expected 2 projects for student1, got ${r.data.data.length}`);
});

await test('unauthenticated → 401', async () => {
  const r = await get('/dashboard/overview');
  assert(r.status === 401, `status: ${r.status}`);
});

// Run score recalculate + detect alerts for project A, then check overview
await post(`/projects/${proj1Id}/scores/recalculate`, {}, profToken);
await post(`/projects/${proj1Id}/alerts/detect`, {}, profToken);

await test('after detect, overview shows alert data', async () => {
  const r = await get('/dashboard/overview', profToken);
  const projA = r.data.data.find(p => p.projectId === proj1Id);
  assert(projA.totalAlertCount >= 0, 'totalAlertCount missing');
  assert(typeof projA.unreadAlertCount === 'number', 'unreadAlertCount not number');
});

await test('after recalculate, scores are populated', async () => {
  const r = await get('/dashboard/overview', profToken);
  const projA = r.data.data.find(p => p.projectId === proj1Id);
  assert(typeof projA.scoreAvg === 'number', 'scoreAvg missing');
  assert(typeof projA.scoreMin === 'number', 'scoreMin missing');
  assert(typeof projA.scoreMax === 'number', 'scoreMax missing');
});

console.log(`\n🏁 Result: ${passed} passed, ${failed} failed out of ${passed + failed}`);
