import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { apiClient } from './api/client';
import { Login } from './components/Login';
import { Layout } from './components/Layout';
import { Dashboard } from './components/Dashboard';
import { ProductTypeList } from './components/ProductTypeList';
import { ProductTypeForm } from './components/ProductTypeForm';
import { CatalogList } from './components/CatalogList';
import { CatalogForm } from './components/CatalogForm';

// Protected Route Component
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  if (!apiClient.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<Login />} />

        {/* Protected Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="product-types" element={<ProductTypeList />} />
          <Route path="product-types/:typeCode" element={<ProductTypeForm />} />
          <Route path="catalog" element={<CatalogList />} />
          <Route path="catalog/:catalogProductId" element={<CatalogForm />} />
        </Route>

        {/* Catch all - redirect to home */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
