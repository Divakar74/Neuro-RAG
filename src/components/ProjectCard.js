import React from 'react';
import { Code, GitBranch, Calendar } from 'lucide-react';

const ProjectCard = ({ project }) => {
  return (
    <div className="bg-white p-4 rounded-lg border border-gray-200 hover:border-blue-300 transition-colors shadow-sm">
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-lg font-semibold text-gray-800">{project.title}</h4>
        <GitBranch className="w-5 h-5 text-gray-400" />
      </div>
      <p className="text-sm text-gray-600 mb-3">{project.description}</p>
      <div className="flex items-center space-x-4 text-xs text-gray-500">
        {project.technologies && (
          <div className="flex items-center space-x-1">
            <Code className="w-4 h-4" />
            <span>{project.technologies.join(', ')}</span>
          </div>
        )}
        {project.duration && (
          <div className="flex items-center space-x-1">
            <Calendar className="w-4 h-4" />
            <span>{project.duration}</span>
          </div>
        )}
      </div>
      {project.link && (
        <a
          href={project.link}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-block mt-3 text-blue-600 hover:underline text-sm"
        >
          View Project
        </a>
      )}
    </div>
  );
};

export default ProjectCard;
