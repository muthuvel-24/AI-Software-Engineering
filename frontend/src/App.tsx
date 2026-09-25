import { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import ChatInterface from './pages/ChatInterface';
import AgentWorkspace from './pages/AgentWorkspace';
import GitHubIntegration from './pages/GitHubIntegration';
import AuthPage from './pages/AuthPage';
import { Sun, Moon, Key, ShieldCheck, CheckCircle2, AlertCircle, RefreshCw, Cpu, Sparkles } from 'lucide-react';

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

  // API Key Testing State
  const [isTestingKey, setIsTestingKey] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

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
    setTestResult(null);
  };

  const saveProvider = (prov: string) => {
    setProvider(prov);
    localStorage.setItem('provider', prov);
    setTestResult(null);
    if (prov === 'openrouter') {
      saveModel('openai/gpt-4o-mini');
    } else if (prov === 'gemini') {
      saveModel('gemini-1.5-flash');
    } else if (prov === 'openai') {
      saveModel('gpt-4o-mini');
    }
  };

  const saveModel = (mod: string) => {
    setModel(mod);
    localStorage.setItem('model', mod);
    setTestResult(null);
  };

  const handleTestApiKey = async () => {
    if (!currentProjectId) {
      setTestResult({ success: false, message: 'Please create or select a project workspace first.' });
      return;
    }
    setIsTestingKey(true);
    setTestResult(null);
    try {
      const res = await fetch(`${apiBaseUrl}/api/agents/analyze-requirements`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          projectId: currentProjectId,
          prompt: 'Respond with exactly: Connection successful.',
          provider,
          model,
          apiKey
        })
      });

      if (res.ok) {
        const text = await res.text();
        if (text.includes("Error communicating with") || text.includes("Error calling AI provider")) {
          setTestResult({ success: false, message: text.split("\n\n")[0] });
        } else {
          setTestResult({ success: true, message: `Connected to ${provider.toUpperCase()} (${model}) successfully!` });
        }
      } else {
        const errText = await res.text();
        setTestResult({ success: false, message: `Server error (${res.status}): ${errText}` });
      }
    } catch (e: any) {
      setTestResult({ success: false, message: `Network error: ${e.message}` });
    } finally {
      setIsTestingKey(false);
    }
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
              setProvider={saveProvider}
              model={model}
              setModel={saveModel}
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
                {/* Credentials & Model Panel */}
                <div className="glass-panel p-6 rounded-3xl space-y-5">
                  <div className="flex justify-between items-center">
                    <h3 className="text-sm font-bold text-slate-300 flex items-center gap-2 uppercase tracking-wider text-[11px]">
                      <Key className="w-4 h-4 text-purple-400" /> AI Provider & Credentials
                    </h3>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                      apiKey ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                    }`}>
                      {apiKey ? <CheckCircle2 className="w-3 h-3" /> : <Sparkles className="w-3 h-3" />}
                      {apiKey ? 'API Key Active' : 'Offline Simulation'}
                    </span>
                  </div>

                  {/* Provider Selector */}
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block ml-1">AI Provider</label>
                    <select
                      value={provider}
                      onChange={(e) => saveProvider(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 focus:border-purple-500 rounded-xl px-3.5 py-2.5 text-xs text-slate-300 focus:outline-none"
                    >
                      <option value="openrouter">OpenRouter (Recommended - GPT-4o, Claude, Gemini, Llama)</option>
                      <option value="gemini">Google Gemini API (Official Direct Endpoint)</option>
                      <option value="openai">OpenAI API (Official Direct Endpoint)</option>
                    </select>
                  </div>

                  {/* Model Selector */}
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block ml-1">Active Model</label>
                    <select
                      value={model}
                      onChange={(e) => saveModel(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 focus:border-purple-500 rounded-xl px-3.5 py-2.5 text-xs text-slate-300 focus:outline-none"
                    >
                      {provider === 'openrouter' && (
                        <>
                          <option value="openai/gpt-4o-mini">openai/gpt-4o-mini (Fast & Intelligent)</option>
                          <option value="google/gemini-2.5-flash">google/gemini-2.5-flash (High Speed)</option>
                          <option value="anthropic/claude-3.5-haiku">anthropic/claude-3.5-haiku (Precision Code)</option>
                          <option value="meta-llama/llama-3.3-70b-instruct:free">meta-llama/llama-3.3-70b-instruct (Free Tier)</option>
                          <option value="deepseek/deepseek-r1">deepseek/deepseek-r1 (Reasoning)</option>
                        </>
                      )}
                      {provider === 'gemini' && (
                        <>
                          <option value="gemini-1.5-flash">gemini-1.5-flash (Fast & Versatile)</option>
                          <option value="gemini-2.0-flash">gemini-2.0-flash (Next Gen High Speed)</option>
                          <option value="gemini-1.5-pro">gemini-1.5-pro (Complex Reasoning)</option>
                        </>
                      )}
                      {provider === 'openai' && (
                        <>
                          <option value="gpt-4o-mini">gpt-4o-mini (Recommended)</option>
                          <option value="gpt-4o">gpt-4o (Flagship Multimodal)</option>
                          <option value="gpt-4-turbo">gpt-4-turbo</option>
                        </>
                      )}
                    </select>
                  </div>
                  
                  {/* API Key Input */}
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block ml-1">
                      {provider === 'openrouter' ? 'OpenRouter API Key (sk-or-v1-...)' : provider === 'gemini' ? 'Google Gemini API Key (AIzaSy...)' : 'OpenAI API Key (sk-proj-...)'}
                    </label>
                    <input 
                      type="password"
                      placeholder={provider === 'openrouter' ? 'sk-or-v1-...' : provider === 'gemini' ? 'AIzaSy...' : 'sk-proj-...'}
                      value={apiKey}
                      onChange={(e) => saveApiKey(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 focus:border-purple-500 rounded-xl px-4 py-2.5 text-xs text-slate-300 focus:outline-none font-mono"
                    />
                    <p className="text-[10px] text-slate-500 italic ml-1">
                      Leave empty to run in simulated offline developer mode. Paste your custom token to run live inference with your selected model.
                    </p>
                  </div>

                  {/* Test Connection Button & Result */}
                  <div className="pt-2">
                    <button
                      onClick={handleTestApiKey}
                      disabled={isTestingKey}
                      className="w-full bg-slate-900 hover:bg-slate-850 border border-purple-500/30 hover:border-purple-500 text-purple-300 font-semibold py-2.5 rounded-xl text-xs transition-colors flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                    >
                      {isTestingKey ? (
                        <>
                          <RefreshCw className="w-3.5 h-3.5 animate-spin text-purple-400" />
                          Testing Connection...
                        </>
                      ) : (
                        <>
                          <Cpu className="w-3.5 h-3.5 text-purple-400" />
                          Test API Key Connection
                        </>
                      )}
                    </button>

                    {testResult && (
                      <div className={`mt-3 p-3 rounded-xl text-xs flex items-start gap-2 ${
                        testResult.success 
                          ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20' 
                          : 'bg-rose-500/10 text-rose-300 border border-rose-500/20'
                      }`}>
                        {testResult.success ? <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400 mt-0.5" /> : <AlertCircle className="w-4 h-4 shrink-0 text-rose-400 mt-0.5" />}
                        <span className="leading-relaxed">{testResult.message}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Identity & Deployment Card */}
                <div className="glass-panel p-6 rounded-3xl space-y-5">
                  <h3 className="text-sm font-bold text-slate-300 flex items-center gap-2 uppercase tracking-wider text-[11px]">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" /> Account & Environment
                  </h3>

                  <div className="space-y-2 text-xs">
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">Developer Profile</span>
                      <span className="text-slate-300 font-semibold">{userName}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">Email Address</span>
                      <span className="text-slate-300 font-semibold">{userEmail}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">Auth Engine</span>
                      <span className="text-emerald-400 font-bold">HMAC-SHA256 JWT</span>
                    </div>
                    <div className="flex justify-between py-2 border-b border-slate-800">
                      <span className="text-slate-500 font-medium">Database Persistence</span>
                      <span className="text-cyan-400 font-mono font-semibold text-[11px]">PostgreSQL 18</span>
                    </div>
                    <div className="flex justify-between py-2">
                      <span className="text-slate-500 font-medium">Project Workspace ID</span>
                      <span className="text-purple-400 font-mono font-semibold">{currentProjectId || 'None Selected'}</span>
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
