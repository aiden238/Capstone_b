'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';

interface FileVault {
  id: string;
  fileName: string;
  fileHash: string;
  fileSize: number;
  version: number;
  uploadedAt: string;
  uploaderName: string;
}

export default function FilesPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [files, setFiles] = useState<FileVault[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [historyName, setHistoryName] = useState<string | null>(null);
  const [history, setHistory] = useState<FileVault[]>([]);
  const [dragOver, setDragOver] = useState(false);

  const fetchFiles = useCallback(async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const { data } = await api.get(`/projects/${projectId}/files`);
      setFiles(data.data);
    } catch { /* ignore */ }
    setLoading(false);
  }, [projectId]);

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  const handleUpload = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0 || !projectId) return;
    setUploading(true);
    try {
      for (const file of Array.from(fileList)) {
        const form = new FormData();
        form.append('file', file);
        await api.post(`/projects/${projectId}/files`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
      }
      await fetchFiles();
    } catch { /* ignore */ }
    setUploading(false);
  };

  const handleDownload = (vaultId: string, fileName: string) => {
    const token = localStorage.getItem('accessToken');
    const url = `/api/files/${vaultId}/download`;
    const a = document.createElement('a');
    // Use fetch to handle auth
    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => res.blob())
      .then((blob) => {
        const blobUrl = URL.createObjectURL(blob);
        a.href = blobUrl;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(blobUrl);
      });
  };

  const showHistory = async (fileName: string) => {
    if (!projectId) return;
    setHistoryName(fileName);
    const { data } = await api.get(`/projects/${projectId}/files/history?fileName=${encodeURIComponent(fileName)}`);
    setHistory(data.data);
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">파일 (Hash Vault)</h2>
        <span className="text-sm text-gray-400">{files.length}개 파일</span>
      </div>

      {/* Upload Zone */}
      <div
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => { e.preventDefault(); setDragOver(false); handleUpload(e.dataTransfer.files); }}
        className={`rounded-lg border-2 border-dashed p-6 text-center transition ${
          dragOver ? 'border-blue-400 bg-blue-50' : 'border-gray-300 bg-gray-50'
        }`}
      >
        {uploading ? (
          <p className="text-sm text-blue-600">업로드 중...</p>
        ) : (
          <>
            <p className="text-sm text-gray-500">파일을 여기에 드래그하거나</p>
            <label className="mt-2 inline-block cursor-pointer rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
              파일 선택
              <input
                type="file"
                multiple
                className="hidden"
                onChange={(e) => handleUpload(e.target.files)}
              />
            </label>
            <p className="mt-1 text-xs text-gray-400">SHA-256 해시로 무결성 보장 · 변조 자동 감지</p>
          </>
        )}
      </div>

      {/* File List */}
      {loading ? (
        <div className="py-8 text-center text-gray-400">로딩 중...</div>
      ) : files.length === 0 ? (
        <div className="py-8 text-center text-gray-400">업로드된 파일이 없습니다</div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">파일명</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">버전</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">크기</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">해시</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">업로더</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">날짜</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">액션</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {files.map((f) => (
                <tr key={f.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{f.fileName}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">v{f.version}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">{formatSize(f.fileSize)}</td>
                  <td className="px-4 py-3">
                    <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                      {f.fileHash.substring(0, 12)}...
                    </code>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{f.uploaderName}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(f.uploadedAt).toLocaleDateString('ko-KR')}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-1">
                      <button
                        onClick={() => handleDownload(f.id, f.fileName)}
                        className="rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50"
                      >
                        다운로드
                      </button>
                      <button
                        onClick={() => showHistory(f.fileName)}
                        className="rounded px-2 py-1 text-xs text-gray-600 hover:bg-gray-100"
                      >
                        이력
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* History Modal */}
      {historyName && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setHistoryName(null)}>
          <div className="w-full max-w-lg rounded-lg bg-white p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="mb-4 text-lg font-bold text-gray-900">
              📜 {historyName} 버전 이력
            </h3>
            {history.length === 0 ? (
              <p className="text-sm text-gray-400">이력이 없습니다</p>
            ) : (
              <div className="space-y-3">
                {history.map((h, i) => (
                  <div key={h.id} className="flex items-center justify-between rounded-lg border border-gray-200 p-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="rounded bg-blue-100 px-1.5 py-0.5 text-xs font-medium text-blue-700">
                          v{h.version}
                        </span>
                        {i === 0 && (
                          <span className="rounded bg-green-100 px-1.5 py-0.5 text-xs font-medium text-green-700">
                            최신
                          </span>
                        )}
                      </div>
                      <code className="mt-1 block text-xs text-gray-500">{h.fileHash.substring(0, 16)}...</code>
                      <div className="mt-1 text-xs text-gray-400">
                        {h.uploaderName} · {new Date(h.uploadedAt).toLocaleString('ko-KR')} · {formatSize(h.fileSize)}
                      </div>
                    </div>
                    <button
                      onClick={() => handleDownload(h.id, `${h.fileName}_v${h.version}`)}
                      className="rounded bg-gray-100 px-2 py-1 text-xs text-gray-700 hover:bg-gray-200"
                    >
                      다운
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setHistoryName(null)}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
