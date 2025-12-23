import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import QuestionCard from '../QuestionCard';

describe('QuestionCard Component', () => {
  const mockQuestionText = 'What is your favorite color?';
  const mockMCQQuestion = {
    id: 1,
    questionText: mockQuestionText,
    questionType: 'mcq',
    choices: ['Red', 'Blue', 'Green'],
  };
  const mockMCQQuestionObjectChoices = {
    id: 2,
    questionText: mockQuestionText,
    questionType: 'mcq',
    choices: [
      { text: 'Red', value: 'red' },
      { text: 'Blue', value: 'blue' },
      { text: 'Green', value: 'green' }
    ],
  };
  const mockTextQuestion = {
    id: 3,
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

  test('renders MCQ question with string choices', () => {
    render(<QuestionCard question={mockMCQQuestion} onSubmit={mockOnSubmit} loading={false} />);
    expect(screen.getByText(mockMCQQuestion.questionText)).toBeInTheDocument();
    expect(screen.getByText('Red')).toBeInTheDocument();
    expect(screen.getByText('Blue')).toBeInTheDocument();
    expect(screen.getByText('Green')).toBeInTheDocument();
  });

  test('renders MCQ question with object choices', () => {
    render(<QuestionCard question={mockMCQQuestionObjectChoices} onSubmit={mockOnSubmit} loading={false} />);
    expect(screen.getByText(mockMCQQuestionObjectChoices.questionText)).toBeInTheDocument();
    expect(screen.getByText('Red')).toBeInTheDocument();
    expect(screen.getByText('Blue')).toBeInTheDocument();
    expect(screen.getByText('Green')).toBeInTheDocument();
  });

  test('submit button disabled when no response for text question', () => {
    render(<QuestionCard question={mockTextQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    expect(submitButton).toBeDisabled();
  });

  test('submit button disabled when no choice selected for MCQ', () => {
    render(<QuestionCard question={mockMCQQuestion} onSubmit={mockOnSubmit} loading={false} />);
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
    const optionButton = screen.getByText('Red');
    fireEvent.click(optionButton);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    expect(submitButton).toBeEnabled();
  });

  test('MCQ choice selection works correctly', () => {
    render(<QuestionCard question={mockMCQQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const redButton = screen.getByText('Red');
    const blueButton = screen.getByText('Blue');

    // Click Red
    fireEvent.click(redButton);
    expect(redButton.closest('button')).toHaveClass('bg-indigo-50');

    // Click Blue (should deselect Red and select Blue)
    fireEvent.click(blueButton);
    expect(blueButton.closest('button')).toHaveClass('bg-indigo-50');
    expect(redButton.closest('button')).not.toHaveClass('bg-indigo-50');
  });

  test('calls onSubmit with correct data for text question', () => {
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

  test('calls onSubmit with correct data for MCQ question', () => {
    render(<QuestionCard question={mockMCQQuestion} onSubmit={mockOnSubmit} loading={false} />);
    const optionButton = screen.getByText('Blue');
    fireEvent.click(optionButton);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitButton);
    expect(mockOnSubmit).toHaveBeenCalled();
    const callArg = mockOnSubmit.mock.calls[0][0];
    expect(callArg.responseChoice).toBe('Blue');
    expect(callArg.questionId).toBe(mockMCQQuestion.id);
  });

  test('calls onSubmit with correct data for MCQ question with object choices', () => {
    render(<QuestionCard question={mockMCQQuestionObjectChoices} onSubmit={mockOnSubmit} loading={false} />);
    const optionButton = screen.getByText('Blue');
    fireEvent.click(optionButton);
    const submitButton = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitButton);
    expect(mockOnSubmit).toHaveBeenCalled();
    const callArg = mockOnSubmit.mock.calls[0][0];
    expect(callArg.responseChoice).toBe('blue'); // Should be the value, not text
    expect(callArg.questionId).toBe(mockMCQQuestionObjectChoices.id);
  });
});
