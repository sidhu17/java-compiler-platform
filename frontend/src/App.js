import React, { useState } from "react";
import "./App.css";
import CodeMirror from "@uiw/react-codemirror";
import { java } from "@codemirror/lang-java";

const API_URL = "https://java-compiler-platform-production.up.railway.app/api/run";

function App() {
  const [files, setFiles] = useState({
    "Main.java": `import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    String name = sc.nextLine();
    System.out.println("Hello, " + name + "!");
  }
}`
  });

  const [activeFile, setActiveFile] = useState("Main.java");
  const [userInput, setUserInput] = useState("World");
  const [output, setOutput] = useState({ text: "", isError: false });
  const [loading, setLoading] = useState(false);

  const runCode = async () => {
    setLoading(true);
    setOutput({ text: "", isError: false });

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          source_code: files[activeFile],
          input: userInput
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API error ${response.status}: ${errorText}`);
      }

      const resultText = await response.text();
      const isError = /error|exception/i.test(resultText);
      setOutput({ text: resultText, isError });
    } catch (err) {
      console.error("Run Code Error:", err);
      setOutput({ text: `Error: ${err.message}`, isError: true });
    } finally {
      setLoading(false);
    }
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
              cursor: "pointer"
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
          onChange={(value) => setFiles({ ...files, [activeFile]: value })}
        />
      </div>

      <textarea
        placeholder="Standard input (System.in)..."
        value={userInput}
        onChange={(e) => setUserInput(e.target.value)}
        rows={4}
        style={{
          width: "90%",
          marginBottom: "1rem",
          padding: "0.5rem",
          borderRadius: "6px"
        }}
      />

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
          overflowX: "auto"
        }}
      >
        {output.text}
      </pre>
    </div>
  );
}

export default App;