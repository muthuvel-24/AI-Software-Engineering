import { useState } from 'react';
import { 
  FileText, 
  Database, 
  Code, 
  Bug, 
  BookOpen, 
  Layers, 
  Terminal, 
  Download, 
  Copy, 
  Check, 
  ChevronRight,
  Sparkles,
  ShieldAlert,
  Server,
  Cpu
} from 'lucide-react';

interface AgentWorkspaceProps {
  projectId: number | null;
  token: string;
  apiBaseUrl: string;
  provider: string;
  model: string;
  apiKey: string;
}

export default function AgentWorkspace({
  projectId,
  token,
  apiBaseUrl,
  provider,
  model,
  apiKey
}: AgentWorkspaceProps) {
  const [activeTab, setActiveTab] = useState('requirements');
  const [loading, setLoading] = useState(false);
  const [copiedText, setCopiedText] = useState(false);

  // Agent State Outputs
  const [requirementsOutput, setRequirementsOutput] = useState('');
  const [dbSchemaOutput, setDbSchemaOutput] = useState('');
  const [codeGenOutput, setCodeGenOutput] = useState('');
  const [sqlGenOutput, setSqlGenOutput] = useState<any>(null);
  const [apiDocOutput, setApiDocOutput] = useState<any>(null);
  const [unitTestOutput, setUnitTestOutput] = useState<any>(null);
  const [deploymentOutput, setDeploymentOutput] = useState('');

  // Code Review Agent specific states
  const [codeToReview, setCodeToReview] = useState('');
  const [reviewReport, setReviewReport] = useState<any>(null);
  const [bugReports, setBugReports] = useState<any[]>([]);
  const [selectedBug, setSelectedBug] = useState<any>(null);

  // Input states
  const [reqInput, setReqInput] = useState('');
  const [sqlPrompt, setSqlPrompt] = useState('');
  const [apiCodeInput, setApiCodeInput] = useState('');
  const [testCodeInput, setTestCodeInput] = useState('');
  const [deployCodeInput, setDeployCodeInput] = useState('');

  const triggerAgent = async (agentName: string, promptText: string, callback: (res: any) => void) => {
    if (!projectId) return;
    setLoading(true);
    try {
      const response = await fetch(`${apiBaseUrl}/api/agents/${agentName}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          projectId,
          prompt: promptText,
          provider,
          model,
          apiKey
        })
      });

      if (response.ok) {
        if (response.headers.get('Content-Type')?.includes('application/json')) {
          const data = await response.json();
          callback(data);
        } else {
          const text = await response.text();
          callback(text);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(true);
    setTimeout(() => setCopiedText(false), 2000);
  };

  const tabs = [
    { id: 'requirements', label: 'Requirements', icon: FileText },
    { id: 'db-schema', label: 'DB Schema', icon: Database },
    { id: 'code-gen', label: 'Code Gen', icon: Code },
    { id: 'code-review', label: 'Code Review', icon: Bug },
    { id: 'sql-gen', label: 'SQL Sandbox', icon: Terminal },
    { id: 'api-docs', label: 'API Docs', icon: BookOpen },
    { id: 'unit-tests', label: 'Unit Tests', icon: Layers },
    { id: 'deployment', label: 'Deployment', icon: Server },
  ];

  if (!projectId) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center select-none">
        <Cpu className="w-16 h-16 text-slate-700 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold text-slate-300">No Active Project Workspace</h3>
        <p className="text-slate-500 text-sm mt-1 max-w-sm">
          Please select or create a project workspace in the sidebar to work with AI agents.
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden select-none">
      
      {/* Agent Workspace Navigation Bar */}
      <div className="border-b border-slate-800 bg-slate-950/20 px-6 py-2 overflow-x-auto shrink-0 flex gap-2">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-semibold tracking-wide whitespace-nowrap transition-all cursor-pointer ${
                isActive 
                  ? 'bg-purple-600/10 border border-purple-500/40 text-purple-300 shadow-md shadow-purple-500/5' 
                  : 'text-slate-400 hover:bg-slate-900/40 hover:text-slate-300 border border-transparent'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Main Tab View contents */}
      <div className="flex-1 overflow-y-auto p-6 md:p-8 space-y-6">
        
        {loading && (
          <div className="fixed inset-0 bg-slate-950/50 backdrop-blur-sm z-50 flex items-center justify-center">
            <div className="glass-panel p-6 rounded-2xl flex items-center space-x-3 border border-purple-500/30">
              <span className="text-xs font-semibold text-purple-300 cursor-pulse uppercase tracking-wider">Agent executing workflow</span>
              <div className="animate-spin rounded-full h-4 w-4 border-2 border-purple-500 border-t-transparent"></div>
            </div>
          </div>
        )}

        {/* 1. Requirements Analyzer Tab */}
        {activeTab === 'requirements' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-purple-400" /> Agent Input
                </h3>
                <p className="text-xs text-slate-500 mb-4">Paste raw requirement texts or project user stories.</p>
                <textarea 
                  value={reqInput}
                  onChange={(e) => setReqInput(e.target.value)}
                  placeholder="Example: Create a book listing library database application with user roles, search features, checkout counters, and JWT auth backend."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none"
                />
              </div>
              <button 
                onClick={() => triggerAgent('analyze-requirements', reqInput, setRequirementsOutput)}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Analyze Requirements
              </button>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">Roadmap Output</span>
                {requirementsOutput && (
                  <button onClick={() => handleCopy(requirementsOutput)} className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer">
                    {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} {copiedText ? 'Copied' : 'Copy'}
                  </button>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 leading-relaxed font-mono whitespace-pre-line">
                {requirementsOutput || 'Analysis report output will display here.'}
              </div>
            </div>
          </div>
        )}

        {/* 2. Database Schema Tab */}
        {activeTab === 'db-schema' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Schema Request</h3>
                <p className="text-xs text-slate-500 mb-4">Paste the requirements analyzer output to generate database models.</p>
                <textarea 
                  placeholder="Paste requirements analysis here..."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none"
                />
              </div>
              <button 
                onClick={() => triggerAgent('generate-db-schema', requirementsOutput, setDbSchemaOutput)}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Generate DB Schema
              </button>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">ER Diagram & SQL Scripts</span>
                {dbSchemaOutput && (
                  <button onClick={() => handleCopy(dbSchemaOutput)} className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer">
                    {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} {copiedText ? 'Copied' : 'Copy'}
                  </button>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 font-mono whitespace-pre leading-relaxed">
                {dbSchemaOutput || 'Generated SQL and ER relationships will display here.'}
              </div>
            </div>
          </div>
        )}

        {/* 3. Code Generator Boilerplate Tab */}
        {activeTab === 'code-gen' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Boilerplate Specifier</h3>
                <p className="text-xs text-slate-500 mb-4">Input specifications for folder structure and boilerplate layout.</p>
                <textarea 
                  placeholder="Example: Spring Boot CRUD for Library system with React table form controllers."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none"
                />
              </div>
              <div className="flex gap-2">
                <button 
                  onClick={() => triggerAgent('generate-code', dbSchemaOutput || reqInput, setCodeGenOutput)}
                  className="flex-1 bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
                >
                  Generate Files & Tree
                </button>
                
                {codeGenOutput && (
                  <button 
                    onClick={async () => {
                      try {
                        const res = await fetch(`${apiBaseUrl}/api/agents/code/download`, {
                          method: 'POST',
                          headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                          body: JSON.stringify({ name: 'ai-gen-project', prompt: codeGenOutput })
                        });
                        if (res.ok) {
                          const blob = await res.blob();
                          const url = window.URL.createObjectURL(blob);
                          const a = document.createElement('a');
                          a.href = url;
                          a.download = 'ai_gen_boilerplate.zip';
                          a.click();
                        }
                      } catch (e) {
                        console.error(e);
                      }
                    }}
                    className="bg-slate-900 hover:bg-slate-850 border border-slate-800 px-4 rounded-xl cursor-pointer"
                    title="Download Code ZIP"
                  >
                    <Download className="w-4 h-4 text-slate-400" />
                  </button>
                )}
              </div>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">Folder Tree & Preview</span>
                {codeGenOutput && (
                  <button onClick={() => handleCopy(codeGenOutput)} className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer">
                    {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} Copy Codes
                  </button>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 font-mono whitespace-pre leading-relaxed">
                {codeGenOutput || 'Boilerplate structure and code previews will display here.'}
              </div>
            </div>
          </div>
        )}

        {/* 4. Code Review & Bug Detector Tab */}
        {activeTab === 'code-review' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            
            {/* Input Code Panel */}
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[520px] lg:col-span-1">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Submit Codes</h3>
                <p className="text-xs text-slate-500 mb-4">Paste code blocks to scan for vulnerabilities or style faults.</p>
                <textarea 
                  value={codeToReview}
                  onChange={(e) => setCodeToReview(e.target.value)}
                  placeholder="Paste Java, JavaScript, Python or SQL code here..."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[340px] resize-none font-mono"
                />
              </div>
              <button 
                onClick={() => triggerAgent('review-code', codeToReview, async (data) => {
                  setReviewReport(data);
                  // Load bugs list associated with review
                  const bugsRes = await fetch(`${apiBaseUrl}/api/agents/reviews/${data.id}/bugs`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                  });
                  if (bugsRes.ok) {
                    const bugs = await bugsRes.json();
                    setBugReports(bugs);
                  }
                })}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3.5 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Scan Code Quality
              </button>
            </div>
            
            {/* Score & Bugs List Panel */}
            <div className="glass-panel p-6 rounded-3xl h-[520px] flex flex-col lg:col-span-2">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">Scanner Metrics</span>
                {reviewReport && (
                  <button 
                    onClick={() => window.open(`${apiBaseUrl}/api/agents/reviews/${reviewReport.id}/export-pdf`)}
                    className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <Download className="w-3.5 h-3.5" /> Export PDF Report
                  </button>
                )}
              </div>

              {!reviewReport ? (
                <div className="flex-1 flex flex-col items-center justify-center text-slate-500 italic text-xs">
                  Run a scan to compile quality metrics and vulnerabilities list.
                </div>
              ) : (
                <div className="flex-1 flex flex-col overflow-hidden space-y-6">
                  {/* Scores grid */}
                  <div className="grid grid-cols-3 md:grid-cols-6 gap-2">
                    {[
                      { name: 'Overall', val: reviewReport.score },
                      { name: 'Readability', val: reviewReport.readability },
                      { name: 'Maintain', val: reviewReport.maintainability },
                      { name: 'Security', val: reviewReport.security },
                      { name: 'Perform', val: reviewReport.performance },
                      { name: 'Arch', val: reviewReport.architecture },
                    ].map((s, i) => (
                      <div key={i} className="bg-slate-900 border border-slate-800 p-2.5 rounded-xl text-center">
                        <p className="text-[10px] text-slate-500 font-bold uppercase">{s.name}</p>
                        <p className="text-base font-extrabold text-slate-200 mt-0.5">{s.val}%</p>
                      </div>
                    ))}
                  </div>

                  {/* Bugs found list */}
                  <div className="flex-grow overflow-y-auto space-y-2.5 pr-1">
                    <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Security & Logical Defect Log</h4>
                    {bugReports.length === 0 ? (
                      <p className="text-xs text-slate-500 italic">No defects identified in the source files.</p>
                    ) : (
                      bugReports.map((bug) => (
                        <div 
                          key={bug.id} 
                          onClick={() => setSelectedBug(bug)}
                          className="p-3 bg-slate-900 border border-slate-800/80 hover:border-purple-500/50 rounded-2xl flex items-center justify-between cursor-pointer transition-colors"
                        >
                          <div className="flex items-center space-x-3">
                            <div className={`p-2 rounded-xl ${bug.severity === 'HIGH' ? 'bg-rose-500/10 text-rose-400' : 'bg-amber-500/10 text-amber-400'}`}>
                              <ShieldAlert className="w-4 h-4" />
                            </div>
                            <div>
                              <p className="text-xs font-semibold text-slate-200">{bug.title}</p>
                              <p className="text-[10px] text-slate-500 font-mono mt-0.5">{bug.filePath}:L{bug.lineNumber}</p>
                            </div>
                          </div>
                          <ChevronRight className="w-4 h-4 text-slate-600" />
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Expandable Bug Drawer Modal */}
            {selectedBug && (
              <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm z-50 flex items-center justify-end select-none">
                <div className="w-full max-w-lg h-full glass-panel border-l border-slate-800 p-8 flex flex-col justify-between overflow-y-auto">
                  <div className="space-y-6">
                    <div className="flex justify-between items-center border-b border-slate-800 pb-4">
                      <span className="text-[11px] font-extrabold text-slate-400 uppercase tracking-widest flex items-center gap-1.5">
                        <ShieldAlert className="w-4 h-4 text-rose-500" /> Defect Detail Panel
                      </span>
                      <button onClick={() => setSelectedBug(null)} className="text-slate-500 hover:text-slate-300 font-semibold cursor-pointer">×</button>
                    </div>

                    <div>
                      <h4 className="text-base font-bold text-slate-100">{selectedBug.title}</h4>
                      <div className="flex items-center gap-2 mt-2">
                        <span className={`px-2 py-0.5 rounded text-[9px] font-bold ${
                          selectedBug.severity === 'HIGH' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                        }`}>{selectedBug.severity} SEVERITY</span>
                        <span className="text-[10px] font-mono text-slate-500">{selectedBug.filePath}:L{selectedBug.lineNumber}</span>
                      </div>
                    </div>

                    <div>
                      <h5 className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5">Description</h5>
                      <p className="text-xs text-slate-300 leading-relaxed bg-slate-900 border border-slate-850 p-3.5 rounded-xl">{selectedBug.description}</p>
                    </div>

                    <div>
                      <h5 className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5">Refactoring Suggested Fix</h5>
                      <pre className="bg-slate-950 border border-slate-800 p-4 rounded-xl text-[11px] font-mono text-slate-300 overflow-x-auto leading-relaxed">
                        <code>{selectedBug.suggestedFix}</code>
                      </pre>
                    </div>
                  </div>

                  <button 
                    onClick={() => setSelectedBug(null)}
                    className="w-full bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-200 font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer mt-8"
                  >
                    Close Panel Drawer
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 5. SQL Sandbox Tab */}
        {activeTab === 'sql-gen' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Prompt to SQL Sandbox</h3>
                <p className="text-xs text-slate-500 mb-4">Input English queries to compile relational SQL scripts.</p>
                <textarea 
                  value={sqlPrompt}
                  onChange={(e) => setSqlPrompt(e.target.value)}
                  placeholder="Example: Create employee table with foreign key department, adding indexes on department id and primary constraints."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none"
                />
              </div>
              <button 
                onClick={() => triggerAgent('generate-sql', sqlPrompt, setSqlGenOutput)}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Compile SQL Query
              </button>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">SQL DDL Output</span>
                {sqlGenOutput && (
                  <div className="flex gap-3">
                    <button 
                      onClick={() => handleCopy(sqlGenOutput.sqlContent)} 
                      className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} Copy
                    </button>
                    <button 
                      onClick={() => window.open(`${apiBaseUrl}/api/agents/sql/${sqlGenOutput.id}/download`)}
                      className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      <Download className="w-3.5 h-3.5" /> Download
                    </button>
                  </div>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 font-mono whitespace-pre leading-relaxed">
                {sqlGenOutput ? (
                  `-- Query DDL Output\n${sqlGenOutput.sqlContent}\n\n-- Explanation & Optimizations\n${sqlGenOutput.explanation}`
                ) : (
                  'Compiled SQL scripts and optimization logs will display here.'
                )}
              </div>
            </div>
          </div>
        )}

        {/* 6. API Swagger Documentation Tab */}
        {activeTab === 'api-docs' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Submit Backend Code</h3>
                <p className="text-xs text-slate-500 mb-4">Paste REST Spring controllers to map Swagger/OpenAPI models.</p>
                <textarea 
                  value={apiCodeInput}
                  onChange={(e) => setApiCodeInput(e.target.value)}
                  placeholder="Paste Java Spring Controller files here..."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none font-mono"
                />
              </div>
              <button 
                onClick={() => triggerAgent('generate-docs', apiCodeInput, setApiDocOutput)}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Compile OpenAPI Schema
              </button>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">Swagger OpenAPI Spec</span>
                {apiDocOutput && (
                  <div className="flex gap-3">
                    <button 
                      onClick={() => handleCopy(apiDocOutput.content)} 
                      className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} Copy
                    </button>
                    <button 
                      onClick={() => window.open(`${apiBaseUrl}/api/agents/docs/${apiDocOutput.id}/download`)}
                      className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      <Download className="w-3.5 h-3.5" /> Download
                    </button>
                  </div>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 font-mono whitespace-pre leading-relaxed">
                {apiDocOutput ? apiDocOutput.content : 'API specification schemas will display here.'}
              </div>
            </div>
          </div>
        )}

        {/* 7. Unit Test Generator Tab */}
        {activeTab === 'unit-tests' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Submit Source Files</h3>
                <p className="text-xs text-slate-500 mb-4">Paste backend services to create JUnit Mockito test cases.</p>
                <textarea 
                  value={testCodeInput}
                  onChange={(e) => setTestCodeInput(e.target.value)}
                  placeholder="Paste Java ServiceImpl files here..."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none font-mono"
                />
              </div>
              <button 
                onClick={() => triggerAgent('generate-tests', testCodeInput, setUnitTestOutput)}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Compile Test Cases
              </button>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">JUnit & Mockito Suite</span>
                {unitTestOutput && (
                  <button onClick={() => handleCopy(unitTestOutput.testContent)} className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer">
                    {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} Copy Tests
                  </button>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 font-mono whitespace-pre leading-relaxed">
                {unitTestOutput ? unitTestOutput.testContent : 'Generated test suites will display here.'}
              </div>
            </div>
          </div>
        )}

        {/* 8. Deployment Assistant Tab */}
        {activeTab === 'deployment' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-panel p-6 rounded-3xl flex flex-col justify-between h-[500px]">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-2 uppercase tracking-wider text-[11px]">Boilerplate Blueprint</h3>
                <p className="text-xs text-slate-500 mb-4">Input specifications for cloud deployments and container tools.</p>
                <textarea 
                  value={deployCodeInput}
                  onChange={(e) => setDeployCodeInput(e.target.value)}
                  placeholder="Example: Docker compose setting up Java Spring app and PostgreSQL DB. Set up GitHub Actions CI/CD to deploy on AWS."
                  className="w-full bg-slate-900/60 border border-slate-800 focus:border-purple-500 rounded-2xl p-4 text-xs text-slate-300 focus:outline-none h-[320px] resize-none"
                />
              </div>
              <button 
                onClick={() => triggerAgent('deployment-help', deployCodeInput, setDeploymentOutput)}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Compile Deployment Guides
              </button>
            </div>
            
            <div className="glass-panel p-6 rounded-3xl h-[500px] flex flex-col">
              <div className="flex justify-between items-center mb-4 border-b border-slate-800 pb-3">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[11px]">Dockerfile, YAML & CI/CD Guides</span>
                {deploymentOutput && (
                  <button onClick={() => handleCopy(deploymentOutput)} className="text-[10px] text-purple-400 font-bold hover:underline flex items-center gap-1 cursor-pointer">
                    {copiedText ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />} Copy YAMLs
                  </button>
                )}
              </div>
              <div className="flex-1 overflow-y-auto text-xs text-slate-400 font-mono whitespace-pre leading-relaxed">
                {deploymentOutput || 'Deployment configs and pipeline instructions will display here.'}
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
