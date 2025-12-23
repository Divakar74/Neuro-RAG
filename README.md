# Neuro-RAG: AI-Powered Skill Assessment and Learning Roadmap Platform

Neuro-RAG is an innovative web application that leverages advanced AI technologies, including Retrieval-Augmented Generation (RAG) and cognitive analysis, to provide personalized skill assessments and learning roadmaps for users. The platform integrates resume parsing, adaptive questioning, and AI-driven insights to help users identify skill gaps and create tailored development plans.

## Features

- **Resume Analysis**: Upload and parse resumes using AI-powered NLP models (Gemini, Hugging Face NER)
- **Adaptive Skill Assessment**: Dynamic question generation based on user responses and cognitive analysis
- **AI-Powered Insights**: Cognitive bias detection, skill gap analysis, and personalized recommendations
- **Learning Roadmaps**: Automated generation of structured learning paths with milestones and resources
- **User Dashboard**: Comprehensive progress tracking and performance analytics
- **Secure Authentication**: JWT-based user authentication and session management

## Technology Stack

### Frontend
- React.js with modern hooks and context API
- Tailwind CSS for responsive styling
- Axios for API communication
- React Router for navigation

### Backend
- Spring Boot (Java) with Spring Security
- MySQL database with JPA/Hibernate
- JWT authentication
- RESTful API design

### AI/ML Integration
- OpenAI GPT models for question generation and analysis
- Hugging Face transformers for NER and NLP tasks
- Custom cognitive analysis algorithms
- RAG implementation for enhanced AI responses

## Prerequisites

- Node.js (v16 or higher)
- Java 11 or higher
- MySQL 8.0 or higher
- Maven 3.6+

## Installation and Setup

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend/demo
   ```

2. Configure database:
   - Create a MySQL database named `skillmap`
   - Update `src/main/resources/application.properties` with your database credentials:
     ```properties
     spring.datasource.username=your_db_username
     spring.datasource.password=your_db_password
     ```

3. Set environment variables for API keys:
   ```bash
   export OPENAI_API_KEY=your_openai_api_key
   export HUGGINGFACE_API_KEY=your_huggingface_api_key
   export SECURITY_JWT_SECRET=your_jwt_secret
   ```

4. Build and run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd neurorag
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

The application will be available at `http://localhost:3000`

## API Endpoints

- `POST /api/auth/login` - User authentication
- `POST /api/auth/register` - User registration
- `POST /api/resume/upload` - Resume upload and parsing
- `GET /api/assessment/start` - Start skill assessment
- `POST /api/assessment/submit` - Submit assessment responses
- `GET /api/roadmap/{userId}` - Get personalized learning roadmap

## Project Structure

```
Neuro-RAG/
├── backend/                 # Spring Boot backend
│   └── demo/
│       ├── src/main/java/com/skillmap/
│       │   ├── controller/  # REST controllers
│       │   ├── service/     # Business logic
│       │   ├── model/       # Entity models
│       │   └── config/      # Configuration classes
│       └── src/main/resources/
│           └── application.properties
├── neurorag/                # React frontend
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── services/        # API services
│   │   └── contexts/        # React contexts
│   ├── public/              # Static assets
│   └── package.json
└── README.md
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built with Spring Boot and React
- AI capabilities powered by OpenAI and Hugging Face
- UI components styled with Tailwind CSS
