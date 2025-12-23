import React, { useEffect, useState } from 'react';
import RoadmapTimeline from './RoadmapTimeline';
import MilestoneCard from './MilestoneCard';
import ResourceCard from './ResourceCard';
import ProjectCard from './ProjectCard';
import MotivationBanner from './MotivationBanner';
import { BookOpen, Code, Users } from 'lucide-react';
import { getRoadmap } from '../services/roadmapService';

const RoadmapPage = ({ sessionId }) => {
  const [roadmapData, setRoadmapData] = useState(null);

  useEffect(() => {
    const load = async () => {
      if (!sessionId) return;
      const res = await getRoadmap(sessionId);
      // Backend returns a list; pick the first for display
      const list = res.data;
      setRoadmapData(Array.isArray(list) ? list[0] : list);
    };
    load();
  }, [sessionId]);

  if (!roadmapData) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Generating your personalized roadmap...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-800 mb-2">Your Learning Roadmap</h1>
          <p className="text-xl text-gray-600">A personalized path to skill mastery</p>
        </div>

        {/* Roadmap Timeline */}
        <div className="mb-12">
          <RoadmapTimeline roadmapData={roadmapData} />
        </div>

        {/* Detailed Phases */}
        <div className="space-y-12">
          {roadmapData.phases?.map((phase, phaseIndex) => (
            <div key={phaseIndex} className="bg-white rounded-xl shadow-lg p-8">
              <div className="flex items-center mb-6">
                <div className="bg-blue-100 rounded-full p-3 mr-4">
                  <BookOpen className="w-6 h-6 text-blue-600" />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-gray-800">{phase.title}</h2>
                  <p className="text-gray-600">{phase.description}</p>
                </div>
              </div>

              {/* Milestones */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                {phase.milestones?.map((milestone, milestoneIndex) => (
                  <MilestoneCard
                    key={milestoneIndex}
                    milestone={milestone}
                    isCompleted={milestone.completed}
                  />
                ))}
              </div>

              {/* Resources */}
              {phase.resources && phase.resources.length > 0 && (
                <div className="mb-8">
                  <h3 className="text-xl font-semibold mb-4 flex items-center">
                    <BookOpen className="w-5 h-5 mr-2 text-green-600" />
                    Recommended Resources
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {phase.resources.map((resource, resourceIndex) => (
                      <ResourceCard key={resourceIndex} resource={resource} />
                    ))}
                  </div>
                </div>
              )}

              {/* Projects */}
              {phase.projects && phase.projects.length > 0 && (
                <div>
                  <h3 className="text-xl font-semibold mb-4 flex items-center">
                    <Code className="w-5 h-5 mr-2 text-purple-600" />
                    Practice Projects
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {phase.projects.map((project, projectIndex) => (
                      <ProjectCard key={projectIndex} project={project} />
                    ))}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Motivation Banner */}
        <div className="mt-12">
          <MotivationBanner />
        </div>
      </div>
    </div>
  );
};

export default RoadmapPage;
