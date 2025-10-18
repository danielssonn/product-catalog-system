import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { ProductTypeDefinition } from '../types/productType';

export function ProductTypeList() {
  const [productTypes, setProductTypes] = useState<ProductTypeDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    loadProductTypes();
  }, []);

  const loadProductTypes = async () => {
    try {
      const data = await apiClient.getActiveProductTypes();
      setProductTypes(data);
    } catch (error) {
      console.error('Failed to load product types', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (typeCode: string) => {
    if (!confirm(`Are you sure you want to delete ${typeCode}?`)) return;

    try {
      await apiClient.deleteProductType(typeCode);
      await loadProductTypes();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to delete product type');
    }
  };

  const handleToggleActive = async (typeCode: string, isActive: boolean) => {
    try {
      if (isActive) {
        await apiClient.deactivateProductType(typeCode);
      } else {
        await apiClient.reactivateProductType(typeCode);
      }
      await loadProductTypes();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to update product type');
    }
  };

  const categories = ['all', ...new Set(productTypes.map(pt => pt.category))];
  const filteredTypes = filter === 'all'
    ? productTypes
    : productTypes.filter(pt => pt.category === filter);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading product types...</div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-3xl font-bold text-gray-900">Product Types</h2>
        <Link
          to="/product-types/new"
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors font-medium"
        >
          Create New Type
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 flex items-center space-x-2">
        <span className="text-sm font-medium text-gray-700">Filter by category:</span>
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setFilter(cat)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              filter === cat
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Product Types Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredTypes.map((type) => (
          <div
            key={type.typeCode}
            className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center space-x-3">
                {type.icon && <span className="text-2xl">{type.icon}</span>}
                <div>
                  <h3 className="font-bold text-lg text-gray-900">{type.name}</h3>
                  <p className="text-sm text-gray-500 font-mono">{type.typeCode}</p>
                </div>
              </div>
              <span className={`px-2 py-1 rounded text-xs font-medium ${
                type.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
              }`}>
                {type.active ? 'Active' : 'Inactive'}
              </span>
            </div>

            {type.description && (
              <p className="text-sm text-gray-600 mb-4">{type.description}</p>
            )}

            <div className="flex items-center space-x-2 mb-4">
              <span className="px-3 py-1 bg-blue-100 text-blue-800 text-xs rounded-full font-medium">
                {type.category}
              </span>
              {type.subcategory && (
                <span className="px-3 py-1 bg-purple-100 text-purple-800 text-xs rounded-full font-medium">
                  {type.subcategory}
                </span>
              )}
            </div>

            {type.tags && type.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 mb-4">
                {type.tags.map((tag, idx) => (
                  <span key={idx} className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded">
                    {tag}
                  </span>
                ))}
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-gray-200">
              <Link
                to={`/product-types/${type.typeCode}`}
                className="text-sm text-blue-600 hover:text-blue-700 font-medium"
              >
                Edit
              </Link>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => handleToggleActive(type.typeCode, type.active)}
                  className="text-sm text-gray-600 hover:text-gray-900 font-medium"
                >
                  {type.active ? 'Deactivate' : 'Activate'}
                </button>
                <button
                  onClick={() => handleDelete(type.typeCode)}
                  className="text-sm text-red-600 hover:text-red-700 font-medium"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {filteredTypes.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">No product types found.</p>
        </div>
      )}
    </div>
  );
}
