import React, { useState, useEffect } from 'react';
import { BookOpen, ExternalLink, Star, Clock, Users } from 'lucide-react';

const FreeResourcesCard = ({ skillGaps, userId }) => {
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(false);

  // Enhanced free resources data with more comprehensive coverage
  const mockResources = [
    {
      id: 1,
      title: "JavaScript Fundamentals",
      platform: "freeCodeCamp",
      type: "Interactive Course",
      duration: "10 hours",
      rating: 4.8,
      students: "2.1M",
      url: "https://www.freecodecamp.org/learn/javascript-algorithms-and-data-structures/",
      skills: ["javascript", "programming", "algorithms"],
      difficulty: "Beginner",
      description: "Complete interactive JavaScript course with hands-on projects"
    },
    {
      id: 2,
      title: "Python for Data Science",
      platform: "Coursera",
      type: "Course",
      duration: "8 hours",
      rating: 4.7,
      students: "500K",
      url: "https://www.coursera.org/learn/python-data-analysis",
      skills: ["python", "data analysis", "pandas"],
      difficulty: "Intermediate",
      description: "Learn Python data analysis with pandas and numpy"
    },
    {
      id: 3,
      title: "React.js Documentation",
      platform: "React",
      type: "Documentation",
      duration: "Self-paced",
      rating: 4.9,
      students: "N/A",
      url: "https://reactjs.org/docs/getting-started.html",
      skills: ["react", "frontend", "javascript"],
      difficulty: "All Levels",
      description: "Official React documentation with tutorials and guides"
    },
    {
      id: 4,
      title: "SQLZoo",
      platform: "SQLZoo",
      type: "Interactive Exercises",
      duration: "6 hours",
      rating: 4.6,
      students: "1M",
      url: "https://sqlzoo.net/",
      skills: ["sql", "database", "queries"],
      difficulty: "Beginner to Intermediate",
      description: "Interactive SQL exercises from basic to advanced queries"
    },
    {
      id: 5,
      title: "CS50's Introduction to Computer Science",
      platform: "Harvard University",
      type: "Video Lectures",
      duration: "12 weeks",
      rating: 4.9,
      students: "2M",
      url: "https://cs50.harvard.edu/college/2022/spring/",
      skills: ["computer science", "programming", "algorithms"],
      difficulty: "Beginner",
      description: "Harvard's introductory computer science course"
    },
    {
      id: 6,
      title: "The Odin Project",
      platform: "The Odin Project",
      type: "Full Curriculum",
      duration: "Self-paced",
      rating: 4.7,
      students: "500K",
      url: "https://www.theodinproject.com/",
      skills: ["web development", "html", "css", "javascript"],
      difficulty: "Beginner to Advanced",
      description: "Open-source full-stack web development curriculum"
    },
    {
      id: 7,
      title: "Machine Learning by Andrew Ng",
      platform: "Coursera",
      type: "Course",
      duration: "11 weeks",
      rating: 4.9,
      students: "3.2M",
      url: "https://www.coursera.org/learn/machine-learning",
      skills: ["machine learning", "ai", "python", "mathematics"],
      difficulty: "Intermediate",
      description: "Stanford's machine learning course by Andrew Ng"
    },
    {
      id: 8,
      title: "AWS Cloud Practitioner Essentials",
      platform: "AWS",
      type: "Course",
      duration: "6 hours",
      rating: 4.6,
      students: "800K",
      url: "https://aws.amazon.com/training/learn-about/cloud-practitioner/",
      skills: ["cloud computing", "aws", "devops"],
      difficulty: "Beginner",
      description: "Learn AWS cloud fundamentals and core services"
    },
    {
      id: 9,
      title: "Docker for Beginners",
      platform: "Docker",
      type: "Tutorial",
      duration: "4 hours",
      rating: 4.5,
      students: "600K",
      url: "https://docker-curriculum.com/",
      skills: ["docker", "containers", "devops"],
      difficulty: "Beginner to Intermediate",
      description: "Learn containerization with Docker"
    },
    {
      id: 10,
      title: "Git & GitHub Crash Course",
      platform: "freeCodeCamp",
      type: "Video Course",
      duration: "1.5 hours",
      rating: 4.7,
      students: "1.8M",
      url: "https://www.youtube.com/watch?v=SWYqp7iY_Tc",
      skills: ["git", "github", "version control"],
      difficulty: "Beginner",
      description: "Complete Git and GitHub tutorial for beginners"
    },
    {
      id: 11,
      title: "System Design Interview",
      platform: "Grokking",
      type: "Course",
      duration: "8 hours",
      rating: 4.8,
      students: "150K",
      url: "https://www.educative.io/courses/grokking-the-system-design-interview",
      skills: ["system design", "architecture", "scalability"],
      difficulty: "Advanced",
      description: "Master system design for technical interviews"
    },
    {
      id: 12,
      title: "Cybersecurity Fundamentals",
      platform: "Cybrary",
      type: "Course",
      duration: "10 hours",
      rating: 4.6,
      students: "400K",
      url: "https://www.cybrary.it/course/comptia-cysa/",
      skills: ["cybersecurity", "security", "networking"],
      difficulty: "Beginner to Intermediate",
      description: "Learn cybersecurity basics and best practices"
    }
  ];

  // Enhanced skill matching with synonyms and better relevance scoring
  const skillSynonyms = {
    'javascript': ['js', 'ecmascript', 'frontend scripting', 'client-side scripting'],
    'python': ['py', 'python programming', 'data science scripting'],
    'react': ['reactjs', 'react.js', 'facebook react', 'frontend framework'],
    'sql': ['database queries', 'relational databases', 'structured query language'],
    'machine learning': ['ml', 'artificial intelligence', 'ai', 'predictive modeling'],
    'data analysis': ['data analytics', 'data science', 'business intelligence'],
    'cloud computing': ['aws', 'azure', 'gcp', 'cloud services'],
    'docker': ['containerization', 'containers', 'docker containers'],
    'git': ['version control', 'source control', 'github', 'gitlab'],
    'system design': ['architecture', 'scalability', 'distributed systems'],
    'cybersecurity': ['security', 'information security', 'cyber defense']
  };

  const calculateSkillRelevance = (resourceSkills, userSkillGap) => {
    const gapSkill = userSkillGap.skill.toLowerCase();
    let maxRelevance = 0;

    resourceSkills.forEach(resourceSkill => {
      const resourceSkillLower = resourceSkill.toLowerCase();

      // Direct match
      if (gapSkill.includes(resourceSkillLower) || resourceSkillLower.includes(gapSkill)) {
        maxRelevance = Math.max(maxRelevance, 1.0);
      }

      // Synonym match
      const synonyms = skillSynonyms[resourceSkillLower] || [];
      synonyms.forEach(synonym => {
        if (gapSkill.includes(synonym) || synonym.includes(gapSkill)) {
          maxRelevance = Math.max(maxRelevance, 0.9);
        }
      });

      // Partial word match for compound skills
      const gapWords = gapSkill.split(' ');
      const resourceWords = resourceSkillLower.split(' ');
      const commonWords = gapWords.filter(word => resourceWords.includes(word));
      if (commonWords.length > 0) {
        maxRelevance = Math.max(maxRelevance, commonWords.length / Math.max(gapWords.length, resourceWords.length));
      }
    });

    return maxRelevance;
  };

  useEffect(() => {
    if (skillGaps && skillGaps.length > 0) {
      // Calculate relevance scores for all resources
      const resourcesWithScores = mockResources.map(resource => {
        const relevanceScores = skillGaps.map(gap => calculateSkillRelevance(resource.skills, gap));
        const maxRelevance = Math.max(...relevanceScores);
        const averageRelevance = relevanceScores.reduce((sum, score) => sum + score, 0) / relevanceScores.length;

        return {
          ...resource,
          relevanceScore: maxRelevance,
          averageRelevance: averageRelevance
        };
      });

      // Filter resources with meaningful relevance (> 0.3) and sort by relevance
      const relevantResources = resourcesWithScores
        .filter(resource => resource.relevanceScore > 0.3)
        .sort((a, b) => b.relevanceScore - a.relevanceScore)
        .slice(0, 4); // Show top 4 most relevant resources

      // If no highly relevant resources found, show the most relevant ones available
      if (relevantResources.length === 0) {
        const fallbackResources = resourcesWithScores
          .sort((a, b) => b.averageRelevance - a.averageRelevance)
          .slice(0, 4);
        setResources(fallbackResources);
      } else {
        setResources(relevantResources);
      }
    } else {
      // Show general popular resources if no specific gaps
      setResources(mockResources.slice(0, 4));
    }
  }, [skillGaps]);

  const getDifficultyColor = (difficulty) => {
    switch (difficulty.toLowerCase()) {
      case 'beginner':
        return 'bg-green-100 text-green-800';
      case 'intermediate':
        return 'bg-yellow-100 text-yellow-800';
      case 'advanced':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-lg p-6">
      <div className="flex items-center mb-4">
        <BookOpen className="w-5 h-5 text-indigo-600 mr-2" />
        <h3 className="text-lg font-semibold text-gray-800">Free Learning Resources</h3>
      </div>

      <p className="text-sm text-gray-600 mb-4">
        Personalized free resources tailored to your specific skill gaps and learning needs.
      </p>

      <div className="space-y-4">
        {resources.map((resource) => (
          <div key={resource.id} className="border border-gray-200 rounded-lg p-4 hover:border-indigo-300 transition-colors">
            <div className="flex justify-between items-start mb-2">
              <h4 className="font-medium text-gray-900 text-sm">{resource.title}</h4>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getDifficultyColor(resource.difficulty)}`}>
                {resource.difficulty}
              </span>
            </div>

            <p className="text-xs text-gray-600 mb-2">{resource.platform} • {resource.type}</p>

            <div className="flex items-center space-x-4 text-xs text-gray-500 mb-3">
              <div className="flex items-center">
                <Clock className="w-3 h-3 mr-1" />
                {resource.duration}
              </div>
              <div className="flex items-center">
                <Star className="w-3 h-3 mr-1 text-yellow-400" />
                {resource.rating}
              </div>
              {resource.students !== 'N/A' && (
                <div className="flex items-center">
                  <Users className="w-3 h-3 mr-1" />
                  {resource.students}
                </div>
              )}
            </div>

            <a
              href={resource.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center text-xs text-indigo-600 hover:text-indigo-800 font-medium"
            >
              Start Learning
              <ExternalLink className="w-3 h-3 ml-1" />
            </a>
          </div>
        ))}
      </div>

      <div className="mt-4 pt-4 border-t border-gray-200">
        <p className="text-xs text-gray-500 text-center">
          Resources are selected based on your skill assessment results.
          More resources available at{' '}
          <a href="#" className="text-indigo-600 hover:text-indigo-800">
            skillmap-resources.com
          </a>
        </p>
      </div>
    </div>
  );
};

export default FreeResourcesCard;
