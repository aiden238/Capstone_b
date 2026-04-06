// Phase 2 API Verification Script
const BASE = 'http://localhost/api';

async function request(method, path, body, token) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (token) opts.headers['Authorization'] = `Bearer ${token}`;
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(`${BASE}${path}`, opts);
  const text = await res.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }
  return { status: res.status, data };
}

function log(label, result) {
  const icon = result.status >= 200 && result.status < 300 ? '✅' : '❌';
  console.log(`\n${icon} [${result.status}] ${label}`);
  console.log(JSON.stringify(result.data, null, 2));
}

async function run() {
  console.log('=== Phase 2 Auth API Tests ===\n');

  // 1. Signup (new user)
  const ts = Date.now();
  const email1 = `student1_${ts}@test.com`;
  const email2 = `student2_${ts}@test.com`;
  const profEmail = `prof_${ts}@test.com`;

  const signup1 = await request('POST', '/auth/signup', {
    email: email1, password: 'Test1234!', name: '학생1', role: 'STUDENT'
  });
  log('POST /auth/signup (학생1)', signup1);

  // 2. Signup second user
  const signup2 = await request('POST', '/auth/signup', {
    email: email2, password: 'Test1234!', name: '학생2', role: 'STUDENT'
  });
  log('POST /auth/signup (학생2)', signup2);

  // 3. Signup professor
  const signupProf = await request('POST', '/auth/signup', {
    email: profEmail, password: 'Test1234!', name: '교수님', role: 'PROFESSOR'
  });
  log('POST /auth/signup (교수)', signupProf);

  // 4. Login
  const login1 = await request('POST', '/auth/login', {
    email: email1, password: 'Test1234!'
  });
  log('POST /auth/login (학생1)', login1);

  if (login1.status !== 200) {
    console.log('\n❌ Login failed, cannot continue auth tests.');
    return;
  }

  const accessToken = login1.data.data?.accessToken || login1.data.accessToken;
  const refreshToken = login1.data.data?.refreshToken || login1.data.refreshToken;
  console.log(`\n🔑 accessToken: ${accessToken?.substring(0, 30)}...`);
  console.log(`🔑 refreshToken: ${refreshToken?.substring(0, 30)}...`);

  // 5. GET /auth/me
  const me = await request('GET', '/auth/me', null, accessToken);
  log('GET /auth/me', me);

  // 6. POST /auth/refresh
  const refresh = await request('POST', '/auth/refresh', { refreshToken });
  log('POST /auth/refresh', refresh);

  const newAccessToken = refresh.data.data?.accessToken || refresh.data.accessToken || accessToken;

  // 7. GET /auth/me with 401 (no token)
  const me401 = await request('GET', '/auth/me');
  log('GET /auth/me (no token → expect 401)', me401);
  if (me401.status === 401) console.log('  → ✅ 401 반환 정상');
  else console.log('  → ⚠️ 401이 아닌 ' + me401.status + ' 반환');

  // Login student2
  const login2 = await request('POST', '/auth/login', {
    email: email2, password: 'Test1234!'
  });
  const token2 = login2.data.data?.accessToken || login2.data.accessToken;

  // Login professor
  const loginProf = await request('POST', '/auth/login', {
    email: profEmail, password: 'Test1234!'
  });
  const tokenProf = loginProf.data.data?.accessToken || loginProf.data.accessToken;

  console.log('\n\n=== Phase 2 Project API Tests ===\n');

  // 8. Create project
  const createProj = await request('POST', '/projects', {
    name: '캡스톤 프로젝트',
    description: '팀 블랙박스 기여도 자동 산출 시스템',
    courseName: '캡스톤디자인',
    semester: '2026-1'
  }, newAccessToken);
  log('POST /projects (프로젝트 생성)', createProj);

  const projectId = createProj.data.data?.id || createProj.data.id;
  const inviteCode = createProj.data.data?.inviteCode || createProj.data.inviteCode;
  console.log(`\n📋 projectId: ${projectId}`);
  console.log(`📋 inviteCode: ${inviteCode}`);

  // 9. GET /projects (내 프로젝트 목록)
  const myProjects = await request('GET', '/projects', null, newAccessToken);
  log('GET /projects (내 프로젝트 목록)', myProjects);

  // 10. GET /projects/:id
  if (projectId) {
    const getProj = await request('GET', `/projects/${projectId}`, null, newAccessToken);
    log(`GET /projects/${projectId} (상세)`, getProj);
  }

  // 11. Join project (학생2)
  if (inviteCode) {
    const join = await request('POST', '/projects/join', { inviteCode }, token2);
    log('POST /projects/join (학생2 참여)', join);
  }

  // 12. GET /projects/:id/members
  if (projectId) {
    const members = await request('GET', `/projects/${projectId}/members`, null, newAccessToken);
    log(`GET /projects/${projectId}/members (멤버 목록)`, members);

    // Find student2 member id
    const memberList = members.data.data || members.data || [];
    const student2Member = Array.isArray(memberList) 
      ? memberList.find(m => m.user?.name === '학생2' || m.name === '학생2' || m.userName === '학생2')
      : null;
    const memberId = student2Member?.id || student2Member?.memberId;
    console.log(`\n👤 학생2 memberId: ${memberId}`);

    // 13. PATCH member role (make student2 MEMBER → LEADER test, then back)
    if (memberId) {
      const roleChange = await request('PATCH', `/projects/${projectId}/members/${memberId}/role`, {
        role: 'MEMBER'
      }, newAccessToken);
      log(`PATCH /projects/${projectId}/members/${memberId}/role`, roleChange);
    }

    // 14. PATCH consent
    const consent = await request('PATCH', `/projects/${projectId}/members/me/consent`, {
      consentPlatform: true,
      consentGithub: false,
      consentDrive: false,
      consentAiAnalysis: false
    }, newAccessToken);
    log(`PATCH /projects/${projectId}/members/me/consent`, consent);

    // 15. POST invite-code regeneration
    const newInvite = await request('POST', `/projects/${projectId}/invite-code`, null, newAccessToken);
    log(`POST /projects/${projectId}/invite-code (초대코드 재생성)`, newInvite);
  }

  // 16. Logout
  const logout = await request('POST', '/auth/logout', null, newAccessToken);
  log('POST /auth/logout', logout);

  // 17. After logout, /auth/me should fail
  const meAfterLogout = await request('GET', '/auth/me', null, newAccessToken);
  log('GET /auth/me (logout 후 → expect 401)', meAfterLogout);
  if (meAfterLogout.status === 401) console.log('  → ✅ logout 후 401 반환 정상 (블랙리스트 적용)');
  else console.log('  → ⚠️ logout 후 ' + meAfterLogout.status + ' 반환 — 블랙리스트 미작동');

  console.log('\n\n=== Summary ===');
  console.log('All Phase 2 API tests completed. Review results above.');
}

run().catch(e => console.error('Script error:', e));
