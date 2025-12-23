import React, { useCallback, useMemo } from 'react';
import ReactFlow, {
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
} from 'reactflow';
import 'reactflow/dist/style.css';

const SkillGraph = ({ skillData, skillDependencies }) => {
  const computedDeps = React.useMemo(() => {
    if (skillDependencies && Object.keys(skillDependencies).length > 0) return skillDependencies;
    // Build a simple chain graph from sorted skills as fallback
    const sorted = [...(skillData || [])].sort((a,b) => b.level - a.level).map(s => s.name);
    const deps = {};
    for (let i = 0; i < sorted.length - 1; i++) {
      const from = sorted[i+1];
      const to = sorted[i];
      if (!deps[from]) deps[from] = [];
      deps[from].push(to);
    }
    return deps;
  }, [skillData, skillDependencies]);
  // Create nodes from skill data (use name as id if numeric id missing)
  const initialNodes = useMemo(() => {
    const uniqueSkills = (skillData || [])
      .filter(s => s && s.name)
      .reduce((acc, s) => {
        acc[s.name] = s; return acc;
      }, {});
    return Object.values(uniqueSkills).map((skill, index) => ({
      id: (skill.id ? String(skill.id) : skill.name),
      position: { x: (index % 5) * 180, y: Math.floor(index / 5) * 120 },
      data: { label: `${skill.name}\nLevel: ${skill.level}/5` },
      style: {
        background: getSkillColor(skill.level),
        color: 'white',
        border: '2px solid #fff',
        borderRadius: '8px',
        padding: '10px',
        fontSize: '12px',
        textAlign: 'center',
        minWidth: '120px',
      },
    }));
  }, [skillData]);

  // Create edges from dependencies (supports name->names mapping via computedDeps)
  const initialEdges = useMemo(() => {
    if (Array.isArray(skillDependencies)) {
      return skillDependencies
        .filter(dep => dep && dep.prerequisiteSkillId && dep.skillId)
        .map(dep => ({
          id: `e${dep.prerequisiteSkillId}-${dep.skillId}`,
          source: dep.prerequisiteSkillId.toString(),
          target: dep.skillId.toString(),
          type: 'smoothstep',
          style: { stroke: '#6b7280', strokeWidth: 2 },
          markerEnd: { type: 'arrowclosed', color: '#6b7280' },
        }));
    }
    // computedDeps: { fromName: [toName, ...] }
    const edges = [];
    Object.entries(computedDeps || {}).forEach(([from, tos]) => {
      (tos || []).forEach(to => {
        edges.push({
          id: `e-${from}-${to}`,
          source: from,
          target: to,
          type: 'smoothstep',
          style: { stroke: '#6b7280', strokeWidth: 2 },
          markerEnd: { type: 'arrowclosed', color: '#6b7280' },
        });
      });
    });
    return edges;
  }, [skillDependencies, computedDeps]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback(
    (params) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  return (
    <div className="bg-white p-6 rounded-lg shadow-lg">
      <h3 className="text-xl font-semibold mb-4 text-gray-800">Skill Dependency Graph</h3>
      <div className="h-96 border border-gray-200 rounded-lg">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          fitView
          attributionPosition="top-right"
        >
          <Controls />
          <MiniMap />
          <Background color="#aaa" gap={16} />
        </ReactFlow>
      </div>
      <div className="mt-4 text-sm text-gray-600">
        <p>Drag nodes to rearrange. Arrows show skill prerequisites.</p>
      </div>
    </div>
  );
};

// Helper function to get color based on skill level
const getSkillColor = (level) => {
  switch (level) {
    case 1: return '#ef4444'; // red
    case 2: return '#f97316'; // orange
    case 3: return '#eab308'; // yellow
    case 4: return '#22c55e'; // green
    case 5: return '#3b82f6'; // blue
    default: return '#6b7280'; // gray
  }
};

export default SkillGraph;
