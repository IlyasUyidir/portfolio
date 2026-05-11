import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ErrorBoundary } from './ErrorBoundary';

// Component that throws on demand
const ThrowingComponent = ({ shouldThrow }: { shouldThrow: boolean }) => {
  if (shouldThrow) {
    throw new Error('Test render error');
  }
  return <div>Normal content</div>;
};

describe('ErrorBoundary', () => {
  // Suppress console.error for expected errors in these tests
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('ErrorBoundary_whenNoError_shouldRenderChildren: renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <div>Hello</div>
      </ErrorBoundary>
    );
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('ErrorBoundary_whenChildThrows_shouldRenderFallbackUI: renders default fallback UI on error', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>
    );
    expect(screen.getByText(/erreur inattendue/i)).toBeInTheDocument();
    expect(screen.queryByText('Normal content')).not.toBeInTheDocument();
  });

  it('ErrorBoundary_whenChildThrows_shouldShowErrorMessage: displays the error message', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>
    );
    expect(screen.getByText('Test render error')).toBeInTheDocument();
  });

  it('ErrorBoundary_retryButton_shouldResetErrorState: resets error state when retry button is clicked', () => {
    const { rerender } = render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByText(/erreur inattendue/i)).toBeInTheDocument();

    // Update props to be healthy, but ErrorBoundary still shows error UI because of its internal state
    rerender(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={false} />
      </ErrorBoundary>
    );

    // Click retry to clear ErrorBoundary's state
    const retryButton = screen.getByText(/réessayer/i);
    fireEvent.click(retryButton);

    expect(screen.getByText('Normal content')).toBeInTheDocument();
    expect(screen.queryByText(/erreur inattendue/i)).not.toBeInTheDocument();
  });

  it('ErrorBoundary_shouldCallComponentDidCatch: calls componentDidCatch lifecycle method', () => {
    const spy = vi.spyOn(ErrorBoundary.prototype, 'componentDidCatch');
    
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(spy).toHaveBeenCalled();
    const [error] = spy.mock.calls[0];
    expect(error.message).toBe('Test render error');
  });

  it('ErrorBoundary_withCustomFallback_shouldRenderCustomFallback: renders custom fallback prop when provided', () => {
    const customFallback = <div>Custom error UI</div>;
    render(
      <ErrorBoundary fallback={customFallback}>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByText('Custom error UI')).toBeInTheDocument();
    expect(screen.queryByText(/erreur inattendue/i)).not.toBeInTheDocument();
  });
});
