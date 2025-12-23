import React, { useEffect, useState } from 'react';
import { fetchFeedback, fetchSessionResponses, fetchSessionResponsesByToken } from '../services/assessmentService';

const Section = ({ title, children }) => (
  <div className="bg-white rounded-xl shadow p-4 border border-slate-100">
    <h4 className="text-sm font-semibold text-slate-700 mb-2">{title}</h4>
    <div className="text-sm text-slate-700 leading-relaxed whitespace-pre-line">{children}</div>
  </div>
);

const FeedbackCard = ({ sessionId }) => {
  const [text, setText] = useState('');
  const [responses, setResponses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const load = async () => {
      if (!sessionId) return;
      setLoading(true);
      setError(null);
      try {
        const [feedbackRes, responsesRes] = await Promise.all([
          fetchFeedback(sessionId),
          fetchSessionResponses(sessionId)
        ]);
        setText(feedbackRes.data || '');
        setResponses(responsesRes || []);
      } catch (e) {
        console.error('Error loading feedback:', e);
        setError('Unable to load feedback.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [sessionId]);

  const extractSection = (label) => {
    const pattern = new RegExp(`${label}[:\n]+([\s\S]*?)(?=\n[-A-Za-z ]+[:]|$)`, 'i');
    const m = text.match(pattern);
    return m ? m[1].trim() : '';
  };

  if (!sessionId) return null;

  return (
    <div className="space-y-4">
      {loading && <div className="text-slate-600 text-sm">Generating feedback…</div>}
      {error && <div className="text-red-600 text-sm">{error}</div>}
      {!loading && !error && text && (
        <>
          <Section title="User Responses Summary">
            {responses.length > 0 ? (
              responses.map((r, idx) => (
                <div key={r.id || idx} className="mb-3">
                  <strong>Q{idx + 1}:</strong> {r.question?.questionText || 'N/A'}
                  <br />
                  <strong>A:</strong> {r.responseText || 'N/A'}
                </div>
              ))
            ) : (
              'No responses available.'
            )}
          </Section>
          <Section title="Skill Strengths & Weaknesses">{extractSection('Skill Strengths & Weaknesses')}</Section>
          <Section title="Consistency Report">{extractSection('Consistency Report')}</Section>
          <Section title="Suggested Next Steps">{extractSection('Suggested Next Steps')}</Section>
        </>
      )}
    </div>
  );
};

export default FeedbackCard;















