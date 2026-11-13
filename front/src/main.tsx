import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";
import { iniciarSessao } from "./plataforma/sessao";
import { iniciarRotas } from "./plataforma/rotas";

// Antes de qualquer render: registra o ouvinte de mensagens da casca e só então envia
// modulo:pronto (Contrato §12.2). A sessão fica guardada fora dos componentes.
iniciarSessao();
iniciarRotas();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
