import React from 'react';
import { TrendingUp, Target } from 'lucide-react';

const GrowthAreasCard = ({ skillData }) => {
  // Find skills that need improvement (level 1-3)
  const growthAreas = skillData?.filter(skill => skill.level <= 3) || [];

  return (
    <div className="bg-white p-6 rounded-lg shadow-lg">
      <div className="flex items-center mb-4">
        <TrendingUp className="w-6 h-6 text-blue-500 mr-2" />
        <h3 className="text-xl font-semibold text-gray-800">Growth Areas</h3>
      </div>

      {growthAreas.length > 0 ? (
        <div className="space-y-3">
          {growthAreas.map((skill, index) => (
            <div key={index} className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
              <div className="flex items-center">
                <Target className="w-5 h-5 text-blue-500 mr-2" />
                <span className="font-medium text-gray-800">{skill.name}</span>
              </div>
              <div className="flex items-center">
                <span className="text-sm text-gray-600 mr-2">Level {skill.level}/5</span>
                <div className="flex">
                  {[...Array(5)].map((_, i) => (
                    <div
                      key={i}
                      className={`w-4 h-4 rounded-full border-2 ${
                        i < skill.level
                          ? 'bg-blue-500 border-blue-500'
                          : 'border-gray-300'
                      }`}
                    />
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-gray-600 text-center py-4">
          Excellent! All your skills are at advanced levels.
        </p>
      )}

      {growthAreas.length > 0 && (
        <div className="mt-4 p-3 bg-yellow-50 rounded-lg">
          <p className="text-sm text-yellow-800">
            💡 Focus on these areas to reach your full potential. Check your personalized roadmap for learning resources.
          </p>
        </div>
      )}
    </div>
  );
};

export default GrowthAreasCard;
