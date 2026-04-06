// Phase 3 Meeting API Verification
const BASE = 'http://localhost/api';

async function request(method, path, body, token) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (token) opts.headers['Authorization'] = `Bearer ${token}`;
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${BASE}${path}`, opts);
  const text = await res.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }
  return { status: res.status, data };
}

let pass = 0, fail = 0;
function log(label, result, expectOk = true) {
  const ok = expectOk ? (result.status >= 200 && result.status < 300) : (result.status >= 400);
  const icon = ok ? '✅' : '❌';
  ok ? pass++ : fail++;
  console.log(`\n${icon} [${result.status}] ${label}`);
  console.log(JSON.stringify(result.data, null, 2));
}

async function run() {
  console.log('=== Phase 3 Meeting API Tests ===\n');

  const ts = Date.now();

  // Setup: 2 users
  await request('POST', '/auth/signup', { email: `m1_${ts}@t.com`, password: 'Test1234!', name: '회의팀A', role: 'STUDENT' });
  await request('POST', '/auth/signup', { email: `m2_${ts}@t.com`, password: 'Test1234!', name: '회의팀B', role: 'STUDENT' });

  const l1 = await request('POST', '/auth/login', { email: `m1_${ts}@t.com`, password: 'Test1234!' });
  const l2 = await request('POST', '/auth/login', { email: `m2_${ts}@t.com`, password: 'Test1234!' });
  const token1 = l1.data.data.accessToken;
  const token2 = l2.data.data.accessToken;

  // Create project + join
  const proj = await request('POST', '/projects', { name: 'Meeting테스트', description: 'test', courseName: 'CS', semester: '2026-1' }, token1);
  const projectId = proj.data.data.id;
  const inviteCode = proj.data.data.inviteCode;
  await request('POST', '/projects/join', { inviteCode }, token2);
  console.log(`📋 projectId: ${projectId}`);

  // 1. Create meeting
  const create1 = await request('POST', `/projects/${projectId}/meetings`, {
    title: '1주차 정기회의',
    meetingDate: '2026-04-01T14:00:00+09:00',
    purpose: '역할 분담 및 일정 논의'
  }, token1);
  log('POST /meetings (회의 생성)', create1);
  const meetingId = create1.data.data?.id;
  const checkinCode = create1.data.data?.checkinCode;
  console.log(`  → checkinCode: ${checkinCode}`);

  // 2. Create second meeting
  const create2 = await request('POST', `/projects/${projectId}/meetings`, {
    title: '2주차 진행상황 회의',
    meetingDate: '2026-04-08T14:00:00+09:00',
    purpose: '진행상황 공유'
  }, token1);
  log('POST /meetings (두번째 회의)', create2);
  const meetingId2 = create2.data.data?.id;

  // 3. Get all meetings
  const allMeetings = await request('GET', `/projects/${projectId}/meetings`, null, token1);
  log('GET /meetings (전체 목록)', allMeetings);
  console.log(`  → 회의 수: ${allMeetings.data.data?.length}`);

  // 4. Get single meeting
  if (meetingId) {
    const single = await request('GET', `/projects/${projectId}/meetings/${meetingId}`, null, token1);
    log(`GET /meetings/${meetingId} (상세)`, single);
  }

  // 5. Update meeting (notes + decisions)
  if (meetingId) {
    const updated = await request('PATCH', `/projects/${projectId}/meetings/${meetingId}`, {
      title: '1주차 정기회의 (수정)',
      notes: '- 프론트: 학생A\n- 백엔드: 학생B',
      decisions: '매주 월요일 2시 정기회의'
    }, token1);
    log(`PATCH /meetings/${meetingId} (수정)`, updated);
    if (updated.data.data?.notes) console.log('  → ✅ notes 정상 저장');
    if (updated.data.data?.decisions) console.log('  → ✅ decisions 정상 저장');
  }

  // 6. Checkin - user1 (creator)
  if (meetingId && checkinCode) {
    const checkin1 = await request('POST', `/projects/${projectId}/meetings/${meetingId}/checkin`, {
      checkinCode
    }, token1);
    log(`POST /meetings/${meetingId}/checkin (user1 체크인)`, checkin1);
    const attendees = checkin1.data.data?.attendees;
    if (attendees?.length > 0) console.log(`  → 참석자 수: ${attendees.length}, checkedIn: ${attendees[0].checkedIn}`);
  }

  // 7. Checkin - user2
  if (meetingId && checkinCode) {
    const checkin2 = await request('POST', `/projects/${projectId}/meetings/${meetingId}/checkin`, {
      checkinCode
    }, token2);
    log(`POST /meetings/${meetingId}/checkin (user2 체크인)`, checkin2);
    const attendees = checkin2.data.data?.attendees;
    console.log(`  → 참석자 수: ${attendees?.length}`);
  }

  // 8. Duplicate checkin - user1 again → should fail
  if (meetingId && checkinCode) {
    const dupCheckin = await request('POST', `/projects/${projectId}/meetings/${meetingId}/checkin`, {
      checkinCode
    }, token1);
    log(`POST /meetings/${meetingId}/checkin (중복 체크인 → 실패)`, dupCheckin, false);
  }

  // 9. Wrong checkin code → should fail
  if (meetingId) {
    const badCode = await request('POST', `/projects/${projectId}/meetings/${meetingId}/checkin`, {
      checkinCode: 'WRONGCOD'
    }, token1);
    log(`POST /meetings/${meetingId}/checkin (잘못된 코드 → 실패)`, badCode, false);
  }

  // 10. Get meeting detail after checkins - verify attendees
  if (meetingId) {
    const detail = await request('GET', `/projects/${projectId}/meetings/${meetingId}`, null, token1);
    log(`GET /meetings/${meetingId} (체크인 후 상세)`, detail);
    const attendees = detail.data.data?.attendees;
    console.log(`  → 참석자(체크인완료): ${attendees?.filter(a => a.checkedIn).length}명`);
  }

  // 11. Delete second meeting
  if (meetingId2) {
    const del = await request('DELETE', `/projects/${projectId}/meetings/${meetingId2}`, null, token1);
    log(`DELETE /meetings/${meetingId2} (삭제)`, del);
  }

  // 12. Verify deletion
  const afterDel = await request('GET', `/projects/${projectId}/meetings`, null, token1);
  log('GET /meetings (삭제 후 목록)', afterDel);
  console.log(`  → 회의 수: ${afterDel.data.data?.length}`);

  // 13. Non-member access → should fail
  await request('POST', '/auth/signup', { email: `outsider_${ts}@t.com`, password: 'Test1234!', name: '외부인', role: 'STUDENT' });
  const l3 = await request('POST', '/auth/login', { email: `outsider_${ts}@t.com`, password: 'Test1234!' });
  const tokenOutsider = l3.data.data.accessToken;
  const outsiderAccess = await request('GET', `/projects/${projectId}/meetings`, null, tokenOutsider);
  log('GET /meetings (비멤버 접근 → 실패)', outsiderAccess, false);

  console.log(`\n\n=== Meeting API Summary ===`);
  console.log(`✅ Passed: ${pass}  ❌ Failed: ${fail}`);
  console.log(`Total: ${pass + fail} tests`);
}

run().catch(e => console.error('Script error:', e));
