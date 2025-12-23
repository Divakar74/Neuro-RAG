import React from 'react';

const isBullet = (line) => /^(•|\-|\*|\u2022|\u25CF|\u25E6|\u00B7)\s+/.test(line.trim());

const normalizeLine = (line) => {
  return line
    .replace(/\u00AD/g, '') // soft hyphen
    .replace(/ﬁ/g, 'fi')
    .replace(/ﬂ/g, 'fl')
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, "'")
    .replace(/\u00A0/g, ' ')
    .replace(/[ \t]{2,}/g, ' ')
    .trimEnd();
};

const groupLines = (text) => {
  if (!text) return [];
  const lines = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
  const groups = [];
  let current = [];
  lines.forEach(raw => {
    const line = normalizeLine(raw);
    if (line.trim() === '') {
      if (current.length) {
        groups.push(current);
        current = [];
      }
      return;
    }
    current.push(line);
  });
  if (current.length) groups.push(current);
  return groups;
};

const renderGroup = (lines, key) => {
  // If majority lines look like bullets, render a list
  const bulletCount = lines.filter(isBullet).length;
  if (bulletCount >= Math.max(2, Math.ceil(lines.length / 2))) {
    return (
      <ul key={key} className="list-disc pl-5 space-y-1">
        {lines.map((l, i) => (
          <li key={i}>
            {l.replace(/^(•|\-|\*|\u2022|\u25CF|\u25E6|\u00B7)\s+/, '')}
          </li>
        ))}
      </ul>
    );
  }

  // Otherwise render a paragraph; join lines that are not bullets
  return (
    <p key={key} className="leading-6">
      {lines
        .map(l => (isBullet(l) ? l.replace(/^.+?\s+/, '') : l))
        .join(' ')}
    </p>
  );
};

const ResumeContentViewer = ({ text, showSummaryOnly = false }) => {
  // If showSummaryOnly is true or text is too long, show a clean summary
  if (showSummaryOnly || (text && text.length > 500)) {
    const wordCount = text ? text.split(/\s+/).length : 0;
    const charCount = text ? text.length : 0;
    const hasContact = text ? /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/.test(text) : false;
    const hasPhone = text ? /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/.test(text) : false;
    
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
        <div className="text-sm text-gray-600 mb-2">
          <strong>Resume Summary:</strong>
        </div>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="font-medium text-gray-700">Word count:</span> {wordCount}
          </div>
          <div>
            <span className="font-medium text-gray-700">Characters:</span> {charCount}
          </div>
          <div>
            <span className="font-medium text-gray-700">Contact info:</span> {hasContact ? 'Present' : 'Not detected'}
          </div>
          <div>
            <span className="font-medium text-gray-700">Phone number:</span> {hasPhone ? 'Present' : 'Not detected'}
          </div>
        </div>
        <div className="mt-3 text-xs text-gray-500">
          Resume content processed and analyzed successfully.
        </div>
      </div>
    );
  }
  
  // Fallback for very short text - still don't show raw content in most cases
  const groups = groupLines(text);
  if (!groups.length) {
    return (
      <div className="text-gray-500 text-sm">No resume content available</div>
    );
  }
  
  // Only show processed content for very short resumes
  if (text && text.length <= 200) {
    return (
      <div className="prose prose-sm max-w-none space-y-3">
        {groups.slice(0, 3).map((g, idx) => renderGroup(g, idx))}
        {groups.length > 3 && (
          <div className="text-gray-500 text-sm">... and more content</div>
        )}
      </div>
    );
  }
  
  // For longer content, always show summary
  return (
    <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
      <div className="text-sm text-gray-600">
        Resume content loaded and processed ({text ? text.split(/\s+/).length : 0} words)
      </div>
    </div>
  );
};

export default ResumeContentViewer;




