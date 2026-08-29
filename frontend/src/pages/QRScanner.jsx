import {
  useEffect,
  useRef,
  useState,
} from "react";

import {
  Html5Qrcode,
} from "html5-qrcode";

import {
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  Loader2,
  RotateCcw,
  ScanLine,
  Undo2,
} from "lucide-react";

import {
  useNavigate,
  useLocation,
} from "react-router-dom";

import transactionService from "../services/transactionService";
import bookCopyService from "../services/bookCopyService";

import "./QRScanner.css";


const READER_ID =
  "smartlib-qr-reader";


export default function QRScanner() {

  const navigate =
    useNavigate();

  const location =
    useLocation();


  const scannerRef =
    useRef(null);

  const startedRef =
    useRef(false);

  const processingRef =
    useRef(false);


  const [scanning, setScanning] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  const [borrowing, setBorrowing] =
    useState(false);

  const [returning, setReturning] =
    useState(false);


  const [copy, setCopy] =
    useState(null);

  const [transaction, setTransaction] =
    useState(null);

  const [scannedToken, setScannedToken] =
    useState("");


  const [error, setError] =
    useState("");

  const [message, setMessage] =
    useState("");


  // ============================================================
  // ROUTE
  // ============================================================

  const adminRoute =
    location.pathname.startsWith(
      "/admin/"
    );


  // ============================================================
  // USER
  // ============================================================

  function getCurrentUser() {

    try {

      const rawUser =
        localStorage.getItem(
          "smartlib_user"
        );

      if (!rawUser) {
        return null;
      }

      return JSON.parse(rawUser);

    } catch (err) {

      console.error(
        "Unable to read user:",
        err
      );

      return null;
    }
  }


  function isAdmin() {

    const user =
      getCurrentUser();

    return (
      user?.role === "ADMIN"
    );
  }


  // ============================================================
  // STOP SCANNER
  // ============================================================

  async function stopScanner() {

    const scanner =
      scannerRef.current;

    if (!scanner) {
      return;
    }

    try {

      if (startedRef.current) {
        await scanner.stop();
      }

    } catch (err) {

      console.warn(
        "Scanner stop warning:",
        err
      );

    }

    try {

      scanner.clear();

    } catch (err) {

      console.warn(
        "Scanner clear warning:",
        err
      );

    }

    scannerRef.current =
      null;

    startedRef.current =
      false;

    setScanning(false);
  }


  // ============================================================
  // START CAMERA
  // ============================================================

  async function startCamera() {

    if (startedRef.current) {
      return;
    }

    setError("");
    setMessage("");

    setCopy(null);
    setTransaction(null);
    setScannedToken("");

    setLoading(false);
    setBorrowing(false);
    setReturning(false);

    processingRef.current =
      false;


    try {

      const reader =
        document.getElementById(
          READER_ID
        );

      if (!reader) {

        setError(
          "Scanner area is not ready. Please try again."
        );

        return;
      }

      reader.innerHTML =
        "";


      const scanner =
        new Html5Qrcode(
          READER_ID
        );

      scannerRef.current =
        scanner;


      console.log(
        "SMARTLIB CAMERA STARTING"
      );


      await scanner.start(

        {
          facingMode:
            "environment",
        },

        {
          fps: 10,

          qrbox: function (
            viewfinderWidth,
            viewfinderHeight
          ) {

            const size =
              Math.min(
                viewfinderWidth * 0.72,
                viewfinderHeight * 0.72,
                420
              );

            return {
              width:
                Math.floor(size),

              height:
                Math.floor(size),
            };

          },

          aspectRatio: 1,
        },

        handleScan,

        () => {
          // Normal scanner failures ignored.
        }

      );


      startedRef.current =
        true;

      setScanning(true);


      console.log(
        "SMARTLIB CAMERA STARTED"
      );

    } catch (err) {

      console.error(
        "CAMERA ERROR:",
        err
      );

      scannerRef.current =
        null;

      startedRef.current =
        false;

      setScanning(false);

      setError(
        "Unable to access the camera. Please allow camera permission and try again."
      );
    }
  }


  // ============================================================
  // QR SCANNED
  // ============================================================

  async function handleScan(
    decodedText
  ) {

    if (processingRef.current) {
      return;
    }

    processingRef.current =
      true;


    console.log(
      "================================="
    );

    console.log(
      "QR SCANNED"
    );

    console.log(
      "RAW QR VALUE:",
      decodedText
    );

    console.log(
      "================================="
    );


    const qrValue =
      decodedText?.trim();


    if (!qrValue) {

      processingRef.current =
        false;

      setError(
        "The QR code could not be read. Please try again."
      );

      return;
    }


    setScannedToken(
      qrValue
    );


    console.log(
      "QR TOKEN SAVED:",
      qrValue
    );


    await stopScanner();


    setLoading(true);

    setError("");
    setMessage("");

    setCopy(null);
    setTransaction(null);


    try {

      console.log(
        "QR VALUE SENT TO API:",
        qrValue
      );


      /*
       * ADMIN:
       *
       * GET /api/admin/book-copies/qr/{token}
       *
       * MEMBER:
       *
       * GET /api/book-copies/qr/{token}
       */

      const response =
        isAdmin()

          ? await bookCopyService.adminGetByQrToken(
              qrValue
            )

          : await bookCopyService.getByQrToken(
              qrValue
            );


      console.log(
        "API STATUS:",
        200
      );

      console.log(
        "API DATA:",
        response
      );


      setCopy(
        response
      );

      setMessage(
        "Book identified successfully."
      );


    } catch (err) {

      console.error(
        "QR LOOKUP FAILED:",
        err
      );


      const status =
        err?.response?.status;

      const backendMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error;


      if (status === 404) {

        setError(
          "This QR code is not registered in SmartLib."
        );

      } else if (
        status === 401 ||
        status === 403
      ) {

        setError(
          "You are not authorized to access this book."
        );

      } else {

        setError(
          backendMessage ||
          "Unable to identify this book copy."
        );

      }

    } finally {

      setLoading(false);

      processingRef.current =
        false;
    }
  }


  // ============================================================
  // MEMBER BORROW
  // ============================================================

  async function handleBorrow() {

    if (!copy) {

      setError(
        "No book copy has been scanned."
      );

      return;
    }


    if (!scannedToken) {

      setError(
        "QR token is missing. Please scan again."
      );

      return;
    }


    if (
      copy.status !==
      "AVAILABLE"
    ) {

      setError(
        "This physical copy is not available."
      );

      return;
    }


    if (isAdmin()) {

      setError(
        "Admin accounts cannot borrow books. Please use a member account."
      );

      return;
    }


    try {

      setBorrowing(true);

      setError("");
      setMessage("");


      console.log(
        "================================="
      );

      console.log(
        "BORROWING PHYSICAL COPY"
      );

      console.log(
        "QR TOKEN:",
        scannedToken
      );

      console.log(
        "COPY:",
        copy.copyNumber
      );

      console.log(
        "BOOK ID:",
        copy.bookId
      );

      console.log(
        "================================="
      );


      const result =
        await transactionService.borrowBookByQr(
          scannedToken
        );


      console.log(
        "QR BORROW SUCCESS:",
        result
      );


      setTransaction(
        result
      );


      setCopy(
        previous => ({
          ...previous,
          status:
            "BORROWED",
        })
      );


      setMessage(
        `Successfully borrowed "${result.bookTitle}".`
      );


    } catch (err) {

      console.error(
        "QR BORROW FAILED:",
        err
      );


      const backendMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error;


      setError(
        backendMessage ||
        err?.message ||
        "Unable to borrow this book."
      );


    } finally {

      setBorrowing(false);
    }
  }


  // ============================================================
  // RETURN
  // ============================================================

  async function handleReturn() {

    if (!copy) {

      setError(
        "No book copy has been scanned."
      );

      return;
    }


    if (!scannedToken) {

      setError(
        "QR token is missing. Please scan again."
      );

      return;
    }


    if (
      copy.status !==
      "BORROWED"
    ) {

      setError(
        "This physical copy is not currently borrowed."
      );

      return;
    }


    try {

      setReturning(true);

      setError("");
      setMessage("");


      const admin =
        isAdmin();


      console.log(
        "================================="
      );

      console.log(
        admin
          ? "ADMIN QR RETURN"
          : "MEMBER QR RETURN"
      );

      console.log(
        "QR TOKEN:",
        scannedToken
      );

      console.log(
        "COPY:",
        copy.copyNumber
      );

      console.log(
        "BOOK ID:",
        copy.bookId
      );

      console.log(
        "USER ROLE:",
        admin
          ? "ADMIN"
          : "MEMBER"
      );

      console.log(
        "================================="
      );


      /*
       * ADMIN:
       *
       * /api/admin/book-copies/qr/{token}/return
       *
       * MEMBER:
       *
       * /api/borrowings/qr/return
       */

      const result =
        admin

          ? await bookCopyService.adminReturnByQr(
              scannedToken
            )

          : await transactionService.returnBookByQr(
              scannedToken
            );


      console.log(
        admin
          ? "ADMIN QR RETURN SUCCESS:"
          : "QR RETURN SUCCESS:",
        result
      );


      setTransaction(
        result
      );


      setCopy(
        previous => ({
          ...previous,
          status:
            "AVAILABLE",
        })
      );


      setMessage(
        admin
          ? `Successfully returned "${copy.bookTitle}" as administrator.`
          : `Successfully returned "${copy.bookTitle}".`
      );


    } catch (err) {

      console.error(
        "QR RETURN FAILED:",
        err
      );


      const backendMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error;


      setError(
        backendMessage ||
        err?.message ||
        "Unable to return this book."
      );


    } finally {

      setReturning(false);
    }
  }


  // ============================================================
  // SCAN ANOTHER
  // ============================================================

  async function scanAnother() {

    await stopScanner();


    setCopy(null);
    setTransaction(null);
    setScannedToken("");

    setError("");
    setMessage("");

    setLoading(false);
    setBorrowing(false);
    setReturning(false);

    processingRef.current =
      false;


    setTimeout(() => {

      startCamera();

    }, 150);
  }


  // ============================================================
  // NAVIGATION HELPERS
  // ============================================================

  function goBack() {

    stopScanner();

    navigate(
      adminRoute
        ? "/admin/dashboard"
        : "/dashboard"
    );
  }


  function goScanRoute() {

    navigate(
      adminRoute
        ? "/admin/qr-scanner"
        : "/qr-scanner"
    );
  }


  // ============================================================
  // CLEANUP
  // ============================================================

  useEffect(() => {

    return () => {

      stopScanner();

    };

  }, []);


  // ============================================================
  // UI
  // ============================================================

  return (

    <div className="smartlib-scanner-page">


      {/* ======================================================
          TOP BAR
      ====================================================== */}

      <header className="scanner-topbar">

        <button
          type="button"
          className="scanner-back-button"
          onClick={goBack}
        >

          <ArrowLeft
            size={18}
          />

          <span>
            Back
          </span>

        </button>


        <div className="scanner-brand">
          SMARTLIB
        </div>

      </header>


      {/* ======================================================
          MAIN
      ====================================================== */}

      <main className="scanner-main">


        <div className="scanner-heading">

          <div className="scanner-eyebrow">
            PHYSICAL COLLECTION
          </div>

          <h1>
            QR Scanner
          </h1>

          <p>
            Scan the QR code attached to a
            physical SmartLib book copy.
          </p>

        </div>


        {/* ====================================================
            CAMERA
        ==================================================== */}

        {!copy &&
          !loading && (

          <section className="scanner-card">

            <div className="scanner-card-header">

              <div className="scanner-icon">

                <ScanLine
                  size={25}
                />

              </div>

              <div>

                <h2>
                  Scan a book
                </h2>

                <p>
                  Start the camera and place the
                  complete QR inside the frame.
                </p>

              </div>

            </div>


            <div
              id={READER_ID}
              className="smartlib-qr-reader"
            />


            {!scanning && (

              <button
                type="button"
                className="start-camera-button"
                onClick={startCamera}
              >

                <ScanLine
                  size={19}
                />

                Start Camera

              </button>

            )}


            {scanning && (

              <div className="scanner-status">

                <span className="scanner-live-dot" />

                Camera active — scan the QR code

              </div>

            )}

          </section>

        )}


        {/* ====================================================
            LOADING
        ==================================================== */}

        {loading && (

          <section className="scanner-result-card">

            <Loader2
              size={30}
              className="scanner-spinner"
            />

            <h2>
              Identifying book...
            </h2>

            <p>
              Checking this physical copy
              in SmartLib.
            </p>

          </section>

        )}


        {/* ====================================================
            ERROR
        ==================================================== */}

        {error && (

          <div className="scanner-error">
            {error}
          </div>

        )}


        {/* ====================================================
            IDENTIFIED BOOK
        ==================================================== */}

        {copy &&
          !loading && (

          <section className="scanner-result-card">


            <div className="identified-badge">

              <CheckCircle2
                size={17}
              />

              Book identified

            </div>


            <div className="result-icon">

              <BookOpen
                size={28}
              />

            </div>


            <h2>
              {copy.bookTitle}
            </h2>


            <div className="result-author">
              {copy.author}
            </div>


            {/* COPY DETAILS */}

            <div className="copy-details">


              <div className="copy-detail">

                <span>
                  COPY
                </span>

                <strong>
                  {copy.copyNumber}
                </strong>

              </div>


              <div className="copy-detail">

                <span>
                  STATUS
                </span>

                <strong
                  className={
                    copy.status ===
                    "AVAILABLE"
                      ? "status-available"
                      : "status-borrowed"
                  }
                >
                  {copy.status}
                </strong>

              </div>


              <div className="copy-detail">

                <span>
                  CONDITION
                </span>

                <strong>
                  {copy.condition ||
                    "GOOD"}
                </strong>

              </div>


              <div className="copy-detail">

                <span>
                  LOCATION
                </span>

                <strong>
                  {copy.location ||
                    "LIBRARY"}
                </strong>

              </div>

            </div>


            {/* =================================================
                AVAILABLE COPY
            ================================================= */}

            {copy.status ===
              "AVAILABLE" && (

              <button
                type="button"
                className="borrow-scanned-button"
                onClick={handleBorrow}
                disabled={
                  borrowing ||
                  isAdmin()
                }
              >

                {borrowing ? (

                  <>

                    <Loader2
                      size={18}
                      className="scanner-spinner"
                    />

                    Borrowing...

                  </>

                ) : (

                  <>

                    <BookOpen
                      size={18}
                    />

                    {isAdmin()
                      ? "Admin Cannot Borrow"
                      : "Borrow This Book"
                    }

                  </>

                )}

              </button>

            )}


            {/* =================================================
                BORROWED COPY
            ================================================= */}

            {copy.status ===
              "BORROWED" && (

              <button
                type="button"
                className="return-scanned-button"
                onClick={handleReturn}
                disabled={
                  returning
                }
              >

                {returning ? (

                  <>

                    <Loader2
                      size={18}
                      className="scanner-spinner"
                    />

                    Returning...

                  </>

                ) : (

                  <>

                    <Undo2
                      size={18}
                    />

                    Return This Book

                  </>

                )}

              </button>

            )}


            {/* =================================================
                SUCCESS
            ================================================= */}

            {transaction && (

              <div className="borrow-success">

                <CheckCircle2
                  size={22}
                />

                <div>

                  <strong>

                    {isAdmin()
                      ? "Book returned successfully!"
                      : copy.status ===
                        "BORROWED"
                        ? "Book borrowed successfully!"
                        : "Book returned successfully!"
                    }

                  </strong>


                  {!isAdmin() &&
                    transaction.dueDate && (

                    <span>

                      Due date:{" "}

                      {transaction.dueDate}

                    </span>

                  )}

                </div>

              </div>

            )}


            {/* =================================================
                ACTIONS
            ================================================= */}

            <div className="scanner-result-actions">


              <button
                type="button"
                className="scan-another-button"
                onClick={scanAnother}
              >

                <RotateCcw
                  size={17}
                />

                Scan Another Book

              </button>


              {transaction &&
                !isAdmin() && (

                <button
                  type="button"
                  className="view-borrowings-button"
                  onClick={() =>
                    navigate(
                      "/borrowings"
                    )
                  }
                >

                  View My Borrowings

                </button>

              )}

            </div>


          </section>

        )}


        {/* ====================================================
            MESSAGE
        ==================================================== */}

        {message &&
          !error && (

          <div className="scanner-message">
            {message}
          </div>

        )}


        {/* ====================================================
            INSTRUCTIONS
        ==================================================== */}

        <section className="scanner-instructions">


          <div>

            <span>
              1
            </span>

            <div>

              <strong>
                Start camera
              </strong>

              <p>
                Allow browser camera access.
              </p>

            </div>

          </div>


          <div>

            <span>
              2
            </span>

            <div>

              <strong>
                Bring QR closer
              </strong>

              <p>
                Keep the complete QR visible.
              </p>

            </div>

          </div>


          <div>

            <span>
              3
            </span>

            <div>

              <strong>
                Hold steady
              </strong>

              <p>
                SmartLib will identify the copy.
              </p>

            </div>

          </div>


        </section>

      </main>

    </div>

  );
}