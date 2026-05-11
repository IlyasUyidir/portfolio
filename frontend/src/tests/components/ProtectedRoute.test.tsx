import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '../../components/ProtectedRoute';
import { useAuth } from '../../hooks/useAuth';

// Mock the useAuth hook
vi.mock('../../hooks/useAuth');
const mockUseAuth = vi.mocked(useAuth);

// Mock AppShell to simplify tests
vi.mock('../../components/layout/AppShell', () => ({
  AppShell: () => <div data-testid="app-shell">App Shell Content</div>
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('ProtectedRoute_whenLoading_shouldShowLoadingIndicator', () => {
    // Arrange
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      user: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn()
    } as any);

    render(
      <MemoryRouter>
        <ProtectedRoute />
      </MemoryRouter>
    );

    // Act & Assert
    expect(screen.getByText(/chargement/i)).toBeInTheDocument();
  });

  it('ProtectedRoute_whenAuthenticatedAndNotLoading_shouldRenderAppShell', () => {
    // Arrange
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      user: { id: 1, email: 'test@folio.io', username: 'testuser' },
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn()
    } as any);

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<div>Dashboard Page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    // Act & Assert
    // ProtectedRoute renders AppShell which contains an Outlet
    // Our mock AppShell renders "App Shell Content"
    expect(screen.getByTestId('app-shell')).toBeInTheDocument();
  });

  it('ProtectedRoute_whenNotAuthenticated_shouldRedirectToLogin', () => {
    // Arrange
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      user: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn()
    } as any);

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<div>Dashboard Page</div>} />
          </Route>
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    // Act & Assert
    expect(screen.queryByText('Dashboard Page')).not.toBeInTheDocument();
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });
});
