import React, { useEffect, useRef } from "react";
import QRCode from "qrcode";

const QR_TOKEN =
  "c35235f7-d11c-43cf-853a-47194c97a2de";

export default function TestQR() {
  const canvasRef = useRef(null);

  useEffect(() => {
    QRCode.toCanvas(
      canvasRef.current,
      QR_TOKEN,
      {
        width: 500,
        margin: 5,
        errorCorrectionLevel: "H",
      },
      (error) => {
        if (error) {
          console.error(
            "QR generation failed:",
            error
          );
        } else {
          console.log(
            "SmartLib QR generated:",
            QR_TOKEN
          );
        }
      }
    );
  }, []);

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: "20px",
        padding: "30px",
        background: "#f5f5f5",
      }}
    >
      <h1>SmartLib Test QR</h1>

      <p>
        The Lean Startup — COPY-003
      </p>

      <canvas
        ref={canvasRef}
        style={{
          width: "500px",
          height: "500px",
          maxWidth: "90vw",
          background: "white",
          padding: "10px",
        }}
      />

      <p
        style={{
          maxWidth: "600px",
          textAlign: "center",
          wordBreak: "break-all",
          fontFamily: "monospace",
        }}
      >
        {QR_TOKEN}
      </p>
    </div>
  );
}