import * as AuthSession from 'expo-auth-session';
import * as SecureStore from 'expo-secure-store';
import * as WebBrowser from 'expo-web-browser';
import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { Platform } from 'react-native';
import { CLIENT_ID, DISCOVERY, REDIRECT_URI } from './config';

WebBrowser.maybeCompleteAuthSession();
type Session = { accessToken: string; refreshToken?: string; expiresAt: number };
type AuthValue = { ready: boolean; signedIn: boolean; busy: boolean; error: string | null; signIn: () => Promise<void>; signOut: () => Promise<void>; accessToken: () => Promise<string | null> };
const KEY = 'opencity.session.v1';
const Context = createContext<AuthValue | null>(null);
function fromToken(token: AuthSession.TokenResponse, previous?: string): Session {
  return { accessToken: token.accessToken, refreshToken: token.refreshToken || previous, expiresAt: Date.now() + (token.expiresIn || 300) * 1000 };
}
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const refreshInFlight = useRef<Promise<string | null> | null>(null);
  const sessionEpoch = useRef(0);
  const [request, , promptAsync] = AuthSession.useAuthRequest({ clientId: CLIENT_ID, redirectUri: REDIRECT_URI, responseType: AuthSession.ResponseType.Code, scopes: ['openid', 'profile', 'email'], usePKCE: true }, DISCOVERY);
  useEffect(() => {
    if (Platform.OS === 'web') { setReady(true); return; }
    SecureStore.getItemAsync(KEY).then(value => { if (value) setSession(JSON.parse(value) as Session); }).catch(() => setError('Không đọc được phiên đăng nhập đã lưu.')).finally(() => setReady(true));
  }, []);
  const save = useCallback(async (value: Session | null) => {
    setSession(value);
    if (Platform.OS !== 'web') { if (value) await SecureStore.setItemAsync(KEY, JSON.stringify(value)); else await SecureStore.deleteItemAsync(KEY); }
  }, []);
  const signIn = useCallback(async () => {
    if (Platform.OS === 'web') { setError('Đăng nhập cần bản Android development build.'); return; }
    if (!request) { setError('Đang chuẩn bị đăng nhập. Hãy thử lại sau vài giây.'); return; }
    setBusy(true); setError(null);
    try {
      const result = await promptAsync();
      if (result.type !== 'success') { if (result.type !== 'dismiss' && result.type !== 'cancel') setError('Đăng nhập chưa hoàn tất.'); return; }
      if (!request.codeVerifier) throw new Error('Thiếu mã xác thực PKCE');
      const token = await AuthSession.exchangeCodeAsync({ clientId: CLIENT_ID, code: result.params.code, redirectUri: REDIRECT_URI, extraParams: { code_verifier: request.codeVerifier } }, DISCOVERY);
      await save(fromToken(token));
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể đăng nhập.'); }
    finally { setBusy(false); }
  }, [promptAsync, request, save]);
  const signOut = useCallback(async () => {
    sessionEpoch.current += 1;
    if (session?.refreshToken) { try { await AuthSession.revokeAsync({ clientId: CLIENT_ID, token: session.refreshToken }, DISCOVERY); } catch { /* Offline sign out still clears local session. */ } }
    await save(null); setError(null);
  }, [save, session]);
  const accessToken = useCallback(async () => {
    if (!session) return null;
    if (session.expiresAt > Date.now() + 60_000) return session.accessToken;
    if (!session.refreshToken) { await save(null); return null; }
    if (refreshInFlight.current) return refreshInFlight.current;
    const epoch = sessionEpoch.current;
    const refresh = (async () => {
      try { const token = await AuthSession.refreshAsync({ clientId: CLIENT_ID, refreshToken: session.refreshToken! }, DISCOVERY); if (epoch !== sessionEpoch.current) return null; const next = fromToken(token, session.refreshToken); await save(next); return next.accessToken; }
      catch { if (epoch === sessionEpoch.current) await save(null); return null; }
    })();
    refreshInFlight.current = refresh;
    try { return await refresh; } finally { refreshInFlight.current = null; }
  }, [session, save]);
  return <Context.Provider value={{ ready, signedIn: !!session, busy, error, signIn, signOut, accessToken }}>{children}</Context.Provider>;
}
export function useAuth() { const value = useContext(Context); if (!value) throw new Error('AuthProvider is missing'); return value; }
