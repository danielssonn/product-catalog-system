import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';

export function Dashboard() {
  const [stats, setStats] = useState({
    productTypes: 0,
    catalogProducts: 0,
    activeTypes: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [types, products] = await Promise.all([
        apiClient.getActiveProductTypes(),
        apiClient.getAvailableCatalogProducts(),
      ]);
      setStats({
        productTypes: types.length,
        catalogProducts: products.length,
        activeTypes: types.filter(t => t.active).length,
      });
    } catch (error) {
      console.error('Failed to load stats', error);
    } finally {
      setLoading(false);
    }
  };

  const cards = [
    {
      title: 'Product Types',
      value: stats.productTypes,
      icon: '📦',
      link: '/product-types',
      color: 'bg-blue-500',
    },
    {
      title: 'Catalog Products',
      value: stats.catalogProducts,
      icon: '📚',
      link: '/catalog',
      color: 'bg-green-500',
    },
    {
      title: 'Active Types',
      value: stats.activeTypes,
      icon: '✅',
      link: '/product-types',
      color: 'bg-purple-500',
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-3xl font-bold text-gray-900 mb-8">Dashboard</h2>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {cards.map((card) => (
          <Link
            key={card.title}
            to={card.link}
            className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow p-6 border border-gray-200"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">{card.title}</p>
                <p className="text-3xl font-bold text-gray-900">{card.value}</p>
              </div>
              <div className={`${card.color} w-16 h-16 rounded-lg flex items-center justify-center text-3xl`}>
                {card.icon}
              </div>
            </div>
          </Link>
        ))}
      </div>

      <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Link
            to="/product-types/new"
            className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <span className="text-2xl">➕</span>
            <div>
              <p className="font-medium text-gray-900">Create Product Type</p>
              <p className="text-sm text-gray-500">Add a new product type definition</p>
            </div>
          </Link>
          <Link
            to="/catalog/new"
            className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <span className="text-2xl">📝</span>
            <div>
              <p className="font-medium text-gray-900">Create Catalog Product</p>
              <p className="text-sm text-gray-500">Add a new product to the catalog</p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}
