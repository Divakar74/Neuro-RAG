import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import QuestionCard from '../QuestionCardUpdated';

describe('QuestionCardUpdated Component', () => {
  const mockQuestionText = 'What is your favorite color?';
  const mockMCQQuestion = {
    id: 1,
    questionText: mockQuestionText,
    questionType: 'mcq',
    options: JSON.stringify(['Red', 'Blue', 'Green']),
  };
  const mockTextQuestion = {
    id: 2,
    questionText: 'Describe your experience with React.',
    questionType: 'text',
  };

  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    mockOnSubmit.mockClear();
  });

  test('renders text question and allows typing', () => {
    render(<QuestionCard question={mockTextQuestion} onSubmit={mockOnSubmit} loading={false} />);
    expect(screen.getByText(mockTextQuestion.questionText)).toBeInTheDocument();
    const textarea = screen.getByPlaceholderText(/type your response here/i);
    fireEvent.change(textarea, { target: { value: 'I love React' } });
    expect(textarea.value).toBe('I love React');
  });

  test('renders MCQ question and options', () => {
    render(<QuestionCard question={mockMCQQuestion} onSubmit={mockOnSubmit} loading={false} />);
    expect(screen.getByText(mockMCQQuestion.questionText)).toBeInTheDocument();
    expect(screen.getByText(/select your answer/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Red')).toBeInTheDocument();
    expect(screen.getByLabelText('Blue')).toBeInTheDocument();
    expect(screen.getByLabelText('Green')).toBeInTheDocument();
  });

  test('submit button disabled when no response', () => {
    render(<QuestionCard question={mockTextQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    expect(submitButton).toBeDisabled();
  });

  test('submit button enabled when text response entered', () => {
    render(<QuestionCard question={mockTextQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const textarea = screen.getByPlaceholderText(/type your response here/i);
    fireEvent.change(textarea, { target: { value: 'Some answer' } });
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    expect(submitButton).toBeEnabled();
  });

  test('submit button enabled when MCQ option selected', () => {
    render(<QuestionCard question={mockMCQQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const option = screen.getByLabelText('Red');
    fireEvent.click(option);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    expect(submitButton).toBeEnabled();
  });

  test('calls onSubmit with correct data on submit', () => {
    render(<QuestionCard question={mockTextQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const textarea = screen.getByPlaceholderText(/type your response here/i);
    fireEvent.change(textarea, { target: { value: 'Answer text' } });
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitButton);
    expect(mockOnSubmit).toHaveBeenCalled();
    const callArg = mockOnSubmit.mock.calls[0][0];
    expect(callArg.responseText).toBe('Answer text');
    expect(callArg.questionId).toBe(mockTextQuestion.id);
  });
});
