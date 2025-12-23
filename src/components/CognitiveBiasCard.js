import React from 'react';
import { AlertTriangle, CheckCircle, Info, Brain } from 'lucide-react';

const CognitiveBiasCard = ({ bias }) => {
  const getLevelColor = (level) => {
    switch (level.toLowerCase()) {
      case 'high':
        return 'text-red-600 bg-red-50 border-red-200';
      case 'medium':
        return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'low':
        return 'text-green-600 bg-green-50 border-green-200';
      default:
        return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  const getLevelIcon = (level) => {
    switch (level.toLowerCase()) {
      case 'high':
        return <AlertTriangle className="w-4 h-4" />;
      case 'medium':
        return <Info className="w-4 h-4" />;
      case 'low':
        return <CheckCircle className="w-4 h-4" />;
      default:
        return <Brain className="w-4 h-4" />;
    }
  };

  return (
    <div className={`rounded-lg border p-4 ${getLevelColor(bias.level)}`}>
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center">
          {getLevelIcon(bias.level)}
          <h4 className="font-semibold ml-2">{bias.type}</h4>
        </div>
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getLevelColor(bias.level)}`}>
          {bias.level}
        </span>
      </div>
      
      <div className="space-y-2">
        <div>
          <p className="text-sm font-medium mb-1">Evidence:</p>
          <p className="text-sm opacity-90">{bias.evidence}</p>
        </div>
        
        <div>
          <p className="text-sm font-medium mb-1">Impact:</p>
          <p className="text-sm opacity-90">{bias.impact}</p>
        </div>
      </div>
    </div>
  );
};

export default CognitiveBiasCard;










