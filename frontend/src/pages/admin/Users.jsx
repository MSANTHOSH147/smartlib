import { useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  Check,
  ChevronDown,
  Mail,
  Search,
  Shield,
  User,
  UserCheck,
  UserX,
  X,
  Trash2,
} from "lucide-react";
import { Link } from "react-router-dom";

import adminService from "../../services/adminService";

import "./Users.css";

function UsersPage() {
  const [users, setUsers] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] =
    useState("ALL");

  const [updatingId, setUpdatingId] =
    useState(null);

  const [roleModal, setRoleModal] =
    useState(null);

  const [selectedRole, setSelectedRole] =
    useState("");

  const [deleteUser, setDeleteUser] =
    useState(null);

  const [deleting, setDeleting] =
    useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  async function loadUsers() {
    try {
      setLoading(true);
      setError("");

      const data =
        await adminService.getUsers();

      setUsers(
        Array.isArray(data) ? data : []
      );
    } catch (err) {
      console.error(
        "Admin users loading error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to load members."
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusChange(
    user
  ) {
    try {
      setUpdatingId(user.id);
      setError("");

      const updated =
        await adminService.updateUserStatus(
          user.id,
          !user.active
        );

      setUsers((current) =>
        current.map((item) =>
          item.id === user.id
            ? updated
            : item
        )
      );
    } catch (err) {
      console.error(
        "User status update error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to update member status."
      );
    } finally {
      setUpdatingId(null);
    }
  }

  function openRoleModal(user) {
    setRoleModal(user);
    setSelectedRole(user.role);
  }

  function closeRoleModal() {
    if (updatingId !== null) return;

    setRoleModal(null);
    setSelectedRole("");
  }

  async function handleRoleChange() {
    if (!roleModal || !selectedRole) {
      return;
    }

    try {
      setUpdatingId(roleModal.id);
      setError("");

      const updated =
        await adminService.updateUserRole(
          roleModal.id,
          selectedRole
        );

      setUsers((current) =>
        current.map((item) =>
          item.id === roleModal.id
            ? updated
            : item
        )
      );

      setRoleModal(null);
      setSelectedRole("");
    } catch (err) {
      console.error(
        "User role update error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to update member role."
      );
    } finally {
      setUpdatingId(null);
    }
  }

  async function confirmDelete() {
    if (!deleteUser) return;

    try {
      setDeleting(true);
      setError("");

      await adminService.deleteUser(
        deleteUser.id
      );

      setUsers((current) =>
        current.filter(
          (item) =>
            item.id !== deleteUser.id
        )
      );

      setDeleteUser(null);
    } catch (err) {
      console.error(
        "User delete error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to delete member."
      );
    } finally {
      setDeleting(false);
    }
  }

  const filteredUsers = useMemo(() => {
    const search =
      query.trim().toLowerCase();

    return users.filter((user) => {
      const matchesSearch =
        !search ||
        String(user.name || "")
          .toLowerCase()
          .includes(search) ||
        String(user.email || "")
          .toLowerCase()
          .includes(search) ||
        String(user.role || "")
          .toLowerCase()
          .includes(search);

      const matchesStatus =
        statusFilter === "ALL" ||
        (statusFilter === "ACTIVE" &&
          user.active) ||
        (statusFilter === "INACTIVE" &&
          !user.active);

      return (
        matchesSearch &&
        matchesStatus
      );
    });
  }, [
    users,
    query,
    statusFilter,
  ]);

  const totalMembers = users.length;

  const activeMembers = users.filter(
    (user) => user.active
  ).length;

  const inactiveMembers =
    users.filter(
      (user) => !user.active
    ).length;

  const adminMembers = users.filter(
    (user) =>
      String(user.role).toUpperCase() ===
      "ADMIN"
  ).length;

  return (
    <div className="admin-users-page">

      <header className="admin-users-header">

        <div className="admin-users-topline">

          <Link
            to="/admin/dashboard"
            className="admin-users-back"
          >
            <ArrowLeft size={16} />
            Dashboard
          </Link>

          <span className="admin-users-label">
            SMARTLIB / MANAGEMENT
          </span>

        </div>

        <section className="admin-users-hero">

          <div>
            <p className="admin-users-eyebrow">
              MEMBER MANAGEMENT
            </p>

            <h1>
              Members<span>.</span>
            </h1>

            <p className="admin-users-subtitle">
              Manage SmartLib members,
              account access, and library
              roles from one place.
            </p>
          </div>

        </section>

      </header>

      <main className="admin-users-content">

        {error && (
          <div className="admin-users-error">

            <span>{error}</span>

            <button
              type="button"
              onClick={loadUsers}
            >
              Retry
            </button>

          </div>
        )}

        <section className="admin-users-stats">

          <div className="admin-user-stat">

            <div>
              <span>MEMBERS</span>

              <strong>
                {totalMembers}
              </strong>

              <small>
                registered accounts
              </small>
            </div>

            <User size={22} />

          </div>

          <div className="admin-user-stat">

            <div>
              <span>ACTIVE</span>

              <strong>
                {activeMembers}
              </strong>

              <small>
                active accounts
              </small>
            </div>

            <UserCheck size={22} />

          </div>

          <div className="admin-user-stat">

            <div>
              <span>INACTIVE</span>

              <strong>
                {inactiveMembers}
              </strong>

              <small>
                disabled accounts
              </small>
            </div>

            <UserX size={22} />

          </div>

          <div className="admin-user-stat admin-user-stat-highlight">

            <div>
              <span>ADMINISTRATORS</span>

              <strong>
                {adminMembers}
              </strong>

              <small>
                admin accounts
              </small>
            </div>

            <Shield size={22} />

          </div>

        </section>

        <section className="admin-users-toolbar">

          <div className="admin-users-search">

            <Search size={18} />

            <input
              type="text"
              value={query}
              onChange={(event) =>
                setQuery(
                  event.target.value
                )
              }
              placeholder="Search members..."
            />

            {query && (
              <button
                type="button"
                onClick={() =>
                  setQuery("")
                }
              >
                <X size={16} />
              </button>
            )}

          </div>

          <div className="admin-users-filter">

            <select
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(
                  event.target.value
                )
              }
            >
              <option value="ALL">
                All members
              </option>

              <option value="ACTIVE">
                Active
              </option>

              <option value="INACTIVE">
                Inactive
              </option>
            </select>

          </div>

        </section>

        <div className="admin-users-results-header">

          <div>
            <p>
              MEMBER DIRECTORY
            </p>

            <h2>
              {filteredUsers.length}{" "}
              {filteredUsers.length === 1
                ? "member"
                : "members"}
            </h2>
          </div>

          <span>
            Showing{" "}
            {filteredUsers.length} of{" "}
            {users.length}
          </span>

        </div>

        {loading ? (

          <div className="admin-users-loading">

            <div className="admin-users-spinner" />

            <p>
              Loading members...
            </p>

          </div>

        ) : filteredUsers.length === 0 ? (

          <div className="admin-users-empty">

            <User size={42} />

            <h2>
              No members found
            </h2>

            <p>
              Try changing your search
              or status filter.
            </p>

            {(query ||
              statusFilter !==
                "ALL") && (
              <button
                type="button"
                onClick={() => {
                  setQuery("");
                  setStatusFilter(
                    "ALL"
                  );
                }}
              >
                Clear filters
              </button>
            )}

          </div>

        ) : (

          <div className="admin-users-table-card">

            <div className="admin-users-table">

              <div className="admin-users-table-head">

                <span>
                  MEMBER
                </span>

                <span>
                  EMAIL
                </span>

                <span>
                  ROLE
                </span>

                <span>
                  STATUS
                </span>

                <span>
                  ACTIONS
                </span>

              </div>

              {filteredUsers.map(
                (user) => {

                  const isAdmin =
                    String(
                      user.role
                    ).toUpperCase() ===
                    "ADMIN";

                  return (
                    <div
                      className="admin-user-row"
                      key={user.id}
                    >

                      <div className="admin-user-identity">

                        <div className="admin-user-avatar">
                          {user.name
                            ?.charAt(0)
                            .toUpperCase() ||
                            "U"}
                        </div>

                        <div>
                          <strong>
                            {user.name}
                          </strong>

                          <small>
                            Member #{user.id}
                          </small>
                        </div>

                      </div>

                      <div className="admin-user-email">

                        <Mail size={15} />

                        <span>
                          {user.email}
                        </span>

                      </div>

                      <div>

                        <span
                          className={`admin-role ${
                            isAdmin
                              ? "admin-role-admin"
                              : "admin-role-member"
                          }`}
                        >
                          {user.role}
                        </span>

                      </div>

                      <div>

                        <span
                          className={`admin-status ${
                            user.active
                              ? "admin-status-active"
                              : "admin-status-inactive"
                          }`}
                        >
                          <span />
                          {user.active
                            ? "ACTIVE"
                            : "INACTIVE"}
                        </span>

                      </div>

                      <div className="admin-user-actions">

                        <button
                          type="button"
                          className="user-role-button"
                          disabled={
                            updatingId ===
                            user.id
                          }
                          onClick={() =>
                            openRoleModal(
                              user
                            )
                          }
                        >
                          <Shield
                            size={14}
                          />
                          Role
                          <ChevronDown
                            size={13}
                          />
                        </button>

                        <button
                          type="button"
                          className={
                            user.active
                              ? "user-disable-button"
                              : "user-enable-button"
                          }
                          disabled={
                            updatingId ===
                            user.id
                          }
                          onClick={() =>
                            handleStatusChange(
                              user
                            )
                          }
                        >
                          {user.active ? (
                            <>
                              <UserX
                                size={14}
                              />
                              Disable
                            </>
                          ) : (
                            <>
                              <UserCheck
                                size={14}
                              />
                              Enable
                            </>
                          )}
                        </button>

                        {!isAdmin && (
                          <button
                            type="button"
                            className="user-delete-button"
                            disabled={
                              deleting
                            }
                            onClick={() =>
                              setDeleteUser(
                                user
                              )
                            }
                            aria-label={`Delete ${user.name}`}
                          >
                            <Trash2
                              size={15}
                            />
                          </button>
                        )}

                      </div>

                    </div>
                  );
                }
              )}

            </div>

          </div>
        )}

        <div className="admin-users-footer">
          <User size={15} />
          Member accounts are managed
          directly through the SmartLib
          administration system.
        </div>

      </main>

      {/* ROLE MODAL */}

      {roleModal && (
        <div
          className="admin-role-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              closeRoleModal();
            }
          }}
        >

          <div className="admin-role-modal">

            <div className="admin-role-modal-header">

              <div>
                <span>
                  ACCOUNT PERMISSIONS
                </span>

                <h2>
                  Change role
                </h2>
              </div>

              <button
                type="button"
                onClick={closeRoleModal}
                disabled={
                  updatingId !== null
                }
              >
                <X size={18} />
              </button>

            </div>

            <p>
              Update the role for{" "}
              <strong>
                {roleModal.name}
              </strong>
              .
            </p>

            <label className="admin-role-select">

              <span>
                ROLE
              </span>

              <select
                value={selectedRole}
                onChange={(event) =>
                  setSelectedRole(
                    event.target.value
                  )
                }
              >
                <option value="MEMBER">
                  MEMBER
                </option>

                <option value="ADMIN">
                  ADMIN
                </option>
              </select>

            </label>

            <div className="admin-role-actions">

              <button
                type="button"
                onClick={closeRoleModal}
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={
                  handleRoleChange
                }
                disabled={
                  updatingId !== null
                }
              >
                {updatingId !== null
                  ? "Updating..."
                  : "Update Role"}
              </button>

            </div>

          </div>

        </div>
      )}

      {/* DELETE MODAL */}

      {deleteUser && (
        <div
          className="admin-role-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              setDeleteUser(null);
            }
          }}
        >

          <div className="admin-role-modal delete-user-modal">

            <div className="delete-user-icon">
              <Trash2 size={21} />
            </div>

            <span className="admin-delete-eyebrow">
              REMOVE MEMBER
            </span>

            <h2>
              Delete this member?
            </h2>

            <p>
              <strong>
                {deleteUser.name}
              </strong>{" "}
              will be permanently removed
              from SmartLib.
            </p>

            <div className="admin-role-actions">

              <button
                type="button"
                onClick={() =>
                  setDeleteUser(null)
                }
                disabled={deleting}
              >
                Cancel
              </button>

              <button
                type="button"
                className="danger-action"
                onClick={confirmDelete}
                disabled={deleting}
              >
                {deleting
                  ? "Deleting..."
                  : "Delete Member"}
              </button>

            </div>

          </div>

        </div>
      )}

    </div>
  );
}

export default UsersPage;
