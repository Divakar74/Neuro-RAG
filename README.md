# Neuro-RAG

## AI-Powered Skill Assessment and Learning Roadmap Platform

Neuro-RAG is an innovative web application that leverages advanced AI technologies to provide personalized skill assessments and learning roadmaps. Built with modern web technologies and cutting-edge AI models, it helps users identify skill gaps and create tailored development plans.

### Key Features

- **Intelligent Resume Analysis**: AI-powered parsing using Gemini and Hugging Face NER
- **Adaptive Assessments**: Dynamic question generation based on cognitive analysis
- **Personalized Roadmaps**: Automated learning path creation with milestones
- **Progress Tracking**: Comprehensive dashboard for skill development monitoring
- **Secure Platform**: JWT-based authentication and data protection

### Technology Stack

- **Frontend**: React.js, Tailwind CSS
- **Backend**: Spring Boot, Java, MySQL
- **AI/ML**: OpenAI GPT, Hugging Face Transformers, RAG implementation

### Quick Start

1. **Backend Setup**:
   ```bash
   cd backend/demo
   # Configure database and API keys
   mvn spring-boot:run
   ```

2. **Frontend Setup**:
   ```bash
   cd neurorag
   npm install
   npm start
   ```

### Project Structure

- `backend/` - Spring Boot REST API
- `neurorag/` - React frontend application

For detailed setup instructions, see the [frontend README](neurorag/README.md).

### Contributing

We welcome contributions! Please see the contributing guidelines in the frontend README.

### License

MIT License - see LICENSE file for details.