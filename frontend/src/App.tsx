import { useState } from "react";
import Dashboard from "./pages/Dashboard";
import Conflicts from "./pages/Conflicts";

type Page = "dashboard" | "conflicts";

function App() {
  const [currentPage, setCurrentPage] = useState<Page>("dashboard");

  return (
    <div className="app-container">
      <nav className="app-navigation">
        <div className="app-brand">
          <span className="app-brand-icon">T</span>
          <span>Tandem</span>
        </div>

        <div className="app-nav-links">
          <button
            className={`app-nav-button ${
              currentPage === "dashboard" ? "active" : ""
            }`}
            onClick={() => setCurrentPage("dashboard")}
          >
            Dashboard
          </button>

          <button
            className={`app-nav-button ${
              currentPage === "conflicts" ? "active" : ""
            }`}
            onClick={() => setCurrentPage("conflicts")}
          >
            Conflicts
          </button>
        </div>
      </nav>

      {currentPage === "dashboard" ? <Dashboard /> : <Conflicts />}
    </div>
  );
}

export default App;