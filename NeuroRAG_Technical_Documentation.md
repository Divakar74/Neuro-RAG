# NeuroRAG: Intelligent Skill Assessment and Career Roadmap Platform

## Executive Summary

NeuroRAG is a sophisticated web application that combines artificial intelligence, cognitive assessment, and personalized learning to provide intelligent skill assessment and career development guidance. The platform uses advanced AI techniques including OpenAI GPT integration, cognitive bias analysis, and adaptive questioning to deliver personalized career roadmaps.

## System Architecture Overview

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React Frontend│    │ Spring Boot API │    │   H2/MySQL DB   │
│   (Port 3000)   │◄──►│   (Port 8080)   │◄──►│                 │
│                 │    │                 │    │                 │
│ - User Interface│    │ - REST API      │    │ - User Data     │
│ - State Mgmt    │    │ - Business Logic│    │ - Assessments   │
│ - Authentication │    │ - AI Services   │    │ - Questions     │
│ - Assessment Flow│    │ - Security      │    │ - Responses     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
       │                        │                        │
       └────────────────────────┼────────────────────────┘
                                │
                    ┌─────────────────┐
                    │   External APIs │
                    │                 │
                    │ - OpenAI GPT-4  │
                    │ - NLP Services  │
                    │ - Resume Parser │
                    └─────────────────┘
