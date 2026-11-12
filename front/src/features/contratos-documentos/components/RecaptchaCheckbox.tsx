import { useEffect, useRef, useState } from "react";

type CaptchaStatus =
  | "loading"
  | "pending"
  | "validated"
  | "expired"
  | "error"
  | "unconfigured";

interface GoogleRecaptcha {
  ready(callback: () => void): void;
  render(
    container: HTMLElement,
    options: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
      "error-callback": () => void;
    },
  ): number;
}

declare global {
  interface Window {
    grecaptcha?: GoogleRecaptcha;
  }
}

const SCRIPT_ID = "google-recaptcha-api";
let scriptPromise: Promise<void> | null = null;

function loadGoogleRecaptcha(): Promise<void> {
  if (window.grecaptcha) return Promise.resolve();
  if (scriptPromise) return scriptPromise;

  scriptPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = "https://www.google.com/recaptcha/api.js?render=explicit";
    script.async = true;
    script.defer = true;
    script.onload = () => {
      if (window.grecaptcha) {
        resolve();
      } else {
        script.remove();
        scriptPromise = null;
        reject(new Error("O reCAPTCHA não foi inicializado."));
      }
    };
    script.onerror = () => {
      script.remove();
      scriptPromise = null;
      reject(new Error("Não foi possível carregar o reCAPTCHA."));
    };
    document.head.appendChild(script);
  });

  return scriptPromise;
}

interface RecaptchaCheckboxProps {
  onTokenChange: (token: string | null) => void;
}

export function RecaptchaCheckbox({ onTokenChange }: RecaptchaCheckboxProps) {
  const [status, setStatus] = useState<CaptchaStatus>("loading");
  const [attempt, setAttempt] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const siteKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY?.trim() ?? "";

  useEffect(() => {
    if (!siteKey) {
      setStatus("unconfigured");
      onTokenChange(null);
      return;
    }

    let active = true;
    setStatus("loading");
    onTokenChange(null);

    loadGoogleRecaptcha()
      .then(() => {
        window.grecaptcha?.ready(() => {
          if (!active || !containerRef.current || !window.grecaptcha) return;

          try {
            window.grecaptcha.render(containerRef.current, {
              sitekey: siteKey,
              callback: (token) => {
                if (!active) return;
                setStatus("validated");
                onTokenChange(token);
              },
              "expired-callback": () => {
                if (!active) return;
                setStatus("expired");
                onTokenChange(null);
              },
              "error-callback": () => {
                if (!active) return;
                setStatus("error");
                onTokenChange(null);
              },
            });
            setStatus("pending");
          } catch {
            setStatus("error");
            onTokenChange(null);
          }
        });
      })
      .catch(() => {
        if (!active) return;
        setStatus("error");
        onTokenChange(null);
      });

    return () => {
      active = false;
    };
  }, [attempt, onTokenChange, siteKey]);

  const statusText: Record<CaptchaStatus, string> = {
    loading: "Carregando reCAPTCHA...",
    pending: "Conclua a verificação antes de criar o contrato.",
    validated: "CAPTCHA concluído.",
    expired: "A verificação expirou. Resolva o CAPTCHA novamente.",
    error: "Não foi possível carregar ou validar o CAPTCHA.",
    unconfigured: "Configure VITE_RECAPTCHA_SITE_KEY para habilitar o CAPTCHA.",
  };

  return (
    <div className="space-y-1">
      {siteKey && <div ref={containerRef} />}
      <p className="text-xs text-gray-500" role="status" aria-live="polite">
        {statusText[status]}
      </p>
      {status === "error" && siteKey && (
        <button
          type="button"
          className="text-xs text-brand-700 underline"
          onClick={() => setAttempt((current) => current + 1)}
        >
          Tentar carregar novamente
        </button>
      )}
    </div>
  );
}
