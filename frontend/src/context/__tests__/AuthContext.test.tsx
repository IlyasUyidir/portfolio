import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { AuthProvider } from '../AuthContext';
import { useAuth } from '../../hooks/useAuth';
import * as authApi from '../../api/authApi';
import type { UserProfile } from '../../types';

// Mock the authApi
vi.mock('../../api/authApi', () => ({
  getMe: vi.fn(),
  logout: vi.fn(),
}));

const mockGetMe = authApi.getMe as ReturnType<typeof vi.fn>;
const mockLogout = authApi.logout as ReturnType<typeof vi.fn>;

// Test component to consume the AuthContext
const TestConsumer = () => {
  const { user, isAuthenticated, isLoading, isPremium, login, logout } = useAuth();

  if (isLoading) return <div data-testid="loading">Loading...</div>;

  return (
    <div>
      <div data-testid="auth-status">{isAuthenticated ? 'authenticated' : 'not-authenticated'}</div>
      <div data-testid="user-email">{user?.email}</div>
      <div data-testid="premium-status">{isPremium ? 'premium' : 'standard'}</div>
      <button onClick={() => login({ id: 1, email: 'login@test.com', username: 'testuser', role: 'STANDARD', createdAt: '' })}>
        Login Action
      </button>
      <button onClick={() => logout()}>Logout Action</button>
    </div>
  );
};

describe('AuthContext', () => {
  const mockUser: UserProfile = {
    id: 1,
    email: 'test@folio.io',
    username: 'test',
    role: 'STANDARD',
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // 1. AuthProvider_onMount_shouldCallGetMeToRestoreSession
  it('AuthProvider_onMount_shouldCallGetMeToRestoreSession', async () => {
    // Arrange
    mockGetMe.mockResolvedValue(mockUser);

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    await waitFor(() => expect(mockGetMe).toHaveBeenCalledOnce());
  });

  // 2. AuthProvider_whenGetMeSucceeds_shouldSetIsAuthenticatedTrue
  it('AuthProvider_whenGetMeSucceeds_shouldSetIsAuthenticatedTrue', async () => {
    // Arrange
    mockGetMe.mockResolvedValue(mockUser);

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
      expect(screen.getByTestId('user-email')).toHaveTextContent('test@folio.io');
    });
  });

  // 3. AuthProvider_whenGetMeFails_shouldSetIsAuthenticatedFalse
  it('AuthProvider_whenGetMeFails_shouldSetIsAuthenticatedFalse', async () => {
    // Arrange
    mockGetMe.mockRejectedValue(new Error('Unauthorized'));

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('not-authenticated');
    });
  });

  // 4. AuthProvider_duringInitialLoad_isLoadingShouldBeTrue
  it('AuthProvider_duringInitialLoad_isLoadingShouldBeTrue', async () => {
    // Arrange
    // Return a promise that doesn't resolve immediately
    mockGetMe.mockReturnValue(new Promise(() => { }));

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    expect(screen.getByTestId('loading')).toBeInTheDocument();
  });

  // 5. login_shouldSetUserAndMarkAuthenticated
  it('login_shouldSetUserAndMarkAuthenticated', async () => {
    // Arrange
    mockGetMe.mockRejectedValue(new Error('Not logged in'));
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );
    await waitFor(() => expect(screen.getByTestId('auth-status')).toHaveTextContent('not-authenticated'));

    // Act
    const loginButton = screen.getByText('Login Action');
    await act(async () => {
      loginButton.click();
    });

    // Assert
    expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    expect(screen.getByTestId('user-email')).toHaveTextContent('login@test.com');
  });

  // 6. logout_shouldCallApiAndClearUser
  it('logout_shouldCallApiAndClearUser', async () => {
    // Arrange
    mockGetMe.mockResolvedValue(mockUser);
    mockLogout.mockResolvedValue(undefined);
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );
    await waitFor(() => expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated'));

    // Act
    const logoutButton = screen.getByText('Logout Action');
    await act(async () => {
      logoutButton.click();
    });

    // Assert
    expect(mockLogout).toHaveBeenCalledOnce();
    expect(screen.getByTestId('auth-status')).toHaveTextContent('not-authenticated');
    expect(screen.queryByTestId('user-email')).toHaveTextContent('');
  });

  // 7. isPremium_forStandardUser_shouldBeFalse
  it('isPremium_forStandardUser_shouldBeFalse', async () => {
    // Arrange
    mockGetMe.mockResolvedValue({ ...mockUser, role: 'STANDARD' });

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('premium-status')).toHaveTextContent('standard');
    });
  });

  // 8. isPremium_forPremiumUser_shouldBeTrue
  it('isPremium_forPremiumUser_shouldBeTrue', async () => {
    // Arrange
    mockGetMe.mockResolvedValue({ ...mockUser, role: 'PREMIUM' });

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('premium-status')).toHaveTextContent('premium');
    });
  });

  // 9. isPremium_forAdminUser_shouldBeTrue
  it('isPremium_forAdminUser_shouldBeTrue', async () => {
    // Arrange
    mockGetMe.mockResolvedValue({ ...mockUser, role: 'ADMIN' });

    // Act
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('premium-status')).toHaveTextContent('premium');
    });
  });

  // 10. useAuth_outsideProvider_shouldThrowError
  it('useAuth_outsideProvider_shouldThrowError', () => {
    // Arrange
    // Suppress console.error for the expected error
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });

    const BuggyComponent = () => {
      useAuth();
      return null;
    };

    // Act & Assert
    expect(() => render(<BuggyComponent />)).toThrow('useAuth must be used within an AuthProvider');

    consoleSpy.mockRestore();
  });
});
