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

1. **Prerequisites**:
   - Java 11+
   - Node.js 16+
   - MySQL 8.0+
   - Maven 3.6+

2. **Environment Setup**:
   Create environment variables for sensitive data:
   ```bash
   # Database credentials
   export DB_USERNAME=your_mysql_username
   export DB_PASSWORD=your_mysql_password

   # API Keys
   export OPENAI_API_KEY=your_openai_api_key
   export HUGGINGFACE_API_KEY=your_huggingface_api_key
   export SECURITY_JWT_SECRET=your_jwt_secret_key
   ```

3. **Database Setup**:
   - Create a MySQL database named `skillmap`
   - The application will auto-create tables on startup

4. **Backend Setup**:
   ```bash
   cd backend/demo
   mvn clean install
   mvn spring-boot:run
   ```

5. **Frontend Setup**:
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