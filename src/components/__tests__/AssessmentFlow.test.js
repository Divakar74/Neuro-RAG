import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AssessmentFlow from '../AssessmentFlow';
import * as useAssessmentHook from '../../hooks/useAssessment';

const mockSession = {
  id: 1,
  sessionToken: 'token123',
};

const mockQuestion = {
  id: 1,
  questionText: 'What is 2 + 2?',
  questionType: 'mcq',
  options: JSON.stringify(['3', '4', '5']),
};

const mockUseAssessment = {
  currentQuestion: mockQuestion,
  progress: { completed: 0, total: 1 },
  shouldStop: false,
  stopReason: null,
  loading: false,
  submitResponse: jest.fn(),
};

describe('AssessmentFlow Component', () => {
  beforeEach(() => {
    jest.spyOn(useAssessmentHook, 'useAssessment').mockReturnValue(mockUseAssessment);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('renders question and progress tracker', () => {
    render(<AssessmentFlow session={mockSession} />);
    expect(screen.getByText(/What is 2 \+ 2\?/i)).toBeInTheDocument();
    expect(screen.getByText(/Take your time to think deeply/i)).toBeInTheDocument();
  });

  test('submits response when form is submitted', async () => {
    render(<AssessmentFlow session={mockSession} />);
    const option = screen.getByText('4');
    fireEvent.click(option);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(mockUseAssessment.submitResponse).toHaveBeenCalled();
    });
  });

  test('shows loading state when submitting', () => {
    const loadingMock = { ...mockUseAssessment, loading: true };
    jest.spyOn(useAssessmentHook, 'useAssessment').mockReturnValue(loadingMock);
    render(<AssessmentFlow session={mockSession} />);
    expect(screen.getByText(/Submitting\.\.\./i)).toBeInTheDocument();
  });

  test('shows stop message when assessment should stop', () => {
    const stopMock = { ...mockUseAssessment, shouldStop: true, stopReason: 'Assessment completed' };
    jest.spyOn(useAssessmentHook, 'useAssessment').mockReturnValue(stopMock);
    render(<AssessmentFlow session={mockSession} />);
    expect(screen.getByText(/Assessment completed/i)).toBeInTheDocument();
  });
});
