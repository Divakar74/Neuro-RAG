-- =======================
-- SEED DATA
-- =======================

-- =======================
-- SEED DATA
-- =======================

-- Skills
INSERT IGNORE INTO skills (skill_code, display_name, category, importance_weight, description, level_descriptors, keywords) VALUES
('java_backend', 'Java Backend Development', 'Programming', 1.5, 'Building scalable backend services with Java', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["Java", "Spring Boot", "Spring", "Hibernate", "JPA"]'),
('rest_api', 'REST API Design', 'Programming', 1.3, 'Designing and implementing RESTful APIs', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["REST", "API", "HTTP", "JSON", "endpoints"]'),
('database_sql', 'SQL & Database Design', 'Data', 1.4, 'Database design, querying, and optimization', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["SQL", "MySQL", "PostgreSQL", "database", "queries"]'),
('system_design', 'System Design', 'Systems', 1.5, 'Designing scalable distributed systems', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["scalability", "architecture", "distributed", "microservices", "load balancing"]'),
('cloud_aws', 'AWS Cloud Services', 'Cloud', 1.2, 'Working with Amazon Web Services', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["AWS", "EC2", "S3", "Lambda", "CloudFormation"]'),
('docker_k8s', 'Docker & Kubernetes', 'Cloud', 1.3, 'Container orchestration and deployment', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["Docker", "Kubernetes", "containers", "pods", "deployment"]'),
('testing', 'Unit & Integration Testing', 'Programming', 1.1, 'Writing and maintaining test suites', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["JUnit", "testing", "TDD", "mocking", "integration tests"]'),
('git_version', 'Git Version Control', 'Programming', 1.0, 'Version control and collaboration workflows', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["Git", "GitHub", "version control", "branches", "pull request"]'),
('problem_solving', 'Problem Solving', 'Soft_Skills', 1.4, 'Analytical thinking and debugging', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["debugging", "algorithms", "troubleshooting", "optimization"]'),
('communication', 'Technical Communication', 'Soft_Skills', 1.2, 'Explaining technical concepts clearly', '{"1": "Basic", "2": "Intermediate", "3": "Advanced", "4": "Expert", "5": "Master"}', '["documentation", "presentation", "communication", "collaboration"]');

-- Dependencies
INSERT IGNORE INTO skill_dependencies (parent_skill_id, child_skill_id, weight, dependency_type) VALUES
(1, 2, 0.8, 'prerequisite'),
(1, 3, 0.6, 'complementary'),
(2, 4, 0.9, 'prerequisite'),
(3, 4, 0.7, 'prerequisite'),
(5, 6, 0.8, 'prerequisite'),
(4, 6, 0.6, 'complementary'),
(1, 7, 0.5, 'complementary');

-- Sample Questions (Mixed Text and MCQ)
INSERT IGNORE INTO questions (skill_id, question_text, question_type, difficulty, difficulty_level, expected_keywords, level_indicators, suggested_answer_length, context_hint, options, correct_answer, topic, explanation) VALUES
-- Text Questions
(1, 'Describe how you would implement a REST API endpoint for user registration in Spring Boot.', 'text', 'Intermediate', 2.5, '["Spring Boot", "REST", "Controller", "RequestMapping", "POST"]', '{"2": ["@RestController", "@PostMapping"], "3": ["@Valid", "DTO", "Service"], "4": ["Security", "Validation", "Error Handling"]}', 200, 'We\'re testing your Spring Boot and REST API knowledge.', NULL, NULL, 'Spring Boot', NULL),
(2, 'Explain the difference between PUT and PATCH HTTP methods in REST APIs.', 'text', 'Easy', 2.0, '["PUT", "PATCH", "HTTP", "REST", "update"]', '{"2": ["PUT replaces", "PATCH modifies"], "3": ["idempotent", "partial update"]}', 150, 'Focus on the semantic differences and use cases.', NULL, NULL, 'REST APIs', NULL),
(3, 'How would you optimize a slow SQL query that joins multiple tables?', 'text', 'Advanced', 3.0, '["EXPLAIN", "INDEX", "JOIN", "optimization", "query"]', '{"3": ["EXPLAIN PLAN", "composite index", "query rewrite"], "4": ["execution plan", "statistics", "partitioning"]}', 250, 'Consider indexing, query structure, and execution plans.', NULL, NULL, 'SQL Optimization', NULL),

-- MCQ Questions
(1, 'Which annotation is used to create a REST controller in Spring Boot?', 'mcq', 'Easy', 1.5, '["@RestController", "Spring", "annotation"]', '{"1": ["@Controller"], "2": ["@RestController"], "3": ["@RestController", "@RequestMapping"]}', 50, 'Basic Spring Boot knowledge test.', '["@Controller", "@RestController", "@Service", "@Repository"]', '@RestController', 'Spring Boot', '@RestController is the correct annotation for creating REST controllers in Spring Boot.'),
(1, 'What HTTP status code should be returned when a new resource is successfully created?', 'mcq', 'Easy', 1.0, '["HTTP", "status code", "created"]', '{"1": ["200"], "2": ["201"], "3": ["201", "Location header"]}', 30, 'HTTP status code knowledge.', '["200 OK", "201 Created", "202 Accepted", "204 No Content"]', '201 Created', 'HTTP Status Codes', '201 Created is the appropriate status code for successful resource creation in REST APIs.'),
(2, 'Which HTTP method is idempotent?', 'mcq', 'Intermediate', 2.0, '["HTTP", "idempotent", "method"]', '{"2": ["PUT"], "3": ["PUT", "GET", "DELETE"]}', 40, 'Understanding of HTTP method properties.', '["POST", "PUT", "PATCH", "All of the above"]', 'PUT', 'HTTP Methods', 'PUT is idempotent because multiple identical requests should have the same effect as a single request.'),
(3, 'What is the primary purpose of an index in a database?', 'mcq', 'Easy', 1.5, '["index", "database", "performance"]', '{"1": ["organization"], "2": ["performance"], "3": ["performance", "query optimization"]}', 40, 'Database indexing fundamentals.', '["To store data", "To improve query performance", "To backup data", "To encrypt data"]', 'To improve query performance', 'Database Indexing', 'Indexes are created to speed up data retrieval operations by providing quick access to rows in a table.'),
(4, 'Which of the following is NOT a valid HTTP status code?', 'mcq', 'Easy', 1.0, '["HTTP", "status code", "valid"]', '{"1": ["recognize valid codes"], "2": ["know common codes"], "3": ["understand status code ranges"]}', 30, 'HTTP status code knowledge.', '["200", "404", "500", "999"]', '999', 'HTTP Status Codes', '999 is not a valid HTTP status code. Valid status codes are in the 100-599 range.'),
(5, 'What does TDD stand for?', 'mcq', 'Easy', 1.0, '["TDD", "testing", "development"]', '{"1": ["Test"], "2": ["Test Driven"], "3": ["Test Driven Development"]}', 20, 'Software development methodology knowledge.', '["Test Driven Development", "Test Data Design", "Test Documentation Development", "Test Driven Design"]', 'Test Driven Development', 'Testing', 'TDD stands for Test Driven Development, a software development approach where tests are written before the code.'),
(6, 'Which Git command is used to create a new branch?', 'mcq', 'Easy', 1.0, '["Git", "branch", "create"]', '{"1": ["git branch"], "2": ["git checkout -b"], "3": ["git branch", "git checkout -b"]}', 25, 'Git version control basics.', '["git new branch", "git create branch", "git checkout -b", "git branch new"]', 'git checkout -b', 'Git', 'git checkout -b creates and switches to a new branch in one command.'),
(7, 'What is the main advantage of using microservices architecture?', 'mcq', 'Intermediate', 2.5, '["microservices", "architecture", "advantage"]', '{"2": ["scalability"], "3": ["scalability", "independence"], "4": ["scalability", "independence", "technology diversity"]}', 60, 'Understanding of architectural patterns.', '["Better performance", "Independent deployment and scaling", "Simpler debugging", "Lower cost"]', 'Independent deployment and scaling', 'Architecture', 'Microservices allow teams to develop, deploy, and scale services independently, providing better flexibility and fault isolation.'),
(8, 'Which of the following is a NoSQL database?', 'mcq', 'Easy', 1.5, '["NoSQL", "database", "type"]', '{"1": ["recognize NoSQL"], "2": ["know common NoSQL"], "3": ["understand NoSQL vs SQL"]}', 35, 'Database technology knowledge.', '["MySQL", "PostgreSQL", "MongoDB", "Oracle"]', 'MongoDB', 'Databases', 'MongoDB is a document-based NoSQL database, while the others are relational SQL databases.'),
(9, 'What does API stand for?', 'mcq', 'Easy', 1.0, '["API", "acronym", "definition"]', '{"1": ["Application"], "2": ["Application Programming"], "3": ["Application Programming Interface"]}', 20, 'Basic software development terminology.', '["Application Programming Interface", "Application Process Integration", "Automated Programming Interface", "Advanced Programming Interface"]', 'Application Programming Interface', 'APIs', 'API stands for Application Programming Interface, which defines how software components should interact.'),
(10, 'Which design pattern is used to ensure only one instance of a class exists?', 'mcq', 'Intermediate', 2.0, '["design pattern", "singleton", "instance"]', '{"2": ["Singleton"], "3": ["Singleton", "Factory"], "4": ["Singleton", "Factory", "Observer"]}', 45, 'Software design patterns knowledge.', '["Factory Pattern", "Observer Pattern", "Singleton Pattern", "Builder Pattern"]', 'Singleton Pattern', 'Design Patterns', 'The Singleton pattern ensures that a class has only one instance and provides global access to that instance.');

-- Sample Resources
INSERT IGNORE INTO resources (skill_id, target_level, resource_type, title, provider, url, estimated_hours, difficulty, rating, description, price) VALUES
(1, 2, 'course', 'Spring Boot Master Class', 'Udemy', 'https://udemy.com/spring-boot', 20, 'intermediate', 4.5, 'Comprehensive Spring Boot course covering REST APIs and JPA', 99.99),
(1, 3, 'book', 'Spring in Action', 'Manning', 'https://manning.com/spring-in-action', 40, 'intermediate', 4.7, 'In-depth guide to Spring Framework', 49.99),
(2, 2, 'tutorial', 'REST API Design Best Practices', 'FreeCodeCamp', 'https://freecodecamp.org/rest-api', 5, 'beginner', 4.8, 'Free tutorial on REST API design principles', 0.00);
