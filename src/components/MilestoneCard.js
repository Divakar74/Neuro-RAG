import React from 'react';
import { CheckCircle, Circle, Clock } from 'lucide-react';

const MilestoneCard = ({ milestone, isCompleted = false }) => {
  return (
    <div className={`p-4 rounded-lg border-2 transition-all ${
      isCompleted
        ? 'bg-green-50 border-green-200'
        : 'bg-white border-gray-200 hover:border-blue-300'
    }`}>
      <div className="flex items-start">
        <div className="flex-shrink-0 mt-1">
          {isCompleted ? (
            <CheckCircle className="w-6 h-6 text-green-500" />
          ) : (
            <Circle className="w-6 h-6 text-gray-400" />
          )}
        </div>

        <div className="ml-3 flex-1">
          <h4 className={`font-semibold ${isCompleted ? 'text-green-800' : 'text-gray-800'}`}>
            {milestone.title}
          </h4>
          <p className="text-sm text-gray-600 mt-1">{milestone.description}</p>

          <div className="flex items-center mt-2 text-xs text-gray-500">
            <Clock className="w-4 h-4 mr-1" />
            <span>{milestone.estimatedTime || '2-3 hours'}</span>
          </div>

          {milestone.skills && milestone.skills.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {milestone.skills.map((skill, index) => (
                <span
                  key={index}
                  className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full"
                >
                  {skill}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MilestoneCard;
