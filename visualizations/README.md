# Neuro RAG System Evaluation Visualizations

## Project Overview

Neuro RAG is an advanced competency assessment and behavioral evaluation framework that leverages Large Language Models (LLMs) combined with symbolic reasoning to provide personalized skill development insights. Unlike traditional ML-based systems that require extensive training data and focus on accuracy/loss metrics, Neuro RAG emphasizes qualitative evaluation of reasoning quality, adaptability, and user-centric performance.

### Core Architecture

The system operates through a multi-layered architecture:

1. **User Interface Layer**: React-based frontend (neurorag/) providing interactive assessment flows, dashboards, and progress tracking
2. **API Layer**: Spring Boot backend (backend/demo/) handling authentication, assessment orchestration, and data persistence
3. **Processing Layer**:
   - **Question Engine**: Manages technical MCQs, coding challenges, and behavioral prompts from JSON datasets
   - **LLM Reasoner**: Integrates OpenAI GPT models for response analysis and feedback generation
   - **Symbolic Context Graph**: Builds and maintains user skill relationships using graph databases
   - **Personalized Feedback Engine**: Generates adaptive suggestions based on user history and skill gaps

### Key Features

- **Dual Assessment Types**:
  - Technical: Multiple choice questions, coding/typing exercises
  - Behavioral: Scenario-based prompts evaluating soft skills

- **Data Management**:
  - User response storage with session tracking
  - Resume parsing using NLP (Gemini, HuggingFace NER)
  - AI response caching for performance optimization

- **Adaptive Learning**:
  - Symbolic skill graph construction
  - Dependency mapping between competencies
  - Personalized roadmap generation

### Evaluation Framework

Since Neuro RAG doesn't involve model training, evaluation focuses on:
- **Reasoning Quality**: Relevance, personalization, clarity, behavioral fit
- **System Performance**: Functional test coverage, response times, user satisfaction
- **Explainability**: How past responses influence current feedback
- **Adaptability**: Improvement over base LLM responses through symbolic integration

### Technology Stack

- **Backend**: Java Spring Boot, JPA/Hibernate, PostgreSQL
- **Frontend**: React.js, Tailwind CSS, Chart.js for basic visualizations
- **AI/ML**: OpenAI API, HuggingFace Transformers, Custom NER models
- **Data**: JSON datasets for questions, graph databases for skill relationships
- **Infrastructure**: Docker, JWT authentication, CORS configuration

### Datasets

- `technical_mcqs.json`: Technical assessment questions
- `behavioral_prompts.json`: Behavioral evaluation scenarios
- `system_design_mcqs.json`: System design competencies

This framework represents a shift from traditional ML evaluation (accuracy/loss) to qualitative assessment of AI-assisted human development systems, focusing on personalization, adaptability, and explainable reasoning.
