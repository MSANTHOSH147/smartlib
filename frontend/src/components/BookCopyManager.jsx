import { useEffect, useState } from "react";
import {
  X,
  QrCode,
  RefreshCw,
  MapPin,
  Pencil,
  Save,
} from "lucide-react";

import bookCopyService from "../services/bookCopyService";
import BookCopyQR from "./BookCopyQR";

import "./BookCopyManager.css";


function BookCopyManager({ book, onClose }) {

  const [copies, setCopies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [selectedQR, setSelectedQR] = useState(null);

  const [deactivatingId, setDeactivatingId] =
    useState(null);

  const [editingCopy, setEditingCopy] =
    useState(null);

  const [editCondition, setEditCondition] =
    useState("");

  const [editLocation, setEditLocation] =
    useState("");

  const [savingEdit, setSavingEdit] =
    useState(false);


  /* ============================================================
     LOAD PHYSICAL COPIES
     ============================================================ */

  useEffect(() => {

    if (!book?.id) {
      return;
    }

    loadCopies();

  }, [book?.id]);


  async function loadCopies() {

    try {

      setLoading(true);
      setError("");

      const data =
        await bookCopyService.getByBookId(
          book.id
        );

      setCopies(
        Array.isArray(data)
          ? data
          : []
      );

    } catch (err) {

      console.error(
        "Book copies loading error:",
        err
      );

      setError(
        err?.response?.data?.message ||
        "Unable to load book copies."
      );

    } finally {

      setLoading(false);

    }
  }


  /* ============================================================
     STATUS CLASS
     ============================================================ */

  function getStatusClass(status) {

    switch (status) {

      case "AVAILABLE":
        return "copy-available";

      case "BORROWED":
        return "copy-borrowed";

      case "RESERVED":
        return "copy-reserved";

      case "DAMAGED":
        return "copy-damaged";

      case "LOST":
        return "copy-lost";

      case "MAINTENANCE":
        return "copy-maintenance";

      case "DEACTIVATED":
        return "copy-deactivated";

      default:
        return "copy-default";
    }
  }


  /* ============================================================
     DEACTIVATE COPY
     ============================================================ */

  async function deactivateCopy(copy) {

    if (!copy?.id) {
      return;
    }


    if (copy.status === "BORROWED") {

      alert(
        "This physical copy is currently borrowed and cannot be deactivated."
      );

      return;
    }


    if (copy.status === "DEACTIVATED") {

      alert(
        "This physical copy is already deactivated."
      );

      return;
    }


    const confirmed =
      window.confirm(
        `Deactivate ${copy.copyNumber}?\n\n` +
        "This copy will no longer be available for borrowing."
      );


    if (!confirmed) {
      return;
    }


    try {

      setDeactivatingId(copy.id);

      await bookCopyService.deactivateCopy(
        copy.id
      );

      await loadCopies();

    } catch (err) {

      console.error(
        "Book copy deactivation error:",
        err
      );

      alert(
        err?.response?.data?.message ||
        "Unable to deactivate this book copy."
      );

    } finally {

      setDeactivatingId(null);

    }
  }


  /* ============================================================
     OPEN EDIT MODAL
     ============================================================ */

  function openEdit(copy) {

    if (!copy?.id) {
      return;
    }

    setEditingCopy(copy);

    setEditCondition(
      copy.condition || "GOOD"
    );

    setEditLocation(
      copy.location || ""
    );
  }


  /* ============================================================
     CLOSE EDIT MODAL
     ============================================================ */

  function closeEdit() {

    if (savingEdit) {
      return;
    }

    setEditingCopy(null);
    setEditCondition("");
    setEditLocation("");
  }


  /* ============================================================
     SAVE EDIT
     ============================================================ */

  async function saveEdit() {

    if (!editingCopy?.id) {
      return;
    }

    if (!editCondition) {

      alert(
        "Please select a condition."
      );

      return;
    }


    try {

      setSavingEdit(true);

      await bookCopyService.updateCopy(
        editingCopy.id,
        {
          condition: editCondition,
          location: editLocation.trim(),
        }
      );

      await loadCopies();

      setEditingCopy(null);
      setEditCondition("");
      setEditLocation("");

    } catch (err) {

      console.error(
        "Book copy update error:",
        err
      );

      alert(
        err?.response?.data?.message ||
        "Unable to update physical copy."
      );

    } finally {

      setSavingEdit(false);

    }
  }


  /* ============================================================
     OPEN QR LABEL
     ============================================================ */

  function openQR(copy) {

    if (!copy?.qrToken) {

      alert(
        "This physical copy does not have a QR token."
      );

      return;
    }

    setSelectedQR(copy);
  }


  /* ============================================================
     CLOSE QR LABEL
     ============================================================ */

  function closeQR() {

    setSelectedQR(null);
  }


  /* ============================================================
     MAIN UI
     ============================================================ */

  return (

    <div
      className="copy-manager-overlay"

      onMouseDown={(event) => {

        if (
          event.target ===
          event.currentTarget
        ) {

          onClose();

        }

      }}
    >

      <div className="copy-manager">


        {/* ======================================================
            HEADER
            ====================================================== */}

        <header className="copy-manager-header">

          <div>

            <span className="copy-manager-eyebrow">
              PHYSICAL INVENTORY
            </span>

            <h2>
              {book?.title || "Book"}
            </h2>

            <p>

              {loading

                ? "Loading physical copies..."

                : `${copies.length} physical ${
                    copies.length === 1
                      ? "copy"
                      : "copies"
                  }`

              }

            </p>

          </div>


          <button
            type="button"
            className="copy-manager-close"
            onClick={onClose}
            aria-label="Close"
          >

            <X size={19} />

          </button>

        </header>


        {/* ======================================================
            ERROR
            ====================================================== */}

        {error && (

          <div className="copy-manager-error">

            <span>
              {error}
            </span>

            <button
              type="button"
              onClick={loadCopies}
            >

              <RefreshCw size={14} />

              Retry

            </button>

          </div>

        )}


        {/* ======================================================
            LOADING
            ====================================================== */}

        {loading ? (

          <div className="copy-manager-loading">

            <RefreshCw
              size={24}
              className="spin"
            />

            <p>
              Loading physical copies...
            </p>

          </div>


        ) : copies.length === 0 ? (


          /* ====================================================
             EMPTY
             ==================================================== */

          <div className="copy-manager-empty">

            <QrCode size={42} />

            <h3>
              No physical copies found
            </h3>

            <p>
              This book does not have
              registered physical copies yet.
            </p>

          </div>


        ) : (


          /* ====================================================
             COPY LIST
             ==================================================== */

          <div className="copy-list">

            {copies.map((copy) => (

              <article
                key={copy.id}
                className="copy-item"
              >


                {/* =================================================
                   COPY ICON
                   ================================================= */}

                <div className="copy-icon">

                  <QrCode size={25} />

                </div>


                {/* =================================================
                   COPY INFORMATION
                   ================================================= */}

                <div className="copy-main">

                  <div className="copy-title-row">

                    <strong>
                      {copy.copyNumber}
                    </strong>

                    <span
                      className={
                        `copy-status ${
                          getStatusClass(
                            copy.status
                          )
                        }`
                      }
                    >

                      {copy.status}

                    </span>

                  </div>


                  <div className="copy-meta">

                    <span>

                      Condition:{" "}

                      {copy.condition ||
                        "GOOD"}

                    </span>


                    {copy.location && (

                      <span>

                        <MapPin size={13} />

                        {copy.location}

                      </span>

                    )}

                  </div>

                </div>


                {/* =================================================
                   ACTIONS
                   ================================================= */}

                <div className="copy-actions">


                  {/* EDIT */}

                  <button
                    type="button"
                    className="copy-edit-button"
                    onClick={() =>
                      openEdit(copy)
                    }
                    disabled={
                      copy.status ===
                      "DEACTIVATED"
                    }
                    title={
                      copy.status ===
                      "DEACTIVATED"

                        ? "Deactivated copies cannot be edited"

                        : "Edit physical copy"
                    }
                  >

                    <Pencil size={15} />

                    Edit

                  </button>


                  {/* QR LABEL */}

                  <button
                    type="button"
                    className="copy-qr-button"
                    onClick={() =>
                      openQR(copy)
                    }
                    disabled={
                      !copy.qrToken
                    }
                    title={
                      copy.qrToken
                        ? "View QR label"
                        : "QR token unavailable"
                    }
                  >

                    <QrCode size={16} />

                    {copy.qrToken
                      ? "QR Label"
                      : "No QR"}

                  </button>


                  {/* DEACTIVATE */}

                  <button
                    type="button"
                    className="copy-deactivate-button"
                    onClick={() =>
                      deactivateCopy(copy)
                    }
                    disabled={
                      copy.status ===
                        "BORROWED" ||

                      copy.status ===
                        "DEACTIVATED" ||

                      deactivatingId ===
                        copy.id
                    }
                    title={
                      copy.status ===
                      "BORROWED"

                        ? "Cannot deactivate a borrowed copy"

                        : copy.status ===
                          "DEACTIVATED"

                          ? "Copy already deactivated"

                          : "Deactivate physical copy"
                    }
                  >

                    {deactivatingId ===
                    copy.id

                      ? "Deactivating..."

                      : copy.status ===
                        "DEACTIVATED"

                        ? "Deactivated"

                        : "Deactivate"

                    }

                  </button>

                </div>

              </article>

            ))}

          </div>

        )}

      </div>


      {/* ========================================================
          QR LABEL MODAL
          ======================================================== */}

      {selectedQR && (

        <BookCopyQR
          copy={selectedQR}
          onClose={closeQR}
        />

      )}


      {/* ========================================================
          EDIT COPY MODAL
          ======================================================== */}

      {editingCopy && (

        <div
          className="copy-edit-overlay"

          onMouseDown={(event) => {

            if (
              event.target ===
              event.currentTarget
            ) {

              closeEdit();

            }

          }}
        >

          <div className="copy-edit-modal">


            {/* ==================================================
                EDIT HEADER
                ================================================== */}

            <header className="copy-edit-header">

              <div>

                <span className="copy-manager-eyebrow">
                  PHYSICAL COPY
                </span>

                <h3>
                  Edit{" "}
                  {editingCopy.copyNumber}
                </h3>

                <p>
                  Update condition and
                  location.
                </p>

              </div>


              <button
                type="button"
                className="copy-manager-close"
                onClick={closeEdit}
                disabled={savingEdit}
                aria-label="Close edit"
              >

                <X size={19} />

              </button>

            </header>


            {/* ==================================================
                EDIT FORM
                ================================================== */}

            <div className="copy-edit-form">


              {/* CONDITION */}

              <label className="copy-edit-field">

                <span>
                  Condition
                </span>

                <select
                  value={editCondition}
                  onChange={(event) =>
                    setEditCondition(
                      event.target.value
                    )
                  }
                  disabled={savingEdit}
                >

                  <option value="NEW">
                    New
                  </option>

                  <option value="GOOD">
                    Good
                  </option>

                  <option value="FAIR">
                    Fair
                  </option>

                  <option value="DAMAGED">
                    Damaged
                  </option>

                  <option value="LOST">
                    Lost
                  </option>

                </select>

              </label>


              {/* LOCATION */}

              <label className="copy-edit-field">

                <span>
                  Location
                </span>

                <input
                  type="text"
                  value={editLocation}
                  onChange={(event) =>
                    setEditLocation(
                      event.target.value
                    )
                  }
                  placeholder="e.g. LIBRARY"
                  maxLength={100}
                  disabled={savingEdit}
                />

              </label>


              {/* COPY NUMBER */}

              <div className="copy-edit-readonly">

                <span>
                  Copy number
                </span>

                <strong>
                  {editingCopy.copyNumber}
                </strong>

              </div>


              {/* STATUS */}

              <div className="copy-edit-readonly">

                <span>
                  Current status
                </span>

                <strong>
                  {editingCopy.status}
                </strong>

              </div>


              {/* QR TOKEN */}

              <div className="copy-edit-readonly">

                <span>
                  QR token
                </span>

                <strong
                  style={{
                    maxWidth: "60%",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                  }}
                  title={
                    editingCopy.qrToken ||
                    ""
                  }
                >

                  {editingCopy.qrToken ||
                    "Unavailable"}

                </strong>

              </div>


              {/* ACTIONS */}

              <div className="copy-edit-actions">


                <button
                  type="button"
                  className="copy-edit-cancel"
                  onClick={closeEdit}
                  disabled={savingEdit}
                >

                  Cancel

                </button>


                <button
                  type="button"
                  className="copy-edit-save"
                  onClick={saveEdit}
                  disabled={
                    savingEdit ||
                    !editCondition
                  }
                >

                  {savingEdit ? (

                    <>

                      <RefreshCw
                        size={15}
                        className="spin"
                      />

                      Saving...

                    </>

                  ) : (

                    <>

                      <Save size={15} />

                      Save Changes

                    </>

                  )}

                </button>

              </div>

            </div>

          </div>

        </div>

      )}

    </div>

  );
}


export default BookCopyManager;