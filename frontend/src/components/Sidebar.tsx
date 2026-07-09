import { useState, type FormEvent } from 'react';
import { 
  LayoutDashboard, 
  MessageSquare, 
  Settings, 
  LogOut, 
  GitBranch,
  FolderOpen,
  Code2,
  Cpu
} from 'lucide-react';

interface SidebarProps {
  currentView: string;
  setView: (view: string) => void;
  projects: any[];
  currentProjectId: number | null;
  setProjectId: (id: number) => void;
  createProject: (name: string) => void;
  userName: string;
  onLogout: () => void;
}

export default function Sidebar({
  currentView,
  setView,
  projects,
  currentProjectId,
  setProjectId,
  createProject,
  userName,
  onLogout
}: SidebarProps) {
  const [newProjectName, setNewProjectName] = useState('');

  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'chat', label: 'AI Chat Panel', icon: MessageSquare },
    { id: 'agents', label: 'Agent Workspace', icon: Cpu },
    { id: 'github', label: 'GitHub Integrator', icon: GitBranch },
    { id: 'settings', label: 'Configuration', icon: Settings },
  ];

  const handleCreateProjectSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (newProjectName.trim()) {
      createProject(newProjectName);
      setNewProjectName('');
    }
  };

  return (
    <div className="w-68 h-screen glass-panel flex flex-col border-r border-slate-800 shrink-0 select-none">
      {/* Brand Header */}
      <div className="p-6 border-b border-slate-800 flex items-center space-x-3">
        <div className="w-9 h-9 rounded-xl bg-purple-600 flex items-center justify-center shadow-lg shadow-purple-500/30">
          <Code2 className="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 className="font-bold text-sm tracking-wide bg-gradient-to-r from-purple-400 to-blue-400 bg-clip-text text-transparent title-font">
            MUTHU
          </h1>
          <p className="text-[10px] text-slate-500 font-medium">AI Dev Workspace</p>
        </div>
      </div>

      {/* Project Selector */}
      <div className="p-4 border-b border-slate-800">
        <div className="flex items-center justify-between mb-2">
          <label className="text-[10px] uppercase font-bold tracking-wider text-slate-400 flex items-center gap-1.5">
            <FolderOpen className="w-3.5 h-3.5" /> Workspace Projects
          </label>
        </div>
        
        {projects.length === 0 ? (
          <p className="text-xs text-slate-500 italic p-2">No active projects</p>
        ) : (
          <select 
            value={currentProjectId || ''} 
            onChange={(e) => setProjectId(Number(e.target.value))}
            className="w-full bg-slate-900 border border-slate-800 text-xs rounded-lg px-2.5 py-2 text-slate-300 focus:outline-none focus:border-purple-500"
          >
            {projects.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        )}

        <form onSubmit={handleCreateProjectSubmit} className="mt-3 flex gap-2">
          <input 
            type="text" 
            placeholder="New Project..."
            value={newProjectName}
            onChange={(e) => setNewProjectName(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 text-xs rounded-lg px-2.5 py-1.5 text-slate-300 focus:outline-none focus:border-purple-500"
          />
          <button 
            type="submit" 
            className="bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs px-2.5 rounded-lg transition-colors cursor-pointer"
          >
            +
          </button>
        </form>
      </div>

      {/* Main Menu Links */}
      <div className="flex-1 py-4 overflow-y-auto px-3 space-y-1">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentView === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setView(item.id)}
              className={`w-full flex items-center space-x-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 cursor-pointer ${
                isActive 
                  ? 'bg-purple-600/10 text-purple-300 border-l-4 border-purple-500 shadow-md shadow-purple-500/5' 
                  : 'text-slate-400 hover:bg-slate-800/40 hover:text-slate-200'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-purple-400' : ''}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      {/* Footer Profile */}
      <div className="p-4 border-t border-slate-800 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-500 to-blue-500 flex items-center justify-center text-xs font-bold text-white shadow-md">
            {userName ? userName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="max-w-[110px] truncate">
            <p className="text-xs font-semibold text-slate-200 truncate">{userName || 'Developer'}</p>
            <p className="text-[10px] text-slate-500 truncate">Workspace User</p>
          </div>
        </div>
        <button 
          onClick={onLogout}
          className="p-2 text-slate-500 hover:text-red-400 rounded-lg hover:bg-slate-800/30 transition-colors cursor-pointer"
          title="Logout"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