```

## Backend Architecture (Spring Boot)

### Core Components

#### 1. Application Layer
- **Main Class**: `SkillMapApplication.java`
  - Standard Spring Boot application with `@SpringBootApplication`
  - Enables auto-configuration and component scanning

#### 2. Security Configuration
- **JwtAuthFilter**: Custom JWT authentication filter
  - Intercepts requests to validate JWT tokens
  - Extracts user information from tokens
  - Handles authentication failures gracefully
- **SecurityConfig**: Spring Security configuration
  - JWT-based authentication
  - CORS configuration for frontend integration
  - Role-based access control
  - Protected endpoints: `/api/assessment/**`, `/api/questions/**`, etc.

#### 3. Data Model (JPA Entities)

##### Core Entities:

**User Entity**
```java
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String firstName, lastName;
    private Boolean isActive = true;
    private LocalDateTime createdAt, updatedAt;

    @OneToMany(mappedBy = "user")
    private List<AssessmentSession> assessmentSessions;
}
```

**AssessmentSession Entity**
```java
@Entity @Table(name = "assessment_sessions")
public class AssessmentSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String sessionToken;
    private String targetRole;
    @Enumerated(EnumType.STRING)
    private Status status; // in_progress, completed, abandoned
    private Boolean resumeUploaded;
    private Integer questionsAsked;
    private LocalDateTime startedAt, completedAt;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;
    @OneToMany(mappedBy = "session")
    private List<Response> responses;
}
```

**Question Entity**
```java
@Entity @Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "skill_id")
    private Skill skill;
    @Column(columnDefinition = "TEXT")
    private String questionText;
    private String questionType; // "text" or "mcq"
    private String difficulty; // "Easy", "Intermediate", "Advanced"
    @Column(columnDefinition = "JSON")
    private String options; // For MCQ questions
    private String correctAnswer;
    private String topic, explanation;
    private Integer timesAsked, avgResponseTime;
}
```

**Response Entity**
```java
@Entity @Table(name = "responses")
public class Response {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "session_id")
    private AssessmentSession session;
    @ManyToOne @JoinColumn(name = "question_id")
    private Question question;
    @Column(columnDefinition = "TEXT")
    private String responseText;

    // Cognitive analysis fields
    private Double specificityScore, depthScore;
    private Double confidenceLevel, consistencyScore;
    private Double cognitiveBiasScore, timePressureScore;
    private Double typingSpeedWpm, similarityScore;
    private Boolean isCorrect; // For MCQ validation
}
```

**Skill Entity**
```java
@Entity @Table(name = "skills")
public class Skill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String skillCode;
    private String displayName;
    @Enumerated(EnumType.STRING)
    private Category category; // Programming, Systems, Data, Cloud, Soft_Skills
    private Double importanceWeight;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "JSON")
    private String levelDescriptors, keywords;
}
```

#### 4. Service Layer

##### OpenAIService
- **Purpose**: AI-powered content generation and analysis
- **Key Features**:
  - GPT-4 integration with caching mechanism
  - Personalized suggestion generation
  - Cognitive bias analysis
  - Multi-layered context engineering
  - Response caching with TTL (24 hours default)
  - Token usage tracking

**Core Methods**:
- `generateSuggestion()`: Basic AI suggestions
- `generateDetailedSuggestionsWithEvidence()`: Evidence-based recommendations
- `generateNeuroRAGTailoredFeedback()`: Cognitive-aware feedback
- `buildUserProfileContext()`: Comprehensive user profiling
- `buildResponseEvidenceContext()`: Response analysis

##### UserService
- User registration and authentication
- JWT token management
- Password encryption (BCrypt)

##### Assessment Services
- Question selection and adaptive testing
- Response analysis and scoring
- Skill gap identification
- Roadmap generation

#### 5. Controller Layer

##### AuthController (`/api/auth`)
- `POST /register`: User registration
- `POST /login`: JWT token generation
- `POST /logout`: Token invalidation
- `GET /validate`: Token validation

##### AssessmentController (`/api/assessment`)
- `POST /start`: Initialize assessment session
- `GET /user/sessions`: Retrieve user assessment history
- `POST /complete/{token}`: Finalize assessment

##### QuestionController (`/api/questions`)
- `GET /next/{sessionToken}`: Adaptive question selection
- `GET /progress/{sessionToken}`: Assessment progress tracking
- `GET /stopping-criteria/{sessionToken}`: Assessment completion criteria

##### ResponseController (`/api/responses`)
- `POST /`: Submit user responses
- `GET /session/{sessionId}`: Retrieve session responses

##### AIController (`/api/ai`)
- `GET /suggestions/{sessionId}`: AI-powered recommendations
- `POST /analyze`: Cognitive analysis requests

## Frontend Architecture (React)

### Core Components

#### 1. Application Structure
```
src/
├── components/          # Reusable UI components
├── contexts/           # React Context for state management
├── services/           # API service layer
├── hooks/             # Custom React hooks
├── App.js             # Main application component
└── index.js           # Application entry point
```

#### 2. State Management
- **UserContext**: Global authentication state
  - JWT token management
  - User data persistence
  - Automatic token validation
  - Login/logout functionality

#### 3. Key Components

**App.js**: Main application orchestrator
- Route management (landing → assessment → dashboard → roadmap)
- Authentication flow
- Resume upload and role selection
- State persistence in localStorage

**AssessmentFlow.js**: Core assessment interface
- Question presentation
- Response collection
- Progress tracking
- Real-time feedback

**EnhancedDashboard.js**: Results visualization
- Skill radar charts
- Performance metrics
- AI suggestions display
- Roadmap preview

**QuestionCard.js**: Individual question component
- Text input with auto-save
- MCQ option selection
- Confidence rating
- Typing speed tracking

#### 4. Service Layer

**assessmentService.js**
- Question fetching and submission
- Session management
- Progress tracking
- AI suggestions integration

**resumeService.js**
- File upload handling
- Resume parsing integration
- Data validation

**cognitiveService.js**
- Cognitive analysis requests
- Bias detection
- Learning pattern analysis

## Data Flow Architecture

### User Journey Flow

```
1. User Registration/Login
   ↓
2. Resume Upload & Parsing
   ↓
3. Role Selection
   ↓
4. Assessment Session Creation
   ↓
5. Adaptive Questioning Loop
   │
   ├── Question Selection (AI-driven)
   │
   ├── User Response Collection
   │   ├── Text Analysis
   │   ├── Cognitive Metrics
   │   └── Real-time Validation
   │
   └── Stopping Criteria Check
       ├── Minimum Questions
       ├── Confidence Thresholds
       └── Skill Coverage
   ↓
6. Assessment Completion
   ↓
7. AI Analysis & Processing
   │
   ├── Skill Level Calculation
   ├── Cognitive Bias Analysis
   ├── Performance Metrics
   └── Gap Identification
   ↓
8. Personalized Recommendations
   ↓
9. Roadmap Generation
   ↓
10. Dashboard Presentation
```

### API Data Flow

```
Frontend Request → JWT Filter → Controller → Service → Repository → Database
                      ↓
                Token Validation
                      ↓
            Security Context Setup
                      ↓
               Business Logic
                      ↓
            Response Generation
                      ↓
Frontend Response ← JSON Serialization ← Controller ← Service ← Repository
```

## AI Integration Architecture

### OpenAI Integration

#### Prompt Engineering Strategy
1. **System Prompts**: Define AI persona and behavior
2. **Context Building**: Multi-layered user profiling
3. **Evidence-Based Reasoning**: Response analysis integration
4. **Personalization**: Cognitive pattern adaptation

#### Caching Mechanism
- SHA-256 hash-based prompt deduplication
- TTL-based cache expiration (configurable)
- Hit rate tracking and analytics
- User-specific and global cache levels

### Cognitive Analysis Framework

#### NeuroRAG Features
- **Cognitive Bias Detection**: Anchoring, overconfidence, fatigue
- **Learning Pattern Analysis**: Response consistency, thinking time
- **Behavioral Insights**: Typing speed, edit patterns, paste detection
- **Adaptive Recommendations**: Personalized learning strategies

#### Analysis Components
1. **Response Analysis**: Text similarity, keyword matching
2. **Performance Metrics**: Accuracy, speed, confidence correlation
3. **Skill Graph Updates**: Dynamic skill level adjustments
4. **Feedback Generation**: Context-aware suggestions

## Database Schema

### Core Tables Relationship

```
users (1) ──── (N) assessment_sessions
    │                    │
    │                    ├── (N) responses
    │                    ├── (N) skill_assessments
    │                    ├── (N) roadmaps
    │                    └── (N) resume_data
    │
    └── (1) user_profiles

skills (1) ──── (N) questions
    │
    ├── (N) skill_dependencies
    └── (N) resources

questions (1) ──── (N) responses

roadmaps (1) ──── (N) roadmap_resources (N) ──── (1) resources
```

### Key Relationships
- **Users** have multiple assessment sessions
- **Sessions** contain multiple responses and skill assessments
- **Questions** are linked to skills and have multiple responses
- **Skills** have dependencies and associated resources
- **Roadmaps** are generated per session with resource recommendations

## Security Implementation

### Authentication Flow
```
1. User Login → Password Verification
2. JWT Token Generation → Client Storage
3. Request Interception → Token Validation
4. Security Context → User Authorization
5. Protected Resource Access → Response
```

### Security Features
- **JWT Tokens**: Stateless authentication with expiration
- **Password Encryption**: BCrypt hashing
- **CORS Configuration**: Frontend-backend communication
- **Request Validation**: Input sanitization and validation
- **Session Management**: Secure session token handling

## Configuration Management

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:h2:mem:skillmap
spring.jpa.hibernate.ddl-auto=update

# Security
security.jwt.secret=${JWT_SECRET}
security.jwt.expiration=2592000000

# AI Integration
openai.api.key=${OPENAI_API_KEY}
ai.cache.enabled=true
ai.cache.ttl.hours=24

# External Services
resume.ner.hf.enabled=${HUGGINGFACE_ENABLED}
huggingface.api.key=${HUGGINGFACE_API_KEY}
```

### Environment Variables
- `DB_USERNAME`, `DB_PASSWORD`: Database credentials
- `JWT_SECRET`: JWT signing key
- `OPENAI_API_KEY`: OpenAI API access
- `HUGGINGFACE_API_KEY`: NLP service access

## Deployment Architecture

### Development Environment
- **Frontend**: React dev server (Port 3000)
- **Backend**: Spring Boot embedded Tomcat (Port 8080)
- **Database**: H2 in-memory database
- **Build Tools**: Maven, npm

### Production Considerations
- **Database**: MySQL/PostgreSQL migration
- **Caching**: Redis for AI response caching
- **Load Balancing**: Nginx reverse proxy
- **Containerization**: Docker deployment
- **Monitoring**: Application metrics and logging

## Performance Optimization

### Backend Optimizations
- **Database Indexing**: Optimized queries for assessment data
- **AI Caching**: Response deduplication and TTL management
- **Connection Pooling**: HikariCP configuration
- **Async Processing**: Background task handling

### Frontend Optimizations
- **Code Splitting**: Route-based component loading
- **State Management**: Efficient context usage
- **API Optimization**: Request batching and caching
- **Progressive Loading**: Assessment step optimization

## Monitoring and Analytics

### Application Metrics
- **Assessment Completion Rates**: Session tracking
- **Question Response Times**: Performance analysis
- **AI Cache Hit Rates**: Optimization monitoring
- **User Engagement**: Session duration and interaction

### Error Handling
- **Global Exception Handling**: Consistent error responses
- **Logging**: Structured logging with SLF4J
- **Validation**: Input validation and sanitization
- **Fallback Mechanisms**: Graceful degradation

## Future Enhancements

### Planned Features
- **Machine Learning Models**: Custom skill assessment algorithms
- **Real-time Collaboration**: Multi-user assessment sessions
- **Advanced Analytics**: Predictive career trajectory modeling
- **Mobile Application**: React Native implementation
- **Integration APIs**: Third-party LMS and HR system integration

### Scalability Improvements
- **Microservices Architecture**: Service decomposition
- **Event-Driven Processing**: Asynchronous task handling
- **Distributed Caching**: Redis cluster implementation
- **Database Sharding**: Horizontal scaling support

## Conclusion

NeuroRAG represents a sophisticated integration of modern web technologies, artificial intelligence, and cognitive assessment methodologies. The platform's architecture supports scalable, personalized skill development through intelligent questioning, comprehensive analysis, and adaptive learning recommendations. The combination of Spring Boot's robustness, React's interactivity, and OpenAI's advanced language models creates a powerful tool for career development and skill assessment.