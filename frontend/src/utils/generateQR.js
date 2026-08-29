import QRCode from "qrcode";

export async function generateSmartLibQR(qrToken) {
  if (!qrToken) {
    throw new Error("QR token is required");
  }

  const canvas = document.createElement("canvas");

  await QRCode.toCanvas(
    canvas,
    qrToken,
    {
      width: 500,
      margin: 4,
      errorCorrectionLevel: "H",
    }
  );

  return canvas;
}

export async function downloadSmartLibQR(
  qrToken,
  fileName = "smartlib-qr.png"
) {
  if (!qrToken) {
    throw new Error("QR token is required");
  }

  const dataUrl = await QRCode.toDataURL(
    qrToken,
    {
      width: 1000,
      margin: 5,
      errorCorrectionLevel: "H",
    }
  );

  const link = document.createElement("a");

  link.href = dataUrl;
  link.download = fileName;

  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}