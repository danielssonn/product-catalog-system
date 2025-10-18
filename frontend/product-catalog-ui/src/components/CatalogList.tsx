import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { CatalogProduct } from '../types/catalog';
import { CatalogStatus } from '../types/catalog';

export function CatalogList() {
  const [catalogProducts, setCatalogProducts] = useState<CatalogProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('all');

  useEffect(() => {
    loadCatalogProducts();
  }, [statusFilter]);

  const loadCatalogProducts = async () => {
    setLoading(true);
    try {
      let data: CatalogProduct[];
      if (statusFilter === 'all') {
        const page = await apiClient.getCatalogProducts(0, 100);
        data = page.content;
      } else {
        const page = await apiClient.getCatalogProductsByStatus(statusFilter);
        data = page.content;
      }
      setCatalogProducts(data);
    } catch (error) {
      console.error('Failed to load catalog products', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (catalogProductId: string) => {
    if (!confirm(`Are you sure you want to delete ${catalogProductId}?`)) return;

    try {
      await apiClient.deleteCatalogProduct(catalogProductId);
      await loadCatalogProducts();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to delete catalog product');
    }
  };

  const getStatusColor = (status: CatalogStatus) => {
    switch (status) {
      case CatalogStatus.AVAILABLE:
        return 'bg-green-100 text-green-800';
      case CatalogStatus.PREVIEW:
        return 'bg-blue-100 text-blue-800';
      case CatalogStatus.DEPRECATED:
        return 'bg-yellow-100 text-yellow-800';
      case CatalogStatus.RETIRED:
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading catalog products...</div>
      </div>
    );
  }

  const statuses = ['all', ...Object.values(CatalogStatus)];

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-3xl font-bold text-gray-900">Catalog Products</h2>
        <Link
          to="/catalog/new"
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors font-medium"
        >
          Create New Product
        </Link>
      </div>

      {/* Status Filter */}
      <div className="mb-6 flex items-center space-x-2">
        <span className="text-sm font-medium text-gray-700">Filter by status:</span>
        {statuses.map(status => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              statusFilter === status
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
            }`}
          >
            {status}
          </button>
        ))}
      </div>

      {/* Catalog Products Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {catalogProducts.map((product) => (
          <div
            key={product.catalogProductId}
            className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow"
          >
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="font-bold text-lg text-gray-900">{product.name}</h3>
                <p className="text-sm text-gray-500 font-mono">{product.catalogProductId}</p>
              </div>
              <span className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(product.status)}`}>
                {product.status}
              </span>
            </div>

            {product.description && (
              <p className="text-sm text-gray-600 mb-4 line-clamp-3">{product.description}</p>
            )}

            <div className="flex items-center space-x-2 mb-4">
              <span className="px-3 py-1 bg-blue-100 text-blue-800 text-xs rounded-full font-medium">
                {product.type}
              </span>
              {product.category && (
                <span className="px-3 py-1 bg-purple-100 text-purple-800 text-xs rounded-full font-medium">
                  {product.category}
                </span>
              )}
            </div>

            {product.pricingTemplate && (
              <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                <p className="text-xs font-medium text-gray-700 mb-1">Pricing</p>
                <div className="text-sm text-gray-600">
                  <div>Type: {product.pricingTemplate.pricingType}</div>
                  {product.pricingTemplate.defaultInterestRate && (
                    <div>Rate: {product.pricingTemplate.defaultInterestRate}%</div>
                  )}
                  <div className="text-xs text-gray-500 mt-1">
                    {product.pricingTemplate.currency}
                  </div>
                </div>
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-gray-200">
              <Link
                to={`/catalog/${product.catalogProductId}`}
                className="text-sm text-blue-600 hover:text-blue-700 font-medium"
              >
                Edit
              </Link>
              <button
                onClick={() => handleDelete(product.catalogProductId)}
                className="text-sm text-red-600 hover:text-red-700 font-medium"
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      {catalogProducts.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">No catalog products found.</p>
        </div>
      )}
    </div>
  );
}
