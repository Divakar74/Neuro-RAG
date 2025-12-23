import React from 'react';
import { Trophy, Star } from 'lucide-react';

const StrengthsCard = ({ skillData }) => {
  // Find top skills (level 4-5)
  const strengths = skillData?.filter(skill => skill.level >= 4) || [];

  return (
    <div className="bg-white p-6 rounded-lg shadow-lg">
      <div className="flex items-center mb-4">
        <Trophy className="w-6 h-6 text-yellow-500 mr-2" />
        <h3 className="text-xl font-semibold text-gray-800">Your Strengths</h3>
      </div>

      {strengths.length > 0 ? (
        <div className="space-y-3">
          {strengths.map((skill, index) => (
            <div key={index} className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
              <div className="flex items-center">
                <Star className="w-5 h-5 text-yellow-500 mr-2" />
                <span className="font-medium text-gray-800">{skill.name}</span>
              </div>
              <div className="flex items-center">
                <span className="text-sm text-gray-600 mr-2">Level {skill.level}/5</span>
                <div className="flex">
                  {[...Array(5)].map((_, i) => (
                    <Star
                      key={i}
                      className={`w-4 h-4 ${i < skill.level ? 'text-yellow-400 fill-current' : 'text-gray-300'}`}
                    />
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-gray-600 text-center py-4">
          Keep working on your skills! Strengths will appear here as you improve.
        </p>
      )}
    </div>
  );
};

export default StrengthsCard;
