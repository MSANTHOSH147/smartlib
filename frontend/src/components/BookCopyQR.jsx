import { useEffect, useState } from "react";
import QRCode from "qrcode";
import {
  X,
  Printer,
  Download,
  QrCode,
} from "lucide-react";
import "./BookCopyQR.css";

export default function BookCopyQR({
  copy,
  onClose,
}) {
  const [qrImage, setQrImage] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    const generateQR = async () => {
      try {
        setLoading(true);
        setError("");

        if (!copy?.qrToken) {
          throw new Error(
            "This physical copy does not have a QR token."
          );
        }

        /*
         * IMPORTANT:
         *
         * The QR contains the SmartLib token itself.
         *
         * Example:
         * c35235f7-d11c-43cf-853a-47194c97a2de
         *
         * It does NOT contain a third-party QR URL.
         */
        const dataUrl = await QRCode.toDataURL(
          copy.qrToken,
          {
            width: 700,
            margin: 3,
            errorCorrectionLevel: "H",
          }
        );

        if (active) {
          setQrImage(dataUrl);
        }
      } catch (err) {
        console.error(
          "QR generation failed:",
          err
        );

        if (active) {
          setError(
            "Unable to generate QR code."
          );
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    generateQR();

    return () => {
      active = false;
    };
  }, [copy]);

  const downloadQR = () => {
    if (!qrImage) return;

    const link =
      document.createElement("a");

    link.href = qrImage;

    link.download =
      `SmartLib-${copy.copyNumber}-QR.png`;

    document.body.appendChild(link);

    link.click();

    document.body.removeChild(link);
  };

  const printQR = () => {
    if (!qrImage) return;

    const printWindow =
      window.open(
        "",
        "_blank",
        "width=700,height=800"
      );

    if (!printWindow) {
      alert(
        "Please allow pop-ups to print the QR label."
      );

      return;
    }

    const title =
      copy.bookTitle ||
      copy.book?.title ||
      "SmartLib Book";

    const author =
      copy.author ||
      copy.book?.author ||
      "";

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>
            SmartLib - ${escapeHtml(
              copy.copyNumber
            )}
          </title>

          <style>

            * {
              box-sizing: border-box;
            }

            html,
            body {
              margin: 0;
              padding: 0;
            }

            body {
              font-family:
                Arial,
                Helvetica,
                sans-serif;

              background: white;

              color: #111;

              display: flex;

              justify-content: center;

              align-items: center;

              min-height: 100vh;
            }

            .label {
              width: 3.5in;
              min-height: 4.3in;

              padding: 0.28in;

              border:
                1px solid #dcdad4;

              border-radius: 18px;

              text-align: center;

              display: flex;

              flex-direction: column;

              align-items: center;

              justify-content: center;
            }

            .brand {
              font-size: 13px;

              font-weight: 800;

              letter-spacing: 3px;

              margin-bottom: 12px;
            }

            .eyebrow {
              font-size: 8px;

              font-weight: 700;

              letter-spacing: 2px;

              color: #777;

              margin-bottom: 8px;
            }

            .qr {
              width: 2.45in;

              height: 2.45in;

              object-fit: contain;

              margin: 8px 0 14px;
            }

            .copy {
              font-size: 18px;

              font-weight: 800;

              letter-spacing: 1px;

              margin-bottom: 7px;
            }

            .title {
              max-width: 2.8in;

              font-size: 13px;

              font-weight: 700;

              line-height: 1.3;
            }

            .author {
              margin-top: 4px;

              font-size: 10px;

              color: #777;
            }

            .token {
              margin-top: 12px;

              max-width: 2.9in;

              word-break: break-all;

              font-family: monospace;

              font-size: 7px;

              color: #999;
            }

            @media print {

              @page {
                size: auto;

                margin: 0.2in;
              }

              body {
                min-height: auto;
              }

              .label {
                border: 1px solid #ddd;
              }

            }

          </style>
        </head>

        <body>

          <div class="label">

            <div class="brand">
              SMARTLIB
            </div>

            <div class="eyebrow">
              PHYSICAL COLLECTION
            </div>

            <img
              class="qr"
              src="${qrImage}"
              alt="SmartLib QR Code"
            />

            <div class="copy">
              ${escapeHtml(
                copy.copyNumber
              )}
            </div>

            <div class="title">
              ${escapeHtml(title)}
            </div>

            ${
              author
                ? `
                  <div class="author">
                    ${escapeHtml(author)}
                  </div>
                `
                : ""
            }

            <div class="token">
              ${escapeHtml(
                copy.qrToken
              )}
            </div>

          </div>

          <script>

            window.onload = function() {
              window.print();
            };

            window.onafterprint = function() {
              window.close();
            };

          </script>

        </body>
      </html>
    `);

    printWindow.document.close();
  };

  if (!copy) {
    return null;
  }

  return (
    <div
      className="book-copy-qr-overlay"
      onMouseDown={(event) => {
        if (
          event.target ===
          event.currentTarget
        ) {
          onClose();
        }
      }}
    >

      <section className="book-copy-qr-modal">

        {/* HEADER */}

        <header className="book-copy-qr-header">

          <div>

            <span className="book-copy-qr-eyebrow">
              SMARTLIB / PHYSICAL COPY
            </span>

            <h2>
              QR Label
            </h2>

          </div>

          <button
            type="button"
            className="book-copy-qr-close"
            onClick={onClose}
            aria-label="Close"
          >
            <X size={19} />
          </button>

        </header>


        {/* BOOK INFO */}

        <div className="book-copy-qr-book-info">

          <div className="book-copy-qr-icon">
            <QrCode size={23} />
          </div>

          <div>

            <strong>
              {copy.bookTitle ||
                copy.book?.title ||
                "SmartLib Book"}
            </strong>

            <span>
              {copy.author ||
                copy.book?.author ||
                "Physical collection"}
            </span>

          </div>

        </div>


        {/* QR */}

        <div className="book-copy-qr-preview">

          {loading && (
            <div className="book-copy-qr-loading">
              Generating QR...
            </div>
          )}

          {error && (
            <div className="book-copy-qr-error">
              {error}
            </div>
          )}

          {qrImage && !error && (
            <img
              src={qrImage}
              alt={`QR for ${copy.copyNumber}`}
            />
          )}

        </div>


        {/* COPY */}

        <div className="book-copy-qr-copy">

          <span>
            COPY NUMBER
          </span>

          <strong>
            {copy.copyNumber}
          </strong>

        </div>


        {/* TOKEN */}

        <div className="book-copy-qr-token">

          <span>
            SMARTLIB QR TOKEN
          </span>

          <code>
            {copy.qrToken}
          </code>

        </div>


        {/* ACTIONS */}

        <div className="book-copy-qr-actions">

          <button
            type="button"
            className="qr-secondary-button"
            onClick={downloadQR}
            disabled={
              !qrImage
            }
          >
            <Download size={17} />
            Download PNG
          </button>

          <button
            type="button"
            className="qr-primary-button"
            onClick={printQR}
            disabled={
              !qrImage
            }
          >
            <Printer size={17} />
            Print Label
          </button>

        </div>

      </section>

    </div>
  );
}


/* =========================================================
   HTML ESCAPE
   ========================================================= */

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll(
      "'",
      "&#039;"
    );
}