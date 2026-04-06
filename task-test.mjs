// Phase 3 Task API Verification
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

function log(label, result) {
  const icon = result.status >= 200 && result.status < 300 ? '✅' : '❌';
  console.log(`\n${icon} [${result.status}] ${label}`);
  console.log(JSON.stringify(result.data, null, 2));
}

async function run() {
  console.log('=== Phase 3 Task API Tests ===\n');

  const ts = Date.now();

  // Setup: create 2 users + login
  await request('POST', '/auth/signup', { email: `t1_${ts}@t.com`, password: 'Test1234!', name: '학생A', role: 'STUDENT' });
  await request('POST', '/auth/signup', { email: `t2_${ts}@t.com`, password: 'Test1234!', name: '학생B', role: 'STUDENT' });

  const l1 = await request('POST', '/auth/login', { email: `t1_${ts}@t.com`, password: 'Test1234!' });
  const l2 = await request('POST', '/auth/login', { email: `t2_${ts}@t.com`, password: 'Test1234!' });
  const token1 = l1.data.data.accessToken;
  const token2 = l2.data.data.accessToken;

  // Create project + join
  const proj = await request('POST', '/projects', { name: 'Task테스트', description: 'test', courseName: 'CS', semester: '2026-1' }, token1);
  const projectId = proj.data.data.id;
  const inviteCode = proj.data.data.inviteCode;
  await request('POST', '/projects/join', { inviteCode }, token2);

  console.log(`📋 projectId: ${projectId}`);

  // Get member info for student B
  const members = await request('GET', `/projects/${projectId}/members`, null, token1);
  const studentB = members.data.data.find(m => m.name === '학생B');
  const studentBId = studentB.userId;

  // 1. Create task
  const create1 = await request('POST', `/projects/${projectId}/tasks`, {
    title: '로그인 페이지 구현',
    description: 'Next.js 로그인/회원가입 페이지',
    priority: 'HIGH',
    tag: '기능',
    dueDate: '2026-04-15',
    assigneeIds: [studentBId]
  }, token1);
  log('POST /tasks (태스크 생성)', create1);
  const taskId = create1.data.data?.id;

  // 2. Create second task
  const create2 = await request('POST', `/projects/${projectId}/tasks`, {
    title: 'README 작성',
    description: '프로젝트 README.md',
    priority: 'LOW',
    tag: '문서'
  }, token1);
  log('POST /tasks (두번째 태스크)', create2);

  // 3. Get all tasks
  const allTasks = await request('GET', `/projects/${projectId}/tasks`, null, token1);
  log('GET /tasks (전체 목록)', allTasks);

  // 4. Get tasks by status
  const todoTasks = await request('GET', `/projects/${projectId}/tasks?status=TODO`, null, token1);
  log('GET /tasks?status=TODO (상태 필터)', todoTasks);

  // 5. Get single task
  if (taskId) {
    const single = await request('GET', `/projects/${projectId}/tasks/${taskId}`, null, token1);
    log(`GET /tasks/${taskId} (상세)`, single);
  }

  // 6. Update task
  if (taskId) {
    const updated = await request('PATCH', `/projects/${projectId}/tasks/${taskId}`, {
      title: '로그인 페이지 구현 (수정)',
      priority: 'URGENT'
    }, token1);
    log(`PATCH /tasks/${taskId} (수정)`, updated);
  }

  // 7. Status change: TODO → IN_PROGRESS
  if (taskId) {
    const inProgress = await request('PATCH', `/projects/${projectId}/tasks/${taskId}/status`, {
      status: 'IN_PROGRESS'
    }, token1);
    log(`PATCH /tasks/${taskId}/status → IN_PROGRESS`, inProgress);
  }

  // 8. Status change: IN_PROGRESS → DONE
  if (taskId) {
    const done = await request('PATCH', `/projects/${projectId}/tasks/${taskId}/status`, {
      status: 'DONE'
    }, token1);
    log(`PATCH /tasks/${taskId}/status → DONE`, done);
    if (done.data.data?.completedAt) console.log('  → ✅ completedAt 자동 설정됨');
  }

  // 9. Add assignee (학생A self-assign)
  const meRes = await request('GET', '/auth/me', null, token1);
  const studentAId = meRes.data.data.id;
  if (taskId) {
    const assign = await request('POST', `/projects/${projectId}/tasks/${taskId}/assignees`, {
      assigneeId: studentAId
    }, token1);
    log(`POST /tasks/${taskId}/assignees (담당자 추가)`, assign);
  }

  // 10. Remove assignee
  if (taskId) {
    const unassign = await request('DELETE', `/projects/${projectId}/tasks/${taskId}/assignees/${studentBId}`, null, token1);
    log(`DELETE /tasks/${taskId}/assignees/${studentBId} (담당자 제거)`, unassign);
  }

  // 11. Delete task (second one)
  const task2Id = create2.data.data?.id;
  if (task2Id) {
    const del = await request('DELETE', `/projects/${projectId}/tasks/${task2Id}`, null, token1);
    log(`DELETE /tasks/${task2Id} (삭제)`, del);
  }

  // 12. Verify deletion
  const afterDelete = await request('GET', `/projects/${projectId}/tasks`, null, token1);
  log('GET /tasks (삭제 후 목록)', afterDelete);
  console.log(`  → 태스크 수: ${afterDelete.data.data?.length}`);

  console.log('\n\n=== Task API Summary ===');
  console.log('All Phase 3 Task API tests completed.');
}

run().catch(e => console.error('Script error:', e));
