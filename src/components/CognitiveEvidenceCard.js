import React from 'react';

const CognitiveEvidenceCard = ({ level, evidence }) => {
  return (
    <div className="bg-white rounded-2xl p-6 shadow-md mb-8">
      <h2 className="text-2xl font-bold text-primary-900 mb-4">Cognitive Level: {level}</h2>
      <div>
        <h3 className="text-lg font-semibold text-primary-900 mb-2">Evidence</h3>
        <ul className="list-disc list-inside text-gray-700 space-y-1">
          {evidence && evidence.length > 0 ? (
            evidence.map((item, index) => (
              <li key={index}>{item}</li>
            ))
          ) : (
            <li>No evidence available</li>
          )}
        </ul>
      </div>
    </div>
  );
};

export default CognitiveEvidenceCard;
