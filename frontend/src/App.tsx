import { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import ChatInterface from './pages/ChatInterface';
import AgentWorkspace from './pages/AgentWorkspace';
import GitHubIntegration from './pages/GitHubIntegration';
import AuthPage from './pages/AuthPage';
import { Sun, Moon, Key, ShieldCheck } from 'lucide-react';

export default function App() {
  // In production: set VITE_API_BASE_URL to your Render backend URL
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';

  // Session & Identity
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [, setUserId] = useState<number | null>(Number(localStorage.getItem('userId')) || null);
  const [userEmail, setUserEmail] = useState<string>(localStorage.getItem('userEmail') || '');
  const [userName, setUserName] = useState<string>(localStorage.getItem('userName') || '');

  // Navigation & Theme
  const [currentView, setView] = useState('dashboard');
  const [themeMode, setThemeMode] = useState<'dark' | 'light'>('dark');

  // Workspaces
  const [projects, setProjects] = useState<any[]>([]);
  const [currentProjectId, setProjectId] = useState<number | null>(null);

  // Model & Credentials — key is loaded from .env (VITE_OPENROUTER_API_KEY)
  const OPENROUTER_KEY = import.meta.env.VITE_OPENROUTER_API_KEY || '';
  const [provider, setProvider] = useState(localStorage.getItem('provider') || 'openrouter');
  const [model, setModel] = useState(localStorage.getItem('model') || 'openai/gpt-4o-mini');
  const [apiKey, setApiKey] = useState(localStorage.getItem('apiKey') || OPENROUTER_KEY);

  useEffect(() => {
    if (token) {
      fetchProjects();
    }
  }, [token]);

  const handleLoginSuccess = (jwt: string, id: number, email: string, name: string) => {
    localStorage.setItem('token', jwt);
    localStorage.setItem('userId', String(id));
    localStorage.setItem('userEmail', email);
    localStorage.setItem('userName', name);
    
    setToken(jwt);
    setUserId(id);
    setUserEmail(email);
    setUserName(name);
  };

  const handleLogout = () => {
    localStorage.clear();
    setToken(null);
    setUserId(null);
    setUserEmail('');
    setUserName('');
    setProjects([]);
    setProjectId(null);
    setView('dashboard');
  };

  const fetchProjects = async () => {
    try {
      const response = await fetch(`${apiBaseUrl}/api/projects`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setProjects(data);
        if (data.length > 0) {
          setProjectId(data[0].id);
        } else {
          // Auto create default project workspace
          handleCreateProject("Primary Workspace");
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCreateProject = async (name: string) => {
    try {
      const response = await fetch(`${apiBaseUrl}/api/projects`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name, description: 'Workspace created for software audits.' })
      });
      if (response.ok) {
        const newProj = await response.json();
        setProjects(prev => [...prev, newProj]);
        setProjectId(newProj.id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const saveApiKey = (key: string) => {
    setApiKey(key);
    localStorage.setItem('apiKey', key);
  };

  const saveProvider = (prov: string) => {
    setProvider(prov);
    localStorage.setItem('provider', prov);
  };

  const saveModel = (mod: string) => {
    setModel(mod);
    localStorage.setItem('model', mod);
  };

  const toggleTheme = () => {
    setThemeMode(prev => prev === 'dark' ? 'light' : 'dark');
  };

  if (!token) {
    return <AuthPage onLoginSuccess={handleLoginSuccess} apiBaseUrl={apiBaseUrl} />;
  }

  return (
    <div className={`flex h-screen w-screen overflow-hidden ${themeMode === 'light' ? 'bg-slate-100 text-slate-900 light' : 'bg-slate-950 text-slate-100'}`}>
      
      {/* Side Navigation panel */}
      <Sidebar 
        currentView={currentView} 
        setView={setView} 
        projects={projects}
        currentProjectId={currentProjectId}
        setProjectId={setProjectId}
        createProject={handleCreateProject}
        userName={userName}
        onLogout={handleLogout}
      />

      {/* Main content body viewport */}
      <div className="flex-1 flex flex-col overflow-hidden relative">
        
        {/* Global Toolbar Header */}
        <div className="h-14 border-b border-slate-800/80 px-6 flex items-center justify-between shrink-0 glass-panel">
          <div className="flex items-center space-x-2 text-xs font-semibold text-slate-400">
            <span>Project Workspace:</span>
            <span className="text-purple-400 font-bold bg-purple-500/5 px-2 py-0.5 rounded border border-purple-500/10">
              {projects.find(p => p.id === currentProjectId)?.name || 'Default'}
            </span>
          </div>
          
          <div className="flex items-center space-x-4">
            {/* Theme Toggle */}
            <button 
              onClick={toggleTheme}
              className="p-2 text-slate-400 hover:text-slate-200 rounded-lg hover:bg-slate-800/40 transition-colors cursor-pointer"
              title="Toggle Theme"
            >
              {themeMode === 'dark' ? <Sun className="w-4.5 h-4.5" /> : <Moon className="w-4.5 h-4.5" />}
            </button>
          </div>
        </div>

        {/* Dynamic View Loader */}
        <div className="flex-1 flex overflow-hidden">
          {currentView === 'dashboard' && (
            <Dashboard 
              projectId={currentProjectId} 
              token={token} 
              apiBaseUrl={apiBaseUrl} 
              setView={setView} 
            />
          )}

          {currentView === 'chat' && (
            <ChatInterface 
              projectId={currentProjectId} 
              token={token} 
              apiBaseUrl={apiBaseUrl} 
              provider={provider}
              setProvider={saveProvider}
              model={model}
              setModel={saveModel}
              apiKey={apiKey}
            />
          )}

          {currentView === 'agents' && (
            <AgentWorkspace 
              projectId={currentProjectId} 
              token={token} 
              apiBaseUrl={apiBaseUrl} 
              provider={provider}
              model={model}
              apiKey={apiKey}
            />
          )}

          {currentView === 'github' && (
            <GitHubIntegration 
              projectId={currentProjectId} 
              token={token} 
              apiBaseUrl={apiBaseUrl} 
            />
          )}

          {/* Configuration View */}
          {currentView === 'settings' && (
            <div className="flex-1 overflow-y-auto p-6 md:p-8 space-y-8 select-none">
              <div>
                <h2 className="text-3xl font-extrabold tracking-tight heading-gradient">Workspace Settings</h2>
                <p className="text-xs text-slate-400 mt-1">Configure models and developer credentials for your agent integrations.</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Credentials Panel */}
                <div className="glass-panel p-6 rounded-3xl space-y-5">
                  <h3 className="text-sm font-bold text-slate-300 flex items-center gap-2 uppercase tracking-wider text-[11px]">
                    <Key className="w-4 h-4 text-purple-400" /> API Access Tokens
                  </h3>
                  
                  <div className="space-y-3">
                    <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block ml-1">AI Provider Token (Gemini/OpenAI)</label>
                    <input 
                      type="password"
                      placeholder="Paste API Access Token..."
                      value={apiKey}
                      onChange={(e) => saveApiKey(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 focus:border-purple-500 rounded-xl px-4 py-3 text-xs text-slate-300 focus:outline-none"
                    />
                    <p className="text-[10px] text-slate-500 italic ml-1">
                      If left empty, all AI models will run in a simulated developer engine, providing realistic offline specs and review outputs.
                    </p>
                  </div>
                </div>

                {/* Identity Card */}
                <div className="glass-panel p-6 rounded-3xl space-y-5">
                  <h3 className="text-sm font-bold text-slate-300 flex items-center gap-2 uppercase tracking-wider text-[11px]">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" /> Security & Profile
                  </h3>

                  <div className="space-y-2 text-xs">
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">Logged in Name</span>
                      <span className="text-slate-300 font-semibold">{userName}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">Email Account</span>
                      <span className="text-slate-300 font-semibold">{userEmail}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">JWT Status</span>
                      <span className="text-emerald-400 font-bold">Authorized Session</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
