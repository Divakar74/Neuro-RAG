import React, { useState, useEffect } from 'react';
import { Target, TrendingUp, CheckCircle, BookOpen, Clock, MapPin, Award, Lightbulb, Sparkles } from 'lucide-react';
import SkillGraph from './SkillGraph';

const SkillGapAnalysis = ({ skillData, targetRole, sessionId, resumeData, userResponses }) => {
  const [gapAnalysis, setGapAnalysis] = useState(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('insights');

  useEffect(() => {
    if (skillData && skillData.length > 0) {
      analyzeSkillGaps();
    }
  }, [skillData, targetRole, resumeData, userResponses, sessionId]);

  const analyzeSkillGaps = () => {
    setLoading(true);

    // Simulate analysis - in real implementation, this would call the backend
    setTimeout(() => {
      const resumeSkills = extractResumeSkills(resumeData);
      const responseEvidence = analyzeUserResponses(userResponses);

      const opportunities = skillData
        .filter(skill => skill.level < 4) // Changed from < 3 to < 4 for more opportunities
        .map(skill => {
          const resumeBoost = resumeSkills[skill.name.toLowerCase()] || 0;
          const adjustedLevel = Math.min(5, Math.max(1, skill.level + resumeBoost));
          const targetLevel = inferTargetLevel(skill, targetRole);
          const evidence = generateEvidence(skill, resumeData, responseEvidence);

          return {
            skill: skill.name,
            currentLevel: adjustedLevel,
            targetLevel,
            growth: Math.max(0, targetLevel - adjustedLevel),
            priority: calculatePriority(skill, targetRole),
            evidence: evidence,
            roadmap: generateRoadmap(skill, targetRole, evidence, responseEvidence),
            id: skill.name // Add id for graph
          };
        })
        .filter(opp => opp.growth > 0)
        .sort((a, b) => b.priority - a.priority);

      setGapAnalysis({
        totalOpportunities: opportunities.length,
        highImpact: opportunities.filter(o => o.priority > 0.7).length,
        moderateImpact: opportunities.filter(o => o.priority <= 0.7 && o.priority > 0.4).length,
        opportunities: opportunities.slice(0, 5) // Show top 5 opportunities
      });

      setLoading(false);
    }, 1000);
  };

  const analyzeUserResponses = (responses) => {
    if (!responses || responses.length === 0) return {};

    const evidence = {};
    responses.forEach(response => {
      const questionType = response.question?.questionType || 'text';
      const responseText = response.responseText || '';
      const confidence = response.confidenceLevel || 0.5;

      if (questionType === 'text' && responseText.length < 50) {
        evidence.shortResponses = (evidence.shortResponses || 0) + 1;
      }
      if (confidence < 0.6) {
        evidence.lowConfidence = (evidence.lowConfidence || 0) + 1;
      }
      if (response.thinkTimeSeconds && response.thinkTimeSeconds < 30) {
        evidence.quickResponses = (evidence.quickResponses || 0) + 1;
      }
    });

    return evidence;
  };

  const calculatePriority = (skill, role) => {
    const baseGap = 4 - Math.min(4, skill.level);
    const programming = skill.name.toLowerCase().includes('programming') || skill.name.toLowerCase().includes('coding');
    const roleBoost = role && skill.name.toLowerCase().includes(role.toLowerCase()) ? 0.5 : 0;
    return (baseGap) * (programming ? 1.5 : 1) + roleBoost;
  };

  const generateEvidence = (skill, resume, responseEvidence) => {
    const evidence = [];

    // Evidence from assessment responses
    if (responseEvidence.lowConfidence > 2) {
      evidence.push('Multiple responses showed uncertainty - indicates knowledge gaps');
    }
    if (responseEvidence.shortResponses > 3) {
      evidence.push('Brief responses suggest areas needing deeper understanding');
    }
    if (responseEvidence.quickResponses > 2) {
      evidence.push('Quick responses may indicate surface-level knowledge');
    }

    // Evidence from skill assessment
    if (skill.confidence < 0.4) {
      evidence.push('Assessment confidence indicates room for improvement');
    }
    if (skill.level < 2.5) {
      evidence.push('Current proficiency level suggests foundational gaps');
    }

    // Evidence from resume
    const resumeSkills = extractResumeSkills(resume);
    if (resumeSkills[skill.name.toLowerCase()] > 0) {
      evidence.push('Resume shows related experience - build on existing foundation');
    } else {
      evidence.push('Limited direct experience in this area');
    }

    // Role relevance
    if (targetRole && skill.name.toLowerCase().includes(targetRole.toLowerCase())) {
      evidence.push('Highly relevant to target career path');
    }

    return evidence;
  };

  const generateRoadmap = (skill, role, evidence, responseEvidence) => {
    const roadmap = [];
    const currentLevel = skill.level;

    // Phase 1: Foundation Building
    if (currentLevel < 2.5) {
      roadmap.push({
        phase: 'Foundation Building',
        duration: '2-4 weeks',
        activities: [
          'Complete introductory tutorials and courses',
          'Practice basic concepts through exercises',
          'Build simple projects to reinforce learning'
        ]
      });
    }

    // Phase 2: Skill Development
    roadmap.push({
      phase: 'Skill Development',
      duration: '4-6 weeks',
      activities: [
        'Work on intermediate-level projects',
        'Study real-world applications and use cases',
        'Participate in coding challenges or exercises'
      ]
    });

    // Phase 3: Advanced Practice
    if (currentLevel < 4) {
      roadmap.push({
        phase: 'Advanced Practice',
        duration: '6-8 weeks',
        activities: [
          'Build complex, portfolio-worthy projects',
          'Contribute to open-source or team projects',
          'Prepare for technical interviews and assessments'
        ]
      });
    }

    // Phase 4: Mastery & Specialization
    roadmap.push({
      phase: 'Mastery & Specialization',
      duration: 'Ongoing',
      activities: [
        'Stay updated with latest trends and technologies',
        'Mentor others and share knowledge',
        'Pursue advanced certifications or specializations'
      ]
    });

    return roadmap;
  };



  const extractResumeSkills = (resume) => {
    try {
      if (!resume) return {};
      let parsed = {};
      if (resume.extractedSkills && typeof resume.extractedSkills === 'string') {
        parsed = JSON.parse(resume.extractedSkills);
      } else if (Array.isArray(resume.extractedSkills)) {
        parsed = resume.extractedSkills;
      }
      const map = {};
      (Array.isArray(parsed) ? parsed : []).forEach(item => {
        const key = (item.skillName || '').toLowerCase();
        if (!key) return;
        const boost = item.yearsExperience >= 2 ? 1 : 0.5;
        map[key] = Math.max(map[key] || 0, boost);
      });
      return map;
    } catch (e) {
      return {};
    }
  };

  const inferTargetLevel = (skill, role) => {
    if (!role) return 4;
    const name = skill.name.toLowerCase();
    if (name.includes(role.toLowerCase())) return 5;
    if (name.includes('foundations') || name.includes('basics')) return 3;
    return 4;
  };

  if (loading) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-gray-200 rounded w-1/3 mb-4"></div>
          <div className="space-y-3">
            <div className="h-4 bg-gray-200 rounded"></div>
            <div className="h-4 bg-gray-200 rounded w-5/6"></div>
            <div className="h-4 bg-gray-200 rounded w-4/6"></div>
          </div>
        </div>
      </div>
    );
  }

  if (!gapAnalysis || gapAnalysis.totalOpportunities === 0) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-6">
        <div className="flex items-center mb-4">
          <div className="bg-green-100 p-3 rounded-lg">
            <CheckCircle className="w-6 h-6 text-green-600" />
          </div>
          <div className="ml-4">
            <h3 className="text-xl font-semibold text-gray-800">Growth Opportunities</h3>
            <p className="text-gray-600">Excellent foundation! Here are some advanced growth opportunities.</p>
          </div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-green-800">
            Your skills are well-developed. Consider exploring advanced specializations, leadership opportunities, or emerging technologies to continue growing in your career.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-lg p-6">
      <div className="flex items-center mb-6">
        <div className="bg-indigo-100 p-3 rounded-lg">
          <Sparkles className="w-6 h-6 text-indigo-600" />
        </div>
        <div className="ml-4">
          <h3 className="text-xl font-semibold text-gray-800">Growth Opportunities</h3>
          <p className="text-gray-600">Personalized development roadmap for {targetRole || 'your career growth'}</p>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex space-x-1 mb-6 bg-gray-100 p-1 rounded-lg">
        <button
          onClick={() => setActiveTab('insights')}
          className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'insights'
              ? 'bg-white text-indigo-700 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Key Insights
        </button>
        <button
          onClick={() => setActiveTab('roadmap')}
          className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'roadmap'
              ? 'bg-white text-indigo-700 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Learning Roadmap
        </button>
        <button
          onClick={() => setActiveTab('roadmap')}
          className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'roadmap'
              ? 'bg-white text-indigo-700 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Visual Roadmap
        </button>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-blue-600">{gapAnalysis.highImpact}</div>
          <div className="text-sm text-blue-700">High Impact</div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-green-600">{gapAnalysis.moderateImpact}</div>
          <div className="text-sm text-green-700">Moderate Impact</div>
        </div>
        <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 text-center">
          <div className="text-2xl font-bold text-purple-600">{gapAnalysis.totalOpportunities}</div>
          <div className="text-sm text-purple-700">Total Opportunities</div>
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'insights' && (
        <div className="space-y-4">
          <h4 className="font-semibold text-gray-800">Top Growth Opportunities</h4>
          {gapAnalysis.opportunities.map((opportunity, index) => (
            <div key={index} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h5 className="font-semibold text-gray-800">{opportunity.skill}</h5>
                  <div className="flex items-center space-x-4 mt-1">
                    <span className="text-sm text-gray-600">
                      Current: {opportunity.currentLevel}/5
                    </span>
                    <span className="text-sm text-gray-600">
                      Target: {opportunity.targetLevel}/5
                    </span>
                    <span className="text-sm font-medium text-indigo-600">
                      Growth: +{opportunity.growth} levels
                    </span>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-16 bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-indigo-500 h-2 rounded-full"
                      style={{ width: `${(opportunity.currentLevel / opportunity.targetLevel) * 100}%` }}
                    ></div>
                  </div>
                  <span className="text-xs text-gray-500">
                    {Math.round((opportunity.currentLevel / opportunity.targetLevel) * 100)}%
                  </span>
                </div>
              </div>

              {/* Evidence */}
              <div className="mb-3">
                <div className="flex items-center mb-2">
                  <Lightbulb className="w-4 h-4 text-amber-500 mr-2" />
                  <span className="text-sm font-medium text-gray-700">Insights from your responses:</span>
                </div>
                <ul className="text-sm text-gray-600 list-disc list-inside ml-6">
                  {opportunity.evidence.map((item, idx) => (
                    <li key={idx}>{item}</li>
                  ))}
                </ul>
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'roadmap' && (
        <div className="space-y-4">
          {gapAnalysis.opportunities.slice(0, 1).map((opportunity, index) => (
            <div key={index}>
              <h4 className="font-semibold text-gray-800 mb-4">Development Roadmap for {opportunity.skill}</h4>
              <div className="space-y-4">
                {opportunity.roadmap.map((phase, phaseIndex) => (
                  <div key={phaseIndex} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-2">
                      <h5 className="font-medium text-gray-800">{phase.phase}</h5>
                      <span className="text-sm text-gray-500">{phase.duration}</span>
                    </div>
                    <ul className="text-sm text-gray-600 list-disc list-inside ml-4">
                      {phase.activities.map((activity, actIndex) => (
                        <li key={actIndex}>{activity}</li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'roadmap' && (
        <div className="space-y-4">
          {gapAnalysis.opportunities.slice(0, 1).map((opportunity, index) => (
            <div key={index}>
              <h4 className="font-semibold text-gray-800 mb-4">Visual Learning Roadmap for {opportunity.skill}</h4>
              <div className="bg-gray-50 rounded-lg p-6">
                <SkillGraph
                  skillData={[opportunity]}
                  targetRole={targetRole}
                  sessionId={sessionId}
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SkillGapAnalysis;



