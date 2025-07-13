import React, { useState } from "react";
import "./App.css";
import CodeMirror from "@uiw/react-codemirror";
import { java } from "@codemirror/lang-java";

function App() {
  const [files, setFiles] = useState({
    "Main.java": `public class Main {
  public static void main(String[] args) {
    System.out.println(Helper.greet());
  }
}`,
    "Helper.java": `public class Helper {
  public static String greet() {
    return "Hello from Helper!";
  }
}`,
  });

  const [activeFile, setActiveFile] = useState("Main.java");
  const [output, setOutput] = useState({ text: "", isError: false });
  const [loading, setLoading] = useState(false);

  const formatCodePayload = () => {
    return Object.entries(files)
      .map(([filename, content]) => `// File: ${filename}\n${content}`)
      .join("\n\n");
  };

  const runCode = async () => {
    setLoading(true);
    try {
      const res = await fetch("https://java-compiler-platform-production.up.railway.app/api/run", {
        method: "POST",
        headers: {
          "Content-Type": "text/plain",
        },
        body: formatCodePayload(),
      });

      const result = await res.text();
      const isError =
        result.toLowerCase().includes("error") ||
        result.toLowerCase().includes("exception");
      setOutput({ text: result, isError });
    } catch (err) {
      setOutput({ text: "Error: " + err.message, isError: true });
    } finally {
      setLoading(false);
    }
  };

  const handleCodeChange = (value) => {
    setFiles({ ...files, [activeFile]: value });
  };

  return (
    <div className="App">
      <h1>Java Compiler</h1>

      <div style={{ marginBottom: "1rem" }}>
        {Object.keys(files).map((filename) => (
          <button
            key={filename}
            onClick={() => setActiveFile(filename)}
            style={{
              marginRight: "10px",
              padding: "8px 16px",
              backgroundColor: activeFile === filename ? "#333" : "#666",
              color: "#fff",
              border: "none",
              borderRadius: "5px",
              cursor: "pointer",
            }}
          >
            {filename}
          </button>
        ))}
      </div>

      <div style={{ width: "90%", margin: "0 auto", marginBottom: "1rem" }}>
        <CodeMirror
          value={files[activeFile]}
          height="300px"
          extensions={[java()]}
          theme="dark"
          onChange={(value) => handleCodeChange(value)}
        />
      </div>

      <button onClick={runCode} disabled={loading}>
        {loading ? "Running..." : "Run Code"}
      </button>

      <pre
        style={{
          textAlign: "left",
          marginTop: "1rem",
          padding: "1rem",
          backgroundColor: output.isError ? "#ffdddd" : "#1e1e1e",
          color: output.isError ? "#a00" : "#0f0",
          width: "90%",
          maxWidth: "800px",
          margin: "1rem auto",
          borderRadius: "6px",
          overflowX: "auto",
        }}
      >
        {output.text}
      </pre>
    </div>
  );
}

export default App;