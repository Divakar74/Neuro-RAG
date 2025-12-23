import React, { useState } from 'react';

const ResumeVerificationModal = ({ open, parsed, onConfirm, onClose }) => {
  const [form, setForm] = useState(() => ({
    name: parsed?.name || '',
    role: parsed?.role || '',
    education: parsed?.education || [],
    skills: parsed?.skills || [],
    experienceYears: parsed?.experienceYears || 0,
  }));

  if (!open) return null;

  const updateField = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-800">Verify Extracted Resume Data</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700">✕</button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm text-gray-600">Name</label>
            <input value={form.name} onChange={(e) => updateField('name', e.target.value)} className="w-full border rounded-md px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm text-gray-600">Target Role</label>
            <input value={form.role} onChange={(e) => updateField('role', e.target.value)} className="w-full border rounded-md px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm text-gray-600">Education (comma separated)</label>
            <input value={(form.education || []).join(', ')} onChange={(e) => updateField('education', e.target.value.split(',').map(s => s.trim()))} className="w-full border rounded-md px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm text-gray-600">Skills (comma separated)</label>
            <input value={(form.skills || []).join(', ')} onChange={(e) => updateField('skills', e.target.value.split(',').map(s => s.trim()))} className="w-full border rounded-md px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm text-gray-600">Total Years of Experience</label>
            <input type="number" value={form.experienceYears} onChange={(e) => updateField('experienceYears', Number(e.target.value))} className="w-full border rounded-md px-3 py-2" />
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button onClick={onClose} className="px-4 py-2 rounded-md border">Cancel</button>
          <button onClick={() => onConfirm(form)} className="px-4 py-2 rounded-md bg-indigo-600 text-white">Save</button>
        </div>
      </div>
    </div>
  );
};

export default ResumeVerificationModal;















