import { GATEWAY_URL } from './config';

export type Report = { id: string; title: string; description: string; category: string; address: string; latitude: number | null; longitude: number | null; state: string; created_at: string; attachment_ids: string[]; history: { action: string; at?: string; timestamp?: string }[]; warnings?: string[] };
export type Bulletin = { id: string; category: 'news' | 'alert'; title: string; summary: string; body: string; area: string | null; latitude: number | null; longitude: number | null; published_at: string };
export type SurveySummary = { id: string; title: string; description: string; status: 'open' | 'closed'; created_at: string; my_option_id: string | null };
export type SurveyDetail = SurveySummary & { options: { id: string; label: string; votes: number | null }[]; total_responses: number | null; closed_at: string | null };
export type Photo = { uri: string; fileName?: string | null; mimeType?: string | null };
export type TokenGetter = () => Promise<string | null>;

export async function api<T>(path: string, getToken: TokenGetter, init?: RequestInit): Promise<T> {
  const token = await getToken();
  if (!token) throw new Error('Vui lòng đăng nhập để tiếp tục.');
  let response: Response;
  try { response = await fetch(`${GATEWAY_URL}/api/opencity${path}`, { ...init, headers: { Authorization: `Bearer ${token}`, ...init?.headers } }); }
  catch { throw new Error('Không kết nối được máy chủ. Kiểm tra Docker và adb reverse.'); }
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new Error(typeof payload?.detail === 'string' ? payload.detail : `HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}
export async function createReport(getToken: TokenGetter, values: { title: string; description: string; category: string; address: string; latitude?: number; longitude?: number; photo?: Photo | null }): Promise<Report> {
  const form = new FormData();
  form.append('title', values.title.trim()); form.append('description', values.description.trim());
  form.append('category', values.category); form.append('address', values.address.trim());
  if (values.latitude !== undefined && values.longitude !== undefined) { form.append('latitude', String(values.latitude)); form.append('longitude', String(values.longitude)); }
  if (values.photo) form.append('photo', { uri: values.photo.uri, name: values.photo.fileName || 'report.jpg', type: values.photo.mimeType || 'image/jpeg' } as unknown as Blob);
  return api<Report>('/reports', getToken, { method: 'POST', body: form });
}
