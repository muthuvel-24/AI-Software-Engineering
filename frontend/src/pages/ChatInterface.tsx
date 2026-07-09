import { useState, useEffect, useRef, type FormEvent, type ChangeEvent, type DragEvent } from 'react';
import { 
  Send, 
  Mic, 
  MicOff, 
  Volume2, 
  VolumeX, 
  FileText, 
  Sparkles,
  Search,
  Copy,
  Check,
  Paperclip
} from 'lucide-react';

interface ChatInterfaceProps {
  projectId: number | null;
  token: string;
  apiBaseUrl: string;
  provider: string;
  setProvider: (prov: string) => void;
  model: string;
  setModel: (mod: string) => void;
  apiKey: string;
}

export default function ChatInterface({
  projectId,
  token,
  apiBaseUrl,
  provider,
  setProvider,
  model,
  setModel,
  apiKey
}: ChatInterfaceProps) {
  const [chats, setChats] = useState<any[]>([]);
  const [currentChatId, setCurrentChatId] = useState<number | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [inputText, setInputText] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [copiedId, setCopiedId] = useState<number | null>(null);

  // Voice States
  const [isListening, setIsListening] = useState(false);
  const [speakingMsgId, setSpeakingMsgId] = useState<number | null>(null);
  const recognitionRef = useRef<any>(null);

  // File Upload State
  const [attachedFiles, setAttachedFiles] = useState<any[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (projectId) {
      fetchChats();
    }
  }, [projectId]);

  useEffect(() => {
    if (currentChatId) {
      fetchMessages(currentChatId);
    } else {
      setMessages([]);
    }
  }, [currentChatId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const fetchChats = async () => {
    try {
      const response = await fetch(`${apiBaseUrl}/api/projects/${projectId}/chats`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setChats(data);
        if (data.length > 0) {
          if (!currentChatId) {
            setCurrentChatId(data[0].id);
          }
        } else {
          // Auto create default chat if none exists
          const createResponse = await fetch(`${apiBaseUrl}/api/projects/${projectId}/chats`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({ title: 'Chat Session 1' })
          });
          if (createResponse.ok) {
            const newChat = await createResponse.json();
            setChats([newChat]);
            setCurrentChatId(newChat.id);
          }
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchMessages = async (chatId: number) => {
    try {
      const response = await fetch(`${apiBaseUrl}/api/chats/${chatId}/messages`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setMessages(data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCreateChat = async () => {
    if (!projectId) return;
    try {
      const response = await fetch(`${apiBaseUrl}/api/projects/${projectId}/chats`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ title: `Chat Session ${chats.length + 1}` })
      });
      if (response.ok) {
        const newChat = await response.json();
        setChats([newChat, ...chats]);
        setCurrentChatId(newChat.id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleSendMessage = async (e?: FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim() && attachedFiles.length === 0) return;

    let chatId = currentChatId;
    const textToSend = inputText;
    setInputText('');
    setLoading(true);

    // Save temporary local message to show immediately
    const tempUserMsg = { id: Date.now(), role: 'user', content: textToSend, createdAt: new Date().toISOString() };
    setMessages(prev => [...prev, tempUserMsg]);

    try {
      if (!chatId) {
        // Auto create default chat if none exists
        const createResponse = await fetch(`${apiBaseUrl}/api/projects/${projectId}/chats`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ title: 'Chat Session 1' })
        });
        if (createResponse.ok) {
          const newChat = await createResponse.json();
          chatId = newChat.id;
          setChats([newChat]);
          setCurrentChatId(newChat.id);
        } else {
          throw new Error("Failed to auto-create chat session");
        }
      }

      const response = await fetch(`${apiBaseUrl}/api/chats/${chatId}/messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          content: textToSend,
          provider,
          model,
          apiKey
        })
      });

      if (response.ok) {
        const assistantMsg = await response.json();
        setMessages(prev => {
          // Remove local temporary and add real database replies
          return prev.filter(m => m.id !== tempUserMsg.id).concat(assistantMsg);
        });
        // Clear files attached
        setAttachedFiles([]);
      } else {
        // Rollback temporary message if the request failed
        setMessages(prev => prev.filter(m => m.id !== tempUserMsg.id));
        setInputText(textToSend);
      }
    } catch (e) {
      console.error(e);
      // Rollback temporary message on network failure
      setMessages(prev => prev.filter(m => m.id !== tempUserMsg.id));
      setInputText(textToSend);
    } finally {
      setLoading(false);
    }
  };

  // --- Voice Utilities (Speech Recognition & Synthesis) ---

  const toggleSpeechInput = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert("Speech recognition is not supported in this browser. Please try Chrome/Safari.");
      return;
    }

    if (isListening) {
      recognitionRef.current?.stop();
      setIsListening(false);
      return;
    }

    const rec = new SpeechRecognition();
    rec.continuous = false;
    rec.interimResults = false;
    rec.lang = 'en-US';

    rec.onstart = () => setIsListening(true);
    rec.onend = () => setIsListening(false);
    rec.onerror = (e: any) => {
      console.error(e);
      setIsListening(false);
    };
    rec.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      setInputText(prev => prev + (prev ? ' ' : '') + transcript);
    };

    recognitionRef.current = rec;
    rec.start();
  };

  const handleSpeakText = (msgId: number, text: String) => {
    if (speakingMsgId === msgId) {
      window.speechSynthesis.cancel();
      setSpeakingMsgId(null);
      return;
    }

    window.speechSynthesis.cancel(); // Stop current speech
    
    // Remove markdown symbols from speech
    const cleanText = text.replace(/[*#`_-]/g, '');
    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.onend = () => setSpeakingMsgId(null);
    utterance.onerror = () => setSpeakingMsgId(null);

    setSpeakingMsgId(msgId);
    window.speechSynthesis.speak(utterance);
  };

  // --- Drag and Drop File RAG Ingestion ---

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      processFiles(e.target.files);
    }
  };

  const processFiles = async (fileList: FileList) => {
    if (!projectId) return;

    for (let i = 0; i < fileList.length; i++) {
      const file = fileList[i];
      const reader = new FileReader();

      reader.onload = async (e: any) => {
        const textContent = e.target.result;
        
        // Register attachment visual chip
        setAttachedFiles(prev => [...prev, { name: file.name, size: file.size }]);

        // Ingest into Vector DB Context
        try {
          await fetch(`${apiBaseUrl}/api/projects/${projectId}/upload-document`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              fileName: file.name,
              content: textContent
            })
          });
        } catch (err) {
          console.error("Document ingestion error", err);
        }
      };

      reader.readAsText(file);
    }
  };

  const handleDragOver = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files) {
      processFiles(e.dataTransfer.files);
    }
  };

  // --- Render Markdown Parsing ---
  
  const renderMessageContent = (msg: any) => {
    const text = msg.content;
    const isCodeBlock = text.startsWith("```");

    if (isCodeBlock) {
      // Find code programming language tag
      const match = text.match(/```(\w*)\n([\s\S]*?)```/);
      const lang = match ? match[1] : 'code';
      const code = match ? match[2] : text.replace(/```/g, '');

      return (
        <div className="my-3 rounded-xl overflow-hidden bg-slate-950 border border-slate-800 font-mono text-xs">
          <div className="bg-slate-900 px-4 py-2 border-b border-slate-800 flex justify-between items-center text-[10px] uppercase font-bold text-slate-500">
            <span>{lang || 'code'}</span>
            <button 
              onClick={() => {
                navigator.clipboard.writeText(code);
                setCopiedId(msg.id);
                setTimeout(() => setCopiedId(null), 2000);
              }}
              className="flex items-center gap-1 hover:text-purple-400 cursor-pointer"
            >
              {copiedId === msg.id ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedId === msg.id ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="p-4 overflow-x-auto text-slate-300 leading-relaxed"><code>{code}</code></pre>
        </div>
      );
    }

    // Basic markdown format compiler: lists, headers, code snippets
    const lines = text.split("\n");
    return (
      <div className="space-y-2 leading-relaxed">
        {lines.map((line: string, idx: number) => {
          let content = line;
          
          if (content.startsWith("### ")) {
            return <h4 key={idx} className="text-sm font-bold text-slate-200 mt-3">{content.substring(4)}</h4>;
          }
          if (content.startsWith("## ")) {
            return <h3 key={idx} className="text-base font-bold text-slate-100 mt-4">{content.substring(3)}</h3>;
          }
          if (content.startsWith("# ")) {
            return <h2 key={idx} className="text-lg font-bold text-white mt-5">{content.substring(2)}</h2>;
          }
          if (content.startsWith("- ") || content.startsWith("* ")) {
            return (
              <ul key={idx} className="list-disc pl-5 text-slate-300 text-xs">
                <li>{content.substring(2)}</li>
              </ul>
            );
          }

          // Inline formatting tags bold `**` and inline backticks
          return (
            <p key={idx} className="text-xs text-slate-300">
              {content.split(" ").map((word, wordIdx) => {
                if (word.startsWith("**") && word.endsWith("**")) {
                  return <strong key={wordIdx} className="text-slate-100 font-bold">{word.replace(/\*\*/g, '')} </strong>;
                }
                if (word.startsWith("`") && word.endsWith("`")) {
                  return <code key={wordIdx} className="bg-slate-950 px-1 py-0.5 rounded border border-slate-800 text-[11px] font-mono text-purple-400">{word.replace(/`/g, '')} </code>;
                }
                return word + " ";
              })}
            </p>
          );
        })}
      </div>
    );
  };

  const filteredChats = chats.filter(c => c.title.toLowerCase().includes(searchQuery.toLowerCase()));

  return (
    <div className="flex-1 flex overflow-hidden select-none">
      
      {/* Chats Sessions Sidebar */}
      <div className="w-64 border-r border-slate-800 bg-slate-950/40 flex flex-col shrink-0">
        <div className="p-4 border-b border-slate-800/80 space-y-3">
          <div className="relative">
            <Search className="absolute left-3 top-3 w-4 h-4 text-slate-500" />
            <input 
              type="text" 
              placeholder="Search chat history..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-slate-300 focus:outline-none focus:border-purple-500"
            />
          </div>
          <button 
            onClick={handleCreateChat}
            className="w-full bg-purple-600/10 hover:bg-purple-600/20 border border-purple-500/30 text-purple-300 font-semibold py-2 rounded-xl text-xs transition-colors cursor-pointer"
          >
            + Start Conversation
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {filteredChats.map((c) => (
            <button
              key={c.id}
              onClick={() => setCurrentChatId(c.id)}
              className={`w-full text-left px-3.5 py-2.5 rounded-xl text-xs font-medium transition-all truncate cursor-pointer ${
                currentChatId === c.id 
                  ? 'bg-slate-800/80 text-purple-400 border border-slate-700' 
                  : 'text-slate-400 hover:bg-slate-900/40 hover:text-slate-300'
              }`}
            >
              {c.title}
            </button>
          ))}
        </div>
      </div>

      {/* Main Chat Interface */}
      <div className="flex-1 flex flex-col bg-slate-950/20 overflow-hidden relative">
        
        {/* Model and API Key Bar */}
        <div className="px-6 py-3 border-b border-slate-800 flex flex-wrap items-center justify-between gap-4 bg-slate-950/30">
          <div className="flex items-center space-x-3 text-xs">
            <Sparkles className="w-4 h-4 text-purple-400" />
            <span className="font-semibold text-slate-300">Model Selector</span>
            <select 
              value={provider} 
              onChange={(e) => {
                setProvider(e.target.value);
                if (e.target.value === 'gemini') setModel('gemini-1.5-flash');
                else if (e.target.value === 'openai') setModel('gpt-4o-mini');
                else setModel('openai/gpt-4o-mini'); // openrouter
              }}
              className="bg-slate-900 border border-slate-800 rounded-lg px-2 py-1 text-slate-300 focus:outline-none"
            >
              <option value="openrouter">OpenRouter (Free)</option>
              <option value="gemini">Gemini API</option>
              <option value="openai">OpenAI API</option>
            </select>

            <select 
              value={model} 
              onChange={(e) => setModel(e.target.value)}
              className="bg-slate-900 border border-slate-800 rounded-lg px-2 py-1 text-slate-300 focus:outline-none"
            >
              {provider === 'gemini' ? (
                <>
                  <option value="gemini-1.5-flash">Gemini 1.5 Flash (Fast)</option>
                  <option value="gemini-1.5-pro">Gemini 1.5 Pro (Analytical)</option>
                  <option value="gemini-2.5-flash">Gemini 2.5 Flash (Latest)</option>
                </>
              ) : provider === 'openrouter' ? (
                <>
                  <option value="openai/gpt-4o-mini">GPT-4o Mini (Fast &amp; Free)</option>
                  <option value="openai/gpt-4o">GPT-4o (Smart)</option>
                  <option value="google/gemini-2.5-flash">Gemini 2.5 Flash</option>
                  <option value="anthropic/claude-3-haiku">Claude 3 Haiku</option>
                  <option value="meta-llama/llama-3.1-8b-instruct:free">Llama 3.1 8B (Free)</option>
                </>
              ) : (
                <>
                  <option value="gpt-4o-mini">GPT-4o Mini (Speed)</option>
                  <option value="gpt-4o">GPT-4o (Smart)</option>
                </>
              )}
            </select>
          </div>
          
          <span className="text-[10px] text-purple-400 uppercase tracking-widest font-bold">
            {provider === 'openrouter' ? '⚡ OPENROUTER ACTIVE' : apiKey ? 'USER KEY ACTIVE' : 'SYSTEM KEY ACTIVE'}
          </span>
        </div>

        {/* Message Log Window */}
        <div 
          className={`flex-1 overflow-y-auto p-6 space-y-6 ${isDragging ? 'bg-purple-950/10 border-2 border-dashed border-purple-500/50' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center p-8">
              <Sparkles className="w-12 h-12 text-purple-500/60 mb-4 animate-pulse" />
              <h4 className="text-lg font-bold text-slate-300">Start Your Engineering Audit</h4>
              <p className="text-xs text-slate-500 max-w-sm mt-1">
                Consult with our AI agents on coding schemas, unit tests, repository files, or deployment pipelines. Drag and drop documentation here to initialize RAG context.
              </p>
            </div>
          ) : (
            messages.map((msg) => {
              const isAssistant = msg.role === 'assistant';
              return (
                <div key={msg.id} className={`flex ${isAssistant ? 'justify-start' : 'justify-end'}`}>
                  <div className={`max-w-2xl p-4 rounded-2xl relative ${
                    isAssistant 
                      ? 'bg-slate-900 border border-slate-800/80 rounded-tl-none' 
                      : 'bg-purple-600 text-white rounded-tr-none'
                  }`}>
                    {/* Role Tag & TTS toggler */}
                    <div className="flex justify-between items-center mb-2.5 text-[10px] uppercase tracking-wider font-bold text-slate-400">
                      <span>{msg.role}</span>
                      {isAssistant && (
                        <button 
                          onClick={() => handleSpeakText(msg.id, msg.content)}
                          className="hover:text-purple-400 cursor-pointer"
                        >
                          {speakingMsgId === msg.id ? <VolumeX className="w-3.5 h-3.5 text-rose-400" /> : <Volume2 className="w-3.5 h-3.5" />}
                        </button>
                      )}
                    </div>
                    {/* Content */}
                    <div className={isAssistant ? 'text-slate-300' : 'text-white'}>
                      {renderMessageContent(msg)}
                    </div>
                  </div>
                </div>
              );
            })
          )}
          {loading && (
            <div className="flex justify-start">
              <div className="bg-slate-900 border border-slate-800 p-4 rounded-2xl rounded-tl-none flex items-center space-x-2">
                <span className="text-xs text-slate-400 cursor-pulse font-medium">Assistant thinking</span>
                <span className="flex space-x-1">
                  <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-bounce delay-100"></span>
                  <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-bounce delay-200"></span>
                  <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-bounce delay-300"></span>
                </span>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar Form */}
        <div className="p-4 border-t border-slate-800/80 bg-slate-950/30">
          
          {/* File attachment chips */}
          {attachedFiles.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-3">
              {attachedFiles.map((file, idx) => (
                <div key={idx} className="flex items-center gap-1.5 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-xl text-[10px] font-bold text-slate-300">
                  <FileText className="w-3.5 h-3.5 text-purple-400" />
                  <span>{file.name} ({(file.size / 1024).toFixed(1)} KB)</span>
                  <button 
                    onClick={() => setAttachedFiles(prev => prev.filter((_, i) => i !== idx))} 
                    className="text-slate-500 hover:text-red-400 ml-1 cursor-pointer"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}

          <form onSubmit={handleSendMessage} className="flex items-center gap-2">
            <button 
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="p-3 bg-slate-900 hover:bg-slate-850 border border-slate-800 text-slate-400 hover:text-slate-200 rounded-xl cursor-pointer transition-colors"
              title="Attach File (RAG)"
            >
              <Paperclip className="w-4 h-4" />
            </button>
            <input 
              type="file" 
              ref={fileInputRef}
              onChange={handleFileChange}
              className="hidden" 
              multiple 
            />

            <input 
              type="text" 
              placeholder={projectId ? "Ask Antigravity to write code or review schema..." : "Please initialize/select a project first"}
              disabled={!projectId || loading}
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-3.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500"
            />
            
            <button 
              type="button"
              onClick={toggleSpeechInput}
              disabled={!projectId}
              className={`p-3 border rounded-xl transition-colors cursor-pointer ${
                isListening 
                  ? 'bg-rose-500/20 border-rose-500 text-rose-400 animate-pulse' 
                  : 'bg-slate-900 hover:bg-slate-850 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
              title="Voice Input"
            >
              {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
            </button>

            <button 
              type="submit" 
              disabled={!projectId || loading || (!inputText.trim() && attachedFiles.length === 0)}
              className="p-3 bg-purple-600 hover:bg-purple-700 disabled:bg-slate-900 text-white disabled:text-slate-600 rounded-xl transition-all cursor-pointer shadow-lg shadow-purple-500/10"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
