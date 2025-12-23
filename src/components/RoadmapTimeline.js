import React from 'react';
import { Calendar, Clock, BookOpen, Trophy, MapPin } from 'lucide-react';

const RoadmapTimeline = ({ roadmapData }) => {
  const data = Array.isArray(roadmapData) ? (roadmapData[0] || null) : roadmapData;
  if (!data || !data.phases) {
    return (
      <div className="bg-white p-6 rounded-lg shadow-lg">
        <h3 className="text-xl font-semibold mb-4 text-gray-800">Learning Roadmap</h3>
        <p className="text-gray-600">No roadmap data available yet.</p>
      </div>
    );
  }

  return (
    <div className="bg-white p-6 rounded-2xl shadow-xl">
      <h3 className="text-xl font-semibold mb-6 text-gray-800">Your Learning Roadmap</h3>

      <div className="space-y-6">
        {data.phases.map((phase, phaseIndex) => (
          <div key={phaseIndex} className="relative">
            {/* Phase Header */}
            <div className="flex items-center mb-4">
              <div className="flex items-center justify-center w-10 h-10 bg-gradient-to-br from-indigo-600 to-purple-600 text-white rounded-full font-bold shadow">
                {phaseIndex + 1}
              </div>
              <div className="ml-4">
                <h4 className="text-lg font-semibold text-gray-800">{phase.title}</h4>
                <p className="text-gray-600">{phase.description}</p>
                <div className="flex items-center mt-1 text-sm text-gray-500">
                  <Clock className="w-4 h-4 mr-1" />
                  <span>{phase.duration} weeks</span>
                </div>
              </div>
            </div>

            {/* Milestones */}
            <div className="ml-14 space-y-3">
              {phase.milestones?.map((milestone, milestoneIndex) => (
                <div key={milestoneIndex} className="flex items-start">
                  <div className="flex items-center justify-center w-6 h-6 bg-emerald-500 text-white rounded-full text-xs font-bold mr-3 mt-1 shadow">
                    ✓
                  </div>
                  <div className="flex-1">
                    <h5 className="font-medium text-gray-800">{milestone.title}</h5>
                    <p className="text-sm text-gray-600 mb-2">{milestone.description}</p>

                    {/* Resources */}
                    {milestone.resources && milestone.resources.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {milestone.resources.map((resource, resourceIndex) => (
                          <span
                            key={resourceIndex}
                            className="inline-flex items-center px-2 py-1 bg-indigo-100 text-indigo-800 text-xs rounded-full"
                          >
                            <BookOpen className="w-3 h-3 mr-1" />
                            {resource.title}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Phase separator */}
            {phaseIndex < data.phases.length - 1 && (
              <div className="ml-5 w-0.5 h-8 bg-gradient-to-b from-indigo-200 to-purple-200"></div>
            )}
          </div>
        ))}
      </div>

      {/* Completion Summary */}
      <div className="mt-8 p-4 bg-gradient-to-r from-green-50 to-blue-50 rounded-xl">
        <div className="flex items-center">
          <Trophy className="w-6 h-6 text-yellow-500 mr-3" />
          <div>
            <h4 className="font-semibold text-gray-800">Expected Completion</h4>
            <p className="text-sm text-gray-600">
              {data.totalDuration} weeks • {data.totalMilestones} milestones
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RoadmapTimeline;
