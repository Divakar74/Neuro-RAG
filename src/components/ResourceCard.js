import React from 'react';
import { ExternalLink, BookOpen, Video, FileText, Link } from 'lucide-react';

const ResourceCard = ({ resource }) => {
  const getResourceIcon = (type) => {
    switch (type?.toLowerCase()) {
      case 'video':
      case 'course':
        return <Video className="w-5 h-5 text-red-500" />;
      case 'book':
      case 'article':
        return <BookOpen className="w-5 h-5 text-blue-500" />;
      case 'documentation':
        return <FileText className="w-5 h-5 text-green-500" />;
      default:
        return <Link className="w-5 h-5 text-gray-500" />;
    }
  };

  const getDifficultyColor = (difficulty) => {
    switch (difficulty?.toLowerCase()) {
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
    <div className="bg-white p-4 rounded-lg border border-gray-200 hover:border-blue-300 transition-colors">
      <div className="flex items-start justify-between">
        <div className="flex items-start">
          <div className="flex-shrink-0 mt-1">
            {getResourceIcon(resource.type)}
          </div>
          <div className="ml-3 flex-1">
            <h4 className="font-semibold text-gray-800 hover:text-blue-600 cursor-pointer">
              {resource.title}
            </h4>
            <p className="text-sm text-gray-600 mt-1">{resource.description}</p>

            <div className="flex items-center mt-2 space-x-2">
              {resource.difficulty && (
                <span className={`px-2 py-1 text-xs rounded-full ${getDifficultyColor(resource.difficulty)}`}>
                  {resource.difficulty}
                </span>
              )}
              {resource.duration && (
                <span className="text-xs text-gray-500">
                  {resource.duration}
                </span>
              )}
              {resource.platform && (
                <span className="text-xs text-gray-500">
                  {resource.platform}
                </span>
              )}
            </div>
          </div>
        </div>

        {resource.url && (
          <a
            href={resource.url}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-shrink-0 ml-2 p-2 text-gray-400 hover:text-blue-500 transition-colors"
          >
            <ExternalLink className="w-4 h-4" />
          </a>
        )}
      </div>

      {resource.tags && resource.tags.length > 0 && (
        <div className="flex flex-wrap gap-1 mt-3">
          {resource.tags.map((tag, index) => (
            <span
              key={index}
              className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-full"
            >
              {tag}
            </span>
          ))}
        </div>
      )}
    </div>
  );
};

export default ResourceCard;
