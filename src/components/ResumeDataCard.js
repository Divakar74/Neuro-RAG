import React from 'react';
import { FileText, Award, GraduationCap, Briefcase } from 'lucide-react';

const ResumeDataCard = ({ resumeData }) => {
  if (!resumeData) return null;

  const skills = JSON.parse(resumeData.extractedSkills || '[]');
  const education = JSON.parse(resumeData.extractedEducation || '[]');

  return (
    <div className="bg-primary-100 rounded-3xl p-6 shadow-lg">
      <h3 className="text-2xl font-semibold mb-6 text-primary-900 flex items-center">
        <FileText className="w-6 h-6 mr-2" />
        Extracted Resume Data
      </h3>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Skills */}
        <div className="space-y-4">
          <h4 className="text-lg font-medium text-primary-900 flex items-center">
            <Award className="w-5 h-5 mr-2" />
            Skills
          </h4>
          <div className="space-y-2">
            {skills.slice(0, 5).map((skill, index) => (
              <div key={index} className="flex justify-between items-center bg-primary-200 rounded-lg p-2">
                <span className="text-primary-900 font-medium">{skill.skillName}</span>
                <span className="text-sm text-primary-700">{skill.yearsExperience} yrs</span>
              </div>
            ))}
          </div>
        </div>

        {/* Education */}
        <div className="space-y-4">
          <h4 className="text-lg font-medium text-primary-900 flex items-center">
            <GraduationCap className="w-5 h-5 mr-2" />
            Education
          </h4>
          <div className="space-y-2">
            {education.slice(0, 3).map((edu, index) => (
              <div key={index} className="bg-primary-200 rounded-lg p-2">
                <span className="text-primary-900">{edu}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Experience */}
        <div className="space-y-4">
          <h4 className="text-lg font-medium text-primary-900 flex items-center">
            <Briefcase className="w-5 h-5 mr-2" />
            Experience
          </h4>
          <div className="bg-primary-200 rounded-lg p-4">
            <p className="text-primary-900">
              <span className="font-medium">Total Years:</span> {resumeData.totalYearsExperience || 0}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResumeDataCard;
