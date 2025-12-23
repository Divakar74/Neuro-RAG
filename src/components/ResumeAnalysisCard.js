import React from 'react';
import ResumeContentViewer from './ResumeContentViewer';
import { FileText, User, Briefcase, GraduationCap, Award, Calendar } from 'lucide-react';

const ResumeAnalysisCard = ({ resumeData }) => {
  if (!resumeData) return null;

  const parseJsonSafely = (jsonString) => {
    try {
      return JSON.parse(jsonString);
    } catch {
      return [];
    }
  };

  const skills = parseJsonSafely(resumeData.extractedSkills || '[]');
  const education = parseJsonSafely(resumeData.extractedEducation || '[]');
  const experience = parseJsonSafely(resumeData.extractedExperience || '[]');

  return (
    <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300">
      <div className="flex items-center mb-6">
        <div className="bg-blue-100 p-3 rounded-lg">
          <FileText className="w-6 h-6 text-blue-600" />
        </div>
        <div className="ml-4">
          <h3 className="text-xl font-semibold text-gray-800">Resume Analysis:</h3>
          <p className="text-gray-600">Extracted insights from your resume</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Skills Section */}
        <div>
          <div className="flex items-center mb-3">
            <Award className="w-4 h-4 text-green-600 mr-2" />
            <h4 className="font-semibold text-gray-800">Skills ({skills.length})</h4>
          </div>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {skills.length > 0 ? (
              skills.slice(0, 8).map((skill, index) => (
                <div key={index} className="bg-green-50 border border-green-200 rounded-lg p-3">
                  <div className="font-medium text-green-900 text-sm">
                    {typeof skill === 'string' ? skill : skill.skillName || 'Unknown Skill'}
                  </div>
                  <div className="text-xs text-green-700 mt-1">
                    Experience: {skill.yearsExperience || 0} years
                    {skill.confidence && (
                      <span className="ml-2">
                        • Confidence: {Math.round(skill.confidence * 100)}%
                      </span>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <div className="text-gray-500 text-sm">No skills extracted</div>
            )}
          </div>
          {skills.length > 8 && (
            <div className="text-xs text-gray-500 mt-2">
              +{skills.length - 8} more skills
            </div>
          )}
        </div>

        {/* Education Section */}
        <div>
          <div className="flex items-center mb-3">
            <GraduationCap className="w-4 h-4 text-blue-600 mr-2" />
            <h4 className="font-semibold text-gray-800">Education ({education.length})</h4>
          </div>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {education.length > 0 ? (
              education.slice(0, 3).map((edu, index) => (
                <div key={index} className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                  <div className="font-medium text-blue-900 text-sm">
                    {edu}
                  </div>
                </div>
              ))
            ) : (
              <div className="text-gray-500 text-sm">No education extracted</div>
            )}
          </div>
        </div>
      </div>

      {/* Experience Summary */}
      {resumeData.totalYearsExperience && (
        <div className="mt-6 pt-6 border-t border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <Briefcase className="w-4 h-4 text-purple-600 mr-2" />
              <span className="font-semibold text-gray-800">Total Experience</span>
            </div>
            <span className="text-2xl font-bold text-purple-600">
              {resumeData.totalYearsExperience} years
            </span>
          </div>
        </div>
      )}

      {/* Readable Resume Preview */}
      <div className="mt-6 pt-6 border-t border-gray-200">
        <h4 className="font-semibold text-gray-800 mb-3">Resume Preview</h4>
        <div className="bg-gray-50 rounded-lg p-4 max-h-64 overflow-y-auto text-sm text-gray-800">
          <ResumeContentViewer text={resumeData.rawText} />
        </div>
      </div>
    </div>
  );
};

export default ResumeAnalysisCard;







