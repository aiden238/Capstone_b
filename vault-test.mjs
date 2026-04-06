// Phase 4 Hash Vault API Verification
const BASE = 'http://localhost/api';

async function request(method, path, body, token, isFormData = false) {
  const opts = { method, headers: {} };
  if (token) opts.headers['Authorization'] = `Bearer ${token}`;
  if (body && !isFormData) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  if (isFormData) {
    opts.body = body; // FormData
  }
  const res = await fetch(`${BASE}${path}`, opts);
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const data = await res.json();
    return { status: res.status, data };
  }
  // binary / download
  const buffer = await res.arrayBuffer();
  return { status: res.status, data: `[binary ${buffer.byteLength} bytes]` };
}

let pass = 0, fail = 0;
function log(label, result, expectOk = true) {
  const ok = expectOk ? (result.status >= 200 && result.status < 300) : (result.status >= 400);
  ok ? pass++ : fail++;
  const icon = ok ? '✅' : '❌';
  console.log(`\n${icon} [${result.status}] ${label}`);
  if (typeof result.data === 'string') {
    console.log(result.data);
  } else {
    console.log(JSON.stringify(result.data, null, 2));
  }
}

async function run() {
  console.log('=== Phase 4 Hash Vault API Tests ===\n');
  const ts = Date.now();

  // Setup user + project
  await request('POST', '/auth/signup', { email: `v1_${ts}@t.com`, password: 'Test1234!', name: '파일팀', role: 'STUDENT' });
  const l1 = await request('POST', '/auth/login', { email: `v1_${ts}@t.com`, password: 'Test1234!' });
  const token = l1.data.data.accessToken;

  const proj = await request('POST', '/projects', { name: 'Vault테스트', description: 'test', courseName: 'CS', semester: '2026-1' }, token);
  const projectId = proj.data.data.id;
  console.log(`📋 projectId: ${projectId}`);

  // 1. Upload file
  const fileContent = `Hello Hash Vault ${ts}`;
  const blob = new Blob([fileContent], { type: 'text/plain' });
  const formData = new FormData();
  formData.append('file', blob, 'test-file.txt');
  const upload1 = await request('POST', `/projects/${projectId}/files`, formData, token, true);
  log('POST /files (파일 업로드)', upload1);
  const vaultId = upload1.data.data?.id;
  const hash1 = upload1.data.data?.fileHash;
  console.log(`  → hash: ${hash1}, version: ${upload1.data.data?.version}`);

  // 2. Upload same file again — should return existing (duplicate hash)
  const formData2 = new FormData();
  formData2.append('file', new Blob([fileContent], { type: 'text/plain' }), 'test-file.txt');
  const upload2 = await request('POST', `/projects/${projectId}/files`, formData2, token, true);
  log('POST /files (동일 파일 재업로드 → 기존 반환)', upload2);
  console.log(`  → same id? ${upload2.data.data?.id === vaultId}`);

  // 3. Upload different content with same name — should create version 2 + tamper log
  const fileContent2 = `Modified content ${ts}`;
  const formData3 = new FormData();
  formData3.append('file', new Blob([fileContent2], { type: 'text/plain' }), 'test-file.txt');
  const upload3 = await request('POST', `/projects/${projectId}/files`, formData3, token, true);
  log('POST /files (동일명 다른 내용 → 버전2 + 변조감지)', upload3);
  console.log(`  → version: ${upload3.data.data?.version}, hash: ${upload3.data.data?.fileHash}`);

  // 4. List all files
  const files = await request('GET', `/projects/${projectId}/files`, null, token);
  log('GET /files (파일 목록)', files);
  console.log(`  → 파일 수: ${files.data.data?.length}`);

  // 5. Get file detail
  if (vaultId) {
    const detail = await request('GET', `/projects/${projectId}/files/${vaultId}`, null, token);
    log(`GET /files/${vaultId} (상세)`, detail);
  }

  // 6. Get file history by name
  const history = await request('GET', `/projects/${projectId}/files/history?fileName=test-file.txt`, null, token);
  log('GET /files/history?fileName=test-file.txt (이력)', history);
  console.log(`  → 버전 수: ${history.data.data?.length}`);

  // 7. Download file
  if (vaultId) {
    const download = await request('GET', `/files/${vaultId}/download`, null, token);
    log(`GET /files/${vaultId}/download (다운로드)`, download);
  }

  // 8. Upload different file
  const formData4 = new FormData();
  formData4.append('file', new Blob(['design document'], { type: 'text/plain' }), 'design.md');
  const upload4 = await request('POST', `/projects/${projectId}/files`, formData4, token, true);
  log('POST /files (다른 파일 업로드)', upload4);

  // 9. Non-member access → should fail
  await request('POST', '/auth/signup', { email: `outsider2_${ts}@t.com`, password: 'Test1234!', name: '외부인2', role: 'STUDENT' });
  const l2 = await request('POST', '/auth/login', { email: `outsider2_${ts}@t.com`, password: 'Test1234!' });
  const tokenOutsider = l2.data.data.accessToken;
  const outsiderFiles = await request('GET', `/projects/${projectId}/files`, null, tokenOutsider);
  log('GET /files (비멤버 접근 → 실패)', outsiderFiles, false);

  console.log(`\n\n=== Vault API Summary ===`);
  console.log(`✅ Passed: ${pass}  ❌ Failed: ${fail}`);
  console.log(`Total: ${pass + fail} tests`);
}

run().catch(e => console.error('Script error:', e));
