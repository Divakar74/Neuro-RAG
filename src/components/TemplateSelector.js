import React, { useState } from 'react';

const templates = [
  {
    id: 'template1',
    name: 'Modern Professional',
    description: 'Clean and modern design with emphasis on skills and experience.',
    previewImage: '/templates/template1.png',
  },
  {
    id: 'template2',
    name: 'Creative Portfolio',
    description: 'Colorful and creative layout for showcasing projects and skills.',
    previewImage: '/templates/template2.png',
  },
  {
    id: 'template3',
    name: 'Minimalist',
    description: 'Simple and elegant template focusing on content clarity.',
    previewImage: '/templates/template3.png',
  },
];

const TemplateSelector = ({ selectedTemplate, onSelectTemplate }) => {
  const [hoveredTemplate, setHoveredTemplate] = useState(null);

  return (
    <div className="p-6 bg-white rounded-lg shadow-lg">
      <h2 className="text-2xl font-semibold mb-4 text-gray-800">Choose Your Resume Template</h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {templates.map((template) => (
          <div
            key={template.id}
            className={`border rounded-lg p-4 cursor-pointer transition-transform transform hover:scale-105 ${
              selectedTemplate === template.id ? 'border-blue-600 shadow-lg' : 'border-gray-300'
            }`}
            onClick={() => onSelectTemplate(template.id)}
            onMouseEnter={() => setHoveredTemplate(template.id)}
            onMouseLeave={() => setHoveredTemplate(null)}
          >
            <img
              src={template.previewImage}
              alt={`${template.name} preview`}
              className="w-full h-40 object-cover rounded-md mb-3"
            />
            <h3 className="text-lg font-medium text-gray-900">{template.name}</h3>
            <p className="text-gray-600 text-sm">{template.description}</p>
            {selectedTemplate === template.id && (
              <div className="mt-2 text-blue-600 font-semibold">Selected</div>
            )}
            {hoveredTemplate === template.id && selectedTemplate !== template.id && (
              <div className="mt-2 text-gray-500 text-sm">Click to select</div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default TemplateSelector;
