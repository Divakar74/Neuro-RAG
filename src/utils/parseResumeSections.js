export function parseResumeSections(resumeData) {
  const text = (resumeData?.rawText || '').replace(/\r\n/g, '\n');
  const sections = {
    skills: [],
    education: [],
    projects: [],
    experience: [],
    achievements: [],
  };

  if (!text) return sections;

  const lower = text.toLowerCase();

  // Helper to extract a section by header keywords
  const extractByHeaders = (headers) => {
    for (const h of headers) {
      const regex = new RegExp(`(^|\n)\s*${h}[\t :]*\n([\s\S]*?)(\n\s*[A-Z][A-Za-z ]{2,}:|\n\n|$)`, 'i');
      const m = text.match(regex);
      if (m && m[2] && m[2].trim().length > 0) {
        return m[2].trim();
      }
    }
    return '';
  };

  const skillsRaw = extractByHeaders(['skills', 'technical skills', 'core competencies', 'competencies', 'expertise', 'technologies', 'tools']);
  if (skillsRaw) {
    const tokens = skillsRaw
      .split(/\n|,|•|\u2022|\u25CF|\u00B7|\t|\s{2,}/)
      .map(s => s.trim())
      .filter(Boolean)
      .filter(s => s.length <= 60);
    const uniq = Array.from(new Set(tokens));
    sections.skills = uniq.slice(0, 50);
  }

  const eduRaw = extractByHeaders(['education', 'academic background', 'qualifications', 'degrees', 'certifications']);
  if (eduRaw) {
    sections.education = eduRaw.split('\n').map(l => l.trim()).filter(Boolean).slice(0, 20);
  }

  const projRaw = extractByHeaders(['projects', 'project experience', 'key projects', 'notable projects']);
  if (projRaw) {
    const lines = projRaw.split('\n').map(l => l.replace(/^\s*[•\-\*]\s+/, '').trim()).filter(Boolean);
    sections.projects = lines.slice(0, 30);
  }

  const expRaw = extractByHeaders(['work experience', 'professional experience', 'experience', 'employment', 'work history', 'career history']);
  if (expRaw) {
    const lines = expRaw.split('\n').map(l => l.replace(/^\s*[•\-\*]\s+/, '').trim()).filter(Boolean);
    sections.experience = lines.slice(0, 40);
  }

  const achRaw = extractByHeaders(['achievements', 'accomplishments', 'awards', 'recognitions']);
  if (achRaw) {
    const lines = achRaw.split('\n').map(l => l.replace(/^\s*[•\-\*]\s+/, '').trim()).filter(Boolean);
    sections.achievements = lines.slice(0, 20);
  }

  return sections;
}




