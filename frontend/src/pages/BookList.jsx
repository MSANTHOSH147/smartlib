import { useEffect, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Search,
  Star,
} from "lucide-react";
import { Link } from "react-router-dom";

import bookService from "../services/bookService";

import "./BookList.css";

function BookList() {
  const [books, setBooks] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadBooks();
  }, []);

  async function loadBooks() {
    try {
      setLoading(true);
      setError("");

      const data = await bookService.getAllBooks();

      setBooks(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Books loading error:", err);

      setError(
        err.response?.data?.message ||
        "Unable to load books."
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch(event) {
    event.preventDefault();

    const value = query.trim();

    if (!value) {
      loadBooks();
      return;
    }

    try {
      setLoading(true);
      setError("");

      const data = await bookService.searchBooks(value);

      setBooks(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Book search error:", err);

      setError(
        err.response?.data?.message ||
        "Unable to search books."
      );
    } finally {
      setLoading(false);
    }
  }

  function clearSearch() {
    setQuery("");
    loadBooks();
  }

  return (
    <div className="books-page">

      <div className="books-page-top">

        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={16} />
            Dashboard
          </Link>

          <h1>Books</h1>

          <p>
            Browse the SmartLib collection and find your next book.
          </p>
        </div>

      </div>

      <form
        className="books-search"
        onSubmit={handleSearch}
      >
        <Search size={18} />

        <input
          type="text"
          placeholder="Search by book title..."
          value={query}
          onChange={(event) =>
            setQuery(event.target.value)
          }
        />

        <button type="submit">
          Search
        </button>
      </form>

      {query && (
        <div className="search-result-info">
          Showing results for <strong>"{query}"</strong>

          <button onClick={clearSearch}>
            Clear
          </button>
        </div>
      )}

      {error && (
        <div className="books-error">
          {error}

          <button onClick={loadBooks}>
            Retry
          </button>
        </div>
      )}

      {loading ? (

        <div className="books-loading">
          <div className="books-spinner" />
          <p>Loading books...</p>
        </div>

      ) : books.length === 0 ? (

        <div className="books-empty">
          <BookOpen size={35} />

          <h2>No books found</h2>

          <p>
            Try searching with a different title.
          </p>
        </div>

      ) : (

        <>

          <div className="books-heading">
            <div>
              <h2>Library Collection</h2>

              <span>
                {books.length}{" "}
                {books.length === 1 ? "book" : "books"}
              </span>
            </div>
          </div>

          <div className="books-grid">

            {books.map((book) => (

              <Link
                key={book.id}
                to={`/books/${book.id}`}
                className="book-item"
              >

                <div className="book-item-cover">

                  {book.coverImageUrl ? (

                    <img
                      src={book.coverImageUrl}
                      alt={book.title}
                    />

                  ) : (

                    <BookOpen size={34} />

                  )}

                </div>

                <div className="book-item-content">

                  <div className="book-item-category">
                    {book.categoryName || "General"}
                  </div>

                  <h3>{book.title}</h3>

                  <p>{book.author}</p>

                  <div className="book-item-bottom">

                    <div className="book-rating">

                      <Star
                        size={13}
                        fill="currentColor"
                      />

                      <span>
                        {book.averageRating
                          ? Number(book.averageRating).toFixed(1)
                          : "No rating"}
                      </span>

                    </div>

                    <span
                      className={
                        book.availableCopies > 0
                          ? "book-available"
                          : "book-unavailable"
                      }
                    >
                      {book.availableCopies > 0
                        ? `${book.availableCopies} available`
                        : "Unavailable"}
                    </span>

                  </div>

                </div>

                <ArrowRight
                  className="book-arrow"
                  size={16}
                />

              </Link>

            ))}

          </div>

        </>

      )}

    </div>
  );
}

export default BookList;
