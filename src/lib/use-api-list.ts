import { useCallback, useState } from 'react';
import { useFocusEffect } from 'expo-router';
import { api } from './api';
import { useAuth } from './auth';

export function useApiList<T>(path: string) {
  const auth = useAuth();
  const [items, setItems] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const refresh = useCallback(async () => {
    if (!auth.signedIn) return;
    setLoading(true); setError(null);
    try { setItems(await api<T[]>(path, auth.accessToken)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Tải dữ liệu thất bại.'); }
    finally { setLoading(false); }
  }, [auth.signedIn, auth.accessToken, path]);
  useFocusEffect(useCallback(() => { void refresh(); }, [refresh]));
  return { items, loading, error, refresh };
}
