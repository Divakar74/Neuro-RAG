jest.mock('axios');
jest.mock('../services/assessmentService');

import React from 'react';
import { render } from '@testing-library/react';
import Dashboard from './Dashboard';
import { fetchAssessmentProgress } from '../services/assessmentService';

// Mock child components to avoid complex dependencies
jest.mock('./HeroSection', () => {
  return function MockHeroSection() {
    return <div data-testid="hero-section">Hero Section</div>;
  };
});

jest.mock('./SkillRadarChart', () => {
  return function MockSkillRadarChart() {
    return <div data-testid="skill-radar-chart">Skill Radar Chart</div>;
  };
});

jest.mock('./SkillGraph', () => {
  return function MockSkillGraph() {
    return <div data-testid="skill-graph">Skill Graph</div>;
  };
});

jest.mock('./StrengthsCard', () => {
  return function MockStrengthsCard() {
    return <div data-testid="strengths-card">Strengths Card</div>;
  };
});

jest.mock('./GrowthAreasCard', () => {
  return function MockGrowthAreasCard() {
    return <div data-testid="growth-areas-card">Growth Areas Card</div>;
  };
});

jest.mock('./LevelBadge', () => {
  return function MockLevelBadge() {
    return <div data-testid="level-badge">Level Badge</div>;
  };
});



const mockSkillData = [
  { id: 1, name: 'JavaScript', level: 4 },
  { id: 2, name: 'React', level: 3 },
  { id: 3, name: 'Node.js', level: 2 }
];

const mockSkillDependencies = [
  { prerequisiteSkillId: 1, skillId: 2 },
  { prerequisiteSkillId: 2, skillId: 3 }
];

const mockAssessmentSession = {
  sessionToken: 'mock-token',
  userId: 1
};

test('renders Dashboard without crashing', () => {
  expect(() => {
    render(<Dashboard skillData={mockSkillData} skillDependencies={mockSkillDependencies} />);
  }).not.toThrow();
});

test('renders Dashboard with empty data', () => {
  expect(() => {
    render(<Dashboard skillData={[]} skillDependencies={[]} />);
  }).not.toThrow();
});

test('renders all dashboard components', () => {
  const { getByTestId } = render(<Dashboard skillData={mockSkillData} skillDependencies={mockSkillDependencies} />);

  expect(getByTestId('hero-section')).toBeInTheDocument();
  expect(getByTestId('skill-radar-chart')).toBeInTheDocument();
  expect(getByTestId('skill-graph')).toBeInTheDocument();
  expect(getByTestId('strengths-card')).toBeInTheDocument();
  expect(getByTestId('growth-areas-card')).toBeInTheDocument();
});
