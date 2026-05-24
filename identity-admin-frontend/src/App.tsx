import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  Check,
  ChevronLeft,
  ChevronRight,
  KeyRound,
  LogOut,
  RefreshCw,
  Search,
  Shield,
  UserPlus,
  Users
} from 'lucide-react';
import { ApiError, identityApi, RegisterPayload, ROLES, User, UserRole } from './api';
import { clearSession, isExpired, loadSession, saveSession, Session, toSession } from './session';

const PAGE_SIZE = 10;

type Notice = {
  type: 'success' | 'error' | 'info';
  message: string;
};

type View = 'users' | 'profile' | 'register';

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Unexpected error';
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function initials(user: User | null): string {
  if (!user) {
    return 'ID';
  }
  return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
}

function emptyRegisterForm(): RegisterPayload {
  return {
    email: '',
    firstName: '',
    lastName: '',
    password: ''
  };
}

export function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession());
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalUsers, setTotalUsers] = useState(0);
  const [page, setPage] = useState(0);
  const [view, setView] = useState<View>('users');
  const [loginEmail, setLoginEmail] = useState('admin@example.com');
  const [loginPassword, setLoginPassword] = useState('');
  const [registerForm, setRegisterForm] = useState<RegisterPayload>(emptyRegisterForm);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [busy, setBusy] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [roleUpdating, setRoleUpdating] = useState<string | null>(null);

  const isAdmin = currentUser?.role === 'ADMIN';

  const applySession = useCallback((nextSession: Session | null) => {
    setSession(nextSession);
    if (nextSession) {
      saveSession(nextSession);
    } else {
      clearSession();
      setCurrentUser(null);
      setUsers([]);
      setTotalPages(0);
      setTotalUsers(0);
    }
  }, []);

  const refreshSession = useCallback(async () => {
    if (!session?.refreshToken) {
      return null;
    }

    setRefreshing(true);
    try {
      const tokens = await identityApi.refresh(session.refreshToken);
      const nextSession = toSession(tokens);
      applySession(nextSession);
      return nextSession;
    } catch (error) {
      applySession(null);
      setNotice({ type: 'error', message: `Session refresh failed: ${errorMessage(error)}` });
      return null;
    } finally {
      setRefreshing(false);
    }
  }, [applySession, session]);

  const validAccessToken = useCallback(async () => {
    if (!session) {
      return null;
    }
    if (isExpired(session.refreshTokenExpiresAt)) {
      applySession(null);
      setNotice({ type: 'error', message: 'Refresh token expired. Sign in again.' });
      return null;
    }
    if (!isExpired(session.accessTokenExpiresAt)) {
      return session.accessToken;
    }
    const nextSession = await refreshSession();
    return nextSession?.accessToken ?? null;
  }, [applySession, refreshSession, session]);

  const loadCurrentUser = useCallback(async () => {
    const accessToken = await validAccessToken();
    if (!accessToken) {
      return;
    }
    try {
      const me = await identityApi.me(accessToken);
      setCurrentUser(me);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        applySession(null);
      }
      setNotice({ type: 'error', message: errorMessage(error) });
    }
  }, [applySession, validAccessToken]);

  const loadUsers = useCallback(async () => {
    if (!isAdmin) {
      return;
    }

    const accessToken = await validAccessToken();
    if (!accessToken) {
      return;
    }

    setBusy(true);
    try {
      const result = await identityApi.users(accessToken, page, PAGE_SIZE);
      setUsers(result.content);
      setTotalPages(result.totalPages);
      setTotalUsers(result.totalElements);
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setBusy(false);
    }
  }, [isAdmin, page, validAccessToken]);

  useEffect(() => {
    if (session) {
      void loadCurrentUser();
    }
  }, [loadCurrentUser, session]);

  useEffect(() => {
    if (isAdmin && view === 'users') {
      void loadUsers();
    }
  }, [isAdmin, loadUsers, view]);

  const filteredUsers = useMemo(() => users, [users]);

  async function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setNotice(null);

    try {
      const tokens = await identityApi.login({ email: loginEmail, password: loginPassword });
      const nextSession = toSession(tokens);
      applySession(nextSession);
      setLoginPassword('');
      setNotice({ type: 'success', message: 'Signed in.' });
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setBusy(false);
    }
  }

  async function submitRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setNotice(null);

    try {
      const created = await identityApi.register(registerForm);
      setRegisterForm(emptyRegisterForm());
      setNotice({ type: 'success', message: `Created ${created.email}. New users start as AUDITOR.` });
      if (isAdmin) {
        setView('users');
        setPage(0);
        await loadUsers();
      }
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setBusy(false);
    }
  }

  async function changeRole(userId: string, role: UserRole) {
    const accessToken = await validAccessToken();
    if (!accessToken) {
      return;
    }

    setRoleUpdating(userId);
    setNotice(null);
    try {
      const updated = await identityApi.updateRole(accessToken, userId, role);
      setUsers((items) => items.map((item) => (item.id === updated.id ? updated : item)));
      if (currentUser?.id === updated.id) {
        setCurrentUser(updated);
      }
      setNotice({ type: 'success', message: `${updated.email} is now ${updated.role}.` });
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setRoleUpdating(null);
    }
  }

  function signOut() {
    applySession(null);
    setNotice({ type: 'info', message: 'Signed out.' });
  }

  if (!session) {
    return (
      <main className="auth-screen">
        <section className="auth-panel" aria-labelledby="login-title">
          <div className="brand-mark">
            <Shield aria-hidden="true" />
          </div>
          <div>
            <p className="eyebrow">Identity Provider</p>
            <h1 id="login-title">Identity Console</h1>
            <p className="muted">Manage users, roles, and your own identity session.</p>
          </div>

          {notice && <NoticeBanner notice={notice} />}

          <form className="form-stack" onSubmit={submitLogin}>
            <label>
              Email
              <input
                autoComplete="email"
                inputMode="email"
                required
                type="email"
                value={loginEmail}
                onChange={(event) => setLoginEmail(event.target.value)}
              />
            </label>
            <label>
              Password
              <input
                autoComplete="current-password"
                required
                type="password"
                value={loginPassword}
                onChange={(event) => setLoginPassword(event.target.value)}
              />
            </label>
            <button className="primary-action" disabled={busy} type="submit">
              <KeyRound size={18} aria-hidden="true" />
              {busy ? 'Signing in' : 'Sign in'}
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Identity Console navigation">
        <div className="sidebar-header">
          <div className="brand-mark small">
            <Shield aria-hidden="true" />
          </div>
          <div>
            <strong>Identity Console</strong>
            <span>Admin service</span>
          </div>
        </div>

        <nav className="nav-list">
          <button className={view === 'users' ? 'active' : ''} onClick={() => setView('users')} type="button">
            <Users size={18} aria-hidden="true" />
            Users
          </button>
          <button className={view === 'profile' ? 'active' : ''} onClick={() => setView('profile')} type="button">
            <Shield size={18} aria-hidden="true" />
            Profile
          </button>
          <button className={view === 'register' ? 'active' : ''} onClick={() => setView('register')} type="button">
            <UserPlus size={18} aria-hidden="true" />
            Register
          </button>
        </nav>

        <button className="ghost-action sign-out" onClick={signOut} type="button">
          <LogOut size={18} aria-hidden="true" />
          Sign out
        </button>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Signed in</p>
            <h1>{currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : 'Loading session'}</h1>
          </div>
          <div className="profile-chip" title={currentUser?.email}>
            <span className="avatar">{initials(currentUser)}</span>
            <span>{currentUser?.role ?? '...'}</span>
          </div>
        </header>

        {notice && <NoticeBanner notice={notice} />}

        {view === 'users' && (
          <section className="content-section" aria-labelledby="users-title">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Admin</p>
                <h2 id="users-title">Users</h2>
              </div>
              <div className="toolbar">
                <span className="result-count">{totalUsers} total</span>
                <button className="icon-action" disabled={busy || !isAdmin} onClick={() => void loadUsers()} type="button">
                  <RefreshCw size={18} aria-hidden="true" className={busy ? 'spin' : ''} />
                  <span>Refresh</span>
                </button>
              </div>
            </div>

            {!isAdmin ? (
              <EmptyState icon={<Search size={28} />} title="Admin role required" text="Only ADMIN users can list accounts or change roles." />
            ) : (
              <>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>User</th>
                        <th>ID</th>
                        <th>Role</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredUsers.map((user) => (
                        <tr key={user.id}>
                          <td>
                            <div className="person">
                              <span className="avatar compact">{initials(user)}</span>
                              <span>
                                <strong>{user.firstName} {user.lastName}</strong>
                                <small>{user.email}</small>
                              </span>
                            </div>
                          </td>
                          <td className="mono">{user.id}</td>
                          <td>
                            <select
                              aria-label={`Role for ${user.email}`}
                              disabled={roleUpdating === user.id}
                              value={user.role}
                              onChange={(event) => void changeRole(user.id, event.target.value as UserRole)}
                            >
                              {ROLES.map((role) => (
                                <option key={role} value={role}>
                                  {role}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td>
                            {roleUpdating === user.id ? (
                              <span className="status pending">Saving</span>
                            ) : (
                              <span className="status ready">
                                <Check size={14} aria-hidden="true" />
                                Synced
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {filteredUsers.length === 0 && !busy && (
                  <EmptyState icon={<Search size={28} />} title="No users found" text="The identity service returned an empty page." />
                )}

                <div className="pagination">
                  <button className="icon-action" disabled={page === 0 || busy} onClick={() => setPage((value) => value - 1)} type="button">
                    <ChevronLeft size={18} aria-hidden="true" />
                    Previous
                  </button>
                  <span>
                    Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
                  </span>
                  <button
                    className="icon-action"
                    disabled={page + 1 >= totalPages || busy}
                    onClick={() => setPage((value) => value + 1)}
                    type="button"
                  >
                    Next
                    <ChevronRight size={18} aria-hidden="true" />
                  </button>
                </div>
              </>
            )}
          </section>
        )}

        {view === 'profile' && (
          <section className="content-section details-grid" aria-labelledby="profile-title">
            <div className="section-heading full">
              <div>
                <p className="eyebrow">Session</p>
                <h2 id="profile-title">Profile</h2>
              </div>
              <button className="icon-action" disabled={refreshing} onClick={() => void refreshSession()} type="button">
                <RefreshCw size={18} aria-hidden="true" className={refreshing ? 'spin' : ''} />
                Refresh token
              </button>
            </div>
            <Detail label="User ID" value={currentUser?.id ?? 'Loading'} />
            <Detail label="Email" value={currentUser?.email ?? 'Loading'} />
            <Detail label="Name" value={currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : 'Loading'} />
            <Detail label="Role" value={currentUser?.role ?? 'Loading'} />
            <Detail label="Access expires" value={formatDate(session.accessTokenExpiresAt)} />
            <Detail label="Refresh expires" value={formatDate(session.refreshTokenExpiresAt)} />
          </section>
        )}

        {view === 'register' && (
          <section className="content-section narrow" aria-labelledby="register-title">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Public API</p>
                <h2 id="register-title">Register User</h2>
              </div>
            </div>
            <form className="form-grid" onSubmit={submitRegister}>
              <label>
                Email
                <input
                  autoComplete="email"
                  required
                  type="email"
                  value={registerForm.email}
                  onChange={(event) => setRegisterForm((form) => ({ ...form, email: event.target.value }))}
                />
              </label>
              <label>
                First name
                <input
                  autoComplete="given-name"
                  required
                  value={registerForm.firstName}
                  onChange={(event) => setRegisterForm((form) => ({ ...form, firstName: event.target.value }))}
                />
              </label>
              <label>
                Last name
                <input
                  autoComplete="family-name"
                  required
                  value={registerForm.lastName}
                  onChange={(event) => setRegisterForm((form) => ({ ...form, lastName: event.target.value }))}
                />
              </label>
              <label>
                Password
                <input
                  autoComplete="new-password"
                  minLength={12}
                  required
                  type="password"
                  value={registerForm.password}
                  onChange={(event) => setRegisterForm((form) => ({ ...form, password: event.target.value }))}
                />
              </label>
              <button className="primary-action" disabled={busy} type="submit">
                <UserPlus size={18} aria-hidden="true" />
                {busy ? 'Creating' : 'Create user'}
              </button>
            </form>
          </section>
        )}
      </main>
    </div>
  );
}

function NoticeBanner({ notice }: { notice: Notice }) {
  return <div className={`notice ${notice.type}`}>{notice.message}</div>;
}

function EmptyState({ icon, title, text }: { icon: JSX.Element; title: string; text: string }) {
  return (
    <div className="empty-state">
      {icon}
      <strong>{title}</strong>
      <span>{text}</span>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
